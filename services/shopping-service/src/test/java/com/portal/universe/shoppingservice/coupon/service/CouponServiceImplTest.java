package com.portal.universe.shoppingservice.coupon.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.coupon.domain.*;
import com.portal.universe.shoppingservice.coupon.dto.CouponResponse;
import com.portal.universe.shoppingservice.coupon.dto.UserCouponResponse;
import com.portal.universe.shoppingservice.coupon.redis.CouponRedisService;
import com.portal.universe.shoppingservice.coupon.repository.CouponRepository;
import com.portal.universe.shoppingservice.coupon.repository.UserCouponRepository;
import com.portal.universe.shoppingservice.event.ShoppingEventPublisher;
import com.portal.universe.shoppingservice.support.fixture.CouponFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private CouponRedisService couponRedisService;

    @Mock
    private ShoppingEventPublisher eventPublisher;

    @InjectMocks
    private CouponServiceImpl couponService;

    @Nested
    @DisplayName("getCoupon")
    class GetCoupon {

        @Test
        @DisplayName("should_returnCoupon_when_found")
        void should_returnCoupon_when_found() {
            // given
            Coupon coupon = CouponFixture.couponBuilder()
                    .id(1L).code("SAVE10").status(CouponStatus.ACTIVE)
                    .totalQuantity(100).issuedQuantity(0).build();
            when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

            // when
            CouponResponse result = couponService.getCoupon(1L);

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should_throwException_when_notFound")
        void should_throwException_when_notFound() {
            // given
            when(couponRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> couponService.getCoupon(999L))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("getAvailableCoupons")
    class GetAvailableCoupons {

        @Test
        @DisplayName("should_returnAvailableCoupons_when_called")
        void should_returnAvailableCoupons_when_called() {
            // given
            Coupon coupon = CouponFixture.couponBuilder()
                    .id(1L).code("SAVE10").status(CouponStatus.ACTIVE)
                    .totalQuantity(100).issuedQuantity(0).build();
            when(couponRepository.findAvailableCoupons(eq(CouponStatus.ACTIVE), any(Instant.class)))
                    .thenReturn(List.of(coupon));

            // when
            List<CouponResponse> result = couponService.getAvailableCoupons();

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("issueCoupon")
    class IssueCoupon {

        @Test
        @DisplayName("should_issueCoupon_when_valid")
        void should_issueCoupon_when_valid() {
            // given
            Coupon coupon = CouponFixture.couponBuilder()
                    .id(1L).code("SAVE10").status(CouponStatus.ACTIVE)
                    .totalQuantity(100).issuedQuantity(0).build();
            when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
            when(couponRedisService.issueCoupon(1L, "1", 100)).thenReturn(1L);

            UserCoupon userCoupon = CouponFixture.userCouponBuilder()
                    .id(1L).userId("1").coupon(coupon).status(UserCouponStatus.AVAILABLE).build();
            when(userCouponRepository.save(any(UserCoupon.class))).thenReturn(userCoupon);

            // when
            UserCouponResponse result = couponService.issueCoupon(1L, "1");

            // then
            assertThat(result).isNotNull();
            verify(eventPublisher).publishCouponIssued(any());
        }

        @Test
        @DisplayName("should_throwException_when_alreadyIssued")
        void should_throwException_when_alreadyIssued() {
            // given
            Coupon coupon = CouponFixture.couponBuilder()
                    .id(1L).code("SAVE10").status(CouponStatus.ACTIVE)
                    .totalQuantity(100).issuedQuantity(0).build();
            when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
            when(couponRedisService.issueCoupon(1L, "1", 100)).thenReturn(-1L);

            // when & then
            assertThatThrownBy(() -> couponService.issueCoupon(1L, "1"))
                    .isInstanceOf(CustomBusinessException.class);
        }

        @Test
        @DisplayName("should_throwException_when_exhausted")
        void should_throwException_when_exhausted() {
            // given
            Coupon coupon = CouponFixture.couponBuilder()
                    .id(1L).code("SAVE10").status(CouponStatus.ACTIVE)
                    .totalQuantity(100).issuedQuantity(0).build();
            when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
            when(couponRedisService.issueCoupon(1L, "1", 100)).thenReturn(0L);

            // when & then
            assertThatThrownBy(() -> couponService.issueCoupon(1L, "1"))
                    .isInstanceOf(CustomBusinessException.class);
        }

        @Test
        @DisplayName("should_throwException_when_inactive")
        void should_throwException_when_inactive() {
            // given
            Coupon coupon = CouponFixture.couponBuilder()
                    .id(1L).code("SAVE10").status(CouponStatus.INACTIVE)
                    .totalQuantity(100).issuedQuantity(0).build();
            when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

            // when & then
            assertThatThrownBy(() -> couponService.issueCoupon(1L, "1"))
                    .isInstanceOf(CustomBusinessException.class);
        }

        @Test
        @DisplayName("should_throwException_when_notStarted")
        void should_throwException_when_notStarted() {
            // given
            Coupon coupon = CouponFixture.couponBuilder()
                    .id(1L).code("SAVE10").status(CouponStatus.ACTIVE)
                    .totalQuantity(100).issuedQuantity(0)
                    .startsAt(Instant.now().plus(10, ChronoUnit.DAYS))
                    .build();
            when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

            // when & then
            assertThatThrownBy(() -> couponService.issueCoupon(1L, "1"))
                    .isInstanceOf(CustomBusinessException.class);
        }

        @Test
        @DisplayName("should_throwException_when_expired")
        void should_throwException_when_expired() {
            // given
            Coupon coupon = CouponFixture.couponBuilder()
                    .id(1L).code("SAVE10").status(CouponStatus.ACTIVE)
                    .totalQuantity(100).issuedQuantity(0)
                    .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                    .build();
            when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

            // when & then
            assertThatThrownBy(() -> couponService.issueCoupon(1L, "1"))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("getUserCoupons")
    class GetUserCoupons {

        @Test
        @DisplayName("should_returnUserCoupons_when_called")
        void should_returnUserCoupons_when_called() {
            // given
            Coupon coupon = CouponFixture.couponBuilder()
                    .id(1L).code("SAVE10").status(CouponStatus.ACTIVE)
                    .totalQuantity(100).issuedQuantity(1).build();
            UserCoupon userCoupon = CouponFixture.userCouponBuilder()
                    .id(1L).userId("user1").coupon(coupon).status(UserCouponStatus.AVAILABLE).build();
            when(userCouponRepository.findByUserId("user1")).thenReturn(List.of(userCoupon));

            // when
            List<UserCouponResponse> result = couponService.getUserCoupons("user1");

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getAvailableUserCoupons")
    class GetAvailableUserCoupons {

        @Test
        @DisplayName("should_returnAvailableUserCoupons_when_called")
        void should_returnAvailableUserCoupons_when_called() {
            // given
            Coupon coupon = CouponFixture.couponBuilder()
                    .id(1L).code("SAVE10").status(CouponStatus.ACTIVE)
                    .totalQuantity(100).issuedQuantity(1).build();
            UserCoupon userCoupon = CouponFixture.userCouponBuilder()
                    .id(1L).userId("user1").coupon(coupon).status(UserCouponStatus.AVAILABLE).build();
            when(userCouponRepository.findAvailableByUserId(eq("user1"), any(Instant.class)))
                    .thenReturn(List.of(userCoupon));

            // when
            List<UserCouponResponse> result = couponService.getAvailableUserCoupons("user1");

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("useCoupon")
    class UseCoupon {

        @Test
        @DisplayName("should_useCoupon_when_valid")
        void should_useCoupon_when_valid() {
            // given
            Coupon coupon = CouponFixture.couponBuilder()
                    .id(1L).code("SAVE10").status(CouponStatus.ACTIVE)
                    .totalQuantity(100).issuedQuantity(1).build();
            UserCoupon userCoupon = CouponFixture.userCouponBuilder()
                    .id(1L).userId("user1").coupon(coupon).status(UserCouponStatus.AVAILABLE).build();
            when(userCouponRepository.findById(1L)).thenReturn(Optional.of(userCoupon));
            when(userCouponRepository.save(any(UserCoupon.class))).thenReturn(userCoupon);

            // when
            couponService.useCoupon(1L, 100L);

            // then
            verify(userCouponRepository).save(any(UserCoupon.class));
        }

        @Test
        @DisplayName("should_throwException_when_alreadyUsed")
        void should_throwException_when_alreadyUsed() {
            // given
            Coupon coupon = CouponFixture.couponBuilder()
                    .id(1L).code("SAVE10").status(CouponStatus.ACTIVE)
                    .totalQuantity(100).issuedQuantity(1).build();
            UserCoupon userCoupon = CouponFixture.userCouponBuilder()
                    .id(1L).userId("user1").coupon(coupon).status(UserCouponStatus.USED).build();
            when(userCouponRepository.findById(1L)).thenReturn(Optional.of(userCoupon));

            // when & then
            assertThatThrownBy(() -> couponService.useCoupon(1L, 100L))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("calculateDiscount")
    class CalculateDiscount {

        @Test
        @DisplayName("should_calculateDiscount_when_valid")
        void should_calculateDiscount_when_valid() {
            // given
            Coupon coupon = CouponFixture.couponBuilder()
                    .id(1L).code("SAVE10").status(CouponStatus.ACTIVE)
                    .totalQuantity(100).issuedQuantity(1).build();
            UserCoupon userCoupon = CouponFixture.userCouponBuilder()
                    .id(1L).userId("user1").coupon(coupon).status(UserCouponStatus.AVAILABLE).build();
            when(userCouponRepository.findById(1L)).thenReturn(Optional.of(userCoupon));

            // when
            BigDecimal result = couponService.calculateDiscount(1L, BigDecimal.valueOf(10000));

            // then
            assertThat(result).isNotNull();
            assertThat(result).isGreaterThan(BigDecimal.ZERO);
        }
    }
}
