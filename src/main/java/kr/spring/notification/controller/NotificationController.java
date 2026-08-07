package kr.spring.notification.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import kr.spring.notification.service.NotificationService;
import kr.spring.notification.vo.NotificationVO;
import kr.spring.users.vo.PrincipalDetails;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/notification")
public class NotificationController {

	private final NotificationService notificationService;

	public NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	// 알림 전체 목록 화면
	@GetMapping("/list")
	public String list(@AuthenticationPrincipal PrincipalDetails principal, Model model) {
		model.addAttribute("currentMenu", "notification");

		long receiver_num = principal.getUsersVO().getUser_num();
		List<NotificationVO> notifications = notificationService.getNotificationListForPage(receiver_num);

		model.addAttribute("notifications", notifications);
		model.addAttribute("unreadNotiCount", notificationService.getUnreadCount(receiver_num));

		return "thviews/notification/list";
	}

	// 알림 1건 읽음 처리 (헤더 드롭다운/목록에서 항목 클릭 시 ajax 로 호출)
	@PostMapping("/read/{noti_num}")
	@ResponseBody
	public String readOne(@AuthenticationPrincipal PrincipalDetails principal,
	                       @PathVariable long noti_num) {
		long receiver_num = principal.getUsersVO().getUser_num();
		notificationService.readNotification(noti_num, receiver_num);
		return "ok";
	}

	// 알림 1건 상세 조회 (알림 상세 팝업, 조회와 동시에 읽음 처리)
	@GetMapping("/{noti_num}")
	@ResponseBody
	public ResponseEntity<NotificationVO> detail(@AuthenticationPrincipal PrincipalDetails principal,
	                                              @PathVariable long noti_num) {
		long receiver_num = principal.getUsersVO().getUser_num();
		NotificationVO vo = notificationService.getNotificationDetail(noti_num, receiver_num);
		if (vo == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(vo);
	}

	// 전체 읽음 처리 (헤더 드롭다운/목록 상단의 "모두 읽음" 버튼)
	@PostMapping("/readAll")
	@ResponseBody
	public String readAll(@AuthenticationPrincipal PrincipalDetails principal) {
		long receiver_num = principal.getUsersVO().getUser_num();
		notificationService.readAllNotification(receiver_num);
		return "ok";
	}

	// 알림 1건 삭제 (목록 화면의 ✕ 버튼)
	@PostMapping("/delete/{noti_num}")
	@ResponseBody
	public String deleteOne(@AuthenticationPrincipal PrincipalDetails principal,
	                         @PathVariable long noti_num) {
		long receiver_num = principal.getUsersVO().getUser_num();
		notificationService.deleteNotification(noti_num, receiver_num);
		return "ok";
	}

	// 전체 삭제 (목록 화면의 "전체 삭제" 버튼)
	@PostMapping("/deleteAll")
	@ResponseBody
	public String deleteAll(@AuthenticationPrincipal PrincipalDetails principal) {
		long receiver_num = principal.getUsersVO().getUser_num();
		notificationService.deleteAllNotification(receiver_num);
		return "ok";
	}
}