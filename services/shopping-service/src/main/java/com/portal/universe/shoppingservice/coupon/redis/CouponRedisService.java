package com.portal.universe.shoppingservice.coupon.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponRedisService {

    private static final String COUPON_STOCK_KEY = "coupon:stock:";
    private static final String COUPON_ISSUED_KEY = "coupon:issued:";

    /**
     * Lua Script로 EXPIRE 명령을 직접 실행합니다.
     * RedissonConnection이 expire/pExpire default 메서드를 제대로 override하지 않아
     * DefaultedRedisConnection에서 무한 재귀(StackOverflow)가 발생하는 버그를 우회합니다.
     */
    private static final String EXPIRE_LUA = "return redis.call('EXPIRE', KEYS[1], ARGV[1])";
    private static final DefaultRedisScript<Long> EXPIRE_SCRIPT;

    static {
        EXPIRE_SCRIPT = new DefaultRedisScript<>(EXPIRE_LUA, Long.class);
    }

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> couponIssueScript;

    /**
     * 쿠폰 재고를 Redis에 초기화합니다. (TTL 없음)
     */
    public void initializeCouponStock(Long couponId, int quantity) {
        String stockKey = COUPON_STOCK_KEY + couponId;
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(quantity));
        log.info("Initialized coupon stock: couponId={}, quantity={}", couponId, quantity);
    }

    /**
     * 쿠폰 재고를 Redis에 초기화합니다. (TTL 포함, SETEX 사용)
     * RedissonConnection의 expire 버그를 우회하기 위해 SET with EX 옵션으로 값과 TTL을 원자적 설정.
     */
    public void initializeCouponStock(Long couponId, int quantity, long ttlSeconds) {
        String stockKey = COUPON_STOCK_KEY + couponId;
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(quantity), ttlSeconds, TimeUnit.SECONDS);
        log.info("Initialized coupon stock with TTL: couponId={}, quantity={}, ttl={}s", couponId, quantity, ttlSeconds);
    }

    /**
     * Lua Script를 사용하여 원자적으로 쿠폰을 발급합니다.
     *
     * @return 1: 성공, 0: 재고 소진, -1: 이미 발급됨
     */
    public Long issueCoupon(Long couponId, String userId, int maxQuantity) {
        String stockKey = COUPON_STOCK_KEY + couponId;
        String issuedKey = COUPON_ISSUED_KEY + couponId;

        Long result = stringRedisTemplate.execute(
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
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(issuedKey, userId);
        return Boolean.TRUE.equals(isMember);
    }

    /**
     * 현재 쿠폰 재고를 조회합니다.
     */
    public int getStock(Long couponId) {
        String stockKey = COUPON_STOCK_KEY + couponId;
        Object stock = stringRedisTemplate.opsForValue().get(stockKey);
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
        Long size = stringRedisTemplate.opsForSet().size(issuedKey);
        return size != null ? size : 0;
    }

    /**
     * 쿠폰 재고를 증가시킵니다 (발급 취소 시).
     */
    public void incrementStock(Long couponId) {
        String stockKey = COUPON_STOCK_KEY + couponId;
        stringRedisTemplate.opsForValue().increment(stockKey);
    }

    /**
     * 사용자의 쿠폰 발급 기록을 제거합니다 (발급 취소 시).
     */
    public void removeIssuedUser(Long couponId, String userId) {
        String issuedKey = COUPON_ISSUED_KEY + couponId;
        stringRedisTemplate.opsForSet().remove(issuedKey, userId);
    }

    /**
     * 발급된 사용자를 Redis Set에 추가합니다 (bootstrap 용).
     */
    public void addIssuedUser(Long couponId, String userId) {
        String issuedKey = COUPON_ISSUED_KEY + couponId;
        stringRedisTemplate.opsForSet().add(issuedKey, userId);
    }

    /**
     * 쿠폰 관련 캐시를 삭제합니다.
     */
    public void deleteCouponCache(Long couponId) {
        String stockKey = COUPON_STOCK_KEY + couponId;
        String issuedKey = COUPON_ISSUED_KEY + couponId;
        stringRedisTemplate.delete(Arrays.asList(stockKey, issuedKey));
        log.info("Deleted coupon cache: couponId={}", couponId);
    }

    /**
     * Issued Set 키에 TTL을 설정합니다.
     * Lua Script로 EXPIRE 명령을 직접 실행하여 RedissonConnection 버그를 우회합니다.
     */
    public void setIssuedKeyExpiration(Long couponId, long ttlSeconds) {
        String issuedKey = COUPON_ISSUED_KEY + couponId;
        expireViaLua(issuedKey, ttlSeconds);
    }

    /**
     * Lua Script를 통해 Redis EXPIRE 명령을 안전하게 실행합니다.
     * RedissonConnection의 expire/pExpire 무한 재귀 버그를 우회합니다.
     */
    private void expireViaLua(String key, long ttlSeconds) {
        stringRedisTemplate.execute(EXPIRE_SCRIPT, Collections.singletonList(key), String.valueOf(ttlSeconds));
    }
}
