package com.portal.universe.authservice.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.authservice.auth.domain.UserMembership;
import com.portal.universe.authservice.auth.repository.UserMembershipRepository;
import com.portal.universe.authservice.auth.repository.UserRoleRepository;
import com.portal.universe.authservice.common.config.JwtProperties;
import com.portal.universe.authservice.user.domain.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final JwtProperties jwtProperties;
    private final UserRoleRepository userRoleRepository;
    private final UserMembershipRepository userMembershipRepository;
    private final RoleHierarchyService roleHierarchyService;

    private String getCurrentKeyId() {
        return jwtProperties.getCurrentKeyId();
    }

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

            String header = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            JsonNode headerNode = objectMapper.readTree(header);
            JsonNode kidNode = headerNode.get("kid");

            if (kidNode == null || kidNode.isNull()) {
                log.warn("JWT token does not contain kid header");
                return null;
            }

            return kidNode.asText();
        } catch (MalformedJwtException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to extract kid from JWT token", e);
            throw new MalformedJwtException("Failed to extract kid from JWT token", e);
        }
    }

    /**
     * Access Token 생성 (enriched membership format).
     * - roles: RBAC 역할 배열
     * - memberships: {membershipGroup: {tier, order}} enriched 형태
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();

        // roles 배열 (RBAC 테이블 기반)
        List<String> roleKeys = userRoleRepository.findActiveRoleKeysByUserId(user.getUuid());
        if (roleKeys.isEmpty()) {
            log.error("No RBAC roles found for user: {}. This indicates RBAC initialization failure.", user.getUuid());
            throw new IllegalStateException(
                    "No roles assigned to user: " + user.getUuid()
                            + ". RBAC data may not be properly initialized.");
        }
        claims.put("roles", roleKeys);

        // effectiveRoles: DAG에서 resolve된 전체 유효 역할
        List<String> effectiveRoles = roleHierarchyService.resolveEffectiveRoles(roleKeys);
        claims.put("effectiveRoles", effectiveRoles);

        // memberships: enriched format {membershipGroup: {tier, order}}
        List<UserMembership> activeMemberships = userMembershipRepository.findActiveByUserId(user.getUuid());
        Map<String, Map<String, Object>> membershipsMap = activeMemberships.stream()
                .collect(Collectors.toMap(
                        UserMembership::getMembershipGroup,
                        m -> Map.of(
                                "tier", (Object) m.getTier().getTierKey(),
                                "order", (Object) m.getTier().getSortOrder()
                        ),
                        (existing, replacement) -> existing
                ));
        claims.put("memberships", membershipsMap);

        claims.put("email", user.getEmail());

        if (user.getProfile() != null) {
            claims.put("nickname", user.getProfile().getNickname());
            if (user.getProfile().getUsername() != null) {
                claims.put("username", user.getProfile().getUsername());
            }
        }

        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

        String currentKeyId = getCurrentKeyId();
        log.debug("Generating access token with key ID: {} for user: {}, roles: {}", currentKeyId, user.getUuid(), roleKeys);

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

    public Claims validateRefreshToken(String token) {
        return validateTokenInternal(token, "refresh");
    }

    public Claims validateAccessToken(String token) {
        return validateTokenInternal(token, "access");
    }

    private Claims validateTokenInternal(String token, String tokenType) {
        try {
            String keyId = extractKeyId(token);
            if (keyId == null || keyId.isBlank()) {
                log.warn("JWT {} token does not contain kid header, using current key", tokenType);
                keyId = getCurrentKeyId();
            }

            log.debug("Validating {} token with key ID: {}", tokenType, keyId);

            SecretKey signingKey = getSigningKeyById(keyId);

            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (ExpiredJwtException e) {
            log.warn("{} token expired: {}", tokenType, e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT {} token: {}", tokenType, e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT {} token: {}", tokenType, e.getMessage());
            throw e;
        } catch (SecurityException e) {
            log.warn("Invalid JWT {} signature: {}", tokenType, e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid JWT {}: {}", tokenType, e.getMessage());
            throw e;
        }
    }

    public Claims parseClaimsAllowExpired(String token) {
        try {
            return validateAccessToken(token);
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public String getUserIdFromToken(String token) {
        Claims claims = validateAccessToken(token);
        return claims.getSubject();
    }

    public long getRemainingExpiration(String token) {
        Claims claims = parseClaimsAllowExpired(token);
        Date expiration = claims.getExpiration();
        Date now = new Date();
        long remaining = expiration.getTime() - now.getTime();
        return Math.max(remaining, 0);
    }
}
