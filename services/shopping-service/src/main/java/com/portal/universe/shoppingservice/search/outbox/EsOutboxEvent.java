package com.portal.universe.shoppingservice.search.outbox;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "es_outbox_events", indexes = {
        @Index(name = "idx_es_outbox_pending", columnList = "status, created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EsOutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "index_name", nullable = false, length = 100)
    private String indexName;

    @Column(name = "document_id", nullable = false, length = 100)
    private String documentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private EsActionType actionType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EsOutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "indexed_at")
    private Instant indexedAt;

    public static EsOutboxEvent index(String indexName, String documentId, String payload) {
        EsOutboxEvent event = new EsOutboxEvent();
        event.indexName = indexName;
        event.documentId = documentId;
        event.actionType = EsActionType.INDEX;
        event.payload = payload;
        event.status = EsOutboxStatus.PENDING;
        event.retryCount = 0;
        event.createdAt = Instant.now();
        return event;
    }

    public static EsOutboxEvent delete(String indexName, String documentId) {
        EsOutboxEvent event = new EsOutboxEvent();
        event.indexName = indexName;
        event.documentId = documentId;
        event.actionType = EsActionType.DELETE;
        event.payload = null;
        event.status = EsOutboxStatus.PENDING;
        event.retryCount = 0;
        event.createdAt = Instant.now();
        return event;
    }

    public void markIndexed() {
        this.status = EsOutboxStatus.INDEXED;
        this.indexedAt = Instant.now();
    }

    public void markFailed() {
        this.status = EsOutboxStatus.FAILED;
    }

    public void incrementRetry() {
        this.retryCount++;
    }
}
