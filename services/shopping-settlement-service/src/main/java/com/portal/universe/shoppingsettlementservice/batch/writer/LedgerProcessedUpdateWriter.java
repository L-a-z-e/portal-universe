package com.portal.universe.shoppingsettlementservice.batch.writer;

import com.portal.universe.shoppingsettlementservice.settlement.domain.Settlement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;

@RequiredArgsConstructor
@Slf4j
public class LedgerProcessedUpdateWriter implements ItemWriter<Settlement> {

    private final JdbcTemplate jdbcTemplate;

    private Instant startDate;
    private Instant endDate;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.startDate = Instant.parse(
                stepExecution.getExecutionContext().getString("startDate"));
        this.endDate = Instant.parse(
                stepExecution.getExecutionContext().getString("endDate"));
    }

    @Override
    public void write(Chunk<? extends Settlement> items) {
        for (Settlement settlement : items) {
            int updated = jdbcTemplate.update(
                    "UPDATE settlement_ledger SET processed = true, updated_at = now() " +
                            "WHERE seller_id = ? AND processed = false " +
                            "AND event_at BETWEEN ? AND ?",
                    settlement.getSellerId(), startDate, endDate
            );
            log.debug("Marked {} ledger entries as processed for seller {}",
                    updated, settlement.getSellerId());
        }
    }
}
