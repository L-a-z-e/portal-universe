package com.portal.universe.shoppingsellerservice.seller.dto;

import com.portal.universe.shoppingsellerservice.seller.domain.Seller;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SellerRegisterRequest(
        @NotBlank @Size(max = 100) String businessName,
        @Size(max = 20) String businessNumber,
        @Size(max = 50) String representativeName,
        @Size(max = 20) String phone,
        @Email @Size(max = 100) String email,
        @Size(max = 50) String bankName,
        @Size(max = 30) String bankAccount
) {
    public Seller toEntity(Long userId) {
        return Seller.builder()
                .userId(userId)
                .businessName(businessName)
                .businessNumber(businessNumber)
                .representativeName(representativeName)
                .phone(phone)
                .email(email)
                .bankName(bankName)
                .bankAccount(bankAccount)
                .commissionRate(new BigDecimal("10.00"))
                .build();
    }
}
