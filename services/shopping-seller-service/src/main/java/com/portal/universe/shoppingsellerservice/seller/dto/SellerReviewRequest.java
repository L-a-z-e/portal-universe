package com.portal.universe.shoppingsellerservice.seller.dto;

import jakarta.validation.constraints.NotNull;

public record SellerReviewRequest(
        @NotNull Boolean approved,
        String reviewComment
) {}
