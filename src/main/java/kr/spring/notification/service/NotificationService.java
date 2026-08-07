package kr.spring.notification.service;

import java.util.List;

import kr.spring.notification.vo.NotificationVO;

public interface NotificationService {

	// 로그인한 사용자(수신자)의 알림 목록 조회 (아이콘/상대시간 가공 포함)
	List<NotificationVO> getNotificationList(long receiver_num);

	// 전체 알림 페이지용 목록 조회 (아이콘/상대시간/날짜그룹 가공 포함)
	List<NotificationVO> getNotificationListForPage(long receiver_num);

	// 알림 1건 상세 조회 (상세 팝업용, 조회 시 읽음 처리 포함)
	NotificationVO getNotificationDetail(long noti_num, long receiver_num);

	// 안읽은 알림 개수
	int getUnreadCount(long receiver_num);

	// 알림 등록
	void addNotification(NotificationVO vo);

	// 알림 1건 읽음 처리
	void readNotification(long noti_num, long receiver_num);

	// 전체 읽음 처리
	void readAllNotification(long receiver_num);

	// 알림 1건 삭제
	void deleteNotification(long noti_num, long receiver_num);

	// 전체 알림 삭제
	void deleteAllNotification(long receiver_num);
}