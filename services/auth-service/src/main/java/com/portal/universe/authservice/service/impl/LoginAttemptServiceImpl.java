package com.portal.universe.authservice.service.impl;

import com.portal.universe.authservice.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * 로그인 시도를 추적하고 계정 잠금을 관리하는 서비스 구현체입니다.
 *
 * <p>Redis 키 구조:
 * <ul>
 *   <li>login_attempt:count:{key} → 실패 횟수 (Integer)</li>
 *   <li>login_attempt:lock:{key}  → 잠금 해제 시간 (Unix Timestamp, milliseconds)</li>
 * </ul>
 *
 * <p>잠금 정책:
 * <ul>
 *   <li>3회 실패 → 1분 잠금</li>
 *   <li>5회 실패 → 5분 잠금</li>
 *   <li>10회 실패 → 30분 잠금</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String COUNT_PREFIX = "login_attempt:count:";
    private static final String LOCK_PREFIX = "login_attempt:lock:";

    // 잠금 임계값 및 잠금 시간 (초)
    private static final int THRESHOLD_FIRST = 3;
    private static final int LOCK_TIME_FIRST = 60; // 1분

    private static final int THRESHOLD_SECOND = 5;
    private static final int LOCK_TIME_SECOND = 300; // 5분

    private static final int THRESHOLD_THIRD = 10;
    private static final int LOCK_TIME_THIRD = 1800; // 30분

    // 실패 카운트 유지 시간 (1시간)
    private static final long COUNT_EXPIRATION = 3600;

    @Override
    public void recordFailure(String key) {
        String countKey = COUNT_PREFIX + key;
        String lockKey = LOCK_PREFIX + key;

        // 현재 실패 횟수 증가
        Long currentCount = redisTemplate.opsForValue().increment(countKey);
        if (currentCount == null) {
            currentCount = 1L;
        }

        // 첫 실패 시 만료 시간 설정
        if (currentCount == 1) {
            redisTemplate.expire(countKey, COUNT_EXPIRATION, TimeUnit.SECONDS);
        }

        log.warn("Login failure recorded for key: {} (attempt: {})", key, currentCount);

        // 잠금 처리
        int lockDuration = determineLockDuration(currentCount.intValue());
        if (lockDuration > 0) {
            long unlockTime = Instant.now().toEpochMilli() + (lockDuration * 1000L);
            redisTemplate.opsForValue().set(lockKey, String.valueOf(unlockTime),
                    lockDuration, TimeUnit.SECONDS);
            log.warn("Account locked for key: {} (duration: {} seconds)", key, lockDuration);
        }
    }

    @Override
    public void recordSuccess(String key) {
        String countKey = COUNT_PREFIX + key;
        String lockKey = LOCK_PREFIX + key;

        // 실패 카운트 및 잠금 삭제
        redisTemplate.delete(countKey);
        redisTemplate.delete(lockKey);

        log.info("Login success recorded for key: {} (attempt counter reset)", key);
    }

    @Override
    public boolean isBlocked(String key) {
        String lockKey = LOCK_PREFIX + key;
        Object lockTimeObj = redisTemplate.opsForValue().get(lockKey);

        if (lockTimeObj == null) {
            return false;
        }

        try {
            long unlockTime = Long.parseLong(lockTimeObj.toString());
            long now = Instant.now().toEpochMilli();

            if (now < unlockTime) {
                return true;
            } else {
                // 잠금 시간이 지났으면 삭제
                redisTemplate.delete(lockKey);
                return false;
            }
        } catch (NumberFormatException e) {
            log.error("Invalid lock time format for key: {}", key, e);
            redisTemplate.delete(lockKey);
            return false;
        }
    }

    @Override
    public int getAttemptCount(String key) {
        String countKey = COUNT_PREFIX + key;
        Object countObj = redisTemplate.opsForValue().get(countKey);

        if (countObj == null) {
            return 0;
        }

        try {
            return Integer.parseInt(countObj.toString());
        } catch (NumberFormatException e) {
            log.error("Invalid attempt count format for key: {}", key, e);
            return 0;
        }
    }

    @Override
    public long getRemainingLockTime(String key) {
        String lockKey = LOCK_PREFIX + key;
        Object lockTimeObj = redisTemplate.opsForValue().get(lockKey);

        if (lockTimeObj == null) {
            return 0;
        }

        try {
            long unlockTime = Long.parseLong(lockTimeObj.toString());
            long now = Instant.now().toEpochMilli();
            long remainingMs = unlockTime - now;

            return remainingMs > 0 ? (remainingMs / 1000) : 0;
        } catch (NumberFormatException e) {
            log.error("Invalid lock time format for key: {}", key, e);
            return 0;
        }
    }

    /**
     * 실패 횟수에 따른 잠금 시간을 결정합니다.
     *
     * @param attemptCount 현재 실패 횟수
     * @return 잠금 시간(초), 잠금하지 않으면 0
     */
    private int determineLockDuration(int attemptCount) {
        if (attemptCount >= THRESHOLD_THIRD) {
            return LOCK_TIME_THIRD;
        } else if (attemptCount >= THRESHOLD_SECOND) {
            return LOCK_TIME_SECOND;
        } else if (attemptCount >= THRESHOLD_FIRST) {
            return LOCK_TIME_FIRST;
        }
        return 0;
    }
}
