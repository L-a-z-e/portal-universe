package com.portal.universe.authservice.oauth2;

import java.util.Map;

/**
 * 소셜 로그인 제공자별 사용자 정보를 추상화한 인터페이스입니다.
 * 각 제공자(Google, Naver, Kakao)마다 응답 형식이 다르므로
 * 이 인터페이스를 통해 일관된 방식으로 사용자 정보를 추출합니다.
 */
public abstract class OAuth2UserInfo {

    protected Map<String, Object> attributes;

    public OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * 소셜 로그인 제공자에서 발급한 고유 식별자
     */
    public abstract String getId();

    /**
     * 사용자 이메일
     */
    public abstract String getEmail();

    /**
     * 사용자 이름 (닉네임)
     */
    public abstract String getName();

    /**
     * 프로필 이미지 URL
     */
    public abstract String getImageUrl();
}
