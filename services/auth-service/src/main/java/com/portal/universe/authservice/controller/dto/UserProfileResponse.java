package com.portal.universe.authservice.controller.dto;

import com.portal.universe.authservice.domain.User;

import java.time.LocalDateTime;

/**
 * 사용자 프로필 응답 DTO
 */
public record UserProfileResponse(
        Long id,
        String email,
        String nickname,
        String username,
        String bio,
        String profileImageUrl,
        String website,
        LocalDateTime createdAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getProfile().getNickname(),
                user.getProfile().getUsername(),
                user.getProfile().getBio(),
                user.getProfile().getProfileImageUrl(),
                user.getProfile().getWebsite(),
                user.getCreatedAt()
        );
    }
}
