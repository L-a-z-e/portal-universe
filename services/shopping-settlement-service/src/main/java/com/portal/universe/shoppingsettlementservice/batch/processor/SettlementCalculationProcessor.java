package com.portal.universe.shoppingsettlementservice.batch.processor;

import com.portal.universe.shoppingsettlementservice.batch.dto.SellerAggregation;
import com.portal.universe.shoppingsettlementservice.batch.exception.InvalidAmountException;
import com.portal.universe.shoppingsettlementservice.settlement.domain.Settlement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
public class SettlementCalculationProcessor implements ItemProcessor<SellerAggregation, Settlement> {

    private static final BigDecimal COMMISSION_RATE = new BigDecimal("10.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private Long periodId;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.periodId = stepExecution.getJobExecution()
                .getExecutionContext().getLong("periodId");
    }

    @Override
    public Settlement process(SellerAggregation item) {
        BigDecimal netSales = item.getTotalSales().subtract(item.getTotalRefunds());

        if (netSales.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAmountException(
                    "Negative net sales for seller " + item.getSellerId() + ": " + netSales);
        }

        BigDecimal commissionAmount = netSales.multiply(COMMISSION_RATE)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal netAmount = netSales.subtract(commissionAmount);

        log.debug("Seller {}: sales={}, refunds={}, net={}, commission={}, payout={}",
                item.getSellerId(), item.getTotalSales(), item.getTotalRefunds(),
                netSales, commissionAmount, netAmount);

        return Settlement.builder()
                .periodId(periodId)
                .sellerId(item.getSellerId())
                .totalSales(item.getTotalSales())
                .totalOrders(item.getOrderCount())
                .totalRefunds(item.getTotalRefunds())
                .commissionAmount(commissionAmount)
                .netAmount(netAmount)
                .build();
    }
}
