package com.portal.universe.shoppingsellerservice.coupon.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.event.seller.CouponCreatedEvent;
import com.portal.universe.event.seller.CouponDeletedEvent;
import com.portal.universe.shoppingsellerservice.common.exception.SellerErrorCode;
import com.portal.universe.shoppingsellerservice.coupon.domain.Coupon;
import com.portal.universe.shoppingsellerservice.coupon.dto.CouponCreateRequest;
import com.portal.universe.shoppingsellerservice.coupon.dto.CouponResponse;
import com.portal.universe.shoppingsellerservice.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public CouponResponse createCoupon(Long sellerId, CouponCreateRequest request) {
        if (couponRepository.existsByCode(request.code())) {
            throw new CustomBusinessException(SellerErrorCode.COUPON_CODE_ALREADY_EXISTS);
        }
        Coupon coupon = request.toEntity(sellerId);
        Coupon saved = couponRepository.save(coupon);

        eventPublisher.publishEvent(CouponCreatedEvent.newBuilder()
                .setCouponId(saved.getId())
                .setSellerId(sellerId)
                .setCode(saved.getCode())
                .setName(saved.getName())
                .setDescription(saved.getDescription())
                .setDiscountType(saved.getDiscountType().name())
                .setDiscountValue(saved.getDiscountValue())
                .setMinimumOrderAmount(saved.getMinimumOrderAmount())
                .setMaximumDiscountAmount(saved.getMaximumDiscountAmount())
                .setTotalQuantity(saved.getTotalQuantity())
                .setStartsAt(saved.getStartsAt())
                .setExpiresAt(saved.getExpiresAt())
                .setTimestamp(Instant.now())
                .build());

        return CouponResponse.from(saved);
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

        eventPublisher.publishEvent(CouponDeletedEvent.newBuilder()
                .setCouponId(couponId)
                .setSellerId(sellerId)
                .setTimestamp(Instant.now())
                .build());
    }
}
