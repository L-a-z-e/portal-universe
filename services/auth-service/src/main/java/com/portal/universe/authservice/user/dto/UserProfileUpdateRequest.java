package com.portal.universe.authservice.user.dto;

import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

/**
 * 사용자 프로필 수정 요청 DTO
 */
public record UserProfileUpdateRequest(
        @Size(max = 50, message = "Nickname must be less than 50 characters")
        String nickname,

        @Size(max = 200, message = "Bio must be less than 200 characters")
        String bio,

        String profileImageUrl,

        @URL(message = "Website must be a valid URL")
        String website
) {}
