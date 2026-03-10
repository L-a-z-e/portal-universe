package com.portal.universe.shoppingservice.support.fixture;

import com.portal.universe.shoppingservice.product.domain.Product;

import java.math.BigDecimal;

public final class ProductFixture {

    private ProductFixture() {}

    public static Product create() {
        return builder().build();
    }

    public static ProductBuilder builder() {
        return new ProductBuilder();
    }

    public static class ProductBuilder {
        private Long id = 1L;
        private Long sellerId = 1L;
        private String name = "Test Product";
        private String description = "Test product description";
        private BigDecimal price = BigDecimal.valueOf(10000);
        private BigDecimal discountPrice;
        private String imageUrl = "test.jpg";
        private String category = "ELECTRONICS";
        private boolean featured = false;

        public ProductBuilder id(Long id) { this.id = id; return this; }
        public ProductBuilder sellerId(Long sellerId) { this.sellerId = sellerId; return this; }
        public ProductBuilder name(String name) { this.name = name; return this; }
        public ProductBuilder description(String description) { this.description = description; return this; }
        public ProductBuilder price(BigDecimal price) { this.price = price; return this; }
        public ProductBuilder discountPrice(BigDecimal discountPrice) { this.discountPrice = discountPrice; return this; }
        public ProductBuilder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public ProductBuilder category(String category) { this.category = category; return this; }
        public ProductBuilder featured(boolean featured) { this.featured = featured; return this; }

        public Product build() {
            Product product = Product.builder()
                    .id(id)
                    .sellerId(sellerId)
                    .name(name)
                    .description(description)
                    .price(price)
                    .discountPrice(discountPrice)
                    .imageUrl(imageUrl)
                    .category(category)
                    .featured(featured)
                    .build();
            return product;
        }
    }
}
