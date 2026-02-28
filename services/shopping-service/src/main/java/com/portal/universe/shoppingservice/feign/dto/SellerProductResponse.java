package com.portal.universe.shoppingservice.feign.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SellerProductResponse(
        Long id,
        Long sellerId,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        String imageUrl,
        String category,
        Instant createdAt,
        Instant updatedAt
) {}
