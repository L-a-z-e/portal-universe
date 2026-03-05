package com.portal.universe.shoppingservice.event.outbox;

import com.portal.universe.event.shopping.OrderCreatedEvent;
import com.portal.universe.event.shopping.OrderItemInfo;
import com.portal.universe.event.shopping.PaymentCompletedEvent;
import com.portal.universe.event.shopping.ShoppingTopics;
import com.portal.universe.shoppingservice.event.EventBridgePublisher;
import org.apache.avro.specific.SpecificRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxPollingScheduler 테스트")
class OutboxPollingSchedulerTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, SpecificRecord> avroKafkaTemplate;

    @Mock
    private EventBridgePublisher eventBridgePublisher;

    @InjectMocks
    private OutboxPollingScheduler scheduler;

    @Nested
    @DisplayName("pollAndPublish")
    class PollAndPublish {

        @Test
        @DisplayName("should_doNothing_when_noPendingEvents")
        void should_doNothing_when_noPendingEvents() {
            // given
            when(outboxEventRepository.findPendingForUpdate(anyInt()))
                    .thenReturn(Collections.emptyList());

            // when
            scheduler.pollAndPublish();

            // then
            verifyNoInteractions(avroKafkaTemplate);
        }

        @Test
        @DisplayName("should_publishAndMarkPublished_when_kafkaSucceeds")
        void should_publishAndMarkPublished_when_kafkaSucceeds() {
            // given
            PaymentCompletedEvent avroEvent = new PaymentCompletedEvent(
                    "PAY-001", "ORD-001", "user1", BigDecimal.valueOf(10000),
                    "CARD", "PG-TX123", Instant.now(), List.of());
            OutboxEvent outbox = OutboxEvent.create(
                    ShoppingTopics.PAYMENT_COMPLETED, "PAY-001", avroEvent);

            when(outboxEventRepository.findPendingForUpdate(anyInt()))
                    .thenReturn(List.of(outbox));

            @SuppressWarnings("unchecked")
            SendResult<String, SpecificRecord> sendResult = mock(SendResult.class);
            when(avroKafkaTemplate.send(anyString(), anyString(), any()))
                    .thenReturn(CompletableFuture.completedFuture(sendResult));

            // when
            scheduler.pollAndPublish();

            // then
            verify(avroKafkaTemplate).send(
                    eq(ShoppingTopics.PAYMENT_COMPLETED), eq("PAY-001"), any(SpecificRecord.class));
            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        }

        @Test
        @DisplayName("should_incrementRetry_when_kafkaFails")
        void should_incrementRetry_when_kafkaFails() {
            // given
            PaymentCompletedEvent avroEvent = new PaymentCompletedEvent(
                    "PAY-001", "ORD-001", "user1", BigDecimal.valueOf(10000),
                    "CARD", "PG-TX123", Instant.now(), List.of());
            OutboxEvent outbox = OutboxEvent.create(
                    ShoppingTopics.PAYMENT_COMPLETED, "PAY-001", avroEvent);

            when(outboxEventRepository.findPendingForUpdate(anyInt()))
                    .thenReturn(List.of(outbox));
            when(avroKafkaTemplate.send(anyString(), anyString(), any()))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Broker down")));

            // when
            scheduler.pollAndPublish();

            // then
            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(outbox.getRetryCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should_markFailed_when_maxRetriesExceeded")
        void should_markFailed_when_maxRetriesExceeded() {
            // given
            PaymentCompletedEvent avroEvent = new PaymentCompletedEvent(
                    "PAY-001", "ORD-001", "user1", BigDecimal.valueOf(10000),
                    "CARD", "PG-TX123", Instant.now(), List.of());
            OutboxEvent outbox = OutboxEvent.create(
                    ShoppingTopics.PAYMENT_COMPLETED, "PAY-001", avroEvent);
            for (int i = 0; i < 4; i++) outbox.incrementRetry();

            when(outboxEventRepository.findPendingForUpdate(anyInt()))
                    .thenReturn(List.of(outbox));
            when(avroKafkaTemplate.send(anyString(), anyString(), any()))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Broker down")));

            // when
            scheduler.pollAndPublish();

            // then
            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
            assertThat(outbox.getRetryCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("should_callEventBridge_when_orderCreated")
        void should_callEventBridge_when_orderCreated() {
            // given
            OrderItemInfo item = new OrderItemInfo(1L, 1L, "Product A", 2, BigDecimal.valueOf(5000));
            OrderCreatedEvent avroEvent = new OrderCreatedEvent(
                    "ORD-001", "user1", BigDecimal.valueOf(10000), 2,
                    List.of(item), Instant.now());
            OutboxEvent outbox = OutboxEvent.create(
                    ShoppingTopics.ORDER_CREATED, "ORD-001", avroEvent);

            when(outboxEventRepository.findPendingForUpdate(anyInt()))
                    .thenReturn(List.of(outbox));
            @SuppressWarnings("unchecked")
            SendResult<String, SpecificRecord> sendResult = mock(SendResult.class);
            when(avroKafkaTemplate.send(anyString(), anyString(), any()))
                    .thenReturn(CompletableFuture.completedFuture(sendResult));

            // when
            scheduler.pollAndPublish();

            // then
            verify(eventBridgePublisher).publishOrderCreated(
                    eq("ORD-001"), eq("user1"), any(), eq(2), anyList());
        }
    }
}
