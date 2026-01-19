package com.portal.universe.shoppingservice.coupon.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType discountType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(precision = 10, scale = 2)
    private BigDecimal minimumOrderAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal maximumDiscountAmount;

    @Column(nullable = false)
    private Integer totalQuantity;

    @Column(nullable = false)
    private Integer issuedQuantity = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponStatus status = CouponStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDateTime startsAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @Builder
    public Coupon(String code, String name, String description, DiscountType discountType,
                  BigDecimal discountValue, BigDecimal minimumOrderAmount, BigDecimal maximumDiscountAmount,
                  Integer totalQuantity, LocalDateTime startsAt, LocalDateTime expiresAt) {
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
        this.createdAt = LocalDateTime.now();
    }

    public void incrementIssuedQuantity() {
        this.issuedQuantity++;
        this.updatedAt = LocalDateTime.now();
        if (this.issuedQuantity >= this.totalQuantity) {
            this.status = CouponStatus.EXHAUSTED;
        }
    }

    public void markAsExhausted() {
        this.status = CouponStatus.EXHAUSTED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsExpired() {
        this.status = CouponStatus.EXPIRED;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.status = CouponStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isAvailable() {
        return this.status == CouponStatus.ACTIVE
                && this.issuedQuantity < this.totalQuantity
                && LocalDateTime.now().isAfter(this.startsAt)
                && LocalDateTime.now().isBefore(this.expiresAt);
    }

    public int getRemainingQuantity() {
        return this.totalQuantity - this.issuedQuantity;
    }
}
