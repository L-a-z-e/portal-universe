package com.portal.universe.shoppingservice.queue.service;

import com.portal.universe.shoppingservice.queue.dto.QueueStatusResponse;

/**
 * QueueService
 * 대기열 관리 서비스 인터페이스
 */
public interface QueueService {

    QueueStatusResponse enterQueue(String eventType, Long eventId, String userId);

    QueueStatusResponse getQueueStatus(String eventType, Long eventId, String userId);

    QueueStatusResponse getQueueStatusByToken(String entryToken);

    void leaveQueue(String eventType, Long eventId, String userId);

    void leaveQueueByToken(String entryToken);

    void processEntries(String eventType, Long eventId);

    boolean validateEntry(String eventType, Long eventId, String userId);

    boolean isQueueActive(String eventType, Long eventId);

    void validateTokenOwnership(String entryToken, String userId);
}
