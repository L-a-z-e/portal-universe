package com.portal.universe.authservice.follow.dto;

/**
 * 팔로우 상태 확인 응답 DTO
 */
public record FollowStatusResponse(
        boolean isFollowing
) {
}
