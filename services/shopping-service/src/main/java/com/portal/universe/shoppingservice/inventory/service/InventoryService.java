package com.portal.universe.shoppingservice.inventory.service;

import com.portal.universe.shoppingservice.inventory.dto.InventoryResponse;

import java.util.List;

/**
 * 재고 조회 서비스 인터페이스입니다. (Buyer 전용, 읽기 전용)
 *
 * 재고 관리(예약/차감/해제)는 shopping-seller-service에서 담당합니다.
 */
public interface InventoryService {

    /**
     * 상품의 재고 정보를 조회합니다.
     */
    InventoryResponse getInventory(Long productId);

    /**
     * 여러 상품의 재고 정보를 일괄 조회합니다.
     */
    List<InventoryResponse> getInventories(List<Long> productIds);
}
