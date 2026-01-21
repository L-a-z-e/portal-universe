package com.portal.universe.blogservice.like.dto;

import lombok.Builder;

/**
 * 좋아요 상태 조회 응답 DTO
 * @param liked 현재 사용자의 좋아요 여부
 * @param likeCount 전체 좋아요 개수
 */
@Builder
public record LikeStatusResponse(
        boolean liked,
        long likeCount
) {
    public static LikeStatusResponse of(boolean liked, long likeCount) {
        return LikeStatusResponse.builder()
                .liked(liked)
                .likeCount(likeCount)
                .build();
    }
}
