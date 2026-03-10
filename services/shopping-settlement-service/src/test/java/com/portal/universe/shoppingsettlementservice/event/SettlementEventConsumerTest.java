package com.portal.universe.shoppingsettlementservice.event;

import com.portal.universe.event.shopping.OrderCancelledEvent;
import com.portal.universe.event.shopping.OrderItemInfo;
import com.portal.universe.event.shopping.OrderSettlementCreatedEvent;
import com.portal.universe.shoppingsettlementservice.settlement.domain.SettlementLedger;
import com.portal.universe.shoppingsettlementservice.settlement.repository.SettlementLedgerRepository;
import com.portal.universe.shoppingsettlementservice.support.fixture.SettlementFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementEventConsumer")
class SettlementEventConsumerTest {

    @InjectMocks
    private SettlementEventConsumer consumer;

    @Mock
    private SettlementLedgerRepository ledgerRepository;

    @Nested
    @DisplayName("onOrderSettlementCreated")
    class OnOrderSettlementCreated {

        @Test
        @DisplayName("should_saveLedgerPerSeller_when_multipleSellerItems")
        void should_saveLedgerPerSeller_when_multipleSellerItems() {
            OrderSettlementCreatedEvent event = createSettlementEvent(
                    "ORD-001",
                    List.of(
                            createOrderItemInfo(100L, new BigDecimal("5000"), 2),
                            createOrderItemInfo(100L, new BigDecimal("3000"), 1),
                            createOrderItemInfo(200L, new BigDecimal("8000"), 1)
                    ),
                    Instant.parse("2026-02-27T12:00:00Z")
            );

            when(ledgerRepository.save(any(SettlementLedger.class))).thenAnswer(inv -> inv.getArgument(0));

            consumer.onOrderSettlementCreated(event);

            ArgumentCaptor<SettlementLedger> captor = ArgumentCaptor.forClass(SettlementLedger.class);
            verify(ledgerRepository, times(2)).save(captor.capture());

            List<SettlementLedger> saved = captor.getAllValues();
            SettlementLedger seller100 = saved.stream()
                    .filter(l -> l.getSellerId() == 100L).findFirst().orElseThrow();
            assertThat(seller100.getAmount()).isEqualByComparingTo("13000");
            assertThat(seller100.getEventType()).isEqualTo("PAYMENT_COMPLETED");

            SettlementLedger seller200 = saved.stream()
                    .filter(l -> l.getSellerId() == 200L).findFirst().orElseThrow();
            assertThat(seller200.getAmount()).isEqualByComparingTo("8000");
        }

        @Test
        @DisplayName("should_skipProcessing_when_itemsEmpty")
        void should_skipProcessing_when_itemsEmpty() {
            OrderSettlementCreatedEvent event = createSettlementEvent(
                    "ORD-002", List.of(), Instant.now());

            consumer.onOrderSettlementCreated(event);

            verifyNoInteractions(ledgerRepository);
        }

        @Test
        @DisplayName("should_skipProcessing_when_itemsNull")
        void should_skipProcessing_when_itemsNull() {
            OrderSettlementCreatedEvent event = createSettlementEvent(
                    "ORD-003", null, Instant.now());

            consumer.onOrderSettlementCreated(event);

            verifyNoInteractions(ledgerRepository);
        }

        @Test
        @DisplayName("should_ignoreDuplicate_when_dataIntegrityViolation")
        void should_ignoreDuplicate_when_dataIntegrityViolation() {
            OrderSettlementCreatedEvent event = createSettlementEvent(
                    "ORD-004",
                    List.of(createOrderItemInfo(100L, new BigDecimal("5000"), 1)),
                    Instant.now()
            );

            when(ledgerRepository.save(any(SettlementLedger.class)))
                    .thenThrow(new DataIntegrityViolationException("Duplicate"));

            consumer.onOrderSettlementCreated(event);

            verify(ledgerRepository).save(any(SettlementLedger.class));
        }
    }

    @Nested
    @DisplayName("onOrderCancelled")
    class OnOrderCancelled {

