package com.portal.universe.shoppingservice.feign.dto;

import java.util.Map;

public record StockReserveRequest(
        String orderNumber,
        Map<Long, Integer> items
) {}
