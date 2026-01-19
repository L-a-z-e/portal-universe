package com.portal.universe.shoppingservice.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchRequest {
    private String keyword;
    private Double minPrice;
    private Double maxPrice;
    private String sort;  // relevance, price_asc, price_desc
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 20;

    public static ProductSearchRequest of(String keyword, int page, int size) {
        return ProductSearchRequest.builder()
                .keyword(keyword)
                .page(page)
                .size(size)
                .build();
    }
}
