package com.portal.universe.shoppingsettlementservice.settlement.dto;

import com.portal.universe.shoppingsettlementservice.settlement.domain.SettlementPeriod;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SettlementPeriodResponse(
        Long id,
        String periodType,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        LocalDateTime createdAt
) {
    public static SettlementPeriodResponse from(SettlementPeriod period) {
        return new SettlementPeriodResponse(
                period.getId(),
                period.getPeriodType().name(),
                period.getStartDate(),
                period.getEndDate(),
                period.getStatus().name(),
                period.getCreatedAt()
        );
    }
}
