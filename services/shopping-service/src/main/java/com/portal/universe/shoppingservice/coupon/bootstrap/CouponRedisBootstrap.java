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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
                long ttlSeconds = Duration.between(Instant.now(), coupon.getExpiresAt()).getSeconds()
                        + TimeUnit.DAYS.toSeconds(1);

                // Stock 키: SETEX로 값 + TTL 원자적 설정 (expire 버그 우회)
                if (ttlSeconds > 0) {
                    couponRedisService.initializeCouponStock(coupon.getId(), remainingQuantity, ttlSeconds);
                } else {
                    couponRedisService.initializeCouponStock(coupon.getId(), remainingQuantity);
                }

                // Issued 키: 사용자 목록 동기화
                List<String> issuedUserIds = userCouponRepository.findUserIdsByCouponId(coupon.getId());
                for (String userId : issuedUserIds) {
                    couponRedisService.addIssuedUser(coupon.getId(), userId);
                }

                // Issued Set 키: Lua Script로 TTL 설정 (expire 버그 우회)
                if (ttlSeconds > 0 && !issuedUserIds.isEmpty()) {
                    couponRedisService.setIssuedKeyExpiration(coupon.getId(), ttlSeconds);
                }

                synced++;
                log.debug("Synced coupon: id={}, stock={}, issuedUsers={}, ttl={}s",
                        coupon.getId(), remainingQuantity, issuedUserIds.size(), ttlSeconds);
            } catch (Exception e) {
                log.error("Failed to sync coupon to Redis: couponId={}", coupon.getId(), e);
            }
        }

        log.info("Coupon Redis bootstrap completed: {}/{} coupons synced", synced, activeCoupons.size());
    }
}
