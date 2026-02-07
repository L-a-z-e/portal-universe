package com.portal.universe.authservice.auth.dto.rbac;

import jakarta.validation.constraints.NotBlank;

public record CreateRoleRequest(
        @NotBlank String roleKey,
        @NotBlank String displayName,
        String description,
        String serviceScope,
        String membershipGroup,
        String parentRoleKey
) {}
