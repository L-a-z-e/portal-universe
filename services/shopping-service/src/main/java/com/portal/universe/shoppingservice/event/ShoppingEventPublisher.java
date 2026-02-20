package com.portal.universe.shoppingservice.event;

import com.portal.universe.event.shopping.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 쇼핑 서비스의 이벤트를 Kafka로 발행하는 퍼블리셔입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShoppingEventPublisher {

    private final KafkaTemplate<String, SpecificRecord> avroKafkaTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        publishEvent(ShoppingTopics.ORDER_CREATED, event.getOrderNumber(), event);
    }

    public void publishOrderConfirmed(OrderConfirmedEvent event) {
        publishEvent(ShoppingTopics.ORDER_CONFIRMED, event.getOrderNumber(), event);
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        publishEvent(ShoppingTopics.ORDER_CANCELLED, event.getOrderNumber(), event);
    }

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        publishEvent(ShoppingTopics.PAYMENT_COMPLETED, event.getPaymentNumber(), event);
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        publishEvent(ShoppingTopics.PAYMENT_FAILED, event.getPaymentNumber(), event);
    }

    public void publishInventoryReserved(InventoryReservedEvent event) {
        publishEvent(ShoppingTopics.INVENTORY_RESERVED, event.getOrderNumber(), event);
    }

    public void publishDeliveryShipped(DeliveryShippedEvent event) {
        publishEvent(ShoppingTopics.DELIVERY_SHIPPED, event.getTrackingNumber(), event);
    }

    public void publishCouponIssued(CouponIssuedEvent event) {
        publishEvent(ShoppingTopics.COUPON_ISSUED, event.getCouponCode(), event);
    }

    public void publishTimeDealStarted(TimeDealStartedEvent event) {
        publishEvent(ShoppingTopics.TIMEDEAL_STARTED, String.valueOf(event.getTimeDealId()), event);
    }

    private void publishEvent(String topic, String key, SpecificRecord event) {
        CompletableFuture<SendResult<String, SpecificRecord>> future = avroKafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Event published successfully: topic={}, key={}, offset={}",
                        topic, key, result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish event: topic={}, key={}, error={}",
                        topic, key, ex.getMessage());
            }
        });
    }
}