        @Test
        @DisplayName("should_createReversalLedger_when_paymentLedgerExists")
        void should_createReversalLedger_when_paymentLedgerExists() {
            SettlementLedger paymentLedger = SettlementFixture.ledgerBuilder()
                    .orderNumber("ORD-001")
                    .sellerId(100L)
                    .eventType("PAYMENT_COMPLETED")
                    .amount(new BigDecimal("10000.00"))
                    .build();

            when(ledgerRepository.findByOrderNumberAndEventType("ORD-001", "PAYMENT_COMPLETED"))
                    .thenReturn(List.of(paymentLedger));
            when(ledgerRepository.save(any(SettlementLedger.class))).thenAnswer(inv -> inv.getArgument(0));

            OrderCancelledEvent event = createCancelledEvent("ORD-001", Instant.now());

            consumer.onOrderCancelled(event);

            ArgumentCaptor<SettlementLedger> captor = ArgumentCaptor.forClass(SettlementLedger.class);
            verify(ledgerRepository).save(captor.capture());

            SettlementLedger reversal = captor.getValue();
            assertThat(reversal.getOrderNumber()).isEqualTo("ORD-001");
            assertThat(reversal.getSellerId()).isEqualTo(100L);
            assertThat(reversal.getEventType()).isEqualTo("ORDER_CANCELLED");
            assertThat(reversal.getAmount()).isEqualByComparingTo("-10000.00");
        }

        @Test
        @DisplayName("should_createReversalForEachSeller_when_multiplePaymentLedgers")
        void should_createReversalForEachSeller_when_multiplePaymentLedgers() {
            SettlementLedger ledger1 = SettlementFixture.ledgerBuilder()
                    .sellerId(100L).amount(new BigDecimal("5000.00")).build();
            SettlementLedger ledger2 = SettlementFixture.ledgerBuilder()
                    .sellerId(200L).amount(new BigDecimal("3000.00")).build();

            when(ledgerRepository.findByOrderNumberAndEventType("ORD-001", "PAYMENT_COMPLETED"))
                    .thenReturn(List.of(ledger1, ledger2));
            when(ledgerRepository.save(any(SettlementLedger.class))).thenAnswer(inv -> inv.getArgument(0));

            OrderCancelledEvent event = createCancelledEvent("ORD-001", Instant.now());

            consumer.onOrderCancelled(event);

            verify(ledgerRepository, times(2)).save(any(SettlementLedger.class));
        }

        @Test
        @DisplayName("should_skipReversal_when_noPaymentLedger")
        void should_skipReversal_when_noPaymentLedger() {
            when(ledgerRepository.findByOrderNumberAndEventType("ORD-UNKNOWN", "PAYMENT_COMPLETED"))
                    .thenReturn(List.of());

            OrderCancelledEvent event = createCancelledEvent("ORD-UNKNOWN", Instant.now());

            consumer.onOrderCancelled(event);

            verify(ledgerRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_ignoreDuplicate_when_reversalAlreadyExists")
        void should_ignoreDuplicate_when_reversalAlreadyExists() {
            SettlementLedger paymentLedger = SettlementFixture.ledgerBuilder()
                    .sellerId(100L).amount(new BigDecimal("5000.00")).build();

            when(ledgerRepository.findByOrderNumberAndEventType("ORD-001", "PAYMENT_COMPLETED"))
                    .thenReturn(List.of(paymentLedger));
            when(ledgerRepository.save(any(SettlementLedger.class)))
                    .thenThrow(new DataIntegrityViolationException("Duplicate"));

            OrderCancelledEvent event = createCancelledEvent("ORD-001", Instant.now());

            consumer.onOrderCancelled(event);

            verify(ledgerRepository).save(any(SettlementLedger.class));
        }
    }

    // ── Helper methods ──

    private OrderSettlementCreatedEvent createSettlementEvent(String orderNumber,
                                                               List<OrderItemInfo> items,
                                                               Instant settledAt) {
        OrderSettlementCreatedEvent event = new OrderSettlementCreatedEvent();
        event.setOrderNumber(orderNumber);
        event.setItems(items);
        event.setSettledAt(settledAt);
        return event;
    }

    private OrderItemInfo createOrderItemInfo(Long sellerId, BigDecimal price, int quantity) {
        OrderItemInfo item = new OrderItemInfo();
        item.setSellerId(sellerId);
        item.setPrice(price);
        item.setQuantity(quantity);
        return item;
    }

    private OrderCancelledEvent createCancelledEvent(String orderNumber, Instant cancelledAt) {
        OrderCancelledEvent event = new OrderCancelledEvent();
        event.setOrderNumber(orderNumber);
        event.setCancelledAt(cancelledAt);
        return event;
    }
}
