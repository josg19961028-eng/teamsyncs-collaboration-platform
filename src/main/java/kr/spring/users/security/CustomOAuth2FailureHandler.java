package kr.spring.users.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.support.SessionFlashMapManager;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CustomOAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {
        String errorCode = resolveErrorCode(exception);
        log.error("[OAuth2 Failure] errorCode={}, exception={}", errorCode, exception.toString());

        if (isGoogleLinkFailure(errorCode)) {
            FlashMap flashMap = new FlashMap();
            flashMap.put("profileError", resolveGoogleLinkMessage(errorCode));
            FlashMapManager flashMapManager = new SessionFlashMapManager();
            flashMapManager.saveOutputFlashMap(flashMap, request, response);

            getRedirectStrategy().sendRedirect(request, response, "/member/myPage");
            return;
        }

        FlashMap flashMap = new FlashMap();
        flashMap.put("error", "error_google");
        FlashMapManager flashMapManager = new SessionFlashMapManager();
        flashMapManager.saveOutputFlashMap(flashMap, request, response);

        setDefaultFailureUrl("/member/login");
        super.onAuthenticationFailure(request, response, exception);
    }

    private boolean isGoogleLinkFailure(String errorCode) {
        return "google_email_already_registered".equals(errorCode)
                || "google_account_already_linked".equals(errorCode)
                || "google_link_failed".equals(errorCode)
                || "invalid_google_link_session".equals(errorCode);
    }

    private String resolveGoogleLinkMessage(String errorCode) {
        if ("google_email_already_registered".equals(errorCode)) {
            return "이미 다른 계정으로 가입된 Google 계정입니다.";
        }
        if ("google_account_already_linked".equals(errorCode)) {
            return "이미 다른 계정에 연동된 Google 계정입니다.";
        }
        return "Google 연동에 실패했습니다. 다시 시도해주세요.";
    }

    private String resolveErrorCode(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof OAuth2AuthenticationException oauthException
                    && oauthException.getError() != null) {
                return oauthException.getError().getErrorCode();
            }
            current = current.getCause();
        }
        return exception.getMessage();
    }
}
