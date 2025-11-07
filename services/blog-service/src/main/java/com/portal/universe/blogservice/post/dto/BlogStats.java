package com.portal.universe.blogservice.post.dto;

import java.time.LocalDateTime;
import java.util.List;

public record BlogStats(
        Long totalPosts,
        Long publishedPosts,
        Long totalViews,
        Long totalLikes,
        List<String> topCategories,
        List<String> topTags,
        LocalDateTime lastPostDate
) {}