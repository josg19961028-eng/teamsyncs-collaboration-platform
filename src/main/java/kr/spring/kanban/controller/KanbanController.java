package kr.spring.kanban.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpSession;
import kr.spring.kanban.service.KanbanService;
import kr.spring.kanban.vo.KanbanCardVO;
import kr.spring.team.service.TeamMemberService;
import kr.spring.team.vo.TeamMemberVO;
import kr.spring.users.vo.PrincipalDetails;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/kanban")
public class KanbanController {
	@Autowired
	private KanbanService kanbanService;
	@Autowired
	private TeamMemberService teamMemberService;
	
	//팀번호 검증
	private TeamMemberVO validateTeamMember(long team_num, long user_num, HttpSession session) {
		Long sessionTeamNum = (Long) session.getAttribute("teamNum");
		if (sessionTeamNum == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "현재 선택된 팀이 없습니다.");
		}
		if (sessionTeamNum.longValue() != team_num) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "현재 선택한 팀과 일치하지 않습니다.");
		}
		TeamMemberVO member = teamMemberService.selectMemberByTeamAndUser(team_num, user_num);
		if (member == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 팀에 접근할 권한이 없습니다.");
		}
		return member;
	}

	@GetMapping("/board")
	public String boardFromSession(HttpSession session) {
		Long team_num = (Long) session.getAttribute("teamNum");

		if (team_num == null) {
			return "redirect:/team/list";
		}

		return "redirect:/kanban/board/" + team_num;
	}

	@GetMapping("/board/{team_num}")
	public String board(@PathVariable("team_num") long team_num,
	        			@AuthenticationPrincipal PrincipalDetails principal,
	        			HttpSession session,Model model) {

		long user_num = principal.getUsersVO().getUser_num();
		validateTeamMember(team_num, user_num, session);
		
	    // 칸반카드 태그 목록 조회
	    List<String> tagList = kanbanService.selectKanbanTagList(team_num);

	    model.addAttribute("currentMenu", "kanban");
	    model.addAttribute("team_num", team_num);
	    model.addAttribute("tagList", tagList);

	    return "thviews/kanban/board";
	}
	/*=====================================================
	 * 칸반보드 작성 폼 열기
	 * ===================================================*/
	@GetMapping("/board/{team_num}/write-form")
	public String getWriteForm(@PathVariable("team_num") long team_num,
			@AuthenticationPrincipal PrincipalDetails principal, HttpSession session, Model model) {
		
		long user_num = principal.getUsersVO().getUser_num();
		validateTeamMember(team_num, user_num, session);
		
		KanbanCardVO card = new KanbanCardVO();
		List<TeamMemberVO> teamMemberList = kanbanService.selectTeamMember(team_num);

		model.addAttribute("kanbanCardVO", card);
		model.addAttribute("teamMemberList", teamMemberList);

		return "thviews/kanban/card_form :: kanbanWriteForm";
	}
	/*=====================================================
	 * 칸반보드 상세보기
	 * ===================================================*/
	@GetMapping("/board/{team_num}/card/{card_num}")
	public String getCardDetail(@PathVariable("team_num") long team_num, 
								@PathVariable("card_num") long card_num,
								@AuthenticationPrincipal PrincipalDetails principal,
								HttpSession session,
								Model model) {
		
		long user_num = principal.getUsersVO().getUser_num();
		validateTeamMember(team_num, user_num, session);
		//카드조회
		KanbanCardVO card = kanbanService.selectKanbanCard(card_num, team_num);
		// 팀원 목록 조회
	    List<TeamMemberVO> teamMemberList = kanbanService.selectTeamMember(team_num);
		
		card.setEditable(kanbanService.canChangeStatus(card_num, user_num, team_num));
		
		model.addAttribute("card", card);
		model.addAttribute("team_num", team_num);
		model.addAttribute("teamMemberList", teamMemberList);

		return "thviews/kanban/card_detail :: kanbanCardDetail";
	}
	/*=====================================================
	 * 칸반보드 수정폼열기 
	 * ===================================================*/
	@GetMapping("/board/{team_num}/card/{card_num}/update-form")
	public String getUpdateForm(@PathVariable("team_num") long team_num,
								@PathVariable("card_num") long card_num,
								@AuthenticationPrincipal PrincipalDetails principal,
								HttpSession session,
								Model model) {
		long user_num = principal.getUsersVO().getUser_num();
		validateTeamMember(team_num, user_num, session);
		
		//권한 체크
		if(!kanbanService.canChangeStatus(card_num, user_num, team_num)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,"작성자 또는 담당자만 카드를 수정할 수 있습니다.");
		}
		//카드조회
		KanbanCardVO card = kanbanService.selectKanbanCard(card_num, team_num);
		//팀원조회
		List<TeamMemberVO> teamMemberList = kanbanService.selectTeamMember(team_num);
		
		
		model.addAttribute("kanbanCardVO", card);
		model.addAttribute("teamMemberList",teamMemberList);
		
		return "thviews/kanban/card_update :: kanbanUpdateForm";
	}
}









