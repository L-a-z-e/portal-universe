package com.portal.universe.shoppingsellerservice.inventory.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.event.seller.InventoryChangedEvent;
import com.portal.universe.shoppingsellerservice.common.exception.SellerErrorCode;
import com.portal.universe.shoppingsellerservice.inventory.domain.Inventory;
import com.portal.universe.shoppingsellerservice.inventory.domain.StockMovement;
import com.portal.universe.shoppingsellerservice.inventory.dto.InventoryResponse;
import com.portal.universe.shoppingsellerservice.inventory.dto.StockAddRequest;
import com.portal.universe.shoppingsellerservice.inventory.dto.StockMovementResponse;
import com.portal.universe.shoppingsellerservice.inventory.dto.StockReserveRequest;
import com.portal.universe.shoppingsellerservice.inventory.repository.InventoryRepository;
import com.portal.universe.shoppingsellerservice.inventory.repository.StockMovementRepository;
import com.portal.universe.shoppingsellerservice.support.fixture.InventoryFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryServiceImpl")
class InventoryServiceImplTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    @Nested
    @DisplayName("getInventory")
    class GetInventory {

        @Test
        @DisplayName("should return inventory when product exists")
        void should_return_inventory_when_product_exists() {
            // given
            Long productId = 1L;
            Inventory inventory = InventoryFixture.builder()
                    .id(1L)
                    .productId(productId)
                    .availableQuantity(100)
                    .build();
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

            // when
            InventoryResponse response = inventoryService.getInventory(productId);

            // then
            assertThat(response.productId()).isEqualTo(productId);
            assertThat(response.availableQuantity()).isEqualTo(100);
            assertThat(response.reservedQuantity()).isEqualTo(0);
        }

        @Test
        @DisplayName("should throw INVENTORY_NOT_FOUND when product does not exist")
        void should_throw_when_product_not_found() {
            // given
            Long productId = 999L;
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> inventoryService.getInventory(productId))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.INVENTORY_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getMovements")
    class GetMovements {

        @Test
        @DisplayName("should return movement page for product")
        void should_return_movement_page() {
            // given
            Long productId = 1L;
            Pageable pageable = PageRequest.of(0, 10);
            StockMovement movement = StockMovement.builder()
                    .inventoryId(1L)
                    .productId(productId)
                    .movementType(com.portal.universe.shoppingsellerservice.inventory.domain.MovementType.ADD)
                    .quantity(50)
                    .previousAvailable(100)
                    .afterAvailable(150)
                    .previousReserved(0)
                    .afterReserved(0)
                    .referenceType("MANUAL")
                    .reason("Test add")
                    .performedBy("admin")
                    .build();
            Page<StockMovement> movementPage = new PageImpl<>(List.of(movement));
            when(stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable))
                    .thenReturn(movementPage);

            // when
            Page<StockMovementResponse> result = inventoryService.getMovements(productId, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).productId()).isEqualTo(productId);
            assertThat(result.getContent().get(0).quantity()).isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("addStock")
    class AddStock {

        @Test
        @DisplayName("should add stock and record movement and publish event")
        void should_add_stock_successfully() {
            // given
            Long productId = 1L;
            Inventory inventory = InventoryFixture.builder()
                    .id(1L)
                    .productId(productId)
                    .availableQuantity(100)
                    .build();
            StockAddRequest request = new StockAddRequest(50, "Restock");
            when(inventoryRepository.findByProductIdForUpdate(productId)).thenReturn(Optional.of(inventory));

            // when
            InventoryResponse response = inventoryService.addStock(productId, request, "admin");

            // then
            assertThat(response.availableQuantity()).isEqualTo(150);
            assertThat(response.totalQuantity()).isEqualTo(150);
            verify(stockMovementRepository).save(any(StockMovement.class));
            verify(eventPublisher).publishEvent(any(InventoryChangedEvent.class));
        }

        @Test
        @DisplayName("should throw INVENTORY_NOT_FOUND when product does not exist")
        void should_throw_when_inventory_not_found() {
            // given
            Long productId = 999L;
            StockAddRequest request = new StockAddRequest(50, "Restock");
            when(inventoryRepository.findByProductIdForUpdate(productId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> inventoryService.addStock(productId, request, "admin"))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.INVENTORY_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("initializeInventory")
    class InitializeInventory {

        @Test
        @DisplayName("should create inventory and record movement and publish event")
        void should_initialize_inventory_successfully() {
            // given
            Long productId = 10L;
            Integer initialQuantity = 200;
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());
            when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            InventoryResponse response = inventoryService.initializeInventory(productId, initialQuantity);

            // then
            assertThat(response.productId()).isEqualTo(productId);
            assertThat(response.availableQuantity()).isEqualTo(200);
            assertThat(response.totalQuantity()).isEqualTo(200);
            assertThat(response.reservedQuantity()).isEqualTo(0);
            verify(inventoryRepository).save(any(Inventory.class));
            verify(stockMovementRepository).save(any(StockMovement.class));
            verify(eventPublisher).publishEvent(any(InventoryChangedEvent.class));
        }

        @Test
        @DisplayName("should throw INVENTORY_ALREADY_EXISTS when inventory exists")
        void should_throw_when_inventory_already_exists() {
            // given
            Long productId = 1L;
            Inventory existing = InventoryFixture.builder().productId(productId).build();
            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(existing));

            // when & then
            assertThatThrownBy(() -> inventoryService.initializeInventory(productId, 100))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.INVENTORY_ALREADY_EXISTS));
        }
    }

    @Nested
    @DisplayName("reserveStock")
    class ReserveStock {

        @Test
        @DisplayName("should reserve stock for multiple products")
        void should_reserve_stock_successfully() {
            // given
            Inventory inv1 = InventoryFixture.builder().id(1L).productId(1L).availableQuantity(100).build();
            Inventory inv2 = InventoryFixture.builder().id(2L).productId(2L).availableQuantity(50).build();
            StockReserveRequest request = new StockReserveRequest("ORD-001", Map.of(1L, 10, 2L, 5));

            when(inventoryRepository.findByProductIdsForUpdate(any())).thenReturn(List.of(inv1, inv2));

            // when
            inventoryService.reserveStock(request);

            // then
            assertThat(inv1.getAvailableQuantity()).isEqualTo(90);
            assertThat(inv1.getReservedQuantity()).isEqualTo(10);
            assertThat(inv2.getAvailableQuantity()).isEqualTo(45);
            assertThat(inv2.getReservedQuantity()).isEqualTo(5);
            verify(stockMovementRepository, org.mockito.Mockito.times(2)).save(any(StockMovement.class));
            verify(eventPublisher, org.mockito.Mockito.times(2)).publishEvent(any(InventoryChangedEvent.class));
        }

        @Test
        @DisplayName("should throw INVENTORY_NOT_FOUND when product not in inventory")
        void should_throw_when_product_not_found() {
            // given
            StockReserveRequest request = new StockReserveRequest("ORD-001", Map.of(999L, 10));
            when(inventoryRepository.findByProductIdsForUpdate(any())).thenReturn(List.of());

            // when & then
            assertThatThrownBy(() -> inventoryService.reserveStock(request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.INVENTORY_NOT_FOUND));
        }

        @Test
        @DisplayName("should throw INSUFFICIENT_STOCK when available quantity is not enough")
        void should_throw_when_insufficient_stock() {
            // given
            Inventory inventory = InventoryFixture.builder().id(1L).productId(1L).availableQuantity(5).build();
            StockReserveRequest request = new StockReserveRequest("ORD-001", Map.of(1L, 10));
            when(inventoryRepository.findByProductIdsForUpdate(any())).thenReturn(List.of(inventory));

            // when & then
            assertThatThrownBy(() -> inventoryService.reserveStock(request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.INSUFFICIENT_STOCK));
        }
    }

    @Nested
    @DisplayName("deductStock")
    class DeductStock {

        @Test
        @DisplayName("should deduct reserved stock after payment")
        void should_deduct_stock_successfully() {
            // given
            Inventory inventory = InventoryFixture.builder()
                    .id(1L).productId(1L).availableQuantity(90).reservedQuantity(10).totalQuantity(100)
                    .build();
            StockReserveRequest request = new StockReserveRequest("ORD-001", Map.of(1L, 10));
            when(inventoryRepository.findByProductIdsForUpdate(any())).thenReturn(List.of(inventory));

            // when
            inventoryService.deductStock(request);

            // then
            assertThat(inventory.getReservedQuantity()).isEqualTo(0);
            assertThat(inventory.getTotalQuantity()).isEqualTo(90);
            assertThat(inventory.getAvailableQuantity()).isEqualTo(90);
            verify(stockMovementRepository).save(any(StockMovement.class));
            verify(eventPublisher).publishEvent(any(InventoryChangedEvent.class));
        }

        @Test
        @DisplayName("should throw INVENTORY_NOT_FOUND when product not in inventory")
        void should_throw_when_product_not_found() {
            // given
            StockReserveRequest request = new StockReserveRequest("ORD-001", Map.of(999L, 10));
            when(inventoryRepository.findByProductIdsForUpdate(any())).thenReturn(List.of());

            // when & then
            assertThatThrownBy(() -> inventoryService.deductStock(request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.INVENTORY_NOT_FOUND));
        }

        @Test
        @DisplayName("should throw STOCK_DEDUCTION_FAILED when reserved quantity is not enough")
        void should_throw_when_reserved_not_enough() {
            // given
            Inventory inventory = InventoryFixture.builder()
                    .id(1L).productId(1L).availableQuantity(95).reservedQuantity(5).totalQuantity(100)
                    .build();
            StockReserveRequest request = new StockReserveRequest("ORD-001", Map.of(1L, 10));
            when(inventoryRepository.findByProductIdsForUpdate(any())).thenReturn(List.of(inventory));

            // when & then
            assertThatThrownBy(() -> inventoryService.deductStock(request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.STOCK_DEDUCTION_FAILED));
        }
    }

    @Nested
    @DisplayName("releaseStock")
    class ReleaseStock {

        @Test
        @DisplayName("should release reserved stock back to available")
        void should_release_stock_successfully() {
            // given
            Inventory inventory = InventoryFixture.builder()
                    .id(1L).productId(1L).availableQuantity(90).reservedQuantity(10).totalQuantity(100)
                    .build();
            StockReserveRequest request = new StockReserveRequest("ORD-001", Map.of(1L, 10));
            when(inventoryRepository.findByProductIdsForUpdate(any())).thenReturn(List.of(inventory));

            // when
            inventoryService.releaseStock(request);

            // then
            assertThat(inventory.getAvailableQuantity()).isEqualTo(100);
            assertThat(inventory.getReservedQuantity()).isEqualTo(0);
            assertThat(inventory.getTotalQuantity()).isEqualTo(100);
            verify(stockMovementRepository).save(any(StockMovement.class));
            verify(eventPublisher).publishEvent(any(InventoryChangedEvent.class));
        }

        @Test
        @DisplayName("should throw INVENTORY_NOT_FOUND when product not in inventory")
        void should_throw_when_product_not_found() {
            // given
            StockReserveRequest request = new StockReserveRequest("ORD-001", Map.of(999L, 5));
            when(inventoryRepository.findByProductIdsForUpdate(any())).thenReturn(List.of());

            // when & then
            assertThatThrownBy(() -> inventoryService.releaseStock(request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.INVENTORY_NOT_FOUND));
        }

        @Test
        @DisplayName("should throw STOCK_RELEASE_FAILED when reserved quantity is not enough")
        void should_throw_when_reserved_not_enough() {
            // given
            Inventory inventory = InventoryFixture.builder()
                    .id(1L).productId(1L).availableQuantity(95).reservedQuantity(5).totalQuantity(100)
                    .build();
            StockReserveRequest request = new StockReserveRequest("ORD-001", Map.of(1L, 10));
            when(inventoryRepository.findByProductIdsForUpdate(any())).thenReturn(List.of(inventory));

            // when & then
            assertThatThrownBy(() -> inventoryService.releaseStock(request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.STOCK_RELEASE_FAILED));
        }
    }

    @Nested
    @DisplayName("restoreStock")
    class RestoreStock {

        @Test
        @DisplayName("should restore deducted stock (saga compensation)")
        void should_restore_stock_successfully() {
            // given
            Inventory inventory = InventoryFixture.builder()
                    .id(1L).productId(1L).availableQuantity(90).totalQuantity(90)
                    .build();
            StockReserveRequest request = new StockReserveRequest("ORD-001", Map.of(1L, 10));
            when(inventoryRepository.findByProductIdsForUpdate(any())).thenReturn(List.of(inventory));

            // when
            inventoryService.restoreStock(request);

            // then
            assertThat(inventory.getAvailableQuantity()).isEqualTo(100);
            assertThat(inventory.getTotalQuantity()).isEqualTo(100);
            verify(stockMovementRepository).save(any(StockMovement.class));
            verify(eventPublisher).publishEvent(any(InventoryChangedEvent.class));
        }

        @Test
        @DisplayName("should throw INVENTORY_NOT_FOUND when product not in inventory")
        void should_throw_when_product_not_found() {
            // given
            StockReserveRequest request = new StockReserveRequest("ORD-001", Map.of(999L, 10));
            when(inventoryRepository.findByProductIdsForUpdate(any())).thenReturn(List.of());

            // when & then
            assertThatThrownBy(() -> inventoryService.restoreStock(request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.INVENTORY_NOT_FOUND));
        }
    }
}
