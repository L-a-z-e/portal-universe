package com.portal.universe.blogservice.post.dto;

import com.portal.universe.blogservice.post.domain.PostStatus;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record PostResponse(
        String id,
        String title,
        String content,
        String summary,
        String authorId,
        String authorUsername,
        String authorNickname,
        PostStatus status,
        Set<String> tags,
        String category,
        String metaDescription,
        String thumbnailUrl,
        List<String> images,
        Long viewCount,
        Long likeCount,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt,

        // 기존 호환성 유지
        String productId
) {}
