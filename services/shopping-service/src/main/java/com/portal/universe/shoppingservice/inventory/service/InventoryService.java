package com.portal.universe.shoppingservice.inventory.service;

import com.portal.universe.shoppingservice.inventory.dto.InventoryResponse;
import com.portal.universe.shoppingservice.inventory.dto.StockMovementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * 재고 관리 서비스 인터페이스입니다.
 */
public interface InventoryService {

    /**
     * 상품의 재고 정보를 조회합니다.
     *
     * @param productId 상품 ID
     * @return 재고 정보
     */
    InventoryResponse getInventory(Long productId);

    /**
     * 상품의 재고를 초기화합니다.
     *
     * @param productId 상품 ID
     * @param initialStock 초기 재고 수량
     * @param userId 작업 수행자 ID
     * @return 생성된 재고 정보
     */
    InventoryResponse initializeInventory(Long productId, int initialStock, String userId);

    /**
     * 재고를 예약합니다 (주문 생성 시).
     *
     * @param productId 상품 ID
     * @param quantity 예약할 수량
     * @param referenceType 참조 유형 (ORDER 등)
     * @param referenceId 참조 ID (주문번호 등)
     * @param userId 작업 수행자 ID
     * @return 업데이트된 재고 정보
     */
    InventoryResponse reserveStock(Long productId, int quantity, String referenceType, String referenceId, String userId);

    /**
     * 여러 상품의 재고를 일괄 예약합니다.
     * 데드락 방지를 위해 상품 ID 순서로 정렬하여 락을 획득합니다.
     *
     * @param quantities 상품 ID → 예약 수량 맵
     * @param referenceType 참조 유형
     * @param referenceId 참조 ID
     * @param userId 작업 수행자 ID
     * @return 업데이트된 재고 정보 목록
     */
    List<InventoryResponse> reserveStockBatch(Map<Long, Integer> quantities, String referenceType, String referenceId, String userId);

    /**
     * 예약된 재고를 실제로 차감합니다 (결제 완료 시).
     *
     * @param productId 상품 ID
     * @param quantity 차감할 수량
     * @param referenceType 참조 유형
     * @param referenceId 참조 ID
     * @param userId 작업 수행자 ID
     * @return 업데이트된 재고 정보
     */
    InventoryResponse deductStock(Long productId, int quantity, String referenceType, String referenceId, String userId);

    /**
     * 여러 상품의 예약된 재고를 일괄 차감합니다.
     *
     * @param quantities 상품 ID → 차감 수량 맵
     * @param referenceType 참조 유형
     * @param referenceId 참조 ID
     * @param userId 작업 수행자 ID
     * @return 업데이트된 재고 정보 목록
     */
    List<InventoryResponse> deductStockBatch(Map<Long, Integer> quantities, String referenceType, String referenceId, String userId);

    /**
     * 예약된 재고를 해제합니다 (주문 취소, 결제 실패 시).
     *
     * @param productId 상품 ID
     * @param quantity 해제할 수량
     * @param referenceType 참조 유형
     * @param referenceId 참조 ID
     * @param userId 작업 수행자 ID
     * @return 업데이트된 재고 정보
     */
    InventoryResponse releaseStock(Long productId, int quantity, String referenceType, String referenceId, String userId);

    /**
     * 여러 상품의 예약된 재고를 일괄 해제합니다.
     *
     * @param quantities 상품 ID → 해제 수량 맵
     * @param referenceType 참조 유형
     * @param referenceId 참조 ID
     * @param userId 작업 수행자 ID
     * @return 업데이트된 재고 정보 목록
     */
    List<InventoryResponse> releaseStockBatch(Map<Long, Integer> quantities, String referenceType, String referenceId, String userId);

    /**
     * 재고를 추가합니다 (입고 시).
     *
     * @param productId 상품 ID
     * @param quantity 추가할 수량
     * @param reason 사유
     * @param userId 작업 수행자 ID
     * @return 업데이트된 재고 정보
     */
    InventoryResponse addStock(Long productId, int quantity, String reason, String userId);

    /**
     * 여러 상품의 재고 정보를 일괄 조회합니다.
     *
     * @param productIds 상품 ID 목록
     * @return 재고 정보 목록
     */
    List<InventoryResponse> getInventories(List<Long> productIds);

    /**
     * 재고 이동 이력을 조회합니다.
     *
     * @param productId 상품 ID
     * @param pageable 페이징 정보
     * @return 이동 이력 페이지
     */
    Page<StockMovementResponse> getStockMovements(Long productId, Pageable pageable);
}
