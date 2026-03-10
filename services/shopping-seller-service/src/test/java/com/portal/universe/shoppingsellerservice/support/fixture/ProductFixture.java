package com.portal.universe.shoppingsellerservice.support.fixture;

import com.portal.universe.shoppingsellerservice.product.domain.Product;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

public final class ProductFixture {

    public static final Long DEFAULT_SELLER_ID = 1L;
    public static final String DEFAULT_NAME = "Test Product";
    public static final BigDecimal DEFAULT_PRICE = new BigDecimal("10000");

    private ProductFixture() {}

    public static Product create() {
        return builder().build();
    }

    public static ProductBuilder builder() {
        return new ProductBuilder();
    }

    public static class ProductBuilder {
        private Long id;
        private Long sellerId = DEFAULT_SELLER_ID;
        private String name = DEFAULT_NAME;
        private String description = "Test product description";
        private BigDecimal price = DEFAULT_PRICE;
        private BigDecimal discountPrice;
        private Integer stock = 100;
        private String imageUrl = "https://img.test.com/product.jpg";
        private String category = "ELECTRONICS";
        private Boolean featured = false;

        public ProductBuilder id(Long id) { this.id = id; return this; }
        public ProductBuilder sellerId(Long sellerId) { this.sellerId = sellerId; return this; }
        public ProductBuilder name(String name) { this.name = name; return this; }
        public ProductBuilder description(String description) { this.description = description; return this; }
        public ProductBuilder price(BigDecimal price) { this.price = price; return this; }
        public ProductBuilder discountPrice(BigDecimal discountPrice) { this.discountPrice = discountPrice; return this; }
        public ProductBuilder stock(Integer stock) { this.stock = stock; return this; }
        public ProductBuilder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public ProductBuilder category(String category) { this.category = category; return this; }
        public ProductBuilder featured(Boolean featured) { this.featured = featured; return this; }

        public Product build() {
            Product product = Product.builder()
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
            if (id != null) {
                ReflectionTestUtils.setField(product, "id", id);
            }
            return product;
        }
    }
}
