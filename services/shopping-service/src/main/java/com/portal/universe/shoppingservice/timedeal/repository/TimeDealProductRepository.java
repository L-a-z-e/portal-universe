package com.portal.universe.shoppingservice.timedeal.repository;

import com.portal.universe.shoppingservice.timedeal.domain.TimeDealProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TimeDealProductRepository extends JpaRepository<TimeDealProduct, Long> {

    List<TimeDealProduct> findByTimeDealId(Long timeDealId);

    @Query("SELECT tdp FROM TimeDealProduct tdp " +
           "JOIN FETCH tdp.timeDeal " +
           "WHERE tdp.id = :id")
    Optional<TimeDealProduct> findByIdWithDeal(@Param("id") Long id);

    boolean existsByTimeDealIdAndProductId(Long timeDealId, Long productId);

    @Modifying
    @Query("UPDATE TimeDealProduct tdp SET tdp.soldQuantity = tdp.soldQuantity + :quantity WHERE tdp.id = :id")
    void incrementSoldQuantity(@Param("id") Long id, @Param("quantity") int quantity);
}
