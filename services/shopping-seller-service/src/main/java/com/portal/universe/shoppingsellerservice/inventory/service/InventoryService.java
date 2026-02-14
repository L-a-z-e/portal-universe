package com.portal.universe.shoppingsellerservice.inventory.service;

import com.portal.universe.shoppingsellerservice.inventory.dto.InventoryResponse;
import com.portal.universe.shoppingsellerservice.inventory.dto.StockAddRequest;
import com.portal.universe.shoppingsellerservice.inventory.dto.StockMovementResponse;
import com.portal.universe.shoppingsellerservice.inventory.dto.StockReserveRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryService {
    InventoryResponse getInventory(Long productId);
    InventoryResponse addStock(Long productId, StockAddRequest request, String performedBy);
    InventoryResponse initializeInventory(Long productId, Integer initialQuantity);
    Page<StockMovementResponse> getMovements(Long productId, Pageable pageable);
    void reserveStock(StockReserveRequest request);
    void deductStock(StockReserveRequest request);
    void releaseStock(StockReserveRequest request);
}
