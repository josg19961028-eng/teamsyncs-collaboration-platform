package kr.spring.kanban.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kr.spring.kanban.service.KanbanService;
import kr.spring.kanban.vo.KanbanAssignVO;
import kr.spring.kanban.vo.KanbanCardVO;
import kr.spring.kanban.vo.KanbanChecklistVO;
import kr.spring.kanban.vo.KanbanCommentVO;
import kr.spring.team.service.TeamMemberService;
import kr.spring.team.vo.TeamMemberVO;
import kr.spring.users.vo.PrincipalDetails;
import lombok.extern.slf4j.Slf4j;


@RestController
@Slf4j
@RequestMapping("/kanban")
public class KanbanRestController {
	@Autowired
	private KanbanService kanbanService;
	@Autowired
	private TeamMemberService teamMemberService;
	
	// 팀번호 검증
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
	
	//카드조회
	@GetMapping("/board/{team_num}/cards")
	public List<KanbanCardVO> getCardList(
	        @PathVariable long team_num,
	        @RequestParam(value = "tag", required = false) String tag,
	        @RequestParam(value = "keyword", required = false) String keyword,
	        @RequestParam(value = "myOnly", required = false, defaultValue = "false") boolean myOnly,
	        @AuthenticationPrincipal PrincipalDetails principal,
	        HttpSession session) {
		
		long user_num = principal.getUsersVO().getUser_num();
		validateTeamMember(team_num, user_num, session);

	    Map<String, Object> map = new HashMap<>();
	    map.put("team_num", team_num);
	    map.put("tag", tag);
	    map.put("keyword", keyword);
	    map.put("myOnly", myOnly);

	    if (myOnly) {
	        map.put("user_num", principal.getUsersVO().getUser_num());
	    }

	    return kanbanService.selectKanbanCardList(map);
	}

	@PostMapping("/board/{team_num}/cards")
	public Map<String, Object> insertCard(@PathVariable long team_num, 
										  @Valid @ModelAttribute KanbanCardVO card,
										  BindingResult result, 
										  @AuthenticationPrincipal PrincipalDetails principal,
										  HttpServletRequest request, HttpSession session) throws IOException {
		
		long user_num = principal.getUsersVO().getUser_num();
		validateTeamMember(team_num, user_num, session);

		if (result.hasErrors()) {
			Map<String, Object> map = new HashMap<>();
			map.put("result", "fail");

			Map<String, String> errors = new HashMap<>();
			result.getFieldErrors().forEach(error -> {
				String field = error.getField();
				String message = error.getDefaultMessage();

				if ("deadline".equals(field)) {
					message = "마감일을 선택하세요.";
				}

				errors.put(field, message);
			});
			map.put("errors", errors);
			return map;
		}

		card.setTeam_num(team_num);
		card.setWriter_num(principal.getUsersVO().getUser_num());
		kanbanService.insertKanbanCard(card,request);

		return Map.of("result", "success");
	}
	
	/*=====================================================
	 * Http 상태코드 메서드 정의
	 * ===================================================*/
	private ResponseEntity<Map<String, Object>> ok(Map<String, Object> map) {
	    return new ResponseEntity<>(map, HttpStatus.OK);
	}

	private ResponseEntity<Map<String, Object>> badRequest(Map<String, Object> map) {
	    return new ResponseEntity<>(map, HttpStatus.BAD_REQUEST);
	}

