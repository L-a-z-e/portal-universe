package com.portal.universe.shoppingservice.coupon.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingservice.coupon.dto.CouponResponse;
import com.portal.universe.shoppingservice.coupon.dto.UserCouponResponse;
import com.portal.universe.shoppingservice.coupon.service.CouponService;
import com.portal.universe.commonlibrary.security.context.AuthUser;
import com.portal.universe.commonlibrary.security.context.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 쿠폰(Coupon) 관련 사용자 API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /**
     * 발급 가능한 쿠폰 목록을 조회합니다.
     */
    @GetMapping
    public ApiResponse<List<CouponResponse>> getAvailableCoupons() {
        return ApiResponse.success(couponService.getAvailableCoupons());
    }

    /**
     * 특정 쿠폰 정보를 조회합니다.
     */
    @GetMapping("/{couponId}")
    public ApiResponse<CouponResponse> getCoupon(@PathVariable Long couponId) {
        return ApiResponse.success(couponService.getCoupon(couponId));
    }

    /**
     * 선착순으로 쿠폰을 발급받습니다.
     */
    @PostMapping("/{couponId}/issue")
    public ApiResponse<UserCouponResponse> issueCoupon(
            @PathVariable Long couponId,
            @CurrentUser AuthUser user) {
        return ApiResponse.success(couponService.issueCoupon(couponId, user.uuid()));
    }

    /**
     * 내 쿠폰 목록을 조회합니다.
     */
    @GetMapping("/my")
    public ApiResponse<List<UserCouponResponse>> getMyCoupons(@CurrentUser AuthUser user) {
        return ApiResponse.success(couponService.getUserCoupons(user.uuid()));
    }

    /**
     * 내 사용 가능한 쿠폰 목록을 조회합니다.
     */
    @GetMapping("/my/available")
    public ApiResponse<List<UserCouponResponse>> getMyAvailableCoupons(@CurrentUser AuthUser user) {
        return ApiResponse.success(couponService.getAvailableUserCoupons(user.uuid()));
    }
}
