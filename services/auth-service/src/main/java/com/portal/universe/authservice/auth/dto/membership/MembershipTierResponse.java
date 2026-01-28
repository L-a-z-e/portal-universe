package com.portal.universe.authservice.auth.dto.membership;

import com.portal.universe.authservice.auth.domain.MembershipTier;

import java.math.BigDecimal;

public record MembershipTierResponse(
        Long id,
        String serviceName,
        String tierKey,
        String displayName,
        BigDecimal priceMonthly,
        BigDecimal priceYearly,
        int sortOrder
) {
    public static MembershipTierResponse from(MembershipTier tier) {
        return new MembershipTierResponse(
                tier.getId(),
                tier.getServiceName(),
                tier.getTierKey(),
                tier.getDisplayName(),
                tier.getPriceMonthly(),
                tier.getPriceYearly(),
                tier.getSortOrder()
        );
    }
}
