package com.portal.universe.blogservice.post.dto;

public record TagStats(
        String tagName,
        Long postCount,
        Long totalViews
) {}