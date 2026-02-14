package com.portal.universe.shoppingsellerservice.queue.dto;

public record QueueStatusResponse(
        Long queueId,
        String eventType,
        Long eventId,
        boolean isActive,
        long waitingCount,
        long enteredCount,
        Integer maxCapacity,
        Integer entryBatchSize,
        Integer entryIntervalSeconds
) {}
