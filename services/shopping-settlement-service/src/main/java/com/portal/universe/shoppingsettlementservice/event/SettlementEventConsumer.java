package com.portal.universe.shoppingsettlementservice.event;

import com.portal.universe.event.shopping.OrderCancelledEvent;
import com.portal.universe.event.shopping.PaymentCompletedEvent;
import com.portal.universe.event.shopping.ShoppingTopics;
import com.portal.universe.shoppingsettlementservice.settlement.domain.SettlementLedger;
import com.portal.universe.shoppingsettlementservice.settlement.repository.SettlementLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
        saveLedgerIdempotent(
                event.getOrderNumber(),
                "PAYMENT_COMPLETED",
                event.getAmount(),
                LocalDateTime.ofInstant(event.getPaidAt(), ZoneId.systemDefault())
        );
    }

    @KafkaListener(topics = ShoppingTopics.ORDER_CANCELLED, groupId = "shopping-settlement-service",
            containerFactory = "avroKafkaListenerContainerFactory")
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Recording cancellation to ledger: order={}, amount={}", event.getOrderNumber(), event.getTotalAmount());
        saveLedgerIdempotent(
                event.getOrderNumber(),
                "ORDER_CANCELLED",
                event.getTotalAmount(),
                LocalDateTime.ofInstant(event.getCancelledAt(), ZoneId.systemDefault())
        );
    }

    private void saveLedgerIdempotent(String orderNumber, String eventType,
                                       BigDecimal amount, LocalDateTime eventAt) {
        try {
            SettlementLedger ledger = SettlementLedger.builder()
                    .orderNumber(orderNumber)
                    .sellerId(1L) // TODO: resolve seller from order items
                    .eventType(eventType)
                    .amount(amount)
                    .eventAt(eventAt)
                    .build();
            ledgerRepository.save(ledger);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate event ignored: order={}, type={}", orderNumber, eventType);
        }
    }
}
