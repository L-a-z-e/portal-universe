package com.portal.universe.shoppingsettlementservice.settlement.repository;

import com.portal.universe.shoppingsettlementservice.settlement.domain.SettlementLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface SettlementLedgerRepository extends JpaRepository<SettlementLedger, Long> {
    List<SettlementLedger> findByProcessedFalseAndEventAtBetween(Instant start, Instant end);

    List<SettlementLedger> findByOrderNumberAndEventType(String orderNumber, String eventType);
}
