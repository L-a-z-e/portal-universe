package com.portal.universe.shoppingservice.order.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.cart.domain.Cart;
import com.portal.universe.shoppingservice.cart.domain.CartItem;
import com.portal.universe.shoppingservice.cart.domain.CartStatus;
import com.portal.universe.shoppingservice.cart.repository.CartRepository;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import com.portal.universe.shoppingservice.coupon.service.CouponService;
import com.portal.universe.shoppingservice.inventory.service.InventoryService;
import com.portal.universe.shoppingservice.order.domain.Order;
import com.portal.universe.shoppingservice.order.domain.OrderItem;
import com.portal.universe.shoppingservice.order.dto.CancelOrderRequest;
import com.portal.universe.shoppingservice.order.dto.CreateOrderRequest;
import com.portal.universe.shoppingservice.order.dto.OrderResponse;
import com.portal.universe.shoppingservice.order.repository.OrderRepository;
import com.portal.universe.shoppingservice.order.repository.SagaStateRepository;
import com.portal.universe.shoppingservice.order.saga.OrderSagaOrchestrator;
import com.portal.universe.shoppingservice.order.saga.SagaState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 주문 관리 서비스 구현체입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final SagaStateRepository sagaStateRepository;
    private final OrderSagaOrchestrator orderSagaOrchestrator;
    private final InventoryService inventoryService;
    private final CouponService couponService;

    @Override
    @Transactional
    public OrderResponse createOrder(String userId, CreateOrderRequest request) {
        // 1. 체크아웃된 장바구니 조회
        Cart cart = cartRepository.findByUserIdAndStatusWithItems(userId, CartStatus.CHECKED_OUT)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.CART_NOT_FOUND));

        if (cart.getItems().isEmpty()) {
            throw new CustomBusinessException(ShoppingErrorCode.CART_EMPTY);
        }

        // 2. 주문 생성
        Order order = Order.builder()
                .userId(userId)
                .shippingAddress(request.shippingAddress().toEntity())
                .build();

        // 장바구니 항목을 주문 항목으로 변환
        for (CartItem cartItem : cart.getItems()) {
            order.addItem(
                    cartItem.getProductId(),
                    cartItem.getProductName(),
                    cartItem.getPrice(),
                    cartItem.getQuantity()
            );
        }

        // 3. 쿠폰 적용 (선택 사항)
        if (request.userCouponId() != null) {
            // 쿠폰 검증 (userId는 String이므로 Long으로 파싱)
            Long userIdLong = Long.parseLong(userId);
            couponService.validateCouponForOrder(request.userCouponId(), userIdLong, order.getTotalAmount());

            // 할인 금액 계산
            BigDecimal discountAmount = couponService.calculateDiscount(request.userCouponId(), order.getTotalAmount());

            // 주문에 쿠폰 적용
            order.applyCoupon(request.userCouponId(), discountAmount);

            log.info("Coupon applied to order: userCouponId={}, discount={}",
                    request.userCouponId(), discountAmount);
        }

        order.confirm(); // 주문 확정 (PENDING -> CONFIRMED)
        Order savedOrder = orderRepository.save(order);

        // 4. 쿠폰 사용 처리
        if (request.userCouponId() != null) {
            couponService.useCoupon(request.userCouponId(), savedOrder.getId());
        }

        // 5. Saga 시작 (재고 예약)
        try {
            orderSagaOrchestrator.startSaga(savedOrder);
        } catch (Exception e) {
            log.error("Failed to start saga for order {}: {}", savedOrder.getOrderNumber(), e.getMessage());
            throw e;
        }

        log.info("Order created successfully: {} (user: {}, items: {}, total: {}, discount: {}, final: {})",
                savedOrder.getOrderNumber(), userId, savedOrder.getItems().size(),
                savedOrder.getTotalAmount(), savedOrder.getDiscountAmount(), savedOrder.getFinalAmount());

        return OrderResponse.from(savedOrder);
    }

    @Override
    public OrderResponse getOrder(String userId, String orderNumber) {
        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.ORDER_NOT_FOUND));

        // 본인 주문인지 확인
        if (!order.getUserId().equals(userId)) {
            throw new CustomBusinessException(ShoppingErrorCode.ORDER_USER_MISMATCH);
        }

        return OrderResponse.from(order);
    }

    @Override
    public Page<OrderResponse> getUserOrders(String userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(OrderResponse::from);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(String userId, String orderNumber, CancelOrderRequest request) {
        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.ORDER_NOT_FOUND));

        // 본인 주문인지 확인
        if (!order.getUserId().equals(userId)) {
            throw new CustomBusinessException(ShoppingErrorCode.ORDER_USER_MISMATCH);
        }

        // 취소 가능 여부 확인
        if (!order.getStatus().isCancellable()) {
            throw new CustomBusinessException(ShoppingErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }

        // Saga 상태 조회
        SagaState sagaState = sagaStateRepository.findByOrderNumber(orderNumber)
                .orElse(null);

        // 예약된 재고 해제
        try {
            Map<Long, Integer> quantities = order.getItems().stream()
                    .collect(Collectors.toMap(
                            OrderItem::getProductId,
                            OrderItem::getQuantity,
                            Integer::sum
                    ));

            inventoryService.releaseStockBatch(
                    quantities,
                    "ORDER_CANCEL",
                    orderNumber,
                    userId
            );
        } catch (Exception e) {
            log.error("Failed to release stock for order {}: {}", orderNumber, e.getMessage());
            // 재고 해제 실패해도 주문 취소는 진행
        }

        // 주문 취소
        order.cancel(request.reason());
        Order savedOrder = orderRepository.save(order);

        // Saga 상태 업데이트
        if (sagaState != null) {
            sagaState.markAsFailed("Order cancelled by user: " + request.reason());
            sagaStateRepository.save(sagaState);
        }

        log.info("Order cancelled: {} (user: {}, reason: {})", orderNumber, userId, request.reason());
        return OrderResponse.from(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse completeOrderAfterPayment(String orderNumber) {
        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.ORDER_NOT_FOUND));

        // Saga의 나머지 단계 실행 (재고 차감, 주문 확정)
        orderSagaOrchestrator.completeSagaAfterPayment(orderNumber);

        // 변경된 주문 조회
        order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.ORDER_NOT_FOUND));

        log.info("Order completed after payment: {}", orderNumber);
        return OrderResponse.from(order);
    }
}
