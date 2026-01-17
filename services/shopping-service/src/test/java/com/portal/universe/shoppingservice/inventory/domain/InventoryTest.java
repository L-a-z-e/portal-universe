package com.portal.universe.shoppingservice.inventory.domain;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Inventory 엔티티 단위 테스트입니다.
 */
class InventoryTest {

    @Test
    @DisplayName("재고 초기화 시 가용 재고와 전체 재고가 동일하게 설정된다")
    void initializeInventory() {
        // given
        int initialStock = 100;

        // when
        Inventory inventory = Inventory.builder()
                .productId(1L)
                .initialQuantity(initialStock)
                .build();

        // then
        assertThat(inventory.getProductId()).isEqualTo(1L);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(initialStock);
        assertThat(inventory.getReservedQuantity()).isEqualTo(0);
        assertThat(inventory.getTotalQuantity()).isEqualTo(initialStock);
    }

    @Test
    @DisplayName("재고 예약 시 가용 재고가 감소하고 예약 재고가 증가한다")
    void reserveStock() {
        // given
        Inventory inventory = createInventoryWithStock(100);

        // when
        inventory.reserve(30);

        // then
        assertThat(inventory.getAvailableQuantity()).isEqualTo(70);
        assertThat(inventory.getReservedQuantity()).isEqualTo(30);
        assertThat(inventory.getTotalQuantity()).isEqualTo(100);
    }

    @Test
    @DisplayName("가용 재고보다 많은 수량 예약 시 예외가 발생한다")
    void reserveStockExceedingAvailable() {
        // given
        Inventory inventory = createInventoryWithStock(10);

        // when & then
        assertThatThrownBy(() -> inventory.reserve(15))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ShoppingErrorCode.INSUFFICIENT_STOCK);
    }

    @Test
    @DisplayName("재고 차감 시 예약 재고와 전체 재고가 감소한다")
    void deductStock() {
        // given
        Inventory inventory = createInventoryWithStock(100);
        inventory.reserve(30);

        // when
        inventory.deduct(30);

        // then
        assertThat(inventory.getAvailableQuantity()).isEqualTo(70);
        assertThat(inventory.getReservedQuantity()).isEqualTo(0);
        assertThat(inventory.getTotalQuantity()).isEqualTo(70);
    }

    @Test
    @DisplayName("예약된 재고보다 많은 수량 차감 시 예외가 발생한다")
    void deductStockExceedingReserved() {
        // given
        Inventory inventory = createInventoryWithStock(100);
        inventory.reserve(10);

        // when & then
        assertThatThrownBy(() -> inventory.deduct(20))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ShoppingErrorCode.STOCK_DEDUCTION_FAILED);
    }

    @Test
    @DisplayName("재고 해제 시 예약 재고가 감소하고 가용 재고가 증가한다")
    void releaseStock() {
        // given
        Inventory inventory = createInventoryWithStock(100);
        inventory.reserve(30);

        // when
        inventory.release(20);

        // then
        assertThat(inventory.getAvailableQuantity()).isEqualTo(90);
        assertThat(inventory.getReservedQuantity()).isEqualTo(10);
        assertThat(inventory.getTotalQuantity()).isEqualTo(100);
    }

    @Test
    @DisplayName("재고 추가 시 가용 재고와 전체 재고가 증가한다")
    void addStock() {
        // given
        Inventory inventory = createInventoryWithStock(100);

        // when
        inventory.addStock(50);

        // then
        assertThat(inventory.getAvailableQuantity()).isEqualTo(150);
        assertThat(inventory.getReservedQuantity()).isEqualTo(0);
        assertThat(inventory.getTotalQuantity()).isEqualTo(150);
    }

    @Test
    @DisplayName("0 이하의 수량으로 예약 시 예외가 발생한다")
    void reserveWithInvalidQuantity() {
        // given
        Inventory inventory = createInventoryWithStock(100);

        // when & then
        assertThatThrownBy(() -> inventory.reserve(0))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ShoppingErrorCode.INVALID_STOCK_QUANTITY);

        assertThatThrownBy(() -> inventory.reserve(-5))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ShoppingErrorCode.INVALID_STOCK_QUANTITY);
    }

    private Inventory createInventoryWithStock(int stock) {
        return Inventory.builder()
                .productId(1L)
                .initialQuantity(stock)
                .build();
    }
}
