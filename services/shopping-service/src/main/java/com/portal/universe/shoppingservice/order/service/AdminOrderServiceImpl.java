package com.portal.universe.shoppingservice.order.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import com.portal.universe.shoppingservice.order.domain.Order;
import com.portal.universe.shoppingservice.order.domain.OrderStatus;
import com.portal.universe.shoppingservice.order.dto.OrderResponse;
import com.portal.universe.shoppingservice.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin 전용 주문 관리 서비스 구현체입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderRepository orderRepository;

    @Override
    public Page<OrderResponse> getOrders(String status, String keyword, Pageable pageable) {
        Page<Order> orders;

        if (status != null && !status.isEmpty()) {
            OrderStatus orderStatus = OrderStatus.valueOf(status);
            orders = orderRepository.findByStatus(orderStatus, pageable);
        } else if (keyword != null && !keyword.isEmpty()) {
            orders = orderRepository.findByOrderNumberContainingOrUserIdContaining(keyword, keyword, pageable);
        } else {
            orders = orderRepository.findAll(pageable);
        }

        return orders.map(OrderResponse::from);
    }

    @Override
    public OrderResponse getOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.ORDER_NOT_FOUND));
        return OrderResponse.from(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(String orderNumber, String status) {
        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.ORDER_NOT_FOUND));

        OrderStatus newStatus = OrderStatus.valueOf(status);
        log.info("Admin updating order status: orderNumber={}, {} -> {}", orderNumber, order.getStatus(), newStatus);

        order.updateStatus(newStatus);
        Order savedOrder = orderRepository.save(order);
        return OrderResponse.from(savedOrder);
    }
}
