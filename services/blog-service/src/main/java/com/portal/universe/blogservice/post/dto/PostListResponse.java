package com.portal.universe.blogservice.post.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record PostListResponse(
        String id,
        String title,
        String summary,
        String authorId,
        String authorName,
        Set<String> tags,
        String category,
        String thumbnailUrl,
        Long viewCount,
        Long likeCount,
        LocalDateTime publishedAt,

        // 읽기 시간 예상 (PRD: UX 개선)
        Integer estimatedReadTimeMinutes
) {}