package com.portal.universe.shoppingservice.product.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {

    @Nested
    @DisplayName("builder")
    class BuilderTest {

        @Test
        @DisplayName("should create product with all fields")
        void should_create_product_with_all_fields() {
            Product product = Product.builder()
                    .name("테스트 상품")
                    .description("테스트 설명")
                    .price(new BigDecimal("10000.00"))
                    .imageUrl("https://example.com/image.jpg")
                    .category("전자제품")
                    .build();

            assertThat(product.getName()).isEqualTo("테스트 상품");
            assertThat(product.getDescription()).isEqualTo("테스트 설명");
            assertThat(product.getPrice()).isEqualByComparingTo(new BigDecimal("10000.00"));
            assertThat(product.getImageUrl()).isEqualTo("https://example.com/image.jpg");
            assertThat(product.getCategory()).isEqualTo("전자제품");
        }

        @Test
        @DisplayName("should create product with required fields only")
        void should_create_product_with_required_fields_only() {
            Product product = Product.builder()
                    .name("필수 상품")
                    .price(new BigDecimal("5000"))
                    .build();

            assertThat(product.getName()).isEqualTo("필수 상품");
            assertThat(product.getPrice()).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(product.getDescription()).isNull();
            assertThat(product.getImageUrl()).isNull();
            assertThat(product.getCategory()).isNull();
            assertThat(product.getId()).isNull();
        }
    }

    @Nested
    @DisplayName("updateFromSource")
    class UpdateFromSourceTest {

        @Test
        @DisplayName("should update all fields from source event")
        void should_update_all_fields_from_source() {
            Product product = Product.builder()
                    .name("원래 상품")
                    .description("원래 설명")
                    .price(new BigDecimal("10000"))
                    .imageUrl("https://example.com/old.jpg")
                    .category("의류")
                    .build();

            product.updateFromSource(
                    "수정된 상품",
                    "수정된 설명",
                    new BigDecimal("20000"),
                    new BigDecimal("15000"),
                    "https://example.com/new.jpg",
                    "전자제품",
                    true
            );

            assertThat(product.getName()).isEqualTo("수정된 상품");
            assertThat(product.getDescription()).isEqualTo("수정된 설명");
            assertThat(product.getPrice()).isEqualByComparingTo(new BigDecimal("20000"));
            assertThat(product.getDiscountPrice()).isEqualByComparingTo(new BigDecimal("15000"));
            assertThat(product.getImageUrl()).isEqualTo("https://example.com/new.jpg");
            assertThat(product.getCategory()).isEqualTo("전자제품");
            assertThat(product.getFeatured()).isTrue();
        }
    }
}
