package com.portal.universe.shoppingsellerservice.event.outbox;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;

@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_pending", columnList = "status, created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 200)
    private String eventType;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(name = "event_key", nullable = false, length = 100)
    private String eventKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    public static OutboxEvent create(String topic, String key, SpecificRecord event) {
        OutboxEvent outbox = new OutboxEvent();
        outbox.aggregateType = extractAggregateType(topic);
        outbox.aggregateId = key;
        outbox.eventType = event.getClass().getName();
        outbox.topic = topic;
        outbox.eventKey = key;
        outbox.payload = serializeToAvroJson(event);
        outbox.status = OutboxStatus.PENDING;
        outbox.retryCount = 0;
        outbox.createdAt = Instant.now();
        return outbox;
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }

    public void markFailed() {
        this.status = OutboxStatus.FAILED;
    }

    public void incrementRetry() {
        this.retryCount++;
    }

    @SuppressWarnings("unchecked")
    private static String serializeToAvroJson(SpecificRecord event) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            JsonEncoder encoder = EncoderFactory.get().jsonEncoder(event.getSchema(), out);
            SpecificDatumWriter<SpecificRecord> writer = new SpecificDatumWriter<>(event.getSchema());
            writer.write(event, encoder);
            encoder.flush();
            return out.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize Avro event to JSON", e);
        }
    }

    private static String extractAggregateType(String topic) {
        String[] parts = topic.split("\\.");
        return parts.length >= 2 ? parts[1].toUpperCase() : topic.toUpperCase();
    }
}
