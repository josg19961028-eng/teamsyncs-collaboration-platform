package kr.spring.team.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kr.spring.notification.service.NotificationService;
import kr.spring.notification.vo.NotificationVO;
import kr.spring.team.service.EmailInvitationService;
import kr.spring.team.service.MailService;
import kr.spring.team.service.TeamInviteCodeService;
import kr.spring.team.service.TeamMemberService;
import kr.spring.team.service.TeamService;
import kr.spring.team.vo.EmailInvitationVO;
import kr.spring.team.vo.TeamInviteCodeVO;
import kr.spring.team.vo.TeamMemberVO;
import kr.spring.team.vo.TeamVO;
import kr.spring.users.service.UsersService;
import kr.spring.users.vo.PrincipalDetails;
import kr.spring.users.vo.UsersVO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/team")
public class TeamInviteController {

	private static final int ROLE_LEADER = 3;
	private static final int ROLE_MEMBER = 1;

	// 팀원 가입 상태
	private static final int JOIN_JOINED = 1;

	// 초대코드 상태
	private static final int CODE_VALID = 1;
	private static final int CODE_EXPIRED = 2;
	private static final int CODE_DISABLED = 3;

	// 이메일 초대 상태
	private static final int INV_PENDING = 1;
	private static final int INV_ACCEPTED = 2;
	private static final int INV_REJECTED = 3;
	private static final int INV_EXPIRED = 4;

	@Autowired
	private TeamInviteCodeService teamInviteCodeService;
	@Autowired
	private EmailInvitationService emailInvitationService;
	@Autowired
	private TeamMemberService teamMemberService;
	@Autowired
	private TeamService teamService;
	@Autowired
	private UsersService usersService;
	@Autowired
	private MailService mailService;
	@Autowired
	private NotificationService notificationService;

	private long getTeamNum(HttpSession session) {
		Object teamNum = session.getAttribute("teamNum");
		return teamNum == null ? 0 : (Long) teamNum;
	}

	/*============================================
	 * 초대코드 발급 / 재발급 (TM-004)
	 *===========================================*/
	@PostMapping("/invite/code/issue")
	@ResponseBody
	public String issueInviteCode(@AuthenticationPrincipal PrincipalDetails principal, HttpSession session) {
		long teamNum = getTeamNum(session);
		long userNum = principal.getUsersVO().getUser_num();

		TeamMemberVO me = teamMemberService.selectMemberByTeamAndUser(teamNum, userNum);
		if (me == null || me.getRole() != ROLE_LEADER) {
			return "NO_PERMISSION";
		}

		String newCode = UUID.randomUUID().toString();
		TeamInviteCodeVO existing = teamInviteCodeService.selectByTeamNum(teamNum);

		if (existing == null) {
			TeamInviteCodeVO vo = new TeamInviteCodeVO();
			vo.setTeam_num(teamNum);
			vo.setCode(newCode);
			vo.setIssuer_num(userNum);
			teamInviteCodeService.insertInviteCode(vo);
		} else {
			existing.setCode(newCode);
			existing.setIssuer_num(userNum);
			teamInviteCodeService.updateInviteCode(existing);
		}

		log.debug("<<초대코드 발급>> teamNum={}, code={}", teamNum, newCode);
		return "OK:" + newCode;
	}

	/*============================================
	 * 초대코드 수동 비활성화 (팀 설정)
	 *===========================================*/
	@PostMapping("/invite/code/disable")
	@ResponseBody
	public String disableInviteCode(@AuthenticationPrincipal PrincipalDetails principal, HttpSession session) {
		long teamNum = getTeamNum(session);
		long userNum = principal.getUsersVO().getUser_num();

		TeamMemberVO me = teamMemberService.selectMemberByTeamAndUser(teamNum, userNum);
		if (me == null || me.getRole() != ROLE_LEADER) {
			return "NO_PERMISSION";
		}

		TeamInviteCodeVO existing = teamInviteCodeService.selectByTeamNum(teamNum);
		if (existing == null) {
			return "NO_CODE";
		}

		teamInviteCodeService.disableInviteCodeByTeam(teamNum);
		log.debug("<<초대코드 수동 비활성화>> teamNum={}", teamNum);
		return "OK";
	}

