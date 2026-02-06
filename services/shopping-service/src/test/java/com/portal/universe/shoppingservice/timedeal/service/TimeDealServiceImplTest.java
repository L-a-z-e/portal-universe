package com.portal.universe.shoppingservice.timedeal.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.product.domain.Product;
import com.portal.universe.shoppingservice.product.repository.ProductRepository;
import com.portal.universe.shoppingservice.timedeal.domain.*;
import com.portal.universe.shoppingservice.timedeal.dto.TimeDealCreateRequest;
import com.portal.universe.shoppingservice.timedeal.dto.TimeDealPurchaseRequest;
import com.portal.universe.shoppingservice.timedeal.dto.TimeDealPurchaseResponse;
import com.portal.universe.shoppingservice.timedeal.dto.TimeDealResponse;
import com.portal.universe.shoppingservice.timedeal.redis.TimeDealRedisService;
import com.portal.universe.shoppingservice.timedeal.repository.TimeDealProductRepository;
import com.portal.universe.shoppingservice.timedeal.repository.TimeDealPurchaseRepository;
import com.portal.universe.shoppingservice.timedeal.repository.TimeDealRepository;
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
    private ProductRepository productRepository;

    @Mock
    private TimeDealRedisService timeDealRedisService;

    @InjectMocks
    private TimeDealServiceImpl timeDealService;

    private Product createProduct(Long id, String name, BigDecimal price) {
        Product product = Product.builder()
                .name(name)
                .description("desc")
                .price(price)
                .stock(100)
                .build();
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private TimeDeal createTimeDeal(Long id, String name, TimeDealStatus status,
                                     LocalDateTime startsAt, LocalDateTime endsAt) {
        TimeDeal timeDeal = TimeDeal.builder()
                .name(name)
                .description("desc")
                .startsAt(startsAt)
                .endsAt(endsAt)
                .build();
        ReflectionTestUtils.setField(timeDeal, "id", id);
        ReflectionTestUtils.setField(timeDeal, "status", status);
        return timeDeal;
    }

    private TimeDealProduct createTimeDealProduct(Long id, TimeDeal timeDeal, Product product,
                                                   BigDecimal dealPrice, int dealQty, int soldQty, int maxPerUser) {
        TimeDealProduct tdp = TimeDealProduct.builder()
                .product(product)
                .dealPrice(dealPrice)
                .dealQuantity(dealQty)
                .maxPerUser(maxPerUser)
                .build();
        ReflectionTestUtils.setField(tdp, "id", id);
        ReflectionTestUtils.setField(tdp, "timeDeal", timeDeal);
        ReflectionTestUtils.setField(tdp, "soldQuantity", soldQty);
        return tdp;
    }

    private TimeDealPurchase createTimeDealPurchase(Long id, String userId, TimeDealProduct tdp, int qty) {
        TimeDealPurchase purchase = TimeDealPurchase.builder()
                .userId(userId)
                .timeDealProduct(tdp)
                .quantity(qty)
                .purchasePrice(tdp.getDealPrice())
                .build();
        ReflectionTestUtils.setField(purchase, "id", id);
        return purchase;
    }

    @Nested
    @DisplayName("getAllTimeDeals")
    class GetAllTimeDeals {

        @Test
        @DisplayName("should_returnTimeDeals_when_called")
        void should_returnTimeDeals_when_called() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            TimeDeal timeDeal = createTimeDeal(1L, "Flash Sale", TimeDealStatus.ACTIVE,
                    LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(5));
            Page<TimeDeal> page = new PageImpl<>(List.of(timeDeal), pageable, 1);
            when(timeDealRepository.findAll(pageable)).thenReturn(page);

            // when
            Page<TimeDealResponse> result = timeDealService.getAllTimeDeals(pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("createTimeDeal")
    class CreateTimeDeal {

        @Test
        @DisplayName("should_createTimeDeal_when_valid")
        void should_createTimeDeal_when_valid() {
            // given
            Product product = createProduct(1L, "Product A", BigDecimal.valueOf(10000));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            TimeDeal savedTimeDeal = createTimeDeal(1L, "Flash Sale", TimeDealStatus.SCHEDULED,
                    LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(5));
            TimeDealProduct tdp = createTimeDealProduct(1L, savedTimeDeal, product,
                    BigDecimal.valueOf(5000), 50, 0, 2);
            savedTimeDeal.getProducts().add(tdp);
            when(timeDealRepository.save(any(TimeDeal.class))).thenReturn(savedTimeDeal);

            TimeDealCreateRequest request = TimeDealCreateRequest.builder()
                    .name("Flash Sale")
                    .description("desc")
                    .startsAt(LocalDateTime.now().plusHours(1))
                    .endsAt(LocalDateTime.now().plusHours(5))
                    .products(List.of(
                            TimeDealCreateRequest.TimeDealProductRequest.builder()
                                    .productId(1L)
                                    .dealPrice(BigDecimal.valueOf(5000))
                                    .dealQuantity(50)
                                    .maxPerUser(2)
                                    .build()
                    ))
                    .build();

            // when
            TimeDealResponse result = timeDealService.createTimeDeal(request);

            // then
            assertThat(result).isNotNull();
            verify(timeDealRepository).save(any(TimeDeal.class));
        }

        @Test
        @DisplayName("should_throwException_when_productNotFound")
        void should_throwException_when_productNotFound() {
            // given
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            TimeDealCreateRequest request = TimeDealCreateRequest.builder()
                    .name("Flash Sale")
                    .startsAt(LocalDateTime.now().plusHours(1))
                    .endsAt(LocalDateTime.now().plusHours(5))
                    .products(List.of(
                            TimeDealCreateRequest.TimeDealProductRequest.builder()
                                    .productId(999L)
                                    .dealPrice(BigDecimal.valueOf(5000))
                                    .dealQuantity(50)
                                    .maxPerUser(2)
                                    .build()
                    ))
                    .build();

            // when & then
            assertThatThrownBy(() -> timeDealService.createTimeDeal(request))
                    .isInstanceOf(CustomBusinessException.class);
        }

        @Test
        @DisplayName("should_throwException_when_invalidPeriod")
        void should_throwException_when_invalidPeriod() {
            // given
            TimeDealCreateRequest request = TimeDealCreateRequest.builder()
                    .name("Flash Sale")
                    .startsAt(LocalDateTime.now().plusHours(5))
                    .endsAt(LocalDateTime.now().plusHours(1))
                    .products(List.of(
                            TimeDealCreateRequest.TimeDealProductRequest.builder()
                                    .productId(1L)
                                    .dealPrice(BigDecimal.valueOf(5000))
                                    .dealQuantity(50)
                                    .maxPerUser(2)
                                    .build()
                    ))
                    .build();

            // when & then
            assertThatThrownBy(() -> timeDealService.createTimeDeal(request))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("getTimeDeal")
    class GetTimeDeal {

        @Test
        @DisplayName("should_returnTimeDeal_when_found")
        void should_returnTimeDeal_when_found() {
            // given
            TimeDeal timeDeal = createTimeDeal(1L, "Flash Sale", TimeDealStatus.ACTIVE,
                    LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(5));
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
            TimeDeal timeDeal = createTimeDeal(1L, "Flash Sale", TimeDealStatus.ACTIVE,
                    LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(5));
            when(timeDealRepository.findActiveDeals(eq(TimeDealStatus.ACTIVE), any(LocalDateTime.class)))
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
            Product product = createProduct(1L, "Product A", BigDecimal.valueOf(10000));
            TimeDeal timeDeal = createTimeDeal(1L, "Flash Sale", TimeDealStatus.ACTIVE,
                    LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(5));
            TimeDealProduct tdp = createTimeDealProduct(10L, timeDeal, product,
                    BigDecimal.valueOf(5000), 50, 0, 2);

            when(timeDealProductRepository.findByIdWithProductAndDeal(10L))
                    .thenReturn(Optional.of(tdp));
            when(timeDealRedisService.purchaseProduct(1L, 1L, "user1", 1, 2)).thenReturn(49L);

            TimeDealPurchase purchase = createTimeDealPurchase(1L, "user1", tdp, 1);
            when(timeDealPurchaseRepository.save(any(TimeDealPurchase.class))).thenReturn(purchase);
            when(timeDealProductRepository.save(any(TimeDealProduct.class))).thenReturn(tdp);

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
            Product product = createProduct(1L, "Product A", BigDecimal.valueOf(10000));
            TimeDeal timeDeal = createTimeDeal(1L, "Flash Sale", TimeDealStatus.SCHEDULED,
                    LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(5));
            TimeDealProduct tdp = createTimeDealProduct(10L, timeDeal, product,
                    BigDecimal.valueOf(5000), 50, 0, 2);

            when(timeDealProductRepository.findByIdWithProductAndDeal(10L))
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
            Product product = createProduct(1L, "Product A", BigDecimal.valueOf(10000));
            TimeDeal timeDeal = createTimeDeal(1L, "Flash Sale", TimeDealStatus.ACTIVE,
                    LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(5));
            TimeDealProduct tdp = createTimeDealProduct(10L, timeDeal, product,
                    BigDecimal.valueOf(5000), 50, 0, 2);

            when(timeDealProductRepository.findByIdWithProductAndDeal(10L))
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
            Product product = createProduct(1L, "Product A", BigDecimal.valueOf(10000));
            TimeDeal timeDeal = createTimeDeal(1L, "Flash Sale", TimeDealStatus.ACTIVE,
                    LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(5));
            TimeDealProduct tdp = createTimeDealProduct(10L, timeDeal, product,
                    BigDecimal.valueOf(5000), 50, 0, 2);

            when(timeDealProductRepository.findByIdWithProductAndDeal(10L))
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
            Product product = createProduct(1L, "Product A", BigDecimal.valueOf(10000));
            TimeDeal timeDeal = createTimeDeal(1L, "Flash Sale", TimeDealStatus.ACTIVE,
                    LocalDateTime.now().minusHours(5), LocalDateTime.now().minusHours(1));
            TimeDealProduct tdp = createTimeDealProduct(10L, timeDeal, product,
                    BigDecimal.valueOf(5000), 50, 0, 2);

            when(timeDealProductRepository.findByIdWithProductAndDeal(10L))
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
            Product product = createProduct(1L, "Product A", BigDecimal.valueOf(10000));
            TimeDeal timeDeal = createTimeDeal(1L, "Flash Sale", TimeDealStatus.ACTIVE,
                    LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(5));
            TimeDealProduct tdp = createTimeDealProduct(10L, timeDeal, product,
                    BigDecimal.valueOf(5000), 50, 1, 2);
            TimeDealPurchase purchase = createTimeDealPurchase(1L, "user1", tdp, 1);

            when(timeDealPurchaseRepository.findByUserIdWithProduct("user1"))
                    .thenReturn(List.of(purchase));

            // when
            List<TimeDealPurchaseResponse> result = timeDealService.getUserPurchases("user1");

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("cancelTimeDeal")
    class CancelTimeDeal {

        @Test
        @DisplayName("should_cancelTimeDeal_when_valid")
        void should_cancelTimeDeal_when_valid() {
            // given
            Product product = createProduct(1L, "Product A", BigDecimal.valueOf(10000));
            TimeDeal timeDeal = createTimeDeal(1L, "Flash Sale", TimeDealStatus.ACTIVE,
                    LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(5));
            TimeDealProduct tdp = createTimeDealProduct(10L, timeDeal, product,
                    BigDecimal.valueOf(5000), 50, 0, 2);
            timeDeal.getProducts().add(tdp);

            when(timeDealRepository.findById(1L)).thenReturn(Optional.of(timeDeal));
            when(timeDealRepository.save(any(TimeDeal.class))).thenReturn(timeDeal);

            // when
            timeDealService.cancelTimeDeal(1L);

            // then
            verify(timeDealRepository).save(any(TimeDeal.class));
            verify(timeDealRedisService).deleteTimeDealCache(1L, 1L);
        }

        @Test
        @DisplayName("should_throwException_when_timeDealNotFound")
        void should_throwException_when_timeDealNotFound() {
            // given
            when(timeDealRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> timeDealService.cancelTimeDeal(999L))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }
}
