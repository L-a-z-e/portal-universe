package com.portal.universe.shoppingsellerservice.inventory.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record StockReserveRequest(
        @NotNull String orderNumber,
        @NotEmpty Map<Long, Integer> items
) {}
