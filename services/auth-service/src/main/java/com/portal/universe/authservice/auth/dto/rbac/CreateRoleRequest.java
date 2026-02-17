package com.portal.universe.authservice.auth.dto.rbac;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateRoleRequest(
        @NotBlank String roleKey,
        @NotBlank String displayName,
        String description,
        String serviceScope,
        String membershipGroup,
        List<String> includedRoleKeys
) {}
