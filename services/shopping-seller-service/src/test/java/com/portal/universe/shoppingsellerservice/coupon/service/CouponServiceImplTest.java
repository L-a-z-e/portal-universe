package com.portal.universe.shoppingsellerservice.coupon.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.event.seller.CouponCreatedEvent;
import com.portal.universe.event.seller.CouponDeletedEvent;
import com.portal.universe.shoppingsellerservice.common.exception.SellerErrorCode;
import com.portal.universe.shoppingsellerservice.coupon.domain.Coupon;
import com.portal.universe.shoppingsellerservice.coupon.domain.CouponStatus;
import com.portal.universe.shoppingsellerservice.coupon.domain.DiscountType;
import com.portal.universe.shoppingsellerservice.coupon.dto.CouponCreateRequest;
import com.portal.universe.shoppingsellerservice.coupon.dto.CouponResponse;
import com.portal.universe.shoppingsellerservice.coupon.repository.CouponRepository;
import com.portal.universe.shoppingsellerservice.support.fixture.CouponFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
@DisplayName("CouponServiceImpl")
class CouponServiceImplTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CouponServiceImpl couponService;

    @Nested
    @DisplayName("createCoupon")
    class CreateCoupon {

        @Test
        @DisplayName("should create coupon and publish event when code is unique")
        void should_create_coupon() {
            // given
            Long sellerId = 1L;
            CouponCreateRequest request = new CouponCreateRequest(
                    "NEW-CODE", "New Coupon", "desc", DiscountType.FIXED,
                    new BigDecimal("1000"), new BigDecimal("10000"), new BigDecimal("5000"),
                    100, Instant.now(), Instant.now().plus(30, ChronoUnit.DAYS)
            );
            when(couponRepository.existsByCode("NEW-CODE")).thenReturn(false);
            when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> {
                Coupon saved = invocation.getArgument(0);
                org.springframework.test.util.ReflectionTestUtils.setField(saved, "id", 1L);
                return saved;
            });

            // when
            CouponResponse response = couponService.createCoupon(sellerId, request);

            // then
            assertThat(response.code()).isEqualTo("NEW-CODE");
            assertThat(response.name()).isEqualTo("New Coupon");
            assertThat(response.status()).isEqualTo(CouponStatus.ACTIVE);
            verify(couponRepository).save(any(Coupon.class));
            verify(eventPublisher).publishEvent(any(CouponCreatedEvent.class));
        }

        @Test
        @DisplayName("should throw COUPON_CODE_ALREADY_EXISTS when code is duplicate")
        void should_throw_when_code_duplicate() {
            // given
            CouponCreateRequest request = new CouponCreateRequest(
                    "DUPLICATE", "Coupon", "desc", DiscountType.FIXED,
                    BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO,
                    10, Instant.now(), Instant.now().plus(1, ChronoUnit.DAYS)
            );
            when(couponRepository.existsByCode("DUPLICATE")).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> couponService.createCoupon(1L, request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.COUPON_CODE_ALREADY_EXISTS));
        }
    }

    @Nested
    @DisplayName("getCoupon")
    class GetCoupon {

        @Test
        @DisplayName("should return coupon when owned by seller")
        void should_return_coupon() {
            // given
            Long sellerId = 1L;
            Coupon coupon = CouponFixture.builder().id(1L).sellerId(sellerId).build();
            when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

            // when
            CouponResponse response = couponService.getCoupon(sellerId, 1L);

            // then
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.sellerId()).isEqualTo(sellerId);
        }

        @Test
        @DisplayName("should throw COUPON_NOT_FOUND when coupon does not exist")
        void should_throw_when_not_found() {
            // given
            when(couponRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> couponService.getCoupon(1L, 999L))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.COUPON_NOT_FOUND));
        }

        @Test
        @DisplayName("should throw COUPON_NOT_OWNED when seller does not own the coupon")
        void should_throw_when_not_owned() {
            // given
            Coupon coupon = CouponFixture.builder().id(1L).sellerId(1L).build();
            when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

            // when & then
            assertThatThrownBy(() -> couponService.getCoupon(99L, 1L))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.COUPON_NOT_OWNED));
        }
    }

    @Nested
    @DisplayName("getSellerCoupons")
    class GetSellerCoupons {

        @Test
        @DisplayName("should return seller's coupons with pagination")
        void should_return_coupons() {
            // given
            Long sellerId = 1L;
            Pageable pageable = PageRequest.of(0, 10);
            Coupon coupon = CouponFixture.builder().id(1L).sellerId(sellerId).build();
            when(couponRepository.findBySellerId(sellerId, pageable))
                    .thenReturn(new PageImpl<>(List.of(coupon)));

            // when
            Page<CouponResponse> result = couponService.getSellerCoupons(sellerId, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("deactivateCoupon")
    class DeactivateCoupon {

        @Test
        @DisplayName("should deactivate coupon and publish event")
        void should_deactivate_coupon() {
            // given
            Long sellerId = 1L;
            Coupon coupon = CouponFixture.builder().id(1L).sellerId(sellerId).build();
            when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

            // when
            couponService.deactivateCoupon(sellerId, 1L);

            // then
            assertThat(coupon.getStatus()).isEqualTo(CouponStatus.INACTIVE);
            verify(eventPublisher).publishEvent(any(CouponDeletedEvent.class));
        }

        @Test
        @DisplayName("should throw COUPON_NOT_FOUND when coupon does not exist")
        void should_throw_when_not_found() {
            // given
            when(couponRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> couponService.deactivateCoupon(1L, 999L))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.COUPON_NOT_FOUND));
        }

        @Test
        @DisplayName("should throw COUPON_NOT_OWNED when seller does not own the coupon")
        void should_throw_when_not_owned() {
            // given
            Coupon coupon = CouponFixture.builder().id(1L).sellerId(1L).build();
            when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

            // when & then
            assertThatThrownBy(() -> couponService.deactivateCoupon(99L, 1L))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.COUPON_NOT_OWNED));
        }
    }
}
