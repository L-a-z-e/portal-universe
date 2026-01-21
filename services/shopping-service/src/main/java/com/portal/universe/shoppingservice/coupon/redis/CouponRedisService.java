package com.portal.universe.shoppingservice.coupon.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponRedisService {

    private static final String COUPON_STOCK_KEY = "coupon:stock:";
    private static final String COUPON_ISSUED_KEY = "coupon:issued:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<Long> couponIssueScript;

    /**
     * 쿠폰 재고를 Redis에 초기화합니다.
     */
    public void initializeCouponStock(Long couponId, int quantity) {
        String stockKey = COUPON_STOCK_KEY + couponId;
        redisTemplate.opsForValue().set(stockKey, quantity);
        log.info("Initialized coupon stock: couponId={}, quantity={}", couponId, quantity);
    }

    /**
     * Lua Script를 사용하여 원자적으로 쿠폰을 발급합니다.
     *
     * @return 1: 성공, 0: 재고 소진, -1: 이미 발급됨
     */
    public Long issueCoupon(Long couponId, String userId, int maxQuantity) {
        String stockKey = COUPON_STOCK_KEY + couponId;
        String issuedKey = COUPON_ISSUED_KEY + couponId;

        Long result = redisTemplate.execute(
                couponIssueScript,
                Arrays.asList(stockKey, issuedKey),
                String.valueOf(userId),
                String.valueOf(maxQuantity)
        );

        log.debug("Coupon issue result: couponId={}, userId={}, result={}", couponId, userId, result);
        return result;
    }

    /**
     * 쿠폰이 이미 발급되었는지 확인합니다.
     */
    public boolean isAlreadyIssued(Long couponId, String userId) {
        String issuedKey = COUPON_ISSUED_KEY + couponId;
        Boolean isMember = redisTemplate.opsForSet().isMember(issuedKey, userId);
        return Boolean.TRUE.equals(isMember);
    }

    /**
     * 현재 쿠폰 재고를 조회합니다.
     */
    public int getStock(Long couponId) {
        String stockKey = COUPON_STOCK_KEY + couponId;
        Object stock = redisTemplate.opsForValue().get(stockKey);
        if (stock == null) {
            return 0;
        }
        return Integer.parseInt(stock.toString());
    }

    /**
     * 발급된 쿠폰 수를 조회합니다.
     */
    public long getIssuedCount(Long couponId) {
        String issuedKey = COUPON_ISSUED_KEY + couponId;
        Long size = redisTemplate.opsForSet().size(issuedKey);
        return size != null ? size : 0;
    }

    /**
     * 쿠폰 재고를 증가시킵니다 (발급 취소 시).
     */
    public void incrementStock(Long couponId) {
        String stockKey = COUPON_STOCK_KEY + couponId;
        redisTemplate.opsForValue().increment(stockKey);
    }

    /**
     * 사용자의 쿠폰 발급 기록을 제거합니다 (발급 취소 시).
     */
    public void removeIssuedUser(Long couponId, String userId) {
        String issuedKey = COUPON_ISSUED_KEY + couponId;
        redisTemplate.opsForSet().remove(issuedKey, userId);
    }

    /**
     * 쿠폰 관련 캐시를 삭제합니다.
     */
    public void deleteCouponCache(Long couponId) {
        String stockKey = COUPON_STOCK_KEY + couponId;
        String issuedKey = COUPON_ISSUED_KEY + couponId;
        redisTemplate.delete(Arrays.asList(stockKey, issuedKey));
        log.info("Deleted coupon cache: couponId={}", couponId);
    }

    /**
     * 쿠폰 캐시 만료 시간을 설정합니다.
     */
    public void setCouponExpiration(Long couponId, long timeout, TimeUnit unit) {
        String stockKey = COUPON_STOCK_KEY + couponId;
        String issuedKey = COUPON_ISSUED_KEY + couponId;
        redisTemplate.expire(stockKey, timeout, unit);
        redisTemplate.expire(issuedKey, timeout, unit);
    }
}
