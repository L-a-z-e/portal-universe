package com.portal.universe.shoppingsellerservice.coupon.service;

import com.portal.universe.shoppingsellerservice.coupon.dto.CouponCreateRequest;
import com.portal.universe.shoppingsellerservice.coupon.dto.CouponResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CouponService {
    CouponResponse createCoupon(Long sellerId, CouponCreateRequest request);
    CouponResponse getCoupon(Long sellerId, Long couponId);
    Page<CouponResponse> getSellerCoupons(Long sellerId, Pageable pageable);
    void deactivateCoupon(Long sellerId, Long couponId);
}
