package com.portal.universe.shoppingsettlementservice.batch;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingsettlementservice.batch.dto.SellerAggregation;
import com.portal.universe.shoppingsettlementservice.batch.exception.InvalidAmountException;
import com.portal.universe.shoppingsettlementservice.batch.listener.SettlementJobListener;
import com.portal.universe.shoppingsettlementservice.batch.partitioner.SellerIdRangePartitioner;
import com.portal.universe.shoppingsettlementservice.batch.processor.SettlementCalculationProcessor;
import com.portal.universe.shoppingsettlementservice.batch.reader.SellerAggregationRowMapper;
import com.portal.universe.shoppingsettlementservice.batch.writer.LedgerProcessedUpdateWriter;
import com.portal.universe.shoppingsettlementservice.common.exception.SettlementErrorCode;
import com.portal.universe.shoppingsettlementservice.settlement.domain.PeriodType;
import com.portal.universe.shoppingsettlementservice.settlement.domain.Settlement;
import com.portal.universe.shoppingsettlementservice.settlement.domain.SettlementPeriod;
import com.portal.universe.shoppingsettlementservice.settlement.repository.SettlementPeriodRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DailySettlementJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final SettlementPeriodRepository periodRepository;
    private final DataSource dataSource;
    private final EntityManagerFactory entityManagerFactory;
    private final SettlementBatchProperties batchProperties;

    // ==================== Job ====================

    @Bean
    public Job dailySettlementJob(SettlementJobListener jobListener) {
        return new JobBuilder("dailySettlementJob", jobRepository)
                .listener(jobListener)
                .start(createPeriodStep())
                .next(partitionedSettlementStep())
                .next(completePeriodStep())
                .build();
    }

    // ==================== Step 1: Create Period ====================

    @Bean
    public Step createPeriodStep() {
        return new StepBuilder("createPeriodStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String targetDateStr = chunkContext.getStepContext()
                            .getJobParameters().get("targetDate") != null
                            ? (String) chunkContext.getStepContext().getJobParameters().get("targetDate")
                            : null;

                    LocalDate targetDate = targetDateStr != null
                            ? LocalDate.parse(targetDateStr)
                            : LocalDate.now().minusDays(1);

                    log.info("Creating settlement period for: {}", targetDate);

                    if (periodRepository.existsByPeriodTypeAndStartDateAndEndDate(
                            PeriodType.DAILY, targetDate, targetDate)) {
                        throw new CustomBusinessException(SettlementErrorCode.DUPLICATE_PERIOD);
                    }

                    SettlementPeriod period = SettlementPeriod.builder()
                            .periodType(PeriodType.DAILY)
                            .startDate(targetDate)
                            .endDate(targetDate)
                            .build();
                    period.startProcessing();
                    periodRepository.save(period);

                    Instant startOfDay = targetDate.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();
                    Instant endOfDay = targetDate.atTime(23, 59, 59).atZone(ZoneId.of("Asia/Seoul")).toInstant();

                    var jobContext = chunkContext.getStepContext()
                            .getStepExecution().getJobExecution().getExecutionContext();
                    jobContext.putLong("periodId", period.getId());
                    jobContext.putString("startDate", startOfDay.toString());
                    jobContext.putString("endDate", endOfDay.toString());

                    log.info("Period created: id={}, date={}", period.getId(), targetDate);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    // ==================== Step 2: Partitioned Settlement ====================

    @Bean
    public Step partitionedSettlementStep() {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        handler.setStep(workerStep());
        handler.setTaskExecutor(settlementTaskExecutor());
        handler.setGridSize(batchProperties.getGridSize());

        return new StepBuilder("partitionedSettlementStep", jobRepository)
                .partitioner("workerStep", settlementPartitioner(null, null))
                .partitionHandler(handler)
                .build();
    }

    @Bean
    @StepScope
    public SellerIdRangePartitioner settlementPartitioner(
            @Value("#{jobExecutionContext['startDate']}") String startDate,
            @Value("#{jobExecutionContext['endDate']}") String endDate) {
        Instant start = startDate != null ? Instant.parse(startDate) : Instant.now();
        Instant end = endDate != null ? Instant.parse(endDate) : Instant.now();
        return new SellerIdRangePartitioner(
                new org.springframework.jdbc.core.JdbcTemplate(dataSource), start, end);
    }

    @Bean
    public Step workerStep() {
        return new StepBuilder("workerStep", jobRepository)
                .<SellerAggregation, Settlement>chunk(batchProperties.getChunkSize(), transactionManager)
                .reader(sellerAggregationReader(null, null, null, null))
                .processor(settlementCalculationProcessor())
                .writer(compositeSettlementWriter())
                .faultTolerant()
                .retry(DeadlockLoserDataAccessException.class)
                .retryLimit(batchProperties.getRetryLimit())
                .skip(DataIntegrityViolationException.class)
                .skipLimit(batchProperties.getSkipLimit())
                .noSkip(InvalidAmountException.class)
                .build();
    }

    // ==================== Reader ====================

    @Bean
    @StepScope
    public JdbcCursorItemReader<SellerAggregation> sellerAggregationReader(
            @Value("#{stepExecutionContext['minSellerId']}") Long minSellerId,
            @Value("#{stepExecutionContext['maxSellerId']}") Long maxSellerId,
            @Value("#{stepExecutionContext['startDate']}") String startDate,
            @Value("#{stepExecutionContext['endDate']}") String endDate) {

        String sql = """
                SELECT seller_id,
                       COALESCE(SUM(CASE WHEN event_type = 'PAYMENT_COMPLETED' THEN amount ELSE 0 END), 0) AS total_sales,
                       COALESCE(SUM(CASE WHEN event_type = 'ORDER_CANCELLED' THEN amount ELSE 0 END), 0) AS total_refunds,
                       COUNT(CASE WHEN event_type = 'PAYMENT_COMPLETED' THEN 1 END) AS order_count
                FROM settlement_ledger
                WHERE processed = false
                  AND event_at BETWEEN ? AND ?
                  AND seller_id BETWEEN ? AND ?
                GROUP BY seller_id
                """;

        Instant start = startDate != null ? Instant.parse(startDate) : Instant.now();
        Instant end = endDate != null ? Instant.parse(endDate) : Instant.now();

        return new JdbcCursorItemReaderBuilder<SellerAggregation>()
                .name("sellerAggregationReader")
                .dataSource(dataSource)
                .sql(sql)
                .preparedStatementSetter(ps -> {
                    ps.setObject(1, start);
                    ps.setObject(2, end);
                    ps.setLong(3, minSellerId != null ? minSellerId : 0L);
                    ps.setLong(4, maxSellerId != null ? maxSellerId : Long.MAX_VALUE);
                })
                .rowMapper(new SellerAggregationRowMapper())
                .build();
    }

    // ==================== Processor ====================

    @Bean
    @StepScope
    public SettlementCalculationProcessor settlementCalculationProcessor() {
        return new SettlementCalculationProcessor();
    }

    // ==================== Writers ====================

    @Bean
    public CompositeItemWriter<Settlement> compositeSettlementWriter() {
        CompositeItemWriter<Settlement> writer = new CompositeItemWriter<>();
        writer.setDelegates(List.of(
                settlementJpaWriter(),
                ledgerProcessedUpdateWriter()
        ));
        return writer;
    }

    @Bean
    public JpaItemWriter<Settlement> settlementJpaWriter() {
        return new JpaItemWriterBuilder<Settlement>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    @Bean
    @StepScope
    public LedgerProcessedUpdateWriter ledgerProcessedUpdateWriter() {
        return new LedgerProcessedUpdateWriter(
                new org.springframework.jdbc.core.JdbcTemplate(dataSource));
    }

    // ==================== Step 3: Complete Period ====================

    @Bean
    public Step completePeriodStep() {
        return new StepBuilder("completePeriodStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    Long periodId = chunkContext.getStepContext()
                            .getStepExecution().getJobExecution()
                            .getExecutionContext().getLong("periodId");

                    SettlementPeriod period = periodRepository.findById(periodId)
                            .orElseThrow(() -> new CustomBusinessException(
                                    SettlementErrorCode.PERIOD_NOT_FOUND));

                    period.complete();
                    log.info("Settlement period {} completed", periodId);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    // ==================== Task Executor ====================

    @Bean
    public TaskExecutor settlementTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(batchProperties.getCorePoolSize());
        executor.setMaxPoolSize(batchProperties.getCorePoolSize());
        executor.setThreadNamePrefix("settlement-worker-");
        executor.afterPropertiesSet();
        return executor;
    }
}
