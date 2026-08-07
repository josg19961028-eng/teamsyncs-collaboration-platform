package kr.spring.notification.advice;

import java.util.Collections;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import kr.spring.notification.service.NotificationService;
import kr.spring.notification.vo.NotificationVO;
import kr.spring.users.vo.PrincipalDetails;
import lombok.extern.slf4j.Slf4j;

/**
 * 로그인한 사용자가 페이지를 요청할 때마다(=로그인 상태에서 매 화면 진입 시)
 * header.html 알림 드롭다운에서 쓸 알림 목록/안읽음 개수를 모델에 채워준다.
 */
@Slf4j
@ControllerAdvice
public class NotificationModelAdvice {

	private final NotificationService notificationService;

	public NotificationModelAdvice(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@ModelAttribute
	public void addNotifications(Model model) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()
				|| !(authentication.getPrincipal() instanceof PrincipalDetails)) {
			model.addAttribute("notifications", Collections.emptyList());
			model.addAttribute("unreadNotiCount", 0);
			return;
		}

		long receiver_num = ((PrincipalDetails) authentication.getPrincipal()).getUsersVO().getUser_num();

		List<NotificationVO> notifications = notificationService.getNotificationList(receiver_num);
		int unreadCount = notificationService.getUnreadCount(receiver_num);

		log.debug("[Notification] 로그인 사용자 receiver_num={} 알림 {}건, 안읽음 {}건",
				receiver_num, notifications.size(), unreadCount);

		model.addAttribute("notifications", notifications);
		model.addAttribute("unreadNotiCount", unreadCount);
	}
}