package com.portal.universe.shoppingservice.coupon.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CouponTest {

    private Coupon createActiveCoupon(DiscountType discountType, BigDecimal discountValue,
                                      int totalQuantity) {
        return Coupon.builder()
                .code("COUPON-001")
                .name("테스트 쿠폰")
                .description("테스트용 쿠폰입니다")
                .discountType(discountType)
                .discountValue(discountValue)
                .minimumOrderAmount(new BigDecimal("10000"))
                .maximumDiscountAmount(new BigDecimal("5000"))
                .totalQuantity(totalQuantity)
                .startsAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
    }

    @Nested
    @DisplayName("builder")
    class BuilderTest {

        @Test
        @DisplayName("should create coupon with default values")
        void should_create_coupon_with_defaults() {
            Coupon coupon = createActiveCoupon(DiscountType.FIXED, new BigDecimal("1000"), 100);

            assertThat(coupon.getCode()).isEqualTo("COUPON-001");
            assertThat(coupon.getName()).isEqualTo("테스트 쿠폰");
            assertThat(coupon.getDescription()).isEqualTo("테스트용 쿠폰입니다");
            assertThat(coupon.getDiscountType()).isEqualTo(DiscountType.FIXED);
            assertThat(coupon.getDiscountValue()).isEqualByComparingTo(new BigDecimal("1000"));
            assertThat(coupon.getTotalQuantity()).isEqualTo(100);
            assertThat(coupon.getIssuedQuantity()).isEqualTo(0);
            assertThat(coupon.getStatus()).isEqualTo(CouponStatus.ACTIVE);
            assertThat(coupon.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("incrementIssuedQuantity")
    class IncrementIssuedQuantityTest {

        @Test
        @DisplayName("should increment issued quantity")
        void should_increment_issued_quantity() {
            Coupon coupon = createActiveCoupon(DiscountType.FIXED, new BigDecimal("1000"), 100);

            coupon.incrementIssuedQuantity();

            assertThat(coupon.getIssuedQuantity()).isEqualTo(1);
            assertThat(coupon.getUpdatedAt()).isNotNull();
            assertThat(coupon.getStatus()).isEqualTo(CouponStatus.ACTIVE);
        }

        @Test
        @DisplayName("should mark as exhausted when reaching total quantity")
        void should_mark_exhausted_when_reaching_total() {
            Coupon coupon = createActiveCoupon(DiscountType.FIXED, new BigDecimal("1000"), 1);

            coupon.incrementIssuedQuantity();

            assertThat(coupon.getIssuedQuantity()).isEqualTo(1);
            assertThat(coupon.getStatus()).isEqualTo(CouponStatus.EXHAUSTED);
        }
    }

    @Nested
    @DisplayName("markAsExpired")
    class MarkAsExpiredTest {

        @Test
        @DisplayName("should change status to EXPIRED")
        void should_change_status_to_expired() {
            Coupon coupon = createActiveCoupon(DiscountType.FIXED, new BigDecimal("1000"), 100);

            coupon.markAsExpired();

            assertThat(coupon.getStatus()).isEqualTo(CouponStatus.EXPIRED);
            assertThat(coupon.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("deactivate")
    class DeactivateTest {

        @Test
        @DisplayName("should change status to INACTIVE")
        void should_change_status_to_inactive() {
            Coupon coupon = createActiveCoupon(DiscountType.FIXED, new BigDecimal("1000"), 100);

            coupon.deactivate();

            assertThat(coupon.getStatus()).isEqualTo(CouponStatus.INACTIVE);
            assertThat(coupon.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("isAvailable")
    class IsAvailableTest {

        @Test
        @DisplayName("should return true when coupon is active, not exhausted, within period")
        void should_return_true_when_available() {
            Coupon coupon = createActiveCoupon(DiscountType.FIXED, new BigDecimal("1000"), 100);

            assertThat(coupon.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("should return false when coupon is inactive")
        void should_return_false_when_inactive() {
            Coupon coupon = createActiveCoupon(DiscountType.FIXED, new BigDecimal("1000"), 100);
            coupon.deactivate();

            assertThat(coupon.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("should return false when coupon is exhausted")
        void should_return_false_when_exhausted() {
            Coupon coupon = createActiveCoupon(DiscountType.FIXED, new BigDecimal("1000"), 1);
            coupon.incrementIssuedQuantity();

            assertThat(coupon.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("should return false when coupon period has not started")
        void should_return_false_when_not_started() {
            Coupon coupon = Coupon.builder()
                    .code("FUTURE-001")
                    .name("미래 쿠폰")
                    .discountType(DiscountType.FIXED)
                    .discountValue(new BigDecimal("1000"))
                    .totalQuantity(100)
                    .startsAt(LocalDateTime.now().plusDays(1))
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .build();

            assertThat(coupon.isAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("getRemainingQuantity")
    class GetRemainingQuantityTest {

        @Test
        @DisplayName("should return correct remaining quantity")
        void should_return_remaining_quantity() {
            Coupon coupon = createActiveCoupon(DiscountType.FIXED, new BigDecimal("1000"), 100);
            coupon.incrementIssuedQuantity();
            coupon.incrementIssuedQuantity();

            assertThat(coupon.getRemainingQuantity()).isEqualTo(98);
        }
    }

    @Nested
    @DisplayName("calculateDiscount")
    class CalculateDiscountTest {

        @Test
        @DisplayName("should return fixed discount amount")
        void should_return_fixed_discount() {
            Coupon coupon = createActiveCoupon(DiscountType.FIXED, new BigDecimal("1000"), 100);

            BigDecimal discount = coupon.calculateDiscount(new BigDecimal("50000"));

            assertThat(discount).isEqualByComparingTo(new BigDecimal("1000"));
        }

        @Test
        @DisplayName("should return percentage discount amount")
        void should_return_percentage_discount() {
            Coupon coupon = Coupon.builder()
                    .code("PCT-001")
                    .name("퍼센트 쿠폰")
                    .discountType(DiscountType.PERCENTAGE)
                    .discountValue(new BigDecimal("10"))
                    .minimumOrderAmount(new BigDecimal("10000"))
                    .maximumDiscountAmount(null)
                    .totalQuantity(100)
                    .startsAt(LocalDateTime.now().minusDays(1))
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .build();

            BigDecimal discount = coupon.calculateDiscount(new BigDecimal("50000"));

            assertThat(discount).isEqualByComparingTo(new BigDecimal("5000.00"));
        }

        @Test
        @DisplayName("should cap discount at maximum discount amount")
        void should_cap_at_maximum_discount() {
            Coupon coupon = Coupon.builder()
                    .code("PCT-002")
                    .name("최대 할인 쿠폰")
                    .discountType(DiscountType.PERCENTAGE)
                    .discountValue(new BigDecimal("50"))
                    .minimumOrderAmount(new BigDecimal("10000"))
                    .maximumDiscountAmount(new BigDecimal("5000"))
                    .totalQuantity(100)
                    .startsAt(LocalDateTime.now().minusDays(1))
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .build();

            BigDecimal discount = coupon.calculateDiscount(new BigDecimal("50000"));

            assertThat(discount).isEqualByComparingTo(new BigDecimal("5000"));
        }

        @Test
        @DisplayName("should return zero when order amount is below minimum")
        void should_return_zero_below_minimum() {
            Coupon coupon = createActiveCoupon(DiscountType.FIXED, new BigDecimal("1000"), 100);

            BigDecimal discount = coupon.calculateDiscount(new BigDecimal("5000"));

            assertThat(discount).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
