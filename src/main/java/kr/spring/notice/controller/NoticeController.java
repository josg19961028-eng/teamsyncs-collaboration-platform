package kr.spring.notice.controller;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpSession;
import kr.spring.notice.service.NoticeService;
import kr.spring.notice.vo.NoticeVO;
import kr.spring.notification.service.NotificationService;
import kr.spring.notification.vo.NotificationVO;
import kr.spring.team.service.TeamMemberService;
import kr.spring.team.vo.TeamMemberVO;
import kr.spring.users.vo.PrincipalDetails;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/notice")
public class NoticeController {

    private static final int ROLE_MEMBER  = 1;
    private static final int ROLE_MANAGER = 2;
    private static final int ROLE_LEADER  = 3;

    // ThreadLocal-safe (SimpleDateFormat은 스레드 비안전 -> 매번 new 또는 ThreadLocal)
    private static final ThreadLocal<SimpleDateFormat> DATE_FMT =
        ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private TeamMemberService teamMemberService;

    @Autowired
    private NotificationService notificationService;

    /*============================================
     * 공지사항 목록 (화면 진입)
     *===========================================*/
    @GetMapping("/list")
    public String list(@AuthenticationPrincipal PrincipalDetails principal,
                       @RequestParam(value = "teamNum", required = false) Long teamNumParam,
                       HttpSession session, Model model) {

        long myUserNum = principal.getUsersVO().getUser_num();

        // 알림 링크 등으로 teamNum이 명시된 경우: 소속 확인 후 해당 팀으로 세션 전환
        // 세션 세팅 후 파라미터 없는 URL로 리다이렉트해야 함
        // (사이드바를 그리는 TeamSidebarAdvice의 @ModelAttribute가 컨트롤러보다 먼저 실행되므로,
        //  같은 요청 안에서 세션을 바꾸면 사이드바는 이전 팀으로 렌더링됨 -> 새 요청으로 갱신)
        if (teamNumParam != null) {
            TeamMemberVO paramMember = teamMemberService.selectMemberByTeamAndUser(teamNumParam, myUserNum);
            if (paramMember == null) return "redirect:/main/home"; // 소속 아님
            session.setAttribute("teamNum", teamNumParam);
            return "redirect:/notice/list";
        }

        Long teamNumAttr = (Long) session.getAttribute("teamNum");
        if (teamNumAttr == null) return "redirect:/main/home";

        long teamNum = teamNumAttr;

        TeamMemberVO me = teamMemberService.selectMemberByTeamAndUser(teamNum, myUserNum);
        if (me == null) return "redirect:/main/home";

        int myRole = me.getRole();

        List<NoticeVO> notices = noticeService.selectNoticesByTeam(teamNum);
        for (NoticeVO n : notices) {
            if (n.getReg_date() != null) {
                n.setReg_date_str(DATE_FMT.get().format(n.getReg_date()));
            }
        }

        model.addAttribute("currentMenu", "notice");
        model.addAttribute("notices",     notices);
        model.addAttribute("myRole",      myRole);
        model.addAttribute("myUserNum",   myUserNum);

        return "thviews/notice/list";
    }

    /*============================================
     * 공지 상세 (AJAX JSON + 조회수 증가)
     * - GET /notice/detail?noticeNum=X
     *===========================================*/
    @GetMapping("/detail")
    @ResponseBody
    public Map<String, Object> detail(@RequestParam long noticeNum,
                                      HttpSession session) {

        Map<String, Object> result = new HashMap<>();

        Long teamNumAttr = (Long) session.getAttribute("teamNum");
        if (teamNumAttr == null) {
            result.put("error", "NO_TEAM");
            return result;
        }

        // 조회수 먼저 올리고
        noticeService.updateViewCount(noticeNum);

        NoticeVO notice = noticeService.selectNoticeByNum(noticeNum);
        if (notice == null || notice.getTeam_num() != teamNumAttr) {
            result.put("error", "NOT_FOUND");
            return result;
        }

        result.put("notice_num",  notice.getNotice_num());
        result.put("team_num",    notice.getTeam_num());
        result.put("user_num",    notice.getUser_num());
        result.put("title",       notice.getTitle());
        result.put("content",     notice.getContent());
        result.put("is_fixed",    notice.getIs_fixed());
        result.put("view_count",  notice.getView_count());
        result.put("writer_name", notice.getWriter_name());
        result.put("reg_date_str",
            notice.getReg_date() != null ? DATE_FMT.get().format(notice.getReg_date()) : "");

        return result;
    }

