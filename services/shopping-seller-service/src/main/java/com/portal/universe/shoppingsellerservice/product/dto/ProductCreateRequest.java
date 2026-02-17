package com.portal.universe.shoppingsellerservice.product.dto;

import com.portal.universe.shoppingsellerservice.product.domain.Product;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductCreateRequest(
        @NotBlank @Size(max = 255) String name,
        String description,
        @NotNull @Min(0) BigDecimal price,
        @Min(0) BigDecimal discountPrice,
        @NotNull @Min(0) Integer stock,
        @Size(max = 500) String imageUrl,
        @Size(max = 100) String category,
        Boolean featured
) {
    public Product toEntity(Long sellerId) {
        return Product.builder()
                .sellerId(sellerId)
                .name(name)
                .description(description)
                .price(price)
                .discountPrice(discountPrice)
                .stock(stock)
                .imageUrl(imageUrl)
                .category(category)
                .featured(featured)
                .build();
    }
}
