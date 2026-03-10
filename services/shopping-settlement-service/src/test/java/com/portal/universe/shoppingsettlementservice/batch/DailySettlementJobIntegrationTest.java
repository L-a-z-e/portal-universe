package com.portal.universe.shoppingsettlementservice.batch;

import com.portal.universe.shoppingsettlementservice.settlement.domain.*;
import com.portal.universe.shoppingsettlementservice.settlement.repository.SettlementLedgerRepository;
import com.portal.universe.shoppingsettlementservice.settlement.repository.SettlementPeriodRepository;
import com.portal.universe.shoppingsettlementservice.settlement.repository.SettlementRepository;
import com.portal.universe.shoppingsettlementservice.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
@DisplayName("DailySettlementJob Integration Test")
class DailySettlementJobIntegrationTest extends IntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private SettlementLedgerRepository ledgerRepository;

    @Autowired
    private SettlementPeriodRepository periodRepository;

    @Autowired
    private SettlementRepository settlementRepository;

    private static final LocalDate TARGET_DATE = LocalDate.of(2026, 2, 27);

    @BeforeEach
    void setUp() {
        settlementRepository.deleteAll();
        periodRepository.deleteAll();
        ledgerRepository.deleteAll();
    }

    @Nested
    @DisplayName("normal execution")
    class NormalExecution {

        @Test
        @DisplayName("should_completeSuccessfully_when_normalLedgerEntries")
        void should_completeSuccessfully_when_normalLedgerEntries() throws Exception {
            Instant eventTime = TARGET_DATE.atTime(12, 0)
                    .atZone(ZoneId.of("Asia/Seoul")).toInstant();

            createLedger("ORD-001", 1L, "PAYMENT_COMPLETED", new BigDecimal("10000.00"), eventTime);
            createLedger("ORD-002", 1L, "PAYMENT_COMPLETED", new BigDecimal("5000.00"), eventTime);
            createLedger("ORD-003", 1L, "ORDER_CANCELLED", new BigDecimal("2000.00"), eventTime);
            createLedger("ORD-004", 2L, "PAYMENT_COMPLETED", new BigDecimal("8000.00"), eventTime);

            JobExecution execution = launchJob();

            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

            List<Settlement> settlements = settlementRepository.findAll();
            assertThat(settlements).hasSize(2);

            Settlement seller1 = settlements.stream()
                    .filter(s -> s.getSellerId() == 1L).findFirst().orElseThrow();
            assertThat(seller1.getTotalSales()).isEqualByComparingTo("15000.00");
            assertThat(seller1.getTotalRefunds()).isEqualByComparingTo("2000.00");
            assertThat(seller1.getTotalOrders()).isEqualTo(2);
            assertThat(seller1.getCommissionAmount()).isEqualByComparingTo("1300.00");
            assertThat(seller1.getNetAmount()).isEqualByComparingTo("11700.00");

            Settlement seller2 = settlements.stream()
                    .filter(s -> s.getSellerId() == 2L).findFirst().orElseThrow();
            assertThat(seller2.getTotalSales()).isEqualByComparingTo("8000.00");
            assertThat(seller2.getCommissionAmount()).isEqualByComparingTo("800.00");

            List<SettlementLedger> ledgers = ledgerRepository.findAll();
            assertThat(ledgers).allMatch(SettlementLedger::getProcessed);

            List<SettlementPeriod> periods = periodRepository.findAll();
            assertThat(periods).hasSize(1);
            assertThat(periods.get(0).getStatus()).isEqualTo(PeriodStatus.COMPLETED);
        }

        @Test
        @DisplayName("should_processMultiplePartitions_when_manySellers")
        void should_processMultiplePartitions_when_manySellers() throws Exception {
            Instant eventTime = TARGET_DATE.atTime(12, 0)
                    .atZone(ZoneId.of("Asia/Seoul")).toInstant();

            for (long sellerId = 1; sellerId <= 6; sellerId++) {
                createLedger("ORD-S" + sellerId, sellerId, "PAYMENT_COMPLETED",
                        new BigDecimal("1000.00"), eventTime);
            }

            JobExecution execution = launchJob();

            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(settlementRepository.findAll()).hasSize(6);
            assertThat(ledgerRepository.findAll()).allMatch(SettlementLedger::getProcessed);
        }
    }

    @Nested
    @DisplayName("edge cases")
    class EdgeCases {

        @Test
        @DisplayName("should_completeWithZeroSettlements_when_emptyLedger")
        void should_completeWithZeroSettlements_when_emptyLedger() throws Exception {
            JobExecution execution = launchJob();

            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(settlementRepository.findAll()).isEmpty();

            List<SettlementPeriod> periods = periodRepository.findAll();
            assertThat(periods).hasSize(1);
            assertThat(periods.get(0).getStatus()).isEqualTo(PeriodStatus.COMPLETED);
        }

        @Test
        @DisplayName("should_failWithDuplicatePeriod_when_sameTargetDateTwice")
        void should_failWithDuplicatePeriod_when_sameTargetDateTwice() throws Exception {
            launchJob();

            JobParameters params2 = new JobParametersBuilder()
                    .addString("targetDate", TARGET_DATE.toString())
                    .addLong("timestamp", System.currentTimeMillis() + 1)
                    .toJobParameters();
            JobExecution execution = jobLauncherTestUtils.launchJob(params2);

            assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
            assertThat(periodRepository.findAll()).hasSize(1);
        }
    }

    // ── Helper methods ──

    private JobExecution launchJob() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addString("targetDate", TARGET_DATE.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        return jobLauncherTestUtils.launchJob(params);
    }

    private void createLedger(String orderNumber, Long sellerId, String eventType,
                              BigDecimal amount, Instant eventAt) {
        SettlementLedger ledger = SettlementLedger.builder()
                .orderNumber(orderNumber)
                .sellerId(sellerId)
                .eventType(eventType)
                .amount(amount)
                .eventAt(eventAt)
                .build();
        ledgerRepository.save(ledger);
    }
}
