package com.portal.universe.shoppingservice.inventory.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingservice.inventory.dto.InventoryBatchRequest;
import com.portal.universe.shoppingservice.inventory.dto.InventoryResponse;
import com.portal.universe.shoppingservice.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 재고 조회 API를 제공하는 컨트롤러입니다. (Buyer 전용, 읽기 전용)
 *
 * 재고 관리(초기화/추가/예약/차감/해제)는 shopping-seller-service로 이전되었습니다.
 * 이 컨트롤러는 구매자가 상품 재고 현황을 확인하기 위한 조회 API만 제공합니다.
 */
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * 여러 상품의 재고 정보를 일괄 조회합니다.
     *
     * @param request productIds가 포함된 요청 본문
     * @return 재고 정보 목록
     */
    @PostMapping("/batch")
    public ApiResponse<List<InventoryResponse>> getInventories(@Valid @RequestBody InventoryBatchRequest request) {
        return ApiResponse.success(inventoryService.getInventories(request.productIds()));
    }

    /**
     * 상품의 재고 정보를 조회합니다.
     *
     * @param productId 상품 ID
     * @return 재고 정보
     */
    @GetMapping("/{productId}")
    public ApiResponse<InventoryResponse> getInventory(@PathVariable Long productId) {
        return ApiResponse.success(inventoryService.getInventory(productId));
    }
}
