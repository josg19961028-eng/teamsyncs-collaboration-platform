package kr.spring.main.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import kr.spring.team.service.TeamMemberService;
import kr.spring.team.vo.TeamMemberVO;
import kr.spring.team.vo.TeamSummaryVO;
import kr.spring.team.vo.TeamVO;
import kr.spring.users.vo.PrincipalDetails;
import kr.spring.users.vo.UserAuth;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class MainController {

	@Autowired
	private TeamMemberService teamMemberService;

	/*============================================
	 * 루트 진입점
	 * - 비로그인: 랜딩 페이지(thviews/main/main) 그대로 노출
	 * - 관리자: /admin/home
	 * - 일반 로그인 사용자: /main/home (대시보드)로 리다이렉트
	 *===========================================*/
	@GetMapping("/")
	public String init(@AuthenticationPrincipal PrincipalDetails principal) {
		if (principal == null) {
			return "thviews/main/main";
		}
		if (principal.getUsersVO().getAuth().equals(UserAuth.ADMIN.getValue())) {
			return "redirect:/admin/home";
		}
		return "redirect:/main/home";
	}

	/*============================================
	 * 로그인 후 사용자 홈 대시보드
	 *===========================================*/
	@GetMapping("/main/home")
	public String home(@AuthenticationPrincipal PrincipalDetails principal, Model model) {
		long myUserNum = principal.getUsersVO().getUser_num();

		// 내가 속한 팀 목록 (TeamMemberMapper.selectTeamsByUserNum)
		List<TeamVO> teams = teamMemberService.selectTeamsByUserNum(myUserNum);

		// 화면 표시용 요약 정보(역할, 팀원 수) 조립
		List<TeamSummaryVO> teamSummaries = new ArrayList<>();
		for (TeamVO team : teams) {
			TeamMemberVO me = teamMemberService.selectMemberByTeamAndUser(team.getTeam_num(), myUserNum);
			int memberCount = teamMemberService.countMembers(team.getTeam_num());

			TeamSummaryVO summary = new TeamSummaryVO();
			summary.setTeam(team);
			summary.setRole(me != null ? me.getRole() : 1);
			summary.setMemberCount(memberCount);
			teamSummaries.add(summary);
		}

		model.addAttribute("userName", principal.getUsersVO().getUser_name());
		model.addAttribute("teamSummaries", teamSummaries);
		return "thviews/main/home";
	}

	/*============================================
	 * 마이페이지 (MP 모듈)
	 * - 프로필 조회/통계는 실데이터, 나머지 탭(비밀번호 변경/개인 To-Do/소셜 연동/
	 *   알림 설정/계정 관리)의 저장 로직은 CM/MP 모듈 담당자 작업 전까지 UI만 제공
	 *===========================================*/
	@GetMapping("/main/mypage")
	public String myPage(@AuthenticationPrincipal PrincipalDetails principal, Model model) {
		long myUserNum = principal.getUsersVO().getUser_num();

		int teamCount = teamMemberService.selectTeamsByUserNum(myUserNum).size();
		boolean isAdmin = principal.getUsersVO().getAuth().equals(UserAuth.ADMIN.getValue());
		boolean isGoogleLinked = principal.getUsersVO().getLogin_type() == 2
				|| principal.getUsersVO().getLogin_type() == 3;

		model.addAttribute("user", principal.getUsersVO());
		model.addAttribute("teamCount", teamCount);
		model.addAttribute("isAdmin", isAdmin);
		model.addAttribute("isGoogleLinked", isGoogleLinked);
		return "thviews/main/mypage";
	}
}