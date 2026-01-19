package com.portal.universe.shoppingservice.timedeal.repository;

import com.portal.universe.shoppingservice.timedeal.domain.TimeDeal;
import com.portal.universe.shoppingservice.timedeal.domain.TimeDealStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TimeDealRepository extends JpaRepository<TimeDeal, Long> {

    List<TimeDeal> findByStatus(TimeDealStatus status);

    @Query("SELECT td FROM TimeDeal td WHERE td.status = :status " +
           "AND td.startsAt <= :now AND td.endsAt > :now")
    List<TimeDeal> findActiveDeals(@Param("status") TimeDealStatus status,
                                    @Param("now") LocalDateTime now);

    @Query("SELECT td FROM TimeDeal td WHERE td.status = 'SCHEDULED' " +
           "AND td.startsAt <= :now")
    List<TimeDeal> findDealsToStart(@Param("now") LocalDateTime now);

    @Query("SELECT td FROM TimeDeal td WHERE td.status = 'ACTIVE' " +
           "AND td.endsAt <= :now")
    List<TimeDeal> findDealsToEnd(@Param("now") LocalDateTime now);

    @Query("SELECT td FROM TimeDeal td LEFT JOIN FETCH td.products " +
           "WHERE td.id = :id")
    TimeDeal findByIdWithProducts(@Param("id") Long id);
}
