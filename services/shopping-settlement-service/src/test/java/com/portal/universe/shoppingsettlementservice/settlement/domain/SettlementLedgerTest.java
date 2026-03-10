package com.portal.universe.shoppingsettlementservice.settlement.domain;

import com.portal.universe.shoppingsettlementservice.support.fixture.SettlementFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SettlementLedger")
class SettlementLedgerTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should_haveProcessedFalse_when_created")
        void should_haveProcessedFalse_when_created() {
            SettlementLedger ledger = SettlementFixture.createLedger();

            assertThat(ledger.getProcessed()).isFalse();
        }

        @Test
        @DisplayName("should_setAllFields_when_builtWithBuilder")
        void should_setAllFields_when_builtWithBuilder() {
            Instant eventTime = Instant.parse("2026-03-01T10:00:00Z");

            SettlementLedger ledger = SettlementFixture.ledgerBuilder()
                    .orderNumber("ORD-999")
                    .sellerId(200L)
                    .eventType("ORDER_CANCELLED")
                    .amount(new BigDecimal("-5000.00"))
                    .eventAt(eventTime)
                    .build();

            assertThat(ledger.getOrderNumber()).isEqualTo("ORD-999");
            assertThat(ledger.getSellerId()).isEqualTo(200L);
            assertThat(ledger.getEventType()).isEqualTo("ORDER_CANCELLED");
            assertThat(ledger.getAmount()).isEqualByComparingTo("-5000.00");
            assertThat(ledger.getEventAt()).isEqualTo(eventTime);
        }
    }

    @Nested
    @DisplayName("markProcessed")
    class MarkProcessed {

        @Test
        @DisplayName("should_setProcessedTrue_when_markProcessed")
        void should_setProcessedTrue_when_markProcessed() {
            SettlementLedger ledger = SettlementFixture.createLedger();

            ledger.markProcessed();

            assertThat(ledger.getProcessed()).isTrue();
        }
    }
}
