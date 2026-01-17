package com.portal.universe.authservice.oauth2;

import java.util.Map;

/**
 * Google OAuth2 사용자 정보를 파싱하는 클래스입니다.
 *
 * Google UserInfo 응답 예시:
 * {
 *   "sub": "1234567890",
 *   "name": "홍길동",
 *   "given_name": "길동",
 *   "family_name": "홍",
 *   "picture": "https://lh3.googleusercontent.com/...",
 *   "email": "user@gmail.com",
 *   "email_verified": true,
 *   "locale": "ko"
 * }
 */
public class GoogleOAuth2UserInfo extends OAuth2UserInfo {

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getImageUrl() {
        return (String) attributes.get("picture");
    }
}
