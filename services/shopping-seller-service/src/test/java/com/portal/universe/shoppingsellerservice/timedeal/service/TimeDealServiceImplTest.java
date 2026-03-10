package com.portal.universe.shoppingsellerservice.timedeal.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.event.seller.TimeDealCancelledEvent;
import com.portal.universe.event.seller.TimeDealCreatedEvent;
import com.portal.universe.shoppingsellerservice.common.exception.SellerErrorCode;
import com.portal.universe.shoppingsellerservice.product.domain.Product;
import com.portal.universe.shoppingsellerservice.product.repository.ProductRepository;
import com.portal.universe.shoppingsellerservice.support.fixture.ProductFixture;
import com.portal.universe.shoppingsellerservice.support.fixture.TimeDealFixture;
import com.portal.universe.shoppingsellerservice.timedeal.domain.TimeDeal;
import com.portal.universe.shoppingsellerservice.timedeal.domain.TimeDealStatus;
import com.portal.universe.shoppingsellerservice.timedeal.dto.TimeDealCreateRequest;
import com.portal.universe.shoppingsellerservice.timedeal.dto.TimeDealResponse;
import com.portal.universe.shoppingsellerservice.timedeal.repository.TimeDealRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimeDealServiceImpl")
class TimeDealServiceImplTest {

    @Mock
    private TimeDealRepository timeDealRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TimeDealServiceImpl timeDealService;

    @Nested
    @DisplayName("createTimeDeal")
    class CreateTimeDeal {

        @Test
        @DisplayName("should create time deal with products and publish event")
        void should_create_timedeal() {
            // given
            Long sellerId = 1L;
            Instant startsAt = Instant.now().plus(1, ChronoUnit.HOURS);
            Instant endsAt = Instant.now().plus(2, ChronoUnit.HOURS);
            Product product = ProductFixture.builder().id(10L).sellerId(sellerId).build();

            TimeDealCreateRequest request = new TimeDealCreateRequest(
                    "Flash Sale", "Big discount", startsAt, endsAt,
                    List.of(new TimeDealCreateRequest.TimeDealProductItem(
                            10L, new BigDecimal("5000"), 50, 5
                    ))
            );
            when(productRepository.findAllById(List.of(10L))).thenReturn(List.of(product));
            when(timeDealRepository.save(any(TimeDeal.class))).thenAnswer(invocation -> {
                TimeDeal saved = invocation.getArgument(0);
                org.springframework.test.util.ReflectionTestUtils.setField(saved, "id", 1L);
                return saved;
            });

            // when
            TimeDealResponse response = timeDealService.createTimeDeal(sellerId, request);

            // then
            assertThat(response.name()).isEqualTo("Flash Sale");
            assertThat(response.status()).isEqualTo(TimeDealStatus.SCHEDULED);
            assertThat(response.products()).hasSize(1);
            verify(timeDealRepository).save(any(TimeDeal.class));
            verify(eventPublisher).publishEvent(any(TimeDealCreatedEvent.class));
        }

        @Test
        @DisplayName("should throw TIMEDEAL_INVALID_PERIOD when startsAt is not before endsAt")
        void should_throw_when_invalid_period() {
            // given
            Instant now = Instant.now();
            TimeDealCreateRequest request = new TimeDealCreateRequest(
                    "Deal", "desc", now.plus(2, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS),
                    List.of(new TimeDealCreateRequest.TimeDealProductItem(1L, BigDecimal.ONE, 1, 1))
            );

            // when & then
            assertThatThrownBy(() -> timeDealService.createTimeDeal(1L, request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.TIMEDEAL_INVALID_PERIOD));
        }

        @Test
        @DisplayName("should throw TIMEDEAL_PRODUCT_NOT_FOUND when product does not exist")
        void should_throw_when_product_not_found() {
            // given
            Instant startsAt = Instant.now().plus(1, ChronoUnit.HOURS);
            Instant endsAt = Instant.now().plus(2, ChronoUnit.HOURS);
            TimeDealCreateRequest request = new TimeDealCreateRequest(
                    "Deal", "desc", startsAt, endsAt,
                    List.of(new TimeDealCreateRequest.TimeDealProductItem(999L, BigDecimal.ONE, 1, 1))
            );
            when(productRepository.findAllById(List.of(999L))).thenReturn(List.of());

            // when & then
            assertThatThrownBy(() -> timeDealService.createTimeDeal(1L, request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.TIMEDEAL_PRODUCT_NOT_FOUND));
        }

        @Test
        @DisplayName("should throw PRODUCT_NOT_OWNED when product belongs to another seller")
        void should_throw_when_product_not_owned() {
            // given
            Long sellerId = 1L;
            Product otherSellerProduct = ProductFixture.builder().id(10L).sellerId(99L).build();
            Instant startsAt = Instant.now().plus(1, ChronoUnit.HOURS);
            Instant endsAt = Instant.now().plus(2, ChronoUnit.HOURS);
            TimeDealCreateRequest request = new TimeDealCreateRequest(
                    "Deal", "desc", startsAt, endsAt,
                    List.of(new TimeDealCreateRequest.TimeDealProductItem(10L, BigDecimal.ONE, 1, 1))
            );
            when(productRepository.findAllById(List.of(10L))).thenReturn(List.of(otherSellerProduct));

            // when & then
            assertThatThrownBy(() -> timeDealService.createTimeDeal(sellerId, request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.PRODUCT_NOT_OWNED));
        }
    }

