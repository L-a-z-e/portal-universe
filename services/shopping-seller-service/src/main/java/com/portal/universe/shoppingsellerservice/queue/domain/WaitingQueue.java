package com.portal.universe.shoppingsellerservice.queue.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "waiting_queues")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
public class WaitingQueue {

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

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

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
        this.activatedAt = LocalDateTime.now();
        this.deactivatedAt = null;
    }

    public void deactivate() {
        this.isActive = false;
        this.deactivatedAt = LocalDateTime.now();
    }
}
