package com.portal.universe.shoppingservice.coupon.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponRedisServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private DefaultRedisScript<Long> couponIssueScript;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private CouponRedisService couponRedisService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    @DisplayName("should_initializeCouponStock_when_called")
    void should_initializeCouponStock_when_called() {
        // given
        Long couponId = 1L;
        int quantity = 100;

        // when
        couponRedisService.initializeCouponStock(couponId, quantity);

        // then
        verify(valueOperations).set("coupon:stock:1", "100");
    }

    @Test
    @DisplayName("should_returnSuccess_when_issueCouponSucceeds")
    void should_returnSuccess_when_issueCouponSucceeds() {
        // given
        Long couponId = 1L;
        String userId = "user-1";
        int maxQuantity = 100;
        when(stringRedisTemplate.execute(eq(couponIssueScript), anyList(), any(), any()))
                .thenReturn(1L);

        // when
        Long result = couponRedisService.issueCoupon(couponId, userId, maxQuantity);

        // then
        assertThat(result).isEqualTo(1L);
        verify(stringRedisTemplate).execute(
                eq(couponIssueScript),
                eq(Arrays.asList("coupon:stock:1", "coupon:issued:1")),
                eq("user-1"),
                eq("100")
        );
    }

    @Test
    @DisplayName("should_returnZero_when_couponSoldOut")
    void should_returnZero_when_couponSoldOut() {
        // given
        Long couponId = 1L;
        String userId = "user-1";
        int maxQuantity = 100;
        when(stringRedisTemplate.execute(eq(couponIssueScript), anyList(), any(), any()))
                .thenReturn(0L);

        // when
        Long result = couponRedisService.issueCoupon(couponId, userId, maxQuantity);

        // then
        assertThat(result).isEqualTo(0L);
    }

    @Test
    @DisplayName("should_returnTrue_when_alreadyIssued")
    void should_returnTrue_when_alreadyIssued() {
        // given
        Long couponId = 1L;
        String userId = "user-1";
        when(setOperations.isMember("coupon:issued:1", "user-1")).thenReturn(true);

        // when
        boolean result = couponRedisService.isAlreadyIssued(couponId, userId);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should_returnFalse_when_notIssued")
    void should_returnFalse_when_notIssued() {
        // given
        Long couponId = 1L;
        String userId = "user-1";
        when(setOperations.isMember("coupon:issued:1", "user-1")).thenReturn(false);

        // when
        boolean result = couponRedisService.isAlreadyIssued(couponId, userId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should_returnStock_when_stockExists")
    void should_returnStock_when_stockExists() {
        // given
        Long couponId = 1L;
        when(valueOperations.get("coupon:stock:1")).thenReturn("50");

        // when
        int stock = couponRedisService.getStock(couponId);

        // then
        assertThat(stock).isEqualTo(50);
    }

    @Test
    @DisplayName("should_returnIssuedCount_when_called")
    void should_returnIssuedCount_when_called() {
        // given
        Long couponId = 1L;
        when(setOperations.size("coupon:issued:1")).thenReturn(25L);

        // when
        long issuedCount = couponRedisService.getIssuedCount(couponId);

        // then
        assertThat(issuedCount).isEqualTo(25L);
    }

    @Test
    @DisplayName("should_incrementStock_when_called")
    void should_incrementStock_when_called() {
        // given
        Long couponId = 1L;

        // when
        couponRedisService.incrementStock(couponId);

        // then
        verify(valueOperations).increment("coupon:stock:1");
    }

    @Test
    @DisplayName("should_removeIssuedUser_when_called")
    void should_removeIssuedUser_when_called() {
        // given
        Long couponId = 1L;
        String userId = "user-1";

        // when
        couponRedisService.removeIssuedUser(couponId, userId);

        // then
        verify(setOperations).remove("coupon:issued:1", "user-1");
    }

    @Test
    @DisplayName("should_deleteCouponCache_when_called")
    void should_deleteCouponCache_when_called() {
        // given
        Long couponId = 1L;

        // when
        couponRedisService.deleteCouponCache(couponId);

        // then
        verify(stringRedisTemplate).delete(Arrays.asList("coupon:stock:1", "coupon:issued:1"));
    }
}
