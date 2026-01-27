package com.portal.universe.blogservice.post.dto.stats;

import java.time.LocalDateTime;

public record CategoryStats(
        String categoryName,
        Long postCount,
        LocalDateTime latestPostDate
) {}
