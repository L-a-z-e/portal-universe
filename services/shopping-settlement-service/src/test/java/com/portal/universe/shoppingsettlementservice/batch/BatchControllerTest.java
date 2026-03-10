package com.portal.universe.shoppingsettlementservice.batch;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingsettlementservice.common.exception.SettlementErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BatchController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("BatchController")
class BatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobLauncher jobLauncher;

    @MockitoBean
    private Job dailySettlementJob;

    @Nested
    @DisplayName("POST /batch/daily")
    class RunDailySettlement {

        @Test
        @DisplayName("should_returnJobStatus_when_targetDateProvided")
        void should_returnJobStatus_when_targetDateProvided() throws Exception {
            JobExecution execution = createCompletedJobExecution();
            when(jobLauncher.run(eq(dailySettlementJob), any(JobParameters.class)))
                    .thenReturn(execution);

            mockMvc.perform(post("/batch/daily").param("targetDate", "2026-02-27"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("should_useYesterdayDate_when_noTargetDate")
        void should_useYesterdayDate_when_noTargetDate() throws Exception {
            JobExecution execution = createCompletedJobExecution();
            when(jobLauncher.run(eq(dailySettlementJob), any(JobParameters.class)))
                    .thenReturn(execution);

            mockMvc.perform(post("/batch/daily"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("should_returnError_when_jobExecutionFails")
        void should_returnError_when_jobExecutionFails() throws Exception {
            when(jobLauncher.run(eq(dailySettlementJob), any(JobParameters.class)))
                    .thenThrow(new RuntimeException("Job failed"));

            mockMvc.perform(post("/batch/daily").param("targetDate", "2026-02-27"))
                    .andExpect(status().isInternalServerError());
        }
    }

    private JobExecution createCompletedJobExecution() {
        JobExecution execution = new JobExecution(1L, new JobParameters());
        execution.setStatus(BatchStatus.COMPLETED);
        execution.setStartTime(LocalDateTime.of(2026, 2, 27, 2, 0, 0));
        execution.setEndTime(LocalDateTime.of(2026, 2, 27, 2, 5, 0));
        execution.setExitStatus(ExitStatus.COMPLETED);
        return execution;
    }
}
