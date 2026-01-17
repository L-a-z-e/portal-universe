package com.portal.universe.shoppingservice.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 주문 취소 요청 DTO입니다.
 */
public record CancelOrderRequest(
        @NotBlank(message = "Cancel reason is required")
        @Size(max = 500, message = "Cancel reason must be at most 500 characters")
        String reason
) {
}
