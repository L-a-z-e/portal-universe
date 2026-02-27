package com.portal.universe.shoppingsettlementservice.event;

import com.portal.universe.event.shopping.OrderCancelledEvent;
import com.portal.universe.event.shopping.OrderItemInfo;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementEventConsumer {

    private final SettlementLedgerRepository ledgerRepository;

    @KafkaListener(topics = ShoppingTopics.PAYMENT_COMPLETED, groupId = "shopping-settlement-service",
            containerFactory = "avroKafkaListenerContainerFactory")
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        LocalDateTime paidAt = LocalDateTime.ofInstant(event.getPaidAt(), ZoneId.systemDefault());
        List<OrderItemInfo> items = event.getItems();

        if (items == null || items.isEmpty()) {
            log.warn("PaymentCompletedEvent has no items, skipping seller breakdown: order={}", event.getOrderNumber());
            return;
        }

        // 판매자별 금액 합산
        Map<Long, BigDecimal> sellerAmounts = items.stream()
                .collect(Collectors.groupingBy(
                        OrderItemInfo::getSellerId,
                        Collectors.reducing(BigDecimal.ZERO,
                                item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())),
                                BigDecimal::add)
                ));

        log.info("Recording payment to ledger: order={}, sellers={}", event.getOrderNumber(), sellerAmounts.size());

        sellerAmounts.forEach((sellerId, amount) ->
                saveLedgerIdempotent(event.getOrderNumber(), sellerId, "PAYMENT_COMPLETED", amount, paidAt)
        );
    }

    @KafkaListener(topics = ShoppingTopics.ORDER_CANCELLED, groupId = "shopping-settlement-service",
            containerFactory = "avroKafkaListenerContainerFactory")
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Recording cancellation to ledger: order={}", event.getOrderNumber());
        LocalDateTime cancelledAt = LocalDateTime.ofInstant(event.getCancelledAt(), ZoneId.systemDefault());

        // 기존 PAYMENT_COMPLETED 레코드에서 판매자별 금액을 조회하여 역분개
        List<SettlementLedger> paymentLedgers = ledgerRepository
                .findByOrderNumberAndEventType(event.getOrderNumber(), "PAYMENT_COMPLETED");

        if (paymentLedgers.isEmpty()) {
            log.warn("No payment ledger found for cancelled order: {}", event.getOrderNumber());
            return;
        }

        for (SettlementLedger paymentLedger : paymentLedgers) {
            saveLedgerIdempotent(
                    event.getOrderNumber(),
                    paymentLedger.getSellerId(),
                    "ORDER_CANCELLED",
                    paymentLedger.getAmount().negate(),
                    cancelledAt
            );
        }
    }

    private void saveLedgerIdempotent(String orderNumber, Long sellerId, String eventType,
                                       BigDecimal amount, LocalDateTime eventAt) {
        try {
            SettlementLedger ledger = SettlementLedger.builder()
                    .orderNumber(orderNumber)
                    .sellerId(sellerId)
                    .eventType(eventType)
                    .amount(amount)
                    .eventAt(eventAt)
                    .build();
            ledgerRepository.save(ledger);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate event ignored: order={}, seller={}, type={}", orderNumber, sellerId, eventType);
        }
    }
}
