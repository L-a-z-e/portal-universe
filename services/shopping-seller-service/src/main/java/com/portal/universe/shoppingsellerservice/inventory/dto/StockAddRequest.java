package com.portal.universe.shoppingsellerservice.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockAddRequest(
        @NotNull @Min(1) Integer quantity,
        String reason
) {}
