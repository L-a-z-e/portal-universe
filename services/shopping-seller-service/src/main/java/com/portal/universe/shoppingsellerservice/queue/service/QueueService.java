package com.portal.universe.shoppingsellerservice.queue.service;

import com.portal.universe.shoppingsellerservice.queue.dto.QueueActivateRequest;
import com.portal.universe.shoppingsellerservice.queue.dto.QueueStatusResponse;

public interface QueueService {
    QueueStatusResponse activateQueue(String eventType, Long eventId, QueueActivateRequest request);
    void deactivateQueue(String eventType, Long eventId);
    QueueStatusResponse getQueueStatus(String eventType, Long eventId);
}
