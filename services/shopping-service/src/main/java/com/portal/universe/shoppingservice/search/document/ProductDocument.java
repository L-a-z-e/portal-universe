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
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;

    public static ProductDocument from(Product product) {
        return ProductDocument.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .build();
    }
}
