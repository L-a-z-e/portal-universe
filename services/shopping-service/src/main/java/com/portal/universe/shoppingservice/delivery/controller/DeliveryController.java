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

    @GetMapping("/{trackingNumber}")
    public ApiResponse<DeliveryResponse> getDelivery(
            @PathVariable String trackingNumber,
            @CurrentUser AuthUser user) {
        return ApiResponse.success(deliveryService.getDeliveryByTrackingNumber(trackingNumber, user.uuid()));
    }

    @GetMapping("/order/{orderNumber}")
    public ApiResponse<DeliveryResponse> getDeliveryByOrder(
            @PathVariable String orderNumber,
            @CurrentUser AuthUser user) {
        return ApiResponse.success(deliveryService.getDeliveryByOrderNumber(orderNumber, user.uuid()));
    }

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
