package com.portal.universe.shoppingservice.timedeal.dto;

import com.portal.universe.shoppingservice.timedeal.domain.TimeDeal;
import com.portal.universe.shoppingservice.timedeal.domain.TimeDealProduct;
import com.portal.universe.shoppingservice.timedeal.domain.TimeDealStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record TimeDealResponse(
        Long id,
        String name,
        String description,
        TimeDealStatus status,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        List<TimeDealProductResponse> products,
        LocalDateTime createdAt
) {
    @Builder
    public record TimeDealProductResponse(
            Long id,
            Long productId,
            String productName,
            BigDecimal originalPrice,
            BigDecimal dealPrice,
            BigDecimal discountRate,
            Integer dealQuantity,
            Integer soldQuantity,
            Integer remainingQuantity,
            Integer maxPerUser,
            boolean available
    ) {
        public static TimeDealProductResponse from(TimeDealProduct tdp) {
            return TimeDealProductResponse.builder()
                    .id(tdp.getId())
                    .productId(tdp.getProduct().getId())
                    .productName(tdp.getProduct().getName())
                    .originalPrice(tdp.getProduct().getPrice())
                    .dealPrice(tdp.getDealPrice())
                    .discountRate(tdp.getDiscountRate())
                    .dealQuantity(tdp.getDealQuantity())
                    .soldQuantity(tdp.getSoldQuantity())
                    .remainingQuantity(tdp.getRemainingQuantity())
                    .maxPerUser(tdp.getMaxPerUser())
                    .available(tdp.isAvailable())
                    .build();
        }
    }

    public static TimeDealResponse from(TimeDeal timeDeal) {
        return TimeDealResponse.builder()
                .id(timeDeal.getId())
                .name(timeDeal.getName())
                .description(timeDeal.getDescription())
                .status(timeDeal.getStatus())
                .startsAt(timeDeal.getStartsAt())
                .endsAt(timeDeal.getEndsAt())
                .products(timeDeal.getProducts().stream()
                        .map(TimeDealProductResponse::from)
                        .toList())
                .createdAt(timeDeal.getCreatedAt())
                .build();
    }
}
