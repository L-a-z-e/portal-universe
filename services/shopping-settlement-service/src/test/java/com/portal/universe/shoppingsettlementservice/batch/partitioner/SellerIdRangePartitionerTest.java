package com.portal.universe.shoppingsettlementservice.batch.partitioner;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("SellerIdRangePartitioner")
class SellerIdRangePartitionerTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final Instant start = Instant.parse("2026-02-27T00:00:00Z");
    private final Instant end = Instant.parse("2026-02-27T23:59:59Z");

    @Nested
    @DisplayName("partition")
    class Partition {

        @Test
        @DisplayName("should_createCorrectPartitions_when_multipleSellers")
        void should_createCorrectPartitions_when_multipleSellers() {
            when(jdbcTemplate.queryForList(anyString(), eq(Long.class), any(), any()))
                    .thenReturn(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L));

            SellerIdRangePartitioner partitioner = new SellerIdRangePartitioner(
                    jdbcTemplate, start, end);

            Map<String, ExecutionContext> partitions = partitioner.partition(4);

            assertThat(partitions).hasSize(4);

            ExecutionContext first = partitions.get("partition0");
            assertThat(first.getLong("minSellerId")).isEqualTo(1L);
            assertThat(first.getLong("maxSellerId")).isEqualTo(2L);
            assertThat(first.getString("startDate")).isEqualTo(start.toString());
            assertThat(first.getString("endDate")).isEqualTo(end.toString());

            ExecutionContext last = partitions.get("partition3");
            assertThat(last.getLong("minSellerId")).isEqualTo(7L);
            assertThat(last.getLong("maxSellerId")).isEqualTo(8L);
        }

        @Test
        @DisplayName("should_returnEmptyMap_when_noSellers")
        void should_returnEmptyMap_when_noSellers() {
            when(jdbcTemplate.queryForList(anyString(), eq(Long.class), any(), any()))
                    .thenReturn(List.of());

            SellerIdRangePartitioner partitioner = new SellerIdRangePartitioner(
                    jdbcTemplate, start, end);

            Map<String, ExecutionContext> partitions = partitioner.partition(4);

            assertThat(partitions).isEmpty();
        }

        @Test
        @DisplayName("should_createFewerPartitions_when_fewerSellersThanGrid")
        void should_createFewerPartitions_when_fewerSellersThanGrid() {
            when(jdbcTemplate.queryForList(anyString(), eq(Long.class), any(), any()))
                    .thenReturn(List.of(10L, 20L));

            SellerIdRangePartitioner partitioner = new SellerIdRangePartitioner(
                    jdbcTemplate, start, end);

            Map<String, ExecutionContext> partitions = partitioner.partition(4);

            assertThat(partitions).hasSize(2);

            ExecutionContext first = partitions.get("partition0");
            assertThat(first.getLong("minSellerId")).isEqualTo(10L);
            assertThat(first.getLong("maxSellerId")).isEqualTo(10L);

            ExecutionContext second = partitions.get("partition1");
            assertThat(second.getLong("minSellerId")).isEqualTo(20L);
            assertThat(second.getLong("maxSellerId")).isEqualTo(20L);
        }

        @Test
        @DisplayName("should_handleUnevenDistribution_when_notDivisible")
        void should_handleUnevenDistribution_when_notDivisible() {
            when(jdbcTemplate.queryForList(anyString(), eq(Long.class), any(), any()))
                    .thenReturn(List.of(1L, 2L, 3L, 4L, 5L));

            SellerIdRangePartitioner partitioner = new SellerIdRangePartitioner(
                    jdbcTemplate, start, end);

            Map<String, ExecutionContext> partitions = partitioner.partition(3);

            assertThat(partitions).hasSize(3);
            assertThat(partitions.get("partition0").getLong("minSellerId")).isEqualTo(1L);
            assertThat(partitions.get("partition0").getLong("maxSellerId")).isEqualTo(2L);
            assertThat(partitions.get("partition1").getLong("minSellerId")).isEqualTo(3L);
            assertThat(partitions.get("partition1").getLong("maxSellerId")).isEqualTo(4L);
            assertThat(partitions.get("partition2").getLong("minSellerId")).isEqualTo(5L);
            assertThat(partitions.get("partition2").getLong("maxSellerId")).isEqualTo(5L);
        }
    }
}
