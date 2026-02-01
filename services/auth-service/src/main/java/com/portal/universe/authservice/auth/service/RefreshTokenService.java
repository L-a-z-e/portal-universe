package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.common.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Refresh Token을 Redis에 저장하고 관리하는 서비스입니다.
 * Redis 키 패턴: refresh_token:{userId}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtProperties jwtProperties;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    /**
     * Refresh Token을 Redis에 저장합니다.
     * TTL은 JWT 설정의 refreshTokenExpiration 값을 사용합니다.
     *
     * @param userId 사용자 UUID
     * @param token Refresh Token
     */
    public void saveRefreshToken(String userId, String token) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.opsForValue().set(
                key,
                token,
                jwtProperties.getRefreshTokenExpiration(),
                TimeUnit.MILLISECONDS
        );
        log.info("Refresh token saved for user: {}", userId);
    }

    /**
     * Redis에서 Refresh Token을 조회합니다.
     *
     * @param userId 사용자 UUID
     * @return Refresh Token (존재하지 않으면 null)
     */
    public String getRefreshToken(String userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        Object token = redisTemplate.opsForValue().get(key);
        return token != null ? token.toString() : null;
    }

    /**
     * Redis에서 Refresh Token을 삭제합니다.
     * 로그아웃 시 호출됩니다.
     *
     * @param userId 사용자 UUID
     */
    public void deleteRefreshToken(String userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.delete(key);
        log.info("Refresh token deleted for user: {}", userId);
    }

    /**
     * 저장된 Refresh Token과 요청된 토큰을 비교하여 검증합니다.
     *
     * @param userId 사용자 UUID
     * @param token 검증할 Refresh Token
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public boolean validateRefreshToken(String userId, String token) {
        String storedToken = getRefreshToken(userId);
        if (storedToken == null) {
            log.warn("Refresh token not found for user: {}", userId);
            return false;
        }

        boolean isValid = storedToken.equals(token);
        if (!isValid) {
            log.warn("Refresh token mismatch for user: {}", userId);
        }
        return isValid;
    }

    /**
     * Refresh Token을 원자적으로 교체합니다 (Rotation).
     * 기존 토큰이 일치하는 경우에만 새 토큰으로 교체합니다.
     *
     * @param userId 사용자 UUID
     * @param oldToken 기존 Refresh Token
     * @param newToken 새로운 Refresh Token
     * @return 교체 성공 여부
     */
    public boolean rotateRefreshToken(String userId, String oldToken, String newToken) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        long ttlMillis = jwtProperties.getRefreshTokenExpiration();

        // Lua Script: 기존 토큰이 일치하면 새 토큰으로 원자적 교체
        String luaScript =
                "local stored = redis.call('GET', KEYS[1]) " +
                "if stored == ARGV[1] then " +
                "  redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3]) " +
                "  return 1 " +
                "end " +
                "return 0";

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);
        Long result = redisTemplate.execute(script,
                Collections.singletonList(key),
                oldToken, newToken, String.valueOf(ttlMillis));

        boolean rotated = result != null && result == 1L;
        if (rotated) {
            log.info("Refresh token rotated for user: {}", userId);
        } else {
            log.warn("Refresh token rotation failed for user: {} (token mismatch)", userId);
        }
        return rotated;
    }
}
