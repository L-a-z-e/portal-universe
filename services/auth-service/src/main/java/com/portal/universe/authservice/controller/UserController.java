package com.portal.universe.authservice.controller;

import com.portal.universe.authservice.controller.dto.*;
import com.portal.universe.authservice.service.UserService;
import com.portal.universe.commonlibrary.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 관련 API를 처리하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    public record UserSignupRequest(
            String email,
            String password,
            String nickname,
            String realName,
            boolean marketingAgree
    ) {}

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<String>> signup(@RequestBody UserSignupRequest request) {
        UserService.SignupCommand command = new UserService.SignupCommand(
                request.email(),
                request.password(),
                request.nickname(),
                request.realName(),
                request.marketingAgree()
        );

        userService.registerUser(command);

        return ResponseEntity.ok(ApiResponse.success("User registered successfully"));
    }

    /**
     * 공개 프로필 조회
     * @param username 조회할 사용자의 username
     */
    @GetMapping("/{username}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @PathVariable String username) {
        UserProfileResponse profile = userService.getProfileByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    /**
     * 내 프로필 조회 (인증 필요)
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal String userIdStr) {
        Long userId = Long.parseLong(userIdStr);
        UserProfileResponse profile = userService.getMyProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    /**
     * 프로필 수정 (인증 필요)
     */
    @PutMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal String userIdStr,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        Long userId = Long.parseLong(userIdStr);
        UserProfileResponse profile = userService.updateProfile(
                userId,
                request.nickname(),
                request.bio(),
                request.profileImageUrl(),
                request.website()
        );
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    /**
     * Username 설정 (최초 1회, 인증 필요)
     */
    @PostMapping("/me/username")
    public ResponseEntity<ApiResponse<UserProfileResponse>> setUsername(
            @AuthenticationPrincipal String userIdStr,
            @Valid @RequestBody UsernameSetRequest request) {
        Long userId = Long.parseLong(userIdStr);
        UserProfileResponse profile = userService.setUsername(userId, request.username());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    /**
     * Username 중복 확인
     * @param username 확인할 username
     */
    @GetMapping("/check-username/{username}")
    public ResponseEntity<ApiResponse<UsernameCheckResponse>> checkUsername(
            @PathVariable String username) {
        boolean available = userService.checkUsernameAvailability(username);
        UsernameCheckResponse response = new UsernameCheckResponse(username, available);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}