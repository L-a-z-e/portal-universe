package com.portal.universe.shoppingservice.timedeal.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.timedeal.domain.*;
import com.portal.universe.shoppingservice.timedeal.dto.TimeDealPurchaseRequest;
import com.portal.universe.shoppingservice.timedeal.dto.TimeDealPurchaseResponse;
import com.portal.universe.shoppingservice.timedeal.dto.TimeDealResponse;
import com.portal.universe.shoppingservice.timedeal.redis.TimeDealRedisService;
import com.portal.universe.shoppingservice.timedeal.repository.TimeDealProductRepository;
import com.portal.universe.shoppingservice.timedeal.repository.TimeDealPurchaseRepository;
import com.portal.universe.shoppingservice.timedeal.repository.TimeDealRepository;
import com.portal.universe.shoppingservice.support.fixture.TimeDealFixture;
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
class TimeDealServiceImplTest {

    @Mock
    private TimeDealRepository timeDealRepository;

    @Mock
    private TimeDealProductRepository timeDealProductRepository;

    @Mock
    private TimeDealPurchaseRepository timeDealPurchaseRepository;

    @Mock
    private TimeDealRedisService timeDealRedisService;

    @InjectMocks
    private TimeDealServiceImpl timeDealService;

    @Nested
    @DisplayName("getTimeDeal")
    class GetTimeDeal {

