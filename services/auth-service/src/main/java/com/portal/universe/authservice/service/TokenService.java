package com.portal.universe.authservice.service;

import com.portal.universe.authservice.config.JwtConfig;
import com.portal.universe.authservice.domain.User;
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
 * HMAC-SHA256 알고리즘을 사용하여 토큰을 서명합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtConfig jwtConfig;

    /**
     * Secret Key를 기반으로 서명용 키를 생성합니다.
     * Plain text secret key를 UTF-8로 인코딩하여 사용합니다.
     *
     * @return SecretKey
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtConfig.getSecretKey().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Access Token을 생성합니다.
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
        Date expiration = new Date(now.getTime() + jwtConfig.getAccessTokenExpiration());

        return Jwts.builder()
                .claims(claims)
                .subject(user.getUuid())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Refresh Token을 생성합니다.
     * - sub: 사용자 UUID
     * - exp: 만료 시간 (7일)
     *
     * @param user 사용자 정보
     * @return Refresh Token
     */
    public String generateRefreshToken(User user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtConfig.getRefreshTokenExpiration());

        return Jwts.builder()
                .subject(user.getUuid())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Access Token을 검증하고 Claims를 반환합니다.
     *
     * @param token Access Token
     * @return Claims
     * @throws JwtException 토큰이 유효하지 않은 경우
     */
    public Claims validateAccessToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
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
            log.warn("JWT claims string is empty: {}", e.getMessage());
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
