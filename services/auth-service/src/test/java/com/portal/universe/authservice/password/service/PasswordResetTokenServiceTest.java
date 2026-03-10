package com.portal.universe.authservice.password.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetTokenService 테스트")
class PasswordResetTokenServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private PasswordResetTokenService passwordResetTokenService;

    private static final String USER_UUID = "test-uuid";

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("should_storeTokenInRedis_when_called")
        void should_storeTokenInRedis_when_called() {
            // given
            ReflectionTestUtils.setField(passwordResetTokenService, "tokenTtlMinutes", 30);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            // when
            String token = passwordResetTokenService.generateToken(USER_UUID);

            // then
            assertThat(token).isNotBlank();
            verify(valueOperations).set(
                    eq("pwd_reset:" + token), eq(USER_UUID), eq(30L), eq(TimeUnit.MINUTES));
        }
    }

    @Nested
    @DisplayName("validateAndConsume")
    class ValidateAndConsume {

        @Test
        @DisplayName("should_returnUserId_when_tokenValid")
        void should_returnUserId_when_tokenValid() {
            // given
            when(redisTemplate.execute(any(RedisScript.class), anyList()))
                    .thenReturn(USER_UUID);

            // when
            String result = passwordResetTokenService.validateAndConsume("valid-token");

            // then
            assertThat(result).isEqualTo(USER_UUID);
        }

        @Test
        @DisplayName("should_returnNull_when_tokenExpired")
        void should_returnNull_when_tokenExpired() {
            // given
            when(redisTemplate.execute(any(RedisScript.class), anyList()))
                    .thenReturn(null);

            // when
            String result = passwordResetTokenService.validateAndConsume("expired-token");

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getUserId")
    class GetUserId {

        @Test
        @DisplayName("should_returnUserId_when_tokenExists")
        void should_returnUserId_when_tokenExists() {
            // given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("pwd_reset:valid-token")).thenReturn(USER_UUID);

            // when
            String result = passwordResetTokenService.getUserId("valid-token");

            // then
            assertThat(result).isEqualTo(USER_UUID);
        }

        @Test
        @DisplayName("should_returnNull_when_tokenNotExists")
        void should_returnNull_when_tokenNotExists() {
            // given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("pwd_reset:invalid-token")).thenReturn(null);

            // when
            String result = passwordResetTokenService.getUserId("invalid-token");

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("isValid")
    class IsValid {

        @Test
        @DisplayName("should_returnTrue_when_keyExists")
        void should_returnTrue_when_keyExists() {
            // given
            when(redisTemplate.hasKey("pwd_reset:valid-token")).thenReturn(Boolean.TRUE);

            // when
            boolean result = passwordResetTokenService.isValid("valid-token");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should_returnFalse_when_keyNotExists")
        void should_returnFalse_when_keyNotExists() {
            // given
            when(redisTemplate.hasKey("pwd_reset:expired-token")).thenReturn(Boolean.FALSE);

            // when
            boolean result = passwordResetTokenService.isValid("expired-token");

            // then
            assertThat(result).isFalse();
        }
    }
}
