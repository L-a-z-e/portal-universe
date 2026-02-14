package com.portal.universe.shoppingsellerservice.inventory.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingsellerservice.common.exception.SellerErrorCode;
import com.portal.universe.shoppingsellerservice.inventory.domain.Inventory;
import com.portal.universe.shoppingsellerservice.inventory.domain.MovementType;
import com.portal.universe.shoppingsellerservice.inventory.domain.StockMovement;
import com.portal.universe.shoppingsellerservice.inventory.dto.InventoryResponse;
import com.portal.universe.shoppingsellerservice.inventory.dto.StockAddRequest;
import com.portal.universe.shoppingsellerservice.inventory.dto.StockReserveRequest;
import com.portal.universe.shoppingsellerservice.inventory.repository.InventoryRepository;
import com.portal.universe.shoppingsellerservice.inventory.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;

    @Override
    public InventoryResponse getInventory(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new CustomBusinessException(SellerErrorCode.INVENTORY_NOT_FOUND));
        return InventoryResponse.from(inventory);
    }

    @Override
    @Transactional
    public InventoryResponse addStock(Long productId, StockAddRequest request, String performedBy) {
        Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new CustomBusinessException(SellerErrorCode.INVENTORY_NOT_FOUND));

        int prevAvailable = inventory.getAvailableQuantity();
        int prevReserved = inventory.getReservedQuantity();
        inventory.addStock(request.quantity());

        recordMovement(inventory, MovementType.ADD, request.quantity(),
                prevAvailable, inventory.getAvailableQuantity(),
                prevReserved, inventory.getReservedQuantity(),
                "MANUAL", null, request.reason(), performedBy);

        return InventoryResponse.from(inventory);
    }

    @Override
    @Transactional
    public InventoryResponse initializeInventory(Long productId, Integer initialQuantity) {
        if (inventoryRepository.findByProductId(productId).isPresent()) {
            throw new CustomBusinessException(SellerErrorCode.INVENTORY_ALREADY_EXISTS);
        }
        Inventory inventory = Inventory.builder()
                .productId(productId)
                .initialQuantity(initialQuantity)
                .build();
        inventoryRepository.save(inventory);

        recordMovement(inventory, MovementType.ADD, initialQuantity,
                0, initialQuantity, 0, 0,
                "INIT", null, "Initial inventory", "SYSTEM");

        return InventoryResponse.from(inventory);
    }

    @Override
    @Transactional
    public void reserveStock(StockReserveRequest request) {
        List<Long> productIds = new ArrayList<>(request.items().keySet());
        productIds.sort(Long::compareTo);

        List<Inventory> inventories = inventoryRepository.findByProductIdsForUpdate(productIds);

        for (Map.Entry<Long, Integer> entry : request.items().entrySet()) {
            Inventory inventory = inventories.stream()
                    .filter(i -> i.getProductId().equals(entry.getKey()))
                    .findFirst()
                    .orElseThrow(() -> new CustomBusinessException(SellerErrorCode.INVENTORY_NOT_FOUND));

            int prevAvailable = inventory.getAvailableQuantity();
            int prevReserved = inventory.getReservedQuantity();
            inventory.reserve(entry.getValue());

            recordMovement(inventory, MovementType.RESERVE, entry.getValue(),
                    prevAvailable, inventory.getAvailableQuantity(),
                    prevReserved, inventory.getReservedQuantity(),
                    "ORDER", request.orderNumber(), "Stock reserved for order", "SYSTEM");
        }
    }

    @Override
    @Transactional
    public void deductStock(StockReserveRequest request) {
        List<Long> productIds = new ArrayList<>(request.items().keySet());
        productIds.sort(Long::compareTo);

        List<Inventory> inventories = inventoryRepository.findByProductIdsForUpdate(productIds);

        for (Map.Entry<Long, Integer> entry : request.items().entrySet()) {
            Inventory inventory = inventories.stream()
                    .filter(i -> i.getProductId().equals(entry.getKey()))
                    .findFirst()
                    .orElseThrow(() -> new CustomBusinessException(SellerErrorCode.INVENTORY_NOT_FOUND));

            int prevAvailable = inventory.getAvailableQuantity();
            int prevReserved = inventory.getReservedQuantity();
            inventory.deduct(entry.getValue());

            recordMovement(inventory, MovementType.DEDUCT, entry.getValue(),
                    prevAvailable, inventory.getAvailableQuantity(),
                    prevReserved, inventory.getReservedQuantity(),
                    "ORDER", request.orderNumber(), "Stock deducted after payment", "SYSTEM");
        }
    }

    @Override
    @Transactional
    public void releaseStock(StockReserveRequest request) {
        List<Long> productIds = new ArrayList<>(request.items().keySet());
        productIds.sort(Long::compareTo);

        List<Inventory> inventories = inventoryRepository.findByProductIdsForUpdate(productIds);

        for (Map.Entry<Long, Integer> entry : request.items().entrySet()) {
            Inventory inventory = inventories.stream()
                    .filter(i -> i.getProductId().equals(entry.getKey()))
                    .findFirst()
                    .orElseThrow(() -> new CustomBusinessException(SellerErrorCode.INVENTORY_NOT_FOUND));

            int prevAvailable = inventory.getAvailableQuantity();
            int prevReserved = inventory.getReservedQuantity();
            inventory.release(entry.getValue());

            recordMovement(inventory, MovementType.RELEASE, entry.getValue(),
                    prevAvailable, inventory.getAvailableQuantity(),
                    prevReserved, inventory.getReservedQuantity(),
                    "ORDER", request.orderNumber(), "Stock released due to cancellation", "SYSTEM");
        }
    }

    private void recordMovement(Inventory inventory, MovementType type, int quantity,
                                int prevAvail, int afterAvail, int prevReserved, int afterReserved,
                                String refType, String refId, String reason, String performedBy) {
        StockMovement movement = StockMovement.builder()
                .inventoryId(inventory.getId())
                .productId(inventory.getProductId())
                .movementType(type)
                .quantity(quantity)
                .previousAvailable(prevAvail)
                .afterAvailable(afterAvail)
                .previousReserved(prevReserved)
                .afterReserved(afterReserved)
                .referenceType(refType)
                .referenceId(refId)
                .reason(reason)
                .performedBy(performedBy)
                .build();
        stockMovementRepository.save(movement);
    }
}