	private ResponseEntity<Map<String, Object>> forbidden(Map<String, Object> map) {
	    return new ResponseEntity<>(map, HttpStatus.FORBIDDEN);
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/*=====================================================
	 * 칸반보드 상태변경 
	 * ===================================================*/
	@PostMapping("/board/{team_num}/card/{card_num}/status")
	public ResponseEntity<Map<String,Object>> updateKanbanStatus(@PathVariable("team_num") long team_num,
												 @PathVariable("card_num") long card_num,
												 @RequestParam("kanban_status") int kanban_status,
												 @AuthenticationPrincipal PrincipalDetails principal,
												 HttpSession session){
		long user_num = principal.getUsersVO().getUser_num();
		validateTeamMember(team_num, user_num, session);
		
		Map<String, Object> map = new HashMap<String, Object>();
		//상태값 검증
		if(kanban_status<1 || kanban_status >4) {
		    map.put("message", "올바르지 않은 상태값입니다.");
		    return badRequest(map);
		}
		//권한 확인
		if(!kanbanService.canChangeStatus(card_num, user_num, team_num)) {
			map.put("message", "작성자 또는 담당자만 상태를 변경할 수 있습니다.");
			return forbidden(map);
		}
		//상태 변경용 VO 생성
		KanbanCardVO card = new KanbanCardVO();
		card.setTeam_num(team_num);
		card.setCard_num(card_num);
		card.setKanban_status(kanban_status);
		//상태변경
		kanbanService.updateKanbanStatus(card);
		map.put("result", "success");		
		return ok(map);
	}
	/*=====================================================
	 * 칸반보드 수정처리
	 * ===================================================*/
	@PostMapping("/board/{team_num}/card/{card_num}/update")
	public ResponseEntity<Map<String, Object>> updateKanbanCard(@PathVariable("team_num") long team_num,
												@PathVariable("card_num") long card_num, 
												@Valid KanbanCardVO card, BindingResult result,
												@AuthenticationPrincipal PrincipalDetails principal,
												HttpServletRequest request, HttpSession session) throws IOException {
		
		long user_num = principal.getUsersVO().getUser_num();
		validateTeamMember(team_num, user_num, session);
		Map<String, Object> map = new HashMap<>();
		// 유효성 검사
		if (result.hasErrors()) {
			Map<String, String> errors = new HashMap<>();
			result.getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
			map.put("result", "validationFail");
			map.put("errors", errors);
			return badRequest(map);
		}
		// 수정 권한 체크
		if (!kanbanService.canChangeStatus(card_num, user_num, team_num)) {
			map.put("result", "fail");
			map.put("message", "작성자 또는 담당자만 카드를 수정할 수 있습니다.");
			return forbidden(map);
		}
		card.setTeam_num(team_num);
		card.setCard_num(card_num);
		kanbanService.updateKanbanCard(card,user_num, request);
		map.put("result", "success");
		return ok(map);
	}
	
	/*=====================================================
	 * 칸반보드 카드삭제
	 * ===================================================*/
	@PostMapping("/board/{team_num}/card/{card_num}/delete")
	public ResponseEntity<Map<String, Object>> deleteKanbanCard(@PathVariable("team_num") long team_num,
												@PathVariable("card_num") long card_num,
												@AuthenticationPrincipal PrincipalDetails principal,
												HttpSession session){
		long user_num = principal.getUsersVO().getUser_num();
		validateTeamMember(team_num, user_num, session);
		
		Map<String, Object> map =new HashMap<String, Object>();
		//삭제 권한 체크
		if(!kanbanService.canChangeStatus(card_num, user_num, team_num)) {
			map.put("message", "작성자 또는 담당자만 카드를 삭제할 수 있습니다.");
			return forbidden(map);
		}
		KanbanCardVO card = new KanbanCardVO();
		card.setTeam_num(team_num);
		card.setCard_num(card_num);
		
		kanbanService.deleteKanbanCard(card);
		
		map.put("result", "success");
		return ok(map);
	}
	
	/*==============================
	 * 체크리스트
	 * ===========================*/
	/*=====================================================
	 * 체크리스트 추가
	 * ===================================================*/
	@PostMapping("/board/{team_num}/card/{card_num}/checklist")
	public ResponseEntity<Map<String, Object>> insertKanbanChecklist(@PathVariable("team_num") long team_num,
													 @PathVariable("card_num") long card_num,
													 @RequestParam("content") String content,
													 @AuthenticationPrincipal PrincipalDetails principal,
													 HttpSession session){
		
		long user_num = principal.getUsersVO().getUser_num();
		validateTeamMember(team_num, user_num, session);
		
		Map<String,Object> map = new HashMap<String, Object>();
		//빈 내용 검증
		if(content==null||content.trim().isEmpty()) {
			map.put("message","체크리스트 내용을 입력하세요.");
			return badRequest(map);
		}
		//권한 체크
		//삭제 권한 체크
		if(!kanbanService.canChangeStatus(card_num, user_num, team_num)) {
			map.put("message", "작성자 또는 담당자만 작성할 수 있습니다.");
			return forbidden(map);
		}
		KanbanChecklistVO checklist = new KanbanChecklistVO();
		checklist.setCard_num(card_num);
		checklist.setContent(content.trim());
		kanbanService.insertKanbanChecklist(checklist);
		map.put("result","success");
		return ok(map);
	}
	/*=====================================================
	 * 체크리스트 완료/미완료 변경
	 * ===================================================*/
	@PostMapping("/board/{team_num}/card/{card_num}/checklist/{checklist_num}/checked")
	public ResponseEntity<Map<String, Object>> updateKanbanChecklistChecked(@PathVariable("team_num") long team_num,
															@PathVariable("card_num") long card_num,
															@PathVariable("checklist_num") long checklist_num,
															@RequestParam("checked") int checked,
															@AuthenticationPrincipal PrincipalDetails principal,
															HttpSession session){
		long user_num = principal.getUsersVO().getUser_num();
		validateTeamMember(team_num, user_num, session);
		
		Map<String, Object> map = new HashMap<String, Object>();
		if(checked != 1 && checked !=2) {
			map.put("message", "올바르지 않은 체크 상태입니다.");
			return badRequest(map);
		}
		// 권한 체크
		if (!kanbanService.canChangeStatus(card_num, user_num, team_num)) {
			map.put("message", "작성자 또는 담당자만 수정할 수 있습니다.");
			return forbidden(map);
		}
		KanbanChecklistVO checklist = new KanbanChecklistVO();
		checklist.setCard_num(card_num);
		checklist.setChecklist_num(checklist_num);
		checklist.setChecked(checked);
		kanbanService.updateKanbanChecklistChecked(checklist);
		map.put("result","success");
		return ok(map);
	}
	/*=====================================================
	 * 체크리스트 삭제
	 * ===================================================*/
	@PostMapping("/board/{team_num}/card/{card_num}/checklist/{checklist_num}/delete")
	public ResponseEntity<Map<String, Object>> deleteKanbanChecklist(@PathVariable("team_num") long team_num,
													 @PathVariable("card_num") long card_num,
													 @PathVariable("checklist_num") long checklist_num,
													 @AuthenticationPrincipal PrincipalDetails principal,
													 HttpSession session){
		
		long user_num = principal.getUsersVO().getUser_num();
		validateTeamMember(team_num, user_num, session);
		
		Map<String, Object> map = new HashMap<String, Object>();
		//권한체크
		if(!kanbanService.canChangeStatus(card_num, user_num, team_num)) {
			map.put("message","작성자 또는 담당자만 삭제할 수 있습니다.");
			return forbidden(map);
		}
		KanbanChecklistVO checklist = new KanbanChecklistVO();
		checklist.setCard_num(card_num);
		checklist.setChecklist_num(checklist_num);
		kanbanService.deleteKanbanChecklist(checklist);
		map.put("result","success");
		return ok(map);
	}
	
	/*=====================================================
	 * 댓글
	 * ===================================================*/
	/*=====================================================
	 * 댓글 목록 조회
	 * ===================================================*/
	@GetMapping("/board/{team_num}/card/{card_num}/comments")
	public ResponseEntity<List<KanbanCommentVO>> selectListComment(@PathVariable("team_num") long team_num,
																   @PathVariable("card_num") long card_num,
																   @AuthenticationPrincipal PrincipalDetails principal,
																   HttpSession session){
		long user_num = principal.getUsersVO().getUser_num();
		validateTeamMember(team_num, user_num, session);
		List<KanbanCommentVO> list = kanbanService.selectListComment(card_num);
		return ResponseEntity.ok(list);
	}
	
	/*=====================================================
	 * 댓글 등록
	 * ===================================================*/
	@PostMapping("/board/{team_num}/card/{card_num}/comments")
	public ResponseEntity<Map<String,Object>> insertComment(@PathVariable("team_num") long team_num,
															@PathVariable("card_num") long card_num,
															@RequestParam("content") String content,
															@AuthenticationPrincipal PrincipalDetails principal,
															HttpSession session){
		long user_num = principal.getUsersVO().getUser_num();
		validateTeamMember(team_num, user_num, session);
		
		Map<String,Object> map = new HashMap<String, Object>();
		
		if(content==null || content.trim().isEmpty()) {
			map.put("message","댓글 내용을 입력하세요.");
			return ResponseEntity.badRequest().body(map);
		}
		//권한 체크
		if(!kanbanService.canChangeStatus(card_num, user_num, team_num)) {
			map.put("message", "작성자 또는 담당자만 작성할 수 있습니다.");
			return forbidden(map);
		}
		KanbanCommentVO comment = new KanbanCommentVO();
		
		comment.setCard_num(card_num);
		comment.setUser_num(principal.getUsersVO().getUser_num());
		comment.setContent(content);
		
		kanbanService.insertComment(comment);
		return ResponseEntity.ok(Map.of("message", "댓글이 등록되었습니다."));
	}
	/*=====================================================
	 * 댓글 수정
	 * ===================================================*/
	@PostMapping("/board/{team_num}/card/{card_num}/comments/{comment_num}/update")
	public ResponseEntity<Map<String,Object>> updateComment(@PathVariable("team_num") long team_num,
															@PathVariable("card_num") long card_num,
															@PathVariable("comment_num") long comment_num,
															@RequestParam("content") String content,
															@AuthenticationPrincipal PrincipalDetails principal,
															HttpSession session){
		long user_num = principal.getUsersVO().getUser_num();
		validateTeamMember(team_num, user_num, session);

		Map<String,Object> map = new HashMap<String, Object>();
		
		if(content == null || content.trim().isEmpty()) {
			map.put("message","댓글 내용을 입력하세요.");
			return badRequest(map);
		}
		KanbanCommentVO dbComment = kanbanService.selectComment(comment_num);
		//권한 체크
		if(dbComment==null || dbComment.getUser_num() != principal.getUsersVO().getUser_num()) {
			map.put("message","본인이 작성한 댓글만 수정할 수 있습니다.");
			return forbidden(map);
		}
	    // 수정용 VO 생성
	    KanbanCommentVO comment = new KanbanCommentVO();
	    comment.setComment_num(comment_num);
	    comment.setContent(content.trim());

	    // 댓글 수정
	    kanbanService.updateComment(comment);

	    map.put("result", "success");
	    return ok(map);
	}
	
	/*=====================================================
	 * 댓글 삭제
	 * ===================================================*/
	@PostMapping("/board/{team_num}/card/{card_num}/comments/{comment_num}/delete")
	public ResponseEntity<Map<String,Object>> deleteComment(@PathVariable("team_num") long team_num,
															@PathVariable("card_num") long card_num,
															@PathVariable("comment_num") long comment_num,
															@AuthenticationPrincipal PrincipalDetails principal,
															HttpSession session){
		long user_num = principal.getUsersVO().getUser_num();
		validateTeamMember(team_num, user_num, session);
		
		Map<String,Object> map = new HashMap<String, Object>();
		
		KanbanCommentVO dbComment = kanbanService.selectComment(comment_num);
		
		if(dbComment==null || dbComment.getUser_num() != principal.getUsersVO().getUser_num()) {
			map.put("message","본인이 작성한 댓글만 삭제할 수 있습니다.");
			return forbidden(map);
		}
		
		kanbanService.deleteComment(comment_num);
		map.put("result", "success");
		return ok(map);
	}
	/*=====================================================
	 * 담당자 추가
	 * ===================================================*/
	@PostMapping("/board/{team_num}/card/{card_num}/assign")
	public ResponseEntity<Map<String,Object>> insertKanbanCardAssign(
	        @PathVariable("team_num") long team_num,
	        @PathVariable("card_num") long card_num,
	        @RequestParam("user_num") long user_num,
	        @AuthenticationPrincipal PrincipalDetails principal,
	        HttpSession session) {
		long login_user_num = principal.getUsersVO().getUser_num();
		validateTeamMember(team_num, login_user_num, session);

	    Map<String,Object> map = new HashMap<>();

	    if(!kanbanService.canChangeStatus(card_num, login_user_num, team_num)) {
	        map.put("message", "작성자 또는 담당자만 담당자를 추가할 수 있습니다.");
	        return forbidden(map);
	    }

	    KanbanAssignVO assign = new KanbanAssignVO();
	    assign.setTeam_num(team_num);
	    assign.setCard_num(card_num);
	    assign.setUser_num(user_num);

	    kanbanService.insertKanbanCardAssign(assign);

	    map.put("result", "success");
	    return ok(map);
	}
	
	/*=====================================================
	 * 담당자 삭제
	 * ===================================================*/
	@PostMapping("/board/{team_num}/card/{card_num}/assign/delete")
	public ResponseEntity<Map<String,Object>> deleteKanbanAssign(
	        @PathVariable("team_num") long team_num,
	        @PathVariable("card_num") long card_num,
	        @RequestParam("user_num") long user_num,
	        @AuthenticationPrincipal PrincipalDetails principal,
	        HttpSession session) {

		long login_user_num = principal.getUsersVO().getUser_num();

		validateTeamMember(team_num, login_user_num, session);		
		
	    Map<String,Object> map = new HashMap<>();


	    if(!kanbanService.canChangeStatus(card_num, login_user_num, team_num)) {
	        map.put("message", "작성자 또는 담당자만 담당자를 삭제할 수 있습니다.");
	        return forbidden(map);
	    }

	    KanbanAssignVO assign = new KanbanAssignVO();
	    assign.setCard_num(card_num);
	    assign.setUser_num(user_num);

	    kanbanService.deleteKanbanAssign(assign);

	    map.put("result", "success");
	    return ok(map);
	}
}















