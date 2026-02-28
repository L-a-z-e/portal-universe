package com.portal.universe.authservice.password.controller;

import com.portal.universe.authservice.common.exception.AuthErrorCode;
import com.portal.universe.authservice.password.dto.ForgotPasswordRequest;
import com.portal.universe.authservice.password.dto.ResetPasswordRequest;
import com.portal.universe.authservice.password.service.PasswordResetService;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.commonlibrary.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    /**
     * 비밀번호 재설정을 요청합니다.
     * 이메일 존재 여부와 관계없이 동일한 성공 응답을 반환합니다.
     */
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<Map<String, String>>> requestPasswordReset(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = IpUtils.getClientIp(servletRequest);
        passwordResetService.requestPasswordReset(request.email(), clientIp);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("message", "If the email exists, a password reset link has been sent")));
    }

    /**
     * 비밀번호를 재설정합니다.
     */
    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<Map<String, String>>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("message", "Password has been reset successfully")));
    }

    /**
     * 토큰 유효성을 확인합니다.
     */
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> validateToken(
            @RequestParam String token) {
        boolean valid = passwordResetService.validateToken(token);
        if (!valid) {
            throw new CustomBusinessException(AuthErrorCode.INVALID_RESET_TOKEN);
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of("valid", true)));
    }
}
