package com.portal.universe.shoppingsellerservice.queue.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "queue_entries")
@Getter
@NoArgsConstructor
public class QueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "queue_id", nullable = false)
    private Long queueId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "entry_token", nullable = false, length = 36)
    private String entryToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QueueEntryStatus status;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "entered_at")
    private LocalDateTime enteredAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;
}
