package kr.spring.team.controller;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import kr.spring.chat.service.ChatService;
import kr.spring.chat.vo.ChatChannelVO;
import kr.spring.kanban.service.KanbanService;
import kr.spring.kanban.vo.KanbanCardVO;
import kr.spring.notice.service.NoticeService;
import kr.spring.notice.vo.NoticeVO;
import kr.spring.schedule.service.ScheduleService;
import kr.spring.schedule.vo.ScheduleVO;
import kr.spring.storage.service.StorageService;
import kr.spring.storage.vo.FileFolderVO;
import kr.spring.team.service.TeamMemberService;
import kr.spring.team.service.TeamService;
import kr.spring.team.vo.TeamMemberVO;
import kr.spring.team.vo.TeamVO;
import kr.spring.users.vo.PrincipalDetails;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/team")
public class TeamController {

	private static final int ROLE_LEADER = 3;

	private static final int TEAM_STATUS_ACTIVE = 1;

	// 색상 HEX 코드 검증 (#RRGGBB)
	private static final Pattern COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");

	@Autowired
	private TeamService teamService;

	@Autowired
	private TeamMemberService teamMemberService;

	@Autowired
	private ChatService chatService;

	@Autowired
	private NoticeService noticeService;

	@Autowired
	private KanbanService kanbanService;

	@Autowired
	private ScheduleService scheduleService;

	@Autowired
	private StorageService storageService;

	@GetMapping("/dashboard")
	public String dashboard(@AuthenticationPrincipal PrincipalDetails principal,
			HttpSession session, Model model) {
		model.addAttribute("currentMenu", "dashboard");

		Long teamNum = (Long) session.getAttribute("teamNum");
		if (teamNum == null) {
			return "redirect:/main/home";
		}

		// 팀원 현황: 팀장(3) > 매니저(2) > 팀원(1) 순, 최대 5명
		List<TeamMemberVO> members = teamMemberService.selectMembersByTeamNum(teamNum);
		List<TeamMemberVO> topMembers = members.stream()
				.sorted(java.util.Comparator.comparingInt(TeamMemberVO::getRole).reversed())
				.limit(5)
				.collect(Collectors.toList());

		model.addAttribute("topMembers", topMembers);
		model.addAttribute("memberTotal", members.size());

		// ── 통계 카드 ─────────────────────────────────────────
		// 1) 이번 달 일정 수: selectScheduleListByTeam → 이달 start_date 필터
		java.time.YearMonth thisMonth = java.time.YearMonth.now();
		List<ScheduleVO> allSchedules = scheduleService.selectScheduleListByTeam(teamNum);
		long thisMonthScheduleCount = allSchedules.stream()
				.filter(s -> s.getStart_date() != null
						&& s.getStart_date().startsWith(thisMonth.toString()))
				.count();

		// 2) 완료 칸반 카드 수: selectKanbanCardList(team_num) → kanban_status == 4
		java.util.Map<String, Object> kanbanParam = new java.util.HashMap<>();
		kanbanParam.put("team_num", teamNum);
		List<KanbanCardVO> allCards = kanbanService.selectKanbanCardList(kanbanParam);
		long doneKanbanCount = allCards.stream()
				.filter(c -> c.getKanban_status() == 4)
				.count();

		// 3) 총 채팅 수(채팅 채널 수): selectChannelList(user_num, team_num)
		long userNum = principal.getUsersVO().getUser_num();
		List<ChatChannelVO> channels = chatService.selectChannelList(userNum, teamNum);
		int totalChannelCount = channels.size();

		// 4) 활성 팀원 수: 이미 조회한 members 재사용
		int activeMemberCount = members.size();

		model.addAttribute("thisMonthScheduleCount", thisMonthScheduleCount);
		model.addAttribute("doneKanbanCount", doneKanbanCount);
		model.addAttribute("totalChannelCount", totalChannelCount);
		model.addAttribute("activeMemberCount", activeMemberCount);
		// ─────────────────────────────────────────────────────

		// 최근 공지 최대 3개 (고정 우선·최신순 - selectNoticesByTeam 쿼리 정렬 그대로 활용)
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		List<NoticeVO> recentNotices = noticeService.selectNoticesByTeam(teamNum)
				.stream()
				.limit(3)
				.collect(Collectors.toList());
		for (NoticeVO n : recentNotices) {
			if (n.getReg_date() != null) {
				n.setReg_date_str(sdf.format(n.getReg_date()));
			}
		}
		// NEW 뱃지 기준일: 오늘로부터 3일 전 (yyyy-MM-dd 문자열 비교용)
		String threeDaysAgo = sdf.format(new java.util.Date(System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000));

		model.addAttribute("recentNotices", recentNotices);
		model.addAttribute("threeDaysAgo",  threeDaysAgo);

		return "thviews/team/dashboard";
	}

