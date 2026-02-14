package com.portal.universe.shoppingsettlementservice.batch;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingsettlementservice.common.exception.SettlementErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/batch")
@RequiredArgsConstructor
@Slf4j
public class BatchController {

    private final JobLauncher jobLauncher;
    private final Job dailySettlementJob;

    @PostMapping("/daily")
    public ApiResponse<String> runDailySettlement() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addDate("executionDate", new Date())
                    .toJobParameters();
            jobLauncher.run(dailySettlementJob, params);
            return ApiResponse.success("Daily settlement job started");
        } catch (Exception e) {
            log.error("Failed to run daily settlement job", e);
            throw new CustomBusinessException(SettlementErrorCode.BATCH_EXECUTION_FAILED);
        }
    }
}
