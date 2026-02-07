package com.portal.universe.commonlibrary.response;

import java.time.Instant;

/**
 * SSE 이벤트의 표준 envelope 구조입니다.
 * 모든 SSE endpoint에서 일관된 데이터 형식을 보장합니다.
 *
 * @param type      이벤트 타입 (e.g., "queue-status", "inventory-update")
 * @param data      이벤트 페이로드
 * @param timestamp ISO-8601 형식의 타임스탬프
 * @param <T>       페이로드 타입
 */
public record SseEnvelope<T>(String type, T data, String timestamp) {

    public static <T> SseEnvelope<T> of(String type, T data) {
        return new SseEnvelope<>(type, data, Instant.now().toString());
    }

    public static SseEnvelope<Void> heartbeat() {
        return new SseEnvelope<>("heartbeat", null, Instant.now().toString());
    }
}
