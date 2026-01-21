package com.portal.universe.blogservice.like.dto;

import lombok.Builder;

/**
 * 좋아요 토글 응답 DTO
 * @param liked 좋아요 상태 (true: 추가됨, false: 취소됨)
 * @param likeCount 현재 좋아요 총 개수
 */
@Builder
public record LikeToggleResponse(
        boolean liked,
        long likeCount
) {
    public static LikeToggleResponse of(boolean liked, long likeCount) {
        return LikeToggleResponse.builder()
                .liked(liked)
                .likeCount(likeCount)
                .build();
    }
}
