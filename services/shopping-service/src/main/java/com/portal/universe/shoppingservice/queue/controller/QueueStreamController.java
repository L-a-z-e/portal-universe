package com.portal.universe.shoppingservice.queue.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.commonlibrary.response.SseEnvelope;
import com.portal.universe.shoppingservice.queue.dto.QueueStatusResponse;
import com.portal.universe.shoppingservice.queue.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

/**
 * QueueStreamController
 * SSE 기반 실시간 대기열 상태 업데이트
 */
@RestController
@RequestMapping("/queue")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Queue Stream", description = "대기열 실시간 스트림 API")
public class QueueStreamController {

    private final QueueService queueService;
    private final ObjectMapper objectMapper;

    // 활성 SSE 연결 관리
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @GetMapping(value = "/{eventType}/{eventId}/subscribe/{entryToken}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "대기열 상태 구독", description = "SSE를 통해 실시간 대기열 상태를 받습니다")
    public SseEmitter subscribe(
            @PathVariable String eventType,
            @PathVariable Long eventId,
            @PathVariable String entryToken
    ) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5분 타임아웃
        String emitterKey = entryToken;

        // 이전 연결 정리
        SseEmitter oldEmitter = emitters.put(emitterKey, emitter);
        if (oldEmitter != null) {
            oldEmitter.complete();
        }

        // 연결 완료/에러/타임아웃 시 정리
        Runnable cleanup = () -> {
            emitters.remove(emitterKey);
            cancelScheduledTask(emitterKey);
        };

        emitter.onCompletion(() -> {
            cleanup.run();
            log.debug("SSE connection completed for token: {}", entryToken);
        });

        emitter.onError(ex -> {
            cleanup.run();
            log.debug("SSE connection error for token: {}", entryToken);
        });

        emitter.onTimeout(() -> {
            cleanup.run();
            emitter.complete();
            log.debug("SSE connection timed out for token: {}", entryToken);
        });

        // 초기 상태 전송
        sendStatusUpdate(emitter, eventType, eventId, entryToken);

        // 주기적 업데이트 스케줄링 (3초마다)
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            if (emitters.containsKey(emitterKey)) {
                sendStatusUpdate(emitter, eventType, eventId, entryToken);
            }
        }, 3, 3, TimeUnit.SECONDS);
        scheduledTasks.put(emitterKey, future);

        return emitter;
    }

    private void sendStatusUpdate(SseEmitter emitter, String eventType, Long eventId, String entryToken) {
        try {
            QueueStatusResponse status = queueService.getQueueStatusByToken(entryToken);
            SseEnvelope<QueueStatusResponse> envelope = SseEnvelope.of("queue-status", status);

            emitter.send(SseEmitter.event()
                .name("queue-status")
                .data(objectMapper.writeValueAsString(envelope)));

            // 입장 완료 또는 만료 시 연결 종료
            if (status.status() != com.portal.universe.shoppingservice.queue.domain.QueueStatus.WAITING) {
                emitter.complete();
                emitters.remove(entryToken);
            }
        } catch (IOException e) {
            log.debug("Failed to send SSE update for token: {}", entryToken);
            emitter.completeWithError(e);
            emitters.remove(entryToken);
        } catch (Exception e) {
            log.error("Error processing queue status for token: {}", entryToken, e);
            emitter.completeWithError(e);
            emitters.remove(entryToken);
        }
    }

    private void cancelScheduledTask(String key) {
        ScheduledFuture<?> future = scheduledTasks.remove(key);
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * 특정 토큰에 대한 연결 상태 확인
     */
    public boolean isConnected(String entryToken) {
        return emitters.containsKey(entryToken);
    }

    /**
     * 모든 연결 종료 (서버 종료 시)
     */
    @PreDestroy
    public void closeAllConnections() {
        scheduledTasks.values().forEach(f -> f.cancel(false));
        scheduledTasks.clear();
        emitters.values().forEach(SseEmitter::complete);
        emitters.clear();
        scheduler.shutdown();
    }
}
