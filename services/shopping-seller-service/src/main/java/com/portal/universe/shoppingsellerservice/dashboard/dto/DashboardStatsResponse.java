package com.portal.universe.shoppingsellerservice.dashboard.dto;

public record DashboardStatsResponse(
        long productCount,
        long couponCount,
        long activeCouponCount,
        long timeDealCount,
        long activeTimeDealCount
) {}
