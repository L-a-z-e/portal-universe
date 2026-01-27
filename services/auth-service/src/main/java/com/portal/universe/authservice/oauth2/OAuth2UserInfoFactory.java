package com.portal.universe.authservice.oauth2;

import com.portal.universe.authservice.oauth2.domain.SocialProvider;

import java.util.Map;

/**
 * 소셜 로그인 제공자에 따라 적절한 OAuth2UserInfo 구현체를 생성하는 팩토리 클래스입니다.
 */
public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        SocialProvider provider = SocialProvider.valueOf(registrationId.toUpperCase());

        return switch (provider) {
            case GOOGLE -> new GoogleOAuth2UserInfo(attributes);
            case NAVER -> new NaverOAuth2UserInfo(attributes);
            case KAKAO -> new KakaoOAuth2UserInfo(attributes);
        };
    }
}
