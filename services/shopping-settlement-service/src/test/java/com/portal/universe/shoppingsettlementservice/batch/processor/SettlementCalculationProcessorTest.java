package com.portal.universe.shoppingsettlementservice.batch.processor;

import com.portal.universe.shoppingsettlementservice.batch.dto.SellerAggregation;
import com.portal.universe.shoppingsettlementservice.batch.exception.InvalidAmountException;
import com.portal.universe.shoppingsettlementservice.settlement.domain.Settlement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.MetaDataInstanceFactory;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SettlementCalculationProcessor")
class SettlementCalculationProcessorTest {

    private SettlementCalculationProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new SettlementCalculationProcessor();

        JobExecution jobExecution = MetaDataInstanceFactory.createJobExecution();
        jobExecution.getExecutionContext().putLong("periodId", 1L);

        StepExecution stepExecution = new StepExecution("workerStep", jobExecution);
        processor.beforeStep(stepExecution);
    }

    @Nested
    @DisplayName("process")
    class Process {

        @Test
        @DisplayName("should_calculateCorrectly_when_normalAggregation")
        void should_calculateCorrectly_when_normalAggregation() {
            SellerAggregation aggregation = new SellerAggregation(
                    100L, new BigDecimal("10000.00"), new BigDecimal("1000.00"), 5);

            Settlement result = processor.process(aggregation);

            assertThat(result).isNotNull();
            assertThat(result.getSellerId()).isEqualTo(100L);
            assertThat(result.getPeriodId()).isEqualTo(1L);
            assertThat(result.getTotalSales()).isEqualByComparingTo("10000.00");
            assertThat(result.getTotalRefunds()).isEqualByComparingTo("1000.00");
            assertThat(result.getTotalOrders()).isEqualTo(5);
            assertThat(result.getCommissionAmount()).isEqualByComparingTo("900.00");
            assertThat(result.getNetAmount()).isEqualByComparingTo("8100.00");
        }

        @Test
        @DisplayName("should_calculateZero_when_salesEqualsRefunds")
        void should_calculateZero_when_salesEqualsRefunds() {
            SellerAggregation aggregation = new SellerAggregation(
                    200L, new BigDecimal("5000.00"), new BigDecimal("5000.00"), 3);

            Settlement result = processor.process(aggregation);

            assertThat(result.getCommissionAmount()).isEqualByComparingTo("0.00");
            assertThat(result.getNetAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("should_throwInvalidAmountException_when_negativeNetSales")
        void should_throwInvalidAmountException_when_negativeNetSales() {
            SellerAggregation aggregation = new SellerAggregation(
                    300L, new BigDecimal("1000.00"), new BigDecimal("2000.00"), 1);

            assertThatThrownBy(() -> processor.process(aggregation))
                    .isInstanceOf(InvalidAmountException.class)
                    .hasMessageContaining("Negative net sales")
                    .hasMessageContaining("300");
        }

        @Test
        @DisplayName("should_roundHalfUp_when_commissionHasDecimals")
        void should_roundHalfUp_when_commissionHasDecimals() {
            SellerAggregation aggregation = new SellerAggregation(
                    400L, new BigDecimal("333.33"), BigDecimal.ZERO, 1);

            Settlement result = processor.process(aggregation);

            assertThat(result.getCommissionAmount()).isEqualByComparingTo("33.33");
            assertThat(result.getNetAmount()).isEqualByComparingTo("300.00");
        }

        @Test
        @DisplayName("should_calculateZeroCommission_when_zeroSales")
        void should_calculateZeroCommission_when_zeroSales() {
            SellerAggregation aggregation = new SellerAggregation(
                    500L, BigDecimal.ZERO, BigDecimal.ZERO, 0);

            Settlement result = processor.process(aggregation);

            assertThat(result.getCommissionAmount()).isEqualByComparingTo("0.00");
            assertThat(result.getNetAmount()).isEqualByComparingTo("0.00");
        }
    }
}