        @Test
        @DisplayName("should_returnTimeDeal_when_found")
        void should_returnTimeDeal_when_found() {
            // given
            TimeDeal timeDeal = TimeDealFixture.builder()
                    .id(1L).name("Flash Sale").status(TimeDealStatus.ACTIVE)
                    .startsAt(Instant.now().minus(1, ChronoUnit.HOURS))
                    .endsAt(Instant.now().plus(5, ChronoUnit.HOURS)).build();
            when(timeDealRepository.findByIdWithProducts(1L)).thenReturn(timeDeal);

            // when
            TimeDealResponse result = timeDealService.getTimeDeal(1L);

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should_throwException_when_notFound")
        void should_throwException_when_notFound() {
            // given
            when(timeDealRepository.findByIdWithProducts(999L)).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> timeDealService.getTimeDeal(999L))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("getActiveTimeDeals")
    class GetActiveTimeDeals {

        @Test
        @DisplayName("should_returnActiveTimeDeals_when_called")
        void should_returnActiveTimeDeals_when_called() {
            // given
            TimeDeal timeDeal = TimeDealFixture.builder()
                    .id(1L).name("Flash Sale").status(TimeDealStatus.ACTIVE)
                    .startsAt(Instant.now().minus(1, ChronoUnit.HOURS))
                    .endsAt(Instant.now().plus(5, ChronoUnit.HOURS)).build();
            when(timeDealRepository.findActiveDeals(eq(TimeDealStatus.ACTIVE), any(Instant.class)))
                    .thenReturn(List.of(timeDeal));

            // when
            List<TimeDealResponse> result = timeDealService.getActiveTimeDeals();

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("purchaseTimeDeal")
    class PurchaseTimeDeal {

        @Test
        @DisplayName("should_purchaseTimeDeal_when_valid")
        void should_purchaseTimeDeal_when_valid() {
            // given
            TimeDeal timeDeal = TimeDealFixture.builder()
                    .id(1L).name("Flash Sale").status(TimeDealStatus.ACTIVE)
                    .startsAt(Instant.now().minus(1, ChronoUnit.HOURS))
                    .endsAt(Instant.now().plus(5, ChronoUnit.HOURS)).build();
            TimeDealProduct tdp = TimeDealFixture.productBuilder()
                    .id(10L).timeDeal(timeDeal).productId(1L)
                    .dealPrice(BigDecimal.valueOf(5000)).dealQuantity(50).maxPerUser(2).build();

            when(timeDealProductRepository.findByIdWithDeal(10L))
                    .thenReturn(Optional.of(tdp));
            when(timeDealRedisService.purchaseProduct(1L, 1L, "user1", 1, 2)).thenReturn(49L);

            TimeDealPurchase purchase = TimeDealFixture.purchaseBuilder()
                    .id(1L).userId("user1").timeDealProduct(tdp).quantity(1).build();
            when(timeDealPurchaseRepository.save(any(TimeDealPurchase.class))).thenReturn(purchase);

            TimeDealPurchaseRequest request = TimeDealPurchaseRequest.builder()
                    .timeDealProductId(10L)
                    .quantity(1)
                    .build();

            // when
            TimeDealPurchaseResponse result = timeDealService.purchaseTimeDeal("user1", request);

            // then
            assertThat(result).isNotNull();
            verify(timeDealPurchaseRepository).save(any(TimeDealPurchase.class));
        }

        @Test
        @DisplayName("should_throwException_when_notActive")
        void should_throwException_when_notActive() {
            // given
            TimeDeal timeDeal = TimeDealFixture.builder()
                    .id(1L).name("Flash Sale").status(TimeDealStatus.SCHEDULED)
                    .startsAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .endsAt(Instant.now().plus(5, ChronoUnit.HOURS)).build();
            TimeDealProduct tdp = TimeDealFixture.productBuilder()
                    .id(10L).timeDeal(timeDeal).productId(1L)
                    .dealPrice(BigDecimal.valueOf(5000)).dealQuantity(50).maxPerUser(2).build();

            when(timeDealProductRepository.findByIdWithDeal(10L))
                    .thenReturn(Optional.of(tdp));

            TimeDealPurchaseRequest request = TimeDealPurchaseRequest.builder()
                    .timeDealProductId(10L)
                    .quantity(1)
                    .build();

            // when & then
            assertThatThrownBy(() -> timeDealService.purchaseTimeDeal("user1", request))
                    .isInstanceOf(CustomBusinessException.class);
        }

        @Test
        @DisplayName("should_throwException_when_soldOut")
        void should_throwException_when_soldOut() {
            // given
            TimeDeal timeDeal = TimeDealFixture.builder()
                    .id(1L).name("Flash Sale").status(TimeDealStatus.ACTIVE)
                    .startsAt(Instant.now().minus(1, ChronoUnit.HOURS))
                    .endsAt(Instant.now().plus(5, ChronoUnit.HOURS)).build();
            TimeDealProduct tdp = TimeDealFixture.productBuilder()
                    .id(10L).timeDeal(timeDeal).productId(1L)
                    .dealPrice(BigDecimal.valueOf(5000)).dealQuantity(50).maxPerUser(2).build();

            when(timeDealProductRepository.findByIdWithDeal(10L))
                    .thenReturn(Optional.of(tdp));
            when(timeDealRedisService.purchaseProduct(1L, 1L, "user1", 1, 2)).thenReturn(0L);

            TimeDealPurchaseRequest request = TimeDealPurchaseRequest.builder()
                    .timeDealProductId(10L)
                    .quantity(1)
                    .build();

            // when & then
            assertThatThrownBy(() -> timeDealService.purchaseTimeDeal("user1", request))
                    .isInstanceOf(CustomBusinessException.class);
        }

        @Test
        @DisplayName("should_throwException_when_limitExceeded")
        void should_throwException_when_limitExceeded() {
            // given
            TimeDeal timeDeal = TimeDealFixture.builder()
                    .id(1L).name("Flash Sale").status(TimeDealStatus.ACTIVE)
                    .startsAt(Instant.now().minus(1, ChronoUnit.HOURS))
                    .endsAt(Instant.now().plus(5, ChronoUnit.HOURS)).build();
            TimeDealProduct tdp = TimeDealFixture.productBuilder()
                    .id(10L).timeDeal(timeDeal).productId(1L)
                    .dealPrice(BigDecimal.valueOf(5000)).dealQuantity(50).maxPerUser(2).build();

            when(timeDealProductRepository.findByIdWithDeal(10L))
                    .thenReturn(Optional.of(tdp));
            when(timeDealRedisService.purchaseProduct(1L, 1L, "user1", 1, 2)).thenReturn(-1L);

            TimeDealPurchaseRequest request = TimeDealPurchaseRequest.builder()
                    .timeDealProductId(10L)
                    .quantity(1)
                    .build();

            // when & then
            assertThatThrownBy(() -> timeDealService.purchaseTimeDeal("user1", request))
                    .isInstanceOf(CustomBusinessException.class);
        }

        @Test
        @DisplayName("should_throwException_when_timeDealExpired")
        void should_throwException_when_timeDealExpired() {
            // given
            TimeDeal timeDeal = TimeDealFixture.builder()
                    .id(1L).name("Flash Sale").status(TimeDealStatus.ACTIVE)
                    .startsAt(Instant.now().minus(5, ChronoUnit.HOURS))
                    .endsAt(Instant.now().minus(1, ChronoUnit.HOURS)).build();
            TimeDealProduct tdp = TimeDealFixture.productBuilder()
                    .id(10L).timeDeal(timeDeal).productId(1L)
                    .dealPrice(BigDecimal.valueOf(5000)).dealQuantity(50).maxPerUser(2).build();

            when(timeDealProductRepository.findByIdWithDeal(10L))
                    .thenReturn(Optional.of(tdp));

            TimeDealPurchaseRequest request = TimeDealPurchaseRequest.builder()
                    .timeDealProductId(10L)
                    .quantity(1)
                    .build();

            // when & then
            assertThatThrownBy(() -> timeDealService.purchaseTimeDeal("user1", request))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("getUserPurchases")
    class GetUserPurchases {

        @Test
        @DisplayName("should_returnUserPurchases_when_called")
        void should_returnUserPurchases_when_called() {
            // given
            TimeDeal timeDeal = TimeDealFixture.builder()
                    .id(1L).name("Flash Sale").status(TimeDealStatus.ACTIVE)
                    .startsAt(Instant.now().minus(1, ChronoUnit.HOURS))
                    .endsAt(Instant.now().plus(5, ChronoUnit.HOURS)).build();
            TimeDealProduct tdp = TimeDealFixture.productBuilder()
                    .id(10L).timeDeal(timeDeal).productId(1L)
                    .dealPrice(BigDecimal.valueOf(5000)).dealQuantity(50).soldQuantity(1).maxPerUser(2).build();
            TimeDealPurchase purchase = TimeDealFixture.purchaseBuilder()
                    .id(1L).userId("user1").timeDealProduct(tdp).quantity(1).build();

            when(timeDealPurchaseRepository.findByUserIdWithProduct("user1"))
                    .thenReturn(List.of(purchase));

            // when
            List<TimeDealPurchaseResponse> result = timeDealService.getUserPurchases("user1");

            // then
            assertThat(result).hasSize(1);
        }
    }
}
