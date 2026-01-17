package com.portal.universe.shoppingservice.inventory.repository;

import com.portal.universe.shoppingservice.inventory.domain.MovementType;
import com.portal.universe.shoppingservice.inventory.domain.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 재고 이동 이력 엔티티에 대한 데이터 액세스를 담당하는 리포지토리입니다.
 */
@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    /**
     * 재고 ID로 이동 이력을 조회합니다 (최신순 정렬).
     *
     * @param inventoryId 재고 ID
     * @param pageable 페이징 정보
     * @return 이동 이력 목록
     */
    Page<StockMovement> findByInventoryIdOrderByCreatedAtDesc(Long inventoryId, Pageable pageable);

    /**
     * 상품 ID로 이동 이력을 조회합니다 (최신순 정렬).
     *
     * @param productId 상품 ID
     * @param pageable 페이징 정보
     * @return 이동 이력 목록
     */
    Page<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    /**
     * 참조 유형과 참조 ID로 이동 이력을 조회합니다.
     *
     * @param referenceType 참조 유형
     * @param referenceId 참조 ID
     * @return 이동 이력 목록
     */
    List<StockMovement> findByReferenceTypeAndReferenceId(String referenceType, String referenceId);

    /**
     * 특정 기간 동안의 이동 이력을 조회합니다.
     *
     * @param productId 상품 ID
     * @param startDate 시작 일시
     * @param endDate 종료 일시
     * @return 이동 이력 목록
     */
    @Query("SELECT sm FROM StockMovement sm WHERE sm.productId = :productId " +
           "AND sm.createdAt BETWEEN :startDate AND :endDate ORDER BY sm.createdAt DESC")
    List<StockMovement> findByProductIdAndPeriod(
            @Param("productId") Long productId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 특정 이동 유형의 이력을 조회합니다.
     *
     * @param productId 상품 ID
     * @param movementType 이동 유형
     * @param pageable 페이징 정보
     * @return 이동 이력 목록
     */
    Page<StockMovement> findByProductIdAndMovementTypeOrderByCreatedAtDesc(
            Long productId,
            MovementType movementType,
            Pageable pageable
    );
}
