package com.portal.universe.shoppingservice.coupon.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import com.portal.universe.shoppingservice.coupon.domain.Coupon;
import com.portal.universe.shoppingservice.coupon.domain.CouponStatus;
import com.portal.universe.shoppingservice.coupon.domain.UserCoupon;
import com.portal.universe.shoppingservice.coupon.domain.UserCouponStatus;
import com.portal.universe.shoppingservice.coupon.dto.CouponResponse;
import com.portal.universe.shoppingservice.coupon.dto.UserCouponResponse;
import com.portal.universe.shoppingservice.coupon.redis.CouponRedisService;
import com.portal.universe.shoppingservice.coupon.repository.CouponRepository;
import com.portal.universe.shoppingservice.coupon.repository.UserCouponRepository;
import com.portal.universe.shoppingservice.event.ShoppingEventPublisher;
import com.portal.universe.event.shopping.CouponIssuedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponRedisService couponRedisService;
    private final ShoppingEventPublisher eventPublisher;

    @Override
    public CouponResponse getCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.COUPON_NOT_FOUND));
        return CouponResponse.from(coupon);
    }

    @Override
    public List<CouponResponse> getAvailableCoupons() {
        Instant now = Instant.now();
        return couponRepository.findAvailableCoupons(CouponStatus.ACTIVE, now)
                .stream()
                .map(CouponResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public UserCouponResponse issueCoupon(Long couponId, String userId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.COUPON_NOT_FOUND));

        validateCouponForIssue(coupon);

        // Phase 1: Redis Lua Script — 원자적 재고 확보 + 중복 검증
        Long result = couponRedisService.issueCoupon(couponId, userId, coupon.getTotalQuantity());

        if (result == -1) {
            throw new CustomBusinessException(ShoppingErrorCode.COUPON_ALREADY_ISSUED);
        }
        if (result == 0) {
            throw new CustomBusinessException(ShoppingErrorCode.COUPON_EXHAUSTED);
        }

        // Phase 2: DB 영속화 (실패 시 Redis 원자적 보상)
        try {
            UserCoupon userCoupon = UserCoupon.builder()
                    .userId(userId)
                    .coupon(coupon)
                    .expiresAt(coupon.getExpiresAt())
                    .build();

            UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

            couponRepository.incrementIssuedQuantity(couponId);

            log.info("Issued coupon: couponId={}, userId={}, userCouponId={}",
                    couponId, userId, savedUserCoupon.getId());

            eventPublisher.publishCouponIssued(CouponIssuedEvent.newBuilder()
                    .setUserId(userId)
                    .setCouponCode(coupon.getCode())
                    .setCouponName(coupon.getName())
                    .setDiscountType(coupon.getDiscountType().name())
                    .setDiscountValue(coupon.getDiscountValue().intValue())
                    .setExpiresAt(coupon.getExpiresAt())
                    .build());

            return UserCouponResponse.from(savedUserCoupon);

        } catch (Exception e) {
            // Phase 3: Lua 원자적 보상 — 재고 복원 + 발급 기록 제거
            try {
                couponRedisService.rollbackIssuance(couponId, userId);
            } catch (Exception rollbackEx) {
                log.error("Redis rollback failed, reconciliation scheduler will fix: couponId={}, userId={}",
                        couponId, userId, rollbackEx);
            }
            throw e;
        }
    }

    private void validateCouponForIssue(Coupon coupon) {
        Instant now = Instant.now();

        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            throw new CustomBusinessException(ShoppingErrorCode.COUPON_INACTIVE);
        }
        if (now.isBefore(coupon.getStartsAt())) {
            throw new CustomBusinessException(ShoppingErrorCode.COUPON_NOT_STARTED);
        }
        if (now.isAfter(coupon.getExpiresAt())) {
            throw new CustomBusinessException(ShoppingErrorCode.COUPON_EXPIRED);
        }
        if (coupon.getIssuedQuantity() >= coupon.getTotalQuantity()) {
            throw new CustomBusinessException(ShoppingErrorCode.COUPON_EXHAUSTED);
        }
    }

    @Override
    public List<UserCouponResponse> getUserCoupons(String userId) {
        return userCouponRepository.findByUserId(userId)
                .stream()
                .map(UserCouponResponse::from)
                .toList();
    }

    @Override
    public List<UserCouponResponse> getAvailableUserCoupons(String userId) {
        Instant now = Instant.now();
        return userCouponRepository.findAvailableByUserId(userId, now)
                .stream()
                .map(UserCouponResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void useCoupon(Long userCouponId, Long orderId) {
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.USER_COUPON_NOT_FOUND));

        if (userCoupon.getStatus() == UserCouponStatus.USED) {
            throw new CustomBusinessException(ShoppingErrorCode.USER_COUPON_ALREADY_USED);
        }
        if (userCoupon.getStatus() == UserCouponStatus.EXPIRED ||
                Instant.now().isAfter(userCoupon.getExpiresAt())) {
            throw new CustomBusinessException(ShoppingErrorCode.USER_COUPON_EXPIRED);
        }

        userCoupon.use(orderId);
        userCouponRepository.save(userCoupon);

        log.info("Used coupon: userCouponId={}, orderId={}", userCouponId, orderId);
    }

    @Override
    public java.math.BigDecimal calculateDiscount(Long userCouponId, java.math.BigDecimal orderAmount) {
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.USER_COUPON_NOT_FOUND));

        return userCoupon.getCoupon().calculateDiscount(orderAmount);
    }

    @Override
    public void validateCouponForOrder(Long userCouponId, String userId, java.math.BigDecimal orderAmount) {
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.USER_COUPON_NOT_FOUND));

        // 소유자 확인
        if (!userCoupon.getUserId().equals(userId)) {
            throw new CustomBusinessException(ShoppingErrorCode.USER_COUPON_NOT_FOUND);
        }

        // 사용 가능 여부 확인
        if (!userCoupon.isUsable()) {
            if (userCoupon.getStatus() == UserCouponStatus.USED) {
                throw new CustomBusinessException(ShoppingErrorCode.USER_COUPON_ALREADY_USED);
            }
            throw new CustomBusinessException(ShoppingErrorCode.USER_COUPON_EXPIRED);
        }

        // 최소 주문 금액 확인
        Coupon coupon = userCoupon.getCoupon();
        if (coupon.getMinimumOrderAmount() != null
                && orderAmount.compareTo(coupon.getMinimumOrderAmount()) < 0) {
            throw new CustomBusinessException(ShoppingErrorCode.COUPON_MINIMUM_ORDER_NOT_MET);
        }
    }
}
