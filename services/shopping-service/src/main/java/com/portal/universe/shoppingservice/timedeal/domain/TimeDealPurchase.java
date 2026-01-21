package com.portal.universe.shoppingservice.timedeal.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "time_deal_purchases",
        indexes = {
                @Index(name = "idx_tdp_user_product", columnList = "user_id, time_deal_product_id")
        })
@Getter
@NoArgsConstructor
public class TimeDealPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_deal_product_id", nullable = false)
    private TimeDealProduct timeDealProduct;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "order_id")
    private Long orderId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime purchasedAt = LocalDateTime.now();

    @Builder
    public TimeDealPurchase(String userId, TimeDealProduct timeDealProduct, Integer quantity,
                            BigDecimal purchasePrice, Long orderId) {
        this.userId = userId;
        this.timeDealProduct = timeDealProduct;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
        this.orderId = orderId;
        this.purchasedAt = LocalDateTime.now();
    }
}
