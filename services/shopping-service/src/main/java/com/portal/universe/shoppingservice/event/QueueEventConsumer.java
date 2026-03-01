package com.portal.universe.shoppingservice.event;

import com.portal.universe.event.seller.QueueActivatedEvent;
import com.portal.universe.event.seller.QueueDeactivatedEvent;
import com.portal.universe.event.seller.SellerTopics;
import com.portal.universe.shoppingservice.queue.domain.WaitingQueue;
import com.portal.universe.shoppingservice.queue.repository.WaitingQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueEventConsumer {

    private final WaitingQueueRepository waitingQueueRepository;

    @KafkaListener(topics = SellerTopics.QUEUE_ACTIVATED, groupId = "shopping-service",
            containerFactory = "avroKafkaListenerContainerFactory")
    @Transactional
    public void onQueueActivated(QueueActivatedEvent event) {
        log.info("Received QueueActivatedEvent: eventType={}, eventId={}",
                event.getEventType(), event.getEventId());

        WaitingQueue queue = waitingQueueRepository
                .findByEventTypeAndEventId(event.getEventType().toString(), event.getEventId())
                .orElseGet(() -> WaitingQueue.builder()
                        .eventType(event.getEventType().toString())
                        .eventId(event.getEventId())
                        .maxCapacity(event.getMaxCapacity())
                        .entryBatchSize(event.getEntryBatchSize())
                        .entryIntervalSeconds(event.getEntryIntervalSeconds())
                        .build());

        queue.updateConfig(event.getMaxCapacity(), event.getEntryBatchSize(), event.getEntryIntervalSeconds());
        queue.activate();

        waitingQueueRepository.save(queue);
        log.info("Synced queue activation: eventType={}, eventId={}",
                event.getEventType(), event.getEventId());
    }

    @KafkaListener(topics = SellerTopics.QUEUE_DEACTIVATED, groupId = "shopping-service",
            containerFactory = "avroKafkaListenerContainerFactory")
    @Transactional
    public void onQueueDeactivated(QueueDeactivatedEvent event) {
        log.info("Received QueueDeactivatedEvent: eventType={}, eventId={}",
                event.getEventType(), event.getEventId());

        waitingQueueRepository
                .findByEventTypeAndEventIdAndIsActiveTrue(event.getEventType().toString(), event.getEventId())
                .ifPresentOrElse(
                        queue -> {
                            queue.deactivate();
                            log.info("Synced queue deactivation: eventType={}, eventId={}",
                                    event.getEventType(), event.getEventId());
                        },
                        () -> log.warn("Active queue not found for eventType={}, eventId={}, skipping deactivation",
                                event.getEventType(), event.getEventId())
                );
    }
}
