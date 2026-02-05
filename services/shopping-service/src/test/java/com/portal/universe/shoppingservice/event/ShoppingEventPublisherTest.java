package com.portal.universe.shoppingservice.event;

import com.portal.universe.event.shopping.*;
import com.portal.universe.shoppingservice.common.config.KafkaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShoppingEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private ShoppingEventPublisher eventPublisher;

    @SuppressWarnings("unchecked")
    private void setupKafkaTemplate() {
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);
    }

    @Nested
    @DisplayName("publishOrderCreated")
    class PublishOrderCreated {

        @Test
        @DisplayName("should_publishOrderCreatedEvent_when_called")
        void should_publishOrderCreatedEvent_when_called() {
            // given
            setupKafkaTemplate();
            OrderCreatedEvent event = new OrderCreatedEvent(
                    "ORD-001", "user1", BigDecimal.valueOf(10000), 2,
                    List.of(new OrderCreatedEvent.OrderItemInfo(1L, "Product A", 2, BigDecimal.valueOf(5000))),
                    LocalDateTime.now()
            );

            // when
            eventPublisher.publishOrderCreated(event);

            // then
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

            assertThat(topicCaptor.getValue()).isEqualTo(KafkaConfig.TOPIC_ORDER_CREATED);
            assertThat(keyCaptor.getValue()).isEqualTo("ORD-001");
            assertThat(eventCaptor.getValue()).isInstanceOf(OrderCreatedEvent.class);
        }
    }

    @Nested
    @DisplayName("publishOrderConfirmed")
    class PublishOrderConfirmed {

        @Test
        @DisplayName("should_publishOrderConfirmedEvent_when_called")
        void should_publishOrderConfirmedEvent_when_called() {
            // given
            setupKafkaTemplate();
            OrderConfirmedEvent event = new OrderConfirmedEvent(
                    "ORD-001", "user1", BigDecimal.valueOf(10000), "PAY-001", LocalDateTime.now()
            );

            // when
            eventPublisher.publishOrderConfirmed(event);

            // then
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), any());

            assertThat(topicCaptor.getValue()).isEqualTo(KafkaConfig.TOPIC_ORDER_CONFIRMED);
            assertThat(keyCaptor.getValue()).isEqualTo("ORD-001");
        }
    }

    @Nested
    @DisplayName("publishOrderCancelled")
    class PublishOrderCancelled {

        @Test
        @DisplayName("should_publishOrderCancelledEvent_when_called")
        void should_publishOrderCancelledEvent_when_called() {
            // given
            setupKafkaTemplate();
            OrderCancelledEvent event = new OrderCancelledEvent(
                    "ORD-001", "user1", BigDecimal.valueOf(10000), "Customer request", LocalDateTime.now()
            );

            // when
            eventPublisher.publishOrderCancelled(event);

            // then
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), any());

            assertThat(topicCaptor.getValue()).isEqualTo(KafkaConfig.TOPIC_ORDER_CANCELLED);
            assertThat(keyCaptor.getValue()).isEqualTo("ORD-001");
        }
    }

    @Nested
    @DisplayName("publishPaymentCompleted")
    class PublishPaymentCompleted {

        @Test
        @DisplayName("should_publishPaymentCompletedEvent_when_called")
        void should_publishPaymentCompletedEvent_when_called() {
            // given
            setupKafkaTemplate();
            PaymentCompletedEvent event = new PaymentCompletedEvent(
                    "PAY-001", "ORD-001", "user1", BigDecimal.valueOf(10000),
                    "CARD", "PG-TX123", LocalDateTime.now()
            );

            // when
            eventPublisher.publishPaymentCompleted(event);

            // then
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), any());

            assertThat(topicCaptor.getValue()).isEqualTo(KafkaConfig.TOPIC_PAYMENT_COMPLETED);
            assertThat(keyCaptor.getValue()).isEqualTo("PAY-001");
        }
    }

    @Nested
    @DisplayName("publishPaymentFailed")
    class PublishPaymentFailed {

        @Test
        @DisplayName("should_publishPaymentFailedEvent_when_called")
        void should_publishPaymentFailedEvent_when_called() {
            // given
            setupKafkaTemplate();
            PaymentFailedEvent event = new PaymentFailedEvent(
                    "PAY-001", "ORD-001", "user1", BigDecimal.valueOf(10000),
                    "CARD", "Card declined", LocalDateTime.now()
            );

            // when
            eventPublisher.publishPaymentFailed(event);

            // then
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), any());

            assertThat(topicCaptor.getValue()).isEqualTo(KafkaConfig.TOPIC_PAYMENT_FAILED);
            assertThat(keyCaptor.getValue()).isEqualTo("PAY-001");
        }
    }

    @Nested
    @DisplayName("publishInventoryReserved")
    class PublishInventoryReserved {

        @Test
        @DisplayName("should_publishInventoryReservedEvent_when_called")
        void should_publishInventoryReservedEvent_when_called() {
            // given
            setupKafkaTemplate();
            InventoryReservedEvent event = new InventoryReservedEvent(
                    "ORD-001", "user1", Map.of(1L, 5, 2L, 3), LocalDateTime.now()
            );

            // when
            eventPublisher.publishInventoryReserved(event);

            // then
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), any());

            assertThat(topicCaptor.getValue()).isEqualTo(KafkaConfig.TOPIC_INVENTORY_RESERVED);
            assertThat(keyCaptor.getValue()).isEqualTo("ORD-001");
        }
    }

    @Nested
    @DisplayName("publishDeliveryShipped")
    class PublishDeliveryShipped {

        @Test
        @DisplayName("should_publishDeliveryShippedEvent_when_called")
        void should_publishDeliveryShippedEvent_when_called() {
            // given
            setupKafkaTemplate();
            DeliveryShippedEvent event = new DeliveryShippedEvent(
                    "TRK-ABC123", "ORD-001", "user1", "Test Carrier",
                    LocalDate.now().plusDays(3), LocalDateTime.now()
            );

            // when
            eventPublisher.publishDeliveryShipped(event);

            // then
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), any());

            assertThat(topicCaptor.getValue()).isEqualTo(KafkaConfig.TOPIC_DELIVERY_SHIPPED);
            assertThat(keyCaptor.getValue()).isEqualTo("TRK-ABC123");
        }
    }

    @Nested
    @DisplayName("publishCouponIssued")
    class PublishCouponIssued {

        @Test
        @DisplayName("should_publishCouponIssuedEvent_when_called")
        void should_publishCouponIssuedEvent_when_called() {
            // given
            setupKafkaTemplate();
            CouponIssuedEvent event = new CouponIssuedEvent(
                    1L, "SAVE10", "10% OFF", "PERCENTAGE", 10,
                    LocalDateTime.now().plusDays(30)
            );

            // when
            eventPublisher.publishCouponIssued(event);

            // then
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), any());

            assertThat(topicCaptor.getValue()).isEqualTo(KafkaConfig.TOPIC_COUPON_ISSUED);
            assertThat(keyCaptor.getValue()).isEqualTo("SAVE10");
        }
    }

    @Nested
    @DisplayName("publishTimeDealStarted")
    class PublishTimeDealStarted {

        @Test
        @DisplayName("should_publishTimeDealStartedEvent_when_called")
        void should_publishTimeDealStartedEvent_when_called() {
            // given
            setupKafkaTemplate();
            TimeDealStartedEvent event = new TimeDealStartedEvent(
                    1L, "Flash Sale", LocalDateTime.now(), LocalDateTime.now().plusHours(5)
            );

            // when
            eventPublisher.publishTimeDealStarted(event);

            // then
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), any());

            assertThat(topicCaptor.getValue()).isEqualTo(KafkaConfig.TOPIC_TIMEDEAL_STARTED);
            assertThat(keyCaptor.getValue()).isEqualTo("1");
        }
    }
}
