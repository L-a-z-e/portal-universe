package com.portal.universe.shoppingservice.event;

import com.portal.universe.event.shopping.OrderItemInfo;
import com.portal.universe.event.shopping.OrderSettlementCreatedEvent;
import com.portal.universe.event.shopping.PaymentCompletedEvent;
import com.portal.universe.event.shopping.ShoppingTopics;
import com.portal.universe.shoppingservice.order.domain.Order;
import com.portal.universe.shoppingservice.order.dto.OrderResponse;
import com.portal.universe.shoppingservice.order.repository.OrderRepository;
import com.portal.universe.shoppingservice.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 결제 완료 이벤트를 수신하여 Saga 후속 단계(재고 차감, 배송 생성, 주문 확정)를 실행하고,
 * 정산용 OrderSettlementCreatedEvent를 발행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedEventConsumer {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final ShoppingEventPublisher eventPublisher;

    @KafkaListener(topics = ShoppingTopics.PAYMENT_COMPLETED, groupId = "shopping-service",
            containerFactory = "avroKafkaListenerContainerFactory")
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        String orderNumber = event.getOrderNumber().toString();

        log.info("Received PaymentCompletedEvent: payment={}, order={}",
                event.getPaymentNumber(), orderNumber);

        try {
            orderService.completeOrderAfterPayment(orderNumber);
            log.info("Order completed after payment: {}", orderNumber);

            // 정산용 이벤트 발행 (items는 Order에서 조회)
            publishSettlementEvent(orderNumber, event);
        } catch (Exception e) {
            log.error("Failed to complete order after payment: order={}, error={}",
                    orderNumber, e.getMessage());
            throw e;
        }
    }

    private void publishSettlementEvent(String orderNumber, PaymentCompletedEvent paymentEvent) {
        Order order = orderRepository.findByOrderNumberWithItems(orderNumber).orElse(null);
        if (order == null || order.getItems().isEmpty()) {
            log.warn("Cannot publish settlement event: order not found or empty items: {}", orderNumber);
            return;
        }

        eventPublisher.publishOrderSettlementCreated(OrderSettlementCreatedEvent.newBuilder()
                .setOrderNumber(orderNumber)
                .setUserId(paymentEvent.getUserId().toString())
                .setPaymentNumber(paymentEvent.getPaymentNumber().toString())
                .setTotalAmount(paymentEvent.getAmount())
                .setItems(order.getItems().stream()
                        .map(item -> OrderItemInfo.newBuilder()
                                .setSellerId(item.getSellerId())
                                .setProductId(item.getProductId())
                                .setProductName(item.getProductName())
                                .setQuantity(item.getQuantity())
                                .setPrice(item.getPrice())
                                .build())
                        .toList())
                .setSettledAt(java.time.Instant.now())
                .build());

        log.info("OrderSettlementCreatedEvent published for order: {}", orderNumber);
    }
}
