package com.portal.universe.shoppingsettlementservice.settlement.domain;

import com.portal.universe.shoppingsettlementservice.support.fixture.SettlementFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Settlement")
class SettlementTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should_haveCalculatedStatus_when_created")
        void should_haveCalculatedStatus_when_created() {
            Settlement settlement = SettlementFixture.createSettlement();

            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.CALCULATED);
            assertThat(settlement.getPaidAt()).isNull();
        }

        @Test
        @DisplayName("should_setAllFields_when_builtWithBuilder")
        void should_setAllFields_when_builtWithBuilder() {
            Settlement settlement = SettlementFixture.settlementBuilder()
                    .periodId(10L)
                    .sellerId(200L)
                    .totalSales(new BigDecimal("50000.00"))
                    .totalOrders(10)
                    .totalRefunds(new BigDecimal("5000.00"))
                    .commissionAmount(new BigDecimal("4500.00"))
                    .netAmount(new BigDecimal("40500.00"))
                    .build();

            assertThat(settlement.getPeriodId()).isEqualTo(10L);
            assertThat(settlement.getSellerId()).isEqualTo(200L);
            assertThat(settlement.getTotalSales()).isEqualByComparingTo("50000.00");
            assertThat(settlement.getTotalOrders()).isEqualTo(10);
            assertThat(settlement.getTotalRefunds()).isEqualByComparingTo("5000.00");
            assertThat(settlement.getCommissionAmount()).isEqualByComparingTo("4500.00");
            assertThat(settlement.getNetAmount()).isEqualByComparingTo("40500.00");
        }
    }

    @Nested
    @DisplayName("confirm")
    class Confirm {

        @Test
        @DisplayName("should_changeStatusToConfirmed_when_confirm")
        void should_changeStatusToConfirmed_when_confirm() {
            Settlement settlement = SettlementFixture.createSettlement();

            settlement.confirm();

            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.CONFIRMED);
        }
    }

    @Nested
    @DisplayName("markPaid")
    class MarkPaid {

        @Test
        @DisplayName("should_changeStatusToPaidAndSetPaidAt_when_markPaid")
        void should_changeStatusToPaidAndSetPaidAt_when_markPaid() {
            Settlement settlement = SettlementFixture.createSettlement();

            settlement.markPaid();

            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.PAID);
            assertThat(settlement.getPaidAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("dispute")
    class Dispute {

        @Test
        @DisplayName("should_changeStatusToDisputed_when_dispute")
        void should_changeStatusToDisputed_when_dispute() {
            Settlement settlement = SettlementFixture.createSettlement();

            settlement.dispute();

            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.DISPUTED);
        }
    }
}
