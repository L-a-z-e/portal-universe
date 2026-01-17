package com.portal.universe.shoppingservice.delivery.service;

import com.portal.universe.shoppingservice.delivery.dto.DeliveryResponse;
import com.portal.universe.shoppingservice.delivery.dto.UpdateDeliveryStatusRequest;
import com.portal.universe.shoppingservice.order.domain.Order;

/**
 * 배송 관리 서비스 인터페이스입니다.
 */
public interface DeliveryService {

    /**
     * 주문에 대한 배송을 생성합니다.
     *
     * @param order 주문 정보
     * @return 생성된 배송 정보
     */
    DeliveryResponse createDelivery(Order order);

    /**
     * 운송장 번호로 배송을 조회합니다.
     *
     * @param trackingNumber 운송장 번호
     * @return 배송 정보
     */
    DeliveryResponse getDeliveryByTrackingNumber(String trackingNumber);

    /**
     * 주문 번호로 배송을 조회합니다.
     *
     * @param orderNumber 주문 번호
     * @return 배송 정보
     */
    DeliveryResponse getDeliveryByOrderNumber(String orderNumber);

    /**
     * 배송 상태를 변경합니다 (관리자 전용).
     *
     * @param trackingNumber 운송장 번호
     * @param request 상태 변경 요청
     * @return 업데이트된 배송 정보
     */
    DeliveryResponse updateDeliveryStatus(String trackingNumber, UpdateDeliveryStatusRequest request);

    /**
     * 배송을 취소합니다 (Saga 보상용).
     *
     * @param orderId 주문 ID
     */
    void cancelDelivery(Long orderId);
}
