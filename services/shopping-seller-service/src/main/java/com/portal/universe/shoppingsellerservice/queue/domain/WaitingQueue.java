package com.portal.universe.shoppingsellerservice.queue.domain;

import com.portal.universe.commonlibrary.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "waiting_queues")
@Getter
@NoArgsConstructor
public class WaitingQueue extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    @Column(name = "entry_batch_size", nullable = false)
    private Integer entryBatchSize;

    @Column(name = "entry_interval_seconds", nullable = false)
    private Integer entryIntervalSeconds;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Builder
    public WaitingQueue(String eventType, Long eventId, Integer maxCapacity,
                        Integer entryBatchSize, Integer entryIntervalSeconds) {
        this.eventType = eventType;
        this.eventId = eventId;
        this.maxCapacity = maxCapacity;
        this.entryBatchSize = entryBatchSize;
        this.entryIntervalSeconds = entryIntervalSeconds;
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
        this.activatedAt = Instant.now();
        this.deactivatedAt = null;
    }

    public void deactivate() {
        this.isActive = false;
        this.deactivatedAt = Instant.now();
    }
}
