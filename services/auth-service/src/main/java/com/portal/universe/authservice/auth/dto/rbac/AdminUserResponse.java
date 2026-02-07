package com.portal.universe.authservice.auth.dto.rbac;

import com.portal.universe.authservice.user.domain.User;

import java.time.format.DateTimeFormatter;

public record AdminUserResponse(
        String uuid,
        String email,
        String username,
        String nickname,
        String profileImageUrl,
        String status,
        String createdAt,
        String lastLoginAt
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static AdminUserResponse from(User user) {
        var profile = user.getProfile();
        return new AdminUserResponse(
                user.getUuid(),
                user.getEmail(),
                profile != null ? profile.getUsername() : null,
                profile != null ? profile.getNickname() : null,
                profile != null ? profile.getProfileImageUrl() : null,
                user.getStatus().name(),
                user.getCreatedAt() != null ? user.getCreatedAt().format(FORMATTER) : null,
                user.getLastLoginAt() != null ? user.getLastLoginAt().format(FORMATTER) : null
        );
    }
}
