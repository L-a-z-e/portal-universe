package com.portal.universe.authservice.user.service;

import com.portal.universe.authservice.user.dto.profile.ChangePasswordRequest;
import com.portal.universe.authservice.user.dto.profile.DeleteAccountRequest;
import com.portal.universe.authservice.user.dto.profile.ProfileResponse;
import com.portal.universe.authservice.user.dto.profile.UpdateProfileRequest;
import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.user.domain.UserProfile;
import com.portal.universe.authservice.common.exception.AuthErrorCode;
import com.portal.universe.authservice.password.PasswordValidator;
import com.portal.universe.authservice.password.ValidationResult;
import com.portal.universe.authservice.password.domain.PasswordHistory;
import com.portal.universe.authservice.password.repository.PasswordHistoryRepository;
import com.portal.universe.authservice.user.repository.UserRepository;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final PasswordHistoryRepository passwordHistoryRepository;

    /**
     * 사용자 프로필을 조회합니다.
     *
     * @param uuid 사용자 UUID
     * @return 프로필 응답 DTO
     */
    public ProfileResponse getProfile(String uuid) {
        User user = findUserByUuid(uuid);
        return ProfileResponse.from(user);
    }

    /**
     * 사용자 프로필을 수정합니다.
     * 수정된 User 엔티티를 반환하여 호출자가 추가 DB 조회 없이 사용할 수 있도록 합니다.
     *
     * @param uuid 사용자 UUID
     * @param request 수정 요청 DTO
     * @return 수정된 User 엔티티
     */
    @Transactional
    public User updateProfile(String uuid, UpdateProfileRequest request) {
        User user = findUserByUuid(uuid);
        UserProfile profile = user.getProfile();

        if (request.nickname() != null) {
            profile.updateNickname(request.nickname());
        }
        if (request.realName() != null) {
            profile.updateRealName(request.realName());
        }
        if (request.phoneNumber() != null) {
            profile.updatePhoneNumber(request.phoneNumber());
        }
        if (request.profileImageUrl() != null) {
            profile.updateProfileImageUrl(request.profileImageUrl());
        }
        if (request.marketingAgree() != null) {
            profile.updateMarketingAgree(request.marketingAgree());
        }

        log.info("Profile updated for user: {}", uuid);
        return user;
    }

    /**
     * 비밀번호를 변경합니다.
     * 소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.
     * 비밀번호 정책 검증 및 히스토리 관리를 수행합니다.
     *
     * @param uuid 사용자 UUID
     * @param request 비밀번호 변경 요청 DTO
     */
    @Transactional
    public void changePassword(String uuid, ChangePasswordRequest request) {
        User user = findUserByUuid(uuid);

        // 소셜 로그인 사용자 체크
        if (user.isSocialUser()) {
            throw new CustomBusinessException(AuthErrorCode.SOCIAL_USER_CANNOT_CHANGE_PASSWORD);
        }

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new CustomBusinessException(AuthErrorCode.INVALID_CURRENT_PASSWORD);
        }

        // 새 비밀번호 확인
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new CustomBusinessException(AuthErrorCode.PASSWORD_MISMATCH);
        }

        // 비밀번호 정책 검증 (길이, 복잡도, 사용자정보 포함, 이전 비밀번호 재사용)
        ValidationResult validationResult = passwordValidator.validate(request.newPassword(), user);
        if (!validationResult.isValid()) {
            String allErrors = String.join("; ", validationResult.getErrors());
            throw new CustomBusinessException(AuthErrorCode.PASSWORD_TOO_WEAK, allErrors);
        }

        // 비밀번호 변경
        String encodedNewPassword = passwordEncoder.encode(request.newPassword());
        user.changePassword(encodedNewPassword);

        // 비밀번호 히스토리 저장
        passwordHistoryRepository.save(PasswordHistory.create(user.getId(), encodedNewPassword));

        log.info("Password changed for user: {}", uuid);
    }

    /**
     * 회원 탈퇴를 처리합니다. (Soft Delete)
     * 소셜 로그인 사용자의 경우 비밀번호 확인 없이 탈퇴 가능합니다.
     *
     * @param uuid 사용자 UUID
     * @param request 탈퇴 요청 DTO
     */
    @Transactional
    public void deleteAccount(String uuid, DeleteAccountRequest request) {
        User user = findUserByUuid(uuid);

        // 소셜 로그인 사용자가 아닌 경우 비밀번호 확인
        if (!user.isSocialUser()) {
            if (!passwordEncoder.matches(request.password(), user.getPassword())) {
                throw new CustomBusinessException(AuthErrorCode.INVALID_PASSWORD);
            }
        }

        // Soft Delete - 상태를 WITHDRAWAL_PENDING으로 변경
        user.markForWithdrawal();

        log.info("Account marked for withdrawal: uuid={}, reason={}", uuid, request.reason());
    }

    private User findUserByUuid(String uuid) {
        return userRepository.findByUuid(uuid)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.USER_NOT_FOUND));
    }
}
