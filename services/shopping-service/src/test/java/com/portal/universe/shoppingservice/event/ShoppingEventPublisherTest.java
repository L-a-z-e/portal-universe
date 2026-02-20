package com.portal.universe.shoppingservice.event;

import com.portal.universe.event.shopping.*;
import org.apache.avro.specific.SpecificRecord;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShoppingEventPublisherTest {

    @Mock
    private KafkaTemplate<String, SpecificRecord> avroKafkaTemplate;

    @InjectMocks
    private ShoppingEventPublisher eventPublisher;

    @SuppressWarnings("unchecked")
    private void setupKafkaTemplate() {
        CompletableFuture<SendResult<String, SpecificRecord>> future = new CompletableFuture<>();
        when(avroKafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);
    }

    @Nested
    @DisplayName("publishOrderCreated")
    class PublishOrderCreated {

        @Test
        @DisplayName("should_publishOrderCreatedEvent_when_called")
        void should_publishOrderCreatedEvent_when_called() {
            // given
            setupKafkaTemplate();
            OrderItemInfo item = new OrderItemInfo(1L, "Product A", 2, BigDecimal.valueOf(5000));
            OrderCreatedEvent event = new OrderCreatedEvent(
                    "ORD-001", "user1", BigDecimal.valueOf(10000), 2,
                    List.of(item),
                    Instant.now()
            );

            // when
            eventPublisher.publishOrderCreated(event);

            // then
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<SpecificRecord> eventCaptor = ArgumentCaptor.forClass(SpecificRecord.class);
            verify(avroKafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

            assertThat(topicCaptor.getValue()).isEqualTo(ShoppingTopics.ORDER_CREATED);
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
                    "ORD-001", "user1", BigDecimal.valueOf(10000), "PAY-001", Instant.now()
            );

            // when
            eventPublisher.publishOrderConfirmed(event);

            // then
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(avroKafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), any());

            assertThat(topicCaptor.getValue()).isEqualTo(ShoppingTopics.ORDER_CONFIRMED);
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
                    "ORD-001", "user1", BigDecimal.valueOf(10000), "Customer request", Instant.now()
            );

            // when
            eventPublisher.publishOrderCancelled(event);

            // then
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(avroKafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), any());

            assertThat(topicCaptor.getValue()).isEqualTo(ShoppingTopics.ORDER_CANCELLED);
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
                    "CARD", "PG-TX123", Instant.now()
            );

            // when
            eventPublisher.publishPaymentCompleted(event);

            // then
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(avroKafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), any());

            assertThat(topicCaptor.getValue()).isEqualTo(ShoppingTopics.PAYMENT_COMPLETED);
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
                    "CARD", "Card declined", Instant.now()
            );

            // when
            eventPublisher.publishPaymentFailed(event);

            // then
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(avroKafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), any());

            assertThat(topicCaptor.getValue()).isEqualTo(ShoppingTopics.PAYMENT_FAILED);
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
            List<ReservedQuantity> reservedQuantities = List.of(
                    new ReservedQuantity(1L, 5),
                    new ReservedQuantity(2L, 3)
            );
            InventoryReservedEvent event = new InventoryReservedEvent(
                    "ORD-001", "user1", reservedQuantities, Instant.now()
            );

            // when
            eventPublisher.publishInventoryReserved(event);

            // then
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(avroKafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), any());

            assertThat(topicCaptor.getValue()).isEqualTo(ShoppingTopics.INVENTORY_RESERVED);
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
                    LocalDate.now().plusDays(3), Instant.now()
            );

            // when
            eventPublisher.publishDeliveryShipped(event);

            // then
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(avroKafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), any());

            assertThat(topicCaptor.getValue()).isEqualTo(ShoppingTopics.DELIVERY_SHIPPED);
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
                    "1", "SAVE10", "10% OFF", "PERCENTAGE", 10,
                    Instant.now().plusSeconds(86400 * 30)
            );

            // when
            eventPublisher.publishCouponIssued(event);

            // then
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(avroKafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), any());

            assertThat(topicCaptor.getValue()).isEqualTo(ShoppingTopics.COUPON_ISSUED);
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
                    1L, "Flash Sale", Instant.now(), Instant.now().plusSeconds(3600 * 5)
            );

            // when
            eventPublisher.publishTimeDealStarted(event);

            // then
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(avroKafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), any());

            assertThat(topicCaptor.getValue()).isEqualTo(ShoppingTopics.TIMEDEAL_STARTED);
            assertThat(keyCaptor.getValue()).isEqualTo("1");
        }
    }
}
