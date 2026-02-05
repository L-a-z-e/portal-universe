package com.portal.universe.shoppingservice.timedeal.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeDealRedisServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private DefaultRedisScript<Long> timeDealPurchaseScript;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TimeDealRedisService timeDealRedisService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("should_initializeStock_when_called")
    void should_initializeStock_when_called() {
        // given
        Long timeDealId = 1L;
        Long productId = 10L;
        int quantity = 50;

        // when
        timeDealRedisService.initializeStock(timeDealId, productId, quantity);

        // then
        verify(valueOperations).set("timedeal:stock:1:10", "50");
    }

    @Test
    @DisplayName("should_returnPositive_when_purchaseProductSucceeds")
    void should_returnPositive_when_purchaseProductSucceeds() {
        // given
        Long timeDealId = 1L;
        Long productId = 10L;
        String userId = "user-1";
        int requestedQuantity = 2;
        int maxPerUser = 5;

        when(stringRedisTemplate.execute(eq(timeDealPurchaseScript), anyList(), any(), any()))
                .thenReturn(48L);

        // when
        Long result = timeDealRedisService.purchaseProduct(timeDealId, productId, userId, requestedQuantity, maxPerUser);

        // then
        assertThat(result).isEqualTo(48L);
        verify(stringRedisTemplate).execute(
                eq(timeDealPurchaseScript),
                eq(Arrays.asList("timedeal:stock:1:10", "timedeal:purchased:1:10:user-1")),
                eq("2"),
                eq("5")
        );
    }

    @Test
    @DisplayName("should_returnZero_when_productSoldOut")
    void should_returnZero_when_productSoldOut() {
        // given
        when(stringRedisTemplate.execute(eq(timeDealPurchaseScript), anyList(), any(), any()))
                .thenReturn(0L);

        // when
        Long result = timeDealRedisService.purchaseProduct(1L, 10L, "user-1", 1, 5);

        // then
        assertThat(result).isEqualTo(0L);
    }

    @Test
    @DisplayName("should_returnStock_when_stockExists")
    void should_returnStock_when_stockExists() {
        // given
        when(valueOperations.get("timedeal:stock:1:10")).thenReturn("30");

        // when
        int stock = timeDealRedisService.getStock(1L, 10L);

        // then
        assertThat(stock).isEqualTo(30);
    }

    @Test
    @DisplayName("should_returnUserPurchasedQuantity_when_exists")
    void should_returnUserPurchasedQuantity_when_exists() {
        // given
        when(valueOperations.get("timedeal:purchased:1:10:user-1")).thenReturn("3");

        // when
        int purchased = timeDealRedisService.getUserPurchasedQuantity(1L, 10L, "user-1");

        // then
        assertThat(purchased).isEqualTo(3);
    }

    @Test
    @DisplayName("should_rollbackStock_when_called")
    void should_rollbackStock_when_called() {
        // given
        Long timeDealId = 1L;
        Long productId = 10L;
        String userId = "user-1";
        int quantity = 2;

        // when
        timeDealRedisService.rollbackStock(timeDealId, productId, userId, quantity);

        // then
        verify(valueOperations).increment("timedeal:stock:1:10", 2);
        verify(valueOperations).decrement("timedeal:purchased:1:10:user-1", 2);
    }

    @Test
    @DisplayName("should_deleteTimeDealCache_when_called")
    void should_deleteTimeDealCache_when_called() {
        // given
        Long timeDealId = 1L;
        Long productId = 10L;

        // when
        timeDealRedisService.deleteTimeDealCache(timeDealId, productId);

        // then
        verify(stringRedisTemplate).delete("timedeal:stock:1:10");
    }

    @Test
    @DisplayName("should_setExpiration_when_called")
    void should_setExpiration_when_called() {
        // given
        Long timeDealId = 1L;
        Long productId = 10L;
        long timeout = 3600;
        TimeUnit unit = TimeUnit.SECONDS;

        // when
        timeDealRedisService.setExpiration(timeDealId, productId, timeout, unit);

        // then
        verify(stringRedisTemplate).expire("timedeal:stock:1:10", timeout, unit);
    }
}
