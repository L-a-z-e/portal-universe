package com.portal.universe.shoppingservice.queue.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * QueueActivateRequest
 * 대기열 활성화 요청 DTO
 */
public record QueueActivateRequest(
    @NotNull(message = "Max capacity is required")
    @Min(value = 1, message = "Max capacity must be at least 1")
    Integer maxCapacity,

    @NotNull(message = "Entry batch size is required")
    @Min(value = 1, message = "Entry batch size must be at least 1")
    Integer entryBatchSize,

    @NotNull(message = "Entry interval is required")
    @Min(value = 1, message = "Entry interval must be at least 1 second")
    Integer entryIntervalSeconds
) {}
