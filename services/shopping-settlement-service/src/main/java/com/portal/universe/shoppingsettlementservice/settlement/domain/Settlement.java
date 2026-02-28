package com.portal.universe.shoppingsettlementservice.settlement.domain;

import com.portal.universe.commonlibrary.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "settlements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "period_id", nullable = false)
    private Long periodId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "total_sales", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalSales;

    @Column(name = "total_orders", nullable = false)
    private Integer totalOrders;

    @Column(name = "total_refunds", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalRefunds;

    @Column(name = "commission_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "net_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Builder
    public Settlement(Long periodId, Long sellerId, BigDecimal totalSales, Integer totalOrders,
                      BigDecimal totalRefunds, BigDecimal commissionAmount, BigDecimal netAmount) {
        this.periodId = periodId;
        this.sellerId = sellerId;
        this.totalSales = totalSales;
        this.totalOrders = totalOrders;
        this.totalRefunds = totalRefunds;
        this.commissionAmount = commissionAmount;
        this.netAmount = netAmount;
        this.status = SettlementStatus.CALCULATED;
    }

    public void confirm() {
        this.status = SettlementStatus.CONFIRMED;
    }

    public void markPaid() {
        this.status = SettlementStatus.PAID;
        this.paidAt = Instant.now();
    }

    public void dispute() {
        this.status = SettlementStatus.DISPUTED;
    }
}
