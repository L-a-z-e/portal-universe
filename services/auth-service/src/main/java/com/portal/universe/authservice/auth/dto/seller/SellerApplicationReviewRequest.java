package com.portal.universe.authservice.auth.dto.seller;

import jakarta.validation.constraints.NotNull;

public record SellerApplicationReviewRequest(
        @NotNull(message = "승인 여부는 필수입니다")
        Boolean approved,

        String reviewComment
) {}
