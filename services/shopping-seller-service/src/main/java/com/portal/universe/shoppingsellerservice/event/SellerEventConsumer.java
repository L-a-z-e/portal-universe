package com.portal.universe.shoppingsellerservice.event;

import com.portal.universe.event.shopping.OrderCancelledEvent;
import com.portal.universe.event.shopping.OrderCreatedEvent;
import com.portal.universe.event.shopping.PaymentCompletedEvent;
import com.portal.universe.event.shopping.ShoppingTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SellerEventConsumer {

    @KafkaListener(topics = ShoppingTopics.ORDER_CREATED, groupId = "shopping-seller-service")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: {}", event.orderNumber());
    }

    @KafkaListener(topics = ShoppingTopics.ORDER_CANCELLED, groupId = "shopping-seller-service")
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Received OrderCancelledEvent: {}", event.orderNumber());
    }

    @KafkaListener(topics = ShoppingTopics.PAYMENT_COMPLETED, groupId = "shopping-seller-service")
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received PaymentCompletedEvent: {}", event.paymentNumber());
    }
}
