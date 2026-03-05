package com.portal.universe.shoppingservice.queue.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import com.portal.universe.shoppingservice.queue.domain.QueueEntry;
import com.portal.universe.shoppingservice.queue.domain.QueueStatus;
import com.portal.universe.shoppingservice.queue.domain.WaitingQueue;
import com.portal.universe.shoppingservice.queue.dto.QueueStatusResponse;
import com.portal.universe.shoppingservice.queue.repository.QueueEntryRepository;
import com.portal.universe.shoppingservice.queue.repository.WaitingQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * QueueServiceImpl
 * Redis Sorted Set 기반 대기열 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueueServiceImpl implements QueueService {

    private final WaitingQueueRepository waitingQueueRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List> queueProcessScript;

    private static final String QUEUE_KEY_PREFIX = "queue:waiting:";
    private static final String ENTERED_KEY_PREFIX = "queue:entered:";

    private String getQueueKey(String eventType, Long eventId) {
        return QUEUE_KEY_PREFIX + eventType + ":" + eventId;
    }

    private String getEnteredKey(String eventType, Long eventId) {
        return ENTERED_KEY_PREFIX + eventType + ":" + eventId;
    }

    @Override
    @Transactional
    public QueueStatusResponse enterQueue(String eventType, Long eventId, String userId) {
        // 1. 활성 대기열 확인
        WaitingQueue queue = waitingQueueRepository.findByEventTypeAndEventIdAndIsActiveTrue(eventType, eventId)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.QUEUE_NOT_FOUND));

        // 2. 이미 대기 중인지 확인
        Optional<QueueEntry> existingEntry = queueEntryRepository.findByQueueAndUserId(queue, userId);
        if (existingEntry.isPresent()) {
            QueueEntry entry = existingEntry.get();
            if (entry.isWaiting() || entry.isEntered()) {
                return getQueueStatusInternal(queue, entry);
            }
        }

        // 3. 새 대기열 엔트리 생성
        QueueEntry entry = QueueEntry.builder()
            .queue(queue)
            .userId(userId)
            .build();
        queueEntryRepository.save(entry);

        // 4. Redis Sorted Set에 추가 (score = timestamp)
        String queueKey = getQueueKey(eventType, eventId);
        double score = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(queueKey, entry.getEntryToken(), score);

        log.info("User {} entered queue for {} {}", userId, eventType, eventId);

        // 5. 상태 반환
        return getQueueStatusInternal(queue, entry);
    }

    @Override
    @Transactional(readOnly = true)
    public QueueStatusResponse getQueueStatus(String eventType, Long eventId, String userId) {
        WaitingQueue queue = waitingQueueRepository.findByEventTypeAndEventIdAndIsActiveTrue(eventType, eventId)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.QUEUE_NOT_FOUND));

        QueueEntry entry = queueEntryRepository.findByQueueAndUserId(queue, userId)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.QUEUE_ENTRY_NOT_FOUND));

        return getQueueStatusInternal(queue, entry);
    }

    @Override
    @Transactional(readOnly = true)
    public QueueStatusResponse getQueueStatusByToken(String entryToken) {
        QueueEntry entry = queueEntryRepository.findByEntryToken(entryToken)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.QUEUE_ENTRY_NOT_FOUND));

        return getQueueStatusInternal(entry.getQueue(), entry);
    }

    private QueueStatusResponse getQueueStatusInternal(WaitingQueue queue, QueueEntry entry) {
        if (entry.isEntered()) {
            return QueueStatusResponse.entered(entry.getEntryToken());
        }

        if (entry.getStatus() == QueueStatus.EXPIRED) {
            return QueueStatusResponse.expired(entry.getEntryToken());
        }

        if (entry.getStatus() == QueueStatus.LEFT) {
            return QueueStatusResponse.left(entry.getEntryToken());
        }

        // 대기 중인 경우 순번 계산
        String queueKey = getQueueKey(queue.getEventType(), queue.getEventId());
        Long position = redisTemplate.opsForZSet().rank(queueKey, entry.getEntryToken());

        if (position == null) {
            // Redis에 없으면 DB에서 계산
            position = queueEntryRepository.countWaitingBefore(queue, entry.getJoinedAt());
        }

        Long totalWaiting = redisTemplate.opsForZSet().zCard(queueKey);
        if (totalWaiting == null) {
            totalWaiting = queueEntryRepository.countByQueueAndStatus(queue, QueueStatus.WAITING);
        }

        // 예상 대기 시간 계산: (position / batchSize) * intervalSeconds
        long estimatedWaitSeconds = ((position + 1) / queue.getEntryBatchSize()) * queue.getEntryIntervalSeconds();

        return QueueStatusResponse.waiting(entry.getEntryToken(), position + 1, estimatedWaitSeconds, totalWaiting);
    }

    @Override
    @Transactional
    public void leaveQueue(String eventType, Long eventId, String userId) {
        WaitingQueue queue = waitingQueueRepository.findByEventTypeAndEventId(eventType, eventId)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.QUEUE_NOT_FOUND));

        QueueEntry entry = queueEntryRepository.findByQueueAndUserId(queue, userId)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.QUEUE_ENTRY_NOT_FOUND));

        leaveQueueInternal(entry, eventType, eventId);
    }

    @Override
    @Transactional
    public void leaveQueueByToken(String entryToken) {
        QueueEntry entry = queueEntryRepository.findByEntryToken(entryToken)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.QUEUE_ENTRY_NOT_FOUND));

        WaitingQueue queue = entry.getQueue();
        leaveQueueInternal(entry, queue.getEventType(), queue.getEventId());
    }

    private void leaveQueueInternal(QueueEntry entry, String eventType, Long eventId) {
        entry.leave();
        queueEntryRepository.save(entry);

        // Redis에서 제거
        String queueKey = getQueueKey(eventType, eventId);
        redisTemplate.opsForZSet().remove(queueKey, entry.getEntryToken());

        log.info("User {} left queue for {} {}", entry.getUserId(), eventType, eventId);
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public void processEntries(String eventType, Long eventId) {
        Optional<WaitingQueue> queueOpt = waitingQueueRepository.findByEventTypeAndEventIdAndIsActiveTrue(eventType, eventId);
        if (queueOpt.isEmpty()) {
            return;
        }

        WaitingQueue queue = queueOpt.get();
        String enteredKey = getEnteredKey(eventType, eventId);
        String queueKey = getQueueKey(eventType, eventId);

        // Lua Script로 CHECK+POP+ADD 원자적 실행 (TOCTOU 방지)
        List<String> tokens = (List<String>) redisTemplate.execute(
                queueProcessScript,
                Arrays.asList(enteredKey, queueKey),
                String.valueOf(queue.getMaxCapacity()),
                String.valueOf(queue.getEntryBatchSize())
        );

        if (tokens == null || tokens.isEmpty()) {
            return;
        }

        // DB 상태 업데이트 (Redis 원자 연산 이후)
        for (String entryToken : tokens) {
            queueEntryRepository.findByEntryToken(entryToken).ifPresent(entry -> {
                entry.enter();
                queueEntryRepository.save(entry);
                log.info("User {} entered from queue for {} {}", entry.getUserId(), eventType, eventId);
            });
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateEntry(String eventType, Long eventId, String userId) {
        Optional<WaitingQueue> queueOpt = waitingQueueRepository.findByEventTypeAndEventIdAndIsActiveTrue(eventType, eventId);
        if (queueOpt.isEmpty()) {
            // 대기열이 없으면 바로 통과
            return true;
        }

        WaitingQueue queue = queueOpt.get();
        Optional<QueueEntry> entryOpt = queueEntryRepository.findByQueueAndUserIdAndStatus(queue, userId, QueueStatus.ENTERED);

        return entryOpt.isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public void validateTokenOwnership(String entryToken, String userId) {
        QueueEntry entry = queueEntryRepository.findByEntryToken(entryToken)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.QUEUE_ENTRY_NOT_FOUND));
        if (!entry.getUserId().equals(userId)) {
            throw new CustomBusinessException(ShoppingErrorCode.QUEUE_TOKEN_USER_MISMATCH);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isQueueActive(String eventType, Long eventId) {
        return waitingQueueRepository.findByEventTypeAndEventIdAndIsActiveTrue(eventType, eventId).isPresent();
    }

}
