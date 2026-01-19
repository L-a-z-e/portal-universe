package com.portal.universe.shoppingservice.timedeal.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record TimeDealCreateRequest(
        @NotBlank(message = "Time deal name is required")
        @Size(max = 100, message = "Name must be less than 100 characters")
        String name,

        String description,

        @NotNull(message = "Start time is required")
        LocalDateTime startsAt,

        @NotNull(message = "End time is required")
        @Future(message = "End time must be in the future")
        LocalDateTime endsAt,

        @NotEmpty(message = "At least one product is required")
        @Valid
        List<TimeDealProductRequest> products
) {
    @Builder
    public record TimeDealProductRequest(
            @NotNull(message = "Product ID is required")
            Long productId,

            @NotNull(message = "Deal price is required")
            @DecimalMin(value = "0.01", message = "Deal price must be greater than 0")
            BigDecimal dealPrice,

            @NotNull(message = "Deal quantity is required")
            @Min(value = 1, message = "Deal quantity must be at least 1")
            Integer dealQuantity,

            @NotNull(message = "Max per user is required")
            @Min(value = 1, message = "Max per user must be at least 1")
            Integer maxPerUser
    ) {}
}