    /*============================================
     * 공지 작성 (LEADER/MANAGER)
     * - POST /notice/write
     *===========================================*/
    @PostMapping("/write")
    @ResponseBody
    public String write(@AuthenticationPrincipal PrincipalDetails principal,
                        HttpSession session,
                        @RequestParam String title,
                        @RequestParam String content,
                        @RequestParam(defaultValue = "N") String isFixed) {

        Long teamNumAttr = (Long) session.getAttribute("teamNum");
        if (teamNumAttr == null) return "NO_TEAM";
        long teamNum   = teamNumAttr;
        long myUserNum = principal.getUsersVO().getUser_num();

        // 권한 체크: MANAGER(2) 이상만 작성 가능
        TeamMemberVO me = teamMemberService.selectMemberByTeamAndUser(teamNum, myUserNum);
        if (me == null || me.getRole() < ROLE_MANAGER) return "NO_PERMISSION";

        // 입력값 검증
        if (title == null || title.trim().isEmpty())     return "INVALID_TITLE";
        if (content == null || content.trim().isEmpty()) return "INVALID_CONTENT";
        if (title.trim().length() > 60)                  return "TITLE_TOO_LONG";

        NoticeVO notice = new NoticeVO();
        notice.setTeam_num(teamNum);
        notice.setUser_num(myUserNum);
        notice.setTitle(title.trim());
        notice.setContent(content.trim());
        notice.setIs_fixed("Y".equals(isFixed) ? "Y" : "N");

        noticeService.insertNotice(notice);

        // 알림: 작성자 제외 팀원 전원에게 (noti_type 5 = 공지)
        try {
            List<TeamMemberVO> members = teamMemberService.selectMembersByTeamNum(teamNum);
            String writerName = principal.getUsersVO().getUser_name();
            for (TeamMemberVO m : members) {
                if (m.getUser_num() == myUserNum) continue; // 본인 제외
                NotificationVO noti = new NotificationVO();
                noti.setSender_num(myUserNum);
                noti.setReceiver_num(m.getUser_num());
                noti.setTeam_num(teamNum);
                noti.setNoti_type(5);
                noti.setTitle("새 공지사항");
                noti.setContent(writerName + "님이 공지를 등록했습니다: " + notice.getTitle());
                noti.setLink("/notice/list?teamNum=" + teamNum);
                notificationService.addNotification(noti);
            }
        } catch (Exception e) {
            log.warn("<<공지 알림 발송 실패>> {}", e.getMessage());
        }

        log.debug("<<공지 작성>> teamNum={}, userNum={}", teamNum, myUserNum);
        return "OK";
    }

    /*============================================
     * 공지 수정 (LEADER/MANAGER 또는 작성자 본인)
     * - POST /notice/update
     *===========================================*/
    @PostMapping("/update")
    @ResponseBody
    public String update(@AuthenticationPrincipal PrincipalDetails principal,
                         HttpSession session,
                         @RequestParam long noticeNum,
                         @RequestParam String title,
                         @RequestParam String content,
                         @RequestParam(defaultValue = "N") String isFixed) {

        Long teamNumAttr = (Long) session.getAttribute("teamNum");
        if (teamNumAttr == null) return "NO_TEAM";
        long teamNum   = teamNumAttr;
        long myUserNum = principal.getUsersVO().getUser_num();

        // 공지 존재 & 팀 소속 확인
        NoticeVO existing = noticeService.selectNoticeByNum(noticeNum);
        if (existing == null || existing.getTeam_num() != teamNum) return "NOT_FOUND";

        // 권한 체크: MANAGER 이상 OR 작성자 본인
        TeamMemberVO me = teamMemberService.selectMemberByTeamAndUser(teamNum, myUserNum);
        int myRole = (me != null) ? me.getRole() : ROLE_MEMBER;
        if (myRole < ROLE_MANAGER && existing.getUser_num() != myUserNum) return "NO_PERMISSION";

        // 입력값 검증
        if (title == null || title.trim().isEmpty())     return "INVALID_TITLE";
        if (content == null || content.trim().isEmpty()) return "INVALID_CONTENT";
        if (title.trim().length() > 60)                  return "TITLE_TOO_LONG";

        NoticeVO update = new NoticeVO();
        update.setNotice_num(noticeNum);
        update.setTitle(title.trim());
        update.setContent(content.trim());
        update.setIs_fixed("Y".equals(isFixed) ? "Y" : "N");

        noticeService.updateNotice(update);
        log.debug("<<공지 수정>> noticeNum={}, by userNum={}", noticeNum, myUserNum);
        return "OK";
    }

    /*============================================
     * 공지 삭제 (LEADER/MANAGER 또는 작성자 본인)
     * - POST /notice/delete
     *===========================================*/
    @PostMapping("/delete")
    @ResponseBody
    public String delete(@AuthenticationPrincipal PrincipalDetails principal,
                         HttpSession session,
                         @RequestParam long noticeNum) {

        Long teamNumAttr = (Long) session.getAttribute("teamNum");
        if (teamNumAttr == null) return "NO_TEAM";
        long teamNum   = teamNumAttr;
        long myUserNum = principal.getUsersVO().getUser_num();

        NoticeVO existing = noticeService.selectNoticeByNum(noticeNum);
        if (existing == null || existing.getTeam_num() != teamNum) return "NOT_FOUND";

        TeamMemberVO me = teamMemberService.selectMemberByTeamAndUser(teamNum, myUserNum);
        int myRole = (me != null) ? me.getRole() : ROLE_MEMBER;
        if (myRole < ROLE_MANAGER && existing.getUser_num() != myUserNum) return "NO_PERMISSION";

        noticeService.deleteNotice(noticeNum);
        log.debug("<<공지 삭제>> noticeNum={}, by userNum={}", noticeNum, myUserNum);
        return "OK";
    }

    /*============================================
     * 고정 토글 (LEADER/MANAGER만)
     * - POST /notice/pin
     *===========================================*/
    @PostMapping("/pin")
    @ResponseBody
    public String pin(@AuthenticationPrincipal PrincipalDetails principal,
                      HttpSession session,
                      @RequestParam long noticeNum) {

        Long teamNumAttr = (Long) session.getAttribute("teamNum");
        if (teamNumAttr == null) return "NO_TEAM";
        long teamNum   = teamNumAttr;
        long myUserNum = principal.getUsersVO().getUser_num();

        NoticeVO existing = noticeService.selectNoticeByNum(noticeNum);
        if (existing == null || existing.getTeam_num() != teamNum) return "NOT_FOUND";

        TeamMemberVO me = teamMemberService.selectMemberByTeamAndUser(teamNum, myUserNum);
        if (me == null || me.getRole() < ROLE_MANAGER) return "NO_PERMISSION";

        noticeService.togglePin(noticeNum);
        log.debug("<<공지 고정 토글>> noticeNum={}, by userNum={}", noticeNum, myUserNum);
        return "OK";
    }
}