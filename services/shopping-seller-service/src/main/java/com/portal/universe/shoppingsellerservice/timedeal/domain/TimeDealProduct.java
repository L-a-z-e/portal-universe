package com.portal.universe.shoppingsellerservice.timedeal.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "time_deal_products")
@Getter
@NoArgsConstructor
public class TimeDealProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_deal_id", nullable = false)
    private TimeDeal timeDeal;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "deal_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal dealPrice;

    @Column(name = "deal_quantity", nullable = false)
    private Integer dealQuantity;

    @Column(name = "sold_quantity", nullable = false)
    private Integer soldQuantity;

    @Column(name = "max_per_user", nullable = false)
    private Integer maxPerUser;

    @Builder
    public TimeDealProduct(TimeDeal timeDeal, Long productId, BigDecimal dealPrice,
                           Integer dealQuantity, Integer maxPerUser) {
        this.timeDeal = timeDeal;
        this.productId = productId;
        this.dealPrice = dealPrice;
        this.dealQuantity = dealQuantity;
        this.soldQuantity = 0;
        this.maxPerUser = maxPerUser;
    }
}
