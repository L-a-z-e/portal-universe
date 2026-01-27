package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.common.config.JwtProperties;
import com.portal.universe.authservice.user.domain.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Access Token과 Refresh Token을 생성하고 검증하는 서비스입니다.
 * HMAC-SHA256 알고리즘을 사용하며, 키 교체(Key Rotation)를 지원합니다.
 *
 * <p>토큰 생성 시에는 currentKeyId에 해당하는 키를 사용하며,
 * JWT 헤더에 kid(Key ID)를 포함시킵니다.</p>
 *
 * <p>토큰 검증 시에는 JWT 헤더의 kid를 확인하여
 * 해당하는 키를 선택하여 서명을 검증합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtProperties jwtProperties;

    /**
     * 현재 활성화된 키 ID를 반환합니다.
     *
     * @return 현재 키 ID
     */
    private String getCurrentKeyId() {
        return jwtProperties.getCurrentKeyId();
    }

    /**
     * 현재 키 ID에 해당하는 서명용 키를 생성합니다.
     *
     * @return SecretKey
     * @throws IllegalStateException 현재 키가 설정되지 않았거나 유효하지 않은 경우
     */
    private SecretKey getCurrentSigningKey() {
        String currentKeyId = getCurrentKeyId();
        if (currentKeyId == null || currentKeyId.isBlank()) {
            throw new IllegalStateException("Current JWT key ID is not configured");
        }

        JwtProperties.KeyConfig keyConfig = jwtProperties.getKeys().get(currentKeyId);
        if (keyConfig == null) {
            throw new IllegalStateException("JWT key not found for ID: " + currentKeyId);
        }

        if (keyConfig.isExpired()) {
            log.warn("Current JWT key is expired: {}", currentKeyId);
            throw new IllegalStateException("Current JWT key is expired: " + currentKeyId);
        }

        return getSigningKey(keyConfig.getSecretKey());
    }

    /**
     * 특정 키 ID에 해당하는 서명용 키를 생성합니다.
     *
     * @param keyId 키 ID
     * @return SecretKey
     * @throws IllegalArgumentException 키를 찾을 수 없거나 만료된 경우
     */
    private SecretKey getSigningKeyById(String keyId) {
        if (keyId == null || keyId.isBlank()) {
            throw new IllegalArgumentException("Key ID cannot be null or empty");
        }

        JwtProperties.KeyConfig keyConfig = jwtProperties.getKeys().get(keyId);
        if (keyConfig == null) {
            throw new IllegalArgumentException("JWT key not found for ID: " + keyId);
        }

        if (keyConfig.isExpired()) {
            log.warn("JWT key is expired: {}", keyId);
            throw new IllegalArgumentException("JWT key is expired: " + keyId);
        }

        return getSigningKey(keyConfig.getSecretKey());
    }

    /**
     * Secret Key 문자열을 SecretKey 객체로 변환합니다.
     *
     * @param secretKey 비밀키 문자열
     * @return SecretKey
     */
    private SecretKey getSigningKey(String secretKey) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * JWT 토큰 헤더에서 kid(Key ID)를 추출합니다.
     *
     * @param token JWT 토큰
     * @return Key ID
     */
    private String extractKeyId(String token) {
        try {
            // 서명 검증 없이 헤더만 파싱
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new MalformedJwtException("Invalid JWT token structure");
            }

            String header = new String(java.util.Base64.getUrlDecoder().decode(parts[0]));
            // Simple JSON parsing for kid
            if (header.contains("\"kid\"")) {
                int kidStart = header.indexOf("\"kid\"") + 6;
                int valueStart = header.indexOf("\"", kidStart) + 1;
                int valueEnd = header.indexOf("\"", valueStart);
                return header.substring(valueStart, valueEnd);
            }

            log.warn("JWT token does not contain kid header");
            return null;
        } catch (Exception e) {
            log.error("Failed to extract kid from JWT token", e);
            throw new MalformedJwtException("Failed to extract kid from JWT token", e);
        }
    }

    /**
     * Access Token을 생성합니다.
     * - header: kid (현재 키 ID)
     * - sub: 사용자 UUID
     * - roles: 사용자 권한
     * - exp: 만료 시간 (15분)
     *
     * @param user 사용자 정보
     * @return Access Token
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRole().getKey());
        claims.put("email", user.getEmail());

        // Profile 정보 추가
        if (user.getProfile() != null) {
            claims.put("nickname", user.getProfile().getNickname());
            if (user.getProfile().getUsername() != null) {
                claims.put("username", user.getProfile().getUsername());
            }
        }

        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

        String currentKeyId = getCurrentKeyId();
        log.debug("Generating access token with key ID: {}", currentKeyId);

        return Jwts.builder()
                .header()
                    .add("kid", currentKeyId)
                    .and()
                .claims(claims)
                .subject(user.getUuid())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getCurrentSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Refresh Token을 생성합니다.
     * - header: kid (현재 키 ID)
     * - sub: 사용자 UUID
     * - exp: 만료 시간 (7일)
     *
     * @param user 사용자 정보
     * @return Refresh Token
     */
    public String generateRefreshToken(User user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());

        String currentKeyId = getCurrentKeyId();
        log.debug("Generating refresh token with key ID: {}", currentKeyId);

        return Jwts.builder()
                .header()
                    .add("kid", currentKeyId)
                    .and()
                .subject(user.getUuid())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getCurrentSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Access Token을 검증하고 Claims를 반환합니다.
     * JWT 헤더의 kid를 확인하여 적절한 키로 서명을 검증합니다.
     *
     * @param token Access Token
     * @return Claims
     * @throws JwtException 토큰이 유효하지 않은 경우
     * @throws IllegalArgumentException 키를 찾을 수 없거나 만료된 경우
     */
    public Claims validateAccessToken(String token) {
        try {
            // 1. 헤더에서 kid 추출
            String keyId = extractKeyId(token);
            if (keyId == null || keyId.isBlank()) {
                log.warn("JWT token does not contain kid header, using current key");
                keyId = getCurrentKeyId();
            }

            log.debug("Validating access token with key ID: {}", keyId);

            // 2. kid에 해당하는 키로 검증
            SecretKey signingKey = getSigningKeyById(keyId);

            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (ExpiredJwtException e) {
            log.warn("Access token expired: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            throw e;
        } catch (SecurityException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 토큰에서 사용자 UUID를 추출합니다.
     *
     * @param token JWT Token
     * @return 사용자 UUID
     */
    public String getUserIdFromToken(String token) {
        Claims claims = validateAccessToken(token);
        return claims.getSubject();
    }

    /**
     * 토큰의 남은 만료 시간(밀리초)을 계산합니다.
     *
     * @param token JWT Token
     * @return 남은 만료 시간 (ms)
     */
    public long getRemainingExpiration(String token) {
        Claims claims = validateAccessToken(token);
        Date expiration = claims.getExpiration();
        Date now = new Date();
        return expiration.getTime() - now.getTime();
    }
}
