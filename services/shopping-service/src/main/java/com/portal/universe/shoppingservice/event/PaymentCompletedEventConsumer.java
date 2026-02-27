package com.portal.universe.shoppingservice.event;

import com.portal.universe.event.shopping.PaymentCompletedEvent;
import com.portal.universe.event.shopping.ShoppingTopics;
import com.portal.universe.shoppingservice.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 결제 완료 이벤트를 수신하여 Saga 후속 단계(재고 차감, 배송 생성, 주문 확정)를 실행합니다.
 * PaymentService → (Kafka) → OrderService 단방향 이벤트 흐름으로 순환 참조를 방지합니다.
 * Payment 서비스 분리 후에도 코드 변경 없이 동작합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedEventConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = ShoppingTopics.PAYMENT_COMPLETED, groupId = "shopping-service",
            containerFactory = "avroKafkaListenerContainerFactory")
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        String orderNumber = event.getOrderNumber().toString();

        log.info("Received PaymentCompletedEvent: payment={}, order={}",
                event.getPaymentNumber(), orderNumber);

        try {
            orderService.completeOrderAfterPayment(orderNumber);
            log.info("Order completed after payment: {}", orderNumber);
        } catch (Exception e) {
            log.error("Failed to complete order after payment: order={}, error={}",
                    orderNumber, e.getMessage());
            throw e;
        }
    }
}
