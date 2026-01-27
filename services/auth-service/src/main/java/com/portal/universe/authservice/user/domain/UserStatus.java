package com.portal.universe.authservice.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 계정 상태를 정의하는 열거형입니다.
 */
@Getter
@RequiredArgsConstructor
public enum UserStatus {
    ACTIVE("정상"),
    DORMANT("휴면"),
    BANNED("정지"),
    WITHDRAWAL_PENDING("탈퇴 대기");

    private final String description;
}
