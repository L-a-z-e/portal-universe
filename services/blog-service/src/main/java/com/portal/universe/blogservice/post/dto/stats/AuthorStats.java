package com.portal.universe.blogservice.post.dto.stats;

import java.time.LocalDateTime;

public record AuthorStats(
        String authorId,
        String authorName,
        Long totalPosts,
        Long publishedPosts,
        Long totalViews,
        Long totalLikes,
        LocalDateTime firstPostDate,
        LocalDateTime lastPostDate
) {}