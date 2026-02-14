package com.portal.universe.shoppingsettlementservice.settlement.repository;

import com.portal.universe.shoppingsettlementservice.settlement.domain.Settlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findByPeriodId(Long periodId);
    Page<Settlement> findBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);
}
