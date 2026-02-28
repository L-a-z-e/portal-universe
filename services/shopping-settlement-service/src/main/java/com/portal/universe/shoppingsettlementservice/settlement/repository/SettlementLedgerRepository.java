package com.portal.universe.shoppingsettlementservice.settlement.repository;

import com.portal.universe.shoppingsettlementservice.settlement.domain.SettlementLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SettlementLedgerRepository extends JpaRepository<SettlementLedger, Long> {
    List<SettlementLedger> findByProcessedFalseAndEventAtBetween(Instant start, Instant end);

    List<SettlementLedger> findByOrderNumberAndEventType(String orderNumber, String eventType);

    @Query("SELECT DISTINCT l.sellerId FROM SettlementLedger l " +
            "WHERE l.processed = false AND l.eventAt BETWEEN :start AND :end " +
            "ORDER BY l.sellerId")
    List<Long> findDistinctSellerIds(@Param("start") Instant start, @Param("end") Instant end);
}
