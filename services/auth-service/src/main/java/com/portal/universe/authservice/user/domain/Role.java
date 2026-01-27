package com.portal.universe.authservice.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자의 역할을 정의하는 열거형(Enum) 클래스입니다.
 * Spring Security에서 권한을 식별하는 키를 포함합니다.
 */
@Getter
@RequiredArgsConstructor
public enum Role {
    /**
     * 일반 사용자 역할
     */
    USER("ROLE_USER"),

    /**
     * 관리자 역할
     */
    ADMIN("ROLE_ADMIN");

    /**
     * Spring Security에서 사용하는 권한 키 (e.g., 'ROLE_USER')
     */
    private final String key;

}