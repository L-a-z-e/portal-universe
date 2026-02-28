package com.portal.universe.shoppingsellerservice.seller.dto;

import com.portal.universe.shoppingsellerservice.seller.domain.Seller;

import java.math.BigDecimal;
import java.time.Instant;

public record SellerResponse(
        Long id,
        String userId,
        String businessName,
        String businessNumber,
        String representativeName,
        String phone,
        String email,
        String bankName,
        String bankAccount,
        BigDecimal commissionRate,
        String status,
        String reason,
        String reviewedBy,
        String reviewComment,
        Instant reviewedAt,
        Instant createdAt
) {
    public static SellerResponse from(Seller seller) {
        return new SellerResponse(
                seller.getId(),
                seller.getUserId(),
                seller.getBusinessName(),
                seller.getBusinessNumber(),
                seller.getRepresentativeName(),
                seller.getPhone(),
                seller.getEmail(),
                seller.getBankName(),
                seller.getBankAccount(),
                seller.getCommissionRate(),
                seller.getStatus().name(),
                seller.getReason(),
                seller.getReviewedBy(),
                seller.getReviewComment(),
                seller.getReviewedAt(),
                seller.getCreatedAt()
        );
    }
}
