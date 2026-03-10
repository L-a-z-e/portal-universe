package com.portal.universe.shoppingsettlementservice.batch.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementScheduler")
class SettlementSchedulerTest {

    @InjectMocks
    private SettlementScheduler scheduler;

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job dailySettlementJob;

    @Nested
    @DisplayName("runDailySettlement")
    class RunDailySettlement {

        @Test
        @DisplayName("should_launchJobWithYesterdayDate_when_called")
        void should_launchJobWithYesterdayDate_when_called() throws Exception {
            when(jobLauncher.run(eq(dailySettlementJob), any(JobParameters.class)))
                    .thenReturn(mock(JobExecution.class));

            scheduler.runDailySettlement();

            ArgumentCaptor<JobParameters> captor = ArgumentCaptor.forClass(JobParameters.class);
            verify(jobLauncher).run(eq(dailySettlementJob), captor.capture());

            String targetDate = captor.getValue().getString("targetDate");
            assertThat(targetDate).isEqualTo(LocalDate.now().minusDays(1).toString());
        }

        @Test
        @DisplayName("should_notThrow_when_jobLauncherFails")
        void should_notThrow_when_jobLauncherFails() throws Exception {
            when(jobLauncher.run(eq(dailySettlementJob), any(JobParameters.class)))
                    .thenThrow(new RuntimeException("Job failed"));

            scheduler.runDailySettlement();

            verify(jobLauncher).run(eq(dailySettlementJob), any(JobParameters.class));
        }
    }
}
