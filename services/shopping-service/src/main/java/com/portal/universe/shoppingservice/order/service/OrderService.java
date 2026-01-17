package com.portal.universe.shoppingservice.order.service;

import com.portal.universe.shoppingservice.order.dto.CancelOrderRequest;
import com.portal.universe.shoppingservice.order.dto.CreateOrderRequest;
import com.portal.universe.shoppingservice.order.dto.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 주문 관리 서비스 인터페이스입니다.
 */
public interface OrderService {

    /**
     * 주문을 생성합니다 (장바구니 체크아웃 후 호출).
     * Saga 패턴을 사용하여 재고 예약까지 처리합니다.
     *
     * @param userId 사용자 ID
     * @param request 주문 생성 요청
     * @return 생성된 주문 정보
     */
    OrderResponse createOrder(String userId, CreateOrderRequest request);

    /**
     * 주문 번호로 주문을 조회합니다.
     *
     * @param userId 사용자 ID (권한 확인용)
     * @param orderNumber 주문 번호
     * @return 주문 정보
     */
    OrderResponse getOrder(String userId, String orderNumber);

    /**
     * 사용자의 주문 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 주문 목록
     */
    Page<OrderResponse> getUserOrders(String userId, Pageable pageable);

    /**
     * 주문을 취소합니다.
     * Saga 보상을 통해 예약된 재고를 해제합니다.
     *
     * @param userId 사용자 ID
     * @param orderNumber 주문 번호
     * @param request 취소 요청
     * @return 취소된 주문 정보
     */
    OrderResponse cancelOrder(String userId, String orderNumber, CancelOrderRequest request);

    /**
     * 결제 완료 후 주문을 처리합니다.
     * Saga의 나머지 단계를 실행합니다.
     *
     * @param orderNumber 주문 번호
     * @return 업데이트된 주문 정보
     */
    OrderResponse completeOrderAfterPayment(String orderNumber);
}
