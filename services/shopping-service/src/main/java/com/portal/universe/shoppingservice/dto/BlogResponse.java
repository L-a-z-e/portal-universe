package com.portal.universe.shoppingservice.dto;

public record BlogResponse(
        String id,
        String title,
        String content,
        String authorId,
        String productId
) {}
