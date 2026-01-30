package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.LocalIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RefreshTokenService 통합 테스트
 * Testcontainers Redis 환경에서 실행됩니다.
 * Phase 2-1: Refresh Token Rotation 원자성 검증
 */
class RefreshTokenServiceIntegrationTest extends LocalIntegrationTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Nested
    @DisplayName("Refresh Token CRUD")
    class CrudOperations {

        @Test
        @DisplayName("should_saveAndRetrieveToken")
        void should_saveAndRetrieveToken() {
            String userId = UUID.randomUUID().toString();
            String token = "test-refresh-token-" + UUID.randomUUID();

            refreshTokenService.saveRefreshToken(userId, token);

            String retrieved = refreshTokenService.getRefreshToken(userId);
            assertThat(retrieved).isEqualTo(token);
        }

        @Test
        @DisplayName("should_returnNull_when_tokenNotExists")
        void should_returnNull_when_tokenNotExists() {
            String result = refreshTokenService.getRefreshToken("non-existent-user");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should_deleteToken")
        void should_deleteToken() {
            String userId = UUID.randomUUID().toString();
            String token = "to-delete-token";

            refreshTokenService.saveRefreshToken(userId, token);
            refreshTokenService.deleteRefreshToken(userId);

            String result = refreshTokenService.getRefreshToken(userId);
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Refresh Token Validation")
    class Validation {

        @Test
        @DisplayName("should_returnTrue_when_tokenMatches")
        void should_returnTrue_when_tokenMatches() {
            String userId = UUID.randomUUID().toString();
            String token = "valid-token";

            refreshTokenService.saveRefreshToken(userId, token);

            assertThat(refreshTokenService.validateRefreshToken(userId, token)).isTrue();
        }

        @Test
        @DisplayName("should_returnFalse_when_tokenMismatch")
        void should_returnFalse_when_tokenMismatch() {
            String userId = UUID.randomUUID().toString();

            refreshTokenService.saveRefreshToken(userId, "stored-token");

            assertThat(refreshTokenService.validateRefreshToken(userId, "wrong-token")).isFalse();
        }

        @Test
        @DisplayName("should_returnFalse_when_noTokenStored")
        void should_returnFalse_when_noTokenStored() {
            assertThat(refreshTokenService.validateRefreshToken("no-user", "any-token")).isFalse();
        }
    }

    @Nested
    @DisplayName("Refresh Token Rotation - Phase 2-1 원자적 교체 검증")
    class RotateRefreshToken {

        @Test
        @DisplayName("should_rotateSuccessfully_when_oldTokenMatches")
        void should_rotateSuccessfully_when_oldTokenMatches() {
            String userId = UUID.randomUUID().toString();
            String oldToken = "old-refresh-token";
            String newToken = "new-refresh-token";

            refreshTokenService.saveRefreshToken(userId, oldToken);

            boolean result = refreshTokenService.rotateRefreshToken(userId, oldToken, newToken);

            assertThat(result).isTrue();
            assertThat(refreshTokenService.getRefreshToken(userId)).isEqualTo(newToken);
        }

        @Test
        @DisplayName("should_failRotation_when_oldTokenDoesNotMatch")
        void should_failRotation_when_oldTokenDoesNotMatch() {
            String userId = UUID.randomUUID().toString();
            String storedToken = "stored-token";
            String wrongOldToken = "wrong-old-token";
            String newToken = "new-token";

            refreshTokenService.saveRefreshToken(userId, storedToken);

            boolean result = refreshTokenService.rotateRefreshToken(userId, wrongOldToken, newToken);

            assertThat(result).isFalse();
            // 원래 토큰이 유지되어야 함
            assertThat(refreshTokenService.getRefreshToken(userId)).isEqualTo(storedToken);
        }

        @Test
        @DisplayName("should_failRotation_when_noTokenStored")
        void should_failRotation_when_noTokenStored() {
            String userId = UUID.randomUUID().toString();

            boolean result = refreshTokenService.rotateRefreshToken(userId, "old", "new");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should_invalidateOldToken_afterRotation - Phase 2-1 rotation 후 old token 무효화")
        void should_invalidateOldToken_afterRotation() {
            String userId = UUID.randomUUID().toString();
            String oldToken = "rotation-old-token";
            String newToken = "rotation-new-token";

            refreshTokenService.saveRefreshToken(userId, oldToken);

            // rotation 실행
            refreshTokenService.rotateRefreshToken(userId, oldToken, newToken);

            // old token으로 validate → false
            assertThat(refreshTokenService.validateRefreshToken(userId, oldToken)).isFalse();
            // new token으로 validate → true
            assertThat(refreshTokenService.validateRefreshToken(userId, newToken)).isTrue();
        }
    }
}
