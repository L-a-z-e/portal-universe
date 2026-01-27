package com.portal.universe.apigateway.filter;

import com.portal.universe.apigateway.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * JWT 토큰을 검증하는 WebFlux 필터입니다.
 * Auth Service와 동일한 HMAC secret key로 토큰 서명을 검증하며,
 * 키 교체(Key Rotation)를 지원합니다.
 *
 * <p>JWT 헤더의 kid(Key ID)를 확인하여 적절한 키로 서명을 검증합니다.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProperties jwtProperties;

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
     * @return Key ID (없으면 현재 키 ID)
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

            log.warn("JWT token does not contain kid header, using current key");
            return jwtProperties.getCurrentKeyId();
        } catch (Exception e) {
            log.error("Failed to extract kid from JWT token, using current key", e);
            return jwtProperties.getCurrentKeyId();
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 공개 경로는 JWT 검증 생략
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // Authorization 헤더에서 토큰 추출
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            // 토큰이 없으면 SecurityConfig의 접근 제어에 위임
            return chain.filter(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            // JWT 검증
            Claims claims = validateToken(token);
            String userId = claims.getSubject();
            String roles = claims.get("roles", String.class);

            log.debug("JWT validated for user: {}, roles: {}", userId, roles);

            // Spring Security Context에 인증 정보 설정
            List<SimpleGrantedAuthority> authorities = roles != null
                    ? List.of(new SimpleGrantedAuthority(roles))
                    : Collections.emptyList();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            // X-User-Id 헤더 추가 (하위 서비스에서 사용)
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Roles", roles != null ? roles : "")
                    .build();

            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(mutatedRequest)
                    .build();

            return chain.filter(mutatedExchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));

        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            return handleUnauthorized(exchange, "Token expired");
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return handleUnauthorized(exchange, "Invalid token");
        }
    }

    /**
     * JWT 토큰을 검증하고 Claims를 반환합니다.
     * JWT 헤더의 kid를 확인하여 적절한 키로 서명을 검증합니다.
     *
     * @param token JWT 토큰
     * @return Claims
     * @throws JwtException 토큰이 유효하지 않은 경우
     */
    private Claims validateToken(String token) {
        // 1. 헤더에서 kid 추출
        String keyId = extractKeyId(token);
        log.debug("Validating token with key ID: {}", keyId);

        // 2. kid에 해당하는 키로 검증
        SecretKey signingKey = getSigningKeyById(keyId);

        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 공개 경로인지 확인합니다.
     */
    private boolean isPublicPath(String path) {
        return path.startsWith("/auth-service/") ||
               path.startsWith("/api/auth/") ||
               path.startsWith("/api/users/") ||
               path.startsWith("/actuator/") ||
               // Shopping 공개 경로
               path.equals("/api/shopping/products") ||
               path.startsWith("/api/shopping/products/") ||
               path.equals("/api/shopping/categories") ||
               path.startsWith("/api/shopping/categories/") ||
               path.equals("/api/shopping/coupons") ||
               path.equals("/api/shopping/time-deals") ||
               path.startsWith("/api/shopping/time-deals/");
    }

    /**
     * 401 Unauthorized 응답을 반환합니다.
     */
    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("X-Auth-Error", message);
        return exchange.getResponse().setComplete();
    }
}
