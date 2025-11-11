package com.portal.universe.blogservice.series.dto;

import java.time.LocalDateTime;

/**
 * 시리즈 목록 응답 DTO (요약)
 */
public record SeriesListResponse(
        String id,
        String name,
        String description,
        String authorId,
        String authorName,
        String thumbnailUrl,
        Integer postCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}