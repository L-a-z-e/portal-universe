package com.portal.universe.shoppingservice.coupon.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import com.portal.universe.shoppingservice.coupon.domain.Coupon;
import com.portal.universe.shoppingservice.coupon.domain.CouponStatus;
import com.portal.universe.shoppingservice.coupon.domain.UserCoupon;
import com.portal.universe.shoppingservice.coupon.domain.UserCouponStatus;
import com.portal.universe.shoppingservice.coupon.dto.CouponCreateRequest;
import com.portal.universe.shoppingservice.coupon.dto.CouponResponse;
import com.portal.universe.shoppingservice.coupon.dto.UserCouponResponse;
import com.portal.universe.shoppingservice.coupon.redis.CouponRedisService;
import com.portal.universe.shoppingservice.coupon.repository.CouponRepository;
import com.portal.universe.shoppingservice.coupon.repository.UserCouponRepository;
import com.portal.universe.shoppingservice.event.ShoppingEventPublisher;
import com.portal.universe.event.shopping.CouponIssuedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    public Page<CouponResponse> getAllCoupons(Pageable pageable) {
        return couponRepository.findAll(pageable)
                .map(CouponResponse::from);
    }

    @Override
    @Transactional
    public CouponResponse createCoupon(CouponCreateRequest request) {
        if (couponRepository.existsByCode(request.code())) {
            throw new CustomBusinessException(ShoppingErrorCode.COUPON_CODE_ALREADY_EXISTS);
        }

        Coupon coupon = Coupon.builder()
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .discountType(request.discountType())
                .discountValue(request.discountValue())
                .minimumOrderAmount(request.minimumOrderAmount())
                .maximumDiscountAmount(request.maximumDiscountAmount())
                .totalQuantity(request.totalQuantity())
                .startsAt(request.startsAt())
                .expiresAt(request.expiresAt())
                .build();

        Coupon savedCoupon = couponRepository.save(coupon);

        // Redis에 쿠폰 재고 초기화
        couponRedisService.initializeCouponStock(savedCoupon.getId(), savedCoupon.getTotalQuantity());

        log.info("Created coupon: id={}, code={}, quantity={}",
                savedCoupon.getId(), savedCoupon.getCode(), savedCoupon.getTotalQuantity());

        return CouponResponse.from(savedCoupon);
    }

    @Override
    public CouponResponse getCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.COUPON_NOT_FOUND));
        return CouponResponse.from(coupon);
    }

    @Override
    public List<CouponResponse> getAvailableCoupons() {
        LocalDateTime now = LocalDateTime.now();
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

        // Lua Script를 통한 원자적 발급
        Long result = couponRedisService.issueCoupon(couponId, userId, coupon.getTotalQuantity());

        if (result == -1) {
            throw new CustomBusinessException(ShoppingErrorCode.COUPON_ALREADY_ISSUED);
        }
        if (result == 0) {
            throw new CustomBusinessException(ShoppingErrorCode.COUPON_EXHAUSTED);
        }

        // DB에 발급 기록 저장
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .coupon(coupon)
                .expiresAt(coupon.getExpiresAt())
                .build();

        UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

        // 쿠폰 발급 수량 증가
        coupon.incrementIssuedQuantity();
        couponRepository.save(coupon);

        log.info("Issued coupon: couponId={}, userId={}, userCouponId={}",
                couponId, userId, savedUserCoupon.getId());

        // 쿠폰 발급 이벤트 발행
        eventPublisher.publishCouponIssued(new CouponIssuedEvent(
                Long.parseLong(userId),
                coupon.getCode(),
                coupon.getName(),
                coupon.getDiscountType().name(),
                coupon.getDiscountValue().intValue(),
                coupon.getExpiresAt()
        ));

        return UserCouponResponse.from(savedUserCoupon);
    }

    private void validateCouponForIssue(Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();

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
        LocalDateTime now = LocalDateTime.now();
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
                LocalDateTime.now().isAfter(userCoupon.getExpiresAt())) {
            throw new CustomBusinessException(ShoppingErrorCode.USER_COUPON_EXPIRED);
        }

        userCoupon.use(orderId);
        userCouponRepository.save(userCoupon);

        log.info("Used coupon: userCouponId={}, orderId={}", userCouponId, orderId);
    }

    @Override
    @Transactional
    public void deactivateCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.COUPON_NOT_FOUND));

        coupon.deactivate();
        couponRepository.save(coupon);

        // Redis 캐시 삭제
        couponRedisService.deleteCouponCache(couponId);

        log.info("Deactivated coupon: id={}", couponId);
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
