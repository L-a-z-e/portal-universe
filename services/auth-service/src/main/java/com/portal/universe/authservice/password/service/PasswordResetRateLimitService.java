package com.portal.universe.authservice.password.service;

import com.portal.universe.authservice.common.exception.AuthErrorCode;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 비밀번호 재설정 요청에 대한 IP 기반 Rate Limiting.
 * Redis 키 패턴: pwd_reset_rate:{ip} → count
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetRateLimitService {

    private static final String KEY_PREFIX = "pwd_reset_rate:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${security.password.reset.rate-limit-max-attempts:3}")
    private int maxAttempts;

    @Value("${security.password.reset.rate-limit-window-seconds:300}")
    private int windowSeconds;

    /**
     * Rate limit을 확인합니다. 초과 시 예외를 던집니다.
     *
     * @param ip 클라이언트 IP 주소
     * @throws CustomBusinessException rate limit 초과 시
     */
    public void checkRateLimit(String ip) {
        String key = KEY_PREFIX + ip;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1L) {
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }

        if (count != null && count > maxAttempts) {
            log.warn("Password reset rate limit exceeded for IP: {}", ip);
            throw new CustomBusinessException(AuthErrorCode.PASSWORD_RESET_RATE_LIMITED);
        }
    }
}