	/*============================================
	 * 현재 팀의 초대코드 조회 (팀 설정 화면 표시용)
	 *===========================================*/
	@GetMapping("/invite/code")
	@ResponseBody
	public TeamInviteCodeVO getInviteCode(HttpSession session) {
		long teamNum = getTeamNum(session);
		TeamInviteCodeVO code = teamInviteCodeService.selectByTeamNum(teamNum);
		if (code == null) {
			return null;
		}
		if (code.getStatus() == CODE_VALID && code.getExpired_at().before(new java.util.Date())) {
			teamInviteCodeService.expireInviteCode(code.getInvite_code_num());
			code.setStatus(CODE_EXPIRED);
		}
		return code;
	}

	/*============================================
	 * 초대코드로 팀 참여 (TM-004)
	 * - 즉시 가입(JOINED) 처리 방식으로 확정된 정책 (랜딩 안 거치는 직접 입력 포함)
	 *===========================================*/
	@PostMapping("/join/code")
	@ResponseBody
	public String joinByCode(@AuthenticationPrincipal PrincipalDetails principal, @RequestParam String code) {
		if (code == null || code.trim().isEmpty()) {
			return "INVALID_CODE";
		}
		TeamInviteCodeVO invite = teamInviteCodeService.selectByCode(code.trim());
		if (invite == null) {
			return "INVALID_CODE";
		}
		if (invite.getStatus() == CODE_DISABLED) {
			return "DISABLED_CODE";
		}
		if (invite.getStatus() == CODE_EXPIRED) {
			return "EXPIRED_CODE";
		}
		if (invite.getExpired_at().before(new java.util.Date())) {
			teamInviteCodeService.expireInviteCode(invite.getInvite_code_num());
			return "EXPIRED_CODE";
		}

		TeamVO team = teamService.selectTeamByNum(invite.getTeam_num());
		if (team == null) {
			return "TEAM_NOT_FOUND";
		}

		long userNum = principal.getUsersVO().getUser_num();
		long teamNum = invite.getTeam_num();

		TeamMemberVO existing = teamMemberService.selectMemberByTeamAndUserAnyStatus(teamNum, userNum);
		if (existing != null) {
			if (existing.getJoin_status() == JOIN_JOINED) {
				return "ALREADY_JOINED";
			}
			teamMemberService.reactivateMember(teamNum, userNum);
		} else {
			TeamMemberVO newMember = new TeamMemberVO();
			newMember.setTeam_num(teamNum);
			newMember.setUser_num(userNum);
			newMember.setRole(ROLE_MEMBER);
			teamMemberService.insertTeamMember(newMember);
		}

		log.debug("<<초대코드 참여>> teamNum={}, userNum={}", teamNum, userNum);
		return "OK:" + teamNum;
	}

