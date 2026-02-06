package com.portal.universe.shoppingservice.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.inventory.domain.Inventory;
import com.portal.universe.shoppingservice.inventory.domain.StockMovement;
import com.portal.universe.shoppingservice.inventory.dto.InventoryResponse;
import com.portal.universe.shoppingservice.inventory.dto.StockMovementResponse;
import com.portal.universe.shoppingservice.inventory.repository.InventoryRepository;
import com.portal.universe.shoppingservice.inventory.repository.StockMovementRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

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

    @Nested
    @DisplayName("initializeInventory")
    class InitializeInventory {

        @Test
        @DisplayName("should_initializeInventory_when_valid")
        void should_initializeInventory_when_valid() {
            // given
            when(inventoryRepository.existsByProductId(100L)).thenReturn(false);
            Inventory inventory = createInventory(1L, 100L, 50, 0);
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
            when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(mock(StockMovement.class));

            // when
            InventoryResponse result = inventoryService.initializeInventory(100L, 50, "admin1");

            // then
            assertThat(result).isNotNull();
            verify(inventoryRepository).save(any(Inventory.class));
            verify(stockMovementRepository).save(any(StockMovement.class));
        }

        @Test
        @DisplayName("should_throwException_when_alreadyExists")
        void should_throwException_when_alreadyExists() {
            // given
            when(inventoryRepository.existsByProductId(100L)).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> inventoryService.initializeInventory(100L, 50, "admin1"))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("reserveStock")
    class ReserveStock {

        @Test
        @DisplayName("should_reserveStock_when_sufficient")
        void should_reserveStock_when_sufficient() throws JsonProcessingException {
            // given
            Inventory inventory = createInventory(1L, 100L, 50, 0);
            when(inventoryRepository.findByProductIdWithLock(100L)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
            when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(mock(StockMovement.class));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // when
            InventoryResponse result = inventoryService.reserveStock(100L, 10, "ORDER", "ORD-001", "user1");

            // then
            assertThat(result).isNotNull();
            verify(inventoryRepository).save(any(Inventory.class));
        }

        @Test
        @DisplayName("should_recordMovement_when_reserveStock")
        void should_recordMovement_when_reserveStock() throws JsonProcessingException {
            // given
            Inventory inventory = createInventory(1L, 100L, 50, 0);
            when(inventoryRepository.findByProductIdWithLock(100L)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
            when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(mock(StockMovement.class));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // when
            inventoryService.reserveStock(100L, 10, "ORDER", "ORD-001", "user1");

            // then
            verify(stockMovementRepository).save(any(StockMovement.class));
        }
    }

    @Nested
    @DisplayName("reserveStockBatch")
    class ReserveStockBatch {

        @Test
        @DisplayName("should_reserveStockBatch_when_valid")
        void should_reserveStockBatch_when_valid() throws JsonProcessingException {
            // given
            Inventory inv1 = createInventory(1L, 100L, 50, 0);
            Inventory inv2 = createInventory(2L, 200L, 30, 0);
            when(inventoryRepository.findByProductIdsWithLock(any())).thenReturn(List.of(inv1, inv2));
            when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(mock(StockMovement.class));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            Map<Long, Integer> quantities = Map.of(100L, 5, 200L, 3);

            // when
            List<InventoryResponse> result = inventoryService.reserveStockBatch(quantities, "ORDER", "ORD-001", "user1");

            // then
            assertThat(result).hasSize(2);
            verify(inventoryRepository).saveAll(any());
        }
    }

    @Nested
    @DisplayName("deductStock")
    class DeductStock {

        @Test
        @DisplayName("should_deductStock_when_valid")
        void should_deductStock_when_valid() throws JsonProcessingException {
            // given
            Inventory inventory = createInventory(1L, 100L, 40, 10);
            when(inventoryRepository.findByProductIdWithLock(100L)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
            when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(mock(StockMovement.class));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // when
            InventoryResponse result = inventoryService.deductStock(100L, 5, "ORDER", "ORD-001", "user1");

            // then
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("deductStockBatch")
    class DeductStockBatch {

        @Test
        @DisplayName("should_deductStockBatch_when_valid")
        void should_deductStockBatch_when_valid() throws JsonProcessingException {
            // given
            Inventory inv1 = createInventory(1L, 100L, 40, 10);
            Inventory inv2 = createInventory(2L, 200L, 25, 5);
            when(inventoryRepository.findByProductIdsWithLock(any())).thenReturn(List.of(inv1, inv2));
            when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(mock(StockMovement.class));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            Map<Long, Integer> quantities = Map.of(100L, 5, 200L, 3);

            // when
            List<InventoryResponse> result = inventoryService.deductStockBatch(quantities, "ORDER", "ORD-001", "user1");

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("releaseStock")
    class ReleaseStock {

        @Test
        @DisplayName("should_releaseStock_when_valid")
        void should_releaseStock_when_valid() throws JsonProcessingException {
            // given
            Inventory inventory = createInventory(1L, 100L, 40, 10);
            when(inventoryRepository.findByProductIdWithLock(100L)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
            when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(mock(StockMovement.class));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // when
            InventoryResponse result = inventoryService.releaseStock(100L, 5, "ORDER_CANCEL", "ORD-001", "user1");

            // then
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("releaseStockBatch")
    class ReleaseStockBatch {

        @Test
        @DisplayName("should_releaseStockBatch_when_valid")
        void should_releaseStockBatch_when_valid() throws JsonProcessingException {
            // given
            Inventory inv1 = createInventory(1L, 100L, 40, 10);
            Inventory inv2 = createInventory(2L, 200L, 25, 5);
            when(inventoryRepository.findByProductIdsWithLock(any())).thenReturn(List.of(inv1, inv2));
            when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(mock(StockMovement.class));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            Map<Long, Integer> quantities = Map.of(100L, 5, 200L, 3);

            // when
            List<InventoryResponse> result = inventoryService.releaseStockBatch(quantities, "ORDER_CANCEL", "ORD-001", "user1");

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("addStock")
    class AddStock {

        @Test
        @DisplayName("should_addStock_when_valid")
        void should_addStock_when_valid() throws JsonProcessingException {
            // given
            Inventory inventory = createInventory(1L, 100L, 50, 0);
            when(inventoryRepository.findByProductIdWithLock(100L)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
            when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(mock(StockMovement.class));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // when
            InventoryResponse result = inventoryService.addStock(100L, 20, "Restocking", "admin1");

            // then
            assertThat(result).isNotNull();
            verify(inventoryRepository).save(any(Inventory.class));
        }

        @Test
        @DisplayName("should_recordMovement_when_addStock")
        void should_recordMovement_when_addStock() throws JsonProcessingException {
            // given
            Inventory inventory = createInventory(1L, 100L, 50, 0);
            when(inventoryRepository.findByProductIdWithLock(100L)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
            when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(mock(StockMovement.class));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // when
            inventoryService.addStock(100L, 20, "Restocking", "admin1");

            // then
            verify(stockMovementRepository).save(any(StockMovement.class));
        }
    }

    @Nested
    @DisplayName("getStockMovements")
    class GetStockMovements {

        @Test
        @DisplayName("should_returnMovements_when_called")
        void should_returnMovements_when_called() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<StockMovement> movementPage = new PageImpl<>(List.of(), pageable, 0);
            when(stockMovementRepository.findByProductIdOrderByCreatedAtDesc(100L, pageable))
                    .thenReturn(movementPage);

            // when
            Page<StockMovementResponse> result = inventoryService.getStockMovements(100L, pageable);

            // then
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("publishToRedis")
    class PublishToRedis {

        @Test
        @DisplayName("should_publishUpdate_when_inventoryChanged")
        void should_publishUpdate_when_inventoryChanged() throws JsonProcessingException {
            // given
            Inventory inventory = createInventory(1L, 100L, 50, 0);
            when(inventoryRepository.findByProductIdWithLock(100L)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
            when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(mock(StockMovement.class));
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"productId\":100}");

            // when
            inventoryService.reserveStock(100L, 5, "ORDER", "ORD-001", "user1");

            // then
            verify(redisTemplate).convertAndSend(eq("inventory:100"), anyString());
        }
    }
}
