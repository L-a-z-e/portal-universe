package com.portal.universe.shoppingservice.order.service;

import com.portal.universe.shoppingservice.order.dto.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Admin 전용 주문 관리 서비스 인터페이스입니다.
 */
public interface AdminOrderService {

    /**
     * 전체 주문 목록을 조회합니다 (관리자).
     *
     * @param status 주문 상태 필터 (null이면 전체)
     * @param keyword 검색 키워드 (null이면 전체)
     * @param pageable 페이징 정보
     * @return 주문 목록
     */
    Page<OrderResponse> getOrders(String status, String keyword, Pageable pageable);

    /**
     * 주문 번호로 주문을 조회합니다 (관리자).
     *
     * @param orderNumber 주문 번호
     * @return 주문 정보
     */
    OrderResponse getOrder(String orderNumber);

    /**
     * 주문 상태를 변경합니다 (관리자).
     *
     * @param orderNumber 주문 번호
     * @param status 변경할 상태
     * @return 변경된 주문 정보
     */
    OrderResponse updateOrderStatus(String orderNumber, String status);
}
