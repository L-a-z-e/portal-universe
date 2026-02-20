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

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementEventConsumer {

    private final SettlementLedgerRepository ledgerRepository;

    @KafkaListener(topics = ShoppingTopics.PAYMENT_COMPLETED, groupId = "shopping-settlement-service",
            containerFactory = "avroKafkaListenerContainerFactory")
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Recording payment to ledger: order={}, amount={}", event.getOrderNumber(), event.getAmount());
        SettlementLedger ledger = SettlementLedger.builder()
                .orderNumber(event.getOrderNumber())
                .sellerId(1L) // TODO: resolve seller from order items
                .eventType("PAYMENT_COMPLETED")
                .amount(event.getAmount())
                .eventAt(LocalDateTime.ofInstant(event.getPaidAt(), ZoneId.systemDefault()))
                .build();
        ledgerRepository.save(ledger);
    }

    @KafkaListener(topics = ShoppingTopics.ORDER_CANCELLED, groupId = "shopping-settlement-service",
            containerFactory = "avroKafkaListenerContainerFactory")
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Recording cancellation to ledger: order={}, amount={}", event.getOrderNumber(), event.getTotalAmount());
        SettlementLedger ledger = SettlementLedger.builder()
                .orderNumber(event.getOrderNumber())
                .sellerId(1L) // TODO: resolve seller from order items
                .eventType("ORDER_CANCELLED")
                .amount(event.getTotalAmount())
                .eventAt(LocalDateTime.ofInstant(event.getCancelledAt(), ZoneId.systemDefault()))
                .build();
        ledgerRepository.save(ledger);
    }
}
