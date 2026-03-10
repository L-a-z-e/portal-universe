package com.portal.universe.authservice.support.fixture;

import com.portal.universe.authservice.common.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * JWT 토큰 및 JwtProperties 테스트 데이터 빌더.
 * 기존 JwtTestHelper의 기능을 흡수하면서 SECRET_KEY/kid 불일치를 해소합니다.
 */
public final class JwtFixture {

    public static final String SECRET_KEY =
            "test-secret-key-for-unit-tests-must-be-at-least-256-bits-long-for-hmac-sha256";
    public static final String KEY_ID = "test-key-1";
    public static final String USER_UUID = "550e8400-e29b-41d4-a716-446655440000";
    public static final String USER_EMAIL = "test@example.com";
    public static final long ACCESS_TOKEN_EXPIRATION = 900_000L;
    public static final long REFRESH_TOKEN_EXPIRATION = 604_800_000L;

    private JwtFixture() {}

    // ========== Token Builder ==========

    public static TokenBuilder tokenBuilder() {
        return new TokenBuilder();
    }

    public static String createValidToken() {
        return tokenBuilder().build();
    }

    public static String createValidToken(List<String> roles) {
        return tokenBuilder().roles(roles).build();
    }

    public static String createExpiredToken() {
        return tokenBuilder()
                .issuedAt(new Date(System.currentTimeMillis() - ACCESS_TOKEN_EXPIRATION))
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .build();
    }

    public static String createTokenWithKid(String kid) {
        return tokenBuilder().kid(kid).build();
    }

    public static String createTokenWithoutKid() {
        return tokenBuilder().kid(null).build();
    }

    public static String createTokenWithMemberships(Map<String, String> memberships) {
        return tokenBuilder().claim("memberships", memberships).build();
    }

    public static class TokenBuilder {
        private String secretKey = SECRET_KEY;
        private String kid = KEY_ID;
        private String subject = USER_UUID;
        private List<String> roles = List.of("ROLE_USER");
        private String email = USER_EMAIL;
        private Date issuedAt = new Date();
        private Date expiration = new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION);
        private final Map<String, Object> extraClaims = new LinkedHashMap<>();

        public TokenBuilder secretKey(String secretKey) {
            this.secretKey = secretKey;
            return this;
        }

        public TokenBuilder kid(String kid) {
            this.kid = kid;
            return this;
        }

        public TokenBuilder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public TokenBuilder roles(List<String> roles) {
            this.roles = roles;
            return this;
        }

        public TokenBuilder email(String email) {
            this.email = email;
            return this;
        }

        public TokenBuilder issuedAt(Date issuedAt) {
            this.issuedAt = issuedAt;
            return this;
        }

        public TokenBuilder expiration(Date expiration) {
            this.expiration = expiration;
            return this;
        }

        public TokenBuilder claim(String key, Object value) {
            this.extraClaims.put(key, value);
            return this;
        }

        public String build() {
            var builder = Jwts.builder();
            if (kid != null) {
                builder.header().add("kid", kid).and();
            }
            builder.subject(subject)
                    .claim("roles", roles)
                    .claim("email", email)
                    .issuedAt(issuedAt)
                    .expiration(expiration);
            extraClaims.forEach(builder::claim);
            builder.signWith(getSigningKey(secretKey), Jwts.SIG.HS256);
            return builder.compact();
        }
    }

    // ========== JwtProperties Builder ==========

    public static JwtProperties createJwtProperties() {
        return jwtPropertiesBuilder().build();
    }

    public static JwtPropertiesBuilder jwtPropertiesBuilder() {
        return new JwtPropertiesBuilder();
    }

    public static class JwtPropertiesBuilder {
        private String currentKeyId = KEY_ID;
        private String secretKey = SECRET_KEY;
        private long accessTokenExpiration = ACCESS_TOKEN_EXPIRATION;
        private long refreshTokenExpiration = REFRESH_TOKEN_EXPIRATION;

        public JwtPropertiesBuilder currentKeyId(String currentKeyId) {
            this.currentKeyId = currentKeyId;
            return this;
        }

        public JwtPropertiesBuilder secretKey(String secretKey) {
            this.secretKey = secretKey;
            return this;
        }

        public JwtPropertiesBuilder accessTokenExpiration(long ms) {
            this.accessTokenExpiration = ms;
            return this;
        }

        public JwtPropertiesBuilder refreshTokenExpiration(long ms) {
            this.refreshTokenExpiration = ms;
            return this;
        }

        public JwtProperties build() {
            JwtProperties props = new JwtProperties();
            props.setCurrentKeyId(currentKeyId);
            props.setAccessTokenExpiration(accessTokenExpiration);
            props.setRefreshTokenExpiration(refreshTokenExpiration);

            JwtProperties.KeyConfig keyConfig = new JwtProperties.KeyConfig();
            keyConfig.setSecretKey(secretKey);
            keyConfig.setActivatedAt(Instant.parse("2025-01-01T00:00:00Z"));

            props.setKeys(new HashMap<>(Map.of(currentKeyId, keyConfig)));
            return props;
        }
    }

    // ========== Utility ==========

    private static SecretKey getSigningKey(String secretKey) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
