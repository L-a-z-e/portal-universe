package com.portal.universe.blogservice.post.dto.stats;

import java.time.Instant;
import java.util.List;

public record BlogStats(
        Long totalPosts,
        Long publishedPosts,
        Long totalViews,
        Long totalLikes,
        List<String> topCategories,
        List<String> topTags,
        Instant lastPostDate
) {}