package com.portal.universe.authservice.auth.dto.rbac;

import jakarta.validation.constraints.NotBlank;

public record UpdateRoleRequest(
        @NotBlank String displayName,
        String description
) {}
