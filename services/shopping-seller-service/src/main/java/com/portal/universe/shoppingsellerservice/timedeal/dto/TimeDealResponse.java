package com.portal.universe.shoppingsellerservice.timedeal.dto;

import com.portal.universe.shoppingsellerservice.timedeal.domain.TimeDeal;
import com.portal.universe.shoppingsellerservice.timedeal.domain.TimeDealProduct;
import com.portal.universe.shoppingsellerservice.timedeal.domain.TimeDealStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TimeDealResponse(
        Long id,
        Long sellerId,
        String name,
        String description,
        TimeDealStatus status,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        List<TimeDealProductResponse> products,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TimeDealResponse from(TimeDeal timeDeal) {
        return new TimeDealResponse(
                timeDeal.getId(),
                timeDeal.getSellerId(),
                timeDeal.getName(),
                timeDeal.getDescription(),
                timeDeal.getStatus(),
                timeDeal.getStartsAt(),
                timeDeal.getEndsAt(),
                timeDeal.getProducts().stream()
                        .map(TimeDealProductResponse::from)
                        .toList(),
                timeDeal.getCreatedAt(),
                timeDeal.getUpdatedAt()
        );
    }

    public record TimeDealProductResponse(
            Long id,
            Long productId,
            BigDecimal dealPrice,
            Integer dealQuantity,
            Integer soldQuantity,
            Integer maxPerUser
    ) {
        public static TimeDealProductResponse from(TimeDealProduct product) {
            return new TimeDealProductResponse(
                    product.getId(),
                    product.getProductId(),
                    product.getDealPrice(),
                    product.getDealQuantity(),
                    product.getSoldQuantity(),
                    product.getMaxPerUser()
            );
        }
    }
}
