package com.portal.universe.shoppingservice.inventory.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record InventoryBatchRequest(
        @NotEmpty(message = "상품 ID 목록은 필수입니다")
        List<Long> productIds
) {}
