package com.portal.universe.authservice.auth.dto.membership;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateMembershipTierRequest(
        @NotBlank @Size(max = 100) String displayName,
        BigDecimal priceMonthly,
        BigDecimal priceYearly,
        @NotNull Integer sortOrder
) {}
