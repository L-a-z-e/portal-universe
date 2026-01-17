package com.portal.universe.shoppingservice.delivery.dto;

import com.portal.universe.shoppingservice.delivery.domain.Delivery;
import com.portal.universe.shoppingservice.delivery.domain.DeliveryStatus;
import com.portal.universe.shoppingservice.order.dto.AddressResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 배송 조회 응답 DTO입니다.
 */
public record DeliveryResponse(
        Long id,
        String trackingNumber,
        String orderNumber,
        DeliveryStatus status,
        String statusDescription,
        String carrier,
        AddressResponse shippingAddress,
        LocalDate estimatedDeliveryDate,
        LocalDate actualDeliveryDate,
        List<DeliveryHistoryResponse> histories,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DeliveryResponse from(Delivery delivery) {
        List<DeliveryHistoryResponse> historyResponses = delivery.getHistories().stream()
                .map(DeliveryHistoryResponse::from)
                .toList();

        AddressResponse addressResponse = null;
        if (delivery.getShippingAddress() != null) {
            addressResponse = AddressResponse.from(delivery.getShippingAddress());
        }

        return new DeliveryResponse(
                delivery.getId(),
                delivery.getTrackingNumber(),
                delivery.getOrderNumber(),
                delivery.getStatus(),
                delivery.getStatus().getDescription(),
                delivery.getCarrier(),
                addressResponse,
                delivery.getEstimatedDeliveryDate(),
                delivery.getActualDeliveryDate(),
                historyResponses,
                delivery.getCreatedAt(),
                delivery.getUpdatedAt()
        );
    }
}
