package com.portal.universe.shoppingsettlementservice.settlement.repository;

import com.portal.universe.shoppingsettlementservice.settlement.domain.SettlementDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementDetailRepository extends JpaRepository<SettlementDetail, Long> {
    Page<SettlementDetail> findBySettlementIdOrderByCreatedAtDesc(Long settlementId, Pageable pageable);
}
