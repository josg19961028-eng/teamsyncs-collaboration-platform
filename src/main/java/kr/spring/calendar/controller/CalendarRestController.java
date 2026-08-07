package kr.spring.calendar.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import kr.spring.calendar.service.CalendarService;
import kr.spring.calendar.vo.CalendarEventVO;
import kr.spring.schedule.vo.ScheduleVO;
import kr.spring.users.vo.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/calendar")
public class CalendarRestController {

    private final CalendarService calendarService;

    /*
     * 팀 하나의 일정 목록
     */
    @GetMapping("/events")
    public List<CalendarEventVO> getCalendarEvents(
            @RequestParam("team_num") Long teamNum,
            @RequestParam("start") String startDate,
            @RequestParam("end") String endDate,
            @AuthenticationPrincipal PrincipalDetails principal) {

        if (principal == null
                || principal.getUsersVO() == null
                || !isValidCalendarPeriod(startDate, endDate)) {
            return List.of();
        }

        Long userNum =
                principal.getUsersVO()
                         .getUser_num();

        log.debug(
            "캘린더 일정 조회 team_num = {}, user_num = {}",
            teamNum,
            userNum
        );

        return calendarService
                .selectTeamCalendarEventList(
                        teamNum,
                        userNum,
                        startDate,
                        endDate
                );
    }

    /*
     * 현재 세션에 저장된 팀 일정
     */
    @GetMapping("/current-team-events")
    public List<CalendarEventVO> getCurrentTeamEvents(
            @RequestParam("start") String startDate,
            @RequestParam("end") String endDate,
            HttpSession session,
            @AuthenticationPrincipal PrincipalDetails principal) {

        if (principal == null
                || principal.getUsersVO() == null
                || !isValidCalendarPeriod(startDate, endDate)) {
            return List.of();
        }

        Object teamObj =
                session.getAttribute("teamNum");

        if (teamObj == null) {
            log.debug("세션에 teamNum 없음");
            return List.of();
        }

        Long teamNum =
                Long.valueOf(teamObj.toString());

        Long userNum =
                principal.getUsersVO()
                         .getUser_num();

        log.debug(
            "현재 팀 일정 조회 team_num = {}, user_num = {}",
            teamNum,
            userNum
        );

        return calendarService
                .selectTeamCalendarEventList(
                        teamNum,
                        userNum,
                        startDate,
                        endDate
                );
    }

