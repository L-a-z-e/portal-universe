package com.portal.universe.shoppingsettlementservice.settlement.service;

import com.portal.universe.shoppingsettlementservice.settlement.dto.SettlementPeriodResponse;
import com.portal.universe.shoppingsettlementservice.settlement.dto.SettlementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SettlementService {
    List<SettlementPeriodResponse> getPeriods(String periodType, Pageable pageable);
    SettlementPeriodResponse getPeriod(Long periodId);
    Page<SettlementResponse> getSellerSettlements(Long sellerId, Pageable pageable);
    void confirmPeriod(Long periodId);
    void markPeriodPaid(Long periodId);
}
