package com.portal.universe.shoppingsettlementservice.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@ConditionalOnProperty(name = "settlement.batch.scheduled-enabled", havingValue = "true")
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SettlementScheduler {

    private final JobLauncher jobLauncher;
    private final Job dailySettlementJob;

    @Scheduled(cron = "0 0 2 * * *")
    public void runDailySettlement() {
        try {
            String targetDate = LocalDate.now().minusDays(1).toString();
            log.info("Scheduled daily settlement started for: {}", targetDate);

            JobParameters params = new JobParametersBuilder()
                    .addString("targetDate", targetDate)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(dailySettlementJob, params);
        } catch (Exception e) {
            log.error("Scheduled daily settlement failed", e);
        }
    }
}
