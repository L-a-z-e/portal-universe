package com.portal.universe.shoppingservice.inventory.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingservice.inventory.dto.InventoryResponse;
import com.portal.universe.shoppingservice.inventory.dto.InventoryUpdateRequest;
import com.portal.universe.shoppingservice.inventory.dto.StockMovementResponse;
import com.portal.universe.shoppingservice.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * 재고 관리 API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/shopping/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

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

    /**
     * 상품의 재고를 초기화합니다. (관리자 전용)
     *
     * @param productId 상품 ID
     * @param request 초기 재고 정보
     * @param jwt 인증 정보
     * @return 생성된 재고 정보
     */
    @PostMapping("/{productId}")
    public ApiResponse<InventoryResponse> initializeInventory(
            @PathVariable Long productId,
            @Valid @RequestBody InventoryUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        return ApiResponse.success(inventoryService.initializeInventory(productId, request.quantity(), userId));
    }

    /**
     * 상품의 재고를 추가합니다 (입고). (관리자 전용)
     *
     * @param productId 상품 ID
     * @param request 추가할 재고 정보
     * @param jwt 인증 정보
     * @return 업데이트된 재고 정보
     */
    @PutMapping("/{productId}/add")
    public ApiResponse<InventoryResponse> addStock(
            @PathVariable Long productId,
            @Valid @RequestBody InventoryUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        return ApiResponse.success(inventoryService.addStock(productId, request.quantity(), request.reason(), userId));
    }

    /**
     * 상품의 재고 이동 이력을 조회합니다. (관리자 전용)
     *
     * @param productId 상품 ID
     * @param pageable 페이징 정보
     * @return 이동 이력 목록
     */
    @GetMapping("/{productId}/movements")
    public ApiResponse<Page<StockMovementResponse>> getStockMovements(
            @PathVariable Long productId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.success(inventoryService.getStockMovements(productId, pageable));
    }
}
