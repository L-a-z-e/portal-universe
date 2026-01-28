package com.portal.universe.shoppingservice.coupon.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingservice.coupon.dto.CouponCreateRequest;
import com.portal.universe.shoppingservice.coupon.dto.CouponResponse;
import com.portal.universe.shoppingservice.coupon.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 쿠폰(Coupon) 관련 관리자 API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/admin/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_SHOPPING_ADMIN', 'ROLE_SUPER_ADMIN')")
public class AdminCouponController {

    private final CouponService couponService;

    /**
     * 새로운 쿠폰을 생성합니다.
     */
    @PostMapping
    public ApiResponse<CouponResponse> createCoupon(@Valid @RequestBody CouponCreateRequest request) {
        return ApiResponse.success(couponService.createCoupon(request));
    }

    /**
     * 특정 쿠폰 정보를 조회합니다.
     */
    @GetMapping("/{couponId}")
    public ApiResponse<CouponResponse> getCoupon(@PathVariable Long couponId) {
        return ApiResponse.success(couponService.getCoupon(couponId));
    }

    /**
     * 쿠폰을 비활성화합니다.
     */
    @DeleteMapping("/{couponId}")
    public ApiResponse<Void> deactivateCoupon(@PathVariable Long couponId) {
        couponService.deactivateCoupon(couponId);
        return ApiResponse.success(null);
    }
}
