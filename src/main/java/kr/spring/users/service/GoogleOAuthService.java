package kr.spring.users.service;

import java.sql.Date;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.spring.users.dao.GoogleUserMapper;
import kr.spring.users.vo.UsersVO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class GoogleOAuthService {

    private final GoogleUserMapper googleUserMapper;

    public UsersVO findByGoogleId(String googleId) {
        if (googleId == null || googleId.isBlank()) {
            return null;
        }
        return googleUserMapper.selectByGoogleId(googleId);
    }

    public UsersVO findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return googleUserMapper.selectByEmail(normalizeEmail(email));
    }

    public UsersVO findByUserNum(long userNum) {
        return googleUserMapper.selectByUserNum(userNum);
    }

    public UsersVO createGoogleUser(String googleId, String email, String name) {
        String displayName = normalizeUserName(name, email);

        UsersVO user = new UsersVO();
        user.setGoogle_id(googleId);
        user.setEmail(normalizeEmail(email));
        user.setUser_name(displayName);
        user.setReal_name(displayName);
        user.setPhone(null);
        user.setBirth(null);
        user.setIntro("");
        user.setAuth("USER_MEMBER");
        user.setLogin_type(2);
        user.setStatus(1);

        googleUserMapper.insertGoogleUser(user);
        return googleUserMapper.selectByGoogleId(googleId);
    }

    public int updateAdditionalInfo(long userNum, String realName, String phone, Date birth) {
        UsersVO user = new UsersVO();
        user.setUser_num(userNum);
        user.setReal_name(realName);
        user.setPhone(phone);
        user.setBirth(birth);
        return googleUserMapper.updateGoogleAdditionalInfo(user);
    }

    public UsersVO linkGoogleAccount(long userNum, String googleId) {
        if (userNum < 1 || isBlank(googleId)) {
            return null;
        }

        int updated = googleUserMapper.linkGoogleAccount(userNum, googleId);
        if (updated < 1) {
            return null;
        }
        return googleUserMapper.selectByUserNum(userNum);
    }

    public UsersVO unlinkGoogleAccount(long userNum) {
        if (userNum < 1) {
            return null;
        }

        int updated = googleUserMapper.unlinkGoogleAccount(userNum);
        if (updated < 1) {
            return null;
        }
        return googleUserMapper.selectByUserNum(userNum);
    }

    public boolean isPhoneDuplicatedExceptUser(String phone, long userNum) {
        return googleUserMapper.countByPhoneExceptUser(phone, userNum) > 0;
    }

    public boolean requiresAdditionalInfo(UsersVO user) {
        return user != null
                && user.getLogin_type() == 2
                && (isBlank(user.getPhone()) || user.getBirth() == null);
    }

    private String normalizeUserName(String name, String email) {
        if (!isBlank(name)) {
            return name.trim();
        }
        if (!isBlank(email) && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }
        return "Google User";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
