package com.portal.universe.shoppingsettlementservice.settlement.domain;

import com.portal.universe.commonlibrary.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "settlement_periods")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementPeriod extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 20)
    private PeriodType periodType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PeriodStatus status;

    @Builder
    public SettlementPeriod(PeriodType periodType, LocalDate startDate, LocalDate endDate) {
        this.periodType = periodType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = PeriodStatus.PENDING;
    }

    public void startProcessing() {
        this.status = PeriodStatus.PROCESSING;
    }

    public void complete() {
        this.status = PeriodStatus.COMPLETED;
    }

    public void fail() {
        this.status = PeriodStatus.FAILED;
    }
}
