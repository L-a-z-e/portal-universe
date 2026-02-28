package com.portal.universe.authservice.auth.dto.seller;

import com.portal.universe.authservice.auth.domain.SellerApplication;
import com.portal.universe.authservice.auth.domain.SellerApplicationStatus;

import java.time.Instant;

public record SellerApplicationResponse(
        Long id,
        String userId,
        String businessName,
        String businessNumber,
        String reason,
        SellerApplicationStatus status,
        String reviewedBy,
        String reviewComment,
        Instant reviewedAt,
        Instant createdAt
) {
    public static SellerApplicationResponse from(SellerApplication app) {
        return new SellerApplicationResponse(
                app.getId(),
                app.getUserId(),
                app.getBusinessName(),
                app.getBusinessNumber(),
                app.getReason(),
                app.getStatus(),
                app.getReviewedBy(),
                app.getReviewComment(),
                app.getReviewedAt(),
                app.getCreatedAt()
        );
    }
}
