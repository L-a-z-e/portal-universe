package com.portal.universe.shoppingservice.timedeal.scheduler;

import com.portal.universe.shoppingservice.product.domain.Product;
import com.portal.universe.shoppingservice.timedeal.domain.TimeDeal;
import com.portal.universe.shoppingservice.timedeal.domain.TimeDealProduct;
import com.portal.universe.shoppingservice.timedeal.redis.TimeDealRedisService;
import com.portal.universe.shoppingservice.timedeal.repository.TimeDealRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeDealSchedulerTest {

    @Mock
    private TimeDealRepository timeDealRepository;

    @Mock
    private TimeDealRedisService timeDealRedisService;

    @InjectMocks
    private TimeDealScheduler timeDealScheduler;

    private TimeDeal createTimeDealWithProduct() {
        TimeDeal deal = TimeDeal.builder()
                .name("테스트 타임딜")
                .description("테스트")
                .startsAt(LocalDateTime.now().minusHours(1))
                .endsAt(LocalDateTime.now().plusHours(1))
                .build();
        ReflectionTestUtils.setField(deal, "id", 1L);

        Product product = Product.builder()
                .name("테스트 상품")
                .price(new BigDecimal("10000"))
                .stock(100)
                .build();
        ReflectionTestUtils.setField(product, "id", 10L);

        TimeDealProduct tdp = TimeDealProduct.builder()
                .product(product)
                .dealPrice(new BigDecimal("5000"))
                .dealQuantity(50)
                .maxPerUser(3)
                .build();

        deal.addProduct(tdp);
        return deal;
    }

    @Test
    @DisplayName("should activate scheduled deals and initialize Redis stock")
    void should_activate_scheduled_deals() {
        TimeDeal deal = createTimeDealWithProduct();
        when(timeDealRepository.findDealsToStart(any(LocalDateTime.class)))
                .thenReturn(List.of(deal));
        when(timeDealRepository.findDealsToEnd(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        timeDealScheduler.updateTimeDealStatus();

        verify(timeDealRepository).saveAll(List.of(deal));
        verify(timeDealRedisService).initializeStock(1L, 10L, 50);
    }

    @Test
    @DisplayName("should end active deals and delete Redis cache")
    void should_end_active_deals() {
        TimeDeal deal = createTimeDealWithProduct();
        deal.activate();
        when(timeDealRepository.findDealsToStart(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(timeDealRepository.findDealsToEnd(any(LocalDateTime.class)))
                .thenReturn(List.of(deal));

        timeDealScheduler.updateTimeDealStatus();

        verify(timeDealRepository).saveAll(List.of(deal));
        verify(timeDealRedisService).deleteTimeDealCache(1L, 10L);
    }

    @Test
    @DisplayName("should handle empty lists gracefully")
    void should_handle_empty_lists() {
        when(timeDealRepository.findDealsToStart(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(timeDealRepository.findDealsToEnd(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        timeDealScheduler.updateTimeDealStatus();

        verify(timeDealRepository, never()).saveAll(anyList());
        verify(timeDealRedisService, never()).initializeStock(anyLong(), anyLong(), anyInt());
        verify(timeDealRedisService, never()).deleteTimeDealCache(anyLong(), anyLong());
    }

    @Test
    @DisplayName("should initialize Redis stock for all products in a deal")
    void should_initialize_redis_stock_for_all_products() {
        TimeDeal deal = TimeDeal.builder()
                .name("복수 상품 딜")
                .description("테스트")
                .startsAt(LocalDateTime.now().minusHours(1))
                .endsAt(LocalDateTime.now().plusHours(1))
                .build();
        ReflectionTestUtils.setField(deal, "id", 2L);

        Product product1 = Product.builder()
                .name("상품1").price(new BigDecimal("10000")).stock(100).build();
        ReflectionTestUtils.setField(product1, "id", 20L);

        Product product2 = Product.builder()
                .name("상품2").price(new BigDecimal("20000")).stock(200).build();
        ReflectionTestUtils.setField(product2, "id", 21L);

        deal.addProduct(TimeDealProduct.builder()
                .product(product1).dealPrice(new BigDecimal("5000"))
                .dealQuantity(30).maxPerUser(2).build());
        deal.addProduct(TimeDealProduct.builder()
                .product(product2).dealPrice(new BigDecimal("15000"))
                .dealQuantity(40).maxPerUser(1).build());

        when(timeDealRepository.findDealsToStart(any(LocalDateTime.class)))
                .thenReturn(List.of(deal));
        when(timeDealRepository.findDealsToEnd(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        timeDealScheduler.updateTimeDealStatus();

        verify(timeDealRedisService).initializeStock(2L, 20L, 30);
        verify(timeDealRedisService).initializeStock(2L, 21L, 40);
    }
}
