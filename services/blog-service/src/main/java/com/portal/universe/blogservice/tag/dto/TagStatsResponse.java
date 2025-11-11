package com.portal.universe.blogservice.tag.dto;

/**
 * 태그 통계 응답 DTO (인기 태그 등)
 */
public record TagStatsResponse(
        String name,
        Long postCount
) {}