    @Nested
    @DisplayName("getTimeDeal")
    class GetTimeDeal {

        @Test
        @DisplayName("should return time deal when owned by seller")
        void should_return_timedeal() {
            // given
            Long sellerId = 1L;
            TimeDeal timeDeal = TimeDealFixture.builder().id(1L).sellerId(sellerId).build();
            when(timeDealRepository.findById(1L)).thenReturn(Optional.of(timeDeal));

            // when
            TimeDealResponse response = timeDealService.getTimeDeal(sellerId, 1L);

            // then
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.sellerId()).isEqualTo(sellerId);
        }

        @Test
        @DisplayName("should throw TIMEDEAL_NOT_FOUND when not found")
        void should_throw_when_not_found() {
            // given
            when(timeDealRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> timeDealService.getTimeDeal(1L, 999L))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.TIMEDEAL_NOT_FOUND));
        }

        @Test
        @DisplayName("should throw TIMEDEAL_NOT_OWNED when seller does not own it")
        void should_throw_when_not_owned() {
            // given
            TimeDeal timeDeal = TimeDealFixture.builder().id(1L).sellerId(1L).build();
            when(timeDealRepository.findById(1L)).thenReturn(Optional.of(timeDeal));

            // when & then
            assertThatThrownBy(() -> timeDealService.getTimeDeal(99L, 1L))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.TIMEDEAL_NOT_OWNED));
        }
    }

    @Nested
    @DisplayName("getSellerTimeDeals")
    class GetSellerTimeDeals {

        @Test
        @DisplayName("should return seller's time deals with pagination")
        void should_return_timedeals() {
            // given
            Long sellerId = 1L;
            Pageable pageable = PageRequest.of(0, 10);
            TimeDeal timeDeal = TimeDealFixture.builder().id(1L).sellerId(sellerId).build();
            when(timeDealRepository.findBySellerId(sellerId, pageable))
                    .thenReturn(new PageImpl<>(List.of(timeDeal)));

            // when
            Page<TimeDealResponse> result = timeDealService.getSellerTimeDeals(sellerId, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("cancelTimeDeal")
    class CancelTimeDeal {

        @Test
        @DisplayName("should cancel scheduled time deal and publish event")
        void should_cancel_scheduled_timedeal() {
            // given
            Long sellerId = 1L;
            TimeDeal timeDeal = TimeDealFixture.builder().id(1L).sellerId(sellerId).build(); // SCHEDULED by default
            when(timeDealRepository.findById(1L)).thenReturn(Optional.of(timeDeal));

            // when
            timeDealService.cancelTimeDeal(sellerId, 1L);

            // then
            assertThat(timeDeal.getStatus()).isEqualTo(TimeDealStatus.CANCELLED);
            verify(eventPublisher).publishEvent(any(TimeDealCancelledEvent.class));
        }

        @Test
        @DisplayName("should cancel active time deal")
        void should_cancel_active_timedeal() {
            // given
            Long sellerId = 1L;
            TimeDeal timeDeal = TimeDealFixture.builder().id(1L).sellerId(sellerId)
                    .status(TimeDealStatus.ACTIVE).build();
            when(timeDealRepository.findById(1L)).thenReturn(Optional.of(timeDeal));

            // when
            timeDealService.cancelTimeDeal(sellerId, 1L);

            // then
            assertThat(timeDeal.getStatus()).isEqualTo(TimeDealStatus.CANCELLED);
        }

        @Test
        @DisplayName("should throw TIMEDEAL_NOT_FOUND when not found")
        void should_throw_when_not_found() {
            // given
            when(timeDealRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> timeDealService.cancelTimeDeal(1L, 999L))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.TIMEDEAL_NOT_FOUND));
        }

        @Test
        @DisplayName("should throw TIMEDEAL_NOT_OWNED when seller does not own it")
        void should_throw_when_not_owned() {
            // given
            TimeDeal timeDeal = TimeDealFixture.builder().id(1L).sellerId(1L).build();
            when(timeDealRepository.findById(1L)).thenReturn(Optional.of(timeDeal));

            // when & then
            assertThatThrownBy(() -> timeDealService.cancelTimeDeal(99L, 1L))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.TIMEDEAL_NOT_OWNED));
        }

        @Test
        @DisplayName("should throw TIMEDEAL_CANNOT_CANCEL when status is ENDED")
        void should_throw_when_not_cancellable() {
            // given
            Long sellerId = 1L;
            TimeDeal timeDeal = TimeDealFixture.builder().id(1L).sellerId(sellerId)
                    .status(TimeDealStatus.ENDED).build();
            when(timeDealRepository.findById(1L)).thenReturn(Optional.of(timeDeal));

            // when & then
            assertThatThrownBy(() -> timeDealService.cancelTimeDeal(sellerId, 1L))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.TIMEDEAL_CANNOT_CANCEL));
        }
    }
}
