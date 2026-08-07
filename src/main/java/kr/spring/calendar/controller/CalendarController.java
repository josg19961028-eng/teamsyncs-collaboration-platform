package kr.spring.calendar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/calendar")
public class CalendarController {

    /*
     * 사이드바 또는 버튼에서 캘린더로 이동
     */
    @GetMapping("/calendar")
    public String calendarFromSession(
            HttpSession session) {

        Object teamObj =
                session.getAttribute("teamNum");

        if (teamObj == null) {
            return "redirect:/main/home";
        }

        Long teamNum =
                Long.valueOf(teamObj.toString());

        return "redirect:/calendar/list?team_num="
                + teamNum;
    }

    /*
     * 큰 캘린더 화면
     */
    @GetMapping("/list")
    public String list(
            @RequestParam(
                value = "team_num",
                required = false
            )
            Long teamNum,
            HttpSession session,
            Model model) {

        if (teamNum == null) {
            Object teamObj =
                    session.getAttribute("teamNum");

            if (teamObj == null) {
                return "redirect:/main/home";
            }

            teamNum =
                    Long.valueOf(
                        teamObj.toString()
                    );
        }

        model.addAttribute(
            "currentMenu",
            "calendar"
        );

        model.addAttribute(
            "team_num",
            teamNum
        );

        return "thviews/calendar/list";
    }
}