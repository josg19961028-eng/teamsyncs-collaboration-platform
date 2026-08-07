package kr.spring.users.vo;

import lombok.Getter;

/**
 * 회원 상태 (USERS.STATUS 컬럼)
 * 1:정상, 2:정지, 3:탈퇴
 */
@Getter
public enum UserStatus {
    NORMAL(1),
    SUSPENDED(2),
    WITHDRAWN(3);

    private final int value;

    UserStatus(int value) {
        this.value = value;
    }
}