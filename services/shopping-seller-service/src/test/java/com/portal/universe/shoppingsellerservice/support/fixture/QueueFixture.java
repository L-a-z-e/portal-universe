package com.portal.universe.shoppingsellerservice.support.fixture;

import com.portal.universe.shoppingsellerservice.queue.domain.WaitingQueue;
import org.springframework.test.util.ReflectionTestUtils;

public final class QueueFixture {

    private QueueFixture() {}

    public static WaitingQueue create() {
        return builder().build();
    }

    public static WaitingQueueBuilder builder() {
        return new WaitingQueueBuilder();
    }

    public static class WaitingQueueBuilder {
        private Long id;
        private String eventType = "TIME_DEAL";
        private Long eventId = 1L;
        private Integer maxCapacity = 1000;
        private Integer entryBatchSize = 100;
        private Integer entryIntervalSeconds = 30;
        private Boolean isActive = false;

        public WaitingQueueBuilder id(Long id) { this.id = id; return this; }
        public WaitingQueueBuilder eventType(String eventType) { this.eventType = eventType; return this; }
        public WaitingQueueBuilder eventId(Long eventId) { this.eventId = eventId; return this; }
        public WaitingQueueBuilder maxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; return this; }
        public WaitingQueueBuilder entryBatchSize(Integer entryBatchSize) { this.entryBatchSize = entryBatchSize; return this; }
        public WaitingQueueBuilder entryIntervalSeconds(Integer entryIntervalSeconds) { this.entryIntervalSeconds = entryIntervalSeconds; return this; }
        public WaitingQueueBuilder active(Boolean isActive) { this.isActive = isActive; return this; }

        public WaitingQueue build() {
            WaitingQueue queue = WaitingQueue.builder()
                    .eventType(eventType)
                    .eventId(eventId)
                    .maxCapacity(maxCapacity)
                    .entryBatchSize(entryBatchSize)
                    .entryIntervalSeconds(entryIntervalSeconds)
                    .build();
            if (id != null) {
                ReflectionTestUtils.setField(queue, "id", id);
            }
            if (isActive) {
                queue.activate();
            }
            return queue;
        }
    }
}
