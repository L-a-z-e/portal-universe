package com.portal.universe.shoppingservice.timedeal.redis;

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
public class TimeDealRedisService {

    private static final String TIMEDEAL_STOCK_KEY = "timedeal:stock:";
    private static final String TIMEDEAL_PURCHASED_KEY = "timedeal:purchased:";

    /**
     * Lua Script로 EXPIRE 명령을 직접 실행합니다.
     * RedissonConnection의 expire/pExpire 무한 재귀 버그를 우회합니다.
     */
    private static final String EXPIRE_LUA = "return redis.call('EXPIRE', KEYS[1], ARGV[1])";
    private static final DefaultRedisScript<Long> EXPIRE_SCRIPT;

    static {
        EXPIRE_SCRIPT = new DefaultRedisScript<>(EXPIRE_LUA, Long.class);
    }

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> timeDealPurchaseScript;

    /**
     * 타임딜 상품 재고를 Redis에 초기화합니다. (TTL 없음)
     */
    public void initializeStock(Long timeDealId, Long productId, int quantity) {
        String stockKey = buildStockKey(timeDealId, productId);
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(quantity));
        log.info("Initialized timedeal stock: dealId={}, productId={}, quantity={}",
                timeDealId, productId, quantity);
    }

    /**
     * 타임딜 상품 재고를 Redis에 초기화합니다. (TTL 포함, SETEX 사용)
     */
    public void initializeStock(Long timeDealId, Long productId, int quantity, long ttlSeconds) {
        String stockKey = buildStockKey(timeDealId, productId);
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(quantity), ttlSeconds, TimeUnit.SECONDS);
        log.info("Initialized timedeal stock with TTL: dealId={}, productId={}, quantity={}, ttl={}s",
                timeDealId, productId, quantity, ttlSeconds);
    }

    /**
     * Lua Script를 사용하여 원자적으로 타임딜 상품을 구매합니다.
     *
     * @return > 0: 구매 성공 (남은 재고), 0: 재고 소진, -1: 구매 제한 초과
     */
    public Long purchaseProduct(Long timeDealId, Long productId, String userId,
                                 int requestedQuantity, int maxPerUser) {
        String stockKey = buildStockKey(timeDealId, productId);
        String purchasedKey = buildPurchasedKey(timeDealId, productId, userId);

        Long result = stringRedisTemplate.execute(
                timeDealPurchaseScript,
                Arrays.asList(stockKey, purchasedKey),
                String.valueOf(requestedQuantity),
                String.valueOf(maxPerUser)
        );

        log.debug("TimeDeal purchase result: dealId={}, productId={}, userId={}, result={}",
                timeDealId, productId, userId, result);
        return result;
    }

    /**
     * 현재 타임딜 상품 재고를 조회합니다.
     */
    public int getStock(Long timeDealId, Long productId) {
        String stockKey = buildStockKey(timeDealId, productId);
        String stock = stringRedisTemplate.opsForValue().get(stockKey);
        if (stock == null) {
            return 0;
        }
        return Integer.parseInt(stock);
    }

    /**
     * 사용자의 현재 구매 수량을 조회합니다.
     */
    public int getUserPurchasedQuantity(Long timeDealId, Long productId, String userId) {
        String purchasedKey = buildPurchasedKey(timeDealId, productId, userId);
        String purchased = stringRedisTemplate.opsForValue().get(purchasedKey);
        if (purchased == null) {
            return 0;
        }
        return Integer.parseInt(purchased);
    }

    /**
     * 재고를 롤백합니다 (구매 취소 시).
     */
    public void rollbackStock(Long timeDealId, Long productId, String userId, int quantity) {
        String stockKey = buildStockKey(timeDealId, productId);
        String purchasedKey = buildPurchasedKey(timeDealId, productId, userId);

        stringRedisTemplate.opsForValue().increment(stockKey, quantity);
        stringRedisTemplate.opsForValue().decrement(purchasedKey, quantity);

        log.info("Rolled back timedeal stock: dealId={}, productId={}, userId={}, quantity={}",
                timeDealId, productId, userId, quantity);
    }

    /**
     * 타임딜 캐시를 삭제합니다.
     */
    public void deleteTimeDealCache(Long timeDealId, Long productId) {
        String pattern = TIMEDEAL_STOCK_KEY + timeDealId + ":" + productId;
        stringRedisTemplate.delete(pattern);
        log.info("Deleted timedeal cache: dealId={}, productId={}", timeDealId, productId);
    }

    /**
     * Purchased 키에 TTL을 설정합니다.
     * Lua Script로 EXPIRE 명령을 직접 실행하여 RedissonConnection 버그를 우회합니다.
     */
    public void setPurchasedKeyExpiration(Long timeDealId, Long productId, String userId, long ttlSeconds) {
        String purchasedKey = buildPurchasedKey(timeDealId, productId, userId);
        expireViaLua(purchasedKey, ttlSeconds);
    }

    /**
     * Lua Script를 통해 Redis EXPIRE 명령을 안전하게 실행합니다.
     */
    private void expireViaLua(String key, long ttlSeconds) {
        stringRedisTemplate.execute(EXPIRE_SCRIPT, Collections.singletonList(key), String.valueOf(ttlSeconds));
    }

    private String buildStockKey(Long timeDealId, Long productId) {
        return TIMEDEAL_STOCK_KEY + timeDealId + ":" + productId;
    }

    private String buildPurchasedKey(Long timeDealId, Long productId, String userId) {
        return TIMEDEAL_PURCHASED_KEY + timeDealId + ":" + productId + ":" + userId;
    }
}
