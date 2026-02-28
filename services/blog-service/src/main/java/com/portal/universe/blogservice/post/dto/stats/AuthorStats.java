package com.portal.universe.blogservice.post.dto.stats;

import java.time.Instant;

public record AuthorStats(
        String authorId,
        String authorUsername,
        String authorNickname,
        Long totalPosts,
        Long publishedPosts,
        Long totalViews,
        Long totalLikes,
        Instant firstPostDate,
        Instant lastPostDate
) {}