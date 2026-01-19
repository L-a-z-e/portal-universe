package com.portal.universe.shoppingservice.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import com.portal.universe.shoppingservice.inventory.domain.Inventory;
import com.portal.universe.shoppingservice.inventory.domain.MovementType;
import com.portal.universe.shoppingservice.inventory.domain.StockMovement;
import com.portal.universe.shoppingservice.inventory.dto.InventoryResponse;
import com.portal.universe.shoppingservice.inventory.dto.StockMovementResponse;
import com.portal.universe.shoppingservice.inventory.repository.InventoryRepository;
import com.portal.universe.shoppingservice.inventory.repository.StockMovementRepository;
import com.portal.universe.shoppingservice.inventory.stream.InventoryUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 재고 관리 서비스 구현체입니다.
 * 비관적 락(Pessimistic Lock)을 사용하여 동시성 문제를 해결합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public InventoryResponse getInventory(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.INVENTORY_NOT_FOUND));
        return InventoryResponse.from(inventory);
    }

    @Override
    @Transactional
    public InventoryResponse initializeInventory(Long productId, int initialStock, String userId) {
        if (inventoryRepository.existsByProductId(productId)) {
            throw new CustomBusinessException(ShoppingErrorCode.INVENTORY_ALREADY_EXISTS);
        }

        Inventory inventory = Inventory.builder()
                .productId(productId)
                .initialQuantity(initialStock)
                .build();

        Inventory savedInventory = inventoryRepository.save(inventory);

        // 재고 이동 이력 기록
        recordMovement(savedInventory, MovementType.INITIAL, initialStock,
                0, initialStock, 0, 0,
                "SYSTEM", String.valueOf(productId), "Initial stock setup", userId);

        log.info("Initialized inventory for product {}: {} units", productId, initialStock);
        return InventoryResponse.from(savedInventory);
    }

    @Override
    @Transactional
    public InventoryResponse reserveStock(Long productId, int quantity, String referenceType, String referenceId, String userId) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.INVENTORY_NOT_FOUND));

        int previousAvailable = inventory.getAvailableQuantity();
        int previousReserved = inventory.getReservedQuantity();

        inventory.reserve(quantity);
        Inventory savedInventory = inventoryRepository.save(inventory);

        // 재고 이동 이력 기록
        recordMovement(savedInventory, MovementType.RESERVE, quantity,
                previousAvailable, savedInventory.getAvailableQuantity(),
                previousReserved, savedInventory.getReservedQuantity(),
                referenceType, referenceId, "Stock reserved for order", userId);

        publishInventoryUpdate(savedInventory);

        log.info("Reserved {} units for product {} (ref: {})", quantity, productId, referenceId);
        return InventoryResponse.from(savedInventory);
    }

    @Override
    @Transactional
    public List<InventoryResponse> reserveStockBatch(Map<Long, Integer> quantities, String referenceType, String referenceId, String userId) {
        // 데드락 방지: 상품 ID 순서로 정렬
        Map<Long, Integer> sortedQuantities = new TreeMap<>(quantities);
        List<Long> productIds = new ArrayList<>(sortedQuantities.keySet());

        // 정렬된 순서로 락 획득
        List<Inventory> inventories = inventoryRepository.findByProductIdsWithLock(productIds);

        if (inventories.size() != productIds.size()) {
            throw new CustomBusinessException(ShoppingErrorCode.INVENTORY_NOT_FOUND);
        }

        List<InventoryResponse> responses = new ArrayList<>();

        for (Inventory inventory : inventories) {
            int quantity = sortedQuantities.get(inventory.getProductId());
            int previousAvailable = inventory.getAvailableQuantity();
            int previousReserved = inventory.getReservedQuantity();

            inventory.reserve(quantity);

            // 재고 이동 이력 기록
            recordMovement(inventory, MovementType.RESERVE, quantity,
                    previousAvailable, inventory.getAvailableQuantity(),
                    previousReserved, inventory.getReservedQuantity(),
                    referenceType, referenceId, "Stock reserved for order (batch)", userId);

            responses.add(InventoryResponse.from(inventory));
        }

        inventoryRepository.saveAll(inventories);

        // Publish updates for all inventories
        inventories.forEach(this::publishInventoryUpdate);

        log.info("Batch reserved stock for {} products (ref: {})", productIds.size(), referenceId);
        return responses;
    }

    @Override
    @Transactional
    public InventoryResponse deductStock(Long productId, int quantity, String referenceType, String referenceId, String userId) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.INVENTORY_NOT_FOUND));

        int previousAvailable = inventory.getAvailableQuantity();
        int previousReserved = inventory.getReservedQuantity();

        inventory.deduct(quantity);
        Inventory savedInventory = inventoryRepository.save(inventory);

        // 재고 이동 이력 기록
        recordMovement(savedInventory, MovementType.DEDUCT, quantity,
                previousAvailable, savedInventory.getAvailableQuantity(),
                previousReserved, savedInventory.getReservedQuantity(),
                referenceType, referenceId, "Stock deducted after payment", userId);

        publishInventoryUpdate(savedInventory);

        log.info("Deducted {} units from product {} (ref: {})", quantity, productId, referenceId);
        return InventoryResponse.from(savedInventory);
    }

    @Override
    @Transactional
    public List<InventoryResponse> deductStockBatch(Map<Long, Integer> quantities, String referenceType, String referenceId, String userId) {
        Map<Long, Integer> sortedQuantities = new TreeMap<>(quantities);
        List<Long> productIds = new ArrayList<>(sortedQuantities.keySet());

        List<Inventory> inventories = inventoryRepository.findByProductIdsWithLock(productIds);

        if (inventories.size() != productIds.size()) {
            throw new CustomBusinessException(ShoppingErrorCode.INVENTORY_NOT_FOUND);
        }

        List<InventoryResponse> responses = new ArrayList<>();

        for (Inventory inventory : inventories) {
            int quantity = sortedQuantities.get(inventory.getProductId());
            int previousAvailable = inventory.getAvailableQuantity();
            int previousReserved = inventory.getReservedQuantity();

            inventory.deduct(quantity);

            recordMovement(inventory, MovementType.DEDUCT, quantity,
                    previousAvailable, inventory.getAvailableQuantity(),
                    previousReserved, inventory.getReservedQuantity(),
                    referenceType, referenceId, "Stock deducted after payment (batch)", userId);

            responses.add(InventoryResponse.from(inventory));
        }

        inventoryRepository.saveAll(inventories);

        // Publish updates for all inventories
        inventories.forEach(this::publishInventoryUpdate);

        log.info("Batch deducted stock for {} products (ref: {})", productIds.size(), referenceId);
        return responses;
    }

    @Override
    @Transactional
    public InventoryResponse releaseStock(Long productId, int quantity, String referenceType, String referenceId, String userId) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.INVENTORY_NOT_FOUND));

        int previousAvailable = inventory.getAvailableQuantity();
        int previousReserved = inventory.getReservedQuantity();

        inventory.release(quantity);
        Inventory savedInventory = inventoryRepository.save(inventory);

        // 재고 이동 이력 기록
        recordMovement(savedInventory, MovementType.RELEASE, quantity,
                previousAvailable, savedInventory.getAvailableQuantity(),
                previousReserved, savedInventory.getReservedQuantity(),
                referenceType, referenceId, "Stock released due to cancellation", userId);

        publishInventoryUpdate(savedInventory);

        log.info("Released {} units for product {} (ref: {})", quantity, productId, referenceId);
        return InventoryResponse.from(savedInventory);
    }

    @Override
    @Transactional
    public List<InventoryResponse> releaseStockBatch(Map<Long, Integer> quantities, String referenceType, String referenceId, String userId) {
        Map<Long, Integer> sortedQuantities = new TreeMap<>(quantities);
        List<Long> productIds = new ArrayList<>(sortedQuantities.keySet());

        List<Inventory> inventories = inventoryRepository.findByProductIdsWithLock(productIds);

        if (inventories.size() != productIds.size()) {
            throw new CustomBusinessException(ShoppingErrorCode.INVENTORY_NOT_FOUND);
        }

        List<InventoryResponse> responses = new ArrayList<>();

        for (Inventory inventory : inventories) {
            int quantity = sortedQuantities.get(inventory.getProductId());
            int previousAvailable = inventory.getAvailableQuantity();
            int previousReserved = inventory.getReservedQuantity();

            inventory.release(quantity);

            recordMovement(inventory, MovementType.RELEASE, quantity,
                    previousAvailable, inventory.getAvailableQuantity(),
                    previousReserved, inventory.getReservedQuantity(),
                    referenceType, referenceId, "Stock released due to cancellation (batch)", userId);

            responses.add(InventoryResponse.from(inventory));
        }

        inventoryRepository.saveAll(inventories);

        // Publish updates for all inventories
        inventories.forEach(this::publishInventoryUpdate);

        log.info("Batch released stock for {} products (ref: {})", productIds.size(), referenceId);
        return responses;
    }

    @Override
    @Transactional
    public InventoryResponse addStock(Long productId, int quantity, String reason, String userId) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.INVENTORY_NOT_FOUND));

        int previousAvailable = inventory.getAvailableQuantity();
        int previousReserved = inventory.getReservedQuantity();

        inventory.addStock(quantity);
        Inventory savedInventory = inventoryRepository.save(inventory);

        // 재고 이동 이력 기록
        recordMovement(savedInventory, MovementType.INBOUND, quantity,
                previousAvailable, savedInventory.getAvailableQuantity(),
                previousReserved, savedInventory.getReservedQuantity(),
                "ADMIN", userId, reason, userId);

        publishInventoryUpdate(savedInventory);

        log.info("Added {} units to product {} by admin {}", quantity, productId, userId);
        return InventoryResponse.from(savedInventory);
    }

    @Override
    public Page<StockMovementResponse> getStockMovements(Long productId, Pageable pageable) {
        return stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(StockMovementResponse::from);
    }

    /**
     * 재고 이동 이력을 기록합니다.
     */
    private void recordMovement(Inventory inventory, MovementType movementType, int quantity,
                                int previousAvailable, int afterAvailable,
                                int previousReserved, int afterReserved,
                                String referenceType, String referenceId, String reason, String performedBy) {
        StockMovement movement = StockMovement.builder()
                .inventoryId(inventory.getId())
                .productId(inventory.getProductId())
                .movementType(movementType)
                .quantity(quantity)
                .previousAvailable(previousAvailable)
                .afterAvailable(afterAvailable)
                .previousReserved(previousReserved)
                .afterReserved(afterReserved)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .reason(reason)
                .performedBy(performedBy)
                .build();

        stockMovementRepository.save(movement);
    }

    /**
     * 재고 변동을 Redis Pub/Sub으로 발행합니다.
     */
    private void publishInventoryUpdate(Inventory inventory) {
        InventoryUpdate update = InventoryUpdate.builder()
                .productId(inventory.getProductId())
                .available(inventory.getAvailableQuantity())
                .reserved(inventory.getReservedQuantity())
                .timestamp(LocalDateTime.now())
                .build();

        String channel = "inventory:" + inventory.getProductId();
        try {
            String jsonPayload = objectMapper.writeValueAsString(update);
            redisTemplate.convertAndSend(channel, jsonPayload);
            log.debug("Published inventory update for product {}: available={}, reserved={}",
                    inventory.getProductId(), update.getAvailable(), update.getReserved());
        } catch (JsonProcessingException e) {
            log.error("Failed to publish inventory update for product {}", inventory.getProductId(), e);
        }
    }
}
