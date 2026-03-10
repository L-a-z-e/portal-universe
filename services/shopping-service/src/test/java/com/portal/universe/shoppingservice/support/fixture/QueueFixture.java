package com.portal.universe.shoppingservice.support.fixture;

import com.portal.universe.shoppingservice.queue.domain.QueueEntry;
import com.portal.universe.shoppingservice.queue.domain.QueueStatus;
import com.portal.universe.shoppingservice.queue.domain.WaitingQueue;
import org.springframework.test.util.ReflectionTestUtils;

public final class QueueFixture {

    private QueueFixture() {}

    public static WaitingQueue createQueue() {
        return queueBuilder().build();
    }

    public static QueueBuilder queueBuilder() {
        return new QueueBuilder();
    }

    public static class QueueBuilder {
        private Long id = 1L;
        private String eventType = "TIMEDEAL";
        private Long eventId = 1L;
        private Integer maxCapacity = 100;
        private Integer entryBatchSize = 10;
        private Integer entryIntervalSeconds = 5;
        private boolean active = true;

        public QueueBuilder id(Long id) { this.id = id; return this; }
        public QueueBuilder eventType(String eventType) { this.eventType = eventType; return this; }
        public QueueBuilder eventId(Long eventId) { this.eventId = eventId; return this; }
        public QueueBuilder maxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; return this; }
        public QueueBuilder entryBatchSize(Integer entryBatchSize) { this.entryBatchSize = entryBatchSize; return this; }
        public QueueBuilder entryIntervalSeconds(Integer entryIntervalSeconds) { this.entryIntervalSeconds = entryIntervalSeconds; return this; }
        public QueueBuilder active(boolean active) { this.active = active; return this; }

        public WaitingQueue build() {
            WaitingQueue queue = WaitingQueue.builder()
                    .eventType(eventType)
                    .eventId(eventId)
                    .maxCapacity(maxCapacity)
                    .entryBatchSize(entryBatchSize)
                    .entryIntervalSeconds(entryIntervalSeconds)
                    .build();
            ReflectionTestUtils.setField(queue, "id", id);
            ReflectionTestUtils.setField(queue, "isActive", active);
            return queue;
        }
    }

    public static QueueEntry createEntry(WaitingQueue queue, String userId) {
        return entryBuilder().queue(queue).userId(userId).build();
    }

    public static EntryBuilder entryBuilder() {
        return new EntryBuilder();
    }

    public static class EntryBuilder {
        private Long id = 1L;
        private WaitingQueue queue;
        private String userId = "test-user-001";
        private QueueStatus status = QueueStatus.WAITING;

        public EntryBuilder id(Long id) { this.id = id; return this; }
        public EntryBuilder queue(WaitingQueue queue) { this.queue = queue; return this; }
        public EntryBuilder userId(String userId) { this.userId = userId; return this; }
        public EntryBuilder status(QueueStatus status) { this.status = status; return this; }

        public QueueEntry build() {
            QueueEntry entry = QueueEntry.builder()
                    .queue(queue)
                    .userId(userId)
                    .build();
            ReflectionTestUtils.setField(entry, "id", id);
            ReflectionTestUtils.setField(entry, "status", status);
            return entry;
        }
    }
}
