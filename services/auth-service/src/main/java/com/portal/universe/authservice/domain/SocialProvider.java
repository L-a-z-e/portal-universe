package com.portal.universe.authservice.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 소셜 로그인 제공자를 정의하는 열거형입니다.
 */
@Getter
@RequiredArgsConstructor
public enum SocialProvider {
    GOOGLE("Google"),
    NAVER("Naver"),
    KAKAO("Kakao");

    private final String description;
}
