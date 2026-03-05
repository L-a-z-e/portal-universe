package com.portal.universe.shoppingservice.inventory.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.inventory.domain.Inventory;
import com.portal.universe.shoppingservice.inventory.dto.InventoryResponse;
import com.portal.universe.shoppingservice.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private Inventory createInventory(Long id, Long productId, int available, int reserved) {
        Inventory inventory = Inventory.builder()
                .productId(productId)
                .initialQuantity(available)
                .build();
        ReflectionTestUtils.setField(inventory, "id", id);
        ReflectionTestUtils.setField(inventory, "availableQuantity", available);
        ReflectionTestUtils.setField(inventory, "reservedQuantity", reserved);
        ReflectionTestUtils.setField(inventory, "totalQuantity", available + reserved);
        return inventory;
    }

    @Nested
    @DisplayName("getInventory")
    class GetInventory {

        @Test
        @DisplayName("should_returnInventory_when_found")
        void should_returnInventory_when_found() {
            // given
            Inventory inventory = createInventory(1L, 100L, 50, 10);
            when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(inventory));

            // when
            InventoryResponse result = inventoryService.getInventory(100L);

            // then
            assertThat(result).isNotNull();
            verify(inventoryRepository).findByProductId(100L);
        }

        @Test
        @DisplayName("should_throwException_when_notFound")
        void should_throwException_when_notFound() {
            // given
            when(inventoryRepository.findByProductId(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> inventoryService.getInventory(999L))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("getInventories")
    class GetInventories {

        @Test
        @DisplayName("should_returnInventories_when_called")
        void should_returnInventories_when_called() {
            // given
            Inventory inv1 = createInventory(1L, 100L, 50, 10);
            Inventory inv2 = createInventory(2L, 200L, 30, 5);
            when(inventoryRepository.findByProductIds(List.of(100L, 200L))).thenReturn(List.of(inv1, inv2));

            // when
            List<InventoryResponse> result = inventoryService.getInventories(List.of(100L, 200L));

            // then
            assertThat(result).hasSize(2);
        }
    }
}
