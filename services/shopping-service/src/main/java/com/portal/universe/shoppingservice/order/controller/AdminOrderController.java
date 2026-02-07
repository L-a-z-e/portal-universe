package com.portal.universe.shoppingservice.order.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.commonlibrary.response.PageResponse;
import com.portal.universe.shoppingservice.order.dto.OrderResponse;
import com.portal.universe.shoppingservice.order.service.AdminOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 전용 주문 관리 API를 제공하는 컨트롤러입니다.
 * 모든 엔드포인트는 ADMIN 권한이 필요합니다.
 */
@RestController
@RequestMapping("/admin/orders")
@PreAuthorize("hasAnyAuthority('ROLE_SHOPPING_ADMIN', 'ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    /**
     * 전체 주문 목록을 조회합니다 (Admin).
     *
     * @param status 주문 상태 필터 (optional)
     * @param keyword 검색 키워드 - 주문번호 또는 사용자 ID (optional)
     * @param pageable 페이징 정보
     * @return 주문 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success(PageResponse.from(adminOrderService.getOrders(status, keyword, pageable))));
    }

    /**
     * 주문 번호로 주문을 조회합니다 (Admin).
     *
     * @param orderNumber 주문 번호
     * @return 주문 정보
     */
    @GetMapping("/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable String orderNumber) {
        return ResponseEntity.ok(
                ApiResponse.success(adminOrderService.getOrder(orderNumber)));
    }

    /**
     * 주문 상태를 변경합니다 (Admin).
     *
     * @param orderNumber 주문 번호
     * @param request 상태 변경 요청
     * @return 변경된 주문 정보
     */
    @PutMapping("/{orderNumber}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable String orderNumber,
            @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(adminOrderService.updateOrderStatus(orderNumber, request.status())));
    }

    /**
     * 주문 상태 변경 요청 DTO
     */
    public record UpdateOrderStatusRequest(String status) {}
}
