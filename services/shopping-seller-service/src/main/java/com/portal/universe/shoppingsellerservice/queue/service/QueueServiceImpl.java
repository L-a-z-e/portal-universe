package com.portal.universe.shoppingsellerservice.queue.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingsellerservice.common.exception.SellerErrorCode;
import com.portal.universe.shoppingsellerservice.queue.domain.QueueEntryStatus;
import com.portal.universe.shoppingsellerservice.queue.domain.WaitingQueue;
import com.portal.universe.shoppingsellerservice.queue.dto.QueueActivateRequest;
import com.portal.universe.shoppingsellerservice.queue.dto.QueueStatusResponse;
import com.portal.universe.shoppingsellerservice.queue.repository.QueueEntryRepository;
import com.portal.universe.shoppingsellerservice.queue.repository.WaitingQueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QueueServiceImpl implements QueueService {

    private final WaitingQueueRepository waitingQueueRepository;
    private final QueueEntryRepository queueEntryRepository;

    @Override
    @Transactional
    public QueueStatusResponse activateQueue(String eventType, Long eventId, QueueActivateRequest request) {
        WaitingQueue queue = waitingQueueRepository.findByEventTypeAndEventId(eventType, eventId)
                .orElseGet(() -> WaitingQueue.builder()
                        .eventType(eventType)
                        .eventId(eventId)
                        .maxCapacity(request.maxCapacity())
                        .entryBatchSize(request.entryBatchSize())
                        .entryIntervalSeconds(request.entryIntervalSeconds())
                        .build());

        if (queue.getIsActive()) {
            throw new CustomBusinessException(SellerErrorCode.QUEUE_ALREADY_ACTIVE);
        }

        queue.activate();
        WaitingQueue saved = waitingQueueRepository.save(queue);
        return toStatusResponse(saved);
    }

    @Override
    @Transactional
    public void deactivateQueue(String eventType, Long eventId) {
        WaitingQueue queue = waitingQueueRepository.findByEventTypeAndEventId(eventType, eventId)
                .orElseThrow(() -> new CustomBusinessException(SellerErrorCode.QUEUE_NOT_FOUND));
        if (!queue.getIsActive()) {
            throw new CustomBusinessException(SellerErrorCode.QUEUE_NOT_ACTIVE);
        }
        queue.deactivate();
    }

    @Override
    public QueueStatusResponse getQueueStatus(String eventType, Long eventId) {
        WaitingQueue queue = waitingQueueRepository.findByEventTypeAndEventId(eventType, eventId)
                .orElseThrow(() -> new CustomBusinessException(SellerErrorCode.QUEUE_NOT_FOUND));
        return toStatusResponse(queue);
    }

    private QueueStatusResponse toStatusResponse(WaitingQueue queue) {
        long waitingCount = queueEntryRepository.countByQueueIdAndStatus(queue.getId(), QueueEntryStatus.WAITING);
        long enteredCount = queueEntryRepository.countByQueueIdAndStatus(queue.getId(), QueueEntryStatus.ENTERED);
        return new QueueStatusResponse(
                queue.getId(),
                queue.getEventType(),
                queue.getEventId(),
                queue.getIsActive(),
                waitingCount,
                enteredCount,
                queue.getMaxCapacity(),
                queue.getEntryBatchSize(),
                queue.getEntryIntervalSeconds()
        );
    }
}
