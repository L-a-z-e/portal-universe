package com.portal.universe.shoppingsellerservice.seller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SellerUpdateRequest(
        @NotBlank @Size(max = 100) String businessName,
        @Size(max = 20) String phone,
        @Email @Size(max = 100) String email,
        @Size(max = 50) String bankName,
        @Size(max = 30) String bankAccount
) {}
