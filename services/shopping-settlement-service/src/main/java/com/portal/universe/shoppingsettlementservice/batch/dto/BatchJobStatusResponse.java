package com.portal.universe.shoppingsettlementservice.batch.dto;

import org.springframework.batch.core.BatchStatus;

import java.time.LocalDateTime;

public record BatchJobStatusResponse(
        Long executionId,
        BatchStatus status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String exitDescription
) {
}
