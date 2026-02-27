package com.portal.universe.shoppingservice.event;

import com.portal.universe.event.shopping.PaymentCancelledEvent;
import com.portal.universe.event.shopping.ShoppingTopics;
import com.portal.universe.shoppingservice.order.dto.CancelOrderRequest;
import com.portal.universe.shoppingservice.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 결제 취소 이벤트를 수신하여 주문 취소를 연쇄 수행합니다.
 * Payment → (Kafka) → Order 단방향 이벤트 흐름으로,
 * Payment 서비스 분리 후에도 코드 변경 없이 동작합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCancelledEventConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = ShoppingTopics.PAYMENT_CANCELLED, groupId = "shopping-service",
            containerFactory = "avroKafkaListenerContainerFactory")
    public void onPaymentCancelled(PaymentCancelledEvent event) {
        String orderNumber = event.getOrderNumber().toString();
        String userId = event.getUserId().toString();

        log.info("Received PaymentCancelledEvent: payment={}, order={}",
                event.getPaymentNumber(), orderNumber);

        try {
            orderService.cancelOrder(
                    userId,
                    orderNumber,
                    new CancelOrderRequest("Payment cancelled: " + event.getCancelReason())
            );
            log.info("Order cancelled after payment cancellation: {}", orderNumber);
        } catch (Exception e) {
            log.error("Failed to cancel order after payment cancellation: order={}, error={}",
                    orderNumber, e.getMessage());
            throw e;
        }
    }
}
