package com.portal.universe.authservice.password.service;

import com.portal.universe.authservice.auth.service.RefreshTokenService;
import com.portal.universe.authservice.common.event.ResilientKafkaPublisher;
import com.portal.universe.authservice.common.exception.AuthErrorCode;
import com.portal.universe.authservice.password.PasswordValidator;
import com.portal.universe.authservice.password.ValidationResult;
import com.portal.universe.authservice.password.domain.PasswordHistory;
import com.portal.universe.authservice.password.dto.ResetPasswordRequest;
import com.portal.universe.authservice.password.repository.PasswordHistoryRepository;
import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.user.repository.UserRepository;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.event.auth.AuthTopics;
import com.portal.universe.event.auth.PasswordResetRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordResetTokenService tokenService;
    private final PasswordResetRateLimitService rateLimitService;
    private final RefreshTokenService refreshTokenService;
    private final ResilientKafkaPublisher kafkaPublisher;

    @Value("${security.password.reset.frontend-base-url:http://localhost:30000}")
    private String frontendBaseUrl;

    /**
     * 비밀번호 재설정을 요청합니다.
     * 이메일 존재 여부와 관계없이 동일한 응답을 반환합니다 (user enumeration 방지).
     *
     * @param email 사용자 이메일
     * @param clientIp 클라이언트 IP (rate limiting)
     */
    public void requestPasswordReset(String email, String clientIp) {
        rateLimitService.checkRateLimit(clientIp);

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.info("Password reset requested for non-existent email: {}", email);
            return;
        }

        User user = userOpt.get();

        // 소셜 전용 사용자는 비밀번호 재설정 불가 (silent skip)
        if (user.isSocialUser()) {
            log.info("Password reset skipped for social-only user: {}", user.getUuid());
            return;
        }

        String token = tokenService.generateToken(user.getUuid());
        String resetLink = frontendBaseUrl + "/reset-password?token=" + token;

        PasswordResetRequestedEvent event = PasswordResetRequestedEvent.newBuilder()
                .setUserId(user.getUuid())
                .setEmail(email)
                .setResetLink(resetLink)
                .setTimestamp(Instant.now())
                .build();

        kafkaPublisher.send(AuthTopics.PASSWORD_RESET_REQUESTED, user.getUuid(), event);
        log.info("Password reset event published for user: {}", user.getUuid());
    }

    /**
     * 비밀번호를 재설정합니다.
     *
     * @param request 재설정 요청 (토큰 + 새 비밀번호)
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // 1. 토큰에서 userId 조회 (소모하지 않음)
        String userId = tokenService.getUserId(request.token());
        if (userId == null) {
            throw new CustomBusinessException(AuthErrorCode.INVALID_RESET_TOKEN);
        }

        // 2. 비밀번호 일치 확인 (토큰 소모 전)
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new CustomBusinessException(AuthErrorCode.PASSWORD_MISMATCH);
        }

        // 3. 사용자 조회 + 비밀번호 정책 검증 (토큰 소모 전)
        User user = userRepository.findByUuid(userId)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.USER_NOT_FOUND));

        ValidationResult validationResult = passwordValidator.validate(request.newPassword(), user);
        if (!validationResult.isValid()) {
            String allErrors = String.join("; ", validationResult.getErrors());
            throw new CustomBusinessException(AuthErrorCode.PASSWORD_TOO_WEAK, allErrors);
        }

        // 4. 모든 검증 통과 후 토큰 소모 (원자적 GET+DELETE)
        String consumedUserId = tokenService.validateAndConsume(request.token());
        if (consumedUserId == null) {
            throw new CustomBusinessException(AuthErrorCode.INVALID_RESET_TOKEN);
        }

        // 5. 비밀번호 변경
        String encodedPassword = passwordEncoder.encode(request.newPassword());
        user.changePassword(encodedPassword);
        passwordHistoryRepository.save(PasswordHistory.create(user.getId(), encodedPassword));

        // 6. 기존 refresh token 삭제 → 재로그인 유도
        refreshTokenService.deleteRefreshToken(userId);

        log.info("Password reset completed for user: {}", userId);
    }

    /**
     * 토큰 유효성을 확인합니다 (소모하지 않음).
     *
     * @param token 비밀번호 재설정 토큰
     * @return 유효 여부
     */
    public boolean validateToken(String token) {
        return tokenService.isValid(token);
    }
}
