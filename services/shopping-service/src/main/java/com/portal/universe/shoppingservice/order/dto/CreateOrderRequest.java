package com.portal.universe.shoppingservice.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * 주문 생성 요청 DTO입니다.
 */
public record CreateOrderRequest(
        @NotNull(message = "Shipping address is required")
        @Valid
        AddressRequest shippingAddress
) {
}
