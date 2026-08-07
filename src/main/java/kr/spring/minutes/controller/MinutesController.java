package kr.spring.minutes.controller;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import kr.spring.minutes.service.MeetingMinutesService;
import kr.spring.minutes.vo.MeetingMinutesAttendeeVO;
import kr.spring.minutes.vo.MeetingMinutesVO;
import kr.spring.schedule.service.ScheduleService;
import kr.spring.schedule.vo.ScheduleVO;
import kr.spring.team.service.TeamMemberService;
import kr.spring.team.vo.TeamMemberVO;
import kr.spring.users.vo.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/minutes")
@RequiredArgsConstructor
public class MinutesController {

	private final MeetingMinutesService meetingMinutesService;
	private final ScheduleService scheduleService;
	private final TeamMemberService teamMemberService;

    /*
     * 회의록 목록 및 선택된 회의록 상세
     *
     * /minutes/list
     * /minutes/list?minutes_num=1
     */
    @GetMapping("/list")
    public String list(
            @RequestParam(required = false) Long minutes_num,
            HttpSession session,
            Model model) {

        Long teamNum = getCurrentTeamNum(session);

        List<MeetingMinutesVO> minutesList =
                meetingMinutesService
                        .selectMeetingMinutesListByTeam(teamNum);

        model.addAttribute("currentMenu", "minutes");
        model.addAttribute("teamNum", teamNum);
        model.addAttribute("minutesList", minutesList);

        /*
         * 회의록 번호가 없으면 목록의 첫 번째 회의록을 선택
         */
        if (minutes_num == null
                && minutesList != null
                && !minutesList.isEmpty()) {

            minutes_num =
                    minutesList.get(0).getMinutes_num();
        }

        /*
         * 선택된 회의록 상세 조회
         */
        if (minutes_num != null) {

            MeetingMinutesVO minutes =
                    meetingMinutesService
                            .selectMeetingMinutes(minutes_num);

            validateMinutesTeam(minutes, teamNum);

            List<MeetingMinutesAttendeeVO> attendeeList =
                    meetingMinutesService
                            .selectMeetingMinutesAttendeeList(
                                    minutes_num);

            model.addAttribute("minutes", minutes);
            model.addAttribute("attendeeList", attendeeList);
        }

        return "thviews/minutes/list";
    }


    /*
     * 회의록 상세 주소
     *
     * 상세 페이지가 별도 페이지가 아니라 목록 오른쪽에 나오므로
     * 목록 주소로 이동시킨다.
     */
    @GetMapping("/detail")
    public String detail(
            @RequestParam Long minutes_num) {

        return "redirect:/minutes/list?minutes_num="
                + minutes_num;
    }


    /*
     * 회의록 작성 폼
     */
    @GetMapping("/write")
    public String writeForm(
            @AuthenticationPrincipal PrincipalDetails principal,
            HttpSession session,
            Model model) {

        Long teamNum = getCurrentTeamNum(session);
        Long userNum = principal.getUsersVO().getUser_num();

        MeetingMinutesVO minutesVO = new MeetingMinutesVO();

        minutesVO.setTeam_num(teamNum);
        minutesVO.setMeeting_date(new Date());

        // 작성자를 기본 참석자로 선택
        minutesVO.setAttendeeNums(List.of(userNum));

        // 현재 팀의 일정 목록
        List<ScheduleVO> scheduleList =
                scheduleService.selectScheduleListByTeam(teamNum);

        // 현재 팀의 활성 팀원 목록
        List<TeamMemberVO> teamMemberList =
                teamMemberService.selectMembersByTeamNum(teamNum);

        model.addAttribute("currentMenu", "minutes");
        model.addAttribute("teamNum", teamNum);
        model.addAttribute("minutesVO", minutesVO);

        model.addAttribute("scheduleList", scheduleList);
        model.addAttribute("teamMemberList", teamMemberList);

        // 왼쪽 회의록 목록
        model.addAttribute(
                "minutesList",
                meetingMinutesService
                        .selectMeetingMinutesListByTeam(teamNum));

        return "thviews/minutes/write";
    }


