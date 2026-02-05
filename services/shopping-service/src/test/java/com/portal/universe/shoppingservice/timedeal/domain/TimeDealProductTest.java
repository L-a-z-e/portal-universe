package com.portal.universe.shoppingservice.timedeal.domain;

import com.portal.universe.shoppingservice.product.domain.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TimeDealProductTest {

    private Product createTestProduct(BigDecimal price) {
        return Product.builder()
                .name("테스트 상품")
                .price(price)
                .stock(100)
                .build();
    }

    private TimeDealProduct createTestTimeDealProduct(Product product, BigDecimal dealPrice,
                                                       int dealQuantity) {
        return TimeDealProduct.builder()
                .product(product)
                .dealPrice(dealPrice)
                .dealQuantity(dealQuantity)
                .maxPerUser(3)
                .build();
    }

    @Nested
    @DisplayName("builder")
    class BuilderTest {

        @Test
        @DisplayName("should create time deal product with default values")
        void should_create_with_defaults() {
            Product product = createTestProduct(new BigDecimal("10000"));
            TimeDealProduct tdp = createTestTimeDealProduct(product, new BigDecimal("5000"), 50);

            assertThat(tdp.getProduct()).isEqualTo(product);
            assertThat(tdp.getDealPrice()).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(tdp.getDealQuantity()).isEqualTo(50);
            assertThat(tdp.getSoldQuantity()).isEqualTo(0);
            assertThat(tdp.getMaxPerUser()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("incrementSoldQuantity")
    class IncrementSoldQuantityTest {

        @Test
        @DisplayName("should increment sold quantity by given amount")
        void should_increment_sold_quantity() {
            Product product = createTestProduct(new BigDecimal("10000"));
            TimeDealProduct tdp = createTestTimeDealProduct(product, new BigDecimal("5000"), 50);

            tdp.incrementSoldQuantity(3);

            assertThat(tdp.getSoldQuantity()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("getRemainingQuantity")
    class GetRemainingQuantityTest {

        @Test
        @DisplayName("should return correct remaining quantity")
        void should_return_remaining() {
            Product product = createTestProduct(new BigDecimal("10000"));
            TimeDealProduct tdp = createTestTimeDealProduct(product, new BigDecimal("5000"), 50);
            tdp.incrementSoldQuantity(10);

            assertThat(tdp.getRemainingQuantity()).isEqualTo(40);
        }
    }

    @Nested
    @DisplayName("isAvailable")
    class IsAvailableTest {

        @Test
        @DisplayName("should return true when sold less than deal quantity")
        void should_return_true_when_available() {
            Product product = createTestProduct(new BigDecimal("10000"));
            TimeDealProduct tdp = createTestTimeDealProduct(product, new BigDecimal("5000"), 50);

            assertThat(tdp.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("should return false when sold out")
        void should_return_false_when_sold_out() {
            Product product = createTestProduct(new BigDecimal("10000"));
            TimeDealProduct tdp = createTestTimeDealProduct(product, new BigDecimal("5000"), 10);
            tdp.incrementSoldQuantity(10);

            assertThat(tdp.isAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("getDiscountRate")
    class GetDiscountRateTest {

        @Test
        @DisplayName("should calculate correct discount rate")
        void should_calculate_discount_rate() {
            Product product = createTestProduct(new BigDecimal("10000"));
            TimeDealProduct tdp = createTestTimeDealProduct(product, new BigDecimal("7000"), 50);

            BigDecimal discountRate = tdp.getDiscountRate();

            // (10000 - 7000) / 10000 * 100 = 30.00
            assertThat(discountRate).isEqualByComparingTo(new BigDecimal("30.00"));
        }

        @Test
        @DisplayName("should return zero when original price is zero")
        void should_return_zero_when_original_price_is_zero() {
            Product product = createTestProduct(BigDecimal.ZERO);
            TimeDealProduct tdp = createTestTimeDealProduct(product, new BigDecimal("0"), 50);

            BigDecimal discountRate = tdp.getDiscountRate();

            assertThat(discountRate).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
