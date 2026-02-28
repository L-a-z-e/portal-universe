package com.portal.universe.shoppingsellerservice.product.dto;

import com.portal.universe.shoppingsellerservice.product.domain.Product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        Long sellerId,
        String name,
        String description,
        BigDecimal price,
        BigDecimal discountPrice,
        Integer stock,
        String imageUrl,
        String category,
        Boolean featured,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSellerId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getDiscountPrice(),
                product.getStock(),
                product.getImageUrl(),
                product.getCategory(),
                product.getFeatured(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
