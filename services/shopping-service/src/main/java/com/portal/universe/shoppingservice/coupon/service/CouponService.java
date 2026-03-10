package com.portal.universe.shoppingservice.coupon.service;

import com.portal.universe.shoppingservice.coupon.dto.CouponResponse;
import com.portal.universe.shoppingservice.coupon.dto.UserCouponResponse;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService {

    CouponResponse getCoupon(Long couponId);

    List<CouponResponse> getAvailableCoupons();

    UserCouponResponse issueCoupon(Long couponId, String userId);

    List<UserCouponResponse> getUserCoupons(String userId);

    List<UserCouponResponse> getAvailableUserCoupons(String userId);

    void useCoupon(Long userCouponId, Long orderId);

    /**
     * @param userCouponId 사용자 쿠폰 ID
     * @param orderAmount 주문 총 금액
     * @return 할인 금액
     */
    BigDecimal calculateDiscount(Long userCouponId, BigDecimal orderAmount);

    /**
     * @param userCouponId 사용자 쿠폰 ID
     * @param userId 사용자 ID
     * @param orderAmount 주문 총 금액
     */
    void validateCouponForOrder(Long userCouponId, String userId, BigDecimal orderAmount);
}