    /*
     * 회의록 등록
     */
    @PostMapping("/write")
    public String write(
            @ModelAttribute MeetingMinutesVO minutesVO,
            @AuthenticationPrincipal
            PrincipalDetails principal,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Long teamNum = getCurrentTeamNum(session);
        Long userNum =
                principal.getUsersVO().getUser_num();

        /*
         * team_num과 writer_num은 브라우저 입력값을 신뢰하지 않고
         * 서버에서 직접 설정
         */
        minutesVO.setTeam_num(teamNum);
        minutesVO.setWriter_num(userNum);
        
        if (minutesVO.getSchedule_num() != null) {

            ScheduleVO schedule =
                    scheduleService.selectSchedule(
                            minutesVO.getSchedule_num());

            if (schedule == null
                    || !Objects.equals(
                            schedule.getTeam_num(),
                            teamNum)) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "현재 팀의 일정이 아닙니다.");
            }
        }

        meetingMinutesService
                .insertMeetingMinutes(minutesVO);

        redirectAttributes.addFlashAttribute(
                "result",
                "minutesWriteSuccess");

        return "redirect:/minutes/list?minutes_num="
                + minutesVO.getMinutes_num();
    }


    /*
     * 회의록 수정 폼
     */
    @GetMapping("/modify")
    public String modifyForm(
            @RequestParam Long minutes_num,
            @AuthenticationPrincipal PrincipalDetails principal,
            HttpSession session,
            Model model) {

        Long teamNum = getCurrentTeamNum(session);
        Long userNum = principal.getUsersVO().getUser_num();

        MeetingMinutesVO minutesVO =
                meetingMinutesService.selectMeetingMinutes(minutes_num);

        // 현재 팀의 회의록인지 확인
        validateMinutesTeam(minutesVO, teamNum);

        // 작성자만 수정 가능
        validateWriter(minutesVO, userNum);

        // 기존 참석자 목록
        List<MeetingMinutesAttendeeVO> attendeeList =
                meetingMinutesService
                        .selectMeetingMinutesAttendeeList(minutes_num);

        // 기존 참석자의 user_num만 추출
        List<Long> selectedAttendeeNums =
                attendeeList.stream()
                        .map(MeetingMinutesAttendeeVO::getUser_num)
                        .toList();

        // VO에 기존 참석자 번호 저장
        minutesVO.setAttendeeNums(selectedAttendeeNums);

        // 현재 팀 일정 목록
        List<ScheduleVO> scheduleList =
                scheduleService.selectScheduleListByTeam(teamNum);

        // 현재 팀원 목록
        List<TeamMemberVO> teamMemberList =
                teamMemberService.selectMembersByTeamNum(teamNum);

        model.addAttribute("currentMenu", "minutes");
        model.addAttribute("teamNum", teamNum);
        model.addAttribute("minutesVO", minutesVO);

        model.addAttribute("scheduleList", scheduleList);
        model.addAttribute("teamMemberList", teamMemberList);

        model.addAttribute(
                "minutesList",
                meetingMinutesService
                        .selectMeetingMinutesListByTeam(teamNum));

        return "thviews/minutes/modify";
    }


    /*
     * 회의록 수정 처리
     */
    @PostMapping("/update")
    public String update(
            @ModelAttribute MeetingMinutesVO minutesVO,
            @AuthenticationPrincipal PrincipalDetails principal,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Long teamNum = getCurrentTeamNum(session);
        Long userNum = principal.getUsersVO().getUser_num();

        MeetingMinutesVO originalMinutes =
                meetingMinutesService.selectMeetingMinutes(
                        minutesVO.getMinutes_num());

        validateMinutesTeam(originalMinutes, teamNum);
        validateWriter(originalMinutes, userNum);

        /*
         * 연결 일정 검증
         */
        if (minutesVO.getSchedule_num() != null) {

            ScheduleVO schedule =
                    scheduleService.selectSchedule(
                            minutesVO.getSchedule_num());

            if (schedule == null
                    || !Objects.equals(
                            schedule.getTeam_num(),
                            teamNum)) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "현재 팀의 일정이 아닙니다.");
            }
        }

        /*
         * 참석자 필수 검증
         */
        if (minutesVO.getAttendeeNums() == null
                || minutesVO.getAttendeeNums().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "참석자를 한 명 이상 선택해주세요.");
        }

        /*
         * 참석자가 현재 팀원인지 검증
         */
        for (Long attendeeNum : minutesVO.getAttendeeNums()) {

            TeamMemberVO teamMember =
                    teamMemberService.selectMemberByTeamAndUser(
                            teamNum,
                            attendeeNum);

            if (teamMember == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "현재 팀에 소속되지 않은 참석자가 포함되어 있습니다.");
            }
        }

        /*
         * 변경하면 안 되는 값은 기존 DB 값 유지
         */
        minutesVO.setTeam_num(originalMinutes.getTeam_num());
        minutesVO.setWriter_num(originalMinutes.getWriter_num());

        meetingMinutesService.updateMeetingMinutes(minutesVO);

        redirectAttributes.addFlashAttribute(
                "result",
                "minutesUpdateSuccess");

        return "redirect:/minutes/list?minutes_num="
                + minutesVO.getMinutes_num();
    }


    /*
     * 회의록 논리 삭제
     */
    @PostMapping("/delete")
    public String delete(
            @RequestParam Long minutes_num,
            @AuthenticationPrincipal
            PrincipalDetails principal,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Long teamNum = getCurrentTeamNum(session);
        Long userNum =
                principal.getUsersVO().getUser_num();

        MeetingMinutesVO minutes =
                meetingMinutesService
                        .selectMeetingMinutes(minutes_num);

        validateMinutesTeam(minutes, teamNum);
        validateWriter(minutes, userNum);

        meetingMinutesService
                .deleteMeetingMinutes(minutes_num);

        redirectAttributes.addFlashAttribute(
                "result",
                "minutesDeleteSuccess");

        return "redirect:/minutes/list";
    }


    /*
     * PDF 저장을 위한 인쇄 전용 페이지
     */
    @GetMapping("/print")
    public String print(
            @RequestParam Long minutes_num,
            HttpSession session,
            Model model) {

        Long teamNum = getCurrentTeamNum(session);

        MeetingMinutesVO minutes =
                meetingMinutesService
                        .selectMeetingMinutes(minutes_num);

        validateMinutesTeam(minutes, teamNum);

        List<MeetingMinutesAttendeeVO> attendeeList =
                meetingMinutesService
                        .selectMeetingMinutesAttendeeList(
                                minutes_num);

        model.addAttribute("minutes", minutes);
        model.addAttribute("attendeeList", attendeeList);

        return "thviews/minutes/print";
    }


    /*
     * 현재 선택된 팀 번호 조회
     *
     * 팀 진입 시 사용하는 실제 세션 키에 맞게 수정할 것
     */
    private Long getCurrentTeamNum(
            HttpSession session) {

        Object value =
                session.getAttribute("teamNum");

        /*
         * 프로젝트에서 team_num이라는 이름을 쓰는 경우도 대응
         */
        if (value == null) {
            value = session.getAttribute("team_num");
        }

        if (value == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "선택된 팀 정보가 없습니다.");
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        try {
            return Long.valueOf(value.toString());

        } catch (NumberFormatException e) {

            log.error(
                    "잘못된 팀 번호입니다. value={}",
                    value,
                    e);

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "팀 번호 형식이 올바르지 않습니다.");
        }
    }


    /*
     * 요청한 회의록이 현재 팀 소속인지 검사
     */
    private void validateMinutesTeam(
            MeetingMinutesVO minutes,
            Long teamNum) {

        if (minutes == null
                || !Objects.equals(
                        minutes.getTeam_num(),
                        teamNum)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "회의록을 찾을 수 없습니다.");
        }
    }


    /*
     * 수정·삭제 권한 검사
     *
     * 현재는 작성자만 수정·삭제 가능
     */
    private void validateWriter(
            MeetingMinutesVO minutes,
            Long userNum) {

        if (!Objects.equals(
                minutes.getWriter_num(),
                userNum)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "회의록을 수정하거나 삭제할 권한이 없습니다.");
        }
    }
}