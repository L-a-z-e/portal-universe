package com.portal.universe.shoppingservice.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        BigDecimal discountPrice,
        Integer stock,
        String imageUrl,
        String category,
        Boolean featured,
        List<String> images,
        Double averageRating,
        Integer reviewCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
