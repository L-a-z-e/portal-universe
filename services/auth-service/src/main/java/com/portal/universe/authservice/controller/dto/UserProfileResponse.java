package com.portal.universe.authservice.controller.dto;

import com.portal.universe.authservice.domain.User;

import java.time.LocalDateTime;

/**
 * 사용자 프로필 응답 DTO
 */
public record UserProfileResponse(
        Long id,
        String uuid,
        String email,
        String nickname,
        String username,
        String bio,
        String profileImageUrl,
        String website,
        int followerCount,
        int followingCount,
        LocalDateTime createdAt
) {
    /**
     * User 엔티티를 응답 DTO로 변환 (팔로워/팔로잉 카운트 없이)
     */
    public static UserProfileResponse from(User user) {
        return from(user, 0, 0);
    }

    /**
     * User 엔티티를 응답 DTO로 변환 (팔로워/팔로잉 카운트 포함)
     */
    public static UserProfileResponse from(User user, int followerCount, int followingCount) {
        return new UserProfileResponse(
                user.getId(),
                user.getUuid(),
                user.getEmail(),
                user.getProfile().getNickname(),
                user.getProfile().getUsername(),
                user.getProfile().getBio(),
                user.getProfile().getProfileImageUrl(),
                user.getProfile().getWebsite(),
                followerCount,
                followingCount,
                user.getCreatedAt()
        );
    }
}
