package kr.spring.notification.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.spring.notification.vo.NotificationVO;

@Mapper
public interface NotificationMapper {

	// 로그인한 사용자(수신자)의 알림 목록 조회 (최신순)
	List<NotificationVO> selectNotificationList(long receiver_num);

	// 로그인한 사용자(수신자)의 알림 전체 목록 조회 (전체 알림 페이지용, 최신순, 최대 100건)
	List<NotificationVO> selectNotificationListPage(long receiver_num);

	// 알림 1건 상세 조회 (본인이 받은 알림만, 상세 팝업용)
	NotificationVO selectNotificationDetail(@Param("noti_num") long noti_num,
	                                         @Param("receiver_num") long receiver_num);

	// 안읽은 알림 개수 (read_status = 1)
	int selectUnreadCount(long receiver_num);

	// 알림 1건 등록
	void insertNotification(NotificationVO vo);

	// 알림 1건 읽음 처리 (read_status -> 2)
	void updateRead(@Param("noti_num") long noti_num,
	                 @Param("receiver_num") long receiver_num);

	// 수신자의 전체 알림 읽음 처리
	void updateReadAll(long receiver_num);

	// 알림 1건 삭제 (본인이 받은 알림만)
	void deleteNotification(@Param("noti_num") long noti_num,
	                         @Param("receiver_num") long receiver_num);

	// 수신자의 전체 알림 삭제
	void deleteAllNotification(long receiver_num);
}