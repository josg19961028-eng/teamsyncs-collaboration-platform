package kr.spring.users.vo;

import lombok.Getter;

/**
 * 회원 권한 (USERS.AUTH 컬럼)
 */
@Getter
public enum UserAuth {
    MEMBER("USER_MEMBER"),
    ADMIN("USER_ADMIN");

    private final String value;

    UserAuth(String value) {
        this.value = value;
    }
}