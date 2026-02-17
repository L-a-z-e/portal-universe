package com.portal.universe.apigateway.filter;

import com.portal.universe.apigateway.config.JwtProperties;
import com.portal.universe.apigateway.config.PublicPathProperties;
import com.portal.universe.apigateway.exception.GatewayErrorCode;
import com.portal.universe.apigateway.exception.GatewayErrorResponse;
import com.portal.universe.apigateway.service.TokenBlacklistChecker;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JWT 토큰을 검증하는 WebFlux 필터입니다.
 * Auth Service와 동일한 HMAC secret key로 토큰 서명을 검증하며,
 * 키 교체(Key Rotation)를 지원합니다.
 *
 * <p>JWT 헤더의 kid(Key ID)를 확인하여 적절한 키로 서명을 검증합니다.</p>
 * <p>Role Hierarchy를 resolve하여 X-User-Effective-Roles 헤더를 추가합니다.</p>
 */
@Slf4j
public class JwtAuthenticationFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProperties jwtProperties;
    private final TokenBlacklistChecker tokenBlacklistChecker;
    private final String[] skipJwtParsingPrefixes;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthenticationFilter(JwtProperties jwtProperties, PublicPathProperties publicPathProperties,
                                   TokenBlacklistChecker tokenBlacklistChecker) {
        this.jwtProperties = jwtProperties;
        this.tokenBlacklistChecker = tokenBlacklistChecker;
        this.skipJwtParsingPrefixes = publicPathProperties.getSkipJwtParsing().toArray(String[]::new);
    }

    /**
     * 특정 키 ID에 해당하는 서명용 키를 생성합니다.
     */
    private SecretKey getSigningKeyById(String keyId) {
        if (keyId == null || keyId.isBlank()) {
            throw new JwtException("Key ID cannot be null or empty");
        }

        JwtProperties.KeyConfig keyConfig = jwtProperties.getKeys().get(keyId);
        if (keyConfig == null) {
            throw new JwtException("JWT key not found for ID: " + keyId);
        }

        if (keyConfig.isExpired()) {
            log.warn("JWT key is expired: {}", keyId);
            throw new JwtException("JWT key is expired: " + keyId);
        }

        return getSigningKey(keyConfig.getSecretKey());
    }

    private SecretKey getSigningKey(String secretKey) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private String extractKeyId(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new MalformedJwtException("Invalid JWT token structure");
            }

            String header = new String(java.util.Base64.getUrlDecoder().decode(parts[0]));
            var headerNode = objectMapper.readTree(header);
            var kidNode = headerNode.get("kid");
            if (kidNode != null && !kidNode.isNull()) {
                return kidNode.asText();
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

        // 외부에서 주입된 X-User-* 헤더를 strip (Header Injection 방어)
        ServerHttpRequest sanitizedRequest = request.mutate()
                .headers(h -> {
                    h.remove("X-User-Id");
                    h.remove("X-User-Roles");
                    h.remove("X-User-Effective-Roles");
                    h.remove("X-User-Memberships");
                    h.remove("X-User-Nickname");
                    h.remove("X-User-Name");
                }).build();
        ServerWebExchange sanitizedExchange = exchange.mutate().request(sanitizedRequest).build();

        // 공개 경로는 JWT 검증 생략 (sanitized exchange 사용)
        if (isPublicPath(path)) {
            return chain.filter(sanitizedExchange);
        }

        // Authorization 헤더에서 토큰 추출
        String authHeader = sanitizedRequest.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return chain.filter(sanitizedExchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        // JWT 서명 검증
        final Claims claims;
        try {
            claims = validateToken(token);
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            return GatewayErrorResponse.write(sanitizedExchange, GatewayErrorCode.TOKEN_EXPIRED);
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return GatewayErrorResponse.write(sanitizedExchange, GatewayErrorCode.INVALID_TOKEN);
        }

        // 블랙리스트 체크 (reactive) → Role Hierarchy resolve → 인증 정보 설정
        return tokenBlacklistChecker.isBlacklisted(token)
                .flatMap(blacklisted -> {
                    if (Boolean.TRUE.equals(blacklisted)) {
                        log.warn("JWT token is blacklisted");
                        return GatewayErrorResponse.write(sanitizedExchange, GatewayErrorCode.TOKEN_REVOKED);
                    }

                    String userId = claims.getSubject();
                    String nickname = claims.get("nickname", String.class);
                    String username = claims.get("username", String.class);

                    // JWT roles 파싱
                    List<String> rolesList = parseRoles(claims);
                    String rolesHeader = String.join(",", rolesList);

                    // JWT memberships 파싱 (enriched JSON passthrough)
                    String membershipsHeader = parseMemberships(claims);

                    // JWT effectiveRoles claim 사용 (없으면 direct roles fallback)
                    List<String> effectiveRoles = parseEffectiveRoles(claims, rolesList);
                    String effectiveRolesHeader = String.join(",", effectiveRoles);

                    log.debug("JWT validated for user: {}, roles: {}, effectiveRoles: {}, memberships: {}",
                            userId, rolesList, effectiveRoles, membershipsHeader);

                    // effective roles 기반 Authority 생성
                    List<SimpleGrantedAuthority> authorities = effectiveRoles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);

                    // 하위 서비스로 전달할 헤더 설정
                    ServerHttpRequest mutatedRequest = sanitizedRequest.mutate()
                            .header("X-User-Id", userId)
                            .header("X-User-Roles", rolesHeader)
                            .header("X-User-Effective-Roles", effectiveRolesHeader)
                            .header("X-User-Memberships", membershipsHeader)
                            .header("X-User-Nickname", nickname != null ? URLEncoder.encode(nickname, StandardCharsets.UTF_8) : "")
                            .header("X-User-Name", username != null ? URLEncoder.encode(username, StandardCharsets.UTF_8) : "")
                            .build();

                    ServerWebExchange mutatedExchange = sanitizedExchange.mutate()
                            .request(mutatedRequest)
                            .build();

                    return chain.filter(mutatedExchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                });
    }

    private Claims validateToken(String token) {
        String keyId = extractKeyId(token);
        log.debug("Validating token with key ID: {}", keyId);

        SecretKey signingKey = getSigningKeyById(keyId);

        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @SuppressWarnings("unchecked")
    private List<String> parseRoles(Claims claims) {
        Object rolesClaim = claims.get("roles");
        if (rolesClaim instanceof List<?> rolesArr) {
            return ((List<Object>) rolesArr).stream()
                    .map(Object::toString)
                    .toList();
        }
        if (rolesClaim != null) {
            log.warn("Unexpected roles claim type: {}", rolesClaim.getClass().getName());
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<String> parseEffectiveRoles(Claims claims, List<String> directRoles) {
        Object effectiveRolesClaim = claims.get("effectiveRoles");
        if (effectiveRolesClaim instanceof List<?> rolesArr) {
            List<String> result = ((List<Object>) rolesArr).stream()
                    .map(Object::toString)
                    .toList();
            if (!result.isEmpty()) {
                log.debug("Using effectiveRoles from JWT claim: {}", result);
                return result;
            }
        }
        log.debug("No effectiveRoles in JWT, using direct roles: {}", directRoles);
        return directRoles;
    }

    private String parseMemberships(Claims claims) {
        Object membershipsClaim = claims.get("memberships");
        if (membershipsClaim instanceof Map<?, ?> membershipsMap) {
            try {
                return objectMapper.writeValueAsString(membershipsMap);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize memberships: {}", e.getMessage());
            }
        }
        return "{}";
    }

    private boolean isPublicPath(String path) {
        for (String prefix : skipJwtParsingPrefixes) {
            if (path.startsWith(prefix) || path.equals(prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix)) {
                return true;
            }
        }
        return false;
    }
}
