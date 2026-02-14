package com.portal.universe.shoppingsettlementservice.settlement.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingsettlementservice.common.exception.SettlementErrorCode;
import com.portal.universe.shoppingsettlementservice.settlement.domain.*;
import com.portal.universe.shoppingsettlementservice.settlement.dto.SettlementPeriodResponse;
import com.portal.universe.shoppingsettlementservice.settlement.dto.SettlementResponse;
import com.portal.universe.shoppingsettlementservice.settlement.repository.SettlementPeriodRepository;
import com.portal.universe.shoppingsettlementservice.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementServiceImpl implements SettlementService {

    private final SettlementPeriodRepository periodRepository;
    private final SettlementRepository settlementRepository;

    @Override
    public List<SettlementPeriodResponse> getPeriods(String periodType, Pageable pageable) {
        PeriodType type = PeriodType.valueOf(periodType.toUpperCase());
        return periodRepository.findByPeriodTypeOrderByStartDateDesc(type, pageable)
                .map(SettlementPeriodResponse::from)
                .getContent();
    }

    @Override
    public SettlementPeriodResponse getPeriod(Long periodId) {
        SettlementPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new CustomBusinessException(SettlementErrorCode.PERIOD_NOT_FOUND));
        return SettlementPeriodResponse.from(period);
    }

    @Override
    public Page<SettlementResponse> getSellerSettlements(Long sellerId, Pageable pageable) {
        return settlementRepository.findBySellerIdOrderByCreatedAtDesc(sellerId, pageable)
                .map(SettlementResponse::from);
    }

    @Override
    @Transactional
    public void confirmPeriod(Long periodId) {
        List<Settlement> settlements = settlementRepository.findByPeriodId(periodId);
        settlements.forEach(Settlement::confirm);
    }

    @Override
    @Transactional
    public void markPeriodPaid(Long periodId) {
        List<Settlement> settlements = settlementRepository.findByPeriodId(periodId);
        settlements.forEach(Settlement::markPaid);

        SettlementPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new CustomBusinessException(SettlementErrorCode.PERIOD_NOT_FOUND));
        period.complete();
    }
}
