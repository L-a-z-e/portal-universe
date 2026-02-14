package com.portal.universe.shoppingsettlementservice.settlement.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlements")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement {

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
    private LocalDateTime paidAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
        this.paidAt = LocalDateTime.now();
    }

    public void dispute() {
        this.status = SettlementStatus.DISPUTED;
    }
}
