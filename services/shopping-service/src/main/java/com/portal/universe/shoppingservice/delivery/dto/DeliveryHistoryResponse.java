package com.portal.universe.shoppingservice.delivery.dto;

import com.portal.universe.shoppingservice.delivery.domain.DeliveryHistory;
import com.portal.universe.shoppingservice.delivery.domain.DeliveryStatus;

import java.time.LocalDateTime;

/**
 * 배송 이력 응답 DTO입니다.
 */
public record DeliveryHistoryResponse(
        Long id,
        DeliveryStatus status,
        String statusDescription,
        String location,
        String description,
        LocalDateTime createdAt
) {
    public static DeliveryHistoryResponse from(DeliveryHistory history) {
        return new DeliveryHistoryResponse(
                history.getId(),
                history.getStatus(),
                history.getStatus().getDescription(),
                history.getLocation(),
                history.getDescription(),
                history.getCreatedAt()
        );
    }
}
