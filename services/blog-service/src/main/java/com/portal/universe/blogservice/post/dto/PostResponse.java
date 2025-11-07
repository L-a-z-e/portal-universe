package com.portal.universe.blogservice.post.dto;

import com.portal.universe.blogservice.post.domain.PostStatus;

import java.time.LocalDateTime;
import java.util.Set;

public record PostResponse(
        String id,
        String title,
        String content,
        String summary,
        String authorId,
        String authorName,
        PostStatus status,
        Set<String> tags,
        String category,
        String metaDescription,
        String thumbnailUrl,
        Long viewCount,
        Long likeCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime publishedAt,

        // 기존 호환성 유지
        String productId
) {}
