package com.portal.universe.shoppingsellerservice.timedeal.scheduler;

import com.portal.universe.event.seller.TimeDealUpdatedEvent;
import com.portal.universe.shoppingsellerservice.timedeal.domain.TimeDeal;
import com.portal.universe.shoppingsellerservice.timedeal.domain.TimeDealProduct;
import com.portal.universe.shoppingsellerservice.timedeal.domain.TimeDealStatus;
import com.portal.universe.shoppingsellerservice.timedeal.repository.TimeDealRepository;
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
class TimeDealSchedulerTest {

    @Mock
    private TimeDealRepository timeDealRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TimeDealScheduler timeDealScheduler;

    private TimeDeal createTimeDeal(Long id, Long sellerId, String name,
                                     TimeDealStatus status, Instant startsAt, Instant endsAt) {
        TimeDeal timeDeal = TimeDeal.builder()
                .sellerId(sellerId)
                .name(name)
                .description("desc")
                .startsAt(startsAt)
                .endsAt(endsAt)
                .build();
        ReflectionTestUtils.setField(timeDeal, "id", id);
        ReflectionTestUtils.setField(timeDeal, "status", status);

        TimeDealProduct product = TimeDealProduct.builder()
                .timeDeal(timeDeal)
                .productId(100L)
                .dealPrice(BigDecimal.valueOf(5000))
                .dealQuantity(50)
                .maxPerUser(2)
                .build();
        timeDeal.addProduct(product);

        return timeDeal;
    }

    @Test
    @DisplayName("should_activateScheduledDeals_when_startTimeReached")
    void should_activateScheduledDeals_when_startTimeReached() {
        // given
        TimeDeal deal = createTimeDeal(1L, 10L, "Flash Sale", TimeDealStatus.SCHEDULED,
                Instant.now().minus(1, ChronoUnit.HOURS), Instant.now().plus(5, ChronoUnit.HOURS));
        when(timeDealRepository.findDealsToStart(any(Instant.class))).thenReturn(List.of(deal));
        when(timeDealRepository.findDealsToEnd(any(Instant.class))).thenReturn(Collections.emptyList());

        // when
        timeDealScheduler.updateTimeDealStatus();

        // then
        assertThat(deal.getStatus()).isEqualTo(TimeDealStatus.ACTIVE);
        verify(timeDealRepository).saveAll(List.of(deal));

        ArgumentCaptor<TimeDealUpdatedEvent> captor = ArgumentCaptor.forClass(TimeDealUpdatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("should_endActiveDeals_when_endTimeReached")
    void should_endActiveDeals_when_endTimeReached() {
        // given
        TimeDeal deal = createTimeDeal(2L, 10L, "Old Deal", TimeDealStatus.ACTIVE,
                Instant.now().minus(5, ChronoUnit.HOURS), Instant.now().minus(1, ChronoUnit.HOURS));
        when(timeDealRepository.findDealsToStart(any(Instant.class))).thenReturn(Collections.emptyList());
        when(timeDealRepository.findDealsToEnd(any(Instant.class))).thenReturn(List.of(deal));

        // when
        timeDealScheduler.updateTimeDealStatus();

        // then
        assertThat(deal.getStatus()).isEqualTo(TimeDealStatus.ENDED);
        verify(timeDealRepository).saveAll(List.of(deal));

        ArgumentCaptor<TimeDealUpdatedEvent> captor = ArgumentCaptor.forClass(TimeDealUpdatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("ENDED");
    }

    @Test
    @DisplayName("should_doNothing_when_noDealsToTransition")
    void should_doNothing_when_noDealsToTransition() {
        // given
        when(timeDealRepository.findDealsToStart(any(Instant.class))).thenReturn(Collections.emptyList());
        when(timeDealRepository.findDealsToEnd(any(Instant.class))).thenReturn(Collections.emptyList());

        // when
        timeDealScheduler.updateTimeDealStatus();

        // then
        verify(timeDealRepository, never()).saveAll(any());
        verify(eventPublisher, never()).publishEvent(any(TimeDealUpdatedEvent.class));
    }

    @Test
    @DisplayName("should_activateAndEndDeals_when_bothExist")
    void should_activateAndEndDeals_when_bothExist() {
        // given
        TimeDeal toActivate = createTimeDeal(1L, 10L, "New Deal", TimeDealStatus.SCHEDULED,
                Instant.now().minus(1, ChronoUnit.HOURS), Instant.now().plus(5, ChronoUnit.HOURS));
        TimeDeal toEnd = createTimeDeal(2L, 10L, "Old Deal", TimeDealStatus.ACTIVE,
                Instant.now().minus(5, ChronoUnit.HOURS), Instant.now().minus(1, ChronoUnit.HOURS));

        when(timeDealRepository.findDealsToStart(any(Instant.class))).thenReturn(List.of(toActivate));
        when(timeDealRepository.findDealsToEnd(any(Instant.class))).thenReturn(List.of(toEnd));

        // when
        timeDealScheduler.updateTimeDealStatus();

        // then
        assertThat(toActivate.getStatus()).isEqualTo(TimeDealStatus.ACTIVE);
        assertThat(toEnd.getStatus()).isEqualTo(TimeDealStatus.ENDED);
        verify(timeDealRepository, times(2)).saveAll(any());
        verify(eventPublisher, times(2)).publishEvent(any(TimeDealUpdatedEvent.class));
    }
}
