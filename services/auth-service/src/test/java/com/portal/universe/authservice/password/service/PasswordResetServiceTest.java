package com.portal.universe.authservice.password.service;

import com.portal.universe.authservice.auth.service.RefreshTokenService;
import com.portal.universe.authservice.common.event.ResilientKafkaPublisher;
import com.portal.universe.authservice.common.exception.AuthErrorCode;
import com.portal.universe.authservice.password.PasswordValidator;
import com.portal.universe.authservice.password.ValidationResult;
import com.portal.universe.authservice.password.dto.ResetPasswordRequest;
import com.portal.universe.authservice.password.repository.PasswordHistoryRepository;
import com.portal.universe.authservice.support.fixture.UserFixture;
import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.user.repository.UserRepository;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService 테스트")
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PasswordValidator passwordValidator;
    @Mock
    private PasswordHistoryRepository passwordHistoryRepository;
    @Mock
    private PasswordResetTokenService tokenService;
    @Mock
    private PasswordResetRateLimitService rateLimitService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private ResilientKafkaPublisher kafkaPublisher;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private static final String EMAIL = "test@example.com";
    private static final String CLIENT_IP = "127.0.0.1";
    private static final String USER_UUID = "test-uuid";

    @Nested
    @DisplayName("requestPasswordReset")
    class RequestPasswordReset {

        @Test
        @DisplayName("should_publishEvent_when_userExists")
        void should_publishEvent_when_userExists() {
            // given
            User user = UserFixture.builder().uuid(USER_UUID).email(EMAIL).build();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(tokenService.generateToken(USER_UUID)).thenReturn("reset-token");

            // when
            passwordResetService.requestPasswordReset(EMAIL, CLIENT_IP);

            // then
            verify(rateLimitService).checkRateLimit(CLIENT_IP);
            verify(tokenService).generateToken(USER_UUID);
            verify(kafkaPublisher).send(any(), eq(USER_UUID), any());
        }

        @Test
        @DisplayName("should_silentlyReturn_when_emailNotFound")
        void should_silentlyReturn_when_emailNotFound() {
            // given — user enumeration 방지: 에러 없이 조용히 반환
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            // when
            passwordResetService.requestPasswordReset(EMAIL, CLIENT_IP);

            // then
            verify(rateLimitService).checkRateLimit(CLIENT_IP);
            verify(tokenService, never()).generateToken(any());
            verify(kafkaPublisher, never()).send(any(), any(), any());
        }

        @Test
        @DisplayName("should_silentlyReturn_when_socialUser")
        void should_silentlyReturn_when_socialUser() {
            // given
            User socialUser = UserFixture.createSocialUser();
            when(userRepository.findByEmail(any())).thenReturn(Optional.of(socialUser));

            // when
            passwordResetService.requestPasswordReset(EMAIL, CLIENT_IP);

            // then
            verify(tokenService, never()).generateToken(any());
        }

        @Test
        @DisplayName("should_throwException_when_rateLimitExceeded")
        void should_throwException_when_rateLimitExceeded() {
            // given
            doThrow(new CustomBusinessException(AuthErrorCode.PASSWORD_RESET_RATE_LIMITED))
                    .when(rateLimitService).checkRateLimit(CLIENT_IP);

            // when & then
            assertThatThrownBy(() -> passwordResetService.requestPasswordReset(EMAIL, CLIENT_IP))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.PASSWORD_RESET_RATE_LIMITED);
                    });
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        @Test
        @DisplayName("should_resetPassword_when_validRequest")
        void should_resetPassword_when_validRequest() {
            // given
            User user = UserFixture.builder().id(1L).uuid(USER_UUID).email(EMAIL).build();
            ResetPasswordRequest request = new ResetPasswordRequest("valid-token", "NewPass1!", "NewPass1!");

            when(tokenService.getUserId("valid-token")).thenReturn(USER_UUID);
            when(userRepository.findByUuid(USER_UUID)).thenReturn(Optional.of(user));
            when(passwordValidator.validate(eq("NewPass1!"), any(User.class)))
                    .thenReturn(ValidationResult.success());
            when(tokenService.validateAndConsume("valid-token")).thenReturn(USER_UUID);
            when(passwordEncoder.encode("NewPass1!")).thenReturn("encodedNewPass");

            // when
            passwordResetService.resetPassword(request);

            // then
            verify(passwordHistoryRepository).save(any());
            verify(refreshTokenService).deleteRefreshToken(USER_UUID);
        }

        @Test
        @DisplayName("should_throwException_when_invalidToken")
        void should_throwException_when_invalidToken() {
            // given
            ResetPasswordRequest request = new ResetPasswordRequest("bad-token", "NewPass1!", "NewPass1!");
            when(tokenService.getUserId("bad-token")).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> passwordResetService.resetPassword(request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_RESET_TOKEN);
                    });
        }

        @Test
        @DisplayName("should_throwException_when_passwordMismatch")
        void should_throwException_when_passwordMismatch() {
            // given
            ResetPasswordRequest request = new ResetPasswordRequest("valid-token", "NewPass1!", "Different!");
            when(tokenService.getUserId("valid-token")).thenReturn(USER_UUID);

            // when & then
            assertThatThrownBy(() -> passwordResetService.resetPassword(request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.PASSWORD_MISMATCH);
                    });
        }

        @Test
        @DisplayName("should_throwException_when_passwordTooWeak")
        void should_throwException_when_passwordTooWeak() {
            // given
            User user = UserFixture.builder().uuid(USER_UUID).email(EMAIL).build();
            ResetPasswordRequest request = new ResetPasswordRequest("valid-token", "weak", "weak");
            when(tokenService.getUserId("valid-token")).thenReturn(USER_UUID);
            when(userRepository.findByUuid(USER_UUID)).thenReturn(Optional.of(user));
            when(passwordValidator.validate(eq("weak"), any(User.class)))
                    .thenReturn(ValidationResult.failure("Too short"));

            // when & then
            assertThatThrownBy(() -> passwordResetService.resetPassword(request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.PASSWORD_TOO_WEAK);
                    });
        }

        @Test
        @DisplayName("should_throwException_when_tokenConsumedByAnotherRequest")
        void should_throwException_when_tokenConsumedByAnotherRequest() {
            // given — getUserId 성공했지만 validateAndConsume 시점에 이미 소모됨 (race condition)
            User user = UserFixture.builder().uuid(USER_UUID).email(EMAIL).build();
            ResetPasswordRequest request = new ResetPasswordRequest("valid-token", "NewPass1!", "NewPass1!");
            when(tokenService.getUserId("valid-token")).thenReturn(USER_UUID);
            when(userRepository.findByUuid(USER_UUID)).thenReturn(Optional.of(user));
            when(passwordValidator.validate(eq("NewPass1!"), any(User.class)))
                    .thenReturn(ValidationResult.success());
            when(tokenService.validateAndConsume("valid-token")).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> passwordResetService.resetPassword(request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_RESET_TOKEN);
                    });
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateToken {

        @Test
        @DisplayName("should_returnTrue_when_tokenValid")
        void should_returnTrue_when_tokenValid() {
            // given
            when(tokenService.isValid("valid-token")).thenReturn(true);

            // when
            boolean result = passwordResetService.validateToken("valid-token");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should_returnFalse_when_tokenInvalid")
        void should_returnFalse_when_tokenInvalid() {
            // given
            when(tokenService.isValid("expired-token")).thenReturn(false);

            // when
            boolean result = passwordResetService.validateToken("expired-token");

            // then
            assertThat(result).isFalse();
        }
    }
}
