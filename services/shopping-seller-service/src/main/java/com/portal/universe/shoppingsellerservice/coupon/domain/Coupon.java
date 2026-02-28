package com.portal.universe.shoppingsellerservice.coupon.domain;

import com.portal.universe.commonlibrary.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "minimum_order_amount", precision = 10, scale = 2)
    private BigDecimal minimumOrderAmount;

    @Column(name = "maximum_discount_amount", precision = 10, scale = 2)
    private BigDecimal maximumDiscountAmount;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "issued_quantity", nullable = false)
    private Integer issuedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponStatus status;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Builder
    public Coupon(Long sellerId, String code, String name, String description,
                  DiscountType discountType, BigDecimal discountValue,
                  BigDecimal minimumOrderAmount, BigDecimal maximumDiscountAmount,
                  Integer totalQuantity, Instant startsAt, Instant expiresAt) {
        this.sellerId = sellerId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minimumOrderAmount = minimumOrderAmount;
        this.maximumDiscountAmount = maximumDiscountAmount;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = 0;
        this.status = CouponStatus.ACTIVE;
        this.startsAt = startsAt;
        this.expiresAt = expiresAt;
    }

    public void deactivate() {
        this.status = CouponStatus.INACTIVE;
    }

    public void expire() {
        this.status = CouponStatus.EXPIRED;
    }
}
