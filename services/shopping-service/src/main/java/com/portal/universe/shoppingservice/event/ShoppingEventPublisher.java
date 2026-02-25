package com.portal.universe.shoppingservice.event;

import com.portal.universe.event.shopping.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 쇼핑 서비스의 이벤트를 Kafka + EventBridge로 발행하는 퍼블리셔입니다.
 * Kafka: 핵심 서비스간 통신 (notification, seller 등)
 * EventBridge: 조건부 라우팅 (고액 주문 알림, 이메일 확인 등)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShoppingEventPublisher {

    private final KafkaTemplate<String, SpecificRecord> avroKafkaTemplate;
    private final EventBridgePublisher eventBridgePublisher;

    public void publishOrderCreated(OrderCreatedEvent event) {
        publishEvent(ShoppingTopics.ORDER_CREATED, event.getOrderNumber(), event);

        // EventBridge로 조건부 라우팅용 이벤트 발행 (비동기, 실패 허용)
        eventBridgePublisher.publishOrderCreated(
                event.getOrderNumber().toString(),
                event.getUserId().toString(),
                event.getTotalAmount(),
                event.getItemCount(),
                event.getItems().stream()
                        .map(item -> Map.<String, Object>of(
                                "productId", item.getProductId(),
                                "productName", item.getProductName().toString(),
                                "quantity", item.getQuantity(),
                                "price", item.getPrice().intValue()
                        ))
                        .toList()
        );
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
