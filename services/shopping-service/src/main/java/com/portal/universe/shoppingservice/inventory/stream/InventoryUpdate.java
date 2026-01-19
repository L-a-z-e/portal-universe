package com.portal.universe.shoppingservice.inventory.stream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryUpdate {
    private Long productId;
    private Integer available;
    private Integer reserved;
    private LocalDateTime timestamp;
}
