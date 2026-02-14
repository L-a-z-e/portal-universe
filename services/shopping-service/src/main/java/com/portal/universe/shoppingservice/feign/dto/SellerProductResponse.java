package com.portal.universe.shoppingservice.feign.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SellerProductResponse(
        Long id,
        Long sellerId,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        String imageUrl,
        String category,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
