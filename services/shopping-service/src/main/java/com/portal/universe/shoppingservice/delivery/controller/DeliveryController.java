package com.portal.universe.shoppingservice.delivery.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingservice.delivery.dto.DeliveryResponse;
import com.portal.universe.shoppingservice.delivery.dto.UpdateDeliveryStatusRequest;
import com.portal.universe.shoppingservice.delivery.service.DeliveryService;
import com.portal.universe.commonlibrary.security.context.AuthUser;
import com.portal.universe.commonlibrary.security.context.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 배송 API를 제공하는 컨트롤러입니다.
 */
@Slf4j
@RestController
@RequestMapping("/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    /**
     * 운송장 번호로 배송을 조회합니다.
     *
     * @param trackingNumber 운송장 번호
     * @param userId 사용자 ID
     * @return 배송 정보
     */
    @GetMapping("/{trackingNumber}")
    public ApiResponse<DeliveryResponse> getDelivery(
            @PathVariable String trackingNumber,
            @CurrentUser AuthUser user) {
        return ApiResponse.success(deliveryService.getDeliveryByTrackingNumber(trackingNumber));
    }

    /**
     * 주문 번호로 배송을 조회합니다.
     *
     * @param orderNumber 주문 번호
     * @param userId 사용자 ID
     * @return 배송 정보
     */
    @GetMapping("/order/{orderNumber}")
    public ApiResponse<DeliveryResponse> getDeliveryByOrder(
            @PathVariable String orderNumber,
            @CurrentUser AuthUser user) {
        return ApiResponse.success(deliveryService.getDeliveryByOrderNumber(orderNumber));
    }

    /**
     * 배송 상태를 변경합니다 (관리자 전용).
     *
     * @param trackingNumber 운송장 번호
     * @param request 상태 변경 요청
     * @param adminId 관리자 ID
     * @return 업데이트된 배송 정보
     */
    @PutMapping("/{trackingNumber}/status")
    public ApiResponse<DeliveryResponse> updateDeliveryStatus(
            @PathVariable String trackingNumber,
            @Valid @RequestBody UpdateDeliveryStatusRequest request,
            @CurrentUser AuthUser user) {
        log.info("Delivery status update requested: trackingNumber={}, newStatus={}, adminId={}",
                trackingNumber, request.status(), user.uuid());
        return ApiResponse.success(deliveryService.updateDeliveryStatus(trackingNumber, request));
    }
}
