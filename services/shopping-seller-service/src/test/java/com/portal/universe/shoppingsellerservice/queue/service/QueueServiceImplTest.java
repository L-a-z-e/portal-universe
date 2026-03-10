package com.portal.universe.shoppingsellerservice.queue.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.event.seller.QueueActivatedEvent;
import com.portal.universe.event.seller.QueueDeactivatedEvent;
import com.portal.universe.shoppingsellerservice.common.exception.SellerErrorCode;
import com.portal.universe.shoppingsellerservice.queue.domain.QueueEntryStatus;
import com.portal.universe.shoppingsellerservice.queue.domain.WaitingQueue;
import com.portal.universe.shoppingsellerservice.queue.dto.QueueActivateRequest;
import com.portal.universe.shoppingsellerservice.queue.dto.QueueStatusResponse;
import com.portal.universe.shoppingsellerservice.queue.repository.QueueEntryRepository;
import com.portal.universe.shoppingsellerservice.queue.repository.WaitingQueueRepository;
import com.portal.universe.shoppingsellerservice.support.fixture.QueueFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("QueueServiceImpl")
class QueueServiceImplTest {

    @Mock
    private WaitingQueueRepository waitingQueueRepository;

    @Mock
    private QueueEntryRepository queueEntryRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private QueueServiceImpl queueService;

    @Nested
    @DisplayName("activateQueue")
    class ActivateQueue {

        @Test
        @DisplayName("should create and activate new queue when not exists")
        void should_activate_new_queue() {
            // given
            QueueActivateRequest request = new QueueActivateRequest(1000, 100, 30);
            when(waitingQueueRepository.findByEventTypeAndEventId("TIME_DEAL", 1L))
                    .thenReturn(Optional.empty());
            when(waitingQueueRepository.save(any(WaitingQueue.class))).thenAnswer(invocation -> {
                WaitingQueue saved = invocation.getArgument(0);
                org.springframework.test.util.ReflectionTestUtils.setField(saved, "id", 1L);
                return saved;
            });
            when(queueEntryRepository.countByQueueIdAndStatus(any(), eq(QueueEntryStatus.WAITING))).thenReturn(0L);
            when(queueEntryRepository.countByQueueIdAndStatus(any(), eq(QueueEntryStatus.ENTERED))).thenReturn(0L);

            // when
            QueueStatusResponse response = queueService.activateQueue("TIME_DEAL", 1L, request);

            // then
            assertThat(response.isActive()).isTrue();
            assertThat(response.maxCapacity()).isEqualTo(1000);
            assertThat(response.entryBatchSize()).isEqualTo(100);
            verify(waitingQueueRepository).save(any(WaitingQueue.class));
            verify(eventPublisher).publishEvent(any(QueueActivatedEvent.class));
        }

        @Test
        @DisplayName("should activate existing inactive queue")
        void should_activate_existing_queue() {
            // given
            WaitingQueue queue = QueueFixture.builder().id(1L).build(); // isActive=false by default
            QueueActivateRequest request = new QueueActivateRequest(1000, 100, 30);
            when(waitingQueueRepository.findByEventTypeAndEventId("TIME_DEAL", 1L))
                    .thenReturn(Optional.of(queue));
            when(waitingQueueRepository.save(any(WaitingQueue.class))).thenReturn(queue);
            when(queueEntryRepository.countByQueueIdAndStatus(any(), eq(QueueEntryStatus.WAITING))).thenReturn(0L);
            when(queueEntryRepository.countByQueueIdAndStatus(any(), eq(QueueEntryStatus.ENTERED))).thenReturn(0L);

            // when
            QueueStatusResponse response = queueService.activateQueue("TIME_DEAL", 1L, request);

            // then
            assertThat(response.isActive()).isTrue();
            verify(eventPublisher).publishEvent(any(QueueActivatedEvent.class));
        }

        @Test
        @DisplayName("should throw QUEUE_ALREADY_ACTIVE when queue is already active")
        void should_throw_when_already_active() {
            // given
            WaitingQueue activeQueue = QueueFixture.builder().id(1L).active(true).build();
            QueueActivateRequest request = new QueueActivateRequest(1000, 100, 30);
            when(waitingQueueRepository.findByEventTypeAndEventId("TIME_DEAL", 1L))
                    .thenReturn(Optional.of(activeQueue));

            // when & then
            assertThatThrownBy(() -> queueService.activateQueue("TIME_DEAL", 1L, request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.QUEUE_ALREADY_ACTIVE));
        }
    }

    @Nested
    @DisplayName("deactivateQueue")
    class DeactivateQueue {

        @Test
        @DisplayName("should deactivate active queue and publish event")
        void should_deactivate_queue() {
            // given
            WaitingQueue queue = QueueFixture.builder().id(1L).active(true).build();
            when(waitingQueueRepository.findByEventTypeAndEventId("TIME_DEAL", 1L))
                    .thenReturn(Optional.of(queue));

            // when
            queueService.deactivateQueue("TIME_DEAL", 1L);

            // then
            assertThat(queue.getIsActive()).isFalse();
            verify(eventPublisher).publishEvent(any(QueueDeactivatedEvent.class));
        }

        @Test
        @DisplayName("should throw QUEUE_NOT_FOUND when queue does not exist")
        void should_throw_when_not_found() {
            // given
            when(waitingQueueRepository.findByEventTypeAndEventId("TIME_DEAL", 999L))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> queueService.deactivateQueue("TIME_DEAL", 999L))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.QUEUE_NOT_FOUND));
        }

        @Test
        @DisplayName("should throw QUEUE_NOT_ACTIVE when queue is already inactive")
        void should_throw_when_not_active() {
            // given
            WaitingQueue inactiveQueue = QueueFixture.builder().id(1L).build(); // isActive=false
            when(waitingQueueRepository.findByEventTypeAndEventId("TIME_DEAL", 1L))
                    .thenReturn(Optional.of(inactiveQueue));

            // when & then
            assertThatThrownBy(() -> queueService.deactivateQueue("TIME_DEAL", 1L))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.QUEUE_NOT_ACTIVE));
        }
    }

    @Nested
    @DisplayName("getQueueStatus")
    class GetQueueStatus {

        @Test
        @DisplayName("should return queue status with entry counts")
        void should_return_queue_status() {
            // given
            WaitingQueue queue = QueueFixture.builder().id(1L).active(true).build();
            when(waitingQueueRepository.findByEventTypeAndEventId("TIME_DEAL", 1L))
                    .thenReturn(Optional.of(queue));
            when(queueEntryRepository.countByQueueIdAndStatus(1L, QueueEntryStatus.WAITING)).thenReturn(50L);
            when(queueEntryRepository.countByQueueIdAndStatus(1L, QueueEntryStatus.ENTERED)).thenReturn(200L);

            // when
            QueueStatusResponse response = queueService.getQueueStatus("TIME_DEAL", 1L);

            // then
            assertThat(response.isActive()).isTrue();
            assertThat(response.waitingCount()).isEqualTo(50);
            assertThat(response.enteredCount()).isEqualTo(200);
        }

        @Test
        @DisplayName("should throw QUEUE_NOT_FOUND when queue does not exist")
        void should_throw_when_not_found() {
            // given
            when(waitingQueueRepository.findByEventTypeAndEventId("TIME_DEAL", 999L))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> queueService.getQueueStatus("TIME_DEAL", 999L))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.QUEUE_NOT_FOUND));
        }
    }
}
