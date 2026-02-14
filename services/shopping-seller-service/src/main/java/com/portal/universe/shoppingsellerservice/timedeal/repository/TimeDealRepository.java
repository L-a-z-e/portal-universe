package com.portal.universe.shoppingsellerservice.timedeal.repository;

import com.portal.universe.shoppingsellerservice.timedeal.domain.TimeDeal;
import com.portal.universe.shoppingsellerservice.timedeal.domain.TimeDealStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeDealRepository extends JpaRepository<TimeDeal, Long> {
    Page<TimeDeal> findBySellerId(Long sellerId, Pageable pageable);
    long countBySellerId(Long sellerId);
    long countBySellerIdAndStatus(Long sellerId, TimeDealStatus status);
}
