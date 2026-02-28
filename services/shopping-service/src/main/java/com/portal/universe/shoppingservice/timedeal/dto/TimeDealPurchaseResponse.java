package com.portal.universe.shoppingservice.timedeal.dto;

import com.portal.universe.shoppingservice.timedeal.domain.TimeDealPurchase;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record TimeDealPurchaseResponse(
        Long id,
        Long timeDealProductId,
        Long productId,
        Integer quantity,
        BigDecimal purchasePrice,
        BigDecimal totalPrice,
        Instant purchasedAt
) {
    public static TimeDealPurchaseResponse from(TimeDealPurchase purchase) {
        return TimeDealPurchaseResponse.builder()
                .id(purchase.getId())
                .timeDealProductId(purchase.getTimeDealProduct().getId())
                .productId(purchase.getTimeDealProduct().getProductId())
                .quantity(purchase.getQuantity())
                .purchasePrice(purchase.getPurchasePrice())
                .totalPrice(purchase.getPurchasePrice().multiply(BigDecimal.valueOf(purchase.getQuantity())))
                .purchasedAt(purchase.getPurchasedAt())
                .build();
    }
}
