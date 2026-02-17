package com.portal.universe.authservice.auth.dto.membership;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateMembershipTierRequest(
        @NotBlank @Size(max = 50) String membershipGroup,
        @NotBlank @Size(max = 50) String tierKey,
        @NotBlank @Size(max = 100) String displayName,
        BigDecimal priceMonthly,
        BigDecimal priceYearly,
        @NotNull Integer sortOrder
) {}
