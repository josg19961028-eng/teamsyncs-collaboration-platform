package kr.spring.users.security;

import java.io.IOException;

import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 로그인 성공 후 처리를 CustomSuccessHandler(원본, 무변경)에 위임하되,
 * "로그인 전 원래 가려던 URL"이 이메일 초대 수락/거절 링크 또는 초대 링크(TM-004)인 경우에만
 * 그 링크로 그대로 복귀시킨다. 그 외 모든 경우(admin 포함)는 100% 기존 동작 그대로.
 *
 * @Primary로 지정해서 SecurityConfig가 AuthenticationSuccessHandler를 주입받을 때
 * 이 클래스가 선택되도록 함. SecurityConfig 코드 자체는 수정할 필요 없음.
 */
@Slf4j
@Component
@Primary
public class InviteReturnAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	private final RequestCache requestCache = new HttpSessionRequestCache();
	private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
	private final CustomSuccessHandler defaultSuccessHandler;

	public InviteReturnAuthenticationSuccessHandler(CustomSuccessHandler defaultSuccessHandler) {
		this.defaultSuccessHandler = defaultSuccessHandler;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {

		SavedRequest saved = requestCache.getRequest(request, response);

		if (saved != null && isInviteUrl(saved.getRedirectUrl())) {
			log.debug("<<초대 링크 복귀>> {}", saved.getRedirectUrl());
			requestCache.removeRequest(request, response);
			redirectStrategy.sendRedirect(request, response, saved.getRedirectUrl());
			return;
		}

		// 초대 링크로의 복귀가 아니면 기존 CustomSuccessHandler 로직 그대로 사용
		defaultSuccessHandler.onAuthenticationSuccess(request, response, authentication);
	}

	private boolean isInviteUrl(String url) {
		return url != null
				&& (url.contains("/team/invite/email/accept")
				 || url.contains("/team/invite/email/reject")
				 || url.contains("/team/invite/email/view")   // 이메일 초대 확인 랜딩 (신규)
				 || url.contains("/team/invite/link"));        // 초대 링크(TM-004) 복귀
	}
}