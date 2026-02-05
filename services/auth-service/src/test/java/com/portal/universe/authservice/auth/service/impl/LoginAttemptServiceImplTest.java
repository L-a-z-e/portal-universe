package com.portal.universe.authservice.auth.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginAttemptServiceImpl 테스트")
class LoginAttemptServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private LoginAttemptServiceImpl loginAttemptService;

    private static final String KEY = "192.168.1.1:test@example.com";
    private static final String COUNT_PREFIX = "login_attempt:count:";
    private static final String LOCK_PREFIX = "login_attempt:lock:";

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("recordFailure")
    class RecordFailure {

        @Test
        @DisplayName("should_incrementCount_when_failureRecorded")
        void should_incrementCount_when_failureRecorded() {
            // given
            when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
                    .thenReturn(1L);

            // when
            loginAttemptService.recordFailure(KEY);

            // then
            verify(redisTemplate).execute(any(DefaultRedisScript.class), anyList(), any());
        }

        @Test
        @DisplayName("should_setLock_when_thresholdReached_3attempts")
        void should_setLock_when_thresholdReached_3attempts() {
            // given
            when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
                    .thenReturn(3L);

            // when
            loginAttemptService.recordFailure(KEY);

            // then
            verify(valueOperations).set(
                    eq(LOCK_PREFIX + KEY),
                    anyString(),
                    eq(60L),
                    eq(java.util.concurrent.TimeUnit.SECONDS)
            );
        }

        @Test
        @DisplayName("should_setLongerLock_when_5attempts")
        void should_setLongerLock_when_5attempts() {
            // given
            when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
                    .thenReturn(5L);

            // when
            loginAttemptService.recordFailure(KEY);

            // then
            verify(valueOperations).set(
                    eq(LOCK_PREFIX + KEY),
                    anyString(),
                    eq(300L),
                    eq(java.util.concurrent.TimeUnit.SECONDS)
            );
        }

        @Test
        @DisplayName("should_setLongestLock_when_10attempts")
        void should_setLongestLock_when_10attempts() {
            // given
            when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
                    .thenReturn(10L);

            // when
            loginAttemptService.recordFailure(KEY);

            // then
            verify(valueOperations).set(
                    eq(LOCK_PREFIX + KEY),
                    anyString(),
                    eq(1800L),
                    eq(java.util.concurrent.TimeUnit.SECONDS)
            );
        }

        @Test
        @DisplayName("should_notSetLock_when_belowThreshold")
        void should_notSetLock_when_belowThreshold() {
            // given
            when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
                    .thenReturn(2L);

            // when
            loginAttemptService.recordFailure(KEY);

            // then
            verify(valueOperations, never()).set(
                    eq(LOCK_PREFIX + KEY),
                    anyString(),
                    anyLong(),
                    any(java.util.concurrent.TimeUnit.class)
            );
        }
    }

    @Nested
    @DisplayName("recordSuccess")
    class RecordSuccess {

        @Test
        @DisplayName("should_deleteCountAndLock_when_successRecorded")
        void should_deleteCountAndLock_when_successRecorded() {
            // when
            loginAttemptService.recordSuccess(KEY);

            // then
            verify(redisTemplate).delete(COUNT_PREFIX + KEY);
            verify(redisTemplate).delete(LOCK_PREFIX + KEY);
        }
    }

    @Nested
    @DisplayName("isBlocked")
    class IsBlocked {

        @Test
        @DisplayName("should_returnTrue_when_lockTimeInFuture")
        void should_returnTrue_when_lockTimeInFuture() {
            // given
            long futureTime = Instant.now().toEpochMilli() + 60_000;
            when(valueOperations.get(LOCK_PREFIX + KEY)).thenReturn(String.valueOf(futureTime));

            // when
            boolean result = loginAttemptService.isBlocked(KEY);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should_returnFalse_when_noLockExists")
        void should_returnFalse_when_noLockExists() {
            // given
            when(valueOperations.get(LOCK_PREFIX + KEY)).thenReturn(null);

            // when
            boolean result = loginAttemptService.isBlocked(KEY);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should_returnFalse_when_lockTimeInPast")
        void should_returnFalse_when_lockTimeInPast() {
            // given
            long pastTime = Instant.now().toEpochMilli() - 60_000;
            when(valueOperations.get(LOCK_PREFIX + KEY)).thenReturn(String.valueOf(pastTime));

            // when
            boolean result = loginAttemptService.isBlocked(KEY);

            // then
            assertThat(result).isFalse();
            verify(redisTemplate).delete(LOCK_PREFIX + KEY);
        }

        @Test
        @DisplayName("should_returnFalse_when_lockTimeFormatInvalid")
        void should_returnFalse_when_lockTimeFormatInvalid() {
            // given
            when(valueOperations.get(LOCK_PREFIX + KEY)).thenReturn("invalid-number");

            // when
            boolean result = loginAttemptService.isBlocked(KEY);

            // then
            assertThat(result).isFalse();
            verify(redisTemplate).delete(LOCK_PREFIX + KEY);
        }
    }

    @Nested
    @DisplayName("getAttemptCount")
    class GetAttemptCount {

        @Test
        @DisplayName("should_returnCount_when_countExists")
        void should_returnCount_when_countExists() {
            // given
            when(valueOperations.get(COUNT_PREFIX + KEY)).thenReturn("5");

            // when
            int count = loginAttemptService.getAttemptCount(KEY);

            // then
            assertThat(count).isEqualTo(5);
        }

        @Test
        @DisplayName("should_returnZero_when_noCount")
        void should_returnZero_when_noCount() {
            // given
            when(valueOperations.get(COUNT_PREFIX + KEY)).thenReturn(null);

            // when
            int count = loginAttemptService.getAttemptCount(KEY);

            // then
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("should_returnZero_when_countFormatInvalid")
        void should_returnZero_when_countFormatInvalid() {
            // given
            when(valueOperations.get(COUNT_PREFIX + KEY)).thenReturn("not-a-number");

            // when
            int count = loginAttemptService.getAttemptCount(KEY);

            // then
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("getRemainingLockTime")
    class GetRemainingLockTime {

        @Test
        @DisplayName("should_returnRemainingSeconds_when_locked")
        void should_returnRemainingSeconds_when_locked() {
            // given
            long futureTime = Instant.now().toEpochMilli() + 30_000; // 30 seconds from now
            when(valueOperations.get(LOCK_PREFIX + KEY)).thenReturn(String.valueOf(futureTime));

            // when
            long remaining = loginAttemptService.getRemainingLockTime(KEY);

            // then
            assertThat(remaining).isGreaterThan(0).isLessThanOrEqualTo(30);
        }

        @Test
        @DisplayName("should_returnZero_when_noLock")
        void should_returnZero_when_noLock() {
            // given
            when(valueOperations.get(LOCK_PREFIX + KEY)).thenReturn(null);

            // when
            long remaining = loginAttemptService.getRemainingLockTime(KEY);

            // then
            assertThat(remaining).isZero();
        }

        @Test
        @DisplayName("should_returnZero_when_lockExpired")
        void should_returnZero_when_lockExpired() {
            // given
            long pastTime = Instant.now().toEpochMilli() - 60_000;
            when(valueOperations.get(LOCK_PREFIX + KEY)).thenReturn(String.valueOf(pastTime));

            // when
            long remaining = loginAttemptService.getRemainingLockTime(KEY);

            // then
            assertThat(remaining).isZero();
        }
    }
}
