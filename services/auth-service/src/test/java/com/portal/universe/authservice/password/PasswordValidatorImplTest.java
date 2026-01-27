package com.portal.universe.authservice.password;

import com.portal.universe.authservice.user.domain.Role;
import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.user.domain.UserProfile;
import com.portal.universe.authservice.password.config.PasswordPolicyProperties;
import com.portal.universe.authservice.password.domain.PasswordHistory;
import com.portal.universe.authservice.password.repository.PasswordHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordValidatorImpl 테스트")
class PasswordValidatorImplTest {

    @Mock
    private PasswordHistoryRepository passwordHistoryRepository;

    private PasswordValidator passwordValidator;
    private PasswordEncoder passwordEncoder;
    private PasswordPolicyProperties policyProperties;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        policyProperties = new PasswordPolicyProperties();
        // 기본 정책 설정
        policyProperties.setMinLength(8);
        policyProperties.setMaxLength(128);
        policyProperties.setRequireUppercase(true);
        policyProperties.setRequireLowercase(true);
        policyProperties.setRequireDigit(true);
        policyProperties.setRequireSpecialChar(true);
        policyProperties.setSpecialChars("!@#$%^&*()_+-=[]{}|;:,.<>?");
        policyProperties.setHistoryCount(5);
        policyProperties.setMaxAge(90);
        policyProperties.setPreventSequential(true);
        policyProperties.setPreventUserInfo(true);

        passwordValidator = new PasswordValidatorImpl(
                policyProperties,
                passwordHistoryRepository,
                passwordEncoder
        );
    }

    @Test
    @DisplayName("정책을 모두 만족하는 비밀번호는 검증에 성공한다")
    void validate_ValidPassword_Success() {
        // given
        String validPassword = "MyP@ssw0rd!";

        // when
        ValidationResult result = passwordValidator.validate(validPassword);

        // then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("최소 길이를 만족하지 않으면 검증에 실패한다")
    void validate_TooShort_Failure() {
        // given
        String shortPassword = "Ab1!";

        // when
        ValidationResult result = passwordValidator.validate(shortPassword);

        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("at least"));
    }

    @Test
    @DisplayName("대문자가 없으면 검증에 실패한다")
    void validate_NoUppercase_Failure() {
        // given
        String noUpperPassword = "myp@ssw0rd!";

        // when
        ValidationResult result = passwordValidator.validate(noUpperPassword);

        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("uppercase"));
    }

    @Test
    @DisplayName("소문자가 없으면 검증에 실패한다")
    void validate_NoLowercase_Failure() {
        // given
        String noLowerPassword = "MYP@SSW0RD!";

        // when
        ValidationResult result = passwordValidator.validate(noLowerPassword);

        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("lowercase"));
    }

    @Test
    @DisplayName("숫자가 없으면 검증에 실패한다")
    void validate_NoDigit_Failure() {
        // given
        String noDigitPassword = "MyP@ssword!";

        // when
        ValidationResult result = passwordValidator.validate(noDigitPassword);

        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("digit"));
    }

    @Test
    @DisplayName("특수문자가 없으면 검증에 실패한다")
    void validate_NoSpecialChar_Failure() {
        // given
        String noSpecialPassword = "MyPassw0rd";

        // when
        ValidationResult result = passwordValidator.validate(noSpecialPassword);

        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("special character"));
    }

    @Test
    @DisplayName("연속 문자가 포함되면 검증에 실패한다")
    void validate_Sequential_Failure() {
        // given
        String sequentialPassword = "MyP@ssw0rd123!";

        // when
        ValidationResult result = passwordValidator.validate(sequentialPassword);

        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("sequential"));
    }

    @Test
    @DisplayName("사용자 이메일이 포함되면 검증에 실패한다")
    void validate_ContainsEmail_Failure() {
        // given
        User user = new User("john@example.com", "encodedPassword", Role.USER);
        UserProfile profile = new UserProfile(user, "TestNick", "TestReal", false);
        user.setProfile(profile);

        // 비밀번호에 이메일 로컬 파트 "john"이 포함됨
        // 연속 문자 없이 검증하기 위해 다른 패턴 사용
        String passwordWithEmail = "John@Pass9!";

        when(passwordHistoryRepository.findRecentByUserId(any(), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

        // when
        ValidationResult result = passwordValidator.validate(passwordWithEmail, user);

        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("user information"));
    }

    @Test
    @DisplayName("최근 사용한 비밀번호를 재사용하면 검증에 실패한다")
    void validate_RecentlyUsed_Failure() {
        // given
        User user = new User("uniqueuser@example.com", "encodedPassword", Role.USER);
        UserProfile profile = new UserProfile(user, "UniqueNick", "UniqueReal", false);
        user.setProfile(profile);

        String password = "MyP@ssw0rd!";
        String encodedPassword = passwordEncoder.encode(password);

        PasswordHistory history = PasswordHistory.create(1L, encodedPassword);

        when(passwordHistoryRepository.findRecentByUserId(any(), any(PageRequest.class)))
                .thenReturn(List.of(history));

        // when
        ValidationResult result = passwordValidator.validate(password, user);

        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("reuse"));
    }

    @Test
    @DisplayName("비밀번호 만료 확인 - 만료되지 않은 경우")
    void isExpired_NotExpired_ReturnsFalse() {
        // given
        User user = new User("user@example.com", "encodedPassword", Role.USER);
        UserProfile profile = new UserProfile(user, "UserNick", "UserReal", false);
        user.setProfile(profile);

        // passwordChangedAt은 null이므로 createdAt 기준
        // createdAt은 엔티티가 저장될 때 자동 설정되지만 테스트에서는 최근으로 가정

        // when
        boolean expired = passwordValidator.isExpired(user);

        // then
        assertThat(expired).isFalse();
    }

    @Test
    @DisplayName("소셜 로그인 사용자는 비밀번호 만료 체크를 하지 않는다")
    void isExpired_SocialUser_ReturnsFalse() {
        // given
        User user = new User("user@example.com", null, Role.USER); // 비밀번호 null
        UserProfile profile = new UserProfile(user, "UserNick", "UserReal", false);
        user.setProfile(profile);

        // when
        boolean expired = passwordValidator.isExpired(user);

        // then
        assertThat(expired).isFalse();
    }
}
