package com.portal.universe.shoppingsettlementservice.batch.listener;

import com.portal.universe.shoppingsettlementservice.settlement.domain.SettlementPeriod;
import com.portal.universe.shoppingsettlementservice.settlement.repository.SettlementPeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementJobListener implements JobExecutionListener {

    private final SettlementPeriodRepository periodRepository;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Settlement job started: params={}",
                jobExecution.getJobParameters());
    }

    @Override
    @Transactional
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.FAILED) {
            Long periodId = jobExecution.getExecutionContext().getLong("periodId", -1L);
            if (periodId > 0) {
                periodRepository.findById(periodId).ifPresent(period -> {
                    period.fail();
                    log.error("Settlement period {} marked as FAILED", periodId);
                });
            }

            jobExecution.getAllFailureExceptions().forEach(ex ->
                    log.error("Job failure cause: {}", ex.getMessage(), ex));
        } else {
            log.info("Settlement job completed: status={}, duration={}ms",
                    jobExecution.getStatus(),
                    jobExecution.getEndTime() != null && jobExecution.getStartTime() != null
                            ? java.time.Duration.between(
                            jobExecution.getStartTime(), jobExecution.getEndTime()).toMillis()
                            : "N/A");
        }
    }
}
