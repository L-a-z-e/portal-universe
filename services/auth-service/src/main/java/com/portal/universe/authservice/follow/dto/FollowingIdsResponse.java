package com.portal.universe.authservice.follow.dto;

import java.util.List;

/**
 * 내가 팔로우하는 사용자들의 UUID 목록 응답 DTO
 * blog-service 피드 API에서 사용합니다.
 */
public record FollowingIdsResponse(
        List<String> followingIds
) {
}
