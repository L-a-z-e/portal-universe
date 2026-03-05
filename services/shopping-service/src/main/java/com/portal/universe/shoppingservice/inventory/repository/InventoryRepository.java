package com.portal.universe.shoppingservice.inventory.repository;

import com.portal.universe.shoppingservice.inventory.domain.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 재고 엔티티에 대한 데이터 액세스를 담당하는 리포지토리입니다. (읽기 전용)
 *
 * 재고 변경(예약/차감/해제)은 shopping-seller-service에서 처리됩니다.
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    boolean existsByProductId(Long productId);

    void deleteByProductId(Long productId);

    @Query("SELECT i FROM Inventory i WHERE i.productId IN :productIds")
    List<Inventory> findByProductIds(@Param("productIds") List<Long> productIds);
}
