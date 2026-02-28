package com.portal.universe.blogservice.series.dto;

import java.time.Instant;

/**
 * 시리즈 목록 응답 DTO (요약)
 */
public record SeriesListResponse(
        String id,
        String name,
        String description,
        String authorId,
        String authorUsername,
        String authorNickname,
        String thumbnailUrl,
        Integer postCount,
        Instant createdAt,
        Instant updatedAt
) {}