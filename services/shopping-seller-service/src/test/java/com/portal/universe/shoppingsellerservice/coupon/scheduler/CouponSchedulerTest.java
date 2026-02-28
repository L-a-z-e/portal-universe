package com.portal.universe.shoppingsellerservice.coupon.scheduler;

import com.portal.universe.event.seller.CouponUpdatedEvent;
import com.portal.universe.shoppingsellerservice.coupon.domain.Coupon;
import com.portal.universe.shoppingsellerservice.coupon.domain.CouponStatus;
import com.portal.universe.shoppingsellerservice.coupon.domain.DiscountType;
import com.portal.universe.shoppingsellerservice.coupon.repository.CouponRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponSchedulerTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CouponScheduler couponScheduler;

    private Coupon createCoupon(Long id, Long sellerId, String code, CouponStatus status, Instant expiresAt) {
        Coupon coupon = Coupon.builder()
                .sellerId(sellerId)
                .code(code)
                .name("Test Coupon")
                .description("desc")
                .discountType(DiscountType.FIXED)
                .discountValue(BigDecimal.valueOf(1000))
                .minimumOrderAmount(BigDecimal.valueOf(5000))
                .maximumDiscountAmount(BigDecimal.valueOf(3000))
                .totalQuantity(100)
                .startsAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .expiresAt(expiresAt)
                .build();
        ReflectionTestUtils.setField(coupon, "id", id);
        ReflectionTestUtils.setField(coupon, "status", status);
        return coupon;
    }

    @Test
    @DisplayName("should_expireCoupons_when_expiresAtPassed")
    void should_expireCoupons_when_expiresAtPassed() {
        // given
        Coupon coupon = createCoupon(1L, 10L, "EXPIRED01", CouponStatus.ACTIVE,
                Instant.now().minus(1, ChronoUnit.DAYS));
        when(couponRepository.findExpiredCoupons(any(Instant.class))).thenReturn(List.of(coupon));

        // when
        couponScheduler.expireOverdueCoupons();

        // then
        assertThat(coupon.getStatus()).isEqualTo(CouponStatus.EXPIRED);
        verify(couponRepository).saveAll(List.of(coupon));

        ArgumentCaptor<CouponUpdatedEvent> captor = ArgumentCaptor.forClass(CouponUpdatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("EXPIRED");
    }

    @Test
    @DisplayName("should_doNothing_when_noExpiredCoupons")
    void should_doNothing_when_noExpiredCoupons() {
        // given
        when(couponRepository.findExpiredCoupons(any(Instant.class))).thenReturn(Collections.emptyList());

        // when
        couponScheduler.expireOverdueCoupons();

        // then
        verify(couponRepository, never()).saveAll(any());
        verify(eventPublisher, never()).publishEvent(any(CouponUpdatedEvent.class));
    }

    @Test
    @DisplayName("should_expireMultipleCoupons_when_multipleExpired")
    void should_expireMultipleCoupons_when_multipleExpired() {
        // given
        Coupon coupon1 = createCoupon(1L, 10L, "EXP01", CouponStatus.ACTIVE,
                Instant.now().minus(2, ChronoUnit.DAYS));
        Coupon coupon2 = createCoupon(2L, 20L, "EXP02", CouponStatus.ACTIVE,
                Instant.now().minus(1, ChronoUnit.DAYS));
        when(couponRepository.findExpiredCoupons(any(Instant.class))).thenReturn(List.of(coupon1, coupon2));

        // when
        couponScheduler.expireOverdueCoupons();

        // then
        assertThat(coupon1.getStatus()).isEqualTo(CouponStatus.EXPIRED);
        assertThat(coupon2.getStatus()).isEqualTo(CouponStatus.EXPIRED);
        verify(couponRepository).saveAll(List.of(coupon1, coupon2));
        verify(eventPublisher, times(2)).publishEvent(any(CouponUpdatedEvent.class));
    }
}
