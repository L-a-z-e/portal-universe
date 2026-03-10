package com.portal.universe.shoppingsettlementservice.batch.listener;

import com.portal.universe.shoppingsettlementservice.settlement.domain.PeriodStatus;
import com.portal.universe.shoppingsettlementservice.settlement.domain.SettlementPeriod;
import com.portal.universe.shoppingsettlementservice.settlement.repository.SettlementPeriodRepository;
import com.portal.universe.shoppingsettlementservice.support.fixture.SettlementFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementJobListener")
class SettlementJobListenerTest {

    @InjectMocks
    private SettlementJobListener listener;

    @Mock
    private SettlementPeriodRepository periodRepository;

    @Nested
    @DisplayName("afterJob")
    class AfterJob {

        @Test
        @DisplayName("should_markPeriodFailed_when_jobFailed")
        void should_markPeriodFailed_when_jobFailed() {
            JobExecution jobExecution = new JobExecution(1L, new JobParameters());
            jobExecution.setStatus(BatchStatus.FAILED);
            jobExecution.getExecutionContext().putLong("periodId", 10L);

            SettlementPeriod period = SettlementFixture.periodBuilder().id(10L).build();
            when(periodRepository.findById(10L)).thenReturn(Optional.of(period));

            listener.afterJob(jobExecution);

            assertThat(period.getStatus()).isEqualTo(PeriodStatus.FAILED);
        }

        @Test
        @DisplayName("should_doNothing_when_jobFailedButNoPeriodId")
        void should_doNothing_when_jobFailedButNoPeriodId() {
            JobExecution jobExecution = new JobExecution(1L, new JobParameters());
            jobExecution.setStatus(BatchStatus.FAILED);

            listener.afterJob(jobExecution);

            verifyNoInteractions(periodRepository);
        }

        @Test
        @DisplayName("should_doNothing_when_jobSucceeded")
        void should_doNothing_when_jobSucceeded() {
            JobExecution jobExecution = new JobExecution(1L, new JobParameters());
            jobExecution.setStatus(BatchStatus.COMPLETED);

            listener.afterJob(jobExecution);

            verifyNoInteractions(periodRepository);
        }
    }

    @Nested
    @DisplayName("beforeJob")
    class BeforeJob {

        @Test
        @DisplayName("should_notThrow_when_beforeJobCalled")
        void should_notThrow_when_beforeJobCalled() {
            JobExecution jobExecution = new JobExecution(1L, new JobParameters());

            listener.beforeJob(jobExecution);

            verifyNoInteractions(periodRepository);
        }
    }
}
