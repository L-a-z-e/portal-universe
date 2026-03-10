package com.portal.universe.shoppingservice.order.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.commonlibrary.response.PageResponse;
import com.portal.universe.shoppingservice.order.dto.CancelOrderRequest;
import com.portal.universe.shoppingservice.order.dto.CreateOrderRequest;
import com.portal.universe.shoppingservice.order.dto.OrderResponse;
import com.portal.universe.shoppingservice.order.service.OrderService;
import com.portal.universe.commonlibrary.security.context.AuthUser;
import com.portal.universe.commonlibrary.security.context.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

/**
 * 주문 API 컨트롤러
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ApiResponse<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @CurrentUser AuthUser user) {
        return ApiResponse.success(orderService.createOrder(user.uuid(), request));
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> getUserOrders(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @CurrentUser AuthUser user) {
        return ApiResponse.success(PageResponse.from(orderService.getUserOrders(user.uuid(), pageable)));
    }

    @GetMapping("/{orderNumber}")
    public ApiResponse<OrderResponse> getOrder(
            @PathVariable String orderNumber,
            @CurrentUser AuthUser user) {
        return ApiResponse.success(orderService.getOrder(user.uuid(), orderNumber));
    }

    @PostMapping("/{orderNumber}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(
            @PathVariable String orderNumber,
            @Valid @RequestBody CancelOrderRequest request,
            @CurrentUser AuthUser user) {
        return ApiResponse.success(orderService.cancelOrder(user.uuid(), orderNumber, request));
    }
}