	@GetMapping("/settings")
	public String settings(@AuthenticationPrincipal PrincipalDetails principal,
			HttpSession session, Model model) {
		model.addAttribute("currentMenu", "settings");

		Long teamNum = (Long) session.getAttribute("teamNum");
		if (teamNum == null) {
			return "redirect:/main/home";
		}

		long userNum = principal.getUsersVO().getUser_num();

		TeamMemberVO member = teamMemberService.selectMemberByTeamAndUser(teamNum, userNum);
		if (member == null) {
			return "redirect:/main/home";
		}

		TeamVO team = teamService.selectTeamByNum(teamNum);
		if (team == null) {
			return "redirect:/main/home";
		}

		boolean hasPhoto = team.getTeam_photo() != null && team.getTeam_photo().length > 0;
		team.setTeam_photo(null); // BLOB 바이트는 뷰로 넘기지 않음 (이미지는 /team/photo/{teamNum} 사용)

		model.addAttribute("team", team);
		model.addAttribute("hasPhoto", hasPhoto);
		model.addAttribute("isLeader", member.getRole() == ROLE_LEADER);
		model.addAttribute("isManagerUp", member.getRole() >= 2); // MANAGER 이상 (초대 관리 가능)
		model.addAttribute("members", teamMemberService.selectMembersByTeamNum(teamNum));
		model.addAttribute("myUserNum", userNum);

		return "thviews/team/settings";
	}

	/*============================================
	 * 팀 프로필 이미지 스트리밍
	 * - TEAM_PHOTO(BLOB)를 이미지로 응답
	 * - 이미지가 없으면 404 -> 프론트에서 기본 이미지 처리
	 *===========================================*/
	@GetMapping("/photo/{teamNum}")
	public ResponseEntity<byte[]> teamPhoto(@PathVariable long teamNum) {
		TeamVO team = teamService.selectTeamByNum(teamNum);
		if (team == null || team.getTeam_photo() == null || team.getTeam_photo().length == 0) {
			return ResponseEntity.notFound().build();
		}

		// 파일명 기반 content-type 추론 (실패 시 PNG 기본)
		MediaType contentType = MediaType.IMAGE_PNG;
		String photoName = team.getTeam_photo_name();
		if (photoName != null) {
			contentType = MediaTypeFactory.getMediaType(photoName).orElse(MediaType.IMAGE_PNG);
		}

		return ResponseEntity.ok()
				.contentType(contentType)
				.body(team.getTeam_photo());
	}

