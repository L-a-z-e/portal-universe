package com.portal.universe.authservice.password.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank(message = "Token is required")
        String token,

        @NotBlank(message = "New password is required")
        String newPassword,

        @NotBlank(message = "Password confirmation is required")
        String confirmPassword
) {}
