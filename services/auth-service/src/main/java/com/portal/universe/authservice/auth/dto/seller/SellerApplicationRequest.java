package com.portal.universe.authservice.auth.dto.seller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SellerApplicationRequest(
        @NotBlank(message = "사업자명은 필수입니다")
        @Size(max = 200, message = "사업자명은 200자 이내여야 합니다")
        String businessName,

        @Size(max = 50, message = "사업자 등록번호는 50자 이내여야 합니다")
        String businessNumber,

        String reason
) {}