	/*============================================
	 * 팀 생성 (TM-001)
	 * - 생성자는 자동으로 LEADER 역할로 TEAM_MEMBER 등록
	 * - 팀명 중복 불가 (DB TEAM_NAME UK 제약과 별개로 사전 체크하여 친절한 에러 반환)
	 * - 팀 프로필 이미지는 선택 사항 (미업로드 시 기본 이미지로 처리 - 프론트에서 default 이미지 표시)
	 *===========================================*/
	@PostMapping("/create")
	@ResponseBody
	public String createTeam(@AuthenticationPrincipal PrincipalDetails principal,
			@RequestParam String teamName,
			@RequestParam(required = false) String description,
			@RequestParam String color,
			@RequestParam(value = "upload", required = false) MultipartFile upload) {

		// ---- 입력값 검증 ----
		if (teamName == null || teamName.trim().isEmpty()) {
			return "INVALID_NAME";
		}
		String trimmedName = teamName.trim();
		if (trimmedName.length() < 2 || trimmedName.length() > 30) {
			return "INVALID_NAME_LENGTH";
		}
		if (color == null || !COLOR_PATTERN.matcher(color.trim()).matches()) {
			return "INVALID_COLOR";
		}
		String trimmedDesc = (description == null) ? null : description.trim();
		if (trimmedDesc != null && trimmedDesc.length() > 200) {
			return "INVALID_DESCRIPTION_LENGTH";
		}

		// ---- 이름 중복 체크 (TM-001) ----
		if (teamService.selectTeamByName(trimmedName) != null) {
			return "DUPLICATE_NAME";
		}

		long creatorNum = principal.getUsersVO().getUser_num();

		TeamVO team = new TeamVO();
		team.setTeam_name(trimmedName);
		team.setDescription(trimmedDesc);
		team.setColor(color.trim());
		team.setCreator_num(creatorNum);

		// 팀 프로필 이미지 (선택) - 미업로드 시 team_photo는 null로 유지 -> 프론트에서 기본 이미지 처리
		if (upload != null && !upload.isEmpty()) {
			try {
				team.setUpload(upload);
			} catch (IOException e) {
				log.error("<<팀 프로필 이미지 업로드 실패>> : {}", e.toString());
				return "UPLOAD_FAIL";
			}
		}

		teamService.insertTeam(team);

		// 생성자를 LEADER로 TEAM_MEMBER 등록
		TeamMemberVO leader = new TeamMemberVO();
		leader.setTeam_num(team.getTeam_num());
		leader.setUser_num(creatorNum);
		leader.setRole(ROLE_LEADER);
		teamMemberService.insertTeamMember(leader);

		// 기본 채팅 채널 자동 개설 (TM-001)
		// 기본 채팅방 생성시 아래 규칙으로 생성됨
		/*
		 * 
		 * 1. 팀번호(teamNum)
		 * 2. 채널이름 : "일반"으로 자동 지정됨
		 * 3. 채팅채널 설명 : "팀 전체 채팅방"으로 자동 지정됨
		 * 4. 카테고리 : int값 1번 지정 (1:'일반') * 참고로 DB에서 category 타입은 VARCHAR2로 지정되어 있음
		 * 5. 기본 채널 여부 : "Y"로 자동 지정 됨
		 * 6. 채팅방 생성자 : '팀 생성자 번호' creatorNum == user_num 으로 자동 지정
		 * 
		 * 순서
		 * 1. 로그인된 사용자가 팀을 만들기를 선택 한다
		 * 2. 팀 이름, 설명, 색상을 지정하고 생성 버튼을 클릭
		 * 3. 현재 사용자의 이름과 팀 이름, 설명, 색상을 통해 팀을 만듬
		 * 4. 팀을 만드는 과정에서 팀 생성자는 권한을 '팀장'으로 지정한다.
		 * 5. 팀 생성 후 바로 채팅방도 생성한다.
		 * 6. 생성 후 바로 Dashboard로 넘어간다.
		 */
		ChatChannelVO defaultChannel = new ChatChannelVO();
		defaultChannel.setTeam_num(team.getTeam_num());
		defaultChannel.setChannel_name("일반");
		defaultChannel.setChannel_desc("팀 전체 채팅방");
		defaultChannel.setCategory(1);
		defaultChannel.setIs_default("Y");
		defaultChannel.setCreate_by(creatorNum);
		chatService.insertChannel(defaultChannel);

		// ---- 기본 보관함 폴더 자동 생성 (TM-001) ----
		// 1) 기본 채팅 채널명과 동일한 이름의 폴더 (채팅 모듈: 채널 생성 = 동일명 폴더 생성 규칙과 통일)
		// 2) 칸반보드 첨부파일용 '칸반보드' 폴더 (칸반 모듈 요구사항)
		// - 둘 다 최상위 폴더 (parent_folder_num = null -> 매퍼에서 jdbcType=NUMERIC 처리됨)
		FileFolderVO chatFolder = new FileFolderVO();
		chatFolder.setTeam_num(team.getTeam_num());
		chatFolder.setFolder_name(defaultChannel.getChannel_name()); // "일반" - 채널명과 항상 동기화
		storageService.insertFolder(chatFolder);

		FileFolderVO kanbanFolder = new FileFolderVO();
		kanbanFolder.setTeam_num(team.getTeam_num());
		kanbanFolder.setFolder_name("칸반보드");
		storageService.insertFolder(kanbanFolder);

		log.debug("<<팀 생성>> teamNum={}, creator={}", team.getTeam_num(), creatorNum);
		return "OK:" + team.getTeam_num();
	}

