package com.portal.universe.shoppingsettlementservice.batch.partitioner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class SellerIdRangePartitioner implements Partitioner {

    private final JdbcTemplate jdbcTemplate;
    private final Instant startDate;
    private final Instant endDate;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        List<Long> sellerIds = jdbcTemplate.queryForList(
                "SELECT DISTINCT seller_id FROM settlement_ledger " +
                        "WHERE processed = false AND event_at BETWEEN ? AND ? " +
                        "ORDER BY seller_id",
                Long.class, startDate, endDate
        );

        Map<String, ExecutionContext> partitions = new HashMap<>();

        if (sellerIds.isEmpty()) {
            log.info("No unprocessed sellers found for partitioning");
            return partitions;
        }

        int partitionSize = Math.max(1, (int) Math.ceil((double) sellerIds.size() / gridSize));
        log.info("Partitioning {} sellers into {} partitions (size ~{})",
                sellerIds.size(), gridSize, partitionSize);

        for (int i = 0; i < sellerIds.size(); i += partitionSize) {
            int end = Math.min(i + partitionSize, sellerIds.size());
            List<Long> partition = sellerIds.subList(i, end);

            ExecutionContext context = new ExecutionContext();
            context.putLong("minSellerId", partition.get(0));
            context.putLong("maxSellerId", partition.get(partition.size() - 1));
            context.putString("startDate", startDate.toString());
            context.putString("endDate", endDate.toString());

            String partitionName = "partition" + (i / partitionSize);
            partitions.put(partitionName, context);

            log.debug("Partition {}: seller_id [{} - {}], {} sellers",
                    partitionName, partition.get(0), partition.get(partition.size() - 1), partition.size());
        }

        return partitions;
    }
}
