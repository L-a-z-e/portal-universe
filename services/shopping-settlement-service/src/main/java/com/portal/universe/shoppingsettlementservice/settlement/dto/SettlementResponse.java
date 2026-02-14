package com.portal.universe.shoppingsettlementservice.settlement.dto;

import com.portal.universe.shoppingsettlementservice.settlement.domain.Settlement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SettlementResponse(
        Long id,
        Long periodId,
        Long sellerId,
        BigDecimal totalSales,
        Integer totalOrders,
        BigDecimal totalRefunds,
        BigDecimal commissionAmount,
        BigDecimal netAmount,
        String status,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {
    public static SettlementResponse from(Settlement settlement) {
        return new SettlementResponse(
                settlement.getId(),
                settlement.getPeriodId(),
                settlement.getSellerId(),
                settlement.getTotalSales(),
                settlement.getTotalOrders(),
                settlement.getTotalRefunds(),
                settlement.getCommissionAmount(),
                settlement.getNetAmount(),
                settlement.getStatus().name(),
                settlement.getPaidAt(),
                settlement.getCreatedAt()
        );
    }
}
