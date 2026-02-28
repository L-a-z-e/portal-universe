package com.portal.universe.authservice.password.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 비밀번호 재설정 토큰을 Redis에 저장하고 관리하는 서비스.
 * Redis 키 패턴: pwd_reset:{token} → userId
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {

    private static final String KEY_PREFIX = "pwd_reset:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${security.password.reset.token-ttl-minutes:30}")
    private int tokenTtlMinutes;

    /**
     * 비밀번호 재설정 토큰을 생성하고 Redis에 저장합니다.
     *
     * @param userId 사용자 UUID
     * @return 생성된 토큰
     */
    public String generateToken(String userId) {
        String token = UUID.randomUUID().toString();
        String key = KEY_PREFIX + token;
        redisTemplate.opsForValue().set(key, userId, tokenTtlMinutes, TimeUnit.MINUTES);
        log.info("Password reset token generated for user: {}", userId);
        return token;
    }

    /**
     * 토큰을 검증하고 소모합니다 (일회용).
     * Lua script로 GET+DELETE를 원자적으로 수행합니다.
     *
     * @param token 비밀번호 재설정 토큰
     * @return userId (유효하지 않으면 null)
     */
    public String validateAndConsume(String token) {
        String key = KEY_PREFIX + token;

        String luaScript =
                "local value = redis.call('GET', KEYS[1]) " +
                "if value then " +
                "  redis.call('DEL', KEYS[1]) " +
                "  return value " +
                "end " +
                "return nil";

        DefaultRedisScript<String> script = new DefaultRedisScript<>(luaScript, String.class);
        String userId = redisTemplate.execute(script, Collections.singletonList(key));

        if (userId != null) {
            log.info("Password reset token consumed for user: {}", userId);
        } else {
            log.warn("Invalid or expired password reset token attempted");
        }
        return userId;
    }

    /**
     * 토큰에서 userId를 조회합니다 (소모하지 않음).
     * 비밀번호 검증 후 소모하기 위해 사전 조회용.
     *
     * @param token 비밀번호 재설정 토큰
     * @return userId (유효하지 않으면 null)
     */
    public String getUserId(String token) {
        String key = KEY_PREFIX + token;
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 토큰 존재 여부만 확인합니다 (소모하지 않음).
     * 프론트엔드 페이지 진입 시 유효성 확인용.
     *
     * @param token 비밀번호 재설정 토큰
     * @return 유효 여부
     */
    public boolean isValid(String token) {
        String key = KEY_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
