package com.portal.universe.blogservice.post.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record PostSummaryResponse(
        String id,
        String title,
        String summary,
        String authorId,
        String authorName,
        Set<String> tags,
        String category,
        String thumbnailUrl,
        List<String> images,
        Long viewCount,
        Long likeCount,
        Long commentCount,  // Phase 3: 댓글 수 추가
        LocalDateTime publishedAt,
        int estimatedReadTime
) {}