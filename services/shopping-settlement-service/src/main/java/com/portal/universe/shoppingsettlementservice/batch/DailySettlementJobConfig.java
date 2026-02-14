package com.portal.universe.shoppingsettlementservice.batch;

import com.portal.universe.shoppingsettlementservice.settlement.domain.*;
import com.portal.universe.shoppingsettlementservice.settlement.repository.SettlementLedgerRepository;
import com.portal.universe.shoppingsettlementservice.settlement.repository.SettlementPeriodRepository;
import com.portal.universe.shoppingsettlementservice.settlement.repository.SettlementRepository;
import com.portal.universe.shoppingsettlementservice.settlement.repository.SettlementDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DailySettlementJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final SettlementLedgerRepository ledgerRepository;
    private final SettlementPeriodRepository periodRepository;
    private final SettlementRepository settlementRepository;
    private final SettlementDetailRepository detailRepository;

    @Bean
    public Job dailySettlementJob() {
        return new JobBuilder("dailySettlementJob", jobRepository)
                .start(calculateDailySettlementStep())
                .build();
    }

    @Bean
    public Step calculateDailySettlementStep() {
        return new StepBuilder("calculateDailySettlementStep", jobRepository)
                .tasklet(dailySettlementTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet dailySettlementTasklet() {
        return (contribution, chunkContext) -> {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            LocalDateTime startOfDay = yesterday.atStartOfDay();
            LocalDateTime endOfDay = yesterday.atTime(23, 59, 59);

            log.info("Starting daily settlement for: {}", yesterday);

            // 1. Create settlement period
            SettlementPeriod period = SettlementPeriod.builder()
                    .periodType(PeriodType.DAILY)
                    .startDate(yesterday)
                    .endDate(yesterday)
                    .build();
            period.startProcessing();
            periodRepository.save(period);

            // 2. Get unprocessed ledger entries for the day
            List<SettlementLedger> entries = ledgerRepository
                    .findByProcessedFalseAndEventAtBetween(startOfDay, endOfDay);

            if (entries.isEmpty()) {
                log.info("No ledger entries found for: {}", yesterday);
                period.complete();
                return RepeatStatus.FINISHED;
            }

            // 3. Group by seller
            Map<Long, List<SettlementLedger>> bySeller = entries.stream()
                    .collect(Collectors.groupingBy(SettlementLedger::getSellerId));

            // 4. Calculate per seller
            for (Map.Entry<Long, List<SettlementLedger>> entry : bySeller.entrySet()) {
                Long sellerId = entry.getKey();
                List<SettlementLedger> sellerEntries = entry.getValue();

                BigDecimal totalSales = sellerEntries.stream()
                        .filter(e -> "PAYMENT_COMPLETED".equals(e.getEventType()))
                        .map(SettlementLedger::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalRefunds = sellerEntries.stream()
                        .filter(e -> "ORDER_CANCELLED".equals(e.getEventType()))
                        .map(SettlementLedger::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                int totalOrders = (int) sellerEntries.stream()
                        .filter(e -> "PAYMENT_COMPLETED".equals(e.getEventType()))
                        .count();

                // Default commission rate 10%
                BigDecimal commissionRate = new BigDecimal("10.00");
                BigDecimal netSales = totalSales.subtract(totalRefunds);
                BigDecimal commissionAmount = netSales.multiply(commissionRate)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                BigDecimal netAmount = netSales.subtract(commissionAmount);

                Settlement settlement = Settlement.builder()
                        .periodId(period.getId())
                        .sellerId(sellerId)
                        .totalSales(totalSales)
                        .totalOrders(totalOrders)
                        .totalRefunds(totalRefunds)
                        .commissionAmount(commissionAmount)
                        .netAmount(netAmount)
                        .build();
                settlementRepository.save(settlement);

                // Mark entries as processed
                sellerEntries.forEach(SettlementLedger::markProcessed);
            }

            period.complete();
            log.info("Daily settlement completed for: {}, sellers: {}", yesterday, bySeller.size());

            return RepeatStatus.FINISHED;
        };
    }
}
