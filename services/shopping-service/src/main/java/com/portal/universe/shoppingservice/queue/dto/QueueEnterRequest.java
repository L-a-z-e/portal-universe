package com.portal.universe.shoppingservice.queue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * QueueEnterRequest
 * 대기열 진입 요청 DTO
 */
public record QueueEnterRequest(
    @NotBlank(message = "Event type is required")
    String eventType,

    @NotNull(message = "Event ID is required")
    Long eventId
) {}
