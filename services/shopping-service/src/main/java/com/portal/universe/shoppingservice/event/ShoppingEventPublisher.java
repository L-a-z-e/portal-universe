package com.portal.universe.shoppingservice.event;

import com.portal.universe.event.shopping.*;
import com.portal.universe.event.shopping.ShoppingTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 주문 생성 이벤트를 발행합니다.
     */
    public void publishOrderCreated(OrderCreatedEvent event) {
        publishEvent(ShoppingTopics.ORDER_CREATED, event.orderNumber(), event);
    }

    /**
     * 주문 확정 이벤트를 발행합니다.
     */
    public void publishOrderConfirmed(OrderConfirmedEvent event) {
        publishEvent(ShoppingTopics.ORDER_CONFIRMED, event.orderNumber(), event);
    }

    /**
     * 주문 취소 이벤트를 발행합니다.
     */
    public void publishOrderCancelled(OrderCancelledEvent event) {
        publishEvent(ShoppingTopics.ORDER_CANCELLED, event.orderNumber(), event);
    }

    /**
     * 결제 완료 이벤트를 발행합니다.
     */
    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        publishEvent(ShoppingTopics.PAYMENT_COMPLETED, event.paymentNumber(), event);
    }

    /**
     * 결제 실패 이벤트를 발행합니다.
     */
    public void publishPaymentFailed(PaymentFailedEvent event) {
        publishEvent(ShoppingTopics.PAYMENT_FAILED, event.paymentNumber(), event);
    }

    /**
     * 재고 예약 이벤트를 발행합니다.
     */
    public void publishInventoryReserved(InventoryReservedEvent event) {
        publishEvent(ShoppingTopics.INVENTORY_RESERVED, event.orderNumber(), event);
    }

    /**
     * 배송 발송 이벤트를 발행합니다.
     */
    public void publishDeliveryShipped(DeliveryShippedEvent event) {
        publishEvent(ShoppingTopics.DELIVERY_SHIPPED, event.trackingNumber(), event);
    }

    /**
     * 쿠폰 발급 이벤트를 발행합니다.
     */
    public void publishCouponIssued(CouponIssuedEvent event) {
        publishEvent(ShoppingTopics.COUPON_ISSUED, event.couponCode(), event);
    }

    /**
     * 타임딜 시작 이벤트를 발행합니다.
     */
    public void publishTimeDealStarted(TimeDealStartedEvent event) {
        publishEvent(ShoppingTopics.TIMEDEAL_STARTED, String.valueOf(event.timeDealId()), event);
    }

    /**
     * 이벤트를 Kafka로 발행합니다.
     *
     * @param topic 토픽명
     * @param key 메시지 키
     * @param event 이벤트 객체
     */
    private void publishEvent(String topic, String key, Object event) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);

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
