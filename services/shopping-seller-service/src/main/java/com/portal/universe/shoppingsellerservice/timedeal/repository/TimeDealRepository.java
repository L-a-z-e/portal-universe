package com.portal.universe.shoppingsellerservice.timedeal.repository;

import com.portal.universe.shoppingsellerservice.timedeal.domain.TimeDeal;
import com.portal.universe.shoppingsellerservice.timedeal.domain.TimeDealStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface TimeDealRepository extends JpaRepository<TimeDeal, Long> {
    Page<TimeDeal> findBySellerId(Long sellerId, Pageable pageable);
    long countBySellerId(Long sellerId);
    long countBySellerIdAndStatus(Long sellerId, TimeDealStatus status);

    @Query("SELECT td FROM TimeDeal td LEFT JOIN FETCH td.products WHERE td.status = 'SCHEDULED' AND td.startsAt <= :now")
    List<TimeDeal> findDealsToStart(@Param("now") Instant now);

    @Query("SELECT td FROM TimeDeal td LEFT JOIN FETCH td.products WHERE td.status = 'ACTIVE' AND td.endsAt <= :now")
    List<TimeDeal> findDealsToEnd(@Param("now") Instant now);
}
