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
@Table(name = "settlement_ledger", uniqueConstraints = {
        @UniqueConstraint(name = "uq_ledger_order_seller_event", columnNames = {"order_number", "seller_id", "event_type"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementLedger extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "event_at", nullable = false)
    private Instant eventAt;

    @Column(nullable = false)
    private Boolean processed;

    @Builder
    public SettlementLedger(String orderNumber, Long sellerId, String eventType,
                            BigDecimal amount, Instant eventAt) {
        this.orderNumber = orderNumber;
        this.sellerId = sellerId;
        this.eventType = eventType;
        this.amount = amount;
        this.eventAt = eventAt;
        this.processed = false;
    }

    public void markProcessed() {
        this.processed = true;
    }
}
