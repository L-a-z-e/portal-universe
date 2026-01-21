package com.portal.universe.authservice.oauth2;

import java.util.Map;

/**
 * Naver OAuth2 사용자 정보를 파싱하는 클래스입니다.
 *
 * Naver UserInfo 응답 예시:
 * {
 *   "resultcode": "00",
 *   "message": "success",
 *   "response": {
 *     "id": "1234567890",
 *     "email": "user@naver.com",
 *     "nickname": "홍길동",
 *     "profile_image": "https://phinf.pstatic.net/...",
 *     "name": "홍길동"
 *   }
 * }
 */
public class NaverOAuth2UserInfo extends OAuth2UserInfo {

    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getResponse() {
        return (Map<String, Object>) attributes.get("response");
    }

    @Override
    public String getId() {
        Map<String, Object> response = getResponse();
        return response != null ? (String) response.get("id") : null;
    }

    @Override
    public String getEmail() {
        Map<String, Object> response = getResponse();
        return response != null ? (String) response.get("email") : null;
    }

    @Override
    public String getName() {
        Map<String, Object> response = getResponse();
        if (response == null) return null;

        // name 우선, 없으면 nickname 사용
        String name = (String) response.get("name");
        return name != null ? name : (String) response.get("nickname");
    }

    @Override
    public String getImageUrl() {
        Map<String, Object> response = getResponse();
        return response != null ? (String) response.get("profile_image") : null;
    }
}