    private boolean isValidCalendarPeriod(
            String startDate,
            String endDate) {

        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            return end.isAfter(start);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /*
     * 로그인한 사용자의 전체 팀 일정
     */
    @GetMapping("/my-events")
    public List<CalendarEventVO> getMyCalendarEvents(
            @AuthenticationPrincipal
            PrincipalDetails principal) {

        Long userNum =
                principal.getUsersVO()
                         .getUser_num();

        log.debug(
            "내 전체 팀 일정 조회 user_num = {}",
            userNum
        );

        return calendarService
                .selectMyScheduleEventList(userNum);
    }

    /*
     * 일정 등록
     */
    @PostMapping("/write")
    public Map<String, Object> writeSchedule(
            @RequestBody ScheduleVO scheduleVO,
            @AuthenticationPrincipal
            PrincipalDetails principal,
            HttpSession session) {

        Map<String, Object> result =
                new HashMap<>();

        if (principal == null
                || principal.getUsersVO() == null) {

            result.put(
                "result",
                "unauthorized"
            );

            result.put(
                "message",
                "로그인이 필요합니다."
            );

            return result;
        }

        /*
         * 홈 화면:
         * 요청 JSON의 team_num 사용
         *
         * 팀 대시보드:
         * 세션의 teamNum 사용
         */
        if (scheduleVO.getTeam_num() == null) {
            Object teamObj =
                    session.getAttribute("teamNum");

            if (teamObj == null) {
                result.put(
                    "result",
                    "noTeam"
                );

                result.put(
                    "message",
                    "현재 팀 정보를 확인할 수 없습니다."
                );

                return result;
            }

            scheduleVO.setTeam_num(
                Long.valueOf(
                    teamObj.toString()
                )
            );
        }

        if (scheduleVO.getTitle() == null
                || scheduleVO.getTitle().isBlank()
                || scheduleVO.getStart_date() == null
                || scheduleVO.getEnd_date() == null) {

            result.put(
                "result",
                "invalid"
            );

            result.put(
                "message",
                "제목과 일정 기간을 입력해주세요."
            );

            return result;
        }

        if (scheduleVO.getEnd_date()
                .compareTo(
                    scheduleVO.getStart_date()
                ) < 0) {

            result.put(
                "result",
                "invalid"
            );

            result.put(
                "message",
                "종료 일시는 시작 일시보다 빠를 수 없습니다."
            );

            return result;
        }

        Long userNum =
                principal.getUsersVO()
                         .getUser_num();

        int permissionCount =
                calendarService
                    .countScheduleWritePermission(
                        scheduleVO.getTeam_num(),
                        userNum
                    );

        if (permissionCount == 0) {
            result.put(
                "result",
                "forbidden"
            );

            result.put(
                "message",
                "팀장 또는 매니저만 일정을 등록할 수 있습니다."
            );

            return result;
        }

        scheduleVO.setUser_num(userNum);

        int insertCount =
                calendarService
                    .insertSchedule(scheduleVO);

        if (insertCount > 0) {
            result.put(
                "result",
                "success"
            );
        } else {
            result.put(
                "result",
                "fail"
            );

            result.put(
                "message",
                "일정 등록에 실패했습니다."
            );
        }

        return result;
    }

    /*
     * 일정 수정
     */
    @PostMapping("/update")
    public Map<String, Object> updateSchedule(
            @RequestBody ScheduleVO scheduleVO,
            @AuthenticationPrincipal
            PrincipalDetails principal) {

        Map<String, Object> result =
                new HashMap<>();

        if (principal == null
                || principal.getUsersVO() == null) {

            result.put(
                "result",
                "unauthorized"
            );

            result.put(
                "message",
                "로그인이 필요합니다."
            );

            return result;
        }

        if (scheduleVO.getSchedule_num() == null) {
            result.put(
                "result",
                "invalid"
            );

            result.put(
                "message",
                "일정 번호가 없습니다."
            );

            return result;
        }

        if (scheduleVO.getTitle() == null
                || scheduleVO.getTitle().isBlank()
                || scheduleVO.getStart_date() == null
                || scheduleVO.getEnd_date() == null) {

            result.put(
                "result",
                "invalid"
            );

            result.put(
                "message",
                "제목과 일정 기간을 입력해주세요."
            );

            return result;
        }

        if (scheduleVO.getAll_day() == null
                || (scheduleVO.getAll_day() != 1
                && scheduleVO.getAll_day() != 2)) {

            result.put(
                "result",
                "invalid"
            );

            result.put(
                "message",
                "일정 유형이 올바르지 않습니다."
            );

            return result;
        }

        if (scheduleVO.getEnd_date()
                .compareTo(
                    scheduleVO.getStart_date()
                ) < 0) {

            result.put(
                "result",
                "invalid"
            );

            result.put(
                "message",
                "종료 일시는 시작 일시보다 빠를 수 없습니다."
            );

            return result;
        }

        scheduleVO.setUser_num(
            principal.getUsersVO()
                     .getUser_num()
        );

        int updateCount =
                calendarService
                    .updateSchedule(scheduleVO);

        if (updateCount > 0) {
            result.put(
                "result",
                "success"
            );
        } else {
            result.put(
                "result",
                "forbidden"
            );

            result.put(
                "message",
                "수정 권한이 없거나 일정이 존재하지 않습니다."
            );
        }

        return result;
    }

    /*
     * 일정 삭제
     */
    @PostMapping("/delete")
    public Map<String, Object> deleteSchedule(
            @RequestBody ScheduleVO scheduleVO,
            @AuthenticationPrincipal
            PrincipalDetails principal) {

        Map<String, Object> result =
                new HashMap<>();

        if (principal == null
                || principal.getUsersVO() == null) {

            result.put(
                "result",
                "unauthorized"
            );

            result.put(
                "message",
                "로그인이 필요합니다."
            );

            return result;
        }

        if (scheduleVO.getSchedule_num() == null) {
            result.put(
                "result",
                "invalid"
            );

            result.put(
                "message",
                "일정 번호가 없습니다."
            );

            return result;
        }

        Long userNum =
                principal.getUsersVO()
                         .getUser_num();

        int deleteCount =
                calendarService
                    .deleteSchedule(
                        scheduleVO.getSchedule_num(),
                        userNum
                    );

        if (deleteCount > 0) {
            result.put(
                "result",
                "success"
            );
        } else {
            result.put(
                "result",
                "forbidden"
            );

            result.put(
                "message",
                "삭제 권한이 없거나 일정이 존재하지 않습니다."
            );
        }

        return result;
    }
}
