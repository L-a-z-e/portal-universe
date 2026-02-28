package com.portal.universe.shoppingsettlementservice.batch;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingsettlementservice.batch.dto.BatchJobStatusResponse;
import com.portal.universe.shoppingsettlementservice.common.exception.SettlementErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/batch")
@RequiredArgsConstructor
@Slf4j
public class BatchController {

    private final JobLauncher jobLauncher;
    private final Job dailySettlementJob;

    @PostMapping("/daily")
    public ApiResponse<BatchJobStatusResponse> runDailySettlement(
            @RequestParam(required = false) String targetDate) {
        try {
            String date = targetDate != null ? targetDate
                    : LocalDate.now().minusDays(1).toString();

            JobParameters params = new JobParametersBuilder()
                    .addString("targetDate", date)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(dailySettlementJob, params);

            BatchJobStatusResponse response = new BatchJobStatusResponse(
                    execution.getId(),
                    execution.getStatus(),
                    execution.getStartTime(),
                    execution.getEndTime(),
                    execution.getExitStatus().getExitDescription()
            );

            return ApiResponse.success(response);
        } catch (CustomBusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to run daily settlement job", e);
            throw new CustomBusinessException(SettlementErrorCode.BATCH_EXECUTION_FAILED);
        }
    }
}
