package com.portal.universe.shoppingsellerservice.seller.dto;

import com.portal.universe.shoppingsellerservice.seller.domain.Seller;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SellerResponse(
        Long id,
        Long userId,
        String businessName,
        String businessNumber,
        String representativeName,
        String phone,
        String email,
        String bankName,
        String bankAccount,
        BigDecimal commissionRate,
        String status,
        LocalDateTime createdAt
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
                seller.getCreatedAt()
        );
    }
}