	/*============================================
	 * 이메일 초대 발송 (TM-005)
	 * - 메일/알림 모두 "초대 확인하기" 랜딩(GET /invite/email/accept) 링크 하나만 사용
	 *===========================================*/
	@PostMapping("/invite/email/send")
	@ResponseBody
	public String sendEmailInvite(@AuthenticationPrincipal PrincipalDetails principal,
			@RequestParam String email, HttpSession session, HttpServletRequest request) {
		long teamNum = getTeamNum(session);
		long userNum = principal.getUsersVO().getUser_num();

		TeamMemberVO me = teamMemberService.selectMemberByTeamAndUser(teamNum, userNum);
		if (me == null || me.getRole() != ROLE_LEADER) {
			return "NO_PERMISSION";
		}
		if (email == null || email.trim().isEmpty()) {
			return "INVALID_EMAIL";
		}
		String targetEmail = email.trim();

		UsersVO targetUser = usersService.selectByEmail(targetEmail);
		if (targetUser == null || targetUser.getStatus() != 1) {
			return "NO_SUCH_USER";
		}
		TeamMemberVO targetMember = teamMemberService.selectMemberByTeamAndUser(teamNum, targetUser.getUser_num());
		if (targetMember != null) {
			return "ALREADY_MEMBER";
		}

		TeamVO team = teamService.selectTeamByNum(teamNum);
		if (team == null) {
			return "TEAM_NOT_FOUND";
		}

		EmailInvitationVO existing = emailInvitationService.selectByTeamAndEmail(teamNum, targetEmail);
		long invitationNum;

		if (existing != null && existing.getStatus() == INV_PENDING) {
			return "ALREADY_PENDING";
		} else if (existing != null) {
			emailInvitationService.updateToPending(existing.getInvitation_num());
			invitationNum = existing.getInvitation_num();
		} else {
			EmailInvitationVO vo = new EmailInvitationVO();
			vo.setTeam_num(teamNum);
			vo.setInviter_num(userNum);
			vo.setInvitee_email(targetEmail);
			emailInvitationService.insertInvitation(vo);
			invitationNum = vo.getInvitation_num();
		}

		String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
		String viewUrl = baseUrl + "/team/invite/email/accept?invitationNum=" + invitationNum;

		try {
			mailService.sendTeamInviteMail(targetEmail, team.getTeam_name(),
					principal.getUsersVO().getUser_name(), viewUrl);
		} catch (RuntimeException e) {
			return "SEND_FAIL";
		}

		log.debug("<<이메일 초대 발송>> teamNum={}, email={}", teamNum, targetEmail);

		// 알림: 초대받은 사람에게 (랜딩 페이지로 이동)
		try {
			NotificationVO noti = new NotificationVO();
			noti.setSender_num(userNum);
			noti.setReceiver_num(targetUser.getUser_num());
			noti.setTeam_num(teamNum);
			noti.setNoti_type(4);
			noti.setTitle("[" + team.getTeam_name() + "] 팀 초대");
			noti.setContent(principal.getUsersVO().getUser_name() + "님이 팀에 초대했습니다.");
			noti.setLink("/team/invite/email/accept?invitationNum=" + invitationNum);
			notificationService.addNotification(noti);
		} catch (Exception e) {
			log.warn("<<이메일 초대 알림 발송 실패>> {}", e.getMessage());
		}

		return "OK";
	}

	/*============================================
	 * 이메일 초대 랜딩 (TM-006) - 조회 전용, 처리는 하지 않음
	 * - 메일 링크 / 알림 클릭 모두 이 URL로 도착 -> 상태만 판단해서 랜딩 화면 표시
	 * - 실제 수락/거절은 랜딩 화면의 버튼이 아래 POST 엔드포인트를 AJAX로 호출
	 *===========================================*/
	@GetMapping("/invite/email/accept")
	public String viewEmailInvite(@AuthenticationPrincipal(errorOnInvalidType = false) PrincipalDetails principal,
			@RequestParam long invitationNum,
			HttpServletRequest request,
			HttpServletResponse response,
			Model model) {

		EmailInvitationVO invitation = emailInvitationService.selectByNum(invitationNum);
		if (invitation == null) {
			model.addAttribute("emailInviteStatus", "NOTFOUND");
			model.addAttribute("invitationNum", invitationNum);
			return "thviews/team/invite-email-landing";
		}

		// 로그인 안 된 상태 -> 현재 URL을 세션에 저장 후 로그인 페이지로
		// 로그인 성공 시 InviteReturnAuthenticationSuccessHandler가 savedRequest로 이 URL에 복귀
		if (principal == null) {
			new org.springframework.security.web.savedrequest.HttpSessionRequestCache()
					.saveRequest(request, response);
			return "redirect:/member/login";
		}

		// 초대 대상과 다른 계정으로 로그인 중 -> 로그아웃 없이 WRONG_USER 상태로 랜딩 표시
		// (로그아웃+redirect 방식은 재로그인 후 무한루프 발생)
		if (!principal.getUsersVO().getEmail().equalsIgnoreCase(invitation.getInvitee_email())) {
			TeamVO wrongTeam = teamService.selectTeamByNum(invitation.getTeam_num());
			model.addAttribute("emailInviteStatus", "WRONG_USER");
			model.addAttribute("invitationNum", invitationNum);
			model.addAttribute("teamName", wrongTeam != null ? wrongTeam.getTeam_name() : "");
			return "thviews/team/invite-email-landing";
		}

		long teamNum = invitation.getTeam_num();
		long userNum = principal.getUsersVO().getUser_num();
		TeamVO team = teamService.selectTeamByNum(teamNum);

		String status;
		if (invitation.getStatus() == INV_ACCEPTED) {
			status = "ALREADY_JOINED";
		} else if (invitation.getStatus() == INV_REJECTED) {
			status = "REJECTED";
		} else if (invitation.getStatus() != INV_PENDING) {
			status = "NOT_PENDING";
		} else if (invitation.getExpired_at().before(new java.util.Date())) {
			emailInvitationService.updateStatusOnly(invitationNum, INV_EXPIRED);
			status = "EXPIRED";
		} else {
			TeamMemberVO existing = teamMemberService.selectMemberByTeamAndUserAnyStatus(teamNum, userNum);
			status = (existing != null && existing.getJoin_status() == JOIN_JOINED) ? "ALREADY_JOINED" : "OK";
		}

		model.addAttribute("emailInviteStatus", status);
		model.addAttribute("invitationNum", invitationNum);
		model.addAttribute("teamName", team != null ? team.getTeam_name() : "");
		return "thviews/team/invite-email-landing";
	}

