package com.portal.universe.shoppingservice.coupon.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserCouponTest {

    private Coupon createTestCoupon() {
        return Coupon.builder()
                .code("TEST-COUPON")
                .name("테스트 쿠폰")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("1000"))
                .totalQuantity(100)
                .startsAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
    }

    private UserCoupon createTestUserCoupon() {
        return UserCoupon.builder()
                .userId("user-001")
                .coupon(createTestCoupon())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
    }

    @Nested
    @DisplayName("builder")
    class BuilderTest {

        @Test
        @DisplayName("should create user coupon with default values")
        void should_create_user_coupon_with_defaults() {
            UserCoupon userCoupon = createTestUserCoupon();

            assertThat(userCoupon.getUserId()).isEqualTo("user-001");
            assertThat(userCoupon.getCoupon()).isNotNull();
            assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE);
            assertThat(userCoupon.getIssuedAt()).isNotNull();
            assertThat(userCoupon.getExpiresAt()).isNotNull();
            assertThat(userCoupon.getUsedAt()).isNull();
            assertThat(userCoupon.getUsedOrderId()).isNull();
        }
    }

    @Nested
    @DisplayName("use")
    class UseTest {

        @Test
        @DisplayName("should mark as used with order id and timestamp")
        void should_mark_as_used() {
            UserCoupon userCoupon = createTestUserCoupon();

            userCoupon.use(100L);

            assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.USED);
            assertThat(userCoupon.getUsedOrderId()).isEqualTo(100L);
            assertThat(userCoupon.getUsedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("markAsExpired")
    class MarkAsExpiredTest {

        @Test
        @DisplayName("should change status to EXPIRED")
        void should_change_status_to_expired() {
            UserCoupon userCoupon = createTestUserCoupon();

            userCoupon.markAsExpired();

            assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.EXPIRED);
        }
    }

    @Nested
    @DisplayName("isUsable")
    class IsUsableTest {

        @Test
        @DisplayName("should return true when available and not expired")
        void should_return_true_when_usable() {
            UserCoupon userCoupon = createTestUserCoupon();

            assertThat(userCoupon.isUsable()).isTrue();
        }

        @Test
        @DisplayName("should return false when already used")
        void should_return_false_when_used() {
            UserCoupon userCoupon = createTestUserCoupon();
            userCoupon.use(100L);

            assertThat(userCoupon.isUsable()).isFalse();
        }

        @Test
        @DisplayName("should return false when expired by time")
        void should_return_false_when_expired_by_time() {
            UserCoupon userCoupon = UserCoupon.builder()
                    .userId("user-001")
                    .coupon(createTestCoupon())
                    .expiresAt(LocalDateTime.now().minusDays(1))
                    .build();

            assertThat(userCoupon.isUsable()).isFalse();
        }
    }
}
