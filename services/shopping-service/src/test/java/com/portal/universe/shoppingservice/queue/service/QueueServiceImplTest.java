package com.portal.universe.shoppingservice.queue.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.queue.domain.QueueEntry;
import com.portal.universe.shoppingservice.queue.domain.QueueStatus;
import com.portal.universe.shoppingservice.queue.domain.WaitingQueue;
import com.portal.universe.shoppingservice.queue.dto.QueueStatusResponse;
import com.portal.universe.shoppingservice.queue.repository.QueueEntryRepository;
import com.portal.universe.shoppingservice.queue.repository.WaitingQueueRepository;
import com.portal.universe.shoppingservice.support.fixture.QueueFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
    private DefaultRedisScript<List> queueProcessScript;

    private QueueServiceImpl queueService;

    @BeforeEach
    void setUp() {
        queueService = new QueueServiceImpl(
                waitingQueueRepository, queueEntryRepository, redisTemplate, queueProcessScript);
    }

    @Nested
    @DisplayName("enterQueue")
    class EnterQueue {

        @Test
        @DisplayName("should_enterQueue_when_valid")
        void should_enterQueue_when_valid() {
            // given
            WaitingQueue queue = QueueFixture.queueBuilder()
                    .id(1L).eventType("TIMEDEAL").eventId(100L)
                    .maxCapacity(50).entryBatchSize(10).entryIntervalSeconds(30).active(true).build();
            when(waitingQueueRepository.findByEventTypeAndEventIdAndIsActiveTrue("TIMEDEAL", 100L))
                    .thenReturn(Optional.of(queue));
            when(queueEntryRepository.findByQueueAndUserId(queue, "user1"))
                    .thenReturn(Optional.empty());

            QueueEntry savedEntry = QueueFixture.entryBuilder()
                    .id(1L).queue(queue).userId("user1").status(QueueStatus.WAITING).build();
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
            WaitingQueue queue = QueueFixture.queueBuilder()
                    .id(1L).eventType("TIMEDEAL").eventId(100L)
                    .maxCapacity(50).entryBatchSize(10).entryIntervalSeconds(30).active(true).build();
            QueueEntry existingEntry = QueueFixture.entryBuilder()
                    .id(1L).queue(queue).userId("user1").status(QueueStatus.WAITING).build();

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
            WaitingQueue queue = QueueFixture.queueBuilder()
                    .id(1L).eventType("TIMEDEAL").eventId(100L)
                    .maxCapacity(50).entryBatchSize(10).entryIntervalSeconds(30).active(true).build();
            QueueEntry existingEntry = QueueFixture.entryBuilder()
                    .id(1L).queue(queue).userId("user1").status(QueueStatus.ENTERED).build();

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
            WaitingQueue queue = QueueFixture.queueBuilder()
                    .id(1L).eventType("TIMEDEAL").eventId(100L)
                    .maxCapacity(50).entryBatchSize(10).entryIntervalSeconds(30).active(true).build();
            QueueEntry entry = QueueFixture.entryBuilder()
                    .id(1L).queue(queue).userId("user1").status(QueueStatus.WAITING).build();

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
            WaitingQueue queue = QueueFixture.queueBuilder()
                    .id(1L).eventType("TIMEDEAL").eventId(100L)
                    .maxCapacity(50).entryBatchSize(10).entryIntervalSeconds(30).active(true).build();
            QueueEntry entry = QueueFixture.entryBuilder()
                    .id(1L).queue(queue).userId("user1").status(QueueStatus.ENTERED).build();

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
            WaitingQueue queue = QueueFixture.queueBuilder()
                    .id(1L).eventType("TIMEDEAL").eventId(100L)
                    .maxCapacity(50).entryBatchSize(10).entryIntervalSeconds(30).active(true).build();
            QueueEntry entry = QueueFixture.entryBuilder()
                    .id(1L).queue(queue).userId("user1").status(QueueStatus.WAITING).build();

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
            WaitingQueue queue = QueueFixture.queueBuilder()
                    .id(1L).eventType("TIMEDEAL").eventId(100L)
                    .maxCapacity(50).entryBatchSize(10).entryIntervalSeconds(30).active(true).build();
            QueueEntry entry = QueueFixture.entryBuilder()
                    .id(1L).queue(queue).userId("user1").status(QueueStatus.WAITING).build();

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
        @DisplayName("should_processEntries_atomically_via_lua_script")
        void should_processEntries_atomically_via_lua_script() {
            // given
            WaitingQueue queue = QueueFixture.queueBuilder()
                    .id(1L).eventType("TIMEDEAL").eventId(100L)
                    .maxCapacity(50).entryBatchSize(10).entryIntervalSeconds(30).active(true).build();
            QueueEntry entry = QueueFixture.entryBuilder()
                    .id(1L).queue(queue).userId("user1").status(QueueStatus.WAITING).build();

            when(waitingQueueRepository.findByEventTypeAndEventIdAndIsActiveTrue("TIMEDEAL", 100L))
                    .thenReturn(Optional.of(queue));

            List<String> processedTokens = List.of(entry.getEntryToken());
            when(redisTemplate.execute(eq(queueProcessScript), anyList(), any(), any()))
                    .thenReturn(processedTokens);

            when(queueEntryRepository.findByEntryToken(entry.getEntryToken()))
                    .thenReturn(Optional.of(entry));

            // when
            queueService.processEntries("TIMEDEAL", 100L);

            // then
            verify(redisTemplate).execute(
                    eq(queueProcessScript),
                    eq(Arrays.asList("queue:entered:TIMEDEAL:100", "queue:waiting:TIMEDEAL:100")),
                    eq("50"), eq("10"));
            verify(queueEntryRepository).save(any(QueueEntry.class));
        }

        @Test
        @DisplayName("should_notProcess_when_luaReturnsEmpty")
        void should_notProcess_when_luaReturnsEmpty() {
            // given
            WaitingQueue queue = QueueFixture.queueBuilder()
                    .id(1L).eventType("TIMEDEAL").eventId(100L)
                    .maxCapacity(50).entryBatchSize(10).entryIntervalSeconds(30).active(true).build();

            when(waitingQueueRepository.findByEventTypeAndEventIdAndIsActiveTrue("TIMEDEAL", 100L))
                    .thenReturn(Optional.of(queue));
            when(redisTemplate.execute(eq(queueProcessScript), anyList(), any(), any()))
                    .thenReturn(Collections.emptyList());

            // when
            queueService.processEntries("TIMEDEAL", 100L);

            // then
            verify(queueEntryRepository, never()).save(any(QueueEntry.class));
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
    @DisplayName("isQueueActive")
    class IsQueueActive {

        @Test
        @DisplayName("should_returnTrue_when_activeQueueExists")
        void should_returnTrue_when_activeQueueExists() {
            // given
            WaitingQueue queue = QueueFixture.queueBuilder()
                    .id(1L).eventType("COUPON").eventId(5L)
                    .maxCapacity(100).entryBatchSize(20).entryIntervalSeconds(10).active(true).build();
            when(waitingQueueRepository.findByEventTypeAndEventIdAndIsActiveTrue("COUPON", 5L))
                    .thenReturn(Optional.of(queue));

            // when
            boolean result = queueService.isQueueActive("COUPON", 5L);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should_returnFalse_when_noActiveQueue")
        void should_returnFalse_when_noActiveQueue() {
            // given
            when(waitingQueueRepository.findByEventTypeAndEventIdAndIsActiveTrue("COUPON", 5L))
                    .thenReturn(Optional.empty());

            // when
            boolean result = queueService.isQueueActive("COUPON", 5L);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("validateEntry")
    class ValidateEntry {

        @Test
        @DisplayName("should_returnTrue_when_entered")
        void should_returnTrue_when_entered() {
            // given
            WaitingQueue queue = QueueFixture.queueBuilder()
                    .id(1L).eventType("TIMEDEAL").eventId(100L)
                    .maxCapacity(50).entryBatchSize(10).entryIntervalSeconds(30).active(true).build();
            QueueEntry entry = QueueFixture.entryBuilder()
                    .id(1L).queue(queue).userId("user1").status(QueueStatus.ENTERED).build();

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
            WaitingQueue queue = QueueFixture.queueBuilder()
                    .id(1L).eventType("TIMEDEAL").eventId(100L)
                    .maxCapacity(50).entryBatchSize(10).entryIntervalSeconds(30).active(true).build();

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
}
