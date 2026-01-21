package com.portal.universe.apigateway.filter;

import com.portal.universe.apigateway.config.JwtConfig;
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
 * Auth Service와 동일한 HMAC secret key로 토큰 서명을 검증합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtConfig jwtConfig;

    /**
     * Secret Key를 기반으로 검증용 키를 생성합니다.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtConfig.getSecretKey().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
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
     */
    private Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
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
