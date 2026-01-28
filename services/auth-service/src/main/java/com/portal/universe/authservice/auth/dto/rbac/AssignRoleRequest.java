package com.portal.universe.authservice.auth.dto.rbac;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record AssignRoleRequest(
        @NotBlank(message = "사용자 ID는 필수입니다")
        String userId,

        @NotBlank(message = "역할 키는 필수입니다")
        String roleKey,

        LocalDateTime expiresAt
) {}
