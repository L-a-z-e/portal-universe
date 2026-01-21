package com.portal.universe.authservice.follow.dto;

import com.portal.universe.authservice.domain.User;

/**
 * 팔로워/팔로잉 목록에서 사용되는 사용자 정보 DTO
 */
public record FollowUserResponse(
        String uuid,
        String username,
        String nickname,
        String profileImageUrl,
        String bio
) {
    public static FollowUserResponse from(User user) {
        return new FollowUserResponse(
                user.getUuid(),
                user.getProfile().getUsername(),
                user.getProfile().getNickname(),
                user.getProfile().getProfileImageUrl(),
                user.getProfile().getBio()
        );
    }
}
