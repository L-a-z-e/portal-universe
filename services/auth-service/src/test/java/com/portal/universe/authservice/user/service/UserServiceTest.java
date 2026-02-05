package com.portal.universe.authservice.user.service;

import com.portal.universe.authservice.common.exception.AuthErrorCode;
import com.portal.universe.authservice.follow.repository.FollowRepository;
import com.portal.universe.authservice.password.PasswordValidator;
import com.portal.universe.authservice.password.ValidationResult;
import com.portal.universe.authservice.password.domain.PasswordHistory;
import com.portal.universe.authservice.password.repository.PasswordHistoryRepository;
import com.portal.universe.authservice.oauth2.domain.SocialAccount;
import com.portal.universe.authservice.oauth2.domain.SocialProvider;
import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.user.domain.UserProfile;
import com.portal.universe.authservice.user.dto.SignupCommand;
import com.portal.universe.authservice.user.dto.UserProfileResponse;
import com.portal.universe.authservice.user.repository.UserRepository;
import com.portal.universe.authservice.auth.service.RbacInitializationService;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PasswordValidator passwordValidator;

    @Mock
    private PasswordHistoryRepository passwordHistoryRepository;

    @Mock
    private RbacInitializationService rbacInitializationService;

    @InjectMocks
    private UserService userService;

    private static final String EMAIL = "test@example.com";
    private static final String UUID = "test-uuid-1234";

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
        UserProfile profile = new UserProfile(user, "testNick", "realName", false);
        user.setProfile(profile);
        return user;
    }

    private User createSocialUser() {
        User user = new User(EMAIL, null);
        try {
            var uuidField = User.class.getDeclaredField("uuid");
            uuidField.setAccessible(true);
            uuidField.set(user, UUID);
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, 2L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        UserProfile profile = new UserProfile(user, "socialNick", "socialName", false);
        user.setProfile(profile);
        user.getSocialAccounts().add(new SocialAccount(user, SocialProvider.GOOGLE, "google-id-123"));
        return user;
    }

    @Nested
    @DisplayName("registerUser")
    class RegisterUser {

        @Test
        @DisplayName("should_registerUser_when_validCommand")
        void should_registerUser_when_validCommand() {
            // given
            SignupCommand command = new SignupCommand(EMAIL, "Password1!", "testNick", "realName", false);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            when(passwordValidator.validate("Password1!")).thenReturn(ValidationResult.success());
            when(passwordEncoder.encode("Password1!")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                try {
                    var idField = User.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(user, 1L);
                    var uuidField = User.class.getDeclaredField("uuid");
                    uuidField.setAccessible(true);
                    uuidField.set(user, UUID);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return user;
            });
            when(passwordHistoryRepository.save(any(PasswordHistory.class))).thenReturn(null);

            // when
            Long userId = userService.registerUser(command);

            // then
            assertThat(userId).isEqualTo(1L);
            verify(rbacInitializationService).initializeNewUser(UUID);
            verify(eventPublisher).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("should_throwException_when_emailAlreadyExists")
        void should_throwException_when_emailAlreadyExists() {
            // given
            SignupCommand command = new SignupCommand(EMAIL, "Password1!", "testNick", "realName", false);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(createTestUser()));

            // when & then
            assertThatThrownBy(() -> userService.registerUser(command))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.EMAIL_ALREADY_EXISTS);
                    });
        }

        @Test
        @DisplayName("should_throwException_when_passwordTooWeak")
        void should_throwException_when_passwordTooWeak() {
            // given
            SignupCommand command = new SignupCommand(EMAIL, "weak", "testNick", "realName", false);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            when(passwordValidator.validate("weak"))
                    .thenReturn(ValidationResult.failure("Password too short"));

            // when & then
            assertThatThrownBy(() -> userService.registerUser(command))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.PASSWORD_TOO_WEAK);
                    });
        }
    }

    @Nested
    @DisplayName("getProfileByUsername")
    class GetProfileByUsername {

        @Test
        @DisplayName("should_returnProfile_when_usernameExists")
        void should_returnProfile_when_usernameExists() {
            // given
            User user = createTestUser();
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(followRepository.countByFollowing(user)).thenReturn(10L);
            when(followRepository.countByFollower(user)).thenReturn(5L);

            // when
            UserProfileResponse response = userService.getProfileByUsername("testuser");

            // then
            assertThat(response.email()).isEqualTo(EMAIL);
            assertThat(response.followerCount()).isEqualTo(10);
            assertThat(response.followingCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("should_throwException_when_usernameNotFound")
        void should_throwException_when_usernameNotFound() {
            // given
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getProfileByUsername("unknown"))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.USER_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("setUsernameByUuid")
    class SetUsernameByUuid {

        @Test
        @DisplayName("should_setUsername_when_validAndNotAlreadySet")
        void should_setUsername_when_validAndNotAlreadySet() {
            // given
            User user = createTestUser();
            when(userRepository.findByUuid(UUID)).thenReturn(Optional.of(user));
            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(followRepository.countByFollowing(user)).thenReturn(0L);
            when(followRepository.countByFollower(user)).thenReturn(0L);

            // when
            UserProfileResponse response = userService.setUsernameByUuid(UUID, "newuser");

            // then
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("should_throwException_when_invalidUsernameFormat")
        void should_throwException_when_invalidUsernameFormat() {
            // given - username with uppercase letters
            assertThatThrownBy(() -> userService.setUsernameByUuid(UUID, "INVALID"))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_USERNAME_FORMAT);
                    });
        }

        @Test
        @DisplayName("should_throwException_when_usernameAlreadySet")
        void should_throwException_when_usernameAlreadySet() {
            // given
            User user = createTestUser();
            user.getProfile().setUsername("existinguser");
            when(userRepository.findByUuid(UUID)).thenReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> userService.setUsernameByUuid(UUID, "newuser"))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.USERNAME_ALREADY_SET);
                    });
        }

        @Test
        @DisplayName("should_throwException_when_usernameAlreadyExists")
        void should_throwException_when_usernameAlreadyExists() {
            // given
            User user = createTestUser();
            when(userRepository.findByUuid(UUID)).thenReturn(Optional.of(user));
            when(userRepository.existsByUsername("taken")).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.setUsernameByUuid(UUID, "taken"))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.USERNAME_ALREADY_EXISTS);
                    });
        }
    }

    @Nested
    @DisplayName("checkUsernameAvailability")
    class CheckUsernameAvailability {

        @Test
        @DisplayName("should_returnTrue_when_usernameAvailable")
        void should_returnTrue_when_usernameAvailable() {
            // given
            when(userRepository.existsByUsername("available")).thenReturn(false);

            // when
            boolean result = userService.checkUsernameAvailability("available");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should_returnFalse_when_usernameTaken")
        void should_returnFalse_when_usernameTaken() {
            // given
            when(userRepository.existsByUsername("taken")).thenReturn(true);

            // when
            boolean result = userService.checkUsernameAvailability("taken");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should_throwException_when_invalidFormat")
        void should_throwException_when_invalidFormat() {
            // when & then
            assertThatThrownBy(() -> userService.checkUsernameAvailability("AB"))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("changePasswordByUuid")
    class ChangePasswordByUuid {

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

            // when
            userService.changePasswordByUuid(UUID, "currentPwd", "NewPassword1!");

            // then
            verify(passwordHistoryRepository).save(any(PasswordHistory.class));
        }

        @Test
        @DisplayName("should_throwException_when_currentPasswordInvalid")
        void should_throwException_when_currentPasswordInvalid() {
            // given
            User user = createTestUser();
            when(userRepository.findByUuid(UUID)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongPwd", "encodedPassword")).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> userService.changePasswordByUuid(UUID, "wrongPwd", "NewPassword1!"))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_CURRENT_PASSWORD);
                    });
        }

        @Test
        @DisplayName("should_throwException_when_socialUser")
        void should_throwException_when_socialUser() {
            // given
            User socialUser = createSocialUser();
            when(userRepository.findByUuid(UUID)).thenReturn(Optional.of(socialUser));

            // when & then
            assertThatThrownBy(() -> userService.changePasswordByUuid(UUID, "currentPwd", "NewPassword1!"))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> {
                        CustomBusinessException cbe = (CustomBusinessException) ex;
                        assertThat(cbe.getErrorCode()).isEqualTo(AuthErrorCode.SOCIAL_USER_CANNOT_CHANGE_PASSWORD);
                    });
        }
    }
}