	/*============================================
	 * 이메일 초대 수락 확정 (TM-006, AJAX)
	 * - 랜딩 화면의 "수락하기" 버튼에서 호출
	 *===========================================*/
	@PostMapping("/invite/email/accept")
	@ResponseBody
	public String confirmAcceptEmailInvite(@AuthenticationPrincipal PrincipalDetails principal,
			@RequestParam long invitationNum) {
		EmailInvitationVO invitation = emailInvitationService.selectByNum(invitationNum);
		if (invitation == null) {
			return "NOTFOUND";
		}

		UsersVO me = principal.getUsersVO();
		if (!me.getEmail().equalsIgnoreCase(invitation.getInvitee_email())) {
			return "NO_PERMISSION";
		}
		if (invitation.getStatus() != INV_PENDING) {
			return "NOT_PENDING";
		}
		if (invitation.getExpired_at().before(new java.util.Date())) {
			emailInvitationService.updateStatusOnly(invitationNum, INV_EXPIRED);
			return "EXPIRED";
		}

		long teamNum = invitation.getTeam_num();
		long userNum = me.getUser_num();

		TeamMemberVO existing = teamMemberService.selectMemberByTeamAndUserAnyStatus(teamNum, userNum);
		if (existing != null && existing.getJoin_status() == JOIN_JOINED) {
			return "ALREADY_JOINED";
		}

		emailInvitationService.respondInvitation(invitationNum, INV_ACCEPTED);

		if (existing != null) {
			teamMemberService.reactivateMember(teamNum, userNum);
		} else {
			TeamMemberVO newMember = new TeamMemberVO();
			newMember.setTeam_num(teamNum);
			newMember.setUser_num(userNum);
			newMember.setRole(ROLE_MEMBER);
			teamMemberService.insertTeamMember(newMember);
		}

		// 알림: 팀장에게
		try {
			TeamMemberVO leader = teamMemberService.selectLeaderByTeamNum(teamNum);
			if (leader != null) {
				TeamVO team = teamService.selectTeamByNum(teamNum);
				NotificationVO noti = new NotificationVO();
				noti.setSender_num(userNum);
				noti.setReceiver_num(leader.getUser_num());
				noti.setTeam_num(teamNum);
				noti.setNoti_type(4);
				noti.setTitle("초대 수락");
				noti.setContent(me.getUser_name() + "님이 " + (team != null ? team.getTeam_name() : "") + " 팀 초대를 수락했습니다.");
				noti.setLink("/main/home");
				notificationService.addNotification(noti);
			}
		} catch (Exception e) {
			log.warn("<<초대 수락 알림 발송 실패>> {}", e.getMessage());
		}

		log.debug("<<이메일 초대 수락>> teamNum={}, userNum={}", teamNum, userNum);
		return "OK";
	}

