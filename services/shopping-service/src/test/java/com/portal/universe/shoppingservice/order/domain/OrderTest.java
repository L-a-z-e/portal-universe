package com.portal.universe.shoppingservice.order.domain;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.domain.Address;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Order 엔티티 단위 테스트입니다.
 */
class OrderTest {

    @Nested
    @DisplayName("주문 생성 테스트")
    class CreateOrderTest {

        @Test
        @DisplayName("새 주문 생성 시 PENDING 상태로 초기화되고 주문번호가 생성된다")
        void createNewOrder() {
            // given
            String userId = "user-123";
            Address address = createAddress();

            // when
            Order order = Order.builder()
                    .userId(userId)
                    .shippingAddress(address)
                    .build();

            // then
            assertThat(order.getUserId()).isEqualTo(userId);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(order.getOrderNumber()).startsWith("ORD-");
            assertThat(order.getOrderNumber()).hasSize(21); // ORD-YYYYMMDD-XXXXXXXX = 21자 (4+8+1+8)
            assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(order.getShippingAddress()).isEqualTo(address);
        }
    }

    @Nested
    @DisplayName("주문 항목 추가 테스트")
    class AddItemTest {

        @Test
        @DisplayName("주문에 항목을 추가하면 총액이 자동 계산된다")
        void addItemCalculatesTotalAmount() {
            // given
            Order order = createOrder();

            // when
            order.addItem(1L, "Product 1", new BigDecimal("10000"), 2); // 20000
            order.addItem(2L, "Product 2", new BigDecimal("5000"), 3);  // 15000

            // then
            assertThat(order.getItems()).hasSize(2);
            assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("35000"));
            assertThat(order.getTotalQuantity()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("주문 상태 전이 테스트")
    class OrderStatusTransitionTest {

        @Test
        @DisplayName("PENDING 주문을 확정하면 CONFIRMED 상태로 변경된다")
        void confirmPendingOrder() {
            // given
            Order order = createOrderWithItems();

            // when
            order.confirm();

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }

        @Test
        @DisplayName("CONFIRMED 주문을 결제 완료하면 PAID 상태로 변경된다")
        void markConfirmedOrderAsPaid() {
            // given
            Order order = createOrderWithItems();
            order.confirm();

            // when
            order.markAsPaid();

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        }

        @Test
        @DisplayName("PAID 주문을 배송 시작하면 SHIPPING 상태로 변경된다")
        void shipPaidOrder() {
            // given
            Order order = createOrderWithItems();
            order.confirm();
            order.markAsPaid();

            // when
            order.ship();

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPING);
        }

        @Test
        @DisplayName("SHIPPING 주문을 배송 완료하면 DELIVERED 상태로 변경된다")
        void deliverShippingOrder() {
            // given
            Order order = createOrderWithItems();
            order.confirm();
            order.markAsPaid();
            order.ship();

            // when
            order.deliver();

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        }

        @Test
        @DisplayName("PENDING이 아닌 주문을 확정하면 예외가 발생한다")
        void confirmNonPendingOrderThrowsException() {
            // given
            Order order = createOrderWithItems();
            order.confirm(); // CONFIRMED

            // when & then
            assertThatThrownBy(order::confirm)
                    .isInstanceOf(CustomBusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ShoppingErrorCode.INVALID_ORDER_STATUS);
        }

        @Test
        @DisplayName("CONFIRMED가 아닌 주문을 결제 완료하면 예외가 발생한다")
        void markAsPaidNonConfirmedOrderThrowsException() {
            // given
            Order order = createOrderWithItems(); // PENDING

            // when & then
            assertThatThrownBy(order::markAsPaid)
                    .isInstanceOf(CustomBusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ShoppingErrorCode.INVALID_ORDER_STATUS);
        }
    }

    @Nested
    @DisplayName("주문 취소 테스트")
    class CancelOrderTest {

        @Test
        @DisplayName("PENDING 주문을 취소할 수 있다")
        void cancelPendingOrder() {
            // given
            Order order = createOrderWithItems();

            // when
            order.cancel("고객 요청");

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.getCancelReason()).isEqualTo("고객 요청");
            assertThat(order.getCancelledAt()).isNotNull();
        }

        @Test
        @DisplayName("CONFIRMED 주문을 취소할 수 있다")
        void cancelConfirmedOrder() {
            // given
            Order order = createOrderWithItems();
            order.confirm();

            // when
            order.cancel("고객 요청");

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("PAID 주문을 취소할 수 있다")
        void cancelPaidOrder() {
            // given
            Order order = createOrderWithItems();
            order.confirm();
            order.markAsPaid();

            // when
            order.cancel("고객 요청");

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("SHIPPING 주문은 취소할 수 없다")
        void cancelShippingOrderThrowsException() {
            // given
            Order order = createOrderWithItems();
            order.confirm();
            order.markAsPaid();
            order.ship();

            // when & then
            assertThatThrownBy(() -> order.cancel("고객 요청"))
                    .isInstanceOf(CustomBusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ShoppingErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }

        @Test
        @DisplayName("DELIVERED 주문은 취소할 수 없다")
        void cancelDeliveredOrderThrowsException() {
            // given
            Order order = createOrderWithItems();
            order.confirm();
            order.markAsPaid();
            order.ship();
            order.deliver();

            // when & then
            assertThatThrownBy(() -> order.cancel("고객 요청"))
                    .isInstanceOf(CustomBusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ShoppingErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }
    }

    @Nested
    @DisplayName("주문 환불 테스트")
    class RefundOrderTest {

        @Test
        @DisplayName("PAID 주문을 환불할 수 있다")
        void refundPaidOrder() {
            // given
            Order order = createOrderWithItems();
            order.confirm();
            order.markAsPaid();

            // when
            order.refund();

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        }

        @Test
        @DisplayName("SHIPPING 주문을 환불할 수 있다")
        void refundShippingOrder() {
            // given
            Order order = createOrderWithItems();
            order.confirm();
            order.markAsPaid();
            order.ship();

            // when
            order.refund();

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        }

        @Test
        @DisplayName("DELIVERED 주문은 환불할 수 없다")
        void refundDeliveredOrderThrowsException() {
            // given
            Order order = createOrderWithItems();
            order.confirm();
            order.markAsPaid();
            order.ship();
            order.deliver();

            // when & then
            assertThatThrownBy(order::refund)
                    .isInstanceOf(CustomBusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ShoppingErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }
    }

    private Order createOrder() {
        return Order.builder()
                .userId("test-user")
                .shippingAddress(createAddress())
                .build();
    }

    private Order createOrderWithItems() {
        Order order = createOrder();
        order.addItem(1L, "Product", new BigDecimal("10000"), 1);
        return order;
    }

    private Address createAddress() {
        return Address.builder()
                .receiverName("홍길동")
                .receiverPhone("010-1234-5678")
                .zipCode("12345")
                .address1("서울시 강남구 테헤란로 123")
                .address2("101동 1001호")
                .build();
    }
}
