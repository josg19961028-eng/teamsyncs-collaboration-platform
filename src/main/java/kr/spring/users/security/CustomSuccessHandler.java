package kr.spring.users.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.support.SessionFlashMapManager;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.spring.users.vo.PrincipalDetails;
import kr.spring.users.vo.UserAuth;
import kr.spring.users.vo.UserStatus;
import kr.spring.users.vo.UsersVO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
// 인증(로그인)에 성공한 후, 리다이렉트할 URL을 지정하거나 처리 로직을 직접 작성할 때 사용
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        UsersVO user = ((PrincipalDetails) authentication.getPrincipal()).getUsersVO();
        log.debug("[Login Check 2] CustomSuccessHandler : " + user);

        if (user.getStatus() == UserStatus.SUSPENDED.getValue()) {
            // 정지 회원: 로그인 자체를 막고 로그아웃 처리
            log.debug("[Login Check 2] 정지회원 : " + user.getEmail());
            new SecurityContextLogoutHandler().logout(request, response, authentication);

            FlashMap flashMap = new FlashMap();
            flashMap.put("error", "error_suspended");
            FlashMapManager flashMapManager = new SessionFlashMapManager();
            flashMapManager.saveOutputFlashMap(flashMap, request, response);

            setDefaultTargetUrl("/member/login");
        } else if (Boolean.TRUE.equals(request.getSession().getAttribute(CustomOAuth2UserService.GOOGLE_LINK_SUCCESS))) {
            request.getSession().removeAttribute(CustomOAuth2UserService.GOOGLE_LINK_SUCCESS);

            FlashMap flashMap = new FlashMap();
            flashMap.put("profileSuccess", "Google 계정이 연동되었습니다.");
            FlashMapManager flashMapManager = new SessionFlashMapManager();
            flashMapManager.saveOutputFlashMap(flashMap, request, response);

            setDefaultTargetUrl("/member/myPage");
        } else if (user.getAuth().equals(UserAuth.ADMIN.getValue())) {
            // 관리자
            setDefaultTargetUrl("/admin/home");
        } else {
            // 일반 회원 - 대시보드로 이동
            setDefaultTargetUrl("/main/home");
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
