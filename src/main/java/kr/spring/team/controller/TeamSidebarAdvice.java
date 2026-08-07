package kr.spring.team.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpSession;
import kr.spring.team.service.TeamService;

/**
 * 팀 사이드바 공통 데이터 주입.
 * - 세션에 teamNum이 있을 때만 동작하며, 모든 뷰 모델에 팀 이름/색상/이미지여부/teamNum을 넣어준다.
 * - BLOB(TEAM_PHOTO)은 조회하지 않고 존재 여부(HAS_PHOTO)만 가져오는 경량 쿼리를 사용한다.
 *   (실제 이미지는 사이드바에서 GET /team/photo/{teamNum} 로 스트리밍)
 */
@ControllerAdvice
public class TeamSidebarAdvice {

    @Autowired
    private TeamService teamService;

    @ModelAttribute
    public void addSidebarTeamInfo(HttpSession session, Model model) {
        Object attr = session.getAttribute("teamNum");
        if (attr == null) {
            return; // 팀 컨텍스트가 없는 페이지(로그인/랜딩 등)는 스킵
        }
        long teamNum = (Long) attr;

        Map<String, Object> head = teamService.selectTeamSidebar(teamNum);
        if (head == null) {
            return; // 삭제되었거나 존재하지 않는 팀
        }

        Object hasPhoto = head.get("HAS_PHOTO");

        model.addAttribute("teamNum", teamNum);
        model.addAttribute("teamName", head.get("TEAM_NAME"));
        model.addAttribute("teamColor", head.get("COLOR"));
        model.addAttribute("teamHasPhoto",
                hasPhoto != null && ((Number) hasPhoto).intValue() == 1);
    }
}