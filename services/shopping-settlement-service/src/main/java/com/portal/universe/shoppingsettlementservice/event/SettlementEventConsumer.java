package com.portal.universe.shoppingsettlementservice.event;

import com.portal.universe.event.shopping.OrderCancelledEvent;
import com.portal.universe.event.shopping.PaymentCompletedEvent;
import com.portal.universe.event.shopping.ShoppingTopics;
import com.portal.universe.shoppingsettlementservice.settlement.domain.SettlementLedger;
import com.portal.universe.shoppingsettlementservice.settlement.repository.SettlementLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementEventConsumer {

    private final SettlementLedgerRepository ledgerRepository;

    @KafkaListener(topics = ShoppingTopics.PAYMENT_COMPLETED, groupId = "shopping-settlement-service")
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Recording payment to ledger: order={}, amount={}", event.orderNumber(), event.amount());
        SettlementLedger ledger = SettlementLedger.builder()
                .orderNumber(event.orderNumber())
                .sellerId(1L) // TODO: resolve seller from order items
                .eventType("PAYMENT_COMPLETED")
                .amount(event.amount())
                .eventAt(event.paidAt())
                .build();
        ledgerRepository.save(ledger);
    }

    @KafkaListener(topics = ShoppingTopics.ORDER_CANCELLED, groupId = "shopping-settlement-service")
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Recording cancellation to ledger: order={}, amount={}", event.orderNumber(), event.totalAmount());
        SettlementLedger ledger = SettlementLedger.builder()
                .orderNumber(event.orderNumber())
                .sellerId(1L) // TODO: resolve seller from order items
                .eventType("ORDER_CANCELLED")
                .amount(event.totalAmount())
                .eventAt(event.cancelledAt())
                .build();
        ledgerRepository.save(ledger);
    }
}
