package com.portal.universe.authservice.oauth2;

import java.util.Map;

/**
 * Kakao OAuth2 사용자 정보를 파싱하는 클래스입니다.
 *
 * Kakao UserInfo 응답 예시:
 * {
 *   "id": 1234567890,
 *   "connected_at": "2023-01-01T00:00:00Z",
 *   "kakao_account": {
 *     "email": "user@kakao.com",
 *     "email_needs_agreement": false,
 *     "profile": {
 *       "nickname": "홍길동",
 *       "thumbnail_image_url": "https://k.kakaocdn.net/...",
 *       "profile_image_url": "https://k.kakaocdn.net/..."
 *     },
 *     "profile_nickname_needs_agreement": false
 *   }
 * }
 */
public class KakaoOAuth2UserInfo extends OAuth2UserInfo {

    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getKakaoAccount() {
        return (Map<String, Object>) attributes.get("kakao_account");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getProfile() {
        Map<String, Object> kakaoAccount = getKakaoAccount();
        return kakaoAccount != null ? (Map<String, Object>) kakaoAccount.get("profile") : null;
    }

    @Override
    public String getId() {
        // Kakao id는 Long 타입으로 반환됨
        Object id = attributes.get("id");
        return id != null ? String.valueOf(id) : null;
    }

    @Override
    public String getEmail() {
        Map<String, Object> kakaoAccount = getKakaoAccount();
        return kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
    }

    @Override
    public String getName() {
        Map<String, Object> profile = getProfile();
        return profile != null ? (String) profile.get("nickname") : null;
    }

    @Override
    public String getImageUrl() {
        Map<String, Object> profile = getProfile();
        if (profile == null) return null;

        // profile_image_url 우선, 없으면 thumbnail_image_url 사용
        String imageUrl = (String) profile.get("profile_image_url");
        return imageUrl != null ? imageUrl : (String) profile.get("thumbnail_image_url");
    }
}
