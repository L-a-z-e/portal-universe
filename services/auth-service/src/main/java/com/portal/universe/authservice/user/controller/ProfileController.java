package com.portal.universe.authservice.user.controller;

import com.portal.universe.authservice.user.dto.profile.ChangePasswordRequest;
import com.portal.universe.authservice.user.dto.profile.DeleteAccountRequest;
import com.portal.universe.authservice.user.dto.profile.ProfileResponse;
import com.portal.universe.authservice.user.dto.profile.UpdateProfileRequest;
import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.common.exception.AuthErrorCode;
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
        String uuid = getCurrentUserUuid();
        log.info("Profile fetch for user: {}", uuid);

        ProfileResponse response = profileService.getProfile(uuid);
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
        String uuid = getCurrentUserUuid();
        log.info("Profile update for user: {}", uuid);

        // updateProfile이 User 엔티티를 반환하므로 추가 DB 조회 불필요
        User user = profileService.updateProfile(uuid, request);
        ProfileResponse response = ProfileResponse.from(user);

        // 프로필 변경 후 새 Access Token 발급 (JWT에 닉네임 등이 포함되므로)
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
        String uuid = getCurrentUserUuid();
        log.info("Password change for user: {}", uuid);

        profileService.changePassword(uuid, request);
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
        String uuid = getCurrentUserUuid();
        log.info("Account deletion for user: {}", uuid);

        // 1. 회원 탈퇴 처리
        profileService.deleteAccount(uuid, request);

        // 2. 현재 Access Token 블랙리스트에 추가
        String accessToken = TokenUtils.extractBearerToken(authorization);
        long remainingExpiration = tokenService.getRemainingExpiration(accessToken);
        tokenBlacklistService.addToBlacklist(accessToken, remainingExpiration);

        // 3. Refresh Token 삭제
        refreshTokenService.deleteRefreshToken(uuid);

        log.info("Account deletion completed for user: {}", uuid);
        return ResponseEntity.ok(
                ApiResponse.success(Map.of("message", "회원 탈퇴가 완료되었습니다")));
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
