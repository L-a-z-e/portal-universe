package com.portal.universe.shoppingservice.inventory.repository;

import com.portal.universe.shoppingservice.inventory.domain.Inventory;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 재고 엔티티에 대한 데이터 액세스를 담당하는 리포지토리입니다.
 * 동시성 제어를 위한 비관적 락(Pessimistic Lock) 메서드를 제공합니다.
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /**
     * 상품 ID로 재고를 조회합니다.
     *
     * @param productId 상품 ID
     * @return 재고 정보
     */
    Optional<Inventory> findByProductId(Long productId);

    /**
     * 상품 ID로 재고를 조회합니다 (비관적 쓰기 락 적용).
     * 동시 수정을 방지하기 위해 SELECT ... FOR UPDATE를 사용합니다.
     * 락 획득 타임아웃은 3초로 설정됩니다.
     *
     * @param productId 상품 ID
     * @return 재고 정보 (락이 걸린 상태)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
    })
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> findByProductIdWithLock(@Param("productId") Long productId);

    /**
     * 여러 상품 ID로 재고를 조회합니다 (비관적 쓰기 락 적용).
     * 데드락 방지를 위해 상품 ID 순서로 정렬하여 조회합니다.
     *
     * @param productIds 상품 ID 목록
     * @return 재고 정보 목록 (락이 걸린 상태)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
    })
    @Query("SELECT i FROM Inventory i WHERE i.productId IN :productIds ORDER BY i.productId")
    List<Inventory> findByProductIdsWithLock(@Param("productIds") List<Long> productIds);

    /**
     * 상품 ID에 대한 재고 존재 여부를 확인합니다.
     *
     * @param productId 상품 ID
     * @return 존재 여부
     */
    boolean existsByProductId(Long productId);

    /**
     * 여러 상품 ID로 재고를 조회합니다 (락 없음, 읽기 전용).
     *
     * @param productIds 상품 ID 목록
     * @return 재고 정보 목록
     */
    @Query("SELECT i FROM Inventory i WHERE i.productId IN :productIds")
    List<Inventory> findByProductIds(@Param("productIds") List<Long> productIds);
}
