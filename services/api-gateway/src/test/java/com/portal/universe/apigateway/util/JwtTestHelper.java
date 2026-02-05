package com.portal.universe.apigateway.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class JwtTestHelper {

    public static final String TEST_SECRET_KEY = "this-is-a-test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha";

    private JwtTestHelper() {}

    public static String createValidToken(String secretKey, String userId, List<String> roles) {
        return createToken(secretKey, null, userId, roles, null, null, null, 3600_000L);
    }

    public static String createExpiredToken(String secretKey, String userId) {
        return createToken(secretKey, null, userId, List.of("ROLE_USER"), null, null, null, -1000L);
    }

    public static String createTokenWithKid(String secretKey, String kid, String userId, List<String> roles) {
        return createToken(secretKey, kid, userId, roles, null, null, null, 3600_000L);
    }

    public static String createTokenWithMemberships(String secretKey, String userId, Map<String, String> memberships) {
        return createToken(secretKey, null, userId, List.of("ROLE_USER"), memberships, null, null, 3600_000L);
    }

    public static String createTokenWithNickname(String secretKey, String userId, String nickname, String username) {
        return createToken(secretKey, null, userId, List.of("ROLE_USER"), null, nickname, username, 3600_000L);
    }

    public static String createToken(String secretKey, String kid, String subject,
                                     List<String> roles, Map<String, String> memberships,
                                     String nickname, String username, long validityMillis) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiration = new Date(now.getTime() + validityMillis);

        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(expiration);

        if (kid != null) {
            builder.header().keyId(kid).and();
        }

        if (roles != null && !roles.isEmpty()) {
            builder.claim("roles", roles);
        }

        if (memberships != null) {
            builder.claim("memberships", memberships);
        }

        if (nickname != null) {
            builder.claim("nickname", nickname);
        }

        if (username != null) {
            builder.claim("username", username);
        }

        return builder.signWith(key).compact();
    }
}
