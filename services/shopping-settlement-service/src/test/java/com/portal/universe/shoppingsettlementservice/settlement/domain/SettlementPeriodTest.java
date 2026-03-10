package com.portal.universe.shoppingsettlementservice.settlement.domain;

import com.portal.universe.shoppingsettlementservice.support.fixture.SettlementFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SettlementPeriod")
class SettlementPeriodTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should_havePendingStatus_when_created")
        void should_havePendingStatus_when_created() {
            SettlementPeriod period = SettlementFixture.createPeriod();

            assertThat(period.getStatus()).isEqualTo(PeriodStatus.PENDING);
        }

        @Test
        @DisplayName("should_setAllFields_when_builtWithBuilder")
        void should_setAllFields_when_builtWithBuilder() {
            SettlementPeriod period = SettlementFixture.periodBuilder()
                    .periodType(PeriodType.WEEKLY)
                    .startDate(LocalDate.of(2026, 3, 1))
                    .endDate(LocalDate.of(2026, 3, 7))
                    .build();

            assertThat(period.getPeriodType()).isEqualTo(PeriodType.WEEKLY);
            assertThat(period.getStartDate()).isEqualTo(LocalDate.of(2026, 3, 1));
            assertThat(period.getEndDate()).isEqualTo(LocalDate.of(2026, 3, 7));
        }
    }

    @Nested
    @DisplayName("startProcessing")
    class StartProcessing {

        @Test
        @DisplayName("should_changeStatusToProcessing_when_startProcessing")
        void should_changeStatusToProcessing_when_startProcessing() {
            SettlementPeriod period = SettlementFixture.createPeriod();

            period.startProcessing();

            assertThat(period.getStatus()).isEqualTo(PeriodStatus.PROCESSING);
        }
    }

    @Nested
    @DisplayName("complete")
    class Complete {

        @Test
        @DisplayName("should_changeStatusToCompleted_when_complete")
        void should_changeStatusToCompleted_when_complete() {
            SettlementPeriod period = SettlementFixture.createPeriod();

            period.complete();

            assertThat(period.getStatus()).isEqualTo(PeriodStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("fail")
    class Fail {

        @Test
        @DisplayName("should_changeStatusToFailed_when_fail")
        void should_changeStatusToFailed_when_fail() {
            SettlementPeriod period = SettlementFixture.createPeriod();

            period.fail();

            assertThat(period.getStatus()).isEqualTo(PeriodStatus.FAILED);
        }
    }
}
