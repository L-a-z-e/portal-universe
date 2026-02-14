package com.portal.universe.shoppingsellerservice.queue.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record QueueActivateRequest(
        @NotNull @Min(1) Integer maxCapacity,
        @NotNull @Min(1) Integer entryBatchSize,
        @NotNull @Min(1) Integer entryIntervalSeconds
) {}
