package com.portal.universe.blogservice.tag.dto;

import java.time.Instant;

/**
 * 태그 응답 DTO
 */
public record TagResponse(
        String id,
        String name,
        Long postCount,
        String description,
        Instant createdAt,
        Instant lastUsedAt
) {}