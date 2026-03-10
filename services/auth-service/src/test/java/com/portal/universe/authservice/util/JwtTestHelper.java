package com.portal.universe.authservice.util;

import com.portal.universe.authservice.support.fixture.JwtFixture;

/**
 * JWT 토큰 생성 유틸리티.
 *
 * @deprecated {@link JwtFixture}를 대신 사용하세요.
 *             이 클래스는 기존 테스트 호환을 위해 유지되며, JwtFixture로 위임합니다.
 */
@Deprecated(forRemoval = true)
public final class JwtTestHelper {

    public static final String SECRET_KEY = JwtFixture.SECRET_KEY;
    public static final String USER_UUID = JwtFixture.USER_UUID;
    public static final String USER_EMAIL = JwtFixture.USER_EMAIL;

    private JwtTestHelper() {}

    public static String createValidToken(String secretKey, String userId, java.util.List<String> roles) {
        return JwtFixture.tokenBuilder()
                .secretKey(secretKey)
                .subject(userId)
                .roles(roles)
                .build();
    }

    public static String createExpiredToken(String secretKey, String userId) {
        return JwtFixture.tokenBuilder()
                .secretKey(secretKey)
                .subject(userId)
                .issuedAt(new java.util.Date(System.currentTimeMillis() - 900_000))
                .expiration(new java.util.Date(System.currentTimeMillis() - 1000))
                .build();
    }

    public static String createTokenWithKid(String secretKey, String kid, String userId) {
        return JwtFixture.tokenBuilder()
                .secretKey(secretKey)
                .kid(kid)
                .subject(userId)
                .build();
    }

    public static String createTokenWithMemberships(String secretKey, String userId,
                                                     java.util.Map<String, String> memberships) {
        return JwtFixture.tokenBuilder()
                .secretKey(secretKey)
                .subject(userId)
                .claim("memberships", memberships)
                .build();
    }

    public static String createTokenWithClaims(String secretKey, String userId,
                                                java.util.Map<String, Object> claims) {
        var builder = JwtFixture.tokenBuilder()
                .secretKey(secretKey)
                .subject(userId);
        claims.forEach(builder::claim);
        return builder.build();
    }

    public static String createTokenWithoutKid(String secretKey, String userId) {
        return JwtFixture.tokenBuilder()
                .secretKey(secretKey)
                .kid(null)
                .subject(userId)
                .build();
    }
}
