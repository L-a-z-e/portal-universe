package com.portal.universe.shoppingservice.queue.service;

import com.portal.universe.shoppingservice.queue.domain.WaitingQueue;
import com.portal.universe.shoppingservice.queue.repository.WaitingQueueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueSchedulerTest {

    @Mock
    private WaitingQueueRepository waitingQueueRepository;

    @Mock
    private QueueService queueService;

    @InjectMocks
    private QueueScheduler queueScheduler;

    private WaitingQueue createActiveQueue(String eventType, Long eventId) {
        WaitingQueue queue = WaitingQueue.builder()
                .eventType(eventType)
                .eventId(eventId)
                .maxCapacity(100)
                .entryBatchSize(10)
                .entryIntervalSeconds(5)
                .build();
        queue.activate();
        return queue;
    }

    @Test
    @DisplayName("should process all active queues")
    void should_process_active_queues() {
        WaitingQueue queue1 = createActiveQueue("TIMEDEAL", 1L);
        WaitingQueue queue2 = createActiveQueue("FLASH_SALE", 2L);
        when(waitingQueueRepository.findByIsActiveTrue()).thenReturn(List.of(queue1, queue2));

        queueScheduler.processActiveQueues();

        verify(queueService).processEntries("TIMEDEAL", 1L);
        verify(queueService).processEntries("FLASH_SALE", 2L);
        verify(queueService, times(2)).processEntries(anyString(), anyLong());
    }

    @Test
    @DisplayName("should continue processing other queues when one fails with exception")
    void should_continue_on_exception() {
        WaitingQueue queue1 = createActiveQueue("TIMEDEAL", 1L);
        WaitingQueue queue2 = createActiveQueue("FLASH_SALE", 2L);
        when(waitingQueueRepository.findByIsActiveTrue()).thenReturn(List.of(queue1, queue2));
        doThrow(new RuntimeException("처리 실패")).when(queueService).processEntries("TIMEDEAL", 1L);

        queueScheduler.processActiveQueues();

        verify(queueService).processEntries("TIMEDEAL", 1L);
        verify(queueService).processEntries("FLASH_SALE", 2L);
    }

    @Test
    @DisplayName("should do nothing when no active queues exist")
    void should_do_nothing_when_empty() {
        when(waitingQueueRepository.findByIsActiveTrue()).thenReturn(Collections.emptyList());

        queueScheduler.processActiveQueues();

        verify(queueService, never()).processEntries(anyString(), anyLong());
    }
}
