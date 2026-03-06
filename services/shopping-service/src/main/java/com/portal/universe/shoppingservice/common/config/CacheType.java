package com.portal.universe.shoppingservice.common.config;

import com.portal.universe.shoppingservice.product.dto.ProductResponse;

import java.util.List;

public enum CacheType {
    PRODUCT_DETAIL(Names.PRODUCT_DETAIL, ProductResponse.class),
    PRODUCT_CATEGORIES(Names.PRODUCT_CATEGORIES, List.class);

    private final String cacheName;
    private final Class<?> type;

    CacheType(String cacheName, Class<?> type) {
        this.cacheName = cacheName;
        this.type = type;
    }

    public String getCacheName() {
        return cacheName;
    }

    public Class<?> getType() {
        return type;
    }

    public static class Names {
        public static final String PRODUCT_DETAIL = "product-detail";
        public static final String PRODUCT_CATEGORIES = "product-categories";
    }
}
