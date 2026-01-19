package com.portal.universe.shoppingservice.queue.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingservice.queue.dto.QueueActivateRequest;
import com.portal.universe.shoppingservice.queue.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AdminQueueController
 * 관리자 대기열 관리 API
 */
@RestController
@RequestMapping("/api/admin/shopping/queue")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Queue", description = "관리자 대기열 관리 API")
public class AdminQueueController {

    private final QueueService queueService;

    @PostMapping("/{eventType}/{eventId}/activate")
    @Operation(summary = "대기열 활성화", description = "이벤트에 대한 대기열을 활성화합니다")
    public ResponseEntity<ApiResponse<Void>> activateQueue(
            @PathVariable String eventType,
            @PathVariable Long eventId,
            @Valid @RequestBody QueueActivateRequest request
    ) {
        queueService.activateQueue(
            eventType,
            eventId,
            request.maxCapacity(),
            request.entryBatchSize(),
            request.entryIntervalSeconds()
        );
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{eventType}/{eventId}/deactivate")
    @Operation(summary = "대기열 비활성화", description = "이벤트에 대한 대기열을 비활성화합니다")
    public ResponseEntity<ApiResponse<Void>> deactivateQueue(
            @PathVariable String eventType,
            @PathVariable Long eventId
    ) {
        queueService.deactivateQueue(eventType, eventId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{eventType}/{eventId}/process")
    @Operation(summary = "대기열 수동 처리", description = "대기열 입장을 수동으로 처리합니다")
    public ResponseEntity<ApiResponse<Void>> processQueue(
            @PathVariable String eventType,
            @PathVariable Long eventId
    ) {
        queueService.processEntries(eventType, eventId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
