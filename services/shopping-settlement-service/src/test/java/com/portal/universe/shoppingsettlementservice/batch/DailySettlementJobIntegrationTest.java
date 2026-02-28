package com.portal.universe.shoppingsettlementservice.batch;

import com.portal.universe.shoppingsettlementservice.settlement.domain.*;
import com.portal.universe.shoppingsettlementservice.settlement.repository.SettlementLedgerRepository;
import com.portal.universe.shoppingsettlementservice.settlement.repository.SettlementPeriodRepository;
import com.portal.universe.shoppingsettlementservice.settlement.repository.SettlementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
@SpringBootTest
@Testcontainers
@ContextConfiguration(initializers = DailySettlementJobIntegrationTest.DataSourceInitializer.class)
@DisplayName("DailySettlementJob Integration Test")
class DailySettlementJobIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("settlement_test_db")
                    .withUsername("test")
                    .withPassword("test");

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private SettlementLedgerRepository ledgerRepository;

    @Autowired
    private SettlementPeriodRepository periodRepository;

    @Autowired
    private SettlementRepository settlementRepository;

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static final LocalDate TARGET_DATE = LocalDate.of(2026, 2, 27);

    @BeforeEach
    void setUp() {
        settlementRepository.deleteAll();
        periodRepository.deleteAll();
        ledgerRepository.deleteAll();
    }

    @Test
    @DisplayName("should_completeSuccessfully_when_normalLedgerEntries")
    void should_completeSuccessfully_when_normalLedgerEntries() throws Exception {
        // Given
        Instant eventTime = TARGET_DATE.atTime(12, 0)
                .atZone(ZoneId.of("Asia/Seoul")).toInstant();

        createLedger("ORD-001", 1L, "PAYMENT_COMPLETED", new BigDecimal("10000.00"), eventTime);
        createLedger("ORD-002", 1L, "PAYMENT_COMPLETED", new BigDecimal("5000.00"), eventTime);
        createLedger("ORD-003", 1L, "ORDER_CANCELLED", new BigDecimal("2000.00"), eventTime);
        createLedger("ORD-004", 2L, "PAYMENT_COMPLETED", new BigDecimal("8000.00"), eventTime);

        // When
        JobParameters params = new JobParametersBuilder()
                .addString("targetDate", TARGET_DATE.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        // Then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        List<Settlement> settlements = settlementRepository.findAll();
        assertThat(settlements).hasSize(2);

        Settlement seller1 = settlements.stream()
                .filter(s -> s.getSellerId() == 1L).findFirst().orElseThrow();
        assertThat(seller1.getTotalSales()).isEqualByComparingTo("15000.00");
        assertThat(seller1.getTotalRefunds()).isEqualByComparingTo("2000.00");
        assertThat(seller1.getTotalOrders()).isEqualTo(2);
        // netSales = 13000, commission = 1300, netAmount = 11700
        assertThat(seller1.getCommissionAmount()).isEqualByComparingTo("1300.00");
        assertThat(seller1.getNetAmount()).isEqualByComparingTo("11700.00");

        Settlement seller2 = settlements.stream()
                .filter(s -> s.getSellerId() == 2L).findFirst().orElseThrow();
        assertThat(seller2.getTotalSales()).isEqualByComparingTo("8000.00");
        assertThat(seller2.getCommissionAmount()).isEqualByComparingTo("800.00");

        // Verify ledger entries marked as processed
        List<SettlementLedger> ledgers = ledgerRepository.findAll();
        assertThat(ledgers).allMatch(SettlementLedger::getProcessed);

        // Verify period is COMPLETED
        List<SettlementPeriod> periods = periodRepository.findAll();
        assertThat(periods).hasSize(1);
        assertThat(periods.get(0).getStatus()).isEqualTo(PeriodStatus.COMPLETED);
    }

    @Test
    @DisplayName("should_completeWithZeroSettlements_when_emptyLedger")
    void should_completeWithZeroSettlements_when_emptyLedger() throws Exception {
        // Given: no ledger entries

        // When
        JobParameters params = new JobParametersBuilder()
                .addString("targetDate", TARGET_DATE.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        // Then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(settlementRepository.findAll()).isEmpty();

        List<SettlementPeriod> periods = periodRepository.findAll();
        assertThat(periods).hasSize(1);
        assertThat(periods.get(0).getStatus()).isEqualTo(PeriodStatus.COMPLETED);
    }

    @Test
    @DisplayName("should_failWithDuplicatePeriod_when_sameTargetDateTwice")
    void should_failWithDuplicatePeriod_when_sameTargetDateTwice() throws Exception {
        // Given: first run succeeds
        JobParameters params1 = new JobParametersBuilder()
                .addString("targetDate", TARGET_DATE.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        jobLauncherTestUtils.launchJob(params1);

        // When: second run with same date
        JobParameters params2 = new JobParametersBuilder()
                .addString("targetDate", TARGET_DATE.toString())
                .addLong("timestamp", System.currentTimeMillis() + 1)
                .toJobParameters();
        JobExecution execution = jobLauncherTestUtils.launchJob(params2);

        // Then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(periodRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("should_processMultiplePartitions_when_manySellers")
    void should_processMultiplePartitions_when_manySellers() throws Exception {
        // Given: 6 sellers
        Instant eventTime = TARGET_DATE.atTime(12, 0)
                .atZone(ZoneId.of("Asia/Seoul")).toInstant();

        for (long sellerId = 1; sellerId <= 6; sellerId++) {
            createLedger("ORD-S" + sellerId, sellerId, "PAYMENT_COMPLETED",
                    new BigDecimal("1000.00"), eventTime);
        }

        // When
        JobParameters params = new JobParametersBuilder()
                .addString("targetDate", TARGET_DATE.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        // Then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(settlementRepository.findAll()).hasSize(6);

        // All ledgers should be processed
        assertThat(ledgerRepository.findAll()).allMatch(SettlementLedger::getProcessed);
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

    static class DataSourceInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext ctx) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(ctx,
                    "spring.cloud.config.enabled=false",
                    "spring.cloud.discovery.enabled=false",
                    "spring.datasource.url=" + postgres.getJdbcUrl(),
                    "spring.datasource.username=" + postgres.getUsername(),
                    "spring.datasource.password=" + postgres.getPassword(),
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.flyway.enabled=false",
                    "spring.batch.jdbc.initialize-schema=always",
                    "spring.batch.job.enabled=false",
                    "settlement.batch.chunk-size=2",
                    "settlement.batch.grid-size=2",
                    "settlement.batch.retry-limit=3",
                    "settlement.batch.skip-limit=10",
                    "settlement.batch.core-pool-size=2",
                    "settlement.batch.scheduled-enabled=false"
            );
        }
    }
}
