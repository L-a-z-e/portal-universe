package com.portal.universe.shoppingservice.timedeal.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "time_deal_products")
@Getter
@NoArgsConstructor
public class TimeDealProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_deal_id", nullable = false)
    private TimeDeal timeDeal;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal dealPrice;

    @Column(nullable = false)
    private Integer dealQuantity;

    @Column(nullable = false)
    private Integer soldQuantity = 0;

    @Column(nullable = false)
    private Integer maxPerUser;

    @Builder
    public TimeDealProduct(Long productId, BigDecimal dealPrice, Integer dealQuantity, Integer maxPerUser) {
        this.productId = productId;
        this.dealPrice = dealPrice;
        this.dealQuantity = dealQuantity;
        this.soldQuantity = 0;
        this.maxPerUser = maxPerUser;
    }

    public void incrementSoldQuantity(int quantity) {
        this.soldQuantity += quantity;
    }

    public int getRemainingQuantity() {
        return this.dealQuantity - this.soldQuantity;
    }

    public boolean isAvailable() {
        return this.soldQuantity < this.dealQuantity;
    }
}
