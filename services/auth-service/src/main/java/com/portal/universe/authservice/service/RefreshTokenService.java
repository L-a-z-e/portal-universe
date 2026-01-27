package com.portal.universe.authservice.service;

import com.portal.universe.authservice.common.config.JwtConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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
    private final JwtConfig jwtConfig;

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
                jwtConfig.getRefreshTokenExpiration(),
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
}
