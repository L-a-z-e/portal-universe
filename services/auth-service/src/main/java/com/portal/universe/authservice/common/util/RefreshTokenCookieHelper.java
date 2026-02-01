package com.portal.universe.authservice.common.util;

import com.portal.universe.authservice.common.config.JwtProperties;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Refresh Token Cookie 관리를 담당하는 헬퍼 클래스입니다.
 * 로그인, OAuth2 콜백, 토큰 갱신, 로그아웃 시 사용됩니다.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenCookieHelper {

    public static final String COOKIE_NAME = "portal_refresh_token";
    private static final String COOKIE_PATH = "/";

    private final JwtProperties jwtProperties;

    @Value("${app.cookie.secure:true}")
    private boolean secure;

    @Value("${app.cookie.same-site:Lax}")
    private String sameSite;

    /**
     * Refresh Token을 HttpOnly Cookie로 설정합니다.
     * maxAge는 JWT refresh token expiration 설정값에서 파생됩니다.
     */
    public void setCookie(HttpServletResponse response, String refreshToken) {
        Duration maxAge = Duration.ofMillis(jwtProperties.getRefreshTokenExpiration());

        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Refresh Token Cookie를 삭제합니다.
     * 로그아웃 시 호출됩니다.
     */
    public void clearCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Cookie 이름을 반환합니다.
     * @CookieValue 어노테이션에서 참조할 때 사용합니다.
     */
    public static String getCookieName() {
        return COOKIE_NAME;
    }
}
