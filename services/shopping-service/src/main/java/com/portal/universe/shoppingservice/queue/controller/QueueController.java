package com.portal.universe.shoppingservice.queue.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingservice.queue.dto.QueueStatusResponse;
import com.portal.universe.shoppingservice.queue.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * QueueController
 * 대기열 API 컨트롤러
 */
@RestController
@RequestMapping("/api/shopping/queue")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Queue", description = "대기열 관리 API")
public class QueueController {

    private final QueueService queueService;

    @PostMapping("/{eventType}/{eventId}/enter")
    @Operation(summary = "대기열 진입", description = "이벤트 대기열에 진입합니다")
    public ResponseEntity<ApiResponse<QueueStatusResponse>> enterQueue(
            @PathVariable String eventType,
            @PathVariable Long eventId,
            HttpServletRequest request
    ) {
        Long userId = extractUserId(request);
        QueueStatusResponse response = queueService.enterQueue(eventType, eventId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{eventType}/{eventId}/status")
    @Operation(summary = "대기열 상태 조회", description = "현재 대기 상태를 조회합니다")
    public ResponseEntity<ApiResponse<QueueStatusResponse>> getQueueStatus(
            @PathVariable String eventType,
            @PathVariable Long eventId,
            HttpServletRequest request
    ) {
        Long userId = extractUserId(request);
        QueueStatusResponse response = queueService.getQueueStatus(eventType, eventId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/token/{entryToken}")
    @Operation(summary = "토큰으로 대기열 상태 조회", description = "토큰으로 대기 상태를 조회합니다")
    public ResponseEntity<ApiResponse<QueueStatusResponse>> getQueueStatusByToken(
            @PathVariable String entryToken
    ) {
        QueueStatusResponse response = queueService.getQueueStatusByToken(entryToken);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{eventType}/{eventId}/leave")
    @Operation(summary = "대기열 이탈", description = "대기열에서 나갑니다")
    public ResponseEntity<ApiResponse<Void>> leaveQueue(
            @PathVariable String eventType,
            @PathVariable Long eventId,
            HttpServletRequest request
    ) {
        Long userId = extractUserId(request);
        queueService.leaveQueue(eventType, eventId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/token/{entryToken}")
    @Operation(summary = "토큰으로 대기열 이탈", description = "토큰으로 대기열에서 나갑니다")
    public ResponseEntity<ApiResponse<Void>> leaveQueueByToken(
            @PathVariable String entryToken
    ) {
        queueService.leaveQueueByToken(entryToken);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private Long extractUserId(HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null) {
            throw new IllegalArgumentException("User ID header is missing");
        }
        return Long.parseLong(userIdHeader);
    }
}
