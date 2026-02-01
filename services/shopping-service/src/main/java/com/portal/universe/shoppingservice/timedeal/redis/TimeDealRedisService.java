package com.portal.universe.shoppingservice.timedeal.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeDealRedisService {

    private static final String TIMEDEAL_STOCK_KEY = "timedeal:stock:";
    private static final String TIMEDEAL_PURCHASED_KEY = "timedeal:purchased:";

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> timeDealPurchaseScript;

    /**
     * 타임딜 상품 재고를 Redis에 초기화합니다.
     */
    public void initializeStock(Long timeDealId, Long productId, int quantity) {
        String stockKey = buildStockKey(timeDealId, productId);
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(quantity));
        log.info("Initialized timedeal stock: dealId={}, productId={}, quantity={}",
                timeDealId, productId, quantity);
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
     * 타임딜 캐시 만료 시간을 설정합니다.
     */
    public void setExpiration(Long timeDealId, Long productId, long timeout, TimeUnit unit) {
        String stockKey = buildStockKey(timeDealId, productId);
        stringRedisTemplate.expire(stockKey, timeout, unit);
    }

    private String buildStockKey(Long timeDealId, Long productId) {
        return TIMEDEAL_STOCK_KEY + timeDealId + ":" + productId;
    }

    private String buildPurchasedKey(Long timeDealId, Long productId, String userId) {
        return TIMEDEAL_PURCHASED_KEY + timeDealId + ":" + productId + ":" + userId;
    }
}
