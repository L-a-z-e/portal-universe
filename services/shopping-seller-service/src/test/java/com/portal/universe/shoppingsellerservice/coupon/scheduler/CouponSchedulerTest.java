package com.portal.universe.shoppingsellerservice.coupon.scheduler;

import com.portal.universe.event.seller.CouponUpdatedEvent;
import com.portal.universe.shoppingsellerservice.coupon.domain.Coupon;
import com.portal.universe.shoppingsellerservice.coupon.domain.CouponStatus;
import com.portal.universe.shoppingsellerservice.coupon.repository.CouponRepository;
import com.portal.universe.shoppingsellerservice.support.fixture.CouponFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponScheduler")
class CouponSchedulerTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CouponScheduler couponScheduler;

    @Nested
    @DisplayName("expireOverdueCoupons")
    class ExpireOverdueCoupons {

        @Test
        @DisplayName("should expire coupon and publish event when expiresAt has passed")
        void should_expire_coupon_when_overdue() {
            // given
            Coupon coupon = CouponFixture.builder()
                    .id(1L).sellerId(10L).code("EXPIRED01")
                    .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                    .build();
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
        @DisplayName("should do nothing when no expired coupons exist")
        void should_do_nothing_when_no_expired_coupons() {
            // given
            when(couponRepository.findExpiredCoupons(any(Instant.class))).thenReturn(Collections.emptyList());

            // when
            couponScheduler.expireOverdueCoupons();

            // then
            verify(couponRepository, never()).saveAll(any());
            verify(eventPublisher, never()).publishEvent(any(CouponUpdatedEvent.class));
        }

        @Test
        @DisplayName("should expire all coupons and publish events for each when multiple expired")
        void should_expire_multiple_coupons() {
            // given
            Coupon coupon1 = CouponFixture.builder()
                    .id(1L).sellerId(10L).code("EXP01")
                    .expiresAt(Instant.now().minus(2, ChronoUnit.DAYS))
                    .build();
            Coupon coupon2 = CouponFixture.builder()
                    .id(2L).sellerId(20L).code("EXP02")
                    .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                    .build();
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
}
