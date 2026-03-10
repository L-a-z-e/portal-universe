package com.portal.universe.shoppingservice.delivery.service;

import com.portal.universe.shoppingservice.delivery.dto.DeliveryResponse;
import com.portal.universe.shoppingservice.delivery.dto.UpdateDeliveryStatusRequest;
import com.portal.universe.shoppingservice.order.domain.Order;

/**
 * 배송 관리 서비스 인터페이스입니다.
 */
public interface DeliveryService {

    DeliveryResponse createDelivery(Order order);

    DeliveryResponse getDeliveryByTrackingNumber(String trackingNumber, String userId);

    DeliveryResponse getDeliveryByOrderNumber(String orderNumber, String userId);

    DeliveryResponse updateDeliveryStatus(String trackingNumber, UpdateDeliveryStatusRequest request);

    void cancelDelivery(Long orderId); // Saga 보상용
}
