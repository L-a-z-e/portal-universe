package com.portal.universe.shoppingsettlementservice.support.fixture;

import com.portal.universe.shoppingsettlementservice.settlement.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class SettlementFixture {

    private SettlementFixture() {}

    // ── Settlement ──

    public static Settlement createSettlement() {
        return settlementBuilder().build();
    }

    public static SettlementBuilder settlementBuilder() {
        return new SettlementBuilder();
    }

    public static class SettlementBuilder {
        private Long id = 1L;
        private Long periodId = 1L;
        private Long sellerId = 100L;
        private BigDecimal totalSales = new BigDecimal("10000.00");
        private Integer totalOrders = 5;
        private BigDecimal totalRefunds = BigDecimal.ZERO;
        private BigDecimal commissionAmount = new BigDecimal("1000.00");
        private BigDecimal netAmount = new BigDecimal("9000.00");

        public SettlementBuilder id(Long id) { this.id = id; return this; }
        public SettlementBuilder periodId(Long periodId) { this.periodId = periodId; return this; }
        public SettlementBuilder sellerId(Long sellerId) { this.sellerId = sellerId; return this; }
        public SettlementBuilder totalSales(BigDecimal totalSales) { this.totalSales = totalSales; return this; }
        public SettlementBuilder totalOrders(Integer totalOrders) { this.totalOrders = totalOrders; return this; }
        public SettlementBuilder totalRefunds(BigDecimal totalRefunds) { this.totalRefunds = totalRefunds; return this; }
        public SettlementBuilder commissionAmount(BigDecimal commissionAmount) { this.commissionAmount = commissionAmount; return this; }
        public SettlementBuilder netAmount(BigDecimal netAmount) { this.netAmount = netAmount; return this; }

        public Settlement build() {
            Settlement settlement = Settlement.builder()
                    .periodId(periodId)
                    .sellerId(sellerId)
                    .totalSales(totalSales)
                    .totalOrders(totalOrders)
                    .totalRefunds(totalRefunds)
                    .commissionAmount(commissionAmount)
                    .netAmount(netAmount)
                    .build();
            ReflectionTestUtils.setField(settlement, "id", id);
            return settlement;
        }
    }

    // ── SettlementPeriod ──

    public static SettlementPeriod createPeriod() {
        return periodBuilder().build();
    }

    public static PeriodBuilder periodBuilder() {
        return new PeriodBuilder();
    }

    public static class PeriodBuilder {
        private Long id = 1L;
        private PeriodType periodType = PeriodType.DAILY;
        private LocalDate startDate = LocalDate.of(2026, 2, 27);
        private LocalDate endDate = LocalDate.of(2026, 2, 27);

        public PeriodBuilder id(Long id) { this.id = id; return this; }
        public PeriodBuilder periodType(PeriodType periodType) { this.periodType = periodType; return this; }
        public PeriodBuilder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
        public PeriodBuilder endDate(LocalDate endDate) { this.endDate = endDate; return this; }

        public SettlementPeriod build() {
            SettlementPeriod period = SettlementPeriod.builder()
                    .periodType(periodType)
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();
            ReflectionTestUtils.setField(period, "id", id);
            return period;
        }
    }

    // ── SettlementLedger ──

    public static SettlementLedger createLedger() {
        return ledgerBuilder().build();
    }

    public static LedgerBuilder ledgerBuilder() {
        return new LedgerBuilder();
    }

    public static class LedgerBuilder {
        private Long id = 1L;
        private String orderNumber = "ORD-001";
        private Long sellerId = 100L;
        private String eventType = "PAYMENT_COMPLETED";
        private BigDecimal amount = new BigDecimal("10000.00");
        private Instant eventAt = Instant.parse("2026-02-27T12:00:00Z");

        public LedgerBuilder id(Long id) { this.id = id; return this; }
        public LedgerBuilder orderNumber(String orderNumber) { this.orderNumber = orderNumber; return this; }
        public LedgerBuilder sellerId(Long sellerId) { this.sellerId = sellerId; return this; }
        public LedgerBuilder eventType(String eventType) { this.eventType = eventType; return this; }
        public LedgerBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public LedgerBuilder eventAt(Instant eventAt) { this.eventAt = eventAt; return this; }

        public SettlementLedger build() {
            SettlementLedger ledger = SettlementLedger.builder()
                    .orderNumber(orderNumber)
                    .sellerId(sellerId)
                    .eventType(eventType)
                    .amount(amount)
                    .eventAt(eventAt)
                    .build();
            ReflectionTestUtils.setField(ledger, "id", id);
            return ledger;
        }
    }

    // ── SettlementDetail ──

    public static SettlementDetail createDetail() {
        return detailBuilder().build();
    }

    public static DetailBuilder detailBuilder() {
        return new DetailBuilder();
    }

    public static class DetailBuilder {
        private Long id = 1L;
        private Long settlementId = 1L;
        private String orderNumber = "ORD-001";
        private BigDecimal orderAmount = new BigDecimal("10000.00");
        private BigDecimal refundAmount = BigDecimal.ZERO;
        private BigDecimal commissionRate = new BigDecimal("10.00");
        private BigDecimal commissionAmount = new BigDecimal("1000.00");
        private BigDecimal netAmount = new BigDecimal("9000.00");

        public DetailBuilder id(Long id) { this.id = id; return this; }
        public DetailBuilder settlementId(Long settlementId) { this.settlementId = settlementId; return this; }
        public DetailBuilder orderNumber(String orderNumber) { this.orderNumber = orderNumber; return this; }
        public DetailBuilder orderAmount(BigDecimal orderAmount) { this.orderAmount = orderAmount; return this; }
        public DetailBuilder refundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; return this; }
        public DetailBuilder commissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; return this; }
        public DetailBuilder commissionAmount(BigDecimal commissionAmount) { this.commissionAmount = commissionAmount; return this; }
        public DetailBuilder netAmount(BigDecimal netAmount) { this.netAmount = netAmount; return this; }

        public SettlementDetail build() {
            SettlementDetail detail = SettlementDetail.builder()
                    .settlementId(settlementId)
                    .orderNumber(orderNumber)
                    .orderAmount(orderAmount)
                    .refundAmount(refundAmount)
                    .commissionRate(commissionRate)
                    .commissionAmount(commissionAmount)
                    .netAmount(netAmount)
                    .build();
            ReflectionTestUtils.setField(detail, "id", id);
            return detail;
        }
    }
}
