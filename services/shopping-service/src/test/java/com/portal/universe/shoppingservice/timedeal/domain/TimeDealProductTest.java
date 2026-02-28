package com.portal.universe.shoppingservice.timedeal.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TimeDealProductTest {

    private TimeDealProduct createTestTimeDealProduct(Long productId, BigDecimal dealPrice,
                                                       int dealQuantity) {
        return TimeDealProduct.builder()
                .productId(productId)
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
            TimeDealProduct tdp = createTestTimeDealProduct(1L, new BigDecimal("5000"), 50);

            assertThat(tdp.getProductId()).isEqualTo(1L);
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
            TimeDealProduct tdp = createTestTimeDealProduct(1L, new BigDecimal("5000"), 50);

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
            TimeDealProduct tdp = createTestTimeDealProduct(1L, new BigDecimal("5000"), 50);
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
            TimeDealProduct tdp = createTestTimeDealProduct(1L, new BigDecimal("5000"), 50);

            assertThat(tdp.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("should return false when sold out")
        void should_return_false_when_sold_out() {
            TimeDealProduct tdp = createTestTimeDealProduct(1L, new BigDecimal("5000"), 10);
            tdp.incrementSoldQuantity(10);

            assertThat(tdp.isAvailable()).isFalse();
        }
    }
}
