package com.portal.universe.shoppingservice.queue.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.queue.domain.QueueEntry;
import com.portal.universe.shoppingservice.queue.domain.QueueStatus;
import com.portal.universe.shoppingservice.queue.domain.WaitingQueue;
import com.portal.universe.shoppingservice.queue.dto.QueueStatusResponse;
import com.portal.universe.shoppingservice.queue.repository.QueueEntryRepository;
import com.portal.universe.shoppingservice.queue.repository.WaitingQueueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueServiceImplTest {

    @Mock
    private WaitingQueueRepository waitingQueueRepository;

    @Mock
    private QueueEntryRepository queueEntryRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private QueueServiceImpl queueService;

    private WaitingQueue createWaitingQueue(Long id, String eventType, Long eventId,
                                             int maxCapacity, int batchSize, int intervalSec, boolean active) {
        WaitingQueue queue = WaitingQueue.builder()
                .eventType(eventType)
                .eventId(eventId)
                .maxCapacity(maxCapacity)
                .entryBatchSize(batchSize)
                .entryIntervalSeconds(intervalSec)
                .build();
        ReflectionTestUtils.setField(queue, "id", id);
        ReflectionTestUtils.setField(queue, "isActive", active);
        return queue;
    }

    private QueueEntry createQueueEntry(Long id, WaitingQueue queue, String userId, QueueStatus status) {
        QueueEntry entry = QueueEntry.builder()
                .queue(queue)
                .userId(userId)
                .build();
        ReflectionTestUtils.setField(entry, "id", id);
        ReflectionTestUtils.setField(entry, "status", status);
        return entry;
    }

    @Nested
    @DisplayName("enterQueue")
    class EnterQueue {

        @Test
        @DisplayName("should_enterQueue_when_valid")
        void should_enterQueue_when_valid() {
            // given
            WaitingQueue queue = createWaitingQueue(1L, "TIMEDEAL", 100L, 50, 10, 30, true);
            when(waitingQueueRepository.findByEventTypeAndEventIdAndIsActiveTrue("TIMEDEAL", 100L))
                    .thenReturn(Optional.of(queue));
            when(queueEntryRepository.findByQueueAndUserId(queue, "user1"))
                    .thenReturn(Optional.empty());

            QueueEntry savedEntry = createQueueEntry(1L, queue, "user1", QueueStatus.WAITING);
            when(queueEntryRepository.save(any(QueueEntry.class))).thenReturn(savedEntry);

            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.add(anyString(), anyString(), anyDouble())).thenReturn(true);
            when(zSetOperations.rank(anyString(), anyString())).thenReturn(0L);
            when(zSetOperations.zCard(anyString())).thenReturn(1L);

            // when
            QueueStatusResponse result = queueService.enterQueue("TIMEDEAL", 100L, "user1");

            // then
            assertThat(result).isNotNull();
            verify(queueEntryRepository).save(any(QueueEntry.class));
        }

        @Test
        @DisplayName("should_throwException_when_queueNotFound")
        void should_throwException_when_queueNotFound() {
            // given
            when(waitingQueueRepository.findByEventTypeAndEventIdAndIsActiveTrue("TIMEDEAL", 999L))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> queueService.enterQueue("TIMEDEAL", 999L, "user1"))
                    .isInstanceOf(CustomBusinessException.class);
        }

        @Test
        @DisplayName("should_returnExistingStatus_when_alreadyWaiting")
        void should_returnExistingStatus_when_alreadyWaiting() {
            // given
            WaitingQueue queue = createWaitingQueue(1L, "TIMEDEAL", 100L, 50, 10, 30, true);
            QueueEntry existingEntry = createQueueEntry(1L, queue, "user1", QueueStatus.WAITING);

            when(waitingQueueRepository.findByEventTypeAndEventIdAndIsActiveTrue("TIMEDEAL", 100L))
                    .thenReturn(Optional.of(queue));
            when(queueEntryRepository.findByQueueAndUserId(queue, "user1"))
                    .thenReturn(Optional.of(existingEntry));

            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.rank(anyString(), anyString())).thenReturn(0L);
            when(zSetOperations.zCard(anyString())).thenReturn(1L);

            // when
            QueueStatusResponse result = queueService.enterQueue("TIMEDEAL", 100L, "user1");

            // then
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(QueueStatus.WAITING);
            verify(queueEntryRepository, never()).save(any(QueueEntry.class));
        }

        @Test
        @DisplayName("should_returnEnteredStatus_when_alreadyEntered")
        void should_returnEnteredStatus_when_alreadyEntered() {
            // given
            WaitingQueue queue = createWaitingQueue(1L, "TIMEDEAL", 100L, 50, 10, 30, true);
            QueueEntry existingEntry = createQueueEntry(1L, queue, "user1", QueueStatus.ENTERED);

            when(waitingQueueRepository.findByEventTypeAndEventIdAndIsActiveTrue("TIMEDEAL", 100L))
                    .thenReturn(Optional.of(queue));
            when(queueEntryRepository.findByQueueAndUserId(queue, "user1"))
                    .thenReturn(Optional.of(existingEntry));

            // when
            QueueStatusResponse result = queueService.enterQueue("TIMEDEAL", 100L, "user1");

            // then
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(QueueStatus.ENTERED);
        }
    }

    @Nested
    @DisplayName("getQueueStatus")
    class GetQueueStatus {

        @Test
        @DisplayName("should_returnQueueStatus_when_found")
        void should_returnQueueStatus_when_found() {
            // given
            WaitingQueue queue = createWaitingQueue(1L, "TIMEDEAL", 100L, 50, 10, 30, true);
            QueueEntry entry = createQueueEntry(1L, queue, "user1", QueueStatus.WAITING);

            when(waitingQueueRepository.findByEventTypeAndEventIdAndIsActiveTrue("TIMEDEAL", 100L))
                    .thenReturn(Optional.of(queue));
            when(queueEntryRepository.findByQueueAndUserId(queue, "user1"))
                    .thenReturn(Optional.of(entry));

            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.rank(anyString(), anyString())).thenReturn(5L);
            when(zSetOperations.zCard(anyString())).thenReturn(20L);

            // when
            QueueStatusResponse result = queueService.getQueueStatus("TIMEDEAL", 100L, "user1");

            // then
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(QueueStatus.WAITING);
        }
    }

    @Nested
    @DisplayName("getQueueStatusByToken")
    class GetQueueStatusByToken {

        @Test
        @DisplayName("should_returnQueueStatus_when_tokenFound")
        void should_returnQueueStatus_when_tokenFound() {
            // given
            WaitingQueue queue = createWaitingQueue(1L, "TIMEDEAL", 100L, 50, 10, 30, true);
            QueueEntry entry = createQueueEntry(1L, queue, "user1", QueueStatus.ENTERED);

            when(queueEntryRepository.findByEntryToken("test-token")).thenReturn(Optional.of(entry));

            // when
            QueueStatusResponse result = queueService.getQueueStatusByToken("test-token");

            // then
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(QueueStatus.ENTERED);
        }

        @Test
        @DisplayName("should_throwException_when_tokenNotFound")
        void should_throwException_when_tokenNotFound() {
            // given
            when(queueEntryRepository.findByEntryToken("invalid-token")).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> queueService.getQueueStatusByToken("invalid-token"))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("leaveQueue")
    class LeaveQueue {

        @Test
        @DisplayName("should_leaveQueue_when_valid")
        void should_leaveQueue_when_valid() {
            // given
            WaitingQueue queue = createWaitingQueue(1L, "TIMEDEAL", 100L, 50, 10, 30, true);
            QueueEntry entry = createQueueEntry(1L, queue, "user1", QueueStatus.WAITING);

            when(waitingQueueRepository.findByEventTypeAndEventId("TIMEDEAL", 100L))
                    .thenReturn(Optional.of(queue));
            when(queueEntryRepository.findByQueueAndUserId(queue, "user1"))
                    .thenReturn(Optional.of(entry));
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

            // when
            queueService.leaveQueue("TIMEDEAL", 100L, "user1");

            // then
            verify(queueEntryRepository).save(any(QueueEntry.class));
            verify(zSetOperations).remove(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("leaveQueueByToken")
    class LeaveQueueByToken {

        @Test
        @DisplayName("should_leaveQueue_when_tokenValid")
        void should_leaveQueue_when_tokenValid() {
            // given
            WaitingQueue queue = createWaitingQueue(1L, "TIMEDEAL", 100L, 50, 10, 30, true);
            QueueEntry entry = createQueueEntry(1L, queue, "user1", QueueStatus.WAITING);

            when(queueEntryRepository.findByEntryToken("test-token")).thenReturn(Optional.of(entry));
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

            // when
            queueService.leaveQueueByToken("test-token");

            // then
            verify(queueEntryRepository).save(any(QueueEntry.class));
        }

        @Test
        @DisplayName("should_throwException_when_tokenNotFound")
        void should_throwException_when_tokenNotFound() {
            // given
            when(queueEntryRepository.findByEntryToken("invalid-token")).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> queueService.leaveQueueByToken("invalid-token"))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("processEntries")
    class ProcessEntries {

        @Test
        @DisplayName("should_processEntries_when_slotsAvailable")
        void should_processEntries_when_slotsAvailable() {
            // given
            WaitingQueue queue = createWaitingQueue(1L, "TIMEDEAL", 100L, 50, 10, 30, true);
            QueueEntry entry = createQueueEntry(1L, queue, "user1", QueueStatus.WAITING);

            when(waitingQueueRepository.findByEventTypeAndEventIdAndIsActiveTrue("TIMEDEAL", 100L))
                    .thenReturn(Optional.of(queue));
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(setOperations.size(anyString())).thenReturn(5L);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

            @SuppressWarnings("unchecked")
            ZSetOperations.TypedTuple<String> tuple = mock(ZSetOperations.TypedTuple.class);
            when(tuple.getValue()).thenReturn(entry.getEntryToken());
            when(zSetOperations.popMin(anyString(), anyLong())).thenReturn(Set.of(tuple));

            when(queueEntryRepository.findByEntryToken(entry.getEntryToken()))
                    .thenReturn(Optional.of(entry));

            // when
            queueService.processEntries("TIMEDEAL", 100L);

            // then
            verify(queueEntryRepository).save(any(QueueEntry.class));
            verify(setOperations).add(anyString(), anyString());
        }

        @Test
        @DisplayName("should_notProcess_when_queueNotActive")
        void should_notProcess_when_queueNotActive() {
            // given
            when(waitingQueueRepository.findByEventTypeAndEventIdAndIsActiveTrue("TIMEDEAL", 100L))
                    .thenReturn(Optional.empty());

            // when
            queueService.processEntries("TIMEDEAL", 100L);

            // then
            verify(queueEntryRepository, never()).save(any(QueueEntry.class));
        }
    }

    @Nested
    @DisplayName("validateEntry")
    class ValidateEntry {

        @Test
        @DisplayName("should_returnTrue_when_entered")
        void should_returnTrue_when_entered() {
            // given
            WaitingQueue queue = createWaitingQueue(1L, "TIMEDEAL", 100L, 50, 10, 30, true);
            QueueEntry entry = createQueueEntry(1L, queue, "user1", QueueStatus.ENTERED);

            when(waitingQueueRepository.findByEventTypeAndEventIdAndIsActiveTrue("TIMEDEAL", 100L))
                    .thenReturn(Optional.of(queue));
            when(queueEntryRepository.findByQueueAndUserIdAndStatus(queue, "user1", QueueStatus.ENTERED))
                    .thenReturn(Optional.of(entry));

            // when
            boolean result = queueService.validateEntry("TIMEDEAL", 100L, "user1");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should_returnTrue_when_noActiveQueue")
        void should_returnTrue_when_noActiveQueue() {
            // given
            when(waitingQueueRepository.findByEventTypeAndEventIdAndIsActiveTrue("TIMEDEAL", 100L))
                    .thenReturn(Optional.empty());

            // when
            boolean result = queueService.validateEntry("TIMEDEAL", 100L, "user1");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should_returnFalse_when_notEntered")
        void should_returnFalse_when_notEntered() {
            // given
            WaitingQueue queue = createWaitingQueue(1L, "TIMEDEAL", 100L, 50, 10, 30, true);

            when(waitingQueueRepository.findByEventTypeAndEventIdAndIsActiveTrue("TIMEDEAL", 100L))
                    .thenReturn(Optional.of(queue));
            when(queueEntryRepository.findByQueueAndUserIdAndStatus(queue, "user1", QueueStatus.ENTERED))
                    .thenReturn(Optional.empty());

            // when
            boolean result = queueService.validateEntry("TIMEDEAL", 100L, "user1");

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("activateQueue")
    class ActivateQueue {

        @Test
        @DisplayName("should_activateQueue_when_exists")
        void should_activateQueue_when_exists() {
            // given
            WaitingQueue queue = createWaitingQueue(1L, "TIMEDEAL", 100L, 50, 10, 30, false);
            when(waitingQueueRepository.findByEventTypeAndEventId("TIMEDEAL", 100L))
                    .thenReturn(Optional.of(queue));
            when(waitingQueueRepository.save(any(WaitingQueue.class))).thenReturn(queue);

            // when
            queueService.activateQueue("TIMEDEAL", 100L, 50, 10, 30);

            // then
            verify(waitingQueueRepository).save(any(WaitingQueue.class));
        }

        @Test
        @DisplayName("should_createAndActivate_when_notExists")
        void should_createAndActivate_when_notExists() {
            // given
            when(waitingQueueRepository.findByEventTypeAndEventId("TIMEDEAL", 100L))
                    .thenReturn(Optional.empty());
            WaitingQueue newQueue = createWaitingQueue(1L, "TIMEDEAL", 100L, 50, 10, 30, true);
            when(waitingQueueRepository.save(any(WaitingQueue.class))).thenReturn(newQueue);

            // when
            queueService.activateQueue("TIMEDEAL", 100L, 50, 10, 30);

            // then
            verify(waitingQueueRepository).save(any(WaitingQueue.class));
        }
    }

    @Nested
    @DisplayName("deactivateQueue")
    class DeactivateQueue {

        @Test
        @DisplayName("should_deactivateQueue_when_exists")
        void should_deactivateQueue_when_exists() {
            // given
            WaitingQueue queue = createWaitingQueue(1L, "TIMEDEAL", 100L, 50, 10, 30, true);
            when(waitingQueueRepository.findByEventTypeAndEventId("TIMEDEAL", 100L))
                    .thenReturn(Optional.of(queue));
            when(waitingQueueRepository.save(any(WaitingQueue.class))).thenReturn(queue);

            // when
            queueService.deactivateQueue("TIMEDEAL", 100L);

            // then
            verify(waitingQueueRepository).save(any(WaitingQueue.class));
            verify(redisTemplate).delete("queue:waiting:TIMEDEAL:100");
            verify(redisTemplate).delete("queue:entered:TIMEDEAL:100");
        }

        @Test
        @DisplayName("should_throwException_when_notFound")
        void should_throwException_when_notFound() {
            // given
            when(waitingQueueRepository.findByEventTypeAndEventId("TIMEDEAL", 999L))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> queueService.deactivateQueue("TIMEDEAL", 999L))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }
}
