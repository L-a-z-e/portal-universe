package com.portal.universe.blogservice.post.dto;

import com.portal.universe.blogservice.series.dto.SeriesNavigationResponse;

/**
 * 포스트 이전/다음 네비게이션 응답 DTO
 */
public record PostNavigationResponse(
        PostSummaryResponse previousPost,
        PostSummaryResponse nextPost,
        SeriesNavigationResponse seriesNavigation
) {
    public static PostNavigationResponse of(
            PostSummaryResponse previousPost,
            PostSummaryResponse nextPost,
            SeriesNavigationResponse seriesNavigation
    ) {
        return new PostNavigationResponse(previousPost, nextPost, seriesNavigation);
    }
}
