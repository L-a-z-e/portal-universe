package com.portal.universe.shoppingservice.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingservice.cart.domain.Cart;
import com.portal.universe.shoppingservice.cart.domain.CartItem;
import com.portal.universe.shoppingservice.cart.domain.CartStatus;
import com.portal.universe.shoppingservice.cart.repository.CartRepository;
import com.portal.universe.shoppingservice.coupon.service.CouponService;
import com.portal.universe.shoppingservice.event.ShoppingEventPublisher;
import com.portal.universe.shoppingservice.feign.PaymentIntentFeignClient;
import com.portal.universe.shoppingservice.feign.dto.PaymentIntentResponse;
import com.portal.universe.shoppingservice.order.domain.Order;
import com.portal.universe.shoppingservice.order.domain.OrderItem;
import com.portal.universe.shoppingservice.order.domain.OrderStatus;
import com.portal.universe.shoppingservice.order.dto.AddressRequest;
import com.portal.universe.shoppingservice.order.dto.CancelOrderRequest;
import com.portal.universe.shoppingservice.order.dto.CreateOrderRequest;
import com.portal.universe.shoppingservice.order.dto.OrderResponse;
import com.portal.universe.shoppingservice.order.repository.OrderRepository;
import com.portal.universe.shoppingservice.order.repository.SagaStateRepository;
import com.portal.universe.shoppingservice.order.saga.OrderSagaOrchestrator;
import com.portal.universe.shoppingservice.support.fixture.CartFixture;
import com.portal.universe.shoppingservice.support.fixture.OrderFixture;
import com.portal.universe.shoppingservice.support.fixture.SagaStateFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private SagaStateRepository sagaStateRepository;

    @Mock
    private OrderSagaOrchestrator orderSagaOrchestrator;

    @Mock
    private CouponService couponService;

    @Mock
    private ShoppingEventPublisher eventPublisher;

    @Mock
    private PaymentIntentFeignClient paymentIntentFeignClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Spy
    private io.micrometer.core.instrument.MeterRegistry meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService.initMetrics();
    }

    private void mockPaymentIntentSuccess() {
        PaymentIntentResponse intentResponse = new PaymentIntentResponse(
                "pi_test123", "ORD-TEST", "user1",
                BigDecimal.valueOf(10000), "PENDING", null, null);
        @SuppressWarnings("unchecked")
        ApiResponse<PaymentIntentResponse> apiResponse = mock(ApiResponse.class);
        when(apiResponse.getData()).thenReturn(intentResponse);
        when(paymentIntentFeignClient.createIntent(any())).thenReturn(apiResponse);
    }

    @Nested
    @DisplayName("createOrder")
    class CreateOrder {

        @Test
        @DisplayName("should_createOrder_when_cartHasItems")
        void should_createOrder_when_cartHasItems() {
            // given
            String userId = "user1";
            Cart cart = CartFixture.builder().userId(userId).status(CartStatus.CHECKED_OUT).build();
            CartItem item = CartFixture.createItem(cart, 1L, "Test Product", BigDecimal.valueOf(5000), 2);
            cart.getItems().add(item);
            AddressRequest addressRequest = new AddressRequest("John", "010-1234-5678", "12345", "Seoul", "Apt 101");
            CreateOrderRequest request = new CreateOrderRequest(addressRequest, null);

            when(cartRepository.findByUserIdAndStatusWithItems(userId, CartStatus.CHECKED_OUT))
                    .thenReturn(List.of(cart));

            Order savedOrder = OrderFixture.builder()
                    .userId(userId).orderNumber("ORD-20260205-TEST0001").status(OrderStatus.CONFIRMED).build();
            OrderItem orderItem = OrderFixture.createItem(savedOrder, 1L, 2, BigDecimal.valueOf(5000));
            savedOrder.getItems().add(orderItem);

            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
            when(orderSagaOrchestrator.startSaga(any(Order.class))).thenReturn(SagaStateFixture.create());
            mockPaymentIntentSuccess();

            // when
            OrderResponse result = orderService.createOrder(userId, request);

            // then
            assertThat(result).isNotNull();
            verify(orderRepository, atLeastOnce()).save(any(Order.class));
            verify(orderSagaOrchestrator).startSaga(any(Order.class));
            verify(eventPublisher).publishOrderCreated(any());
        }

        @Test
        @DisplayName("should_throwException_when_cartNotFound")
        void should_throwException_when_cartNotFound() {
            // given
            String userId = "user1";
            AddressRequest addressRequest = new AddressRequest("John", "010-1234-5678", "12345", "Seoul", "Apt 101");
            CreateOrderRequest request = new CreateOrderRequest(addressRequest, null);

            when(cartRepository.findByUserIdAndStatusWithItems(userId, CartStatus.CHECKED_OUT))
                    .thenReturn(List.of());

            // when & then
            assertThatThrownBy(() -> orderService.createOrder(userId, request))
                    .isInstanceOf(CustomBusinessException.class);
        }

        @Test
        @DisplayName("should_throwException_when_cartEmpty")
        void should_throwException_when_cartEmpty() {
            // given
            String userId = "user1";
            Cart cart = CartFixture.builder().userId(userId).status(CartStatus.CHECKED_OUT).build();

            AddressRequest addressRequest = new AddressRequest("John", "010-1234-5678", "12345", "Seoul", "Apt 101");
            CreateOrderRequest request = new CreateOrderRequest(addressRequest, null);

            when(cartRepository.findByUserIdAndStatusWithItems(userId, CartStatus.CHECKED_OUT))
                    .thenReturn(List.of(cart));

            // when & then
            assertThatThrownBy(() -> orderService.createOrder(userId, request))
                    .isInstanceOf(CustomBusinessException.class);
        }

        @Test
        @DisplayName("should_applyCoupon_when_couponProvided")
        void should_applyCoupon_when_couponProvided() {
            // given
            String userId = "user1";
            Cart cart = CartFixture.builder().userId(userId).status(CartStatus.CHECKED_OUT).build();
            CartItem item = CartFixture.createItem(cart, 1L, "Test Product", BigDecimal.valueOf(5000), 2);
            cart.getItems().add(item);
            AddressRequest addressRequest = new AddressRequest("John", "010-1234-5678", "12345", "Seoul", "Apt 101");
            CreateOrderRequest request = new CreateOrderRequest(addressRequest, 100L);

            when(cartRepository.findByUserIdAndStatusWithItems(userId, CartStatus.CHECKED_OUT))
                    .thenReturn(List.of(cart));

            when(couponService.calculateDiscount(eq(100L), any(BigDecimal.class)))
                    .thenReturn(BigDecimal.valueOf(1000));

            Order savedOrder = OrderFixture.builder()
                    .userId(userId).orderNumber("ORD-20260205-TEST0002").status(OrderStatus.CONFIRMED).build();
            OrderItem orderItem = OrderFixture.createItem(savedOrder, 1L, 2, BigDecimal.valueOf(5000));
            savedOrder.getItems().add(orderItem);

            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
            when(orderSagaOrchestrator.startSaga(any(Order.class))).thenReturn(SagaStateFixture.create());
            mockPaymentIntentSuccess();

            // when
            OrderResponse result = orderService.createOrder(userId, request);

            // then
            assertThat(result).isNotNull();
            verify(couponService).validateCouponForOrder(eq(100L), eq(userId), any(BigDecimal.class));
            verify(couponService).calculateDiscount(eq(100L), any(BigDecimal.class));
            verify(couponService).useCoupon(100L, savedOrder.getId());
        }

        @Test
        @DisplayName("should_startSaga_when_orderCreated")
        void should_startSaga_when_orderCreated() {
            // given
            String userId = "user1";
            Cart cart = CartFixture.builder().userId(userId).status(CartStatus.CHECKED_OUT).build();
            CartItem item = CartFixture.createItem(cart, 1L, "Test Product", BigDecimal.valueOf(5000), 2);
            cart.getItems().add(item);
            AddressRequest addressRequest = new AddressRequest("John", "010-1234-5678", "12345", "Seoul", "Apt 101");
            CreateOrderRequest request = new CreateOrderRequest(addressRequest, null);

            when(cartRepository.findByUserIdAndStatusWithItems(userId, CartStatus.CHECKED_OUT))
                    .thenReturn(List.of(cart));

            Order savedOrder = OrderFixture.builder()
                    .userId(userId).orderNumber("ORD-20260205-TEST0003").status(OrderStatus.CONFIRMED).build();
            OrderItem orderItem = OrderFixture.createItem(savedOrder, 1L, 2, BigDecimal.valueOf(5000));
            savedOrder.getItems().add(orderItem);

            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
            when(orderSagaOrchestrator.startSaga(any(Order.class))).thenReturn(SagaStateFixture.create());
            mockPaymentIntentSuccess();

            // when
            orderService.createOrder(userId, request);

            // then
            verify(orderSagaOrchestrator).startSaga(any(Order.class));
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
            OrderResponse result = orderService.getOrder("user1", "ORD-001");

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
            assertThatThrownBy(() -> orderService.getOrder("user1", "ORD-999"))
                    .isInstanceOf(CustomBusinessException.class);
        }

        @Test
        @DisplayName("should_throwException_when_userMismatch")
        void should_throwException_when_userMismatch() {
            // given
            Order order = OrderFixture.builder()
                    .userId("user1").orderNumber("ORD-001").status(OrderStatus.CONFIRMED).build();
            when(orderRepository.findByOrderNumberWithItems("ORD-001")).thenReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.getOrder("user2", "ORD-001"))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("getUserOrders")
    class GetUserOrders {

        @Test
        @DisplayName("should_returnUserOrders_when_called")
        void should_returnUserOrders_when_called() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Order order = OrderFixture.builder()
                    .userId("user1").orderNumber("ORD-001").status(OrderStatus.CONFIRMED).build();
            Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);
            when(orderRepository.findByUserIdOrderByCreatedAtDesc("user1", pageable)).thenReturn(orderPage);

            // when
            Page<OrderResponse> result = orderService.getUserOrders("user1", pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrder {

        @Test
        @DisplayName("should_cancelOrder_when_cancellable")
        void should_cancelOrder_when_cancellable() {
            // given
            Order order = OrderFixture.builder()
                    .userId("user1").orderNumber("ORD-001").status(OrderStatus.CONFIRMED).build();
            when(orderRepository.findByOrderNumberWithItems("ORD-001")).thenReturn(Optional.of(order));
            when(sagaStateRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.empty());
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            CancelOrderRequest request = new CancelOrderRequest("Changed my mind");

            // when
            OrderResponse result = orderService.cancelOrder("user1", "ORD-001", request);

            // then
            assertThat(result).isNotNull();
            verify(eventPublisher).publishOrderCancelled(any());
        }

        @Test
        @DisplayName("should_throwException_when_orderCannotBeCancelled")
        void should_throwException_when_orderCannotBeCancelled() {
            // given
            Order order = OrderFixture.builder()
                    .userId("user1").orderNumber("ORD-001").status(OrderStatus.DELIVERED).build();
            when(orderRepository.findByOrderNumberWithItems("ORD-001")).thenReturn(Optional.of(order));

            CancelOrderRequest request = new CancelOrderRequest("Want to cancel");

            // when & then
            assertThatThrownBy(() -> orderService.cancelOrder("user1", "ORD-001", request))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("completeOrderAfterPayment")
    class CompleteOrderAfterPayment {

        @Test
        @DisplayName("should_completeOrder_when_paymentDone")
        void should_completeOrder_when_paymentDone() {
            // given
            Order order = OrderFixture.builder()
                    .userId("user1").orderNumber("ORD-001").status(OrderStatus.PAID).build();
            when(orderRepository.findByOrderNumberWithItems("ORD-001")).thenReturn(Optional.of(order));

            // when
            OrderResponse result = orderService.completeOrderAfterPayment("ORD-001");

            // then
            assertThat(result).isNotNull();
            verify(orderSagaOrchestrator).completeSagaAfterPayment("ORD-001");
        }
    }
}
