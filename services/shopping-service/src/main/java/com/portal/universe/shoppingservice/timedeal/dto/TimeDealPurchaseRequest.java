package com.portal.universe.shoppingservice.timedeal.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record TimeDealPurchaseRequest(
        @NotNull(message = "Product ID is required")
        Long timeDealProductId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity
) {}
