package com.portal.universe.authservice.follow.dto;

/**
 * 팔로우/언팔로우 응답 DTO
 */
public record FollowResponse(
        boolean following,
        int followerCount,
        int followingCount
) {
}
