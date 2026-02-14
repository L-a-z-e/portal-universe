package com.portal.universe.shoppingsellerservice.coupon.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingsellerservice.common.exception.SellerErrorCode;
import com.portal.universe.shoppingsellerservice.coupon.domain.Coupon;
import com.portal.universe.shoppingsellerservice.coupon.dto.CouponCreateRequest;
import com.portal.universe.shoppingsellerservice.coupon.dto.CouponResponse;
import com.portal.universe.shoppingsellerservice.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

    @Override
    @Transactional
    public CouponResponse createCoupon(Long sellerId, CouponCreateRequest request) {
        if (couponRepository.existsByCode(request.code())) {
            throw new CustomBusinessException(SellerErrorCode.COUPON_CODE_ALREADY_EXISTS);
        }
        Coupon coupon = request.toEntity(sellerId);
        return CouponResponse.from(couponRepository.save(coupon));
    }

    @Override
    public CouponResponse getCoupon(Long sellerId, Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CustomBusinessException(SellerErrorCode.COUPON_NOT_FOUND));
        if (!coupon.getSellerId().equals(sellerId)) {
            throw new CustomBusinessException(SellerErrorCode.COUPON_NOT_OWNED);
        }
        return CouponResponse.from(coupon);
    }

    @Override
    public Page<CouponResponse> getSellerCoupons(Long sellerId, Pageable pageable) {
        return couponRepository.findBySellerId(sellerId, pageable)
                .map(CouponResponse::from);
    }

    @Override
    @Transactional
    public void deactivateCoupon(Long sellerId, Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CustomBusinessException(SellerErrorCode.COUPON_NOT_FOUND));
        if (!coupon.getSellerId().equals(sellerId)) {
            throw new CustomBusinessException(SellerErrorCode.COUPON_NOT_OWNED);
        }
        coupon.deactivate();
    }
}
