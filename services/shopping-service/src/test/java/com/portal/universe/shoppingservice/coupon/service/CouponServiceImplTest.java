package com.portal.universe.shoppingservice.coupon.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.coupon.domain.*;
import com.portal.universe.shoppingservice.coupon.dto.CouponCreateRequest;
import com.portal.universe.shoppingservice.coupon.dto.CouponResponse;
import com.portal.universe.shoppingservice.coupon.dto.UserCouponResponse;
import com.portal.universe.shoppingservice.coupon.redis.CouponRedisService;
import com.portal.universe.shoppingservice.coupon.repository.CouponRepository;
import com.portal.universe.shoppingservice.coupon.repository.UserCouponRepository;
import com.portal.universe.shoppingservice.event.ShoppingEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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

    private Coupon createCoupon(Long id, String code, CouponStatus status, int totalQty, int issuedQty) {
        Coupon coupon = Coupon.builder()
                .code(code)
                .name("Test Coupon")
                .description("desc")
                .discountType(DiscountType.FIXED)
                .discountValue(BigDecimal.valueOf(1000))
                .minimumOrderAmount(BigDecimal.valueOf(5000))
                .maximumDiscountAmount(BigDecimal.valueOf(3000))
                .totalQuantity(totalQty)
                .startsAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
        ReflectionTestUtils.setField(coupon, "id", id);
        ReflectionTestUtils.setField(coupon, "status", status);
        ReflectionTestUtils.setField(coupon, "issuedQuantity", issuedQty);
        return coupon;
    }

    private UserCoupon createUserCoupon(Long id, String userId, Coupon coupon, UserCouponStatus status) {
        UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .coupon(coupon)
                .expiresAt(coupon.getExpiresAt())
                .build();
        ReflectionTestUtils.setField(userCoupon, "id", id);
        ReflectionTestUtils.setField(userCoupon, "status", status);
        return userCoupon;
    }

    @Nested
    @DisplayName("getAllCoupons")
    class GetAllCoupons {

        @Test
        @DisplayName("should_returnCoupons_when_called")
        void should_returnCoupons_when_called() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Coupon coupon = createCoupon(1L, "SAVE10", CouponStatus.ACTIVE, 100, 0);
            Page<Coupon> couponPage = new PageImpl<>(List.of(coupon), pageable, 1);
            when(couponRepository.findAll(pageable)).thenReturn(couponPage);

            // when
            Page<CouponResponse> result = couponService.getAllCoupons(pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("createCoupon")
    class CreateCoupon {

        @Test
        @DisplayName("should_createCoupon_when_valid")
        void should_createCoupon_when_valid() {
            // given
            CouponCreateRequest request = CouponCreateRequest.builder()
                    .code("NEW10")
                    .name("New Coupon")
                    .description("desc")
                    .discountType(DiscountType.FIXED)
                    .discountValue(BigDecimal.valueOf(1000))
                    .totalQuantity(100)
                    .startsAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .build();

            when(couponRepository.existsByCode("NEW10")).thenReturn(false);
            Coupon savedCoupon = createCoupon(1L, "NEW10", CouponStatus.ACTIVE, 100, 0);
            when(couponRepository.save(any(Coupon.class))).thenReturn(savedCoupon);

            // when
            CouponResponse result = couponService.createCoupon(request);

            // then
            assertThat(result).isNotNull();
            verify(couponRedisService).initializeCouponStock(1L, 100);
        }

        @Test
        @DisplayName("should_throwException_when_codeExists")
        void should_throwException_when_codeExists() {
            // given
            CouponCreateRequest request = CouponCreateRequest.builder()
                    .code("EXISTING")
                    .name("Dup")
                    .discountType(DiscountType.FIXED)
                    .discountValue(BigDecimal.valueOf(1000))
                    .totalQuantity(100)
                    .startsAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .build();

            when(couponRepository.existsByCode("EXISTING")).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> couponService.createCoupon(request))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("getCoupon")
    class GetCoupon {

        @Test
        @DisplayName("should_returnCoupon_when_found")
        void should_returnCoupon_when_found() {
            // given
            Coupon coupon = createCoupon(1L, "SAVE10", CouponStatus.ACTIVE, 100, 0);
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
            Coupon coupon = createCoupon(1L, "SAVE10", CouponStatus.ACTIVE, 100, 0);
            when(couponRepository.findAvailableCoupons(eq(CouponStatus.ACTIVE), any(LocalDateTime.class)))
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
            Coupon coupon = createCoupon(1L, "SAVE10", CouponStatus.ACTIVE, 100, 0);
            when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
            when(couponRedisService.issueCoupon(1L, "1", 100)).thenReturn(1L);

            UserCoupon userCoupon = createUserCoupon(1L, "1", coupon, UserCouponStatus.AVAILABLE);
            when(userCouponRepository.save(any(UserCoupon.class))).thenReturn(userCoupon);
            when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);

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
            Coupon coupon = createCoupon(1L, "SAVE10", CouponStatus.ACTIVE, 100, 0);
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
            Coupon coupon = createCoupon(1L, "SAVE10", CouponStatus.ACTIVE, 100, 0);
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
            Coupon coupon = createCoupon(1L, "SAVE10", CouponStatus.INACTIVE, 100, 0);
            when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

            // when & then
            assertThatThrownBy(() -> couponService.issueCoupon(1L, "1"))
                    .isInstanceOf(CustomBusinessException.class);
        }

        @Test
        @DisplayName("should_throwException_when_notStarted")
        void should_throwException_when_notStarted() {
            // given
            Coupon coupon = createCoupon(1L, "SAVE10", CouponStatus.ACTIVE, 100, 0);
            ReflectionTestUtils.setField(coupon, "startsAt", LocalDateTime.now().plusDays(10));
            when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

            // when & then
            assertThatThrownBy(() -> couponService.issueCoupon(1L, "1"))
                    .isInstanceOf(CustomBusinessException.class);
        }

        @Test
        @DisplayName("should_throwException_when_expired")
        void should_throwException_when_expired() {
            // given
            Coupon coupon = createCoupon(1L, "SAVE10", CouponStatus.ACTIVE, 100, 0);
            ReflectionTestUtils.setField(coupon, "expiresAt", LocalDateTime.now().minusDays(1));
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
            Coupon coupon = createCoupon(1L, "SAVE10", CouponStatus.ACTIVE, 100, 1);
            UserCoupon userCoupon = createUserCoupon(1L, "user1", coupon, UserCouponStatus.AVAILABLE);
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
            Coupon coupon = createCoupon(1L, "SAVE10", CouponStatus.ACTIVE, 100, 1);
            UserCoupon userCoupon = createUserCoupon(1L, "user1", coupon, UserCouponStatus.AVAILABLE);
            when(userCouponRepository.findAvailableByUserId(eq("user1"), any(LocalDateTime.class)))
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
            Coupon coupon = createCoupon(1L, "SAVE10", CouponStatus.ACTIVE, 100, 1);
            UserCoupon userCoupon = createUserCoupon(1L, "user1", coupon, UserCouponStatus.AVAILABLE);
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
            Coupon coupon = createCoupon(1L, "SAVE10", CouponStatus.ACTIVE, 100, 1);
            UserCoupon userCoupon = createUserCoupon(1L, "user1", coupon, UserCouponStatus.USED);
            when(userCouponRepository.findById(1L)).thenReturn(Optional.of(userCoupon));

            // when & then
            assertThatThrownBy(() -> couponService.useCoupon(1L, 100L))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("deactivateCoupon")
    class DeactivateCoupon {

        @Test
        @DisplayName("should_deactivateCoupon_when_valid")
        void should_deactivateCoupon_when_valid() {
            // given
            Coupon coupon = createCoupon(1L, "SAVE10", CouponStatus.ACTIVE, 100, 0);
            when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
            when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);

            // when
            couponService.deactivateCoupon(1L);

            // then
            verify(couponRepository).save(any(Coupon.class));
            verify(couponRedisService).deleteCouponCache(1L);
        }
    }

    @Nested
    @DisplayName("calculateDiscount")
    class CalculateDiscount {

        @Test
        @DisplayName("should_calculateDiscount_when_valid")
        void should_calculateDiscount_when_valid() {
            // given
            Coupon coupon = createCoupon(1L, "SAVE10", CouponStatus.ACTIVE, 100, 1);
            UserCoupon userCoupon = createUserCoupon(1L, "user1", coupon, UserCouponStatus.AVAILABLE);
            when(userCouponRepository.findById(1L)).thenReturn(Optional.of(userCoupon));

            // when
            BigDecimal result = couponService.calculateDiscount(1L, BigDecimal.valueOf(10000));

            // then
            assertThat(result).isNotNull();
            assertThat(result).isGreaterThan(BigDecimal.ZERO);
        }
    }
}
