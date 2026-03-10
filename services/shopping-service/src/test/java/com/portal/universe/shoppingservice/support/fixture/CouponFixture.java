package com.portal.universe.shoppingservice.support.fixture;

import com.portal.universe.shoppingservice.coupon.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class CouponFixture {

    private CouponFixture() {}

    public static Coupon create() {
        return couponBuilder().build();
    }

    public static CouponBuilder couponBuilder() {
        return new CouponBuilder();
    }

    public static class CouponBuilder {
        private Long id = 1L;
        private Long sourceCouponId = 100L;
        private Long sellerId = 1L;
        private String code = "TEST-COUPON";
        private String name = "Test Coupon";
        private DiscountType discountType = DiscountType.FIXED;
        private BigDecimal discountValue = BigDecimal.valueOf(1000);
        private BigDecimal minimumOrderAmount = BigDecimal.valueOf(5000);
        private BigDecimal maximumDiscountAmount = BigDecimal.valueOf(3000);
        private Integer totalQuantity = 100;
        private Integer issuedQuantity = 0;
        private CouponStatus status = CouponStatus.ACTIVE;
        private Instant startsAt = Instant.now().minus(1, ChronoUnit.DAYS);
        private Instant expiresAt = Instant.now().plus(30, ChronoUnit.DAYS);

        public CouponBuilder id(Long id) { this.id = id; return this; }
        public CouponBuilder sourceCouponId(Long sourceCouponId) { this.sourceCouponId = sourceCouponId; return this; }
        public CouponBuilder sellerId(Long sellerId) { this.sellerId = sellerId; return this; }
        public CouponBuilder code(String code) { this.code = code; return this; }
        public CouponBuilder name(String name) { this.name = name; return this; }
        public CouponBuilder discountType(DiscountType discountType) { this.discountType = discountType; return this; }
        public CouponBuilder discountValue(BigDecimal discountValue) { this.discountValue = discountValue; return this; }
        public CouponBuilder minimumOrderAmount(BigDecimal minimumOrderAmount) { this.minimumOrderAmount = minimumOrderAmount; return this; }
        public CouponBuilder maximumDiscountAmount(BigDecimal maximumDiscountAmount) { this.maximumDiscountAmount = maximumDiscountAmount; return this; }
        public CouponBuilder totalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; return this; }
        public CouponBuilder issuedQuantity(Integer issuedQuantity) { this.issuedQuantity = issuedQuantity; return this; }
        public CouponBuilder status(CouponStatus status) { this.status = status; return this; }
        public CouponBuilder startsAt(Instant startsAt) { this.startsAt = startsAt; return this; }
        public CouponBuilder expiresAt(Instant expiresAt) { this.expiresAt = expiresAt; return this; }

        public Coupon build() {
            Coupon coupon = Coupon.builder()
                    .sourceCouponId(sourceCouponId)
                    .sellerId(sellerId)
                    .code(code)
                    .name(name)
                    .discountType(discountType)
                    .discountValue(discountValue)
                    .minimumOrderAmount(minimumOrderAmount)
                    .maximumDiscountAmount(maximumDiscountAmount)
                    .totalQuantity(totalQuantity)
                    .startsAt(startsAt)
                    .expiresAt(expiresAt)
                    .build();
            ReflectionTestUtils.setField(coupon, "id", id);
            if (status != CouponStatus.ACTIVE) {
                ReflectionTestUtils.setField(coupon, "status", status);
            }
            if (issuedQuantity != 0) {
                ReflectionTestUtils.setField(coupon, "issuedQuantity", issuedQuantity);
            }
            return coupon;
        }
    }

    public static UserCoupon createUserCoupon(Coupon coupon, String userId) {
        return userCouponBuilder().coupon(coupon).userId(userId).build();
    }

    public static UserCouponBuilder userCouponBuilder() {
        return new UserCouponBuilder();
    }

    public static class UserCouponBuilder {
        private Long id = 1L;
        private String userId = "test-user-001";
        private Coupon coupon;
        private UserCouponStatus status = UserCouponStatus.AVAILABLE;
        private Instant expiresAt = Instant.now().plus(30, ChronoUnit.DAYS);

        public UserCouponBuilder id(Long id) { this.id = id; return this; }
        public UserCouponBuilder userId(String userId) { this.userId = userId; return this; }
        public UserCouponBuilder coupon(Coupon coupon) { this.coupon = coupon; return this; }
        public UserCouponBuilder status(UserCouponStatus status) { this.status = status; return this; }
        public UserCouponBuilder expiresAt(Instant expiresAt) { this.expiresAt = expiresAt; return this; }

        public UserCoupon build() {
            UserCoupon userCoupon = UserCoupon.builder()
                    .userId(userId)
                    .coupon(coupon)
                    .expiresAt(expiresAt)
                    .build();
            ReflectionTestUtils.setField(userCoupon, "id", id);
            if (status != UserCouponStatus.AVAILABLE) {
                ReflectionTestUtils.setField(userCoupon, "status", status);
            }
            return userCoupon;
        }
    }
}
