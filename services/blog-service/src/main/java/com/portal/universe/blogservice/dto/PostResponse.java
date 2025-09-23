package com.portal.universe.blogservice.dto;

import java.time.LocalDateTime;

public record PostResponse(
        String id,
        String title,
        String content,
        String authorId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String productId
) {
}
