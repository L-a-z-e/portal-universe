package com.portal.universe.shoppingservice.timedeal.scheduler;

import com.portal.universe.shoppingservice.timedeal.domain.TimeDeal;
import com.portal.universe.shoppingservice.timedeal.domain.TimeDealProduct;
import com.portal.universe.shoppingservice.timedeal.redis.TimeDealRedisService;
import com.portal.universe.shoppingservice.timedeal.repository.TimeDealRepository;
import com.portal.universe.shoppingservice.support.fixture.TimeDealFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
        TimeDeal deal = TimeDealFixture.builder()
                .name("테스트 타임딜")
                .startsAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .endsAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        deal.addProduct(TimeDealProduct.builder()
                .productId(10L)
                .dealPrice(new BigDecimal("5000"))
                .dealQuantity(50)
                .maxPerUser(3)
                .build());
        return deal;
    }

    @Test
    @DisplayName("should activate scheduled deals and initialize Redis stock")
    void should_activate_scheduled_deals() {
        TimeDeal deal = createTimeDealWithProduct();
        when(timeDealRepository.findDealsToStart(any(Instant.class)))
                .thenReturn(List.of(deal));
        when(timeDealRepository.findDealsToEnd(any(Instant.class)))
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
        when(timeDealRepository.findDealsToStart(any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(timeDealRepository.findDealsToEnd(any(Instant.class)))
                .thenReturn(List.of(deal));

        timeDealScheduler.updateTimeDealStatus();

        verify(timeDealRepository).saveAll(List.of(deal));
        verify(timeDealRedisService).deleteTimeDealCache(1L, 10L);
    }

    @Test
    @DisplayName("should handle empty lists gracefully")
    void should_handle_empty_lists() {
        when(timeDealRepository.findDealsToStart(any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(timeDealRepository.findDealsToEnd(any(Instant.class)))
                .thenReturn(Collections.emptyList());

        timeDealScheduler.updateTimeDealStatus();

        verify(timeDealRepository, never()).saveAll(anyList());
        verify(timeDealRedisService, never()).initializeStock(anyLong(), anyLong(), anyInt());
        verify(timeDealRedisService, never()).deleteTimeDealCache(anyLong(), anyLong());
    }

    @Test
    @DisplayName("should initialize Redis stock for all products in a deal")
    void should_initialize_redis_stock_for_all_products() {
        TimeDeal deal = TimeDealFixture.builder()
                .id(2L).name("복수 상품 딜")
                .startsAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .endsAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        deal.addProduct(TimeDealProduct.builder()
                .productId(20L).dealPrice(new BigDecimal("5000"))
                .dealQuantity(30).maxPerUser(2).build());
        deal.addProduct(TimeDealProduct.builder()
                .productId(21L).dealPrice(new BigDecimal("15000"))
                .dealQuantity(40).maxPerUser(1).build());

        when(timeDealRepository.findDealsToStart(any(Instant.class)))
                .thenReturn(List.of(deal));
        when(timeDealRepository.findDealsToEnd(any(Instant.class)))
                .thenReturn(Collections.emptyList());

        timeDealScheduler.updateTimeDealStatus();

        verify(timeDealRedisService).initializeStock(2L, 20L, 30);
        verify(timeDealRedisService).initializeStock(2L, 21L, 40);
    }
}
