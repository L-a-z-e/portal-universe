package com.portal.universe.blogservice.post.dto;

/**
 * 시리즈 내 네비게이션 정보 응답 DTO
 */
public record SeriesNavigationResponse(
        String seriesId,
        String seriesName,
        int currentIndex,
        int totalPosts,
        String previousPostId,
        String nextPostId
) {
    public static SeriesNavigationResponse of(
            String seriesId,
            String seriesName,
            int currentIndex,
            int totalPosts,
            String previousPostId,
            String nextPostId
    ) {
        return new SeriesNavigationResponse(
                seriesId,
                seriesName,
                currentIndex,
                totalPosts,
                previousPostId,
                nextPostId
        );
    }
}
