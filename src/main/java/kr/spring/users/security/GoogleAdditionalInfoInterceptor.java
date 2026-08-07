package kr.spring.users.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.spring.users.service.GoogleOAuthService;
import kr.spring.users.vo.PrincipalDetails;
import kr.spring.users.vo.UsersVO;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GoogleAdditionalInfoInterceptor implements HandlerInterceptor {

    private final GoogleOAuthService googleOAuthService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof PrincipalDetails principal)) {
            return true;
        }

        UsersVO user = principal.getUsersVO();
        if (!googleOAuthService.requiresAdditionalInfo(user)) {
            return true;
        }

        response.sendRedirect("/users/google/additional-info");
        return false;
    }
}
