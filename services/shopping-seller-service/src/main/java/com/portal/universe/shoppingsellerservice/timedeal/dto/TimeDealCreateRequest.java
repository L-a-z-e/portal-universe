package com.portal.universe.shoppingsellerservice.timedeal.dto;

import com.portal.universe.shoppingsellerservice.timedeal.domain.TimeDeal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TimeDealCreateRequest(
        @NotBlank @Size(max = 100) String name,
        String description,
        @NotNull LocalDateTime startsAt,
        @NotNull LocalDateTime endsAt,
        @NotEmpty @Valid List<TimeDealProductItem> products
) {
    public TimeDeal toEntity(Long sellerId) {
        return TimeDeal.builder()
                .sellerId(sellerId)
                .name(name)
                .description(description)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .build();
    }

    public record TimeDealProductItem(
            @NotNull Long productId,
            @NotNull @DecimalMin("0.01") BigDecimal dealPrice,
            @NotNull @Min(1) Integer dealQuantity,
            @NotNull @Min(1) Integer maxPerUser
    ) {}
}
