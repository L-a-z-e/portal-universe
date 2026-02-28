package com.portal.universe.blogservice.post.dto.stats;

import java.time.Instant;

public record CategoryStats(
        String categoryName,
        Long postCount,
        Instant latestPostDate
) {}
