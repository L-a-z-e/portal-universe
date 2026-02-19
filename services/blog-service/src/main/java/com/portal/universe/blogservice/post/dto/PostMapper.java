package com.portal.universe.blogservice.post.dto;

import com.portal.universe.blogservice.post.domain.Post;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Post 엔티티를 DTO로 변환하는 유틸리티 클래스.
 * PostServiceImpl과 SeriesService 등에서 공통으로 사용합니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PostMapper {

    /**
     * Post → PostSummaryResponse 변환
     */
    public static PostSummaryResponse toSummary(Post post) {
        return new PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                post.getSummary(),
                post.getAuthorId(),
                post.getAuthorUsername(),
                post.getAuthorNickname(),
                post.getTags(),
                post.getCategory(),
                post.getThumbnailUrl(),
                post.getImages(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getCommentCount() != null ? post.getCommentCount() : 0L,
                post.getPublishedAt(),
                calculateReadTime(post.getContent())
        );
    }

    /**
     * 읽기 시간 계산 (평균 200자/분 기준)
     */
    public static int calculateReadTime(String content) {
        if (content == null || content.isEmpty()) return 1;
        int charCount = content.length();
        return Math.max(1, (int) Math.ceil(charCount / 200.0));
    }
}
