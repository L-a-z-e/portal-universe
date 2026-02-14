package com.portal.universe.shoppingsettlementservice.settlement.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlement_ledger")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementLedger {

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
    private LocalDateTime eventAt;

    @Column(nullable = false)
    private Boolean processed;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public SettlementLedger(String orderNumber, Long sellerId, String eventType,
                            BigDecimal amount, LocalDateTime eventAt) {
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
