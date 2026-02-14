package com.portal.universe.shoppingsellerservice.inventory.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingsellerservice.inventory.dto.StockReserveRequest;
import com.portal.universe.shoppingsellerservice.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/inventory")
@RequiredArgsConstructor
public class InternalInventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/reserve")
    public ApiResponse<Void> reserveStock(@Valid @RequestBody StockReserveRequest request) {
        inventoryService.reserveStock(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/deduct")
    public ApiResponse<Void> deductStock(@Valid @RequestBody StockReserveRequest request) {
        inventoryService.deductStock(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/release")
    public ApiResponse<Void> releaseStock(@Valid @RequestBody StockReserveRequest request) {
        inventoryService.releaseStock(request);
        return ApiResponse.success(null);
    }
}
