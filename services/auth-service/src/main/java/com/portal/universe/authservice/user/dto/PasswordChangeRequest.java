package com.portal.universe.authservice.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 변경 요청 DTO
 */
public record PasswordChangeRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        String newPassword,

        @NotBlank(message = "Password confirmation is required")
        String confirmPassword
) {
    /**
     * 새 비밀번호와 확인 비밀번호가 일치하는지 확인
     */
    public boolean isPasswordMatching() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}
