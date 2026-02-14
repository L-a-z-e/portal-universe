package com.portal.universe.shoppingsellerservice.coupon.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingsellerservice.coupon.dto.CouponCreateRequest;
import com.portal.universe.shoppingsellerservice.coupon.dto.CouponResponse;
import com.portal.universe.shoppingsellerservice.coupon.service.CouponService;
import com.portal.universe.shoppingsellerservice.seller.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final SellerService sellerService;

    @GetMapping
    public ApiResponse<Page<CouponResponse>> getSellerCoupons(
            @AuthenticationPrincipal String userId,
            Pageable pageable) {
        Long sellerId = getSellerIdFromUser(userId);
        return ApiResponse.success(couponService.getSellerCoupons(sellerId, pageable));
    }

    @GetMapping("/{couponId}")
    public ApiResponse<CouponResponse> getCoupon(
            @AuthenticationPrincipal String userId,
            @PathVariable Long couponId) {
        Long sellerId = getSellerIdFromUser(userId);
        return ApiResponse.success(couponService.getCoupon(sellerId, couponId));
    }

    @PostMapping
    public ApiResponse<CouponResponse> createCoupon(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody CouponCreateRequest request) {
        Long sellerId = getSellerIdFromUser(userId);
        return ApiResponse.success(couponService.createCoupon(sellerId, request));
    }

    @DeleteMapping("/{couponId}")
    public ApiResponse<Void> deactivateCoupon(
            @AuthenticationPrincipal String userId,
            @PathVariable Long couponId) {
        Long sellerId = getSellerIdFromUser(userId);
        couponService.deactivateCoupon(sellerId, couponId);
        return ApiResponse.success(null);
    }

    private Long getSellerIdFromUser(String userId) {
        return sellerService.getMyInfo(userId).id();
    }
}
