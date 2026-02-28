package com.portal.universe.shoppingservice.event;

import com.portal.universe.event.shopping.*;
import com.portal.universe.shoppingservice.event.outbox.OutboxEvent;
import com.portal.universe.shoppingservice.event.outbox.OutboxEventRepository;
import com.portal.universe.shoppingservice.event.outbox.OutboxStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ShoppingEventPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private ShoppingEventPublisher eventPublisher;

    @Nested
    @DisplayName("publishOrderCreated")
    class PublishOrderCreated {

        @Test
        @DisplayName("should_saveOutboxEvent_when_called")
        void should_saveOutboxEvent_when_called() {
            // given
            OrderItemInfo item = new OrderItemInfo(1L, 1L, "Product A", 2, BigDecimal.valueOf(5000));
            OrderCreatedEvent event = new OrderCreatedEvent(
                    "ORD-001", "user1", BigDecimal.valueOf(10000), 2,
                    List.of(item), Instant.now());

            // when
            eventPublisher.publishOrderCreated(event);

            // then
            ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxEventRepository).save(captor.capture());

            OutboxEvent saved = captor.getValue();
            assertThat(saved.getTopic()).isEqualTo(ShoppingTopics.ORDER_CREATED);
            assertThat(saved.getEventKey()).isEqualTo("ORD-001");
            assertThat(saved.getAggregateType()).isEqualTo("ORDER");
            assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(saved.getEventType()).isEqualTo(OrderCreatedEvent.class.getName());
            assertThat(saved.getPayload()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("publishOrderCancelled")
    class PublishOrderCancelled {

        @Test
        @DisplayName("should_saveOutboxEvent_when_called")
        void should_saveOutboxEvent_when_called() {
            // given
            OrderCancelledEvent event = new OrderCancelledEvent(
                    "ORD-001", "user1", BigDecimal.valueOf(10000), "Customer request", Instant.now());

            // when
            eventPublisher.publishOrderCancelled(event);

            // then
            ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxEventRepository).save(captor.capture());

            assertThat(captor.getValue().getTopic()).isEqualTo(ShoppingTopics.ORDER_CANCELLED);
            assertThat(captor.getValue().getEventKey()).isEqualTo("ORD-001");
        }
    }
}
