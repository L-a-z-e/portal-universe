package com.portal.universe.shoppingservice.order.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingservice.order.dto.CancelOrderRequest;
import com.portal.universe.shoppingservice.order.dto.CreateOrderRequest;
import com.portal.universe.shoppingservice.order.dto.OrderResponse;
import com.portal.universe.shoppingservice.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * 주문 API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/shopping/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 주문을 생성합니다.
     * 장바구니가 체크아웃된 상태여야 합니다.
     *
     * @param request 주문 생성 요청
     * @param jwt 인증 정보
     * @return 생성된 주문 정보
     */
    @PostMapping
    public ApiResponse<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        return ApiResponse.success(orderService.createOrder(userId, request));
    }

    /**
     * 사용자의 주문 목록을 조회합니다.
     *
     * @param pageable 페이징 정보
     * @param jwt 인증 정보
     * @return 주문 목록
     */
    @GetMapping
    public ApiResponse<Page<OrderResponse>> getUserOrders(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        return ApiResponse.success(orderService.getUserOrders(userId, pageable));
    }

    /**
     * 주문 번호로 주문을 조회합니다.
     *
     * @param orderNumber 주문 번호
     * @param jwt 인증 정보
     * @return 주문 정보
     */
    @GetMapping("/{orderNumber}")
    public ApiResponse<OrderResponse> getOrder(
            @PathVariable String orderNumber,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        return ApiResponse.success(orderService.getOrder(userId, orderNumber));
    }

    /**
     * 주문을 취소합니다.
     *
     * @param orderNumber 주문 번호
     * @param request 취소 요청
     * @param jwt 인증 정보
     * @return 취소된 주문 정보
     */
    @PostMapping("/{orderNumber}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(
            @PathVariable String orderNumber,
            @Valid @RequestBody CancelOrderRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        return ApiResponse.success(orderService.cancelOrder(userId, orderNumber, request));
    }
}
