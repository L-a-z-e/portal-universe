package com.portal.universe.shoppingservice.delivery.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingservice.delivery.dto.DeliveryResponse;
import com.portal.universe.shoppingservice.delivery.dto.UpdateDeliveryStatusRequest;
import com.portal.universe.shoppingservice.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 배송 API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    /**
     * 운송장 번호로 배송을 조회합니다.
     *
     * @param trackingNumber 운송장 번호
     * @return 배송 정보
     */
    @GetMapping("/{trackingNumber}")
    public ApiResponse<DeliveryResponse> getDelivery(@PathVariable String trackingNumber) {
        return ApiResponse.success(deliveryService.getDeliveryByTrackingNumber(trackingNumber));
    }

    /**
     * 주문 번호로 배송을 조회합니다.
     *
     * @param orderNumber 주문 번호
     * @return 배송 정보
     */
    @GetMapping("/order/{orderNumber}")
    public ApiResponse<DeliveryResponse> getDeliveryByOrder(@PathVariable String orderNumber) {
        return ApiResponse.success(deliveryService.getDeliveryByOrderNumber(orderNumber));
    }

    /**
     * 배송 상태를 변경합니다 (관리자 전용).
     *
     * @param trackingNumber 운송장 번호
     * @param request 상태 변경 요청
     * @return 업데이트된 배송 정보
     */
    @PutMapping("/{trackingNumber}/status")
    public ApiResponse<DeliveryResponse> updateDeliveryStatus(
            @PathVariable String trackingNumber,
            @Valid @RequestBody UpdateDeliveryStatusRequest request) {

        return ApiResponse.success(deliveryService.updateDeliveryStatus(trackingNumber, request));
    }
}
