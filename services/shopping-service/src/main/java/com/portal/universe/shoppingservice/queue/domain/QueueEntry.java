package com.portal.universe.shoppingservice.queue.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * QueueEntry
 * 대기열 엔트리 - 개별 사용자의 대기열 위치 추적
 */
@Entity
@Table(name = "queue_entries",
       indexes = {
           @Index(name = "idx_queue_entry_queue_user", columnList = "queue_id, user_id"),
           @Index(name = "idx_queue_entry_token", columnList = "entry_token")
       })
@Getter
@NoArgsConstructor
public class QueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "queue_id", nullable = false)
    private WaitingQueue queue;

    @Column(nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, unique = true, length = 36)
    private String entryToken;   // 고유 토큰 (UUID)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QueueStatus status = QueueStatus.WAITING;

    @Column(nullable = false, updatable = false)
    private Instant joinedAt;

    private Instant enteredAt;   // 입장 시간

    private Instant expiredAt;   // 만료 시간

    private Instant leftAt;      // 이탈 시간

    @Builder
    public QueueEntry(WaitingQueue queue, String userId) {
        this.queue = queue;
        this.userId = userId;
        this.entryToken = UUID.randomUUID().toString();
        this.status = QueueStatus.WAITING;
        this.joinedAt = Instant.now();
    }

    public void enter() {
        this.status = QueueStatus.ENTERED;
        this.enteredAt = Instant.now();
    }

    public void expire() {
        this.status = QueueStatus.EXPIRED;
        this.expiredAt = Instant.now();
    }

    public void leave() {
        this.status = QueueStatus.LEFT;
        this.leftAt = Instant.now();
    }

    public boolean isWaiting() {
        return this.status == QueueStatus.WAITING;
    }

    public boolean isEntered() {
        return this.status == QueueStatus.ENTERED;
    }
}
