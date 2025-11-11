package com.portal.universe.blogservice.series.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 시리즈 응답 DTO (상세)
 */
public record SeriesResponse(
        String id,
        String name,
        String description,
        String authorId,
        String authorName,
        String thumbnailUrl,
        List<String> postIds,
        Integer postCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}