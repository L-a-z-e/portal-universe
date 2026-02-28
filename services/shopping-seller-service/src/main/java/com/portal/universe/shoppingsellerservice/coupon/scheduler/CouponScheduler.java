package com.portal.universe.shoppingsellerservice.coupon.scheduler;

import com.portal.universe.event.seller.CouponUpdatedEvent;
import com.portal.universe.shoppingsellerservice.common.annotation.DistributedLock;
import com.portal.universe.shoppingsellerservice.coupon.domain.Coupon;
import com.portal.universe.shoppingsellerservice.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponScheduler {

    private final CouponRepository couponRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedRate = 60000)
    @DistributedLock(key = "'scheduler:seller:coupon:expiry'", waitTime = 0, leaseTime = 55)
    @Transactional
    public void expireOverdueCoupons() {
        Instant now = Instant.now();
        log.debug("Running Coupon expiry scheduler at {}", now);

        List<Coupon> expiredCoupons = couponRepository.findExpiredCoupons(now);
        if (expiredCoupons.isEmpty()) return;

        expiredCoupons.forEach(Coupon::expire);
        couponRepository.saveAll(expiredCoupons);

        for (Coupon coupon : expiredCoupons) {
            eventPublisher.publishEvent(CouponUpdatedEvent.newBuilder()
                    .setCouponId(coupon.getId())
                    .setSellerId(coupon.getSellerId())
                    .setCode(coupon.getCode())
                    .setName(coupon.getName())
                    .setDescription(coupon.getDescription())
                    .setDiscountType(coupon.getDiscountType().name())
                    .setDiscountValue(coupon.getDiscountValue())
                    .setMinimumOrderAmount(coupon.getMinimumOrderAmount())
                    .setMaximumDiscountAmount(coupon.getMaximumDiscountAmount())
                    .setTotalQuantity(coupon.getTotalQuantity())
                    .setStartsAt(coupon.getStartsAt())
                    .setExpiresAt(coupon.getExpiresAt())
                    .setStatus(coupon.getStatus().name())
                    .setTimestamp(Instant.now())
                    .build());

            log.info("Expired coupon: id={}, code={}", coupon.getId(), coupon.getCode());
        }
    }
}
