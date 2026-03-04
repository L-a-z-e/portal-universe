package com.portal.universe.shoppingservice.queue.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * WaitingQueue
 * 대기열 설정 엔티티 - 이벤트/타임딜별 대기열 구성
 */
@Entity
@Table(name = "waiting_queues")
@Getter
@NoArgsConstructor
public class WaitingQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String eventType;  // TIMEDEAL, FLASH_SALE, etc.

    @Column(nullable = false)
    private Long eventId;      // TimeDeal ID or other event ID

    @Column(nullable = false)
    private Integer maxCapacity;      // 동시 입장 가능 인원

    @Column(nullable = false)
    private Integer entryBatchSize;   // 한 번에 입장시킬 인원

    @Column(nullable = false)
    private Integer entryIntervalSeconds;  // 입장 간격 (초)

    @Column(nullable = false)
    private Boolean isActive = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant activatedAt;

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
        this.createdAt = Instant.now();
    }

    public void updateConfig(Integer maxCapacity, Integer entryBatchSize, Integer entryIntervalSeconds) {
        this.maxCapacity = maxCapacity;
        this.entryBatchSize = entryBatchSize;
        this.entryIntervalSeconds = entryIntervalSeconds;
    }

    public void activate() {
        this.isActive = true;
        this.activatedAt = Instant.now();
    }

    public void deactivate() {
        this.isActive = false;
        this.deactivatedAt = Instant.now();
    }
}
