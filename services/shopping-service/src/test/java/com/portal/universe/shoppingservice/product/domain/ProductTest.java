package com.portal.universe.shoppingservice.product.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
                    .stock(100)
                    .imageUrl("https://example.com/image.jpg")
                    .category("전자제품")
                    .build();

            assertThat(product.getName()).isEqualTo("테스트 상품");
            assertThat(product.getDescription()).isEqualTo("테스트 설명");
            assertThat(product.getPrice()).isEqualByComparingTo(new BigDecimal("10000.00"));
            assertThat(product.getStock()).isEqualTo(100);
            assertThat(product.getImageUrl()).isEqualTo("https://example.com/image.jpg");
            assertThat(product.getCategory()).isEqualTo("전자제품");
        }

        @Test
        @DisplayName("should create product with required fields only")
        void should_create_product_with_required_fields_only() {
            Product product = Product.builder()
                    .name("필수 상품")
                    .price(new BigDecimal("5000"))
                    .stock(50)
                    .build();

            assertThat(product.getName()).isEqualTo("필수 상품");
            assertThat(product.getPrice()).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(product.getStock()).isEqualTo(50);
            assertThat(product.getDescription()).isNull();
            assertThat(product.getImageUrl()).isNull();
            assertThat(product.getCategory()).isNull();
            assertThat(product.getId()).isNull();
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTest {

        @Test
        @DisplayName("should update all fields")
        void should_update_all_fields() {
            Product product = Product.builder()
                    .name("원래 상품")
                    .description("원래 설명")
                    .price(new BigDecimal("10000"))
                    .stock(100)
                    .imageUrl("https://example.com/old.jpg")
                    .category("의류")
                    .build();

            product.update(
                    "수정된 상품",
                    "수정된 설명",
                    new BigDecimal("20000"),
                    200,
                    "https://example.com/new.jpg",
                    "전자제품"
            );

            assertThat(product.getName()).isEqualTo("수정된 상품");
            assertThat(product.getDescription()).isEqualTo("수정된 설명");
            assertThat(product.getPrice()).isEqualByComparingTo(new BigDecimal("20000"));
            assertThat(product.getStock()).isEqualTo(200);
            assertThat(product.getImageUrl()).isEqualTo("https://example.com/new.jpg");
            assertThat(product.getCategory()).isEqualTo("전자제품");
        }
    }
}
