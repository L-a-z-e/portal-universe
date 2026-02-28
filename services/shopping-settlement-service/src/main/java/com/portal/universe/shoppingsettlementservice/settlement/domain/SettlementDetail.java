package com.portal.universe.shoppingsettlementservice.settlement.domain;

import com.portal.universe.commonlibrary.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "settlement_details")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementDetail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_id", nullable = false)
    private Long settlementId;

    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;

    @Column(name = "order_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal orderAmount;

    @Column(name = "refund_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @Column(name = "commission_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "net_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal netAmount;

    @Builder
    public SettlementDetail(Long settlementId, String orderNumber, BigDecimal orderAmount,
                            BigDecimal refundAmount, BigDecimal commissionRate,
                            BigDecimal commissionAmount, BigDecimal netAmount) {
        this.settlementId = settlementId;
        this.orderNumber = orderNumber;
        this.orderAmount = orderAmount;
        this.refundAmount = refundAmount;
        this.commissionRate = commissionRate;
        this.commissionAmount = commissionAmount;
        this.netAmount = netAmount;
    }
}
