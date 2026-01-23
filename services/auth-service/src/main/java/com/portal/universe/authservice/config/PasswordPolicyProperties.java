package com.portal.universe.authservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 비밀번호 정책 설정을 관리하는 클래스입니다.
 * application.yml의 security.password 속성과 바인딩됩니다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "security.password")
public class PasswordPolicyProperties {

    /**
     * 비밀번호 최소 길이
     */
    private int minLength = 8;

    /**
     * 비밀번호 최대 길이
     */
    private int maxLength = 128;

    /**
     * 대문자 포함 필수 여부
     */
    private boolean requireUppercase = true;

    /**
     * 소문자 포함 필수 여부
     */
    private boolean requireLowercase = true;

    /**
     * 숫자 포함 필수 여부
     */
    private boolean requireDigit = true;

    /**
     * 특수문자 포함 필수 여부
     */
    private boolean requireSpecialChar = true;

    /**
     * 허용되는 특수문자 목록
     */
    private String specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?";

    /**
     * 비밀번호 히스토리 보관 개수 (최근 N개 비밀번호 재사용 금지)
     */
    private int historyCount = 5;

    /**
     * 비밀번호 만료 기간 (일)
     */
    private int maxAge = 90;

    /**
     * 연속된 문자/숫자 금지 여부
     */
    private boolean preventSequential = true;

    /**
     * 사용자 정보 포함 금지 여부
     */
    private boolean preventUserInfo = true;
}
