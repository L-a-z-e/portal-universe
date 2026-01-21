package com.portal.universe.authservice.follow.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 팔로워/팔로잉 목록 응답 DTO
 */
public record FollowListResponse(
        List<FollowUserResponse> users,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static FollowListResponse from(Page<FollowUserResponse> page) {
        return new FollowListResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