	/*============================================
	 * 이메일 초대 거절 확정 (TM-006, AJAX)
	 * - 랜딩 화면의 "거절하기" 버튼에서 호출
	 *===========================================*/
	@PostMapping("/invite/email/reject")
	@ResponseBody
	public String confirmRejectEmailInvite(@AuthenticationPrincipal PrincipalDetails principal,
			@RequestParam long invitationNum) {
		EmailInvitationVO invitation = emailInvitationService.selectByNum(invitationNum);
		if (invitation == null) {
			return "NOTFOUND";
		}

		if (!principal.getUsersVO().getEmail().equalsIgnoreCase(invitation.getInvitee_email())) {
			return "NO_PERMISSION";
		}
		if (invitation.getStatus() != INV_PENDING) {
			return "NOT_PENDING";
		}

		emailInvitationService.respondInvitation(invitationNum, INV_REJECTED);

		// 알림: 팀장에게
		try {
			TeamMemberVO leader = teamMemberService.selectLeaderByTeamNum(invitation.getTeam_num());
			if (leader != null) {
				TeamVO team = teamService.selectTeamByNum(invitation.getTeam_num());
				NotificationVO noti = new NotificationVO();
				noti.setSender_num(principal.getUsersVO().getUser_num());
				noti.setReceiver_num(leader.getUser_num());
				noti.setTeam_num(invitation.getTeam_num());
				noti.setNoti_type(4);
				noti.setTitle("초대 거절");
				noti.setContent(principal.getUsersVO().getUser_name() + "님이 " + (team != null ? team.getTeam_name() : "") + " 팀 초대를 거절했습니다.");
				noti.setLink(null);
				notificationService.addNotification(noti);
			}
		} catch (Exception e) {
			log.warn("<<초대 거절 알림 발송 실패>> {}", e.getMessage());
		}

		log.debug("<<이메일 초대 거절>> invitationNum={}", invitationNum);
		return "OK";
	}

	/*============================================
	 * 이메일 자동완성 검색 (이메일 초대 입력 시)
	 *===========================================*/
	@GetMapping("/invite/email/search")
	@ResponseBody
	public java.util.List<String> searchInviteEmail(@AuthenticationPrincipal PrincipalDetails principal,
			@RequestParam String keyword, HttpSession session) {
		long teamNum = getTeamNum(session);
		long userNum = principal.getUsersVO().getUser_num();

		TeamMemberVO me = teamMemberService.selectMemberByTeamAndUser(teamNum, userNum);
		if (me == null || me.getRole() != ROLE_LEADER) {
			return java.util.Collections.emptyList();
		}
		if (keyword == null || keyword.trim().length() < 2) {
			return java.util.Collections.emptyList();
		}
		return teamMemberService.searchEmailsForInvite(teamNum, keyword.trim());
	}

	/*============================================
	 * 초대 링크 랜딩 (TM-004 초대 링크 공유)
	 *===========================================*/
	@GetMapping("/invite/link/{code}")
	public String inviteLink(@AuthenticationPrincipal(errorOnInvalidType = false) PrincipalDetails principal,
			@PathVariable String code, Model model) {

		if (principal == null) {
			return "redirect:/member/login";
		}

		String status;
		String teamName = null;

		TeamInviteCodeVO invite = teamInviteCodeService.selectByCode(code);

		if (invite == null) {
			status = "INVALID";
		} else if (invite.getStatus() == CODE_DISABLED) {
			status = "DISABLED";
		} else if (invite.getStatus() == CODE_EXPIRED
				|| invite.getExpired_at().before(new java.util.Date())) {
			if (invite.getStatus() == CODE_VALID) {
				teamInviteCodeService.expireInviteCode(invite.getInvite_code_num());
			}
			status = "EXPIRED";
		} else {
			TeamVO team = teamService.selectTeamByNum(invite.getTeam_num());
			if (team == null) {
				status = "TEAM_NOT_FOUND";
			} else {
				teamName = team.getTeam_name();
				long userNum = principal.getUsersVO().getUser_num();
				TeamMemberVO existing =
						teamMemberService.selectMemberByTeamAndUserAnyStatus(invite.getTeam_num(), userNum);
				status = (existing != null && existing.getJoin_status() == JOIN_JOINED) ? "ALREADY" : "OK";
			}
		}

		model.addAttribute("inviteStatus", status);
		model.addAttribute("inviteCode", code);
		model.addAttribute("teamName", teamName);
		return "thviews/team/invite-landing";
	}

}