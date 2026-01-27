package com.portal.universe.authservice.user.service;

import com.portal.universe.authservice.user.dto.profile.ChangePasswordRequest;
import com.portal.universe.authservice.user.dto.profile.DeleteAccountRequest;
import com.portal.universe.authservice.user.dto.profile.ProfileResponse;
import com.portal.universe.authservice.user.dto.profile.UpdateProfileRequest;
import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.user.domain.UserProfile;
import com.portal.universe.authservice.common.exception.AuthErrorCode;
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

    /**
     * 사용자 프로필을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 프로필 응답 DTO
     */
    public ProfileResponse getProfile(Long userId) {
        User user = findUserById(userId);
        return ProfileResponse.from(user);
    }

    /**
     * 사용자 프로필을 수정합니다.
     *
     * @param userId 사용자 ID
     * @param request 수정 요청 DTO
     * @return 수정된 프로필 응답 DTO
     */
    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserById(userId);
        UserProfile profile = user.getProfile();

        // 각 필드가 null이 아닌 경우에만 업데이트
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

        log.info("Profile updated for user: {}", userId);
        return ProfileResponse.from(user);
    }

    /**
     * 비밀번호를 변경합니다.
     * 소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.
     *
     * @param userId 사용자 ID
     * @param request 비밀번호 변경 요청 DTO
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUserById(userId);

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

        // 비밀번호 변경
        String encodedNewPassword = passwordEncoder.encode(request.newPassword());
        user.changePassword(encodedNewPassword);

        log.info("Password changed for user: {}", userId);
    }

    /**
     * 회원 탈퇴를 처리합니다. (Soft Delete)
     * 소셜 로그인 사용자의 경우 비밀번호 확인 없이 탈퇴 가능합니다.
     *
     * @param userId 사용자 ID
     * @param request 탈퇴 요청 DTO
     */
    @Transactional
    public void deleteAccount(Long userId, DeleteAccountRequest request) {
        User user = findUserById(userId);

        // 소셜 로그인 사용자가 아닌 경우 비밀번호 확인
        if (!user.isSocialUser()) {
            if (!passwordEncoder.matches(request.password(), user.getPassword())) {
                throw new CustomBusinessException(AuthErrorCode.INVALID_PASSWORD);
            }
        }

        // Soft Delete - 상태를 WITHDRAWAL_PENDING으로 변경
        user.markForWithdrawal();

        log.info("Account marked for withdrawal: userId={}, reason={}", userId, request.reason());
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.USER_NOT_FOUND));
    }
}
