package com.portal.universe.shoppingsellerservice.inventory.repository;

import com.portal.universe.shoppingsellerservice.inventory.domain.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    Page<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);
}
