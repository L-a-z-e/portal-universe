package com.portal.universe.shoppingsellerservice.inventory.domain;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingsellerservice.common.exception.SellerErrorCode;
import com.portal.universe.shoppingsellerservice.support.fixture.InventoryFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Inventory")
class InventoryTest {

    @Nested
    @DisplayName("reserve")
    class Reserve {

        @Test
        @DisplayName("should decrease available and increase reserved")
        void should_reserve_stock() {
            Inventory inventory = InventoryFixture.builder().availableQuantity(100).build();

            inventory.reserve(30);

            assertThat(inventory.getAvailableQuantity()).isEqualTo(70);
            assertThat(inventory.getReservedQuantity()).isEqualTo(30);
        }

        @Test
        @DisplayName("should throw INSUFFICIENT_STOCK when available is less than requested")
        void should_throw_when_insufficient() {
            Inventory inventory = InventoryFixture.builder().availableQuantity(5).build();

            assertThatThrownBy(() -> inventory.reserve(10))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.INSUFFICIENT_STOCK));
        }

        @Test
        @DisplayName("should throw INVALID_STOCK_QUANTITY when quantity is zero or negative")
        void should_throw_when_invalid_quantity() {
            Inventory inventory = InventoryFixture.create();

            assertThatThrownBy(() -> inventory.reserve(0))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.INVALID_STOCK_QUANTITY));

            assertThatThrownBy(() -> inventory.reserve(-1))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.INVALID_STOCK_QUANTITY));
        }
    }

    @Nested
    @DisplayName("deduct")
    class Deduct {

        @Test
        @DisplayName("should decrease reserved and total")
        void should_deduct_stock() {
            Inventory inventory = InventoryFixture.builder()
                    .availableQuantity(70).reservedQuantity(30).totalQuantity(100).build();

            inventory.deduct(30);

            assertThat(inventory.getReservedQuantity()).isEqualTo(0);
            assertThat(inventory.getTotalQuantity()).isEqualTo(70);
            assertThat(inventory.getAvailableQuantity()).isEqualTo(70);
        }

        @Test
        @DisplayName("should throw STOCK_DEDUCTION_FAILED when reserved is less than requested")
        void should_throw_when_reserved_insufficient() {
            Inventory inventory = InventoryFixture.builder()
                    .availableQuantity(90).reservedQuantity(10).totalQuantity(100).build();

            assertThatThrownBy(() -> inventory.deduct(20))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.STOCK_DEDUCTION_FAILED));
        }

        @Test
        @DisplayName("should throw INVALID_STOCK_QUANTITY when quantity is zero or negative")
        void should_throw_when_invalid_quantity() {
            Inventory inventory = InventoryFixture.builder().reservedQuantity(10).build();

            assertThatThrownBy(() -> inventory.deduct(0))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.INVALID_STOCK_QUANTITY));
        }
    }

    @Nested
    @DisplayName("release")
    class Release {

        @Test
        @DisplayName("should decrease reserved and increase available")
        void should_release_stock() {
            Inventory inventory = InventoryFixture.builder()
                    .availableQuantity(70).reservedQuantity(30).totalQuantity(100).build();

            inventory.release(30);

            assertThat(inventory.getAvailableQuantity()).isEqualTo(100);
            assertThat(inventory.getReservedQuantity()).isEqualTo(0);
            assertThat(inventory.getTotalQuantity()).isEqualTo(100);
        }

        @Test
        @DisplayName("should throw STOCK_RELEASE_FAILED when reserved is less than requested")
        void should_throw_when_reserved_insufficient() {
            Inventory inventory = InventoryFixture.builder()
                    .availableQuantity(90).reservedQuantity(10).totalQuantity(100).build();

            assertThatThrownBy(() -> inventory.release(20))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.STOCK_RELEASE_FAILED));
        }
    }

    @Nested
    @DisplayName("addStock")
    class AddStock {

        @Test
        @DisplayName("should increase available and total")
        void should_add_stock() {
            Inventory inventory = InventoryFixture.builder().availableQuantity(100).build();

            inventory.addStock(50);

            assertThat(inventory.getAvailableQuantity()).isEqualTo(150);
            assertThat(inventory.getTotalQuantity()).isEqualTo(150);
        }

        @Test
        @DisplayName("should throw INVALID_STOCK_QUANTITY when quantity is zero or negative")
        void should_throw_when_invalid_quantity() {
            Inventory inventory = InventoryFixture.create();

            assertThatThrownBy(() -> inventory.addStock(0))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.INVALID_STOCK_QUANTITY));
        }
    }

    @Nested
    @DisplayName("restore")
    class Restore {

        @Test
        @DisplayName("should increase available and total (deduct reverse)")
        void should_restore_stock() {
            Inventory inventory = InventoryFixture.builder()
                    .availableQuantity(70).totalQuantity(70).build();

            inventory.restore(30);

            assertThat(inventory.getAvailableQuantity()).isEqualTo(100);
            assertThat(inventory.getTotalQuantity()).isEqualTo(100);
        }

        @Test
        @DisplayName("should throw INVALID_STOCK_QUANTITY when quantity is zero or negative")
        void should_throw_when_invalid_quantity() {
            Inventory inventory = InventoryFixture.create();

            assertThatThrownBy(() -> inventory.restore(-1))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.INVALID_STOCK_QUANTITY));
        }
    }

    @Nested
    @DisplayName("builder")
    class Builder {

        @Test
        @DisplayName("should initialize with correct values")
        void should_initialize_correctly() {
            Inventory inventory = Inventory.builder()
                    .productId(10L)
                    .initialQuantity(200)
                    .build();

            assertThat(inventory.getProductId()).isEqualTo(10L);
            assertThat(inventory.getAvailableQuantity()).isEqualTo(200);
            assertThat(inventory.getReservedQuantity()).isEqualTo(0);
            assertThat(inventory.getTotalQuantity()).isEqualTo(200);
        }

        @Test
        @DisplayName("should default to 0 when initialQuantity is null")
        void should_default_to_zero() {
            Inventory inventory = Inventory.builder()
                    .productId(10L)
                    .initialQuantity(null)
                    .build();

            assertThat(inventory.getAvailableQuantity()).isEqualTo(0);
            assertThat(inventory.getTotalQuantity()).isEqualTo(0);
        }
    }
}
