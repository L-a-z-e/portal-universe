package com.portal.universe.shoppingservice.queue.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true, length = 36)
    private String entryToken;   // 고유 토큰 (UUID)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QueueStatus status = QueueStatus.WAITING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    private LocalDateTime enteredAt;   // 입장 시간

    private LocalDateTime expiredAt;   // 만료 시간

    private LocalDateTime leftAt;      // 이탈 시간

    @Builder
    public QueueEntry(WaitingQueue queue, Long userId) {
        this.queue = queue;
        this.userId = userId;
        this.entryToken = UUID.randomUUID().toString();
        this.status = QueueStatus.WAITING;
        this.joinedAt = LocalDateTime.now();
    }

    public void enter() {
        this.status = QueueStatus.ENTERED;
        this.enteredAt = LocalDateTime.now();
    }

    public void expire() {
        this.status = QueueStatus.EXPIRED;
        this.expiredAt = LocalDateTime.now();
    }

    public void leave() {
        this.status = QueueStatus.LEFT;
        this.leftAt = LocalDateTime.now();
    }

    public boolean isWaiting() {
        return this.status == QueueStatus.WAITING;
    }

    public boolean isEntered() {
        return this.status == QueueStatus.ENTERED;
    }
}
