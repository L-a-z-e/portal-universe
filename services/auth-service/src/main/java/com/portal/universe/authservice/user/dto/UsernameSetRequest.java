package com.portal.universe.authservice.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Username 설정 요청 DTO
 */
public record UsernameSetRequest(
        @NotBlank(message = "Username is required")
        @Pattern(
                regexp = "^[a-z0-9_]{3,20}$",
                message = "Username must be 3-20 characters long and contain only lowercase letters, numbers, and underscores"
        )
        String username
) {}
