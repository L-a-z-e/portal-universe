package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.common.config.JwtProperties;
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

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService 테스트")
class RefreshTokenServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private static final String USER_ID = "test-user-uuid";
    private static final String TOKEN = "sample-refresh-token";
    private static final String KEY_PREFIX = "refresh_token:";
    private static final long EXPIRATION = 604_800_000L;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(jwtProperties.getRefreshTokenExpiration()).thenReturn(EXPIRATION);
    }

    @Nested
    @DisplayName("saveRefreshToken")
    class SaveRefreshToken {

        @Test
        @DisplayName("should_saveTokenToRedis_when_called")
        void should_saveTokenToRedis_when_called() {
            // when
            refreshTokenService.saveRefreshToken(USER_ID, TOKEN);

            // then
            verify(valueOperations).set(
                    eq(KEY_PREFIX + USER_ID),
                    eq(TOKEN),
                    eq(EXPIRATION),
                    eq(TimeUnit.MILLISECONDS)
            );
        }
    }

    @Nested
    @DisplayName("getRefreshToken")
    class GetRefreshToken {

        @Test
        @DisplayName("should_returnToken_when_tokenExists")
        void should_returnToken_when_tokenExists() {
            // given
            when(valueOperations.get(KEY_PREFIX + USER_ID)).thenReturn(TOKEN);

            // when
            String result = refreshTokenService.getRefreshToken(USER_ID);

            // then
            assertThat(result).isEqualTo(TOKEN);
        }

        @Test
        @DisplayName("should_returnNull_when_tokenNotExists")
        void should_returnNull_when_tokenNotExists() {
            // given
            when(valueOperations.get(KEY_PREFIX + USER_ID)).thenReturn(null);

            // when
            String result = refreshTokenService.getRefreshToken(USER_ID);

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("deleteRefreshToken")
    class DeleteRefreshToken {

        @Test
        @DisplayName("should_deleteFromRedis_when_called")
        void should_deleteFromRedis_when_called() {
            // when
            refreshTokenService.deleteRefreshToken(USER_ID);

            // then
            verify(redisTemplate).delete(KEY_PREFIX + USER_ID);
        }
    }

    @Nested
    @DisplayName("validateRefreshToken")
    class ValidateRefreshToken {

        @Test
        @DisplayName("should_returnTrue_when_tokenMatches")
        void should_returnTrue_when_tokenMatches() {
            // given
            when(valueOperations.get(KEY_PREFIX + USER_ID)).thenReturn(TOKEN);

            // when
            boolean result = refreshTokenService.validateRefreshToken(USER_ID, TOKEN);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should_returnFalse_when_tokenNotFound")
        void should_returnFalse_when_tokenNotFound() {
            // given
            when(valueOperations.get(KEY_PREFIX + USER_ID)).thenReturn(null);

            // when
            boolean result = refreshTokenService.validateRefreshToken(USER_ID, TOKEN);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should_returnFalse_when_tokenMismatch")
        void should_returnFalse_when_tokenMismatch() {
            // given
            when(valueOperations.get(KEY_PREFIX + USER_ID)).thenReturn("different-token");

            // when
            boolean result = refreshTokenService.validateRefreshToken(USER_ID, TOKEN);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("rotateRefreshToken")
    class RotateRefreshToken {

        @Test
        @DisplayName("should_returnTrue_when_rotationSucceeds")
        void should_returnTrue_when_rotationSucceeds() {
            // given
            String newToken = "new-refresh-token";
            when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any()))
                    .thenReturn(1L);

            // when
            boolean result = refreshTokenService.rotateRefreshToken(USER_ID, TOKEN, newToken);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should_returnFalse_when_rotationFails")
        void should_returnFalse_when_rotationFails() {
            // given
            String newToken = "new-refresh-token";
            when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any()))
                    .thenReturn(0L);

            // when
            boolean result = refreshTokenService.rotateRefreshToken(USER_ID, TOKEN, newToken);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should_returnFalse_when_resultIsNull")
        void should_returnFalse_when_resultIsNull() {
            // given
            String newToken = "new-refresh-token";
            when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any()))
                    .thenReturn(null);

            // when
            boolean result = refreshTokenService.rotateRefreshToken(USER_ID, TOKEN, newToken);

            // then
            assertThat(result).isFalse();
        }
    }
}
