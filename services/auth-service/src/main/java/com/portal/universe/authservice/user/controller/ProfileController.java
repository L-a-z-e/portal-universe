package com.portal.universe.authservice.user.controller;

import com.portal.universe.authservice.user.dto.profile.ChangePasswordRequest;
import com.portal.universe.authservice.user.dto.profile.DeleteAccountRequest;
import com.portal.universe.authservice.user.dto.profile.ProfileResponse;
import com.portal.universe.authservice.user.dto.profile.UpdateProfileRequest;
import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.common.exception.AuthErrorCode;
import com.portal.universe.authservice.user.repository.UserRepository;
import com.portal.universe.authservice.common.util.TokenUtils;
import com.portal.universe.authservice.user.service.ProfileService;
import com.portal.universe.authservice.auth.service.RefreshTokenService;
import com.portal.universe.authservice.auth.service.TokenBlacklistService;
import com.portal.universe.authservice.auth.service.TokenService;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.commonlibrary.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 프로필 관련 API 컨트롤러
 * 프로필 조회, 수정, 비밀번호 변경, 회원 탈퇴 기능을 제공합니다.
 * 모든 API는 인증이 필요합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;

    /**
     * 내 프로필 조회 API
     *
     * @return 프로필 응답 DTO
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile() {
        Long userId = getCurrentUserId();
        log.info("Profile fetch for user: {}", userId);

        ProfileResponse response = profileService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 프로필 수정 API
     * 프로필 변경 시 새 Access Token을 함께 반환하여 JWT의 stale data를 방지합니다.
     *
     * @param request 수정 요청 DTO
     * @return 수정된 프로필 응답 DTO (새 accessToken 포함)
     */
    @PatchMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = getCurrentUserId();
        String userUuid = getCurrentUserUuid();
        log.info("Profile update for user: {}", userId);

        ProfileResponse response = profileService.updateProfile(userId, request);

        // 프로필 변경 후 새 Access Token 발급 (JWT에 닉네임 등이 포함되므로)
        User user = userRepository.findByUuidWithProfile(userUuid)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.USER_NOT_FOUND));
        String newAccessToken = tokenService.generateAccessToken(user);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "profile", response,
                "accessToken", newAccessToken
        )));
    }

    /**
     * 비밀번호 변경 API
     *
     * @param request 비밀번호 변경 요청 DTO
     * @return 성공 메시지
     */
    @PostMapping("/password")
    public ResponseEntity<ApiResponse<Map<String, String>>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        Long userId = getCurrentUserId();
        log.info("Password change for user: {}", userId);

        profileService.changePassword(userId, request);
        return ResponseEntity.ok(
                ApiResponse.success(Map.of("message", "비밀번호가 변경되었습니다")));
    }

    /**
     * 회원 탈퇴 API
     * 탈퇴 처리 후 토큰을 무효화합니다.
     *
     * @param authorization Authorization 헤더 (Bearer {accessToken})
     * @param request 탈퇴 요청 DTO
     * @return 성공 메시지
     */
    @DeleteMapping("/account")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteAccount(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody DeleteAccountRequest request) {
        Long userId = getCurrentUserId();
        String userUuid = getCurrentUserUuid();
        log.info("Account deletion for user: {}", userId);

        // 1. 회원 탈퇴 처리
        profileService.deleteAccount(userId, request);

        // 2. 현재 Access Token 블랙리스트에 추가
        String accessToken = TokenUtils.extractBearerToken(authorization);
        long remainingExpiration = tokenService.getRemainingExpiration(accessToken);
        tokenBlacklistService.addToBlacklist(accessToken, remainingExpiration);

        // 3. Refresh Token 삭제
        refreshTokenService.deleteRefreshToken(userUuid);

        log.info("Account deletion completed for user: {}", userId);
        return ResponseEntity.ok(
                ApiResponse.success(Map.of("message", "회원 탈퇴가 완료되었습니다")));
    }

    /**
     * SecurityContext에서 현재 인증된 사용자의 ID(Long)를 가져옵니다.
     * JWT의 subject는 UUID이므로, UUID로 사용자를 조회하여 ID를 반환합니다.
     *
     * @return 사용자 ID
     */
    private Long getCurrentUserId() {
        String uuid = getCurrentUserUuid();
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.USER_NOT_FOUND));
        return user.getId();
    }

    /**
     * SecurityContext에서 현재 인증된 사용자의 UUID를 가져옵니다.
     *
     * @return 사용자 UUID
     */
    private String getCurrentUserUuid() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new CustomBusinessException(AuthErrorCode.INVALID_TOKEN);
        }
        return (String) authentication.getPrincipal();
    }

}
