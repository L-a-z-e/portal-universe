package com.portal.universe.shoppingservice.queue.service;

import com.portal.universe.shoppingservice.common.annotation.DistributedLock;
import com.portal.universe.shoppingservice.queue.domain.WaitingQueue;
import com.portal.universe.shoppingservice.queue.repository.WaitingQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * QueueScheduler
 * 대기열 입장 처리 스케줄러
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QueueScheduler {

    private final WaitingQueueRepository waitingQueueRepository;
    private final QueueService queueService;

    /**
     * 활성 대기열에서 대기자를 입장 처리
     * 5초마다 실행, 멀티 Pod 환경에서 동시 실행 방지
     */
    @Scheduled(fixedDelay = 5000)
    @DistributedLock(key = "'scheduler:queue:process'", waitTime = 0, leaseTime = 4)
    public void processActiveQueues() {
        List<WaitingQueue> activeQueues = waitingQueueRepository.findByIsActiveTrue();

        for (WaitingQueue queue : activeQueues) {
            try {
                queueService.processEntries(queue.getEventType(), queue.getEventId());
            } catch (Exception e) {
                log.error("Failed to process queue entries for {} {}: {}",
                    queue.getEventType(), queue.getEventId(), e.getMessage());
            }
        }
    }
}
