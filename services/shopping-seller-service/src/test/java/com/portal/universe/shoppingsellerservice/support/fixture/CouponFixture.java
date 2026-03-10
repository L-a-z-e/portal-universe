package com.portal.universe.shoppingsellerservice.support.fixture;

import com.portal.universe.shoppingsellerservice.coupon.domain.Coupon;
import com.portal.universe.shoppingsellerservice.coupon.domain.CouponStatus;
import com.portal.universe.shoppingsellerservice.coupon.domain.DiscountType;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class CouponFixture {

    private CouponFixture() {}

    public static Coupon create() {
        return builder().build();
    }

    public static CouponBuilder builder() {
        return new CouponBuilder();
    }

    public static class CouponBuilder {
        private Long id;
        private Long sellerId = 1L;
        private String code = "TEST-COUPON-001";
        private String name = "Test Coupon";
        private String description = "Test coupon description";
        private DiscountType discountType = DiscountType.FIXED;
        private BigDecimal discountValue = new BigDecimal("1000");
        private BigDecimal minimumOrderAmount = new BigDecimal("10000");
        private BigDecimal maximumDiscountAmount = new BigDecimal("5000");
        private Integer totalQuantity = 100;
        private Integer issuedQuantity = 0;
        private CouponStatus status;
        private Instant startsAt = Instant.now();
        private Instant expiresAt = Instant.now().plus(30, ChronoUnit.DAYS);

        public CouponBuilder id(Long id) { this.id = id; return this; }
        public CouponBuilder sellerId(Long sellerId) { this.sellerId = sellerId; return this; }
        public CouponBuilder code(String code) { this.code = code; return this; }
        public CouponBuilder name(String name) { this.name = name; return this; }
        public CouponBuilder description(String description) { this.description = description; return this; }
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
                    .sellerId(sellerId)
                    .code(code)
                    .name(name)
                    .description(description)
                    .discountType(discountType)
                    .discountValue(discountValue)
                    .minimumOrderAmount(minimumOrderAmount)
                    .maximumDiscountAmount(maximumDiscountAmount)
                    .totalQuantity(totalQuantity)
                    .startsAt(startsAt)
                    .expiresAt(expiresAt)
                    .build();
            if (id != null) {
                ReflectionTestUtils.setField(coupon, "id", id);
            }
            if (issuedQuantity != 0) {
                ReflectionTestUtils.setField(coupon, "issuedQuantity", issuedQuantity);
            }
            if (status != null && status != CouponStatus.ACTIVE) {
                ReflectionTestUtils.setField(coupon, "status", status);
            }
            return coupon;
        }
    }
}
