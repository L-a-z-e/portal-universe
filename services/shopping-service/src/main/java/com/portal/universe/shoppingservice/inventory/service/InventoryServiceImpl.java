package com.portal.universe.shoppingservice.inventory.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import com.portal.universe.shoppingservice.inventory.domain.Inventory;
import com.portal.universe.shoppingservice.inventory.dto.InventoryResponse;
import com.portal.universe.shoppingservice.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 재고 조회 서비스 구현체입니다. (Buyer 전용, 읽기 전용)
 *
 * 재고 관리(예약/차감/해제)는 shopping-seller-service에서 Feign으로 처리됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    @Override
    public InventoryResponse getInventory(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.INVENTORY_NOT_FOUND));
        return InventoryResponse.from(inventory);
    }

    @Override
    public List<InventoryResponse> getInventories(List<Long> productIds) {
        return inventoryRepository.findByProductIds(productIds).stream()
                .map(InventoryResponse::from)
                .collect(Collectors.toList());
    }
}
