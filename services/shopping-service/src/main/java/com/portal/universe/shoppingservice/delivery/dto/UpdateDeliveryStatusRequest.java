package com.portal.universe.shoppingservice.delivery.dto;

import com.portal.universe.shoppingservice.delivery.domain.DeliveryStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 배송 상태 변경 요청 DTO입니다.
 */
public record UpdateDeliveryStatusRequest(
        @NotNull(message = "Status is required")
        DeliveryStatus status,

        @Size(max = 255, message = "Location must be at most 255 characters")
        String location,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description
) {
}
