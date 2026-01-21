package com.portal.universe.shoppingservice.coupon.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_coupons",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "coupon_id"}))
@Getter
@NoArgsConstructor
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserCouponStatus status = UserCouponStatus.AVAILABLE;

    @Column(name = "order_id")
    private Long usedOrderId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime issuedAt = LocalDateTime.now();

    private LocalDateTime usedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder
    public UserCoupon(String userId, Coupon coupon, LocalDateTime expiresAt) {
        this.userId = userId;
        this.coupon = coupon;
        this.status = UserCouponStatus.AVAILABLE;
        this.issuedAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
    }

    public void use(Long orderId) {
        this.status = UserCouponStatus.USED;
        this.usedOrderId = orderId;
        this.usedAt = LocalDateTime.now();
    }

    public void markAsExpired() {
        this.status = UserCouponStatus.EXPIRED;
    }

    public boolean isUsable() {
        return this.status == UserCouponStatus.AVAILABLE
                && LocalDateTime.now().isBefore(this.expiresAt);
    }
}
