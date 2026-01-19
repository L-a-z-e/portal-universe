package com.portal.universe.shoppingservice.timedeal.repository;

import com.portal.universe.shoppingservice.timedeal.domain.TimeDealPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TimeDealPurchaseRepository extends JpaRepository<TimeDealPurchase, Long> {

    @Query("SELECT COALESCE(SUM(p.quantity), 0) FROM TimeDealPurchase p " +
           "WHERE p.userId = :userId AND p.timeDealProduct.id = :timeDealProductId")
    int getTotalPurchasedQuantity(@Param("userId") Long userId,
                                   @Param("timeDealProductId") Long timeDealProductId);

    List<TimeDealPurchase> findByUserId(Long userId);

    @Query("SELECT p FROM TimeDealPurchase p " +
           "JOIN FETCH p.timeDealProduct tdp " +
           "JOIN FETCH tdp.product " +
           "WHERE p.userId = :userId")
    List<TimeDealPurchase> findByUserIdWithProduct(@Param("userId") Long userId);
}
