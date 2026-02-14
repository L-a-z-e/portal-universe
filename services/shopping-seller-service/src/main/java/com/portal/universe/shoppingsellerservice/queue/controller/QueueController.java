package com.portal.universe.shoppingsellerservice.queue.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingsellerservice.queue.dto.QueueActivateRequest;
import com.portal.universe.shoppingsellerservice.queue.dto.QueueStatusResponse;
import com.portal.universe.shoppingsellerservice.queue.service.QueueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    @PostMapping("/{eventType}/{eventId}/activate")
    public ApiResponse<QueueStatusResponse> activateQueue(
            @PathVariable String eventType,
            @PathVariable Long eventId,
            @Valid @RequestBody QueueActivateRequest request) {
        return ApiResponse.success(queueService.activateQueue(eventType, eventId, request));
    }

    @PostMapping("/{eventType}/{eventId}/deactivate")
    public ApiResponse<Void> deactivateQueue(
            @PathVariable String eventType,
            @PathVariable Long eventId) {
        queueService.deactivateQueue(eventType, eventId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{eventType}/{eventId}/status")
    public ApiResponse<QueueStatusResponse> getQueueStatus(
            @PathVariable String eventType,
            @PathVariable Long eventId) {
        return ApiResponse.success(queueService.getQueueStatus(eventType, eventId));
    }
}
