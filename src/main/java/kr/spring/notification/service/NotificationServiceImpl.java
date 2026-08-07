package kr.spring.notification.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.spring.notification.dao.NotificationMapper;
import kr.spring.notification.vo.NotificationVO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

	private final NotificationMapper notificationMapper;

	public NotificationServiceImpl(NotificationMapper notificationMapper) {
		this.notificationMapper = notificationMapper;
	}

	// noti_type 별 아이콘: 1=일정 2=채팅 3=칸반 4=팀 5=공지
	private static final Map<Integer, String> ICON_MAP = Map.of(
			1, "📅",
			2, "💬",
			3, "📋",
			4, "👥",
			5, "📢"
	);

	// noti_type 별 표시 텍스트
	private static final Map<Integer, String> TYPE_LABEL_MAP = Map.of(
			1, "일정",
			2, "채팅",
			3, "칸반",
			4, "팀",
			5, "공지"
	);

	// noti_type 별 뱃지 CSS 클래스
	private static final Map<Integer, String> TYPE_CLASS_MAP = Map.of(
			1, "type-1",
			2, "type-2",
			3, "type-3",
			4, "type-4",
			5, "type-5"
	);

	@Override
	public List<NotificationVO> getNotificationList(long receiver_num) {
		List<NotificationVO> list = notificationMapper.selectNotificationList(receiver_num);
		enrichList(list);
		log.debug("[Notification] receiver_num={} 알림 {}건 조회", receiver_num, list.size());
		return list;
	}

	@Override
	public List<NotificationVO> getNotificationListForPage(long receiver_num) {
		List<NotificationVO> list = notificationMapper.selectNotificationListPage(receiver_num);
		enrichList(list);
		log.debug("[Notification] receiver_num={} 전체 알림 페이지 {}건 조회", receiver_num, list.size());
		return list;
	}

	@Override
	public int getUnreadCount(long receiver_num) {
		return notificationMapper.selectUnreadCount(receiver_num);
	}

	// 알림 1건 상세 조회: 조회와 동시에 읽음 처리 후 상세 정보를 가공해서 반환
	@Override
	public NotificationVO getNotificationDetail(long noti_num, long receiver_num) {
		notificationMapper.updateRead(noti_num, receiver_num);
		NotificationVO vo = notificationMapper.selectNotificationDetail(noti_num, receiver_num);
		if (vo != null) {
			enrichOne(vo);
		}
		log.debug("[Notification] receiver_num={} noti_num={} 상세 조회", receiver_num, noti_num);
		return vo;
	}

	@Override
	public void addNotification(NotificationVO vo) {
		notificationMapper.insertNotification(vo);
	}

	@Override
	public void readNotification(long noti_num, long receiver_num) {
		notificationMapper.updateRead(noti_num, receiver_num);
	}

	@Override
	public void readAllNotification(long receiver_num) {
		notificationMapper.updateReadAll(receiver_num);
	}

	@Override
	public void deleteNotification(long noti_num, long receiver_num) {
		notificationMapper.deleteNotification(noti_num, receiver_num);
	}

	@Override
	public void deleteAllNotification(long receiver_num) {
		notificationMapper.deleteAllNotification(receiver_num);
	}

	// 목록 화면 표시용 필드(아이콘/뱃지/상대시간/날짜그룹) 가공
	private void enrichList(List<NotificationVO> list) {
		for (NotificationVO vo : list) {
			enrichOne(vo);
		}
	}

	// 알림 1건에 화면 표시용 필드(아이콘/뱃지/상대시간/날짜그룹/전체일시) 가공
	private void enrichOne(NotificationVO vo) {
		vo.setIcon(ICON_MAP.getOrDefault(vo.getNoti_type(), "🔔"));
		vo.setTypeLabel(TYPE_LABEL_MAP.getOrDefault(vo.getNoti_type(), "알림"));
		vo.setTypeClass(TYPE_CLASS_MAP.getOrDefault(vo.getNoti_type(), "type-0"));
		vo.setTimeText(toTimeText(vo.getReg_date()));
		vo.setDateLabel(toDateLabel(vo.getReg_date()));
		vo.setFullDateText(toFullDateText(vo.getReg_date()));
	}

	// 알림 시각을 "방금 전 / N분 전 / N시간 전 / N일 전" 형태로 변환
	private String toTimeText(java.sql.Date regDate) {
		if (regDate == null) return "";
		LocalDateTime regDateTime = new java.sql.Timestamp(regDate.getTime()).toLocalDateTime();
		Duration diff = Duration.between(regDateTime, LocalDateTime.now());

		long minutes = diff.toMinutes();
		if (minutes < 1)  return "방금 전";
		if (minutes < 60) return minutes + "분 전";

		long hours = diff.toHours();
		if (hours < 24) return hours + "시간 전";

		long days = diff.toDays();
		if (days < 2) return "어제";
		return days + "일 전";
	}

	// 알림 날짜를 목록 그룹 헤더용 텍스트로 변환: "오늘" / "어제" / "yyyy-MM-dd"
	private String toDateLabel(java.sql.Date regDate) {
		if (regDate == null) return "";
		LocalDate regLocalDate = new java.sql.Timestamp(regDate.getTime()).toLocalDateTime().toLocalDate();
		LocalDate today = LocalDate.now();
		long daysBetween = ChronoUnit.DAYS.between(regLocalDate, today);

		if (daysBetween == 0) return "오늘";
		if (daysBetween == 1) return "어제";
		return regLocalDate.toString();
	}

	// 알림 시각을 상세 팝업용 전체 일시 텍스트로 변환: "yyyy.MM.dd HH:mm"
	private String toFullDateText(java.sql.Date regDate) {
		if (regDate == null) return "";
		LocalDateTime regDateTime = new java.sql.Timestamp(regDate.getTime()).toLocalDateTime();
		return regDateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"));
	}
}