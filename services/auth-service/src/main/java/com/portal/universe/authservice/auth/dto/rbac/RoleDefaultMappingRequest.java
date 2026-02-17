package com.portal.universe.authservice.auth.dto.rbac;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoleDefaultMappingRequest(
        @NotBlank @Size(max = 50) String roleKey,
        @NotBlank @Size(max = 50) String membershipGroup,
        @NotBlank @Size(max = 50) String defaultTierKey
) {}
