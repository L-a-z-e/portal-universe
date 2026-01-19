package com.portal.universe.shoppingservice.coupon.service;

import com.portal.universe.shoppingservice.coupon.dto.CouponCreateRequest;
import com.portal.universe.shoppingservice.coupon.dto.CouponResponse;
import com.portal.universe.shoppingservice.coupon.dto.UserCouponResponse;

import java.util.List;

public interface CouponService {

    /**
     * 새로운 쿠폰을 생성합니다. (관리자)
     */
    CouponResponse createCoupon(CouponCreateRequest request);

    /**
     * 쿠폰 ID로 쿠폰을 조회합니다.
     */
    CouponResponse getCoupon(Long couponId);

    /**
     * 발급 가능한 모든 쿠폰을 조회합니다.
     */
    List<CouponResponse> getAvailableCoupons();

    /**
     * 선착순으로 쿠폰을 발급합니다.
     */
    UserCouponResponse issueCoupon(Long couponId, Long userId);

    /**
     * 사용자의 쿠폰 목록을 조회합니다.
     */
    List<UserCouponResponse> getUserCoupons(Long userId);

    /**
     * 사용자의 사용 가능한 쿠폰 목록을 조회합니다.
     */
    List<UserCouponResponse> getAvailableUserCoupons(Long userId);

    /**
     * 쿠폰을 사용합니다.
     */
    void useCoupon(Long userCouponId, Long orderId);

    /**
     * 쿠폰을 비활성화합니다. (관리자)
     */
    void deactivateCoupon(Long couponId);
}
