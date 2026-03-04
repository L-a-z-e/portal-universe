package com.portal.universe.shoppingservice.search.document;

import com.portal.universe.shoppingservice.product.domain.Product;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDocument {
    private Long id;
    private Long sellerId;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String imageUrl;
    private String category;
    private Boolean featured;

    public static ProductDocument from(Product product) {
        return ProductDocument.builder()
                .id(product.getId())
                .sellerId(product.getSellerId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .discountPrice(product.getDiscountPrice())
                .imageUrl(product.getImageUrl())
                .category(product.getCategory())
                .featured(product.getFeatured())
                .build();
    }
}
