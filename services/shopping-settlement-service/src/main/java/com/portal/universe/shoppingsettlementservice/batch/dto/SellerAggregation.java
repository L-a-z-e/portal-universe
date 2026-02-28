package com.portal.universe.shoppingsettlementservice.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class SellerAggregation {

    private final Long sellerId;
    private final BigDecimal totalSales;
    private final BigDecimal totalRefunds;
    private final int orderCount;
}
