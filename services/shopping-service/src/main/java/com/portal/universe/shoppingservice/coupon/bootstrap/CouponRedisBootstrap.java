package com.portal.universe.shoppingservice.coupon.bootstrap;

import com.portal.universe.shoppingservice.coupon.domain.Coupon;
import com.portal.universe.shoppingservice.coupon.domain.CouponStatus;
import com.portal.universe.shoppingservice.coupon.redis.CouponRedisService;
import com.portal.universe.shoppingservice.coupon.repository.CouponRepository;
import com.portal.universe.shoppingservice.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponRedisBootstrap implements ApplicationRunner {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponRedisService couponRedisService;

    @Override
    public void run(ApplicationArguments args) {
        syncCouponStockToRedis();
    }

    private void syncCouponStockToRedis() {
        List<Coupon> activeCoupons = couponRepository.findByStatus(CouponStatus.ACTIVE);

        if (activeCoupons.isEmpty()) {
            log.info("No active coupons to sync to Redis");
            return;
        }

        int synced = 0;
        for (Coupon coupon : activeCoupons) {
            try {
                int remainingQuantity = coupon.getRemainingQuantity();
                couponRedisService.initializeCouponStock(coupon.getId(), remainingQuantity);

                List<String> issuedUserIds = userCouponRepository.findUserIdsByCouponId(coupon.getId());
                for (String userId : issuedUserIds) {
                    couponRedisService.addIssuedUser(coupon.getId(), userId);
                }

                synced++;
                log.debug("Synced coupon: id={}, stock={}, issuedUsers={}",
                        coupon.getId(), remainingQuantity, issuedUserIds.size());
            } catch (Exception e) {
                log.error("Failed to sync coupon to Redis: couponId={}", coupon.getId(), e);
            }
        }

        log.info("Coupon Redis bootstrap completed: {}/{} coupons synced", synced, activeCoupons.size());
    }
}
