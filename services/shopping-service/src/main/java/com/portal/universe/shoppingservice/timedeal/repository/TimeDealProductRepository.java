package com.portal.universe.shoppingservice.timedeal.repository;

import com.portal.universe.shoppingservice.timedeal.domain.TimeDealProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TimeDealProductRepository extends JpaRepository<TimeDealProduct, Long> {

    @Query("SELECT tdp FROM TimeDealProduct tdp " +
           "JOIN FETCH tdp.product " +
           "WHERE tdp.timeDeal.id = :timeDealId")
    List<TimeDealProduct> findByTimeDealId(@Param("timeDealId") Long timeDealId);

    @Query("SELECT tdp FROM TimeDealProduct tdp " +
           "JOIN FETCH tdp.product " +
           "JOIN FETCH tdp.timeDeal " +
           "WHERE tdp.id = :id")
    Optional<TimeDealProduct> findByIdWithProductAndDeal(@Param("id") Long id);

    boolean existsByTimeDealIdAndProductId(Long timeDealId, Long productId);
}
