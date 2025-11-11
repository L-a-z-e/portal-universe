package com.portal.universe.blogservice.tag.dto;

import java.time.LocalDateTime;

/**
 * 태그 응답 DTO
 */
public record TagResponse(
        String id,
        String name,
        Long postCount,
        String description,
        LocalDateTime createdAt,
        LocalDateTime lastUsedAt
) {}