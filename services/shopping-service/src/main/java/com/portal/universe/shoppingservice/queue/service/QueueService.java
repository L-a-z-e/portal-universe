package com.portal.universe.shoppingservice.queue.service;

import com.portal.universe.shoppingservice.queue.dto.QueueStatusResponse;

/**
 * QueueService
 * 대기열 관리 서비스 인터페이스
 */
public interface QueueService {

    /**
     * 대기열 진입
     */
    QueueStatusResponse enterQueue(String eventType, Long eventId, Long userId);

    /**
     * 대기열 상태 조회
     */
    QueueStatusResponse getQueueStatus(String eventType, Long eventId, Long userId);

    /**
     * 대기열 상태 조회 (토큰 기반)
     */
    QueueStatusResponse getQueueStatusByToken(String entryToken);

    /**
     * 대기열 이탈
     */
    void leaveQueue(String eventType, Long eventId, Long userId);

    /**
     * 대기열 이탈 (토큰 기반)
     */
    void leaveQueueByToken(String entryToken);

    /**
     * 사용자 입장 처리 (스케줄러에서 호출)
     */
    void processEntries(String eventType, Long eventId);

    /**
     * 입장 확인 (구매 시 호출)
     */
    boolean validateEntry(String eventType, Long eventId, Long userId);

    /**
     * 대기열 생성/활성화 (Admin)
     */
    void activateQueue(String eventType, Long eventId, Integer maxCapacity, Integer entryBatchSize, Integer entryIntervalSeconds);

    /**
     * 대기열 비활성화 (Admin)
     */
    void deactivateQueue(String eventType, Long eventId);
}
