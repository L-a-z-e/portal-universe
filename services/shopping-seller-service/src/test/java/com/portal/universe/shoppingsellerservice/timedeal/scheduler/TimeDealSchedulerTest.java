package com.portal.universe.shoppingsellerservice.timedeal.scheduler;

import com.portal.universe.event.seller.TimeDealUpdatedEvent;
import com.portal.universe.shoppingsellerservice.support.fixture.TimeDealFixture;
import com.portal.universe.shoppingsellerservice.timedeal.domain.TimeDeal;
import com.portal.universe.shoppingsellerservice.timedeal.domain.TimeDealStatus;
import com.portal.universe.shoppingsellerservice.timedeal.repository.TimeDealRepository;
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
@DisplayName("TimeDealScheduler")
class TimeDealSchedulerTest {

    @Mock
    private TimeDealRepository timeDealRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TimeDealScheduler timeDealScheduler;

    @Nested
    @DisplayName("updateTimeDealStatus")
    class UpdateTimeDealStatus {

        @Test
        @DisplayName("should activate scheduled deal and publish event when start time reached")
        void should_activate_scheduled_deal() {
            // given
            TimeDeal deal = TimeDealFixture.builder()
                    .id(1L).sellerId(10L).name("Flash Sale")
                    .status(TimeDealStatus.SCHEDULED)
                    .startsAt(Instant.now().minus(1, ChronoUnit.HOURS))
                    .endsAt(Instant.now().plus(5, ChronoUnit.HOURS))
                    .build();
            deal.addProduct(TimeDealFixture.createProduct(deal, 100L));
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
        @DisplayName("should end active deal and publish event when end time reached")
        void should_end_active_deal() {
            // given
            TimeDeal deal = TimeDealFixture.builder()
                    .id(2L).sellerId(10L).name("Old Deal")
                    .status(TimeDealStatus.ACTIVE)
                    .startsAt(Instant.now().minus(5, ChronoUnit.HOURS))
                    .endsAt(Instant.now().minus(1, ChronoUnit.HOURS))
                    .build();
            deal.addProduct(TimeDealFixture.createProduct(deal, 100L));
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
        @DisplayName("should do nothing when no deals need status transition")
        void should_do_nothing_when_no_transitions() {
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
        @DisplayName("should activate and end deals when both exist simultaneously")
        void should_handle_both_activate_and_end() {
            // given
            TimeDeal toActivate = TimeDealFixture.builder()
                    .id(1L).sellerId(10L).name("New Deal")
                    .status(TimeDealStatus.SCHEDULED)
                    .startsAt(Instant.now().minus(1, ChronoUnit.HOURS))
                    .endsAt(Instant.now().plus(5, ChronoUnit.HOURS))
                    .build();
            toActivate.addProduct(TimeDealFixture.createProduct(toActivate, 100L));

            TimeDeal toEnd = TimeDealFixture.builder()
                    .id(2L).sellerId(10L).name("Old Deal")
                    .status(TimeDealStatus.ACTIVE)
                    .startsAt(Instant.now().minus(5, ChronoUnit.HOURS))
                    .endsAt(Instant.now().minus(1, ChronoUnit.HOURS))
                    .build();
            toEnd.addProduct(TimeDealFixture.createProduct(toEnd, 200L));

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
}
