package com.portal.universe.shoppingservice.inventory.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.IntegrationTest;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import com.portal.universe.shoppingservice.inventory.dto.InventoryResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * InventoryService 통합 테스트입니다.
 * Testcontainers를 사용하여 실제 MySQL 환경에서 테스트합니다.
 */
@Disabled("통합 테스트 - Docker/Testcontainers 환경 필요")
class InventoryServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Test
    @DisplayName("재고를 초기화하고 조회할 수 있다")
    void initializeAndGetInventory() {
        // given
        Long productId = 1001L;
        int initialStock = 100;

        // when
        InventoryResponse response = inventoryService.initializeInventory(productId, initialStock, "test-admin");

        // then
        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.availableQuantity()).isEqualTo(initialStock);
        assertThat(response.reservedQuantity()).isEqualTo(0);
        assertThat(response.totalQuantity()).isEqualTo(initialStock);

        // 조회 확인
        InventoryResponse fetched = inventoryService.getInventory(productId);
        assertThat(fetched.availableQuantity()).isEqualTo(initialStock);
    }

    @Test
    @DisplayName("이미 존재하는 상품의 재고를 초기화하면 예외가 발생한다")
    void initializeDuplicateInventoryThrowsException() {
        // given
        Long productId = 1002L;
        inventoryService.initializeInventory(productId, 50, "test-admin");

        // when & then
        assertThatThrownBy(() -> inventoryService.initializeInventory(productId, 100, "test-admin"))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ShoppingErrorCode.INVENTORY_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("재고를 예약하면 가용 재고가 감소하고 예약 재고가 증가한다")
    void reserveStockUpdatesQuantities() {
        // given
        Long productId = 1003L;
        inventoryService.initializeInventory(productId, 100, "test-admin");

        // when
        InventoryResponse response = inventoryService.reserveStock(
                productId, 30, "ORDER", "ORD-001", "test-user");

        // then
        assertThat(response.availableQuantity()).isEqualTo(70);
        assertThat(response.reservedQuantity()).isEqualTo(30);
        assertThat(response.totalQuantity()).isEqualTo(100);
    }

    @Test
    @DisplayName("가용 재고보다 많은 수량을 예약하면 예외가 발생한다")
    void reserveExceedingStockThrowsException() {
        // given
        Long productId = 1004L;
        inventoryService.initializeInventory(productId, 10, "test-admin");

        // when & then
        assertThatThrownBy(() -> inventoryService.reserveStock(
                productId, 15, "ORDER", "ORD-002", "test-user"))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ShoppingErrorCode.INSUFFICIENT_STOCK);
    }

    @Test
    @DisplayName("예약된 재고를 차감하면 예약 재고와 전체 재고가 감소한다")
    void deductStockUpdatesQuantities() {
        // given
        Long productId = 1005L;
        inventoryService.initializeInventory(productId, 100, "test-admin");
        inventoryService.reserveStock(productId, 30, "ORDER", "ORD-003", "test-user");

        // when
        InventoryResponse response = inventoryService.deductStock(
                productId, 30, "PAYMENT", "PAY-001", "test-user");

        // then
        assertThat(response.availableQuantity()).isEqualTo(70);
        assertThat(response.reservedQuantity()).isEqualTo(0);
        assertThat(response.totalQuantity()).isEqualTo(70);
    }

    @Test
    @DisplayName("예약된 재고를 해제하면 가용 재고가 증가하고 예약 재고가 감소한다")
    void releaseStockUpdatesQuantities() {
        // given
        Long productId = 1006L;
        inventoryService.initializeInventory(productId, 100, "test-admin");
        inventoryService.reserveStock(productId, 30, "ORDER", "ORD-004", "test-user");

        // when
        InventoryResponse response = inventoryService.releaseStock(
                productId, 20, "CANCEL", "ORD-004", "test-user");

        // then
        assertThat(response.availableQuantity()).isEqualTo(90);
        assertThat(response.reservedQuantity()).isEqualTo(10);
        assertThat(response.totalQuantity()).isEqualTo(100);
    }

    @Test
    @DisplayName("재고를 추가하면 가용 재고와 전체 재고가 증가한다")
    void addStockUpdatesQuantities() {
        // given
        Long productId = 1007L;
        inventoryService.initializeInventory(productId, 100, "test-admin");

        // when
        InventoryResponse response = inventoryService.addStock(
                productId, 50, "입고", "warehouse-admin");

        // then
        assertThat(response.availableQuantity()).isEqualTo(150);
        assertThat(response.totalQuantity()).isEqualTo(150);
    }

    @Test
    @DisplayName("배치 예약이 정상적으로 동작한다")
    void reserveStockBatchWorks() {
        // given
        Long productId1 = 1008L;
        Long productId2 = 1009L;
        inventoryService.initializeInventory(productId1, 100, "test-admin");
        inventoryService.initializeInventory(productId2, 50, "test-admin");

        Map<Long, Integer> quantities = Map.of(
                productId1, 30,
                productId2, 20
        );

        // when
        var responses = inventoryService.reserveStockBatch(
                quantities, "ORDER", "ORD-005", "test-user");

        // then
        assertThat(responses).hasSize(2);

        // 첫 번째 상품 확인
        var response1 = responses.stream()
                .filter(r -> r.productId().equals(productId1))
                .findFirst()
                .orElseThrow();
        assertThat(response1.availableQuantity()).isEqualTo(70);
        assertThat(response1.reservedQuantity()).isEqualTo(30);

        // 두 번째 상품 확인
        var response2 = responses.stream()
                .filter(r -> r.productId().equals(productId2))
                .findFirst()
                .orElseThrow();
        assertThat(response2.availableQuantity()).isEqualTo(30);
        assertThat(response2.reservedQuantity()).isEqualTo(20);
    }

    @Test
    @DisplayName("동시에 여러 요청이 재고를 예약할 때 데이터 일관성이 유지된다")
    void concurrentReservationsMaintainConsistency() throws InterruptedException {
        // given
        Long productId = 1010L;
        int initialStock = 100;
        inventoryService.initializeInventory(productId, initialStock, "test-admin");

        int numberOfThreads = 10;
        int reservationPerThread = 5; // 각 스레드가 5개씩 예약 (총 50개)

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    inventoryService.reserveStock(
                            productId, reservationPerThread,
                            "ORDER", "ORD-CONCURRENT-" + threadNum, "test-user");
                    successCount.incrementAndGet();
                } catch (CustomBusinessException e) {
                    if (e.getErrorCode() == ShoppingErrorCode.INSUFFICIENT_STOCK) {
                        failCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then
        InventoryResponse finalState = inventoryService.getInventory(productId);

        // 모든 요청이 성공했으므로 50개가 예약되어야 함
        assertThat(successCount.get()).isEqualTo(numberOfThreads);
        assertThat(finalState.availableQuantity()).isEqualTo(initialStock - (numberOfThreads * reservationPerThread));
        assertThat(finalState.reservedQuantity()).isEqualTo(numberOfThreads * reservationPerThread);
        assertThat(finalState.totalQuantity()).isEqualTo(initialStock);
    }

    @Test
    @DisplayName("가용 재고보다 많은 동시 예약 요청 시 일부만 성공한다")
    void concurrentReservationsWithInsufficientStock() throws InterruptedException {
        // given
        Long productId = 1011L;
        int initialStock = 10;
        inventoryService.initializeInventory(productId, initialStock, "test-admin");

        int numberOfThreads = 15;
        int reservationPerThread = 1; // 각 스레드가 1개씩 예약 (총 15개 시도, 10개만 성공해야 함)

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    inventoryService.reserveStock(
                            productId, reservationPerThread,
                            "ORDER", "ORD-OVERFLOW-" + threadNum, "test-user");
                    successCount.incrementAndGet();
                } catch (CustomBusinessException e) {
                    if (e.getErrorCode() == ShoppingErrorCode.INSUFFICIENT_STOCK) {
                        failCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then
        InventoryResponse finalState = inventoryService.getInventory(productId);

        // 10개만 성공, 5개 실패
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failCount.get()).isEqualTo(5);
        assertThat(finalState.availableQuantity()).isEqualTo(0);
        assertThat(finalState.reservedQuantity()).isEqualTo(10);
        assertThat(finalState.totalQuantity()).isEqualTo(10);
    }
}
