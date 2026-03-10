package com.portal.universe.authservice.password.service;

import com.portal.universe.authservice.common.exception.AuthErrorCode;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetRateLimitService 테스트")
class PasswordResetRateLimitServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private PasswordResetRateLimitService rateLimitService;

    private static final String IP = "192.168.1.1";

    @Nested
    @DisplayName("checkRateLimit")
    class CheckRateLimit {

        @Test
        @DisplayName("should_pass_when_firstAttempt")
        void should_pass_when_firstAttempt() {
            // given
            ReflectionTestUtils.setField(rateLimitService, "maxAttempts", 3);
            ReflectionTestUtils.setField(rateLimitService, "windowSeconds", 300);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.increment("pwd_reset_rate:" + IP)).thenReturn(1L);

            // when — 예외 없이 통과해야 함
            rateLimitService.checkRateLimit(IP);

            // then — 첫 요청이므로 TTL 설정
            verify(redisTemplate).expire("pwd_reset_rate:" + IP, 300, TimeUnit.SECONDS);
        }

        @Test
        @DisplayName("should_pass_when_withinLimit")
        void should_pass_when_withinLimit() {
            // given
            ReflectionTestUtils.setField(rateLimitService, "maxAttempts", 3);
            ReflectionTestUtils.setField(rateLimitService, "windowSeconds", 300);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.increment("pwd_reset_rate:" + IP)).thenReturn(3L);

            // when — limit과 같으면 통과
            rateLimitService.checkRateLimit(IP);

            // then — 정상 통과, expire 호출 없음 (count > 1)
            verify(redisTemplate, never()).expire(anyString(), anyLong(), any());
        }

        @Test
        @DisplayName("should_throwException_when_exceedsLimit")
        void should_throwException_when_exceedsLimit() {
            // given
            ReflectionTestUtils.setField(rateLimitService, "maxAttempts", 3);
            ReflectionTestUtils.setField(rateLimitService, "windowSeconds", 300);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.increment("pwd_reset_rate:" + IP)).thenReturn(4L);

            // when & then
            assertThatThrownBy(() -> rateLimitService.checkRateLimit(IP))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.PASSWORD_RESET_RATE_LIMITED);
                    });
        }
    }
}
