package com.portal.universe.authservice.user.service;

import com.portal.universe.authservice.common.exception.AuthErrorCode;
import com.portal.universe.authservice.password.PasswordValidator;
import com.portal.universe.authservice.password.ValidationResult;
import com.portal.universe.authservice.password.repository.PasswordHistoryRepository;
import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.user.domain.UserProfile;
import com.portal.universe.authservice.user.domain.UserStatus;
import com.portal.universe.authservice.user.dto.profile.ChangePasswordRequest;
import com.portal.universe.authservice.user.dto.profile.DeleteAccountRequest;
import com.portal.universe.authservice.user.dto.profile.ProfileResponse;
import com.portal.universe.authservice.user.dto.profile.UpdateProfileRequest;
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
@DisplayName("ProfileService 테스트")
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordValidator passwordValidator;

    @Mock
    private PasswordHistoryRepository passwordHistoryRepository;

    @InjectMocks
    private ProfileService profileService;

    private static final String UUID = "test-uuid-1234";
    private static final String EMAIL = "test@example.com";

    private User createTestUser() {
        User user = new User(EMAIL, "encodedPassword");
        try {
            var uuidField = User.class.getDeclaredField("uuid");
            uuidField.setAccessible(true);
            uuidField.set(user, UUID);
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        UserProfile profile = new UserProfile(user, "testNick", "realName", true);
        user.setProfile(profile);
        return user;
    }

    @Nested
    @DisplayName("getProfile")
    class GetProfile {

        @Test
        @DisplayName("should_returnProfileResponse_when_userExists")
        void should_returnProfileResponse_when_userExists() {
            // given
            User user = createTestUser();
            when(userRepository.findByUuid(UUID)).thenReturn(Optional.of(user));

            // when
            ProfileResponse response = profileService.getProfile(UUID);

            // then
            assertThat(response.uuid()).isEqualTo(UUID);
            assertThat(response.email()).isEqualTo(EMAIL);
            assertThat(response.nickname()).isEqualTo("testNick");
        }

        @Test
        @DisplayName("should_throwException_when_userNotFound")
        void should_throwException_when_userNotFound() {
            // given
            when(userRepository.findByUuid(UUID)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> profileService.getProfile(UUID))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.USER_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        @Test
        @DisplayName("should_updateProfile_when_validRequest")
        void should_updateProfile_when_validRequest() {
            // given
            User user = createTestUser();
            when(userRepository.findByUuid(UUID)).thenReturn(Optional.of(user));
            UpdateProfileRequest request = new UpdateProfileRequest(
                    "newNick", "newRealName", "010-1234-5678", "http://image.url", true
            );

            // when
            User updatedUser = profileService.updateProfile(UUID, request);

            // then
            assertThat(updatedUser.getProfile().getNickname()).isEqualTo("newNick");
            assertThat(updatedUser.getProfile().getRealName()).isEqualTo("newRealName");
            assertThat(updatedUser.getProfile().getPhoneNumber()).isEqualTo("010-1234-5678");
        }

        @Test
        @DisplayName("should_updateOnlyNonNullFields_when_partialRequest")
        void should_updateOnlyNonNullFields_when_partialRequest() {
            // given
            User user = createTestUser();
            when(userRepository.findByUuid(UUID)).thenReturn(Optional.of(user));
            UpdateProfileRequest request = new UpdateProfileRequest(
                    "updatedNick", null, null, null, null
            );

            // when
            User updatedUser = profileService.updateProfile(UUID, request);

            // then
            assertThat(updatedUser.getProfile().getNickname()).isEqualTo("updatedNick");
            assertThat(updatedUser.getProfile().getRealName()).isEqualTo("realName"); // unchanged
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("should_changePassword_when_validRequest")
        void should_changePassword_when_validRequest() {
            // given
            User user = createTestUser();
            when(userRepository.findByUuid(UUID)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("currentPwd", "encodedPassword")).thenReturn(true);
            when(passwordValidator.validate(eq("NewPassword1!"), any(User.class)))
                    .thenReturn(ValidationResult.success());
            when(passwordEncoder.encode("NewPassword1!")).thenReturn("newEncodedPassword");

            ChangePasswordRequest request = new ChangePasswordRequest(
                    "currentPwd", "NewPassword1!", "NewPassword1!"
            );

            // when
            profileService.changePassword(UUID, request);

            // then
            verify(passwordHistoryRepository).save(any());
        }

        @Test
        @DisplayName("should_throwException_when_socialUser")
        void should_throwException_when_socialUser() {
            // given - social user has null password and social accounts
            User user = new User(EMAIL, null);
            try {
                var uuidField = User.class.getDeclaredField("uuid");
                uuidField.setAccessible(true);
                uuidField.set(user, UUID);
                // Add a social account to make isSocialUser() return true
                var socialField = User.class.getDeclaredField("socialAccounts");
                socialField.setAccessible(true);
                var socialAccounts = new java.util.ArrayList<>();
                socialAccounts.add(new Object()); // mock social account
                socialField.set(user, socialAccounts);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            UserProfile profile = new UserProfile(user, "social", "Social User", false);
            user.setProfile(profile);
            when(userRepository.findByUuid(UUID)).thenReturn(Optional.of(user));

            ChangePasswordRequest request = new ChangePasswordRequest(
                    "currentPwd", "NewPassword1!", "NewPassword1!"
            );

            // when & then
            assertThatThrownBy(() -> profileService.changePassword(UUID, request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.SOCIAL_USER_CANNOT_CHANGE_PASSWORD);
                    });
        }

        @Test
        @DisplayName("should_throwException_when_currentPasswordIncorrect")
        void should_throwException_when_currentPasswordIncorrect() {
            // given
            User user = createTestUser();
            when(userRepository.findByUuid(UUID)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongPwd", "encodedPassword")).thenReturn(false);

            ChangePasswordRequest request = new ChangePasswordRequest(
                    "wrongPwd", "NewPassword1!", "NewPassword1!"
            );

            // when & then
            assertThatThrownBy(() -> profileService.changePassword(UUID, request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_CURRENT_PASSWORD);
                    });
        }

        @Test
        @DisplayName("should_throwException_when_passwordMismatch")
        void should_throwException_when_passwordMismatch() {
            // given
            User user = createTestUser();
            when(userRepository.findByUuid(UUID)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("currentPwd", "encodedPassword")).thenReturn(true);

            ChangePasswordRequest request = new ChangePasswordRequest(
                    "currentPwd", "NewPassword1!", "DifferentPassword!"
            );

            // when & then
            assertThatThrownBy(() -> profileService.changePassword(UUID, request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.PASSWORD_MISMATCH);
                    });
        }

        @Test
        @DisplayName("should_throwException_when_newPasswordTooWeak")
        void should_throwException_when_newPasswordTooWeak() {
            // given
            User user = createTestUser();
            when(userRepository.findByUuid(UUID)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("currentPwd", "encodedPassword")).thenReturn(true);
            when(passwordValidator.validate(eq("weakpwd"), any(User.class)))
                    .thenReturn(ValidationResult.failure("Too weak"));

            ChangePasswordRequest request = new ChangePasswordRequest(
                    "currentPwd", "weakpwd", "weakpwd"
            );

            // when & then
            assertThatThrownBy(() -> profileService.changePassword(UUID, request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.PASSWORD_TOO_WEAK);
                    });
        }
    }

    @Nested
    @DisplayName("deleteAccount")
    class DeleteAccount {

        @Test
        @DisplayName("should_markForWithdrawal_when_passwordCorrect")
        void should_markForWithdrawal_when_passwordCorrect() {
            // given
            User user = createTestUser();
            when(userRepository.findByUuid(UUID)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);

            DeleteAccountRequest request = new DeleteAccountRequest("password", "leaving");

            // when
            profileService.deleteAccount(UUID, request);

            // then
            assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWAL_PENDING);
        }

        @Test
        @DisplayName("should_throwException_when_passwordIncorrect")
        void should_throwException_when_passwordIncorrect() {
            // given
            User user = createTestUser();
            when(userRepository.findByUuid(UUID)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongPwd", "encodedPassword")).thenReturn(false);

            DeleteAccountRequest request = new DeleteAccountRequest("wrongPwd", "leaving");

            // when & then
            assertThatThrownBy(() -> profileService.deleteAccount(UUID, request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_PASSWORD);
                    });
        }
    }
}
