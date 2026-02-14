package com.portal.universe.shoppingsettlementservice.settlement.repository;

import com.portal.universe.shoppingsettlementservice.settlement.domain.PeriodType;
import com.portal.universe.shoppingsettlementservice.settlement.domain.SettlementPeriod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementPeriodRepository extends JpaRepository<SettlementPeriod, Long> {
    Page<SettlementPeriod> findByPeriodTypeOrderByStartDateDesc(PeriodType periodType, Pageable pageable);
}
