package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.LocalIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LoginAttemptService 통합 테스트
 * Testcontainers Redis 환경에서 실행됩니다.
 * Phase 4-3: Lua Script 원자성 검증
 */
class LoginAttemptServiceIntegrationTest extends LocalIntegrationTest {

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private String testKey;

    @BeforeEach
    void setUp() {
        testKey = "test-ip:" + UUID.randomUUID() + "@test.com";
    }

    @Nested
    @DisplayName("실패 기록 및 카운트")
    class RecordFailure {

        @Test
        @DisplayName("should_incrementCount_when_failureRecorded")
        void should_incrementCount_when_failureRecorded() {
            loginAttemptService.recordFailure(testKey);
            assertThat(loginAttemptService.getAttemptCount(testKey)).isEqualTo(1);

            loginAttemptService.recordFailure(testKey);
            assertThat(loginAttemptService.getAttemptCount(testKey)).isEqualTo(2);
        }

        @Test
        @DisplayName("should_lockAfter3Failures - Lua Script 원자성")
        void should_lockAfter3Failures() {
            // 3회 실패
            for (int i = 0; i < 3; i++) {
                loginAttemptService.recordFailure(testKey);
            }

            assertThat(loginAttemptService.isBlocked(testKey)).isTrue();
            assertThat(loginAttemptService.getRemainingLockTime(testKey)).isGreaterThan(0);
        }

        @Test
        @DisplayName("should_notLockBefore3Failures")
        void should_notLockBefore3Failures() {
            loginAttemptService.recordFailure(testKey);
            loginAttemptService.recordFailure(testKey);

            assertThat(loginAttemptService.isBlocked(testKey)).isFalse();
        }
    }

    @Nested
    @DisplayName("성공 기록")
    class RecordSuccess {

        @Test
        @DisplayName("should_resetCountAndUnlock_when_successRecorded")
        void should_resetCountAndUnlock_when_successRecorded() {
            // 3회 실패 → 잠금
            for (int i = 0; i < 3; i++) {
                loginAttemptService.recordFailure(testKey);
            }
            assertThat(loginAttemptService.isBlocked(testKey)).isTrue();

            // 성공 기록 → 잠금 해제
            loginAttemptService.recordSuccess(testKey);

            assertThat(loginAttemptService.isBlocked(testKey)).isFalse();
            assertThat(loginAttemptService.getAttemptCount(testKey)).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("잠금 정책 단계별 검증")
    class LockPolicy {

        @Test
        @DisplayName("should_lockFor1Minute_when_3Failures")
        void should_lockFor1Minute_when_3Failures() {
            for (int i = 0; i < 3; i++) {
                loginAttemptService.recordFailure(testKey);
            }

            long remaining = loginAttemptService.getRemainingLockTime(testKey);
            // 1분(60초) 이내여야 함
            assertThat(remaining).isGreaterThan(0).isLessThanOrEqualTo(60);
        }

        @Test
        @DisplayName("should_lockFor5Minutes_when_5Failures")
        void should_lockFor5Minutes_when_5Failures() {
            for (int i = 0; i < 5; i++) {
                loginAttemptService.recordFailure(testKey);
            }

            long remaining = loginAttemptService.getRemainingLockTime(testKey);
            // 5분(300초) 이내여야 함, 1분(60초)보다 커야 함
            assertThat(remaining).isGreaterThan(60).isLessThanOrEqualTo(300);
        }

        @Test
        @DisplayName("should_lockFor30Minutes_when_10Failures")
        void should_lockFor30Minutes_when_10Failures() {
            for (int i = 0; i < 10; i++) {
                loginAttemptService.recordFailure(testKey);
            }

            long remaining = loginAttemptService.getRemainingLockTime(testKey);
            // 30분(1800초) 이내여야 함, 5분(300초)보다 커야 함
            assertThat(remaining).isGreaterThan(300).isLessThanOrEqualTo(1800);
        }
    }

    @Nested
    @DisplayName("Lua Script 원자성 - Phase 4-3")
    class LuaScriptAtomicity {

        @Test
        @DisplayName("should_atomicallyIncrementAndExpire_onFirstFailure")
        void should_atomicallyIncrementAndExpire_onFirstFailure() {
            loginAttemptService.recordFailure(testKey);

            // INCR와 EXPIRE가 원자적으로 실행되었는지 검증
            // count가 1이면 TTL이 설정되어야 함
            assertThat(loginAttemptService.getAttemptCount(testKey)).isEqualTo(1);

            // Redis에 TTL이 설정되어 있는지 확인
            String countKey = "login_attempt:count:" + testKey;
            Long ttl = redisTemplate.getExpire(countKey);
            assertThat(ttl).isNotNull().isGreaterThan(0);
        }

        @Test
        @DisplayName("should_maintainAccurateCount_afterMultipleFailures")
        void should_maintainAccurateCount_afterMultipleFailures() {
            int expectedCount = 7;
            for (int i = 0; i < expectedCount; i++) {
                loginAttemptService.recordFailure(testKey);
            }

            assertThat(loginAttemptService.getAttemptCount(testKey)).isEqualTo(expectedCount);
        }
    }
}
