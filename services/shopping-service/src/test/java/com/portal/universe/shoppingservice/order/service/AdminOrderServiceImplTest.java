package com.portal.universe.shoppingservice.order.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.order.domain.Order;
import com.portal.universe.shoppingservice.order.domain.OrderStatus;
import com.portal.universe.shoppingservice.order.dto.OrderResponse;
import com.portal.universe.shoppingservice.order.repository.OrderRepository;
import com.portal.universe.shoppingservice.support.fixture.OrderFixture;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private AdminOrderServiceImpl adminOrderService;

    @Nested
    @DisplayName("getOrders")
    class GetOrders {

        @Test
        @DisplayName("should_filterByStatus_when_statusProvided")
        void should_filterByStatus_when_statusProvided() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Order order = OrderFixture.builder()
                    .userId("user1").orderNumber("ORD-001").status(OrderStatus.CONFIRMED).build();
            Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);
            when(orderRepository.findByStatus(OrderStatus.CONFIRMED, pageable)).thenReturn(orderPage);

            // when
            Page<OrderResponse> result = adminOrderService.getOrders("CONFIRMED", null, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            verify(orderRepository).findByStatus(OrderStatus.CONFIRMED, pageable);
        }

        @Test
        @DisplayName("should_filterByKeyword_when_keywordProvided")
        void should_filterByKeyword_when_keywordProvided() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Order order = OrderFixture.builder()
                    .userId("user1").orderNumber("ORD-001").status(OrderStatus.CONFIRMED).build();
            Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);
            when(orderRepository.findByOrderNumberContainingOrUserIdContaining("ORD", "ORD", pageable))
                    .thenReturn(orderPage);

            // when
            Page<OrderResponse> result = adminOrderService.getOrders(null, "ORD", pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            verify(orderRepository).findByOrderNumberContainingOrUserIdContaining("ORD", "ORD", pageable);
        }
    }

    @Nested
    @DisplayName("getOrder")
    class GetOrder {

        @Test
        @DisplayName("should_returnOrder_when_found")
        void should_returnOrder_when_found() {
            // given
            Order order = OrderFixture.builder()
                    .userId("user1").orderNumber("ORD-001").status(OrderStatus.CONFIRMED).build();
            when(orderRepository.findByOrderNumberWithItems("ORD-001")).thenReturn(Optional.of(order));

            // when
            OrderResponse result = adminOrderService.getOrder("ORD-001");

            // then
            assertThat(result).isNotNull();
            assertThat(result.orderNumber()).isEqualTo("ORD-001");
        }

        @Test
        @DisplayName("should_throwException_when_notFound")
        void should_throwException_when_notFound() {
            // given
            when(orderRepository.findByOrderNumberWithItems("ORD-999")).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminOrderService.getOrder("ORD-999"))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("updateOrderStatus")
    class UpdateOrderStatus {

        @Test
        @DisplayName("should_updateStatus_when_valid")
        void should_updateStatus_when_valid() {
            // given
            Order order = OrderFixture.builder()
                    .userId("user1").orderNumber("ORD-001").status(OrderStatus.CONFIRMED).build();
            when(orderRepository.findByOrderNumberWithItems("ORD-001")).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            // when
            OrderResponse result = adminOrderService.updateOrderStatus("ORD-001", "PAID");

            // then
            assertThat(result).isNotNull();
            verify(orderRepository).save(any(Order.class));
        }
    }
}
