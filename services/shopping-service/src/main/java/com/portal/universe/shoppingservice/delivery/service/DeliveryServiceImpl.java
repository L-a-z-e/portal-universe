package com.portal.universe.shoppingservice.delivery.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import com.portal.universe.shoppingservice.delivery.domain.Delivery;
import com.portal.universe.shoppingservice.delivery.dto.DeliveryResponse;
import com.portal.universe.shoppingservice.delivery.dto.UpdateDeliveryStatusRequest;
import com.portal.universe.shoppingservice.delivery.repository.DeliveryRepository;
import com.portal.universe.shoppingservice.order.domain.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 배송 관리 서비스 구현체입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;

    @Override
    @Transactional
    public DeliveryResponse createDelivery(Order order) {
        // 이미 배송이 존재하는지 확인
        deliveryRepository.findByOrderId(order.getId())
                .ifPresent(d -> {
                    throw new CustomBusinessException(ShoppingErrorCode.DELIVERY_ALREADY_SHIPPED);
                });

        Delivery delivery = Delivery.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .shippingAddress(order.getShippingAddress())
                .carrier("기본택배")
                .build();

        Delivery savedDelivery = deliveryRepository.save(delivery);

        log.info("Delivery created: {} for order {}", savedDelivery.getTrackingNumber(), order.getOrderNumber());
        return DeliveryResponse.from(savedDelivery);
    }

    @Override
    public DeliveryResponse getDeliveryByTrackingNumber(String trackingNumber) {
        Delivery delivery = deliveryRepository.findByTrackingNumberWithHistories(trackingNumber)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.DELIVERY_NOT_FOUND));

        return DeliveryResponse.from(delivery);
    }

    @Override
    public DeliveryResponse getDeliveryByOrderNumber(String orderNumber) {
        Delivery delivery = deliveryRepository.findByOrderNumberWithHistories(orderNumber)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.DELIVERY_NOT_FOUND));

        return DeliveryResponse.from(delivery);
    }

    @Override
    @Transactional
    public DeliveryResponse updateDeliveryStatus(String trackingNumber, UpdateDeliveryStatusRequest request) {
        Delivery delivery = deliveryRepository.findByTrackingNumberWithHistories(trackingNumber)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.DELIVERY_NOT_FOUND));

        delivery.updateStatus(request.status(), request.location(), request.description());
        Delivery savedDelivery = deliveryRepository.save(delivery);

        log.info("Delivery status updated: {} -> {} (order: {})",
                trackingNumber, request.status(), delivery.getOrderNumber());
        return DeliveryResponse.from(savedDelivery);
    }

    @Override
    @Transactional
    public void cancelDelivery(Long orderId) {
        deliveryRepository.findByOrderId(orderId)
                .ifPresent(delivery -> {
                    if (delivery.getStatus().isCancellable()) {
                        delivery.cancel("Order cancelled");
                        deliveryRepository.save(delivery);
                        log.info("Delivery cancelled for order ID: {}", orderId);
                    } else {
                        log.warn("Cannot cancel delivery for order ID: {} (status: {})",
                                orderId, delivery.getStatus());
                    }
                });
    }
}
