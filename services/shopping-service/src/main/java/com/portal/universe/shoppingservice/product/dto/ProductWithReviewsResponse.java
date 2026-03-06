package com.portal.universe.shoppingservice.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductWithReviewsResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        String category,
        List<Object> reviews
) {}
