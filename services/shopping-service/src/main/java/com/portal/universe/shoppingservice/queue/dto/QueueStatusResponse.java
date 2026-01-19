package com.portal.universe.shoppingservice.queue.dto;

import com.portal.universe.shoppingservice.queue.domain.QueueStatus;

/**
 * QueueStatusResponse
 * 대기열 상태 응답 DTO
 */
public record QueueStatusResponse(
    String entryToken,
    QueueStatus status,
    Long position,            // 대기 순번
    Long estimatedWaitSeconds, // 예상 대기 시간 (초)
    Long totalWaiting,        // 전체 대기 인원
    String message
) {
    public static QueueStatusResponse waiting(String entryToken, Long position, Long estimatedWaitSeconds, Long totalWaiting) {
        return new QueueStatusResponse(
            entryToken,
            QueueStatus.WAITING,
            position,
            estimatedWaitSeconds,
            totalWaiting,
            String.format("현재 %d번째 대기 중입니다. 예상 대기 시간: %d초", position, estimatedWaitSeconds)
        );
    }

    public static QueueStatusResponse entered(String entryToken) {
        return new QueueStatusResponse(
            entryToken,
            QueueStatus.ENTERED,
            0L,
            0L,
            0L,
            "입장이 완료되었습니다."
        );
    }

    public static QueueStatusResponse expired(String entryToken) {
        return new QueueStatusResponse(
            entryToken,
            QueueStatus.EXPIRED,
            0L,
            0L,
            0L,
            "대기열이 만료되었습니다."
        );
    }

    public static QueueStatusResponse left(String entryToken) {
        return new QueueStatusResponse(
            entryToken,
            QueueStatus.LEFT,
            0L,
            0L,
            0L,
            "대기열에서 나갔습니다."
        );
    }
}
