package kr.spring.users.security;

import java.util.Map;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpSession;
import kr.spring.users.service.GoogleOAuthService;
import kr.spring.users.vo.PrincipalDetails;
import kr.spring.users.vo.UserStatus;
import kr.spring.users.vo.UsersVO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    public static final String GOOGLE_LINK_USER_NUM = "GOOGLE_LINK_USER_NUM";
    public static final String GOOGLE_LINK_EMAIL = "GOOGLE_LINK_EMAIL";
    public static final String GOOGLE_LINK_SUCCESS = "GOOGLE_LINK_SUCCESS";

    private final GoogleOAuthService googleOAuthService;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = delegate.loadUser(userRequest);
        Map<String, Object> attributes = oauthUser.getAttributes();

        String googleId = stringValue(attributes.get("sub"));
        String email = stringValue(attributes.get("email"));
        String name = stringValue(attributes.get("name"));

        if (googleId == null || email == null) {
            throw new OAuth2AuthenticationException("Google account information is missing.");
        }

        UsersVO linkedUser = handleGoogleLink(googleId, email, attributes);
        if (linkedUser != null) {
            return new PrincipalDetails(linkedUser, attributes);
        }

        UsersVO user = googleOAuthService.findByGoogleId(googleId);
        if (user == null) {
            UsersVO emailUser = googleOAuthService.findByEmail(email);
            if (emailUser != null) {
                throw new OAuth2AuthenticationException("already_registered_email");
            }
            user = googleOAuthService.createGoogleUser(googleId, email, name);
        }

        if (user == null || user.getStatus() == UserStatus.WITHDRAWN.getValue()) {
            throw new OAuth2AuthenticationException("UserNotFound");
        }

        if (user.getStatus() == UserStatus.SUSPENDED.getValue()) {
            throw new DisabledException("Suspended user");
        }

        return new PrincipalDetails(user, attributes);
    }

    private UsersVO handleGoogleLink(String googleId, String googleEmail, Map<String, Object> attributes) {
        HttpSession session = currentSession();
        if (session == null) {
            return null;
        }

        Object linkUserNumValue = session.getAttribute(GOOGLE_LINK_USER_NUM);
        String linkEmail = stringValue(session.getAttribute(GOOGLE_LINK_EMAIL));
        if (linkUserNumValue == null || linkEmail == null) {
            return null;
        }

        long linkUserNum = toLong(linkUserNumValue);
        clearGoogleLinkSession(session);

        if (linkUserNum < 1) {
            throw new OAuth2AuthenticationException("invalid_google_link_session");
        }

        UsersVO currentUser = googleOAuthService.findByUserNum(linkUserNum);
        if (currentUser == null || currentUser.getStatus() == UserStatus.WITHDRAWN.getValue()) {
            throw new OAuth2AuthenticationException("UserNotFound");
        }

        if (currentUser.getStatus() == UserStatus.SUSPENDED.getValue()) {
            throw new DisabledException("Suspended user");
        }

        UsersVO emailOwner = googleOAuthService.findByEmail(googleEmail);
        if (emailOwner != null && emailOwner.getUser_num() != linkUserNum) {
            throw new OAuth2AuthenticationException("google_email_already_registered");
        }

        UsersVO googleOwner = googleOAuthService.findByGoogleId(googleId);
        if (googleOwner != null && googleOwner.getUser_num() != linkUserNum) {
            throw new OAuth2AuthenticationException("google_account_already_linked");
        }

        if (googleOwner != null) {
            session.setAttribute(GOOGLE_LINK_SUCCESS, true);
            return googleOwner;
        }

        UsersVO updatedUser = googleOAuthService.linkGoogleAccount(linkUserNum, googleId);
        if (updatedUser == null) {
            throw new OAuth2AuthenticationException("google_link_failed");
        }

        session.setAttribute(GOOGLE_LINK_SUCCESS, true);
        return updatedUser;
    }

    private HttpSession currentSession() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest().getSession(false);
    }

    private void clearGoogleLinkSession(HttpSession session) {
        session.removeAttribute(GOOGLE_LINK_USER_NUM);
        session.removeAttribute(GOOGLE_LINK_EMAIL);
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
