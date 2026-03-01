package com.portal.universe.shoppingservice.coupon.scheduler;

import com.portal.universe.shoppingservice.common.annotation.DistributedLock;
import com.portal.universe.shoppingservice.coupon.domain.Coupon;
import com.portal.universe.shoppingservice.coupon.domain.CouponStatus;
import com.portal.universe.shoppingservice.coupon.redis.CouponRedisService;
import com.portal.universe.shoppingservice.coupon.repository.CouponRepository;
import com.portal.universe.shoppingservice.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponReconciliationScheduler {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponRedisService couponRedisService;

    /**
     * 5분마다 Redis-DB 쿠폰 재고 정합성을 검증하고 불일치를 보정합니다.
     * DB를 source of truth로 삼아 Redis를 재설정합니다.
     *
     * 보정 대상:
     * - JVM 크래시로 Redis 보상이 실행되지 못한 경우
     * - Redis 보상 자체가 실패한 경우
     * - Redis-DB 간 네트워크 이슈로 불일치가 발생한 경우
     */
    @Scheduled(fixedDelay = 300_000) // 5분
    @DistributedLock(key = "'scheduler:coupon:reconciliation'", waitTime = 0, leaseTime = 240)
    public void reconcileCouponStock() {
        List<Coupon> activeCoupons = couponRepository.findByStatus(CouponStatus.ACTIVE);

        if (activeCoupons.isEmpty()) {
            return;
        }

        int mismatchCount = 0;
        for (Coupon coupon : activeCoupons) {
            try {
                if (reconcileSingleCoupon(coupon)) {
                    mismatchCount++;
                }
            } catch (Exception e) {
                log.error("Failed to reconcile coupon: couponId={}", coupon.getId(), e);
            }
        }

        if (mismatchCount > 0) {
            log.warn("Coupon reconciliation completed: {}/{} coupons had mismatches",
                    mismatchCount, activeCoupons.size());
        } else {
            log.debug("Coupon reconciliation completed: all {} coupons consistent", activeCoupons.size());
        }
    }

    private boolean reconcileSingleCoupon(Coupon coupon) {
        long redisIssuedCount = couponRedisService.getIssuedCount(coupon.getId());
        long dbIssuedCount = userCouponRepository.countByCouponId(coupon.getId());

        if (redisIssuedCount == dbIssuedCount) {
            return false;
        }

        log.warn("Coupon stock mismatch detected: couponId={}, redisIssued={}, dbIssued={}, totalQty={}",
                coupon.getId(), redisIssuedCount, dbIssuedCount, coupon.getTotalQuantity());

        // DB 기준으로 Redis 재고 재설정
        int correctStock = coupon.getTotalQuantity() - (int) dbIssuedCount;
        couponRedisService.initializeCouponStock(coupon.getId(), Math.max(correctStock, 0));

        // DB 기준으로 Issued Set 재구성
        List<String> dbIssuedUserIds = userCouponRepository.findUserIdsByCouponId(coupon.getId());
        couponRedisService.rebuildIssuedSet(coupon.getId(), dbIssuedUserIds);

        log.info("Coupon reconciled: couponId={}, correctedStock={}, issuedUsers={}",
                coupon.getId(), correctStock, dbIssuedUserIds.size());
        return true;
    }
}
