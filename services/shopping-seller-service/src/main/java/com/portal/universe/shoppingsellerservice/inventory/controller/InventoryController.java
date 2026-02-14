package com.portal.universe.shoppingsellerservice.inventory.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingsellerservice.inventory.dto.InventoryResponse;
import com.portal.universe.shoppingsellerservice.inventory.dto.StockAddRequest;
import com.portal.universe.shoppingsellerservice.inventory.dto.StockMovementResponse;
import com.portal.universe.shoppingsellerservice.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    public ApiResponse<InventoryResponse> getInventory(@PathVariable Long productId) {
        return ApiResponse.success(inventoryService.getInventory(productId));
    }

    @PutMapping("/{productId}/add")
    public ApiResponse<InventoryResponse> addStock(
            @PathVariable Long productId,
            @Valid @RequestBody StockAddRequest request,
            @AuthenticationPrincipal String userId) {
        return ApiResponse.success(inventoryService.addStock(productId, request, userId));
    }

    @GetMapping("/{productId}/movements")
    public ApiResponse<Page<StockMovementResponse>> getMovements(
            @PathVariable Long productId,
            Pageable pageable) {
        return ApiResponse.success(inventoryService.getMovements(productId, pageable));
    }

    @PostMapping("/{productId}")
    public ApiResponse<InventoryResponse> initializeInventory(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") Integer initialQuantity) {
        return ApiResponse.success(inventoryService.initializeInventory(productId, initialQuantity));
    }
}
