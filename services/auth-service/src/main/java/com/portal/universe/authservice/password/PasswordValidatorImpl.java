package com.portal.universe.authservice.password;

import com.portal.universe.authservice.config.PasswordPolicyProperties;
import com.portal.universe.authservice.domain.PasswordHistory;
import com.portal.universe.authservice.domain.User;
import com.portal.universe.authservice.repository.PasswordHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 비밀번호 정책 검증 구현체입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordValidatorImpl implements PasswordValidator {

    private final PasswordPolicyProperties policyProperties;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    // 연속 문자 패턴 (예: abc, 123, zzz)
    private static final Pattern SEQUENTIAL_CHARS = Pattern.compile("(.)\\1{2,}|" +
            "(?:abc|bcd|cde|def|efg|fgh|ghi|hij|ijk|jkl|klm|lmn|mno|nop|opq|pqr|qrs|rst|stu|tuv|uvw|vwx|wxy|xyz)|" +
            "(?:012|123|234|345|456|567|678|789)");

    @Override
    public ValidationResult validate(String password) {
        List<String> errors = new ArrayList<>();

        // 1. 길이 검증
        if (password == null || password.length() < policyProperties.getMinLength()) {
            errors.add("Password must be at least " + policyProperties.getMinLength() + " characters long");
        }

        if (password != null && password.length() > policyProperties.getMaxLength()) {
            errors.add("Password must not exceed " + policyProperties.getMaxLength() + " characters");
        }

        if (password == null) {
            return ValidationResult.failure(errors);
        }

        // 2. 복잡도 검증
        if (policyProperties.isRequireUppercase() && !containsUppercase(password)) {
            errors.add("Password must contain at least one uppercase letter");
        }

        if (policyProperties.isRequireLowercase() && !containsLowercase(password)) {
            errors.add("Password must contain at least one lowercase letter");
        }

        if (policyProperties.isRequireDigit() && !containsDigit(password)) {
            errors.add("Password must contain at least one digit");
        }

        if (policyProperties.isRequireSpecialChar() && !containsSpecialChar(password)) {
            errors.add("Password must contain at least one special character (" +
                    policyProperties.getSpecialChars() + ")");
        }

        // 3. 연속 문자 검증
        if (policyProperties.isPreventSequential() && containsSequential(password)) {
            errors.add("Password cannot contain sequential characters (e.g., abc, 123)");
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    @Override
    public ValidationResult validate(String password, User user) {
        // 1. 기본 정책 검증
        ValidationResult basicResult = validate(password);
        if (!basicResult.isValid()) {
            return basicResult;
        }

        List<String> errors = new ArrayList<>();

        // 2. 사용자 정보 포함 검증
        if (policyProperties.isPreventUserInfo() && containsUserInfo(password, user)) {
            errors.add("Password cannot contain user information (email, username)");
        }

        // 3. 이전 비밀번호 재사용 검증
        if (policyProperties.getHistoryCount() > 0 && isPasswordRecentlyUsed(password, user.getId())) {
            errors.add("Cannot reuse recently used passwords (last " +
                    policyProperties.getHistoryCount() + " passwords)");
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    @Override
    public boolean isExpired(User user) {
        if (policyProperties.getMaxAge() <= 0) {
            return false; // 만료 정책 비활성화
        }

        // 소셜 로그인 사용자는 비밀번호 만료 체크 안 함
        if (user.isSocialUser()) {
            return false;
        }

        // 비밀번호 변경일 기준으로 확인
        LocalDateTime passwordChangedAt = user.getPasswordChangedAt();
        if (passwordChangedAt == null) {
            // passwordChangedAt이 null이면 생성일 기준으로 확인
            passwordChangedAt = user.getCreatedAt();
        }

        if (passwordChangedAt == null) {
            return false;
        }

        long daysSinceChange = ChronoUnit.DAYS.between(passwordChangedAt, LocalDateTime.now());
        return daysSinceChange > policyProperties.getMaxAge();
    }

    // ==================== Private Helper Methods ====================

    /**
     * 대문자 포함 여부 확인
     */
    private boolean containsUppercase(String password) {
        return password.chars().anyMatch(Character::isUpperCase);
    }

    /**
     * 소문자 포함 여부 확인
     */
    private boolean containsLowercase(String password) {
        return password.chars().anyMatch(Character::isLowerCase);
    }

    /**
     * 숫자 포함 여부 확인
     */
    private boolean containsDigit(String password) {
        return password.chars().anyMatch(Character::isDigit);
    }

    /**
     * 특수문자 포함 여부 확인
     */
    private boolean containsSpecialChar(String password) {
        String specialChars = policyProperties.getSpecialChars();
        return password.chars()
                .mapToObj(c -> (char) c)
                .anyMatch(c -> specialChars.indexOf(c) >= 0);
    }

    /**
     * 연속 문자 포함 여부 확인
     */
    private boolean containsSequential(String password) {
        String lowerPassword = password.toLowerCase();
        return SEQUENTIAL_CHARS.matcher(lowerPassword).find();
    }

    /**
     * 사용자 정보 포함 여부 확인
     */
    private boolean containsUserInfo(String password, User user) {
        String lowerPassword = password.toLowerCase();

        // 이메일 로컬 파트 확인
        String emailLocal = user.getEmail().split("@")[0].toLowerCase();
        if (lowerPassword.contains(emailLocal)) {
            return true;
        }

        // Username 확인 (null 체크)
        if (user.getProfile() != null && user.getProfile().getUsername() != null) {
            String username = user.getProfile().getUsername().toLowerCase();
            if (lowerPassword.contains(username)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 최근 사용한 비밀번호 재사용 여부 확인
     */
    private boolean isPasswordRecentlyUsed(String password, Long userId) {
        int historyCount = policyProperties.getHistoryCount();
        if (historyCount <= 0) {
            return false;
        }

        List<PasswordHistory> recentPasswords = passwordHistoryRepository.findRecentByUserId(
                userId,
                PageRequest.of(0, historyCount)
        );

        for (PasswordHistory history : recentPasswords) {
            if (passwordEncoder.matches(password, history.getPasswordHash())) {
                log.warn("User {} attempted to reuse a recent password", userId);
                return true;
            }
        }

        return false;
    }
}