	/*============================================
	 * 팀 정보 수정 (TM-002)
	 * - 선행조건: 수행자가 해당 팀의 LEADER(3) 이면서 소속 이어야 함
	 * - 이름/설명/색상 수정, 팀 프로필 이미지 교체/제거/유지
	 * - 이름 중복 체크는 "본인 팀 제외" (수정 시 이름 그대로 두는 경우 통과)
	 * - 정상(STATUS=1) 팀만 수정 가능 (비활성/삭제 팀 차단)
	 *
	 * 이미지 처리 우선순위: 새 업로드(교체) > removePhoto(제거) > 유지
	 *===========================================*/
	@PostMapping("/update")
	@ResponseBody
	public String updateTeam(@AuthenticationPrincipal PrincipalDetails principal,
			@RequestParam long teamNum,
			@RequestParam String teamName,
			@RequestParam(required = false) String description,
			@RequestParam String color,
			@RequestParam(value = "upload", required = false) MultipartFile upload,
			@RequestParam(value = "removePhoto", required = false, defaultValue = "false") boolean removePhoto) {

		long userNum = principal.getUsersVO().getUser_num();

		// ---- 권한 체크: 해당 팀의 LEADER 인지 ----
		TeamMemberVO member = teamMemberService.selectMemberByTeamAndUser(teamNum, userNum);
		if (member == null || member.getRole() != ROLE_LEADER) {
			return "NO_AUTH";
		}

		// ---- 팀 존재 / 상태 체크 (정상 팀만 수정 가능) ----
		TeamVO current = teamService.selectTeamByNum(teamNum); // STATUS=3(삭제)는 여기서 걸러짐
		if (current == null) {
			return "TEAM_NOT_FOUND";
		}
		if (current.getStatus() != TEAM_STATUS_ACTIVE) { // 비활성(2) 등은 수정 불가
			return "TEAM_NOT_ACTIVE";
		}

		// ---- 입력값 검증 (createTeam과 동일 규칙) ----
		if (teamName == null || teamName.trim().isEmpty()) {
			return "INVALID_NAME";
		}
		String trimmedName = teamName.trim();
		if (trimmedName.length() < 2 || trimmedName.length() > 30) {
			return "INVALID_NAME_LENGTH";
		}
		if (color == null || !COLOR_PATTERN.matcher(color.trim()).matches()) {
			return "INVALID_COLOR";
		}
		String trimmedDesc = (description == null) ? null : description.trim();
		if (trimmedDesc != null && trimmedDesc.length() > 200) {
			return "INVALID_DESCRIPTION_LENGTH";
		}

		// ---- 이름 중복 체크 (본인 팀 제외) ----
		TeamVO dup = teamService.selectTeamByName(trimmedName);
		if (dup != null && dup.getTeam_num() != teamNum) {
			return "DUPLICATE_NAME";
		}

		// ---- 수정할 VO 구성 ----
		TeamVO team = new TeamVO();
		team.setTeam_num(teamNum);
		team.setTeam_name(trimmedName);
		team.setDescription(trimmedDesc);
		team.setColor(color.trim());

		// ---- 이미지 처리: 교체 > 제거 > 유지 ----
		if (upload != null && !upload.isEmpty()) {
			try {
				team.setUpload(upload); // team_photo / team_photo_name 세팅
			} catch (IOException e) {
				log.error("<<팀 프로필 이미지 업로드 실패>> : {}", e.toString());
				return "UPLOAD_FAIL";
			}
			team.setPhotoAction(1); // 교체
		} else if (removePhoto) {
			team.setPhotoAction(2); // 제거
		} else {
			team.setPhotoAction(0); // 유지
		}

		teamService.updateTeam(team);

		log.debug("<<팀 정보 수정>> teamNum={}, by userNum={}, photoAction={}",
				teamNum, userNum, team.getPhotoAction());
		return "OK";
	}

	/*============================================
	 * 팀 입장 (사용자 홈 -> 팀 클릭)
	 * - 해당 팀의 JOINED 멤버인지 확인 후 세션에 teamNum 저장
	 *===========================================*/
	@GetMapping("/enter/{teamNum}")
	public String enterTeam(@AuthenticationPrincipal PrincipalDetails principal,
			@PathVariable long teamNum,
			HttpSession session) {
		long userNum = principal.getUsersVO().getUser_num();

		TeamMemberVO member = teamMemberService.selectMemberByTeamAndUser(teamNum, userNum);
		if (member == null) {
			log.debug("<<팀 입장 거부>> 소속 아님: teamNum={}, userNum={}", teamNum, userNum);
			return "redirect:/main/home";
		}

		session.setAttribute("teamNum", teamNum);
		log.debug("<<팀 입장>> teamNum={}, userNum={}", teamNum, userNum);
		return "redirect:/team/dashboard";
	}
}