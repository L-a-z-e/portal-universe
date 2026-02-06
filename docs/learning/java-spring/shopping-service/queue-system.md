# Queue System (대기열 시스템)

## 개요

대용량 트래픽 상황(타임딜, Flash Sale 등)에서 시스템 부하를 분산하기 위한
대기열 시스템 구현을 설명합니다.
Redis Sorted Set을 활용하여 FIFO 방식의 공정한 대기열을 구현합니다.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Queue System                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐   │
│  │   QueueService  │────▶│  Redis Sorted   │────▶│  QueueScheduler │   │
│  │                 │     │      Set        │     │   (5초 주기)     │   │
│  └─────────────────┘     └─────────────────┘     └─────────────────┘   │
│           │                      │                       │             │
│           ▼                      ▼                       ▼             │
│  ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐   │
│  │  WaitingQueue   │     │ queue:waiting:  │     │   processEntries│   │
│  │   (DB 설정)     │     │  {type}:{id}    │     │   (배치 입장)   │   │
│  └─────────────────┘     └─────────────────┘     └─────────────────┘   │
│           │                      │                                     │
│           ▼                      ▼                                     │
│  ┌─────────────────┐     ┌─────────────────┐                          │
│  │   QueueEntry    │     │ queue:entered:  │                          │
│  │   (DB 기록)     │     │  {type}:{id}    │                          │
│  └─────────────────┘     └─────────────────┘                          │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

## Redis Key 구조

| Key Pattern | Type | 용도 |
|-------------|------|------|
| `queue:waiting:{eventType}:{eventId}` | Sorted Set | 대기 중인 사용자 (score = timestamp) |
| `queue:entered:{eventType}:{eventId}` | Set | 입장 완료된 사용자 |

## Domain Model

### WaitingQueue Entity (대기열 설정)

```java
@Entity
@Table(name = "waiting_queues")
public class WaitingQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String eventType;           // TIMEDEAL, FLASH_SALE, etc.

    @Column(nullable = false)
    private Long eventId;               // TimeDeal ID or other event ID

    @Column(nullable = false)
    private Integer maxCapacity;        // 동시 입장 가능 인원

    @Column(nullable = false)
    private Integer entryBatchSize;     // 한 번에 입장시킬 인원

    @Column(nullable = false)
    private Integer entryIntervalSeconds;  // 입장 간격 (초)

    @Column(nullable = false)
    private Boolean isActive = false;

    private LocalDateTime activatedAt;
    private LocalDateTime deactivatedAt;

    public void activate() {
        this.isActive = true;
        this.activatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.isActive = false;
        this.deactivatedAt = LocalDateTime.now();
    }
}
```

### QueueEntry Entity (대기열 엔트리)

```java
@Entity
@Table(name = "queue_entries")
public class QueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "queue_id", nullable = false)
    private WaitingQueue queue;

    @Column(nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, unique = true, length = 36)
    private String entryToken;          // 고유 토큰 (UUID)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QueueStatus status = QueueStatus.WAITING;

    private LocalDateTime joinedAt;     // 대기열 진입 시간
    private LocalDateTime enteredAt;    // 입장 시간
    private LocalDateTime expiredAt;    // 만료 시간
    private LocalDateTime leftAt;       // 이탈 시간

    @Builder
    public QueueEntry(WaitingQueue queue, String userId) {
        this.queue = queue;
        this.userId = userId;
        this.entryToken = UUID.randomUUID().toString();
        this.status = QueueStatus.WAITING;
        this.joinedAt = LocalDateTime.now();
    }

    public void enter() {
        this.status = QueueStatus.ENTERED;
        this.enteredAt = LocalDateTime.now();
    }

    public void expire() {
        this.status = QueueStatus.EXPIRED;
        this.expiredAt = LocalDateTime.now();
    }

    public void leave() {
        this.status = QueueStatus.LEFT;
        this.leftAt = LocalDateTime.now();
    }
}
```

### QueueStatus Enum

```java
public enum QueueStatus {
    WAITING,    // 대기 중
    ENTERED,    // 입장 완료
    EXPIRED,    // 만료됨
    LEFT        // 이탈함
}
```

## QueueService 구현

### 대기열 진입

```java
@Service
@RequiredArgsConstructor
public class QueueServiceImpl implements QueueService {

    private static final String QUEUE_KEY_PREFIX = "queue:waiting:";
    private static final String ENTERED_KEY_PREFIX = "queue:entered:";

    private final WaitingQueueRepository waitingQueueRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public QueueStatusResponse enterQueue(String eventType, Long eventId, String userId) {
        // 1. 활성 대기열 확인
        WaitingQueue queue = waitingQueueRepository
            .findByEventTypeAndEventIdAndIsActiveTrue(eventType, eventId)
            .orElseThrow(() -> new CustomBusinessException(QUEUE_NOT_FOUND));

        // 2. 이미 대기 중인지 확인
        Optional<QueueEntry> existingEntry = queueEntryRepository
            .findByQueueAndUserId(queue, userId);

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

        return getQueueStatusInternal(queue, entry);
    }
}
```

### 대기열 상태 조회

```java
private QueueStatusResponse getQueueStatusInternal(WaitingQueue queue, QueueEntry entry) {
    // 이미 입장한 경우
    if (entry.isEntered()) {
        return QueueStatusResponse.entered(entry.getEntryToken());
    }

    // 만료된 경우
    if (entry.getStatus() == QueueStatus.EXPIRED) {
        return QueueStatusResponse.expired(entry.getEntryToken());
    }

    // 이탈한 경우
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

    // 예상 대기 시간 계산
    // (position / batchSize) * intervalSeconds
    long estimatedWaitSeconds = ((position + 1) / queue.getEntryBatchSize())
                               * queue.getEntryIntervalSeconds();

    return QueueStatusResponse.waiting(
        entry.getEntryToken(),
        position + 1,           // 1-based 순번
        estimatedWaitSeconds,
        totalWaiting
    );
}
```

### 대기열 이탈

```java
@Override
@Transactional
public void leaveQueue(String eventType, Long eventId, String userId) {
    WaitingQueue queue = waitingQueueRepository
        .findByEventTypeAndEventId(eventType, eventId)
        .orElseThrow(() -> new CustomBusinessException(QUEUE_NOT_FOUND));

    QueueEntry entry = queueEntryRepository.findByQueueAndUserId(queue, userId)
        .orElseThrow(() -> new CustomBusinessException(QUEUE_ENTRY_NOT_FOUND));

    entry.leave();
    queueEntryRepository.save(entry);

    // Redis에서 제거
    String queueKey = getQueueKey(eventType, eventId);
    redisTemplate.opsForZSet().remove(queueKey, entry.getEntryToken());

    log.info("User {} left queue for {} {}", userId, eventType, eventId);
}
```

### 배치 입장 처리

```java
@Override
@Transactional
public void processEntries(String eventType, Long eventId) {
    Optional<WaitingQueue> queueOpt = waitingQueueRepository
        .findByEventTypeAndEventIdAndIsActiveTrue(eventType, eventId);

    if (queueOpt.isEmpty()) {
        return;
    }

    WaitingQueue queue = queueOpt.get();
    String queueKey = getQueueKey(eventType, eventId);
    String enteredKey = getEnteredKey(eventType, eventId);

    // 1. 현재 입장한 인원 확인
    Long enteredCount = redisTemplate.opsForSet().size(enteredKey);
    if (enteredCount == null) enteredCount = 0L;

    // 2. 입장 가능 슬롯 계산
    int availableSlots = queue.getMaxCapacity() - enteredCount.intValue();
    if (availableSlots <= 0) {
        log.debug("Queue full for {} {}", eventType, eventId);
        return;
    }

    int toProcess = Math.min(availableSlots, queue.getEntryBatchSize());

    // 3. Redis에서 대기열 상위 N명 가져오기 (ZPOPMIN)
    Set<ZSetOperations.TypedTuple<String>> topEntries =
        redisTemplate.opsForZSet().popMin(queueKey, toProcess);

    if (topEntries == null || topEntries.isEmpty()) {
        return;
    }

    // 4. 입장 처리
    for (ZSetOperations.TypedTuple<String> tuple : topEntries) {
        String entryToken = tuple.getValue();
        if (entryToken == null) continue;

        queueEntryRepository.findByEntryToken(entryToken).ifPresent(entry -> {
            entry.enter();
            queueEntryRepository.save(entry);

            // 입장 목록에 추가
            redisTemplate.opsForSet().add(enteredKey, entryToken);

            log.info("User {} entered from queue for {} {}",
                    entry.getUserId(), eventType, eventId);
        });
    }
}
```

## QueueScheduler

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class QueueScheduler {

    private final WaitingQueueRepository waitingQueueRepository;
    private final QueueService queueService;

    /**
     * 활성 대기열에서 대기자를 입장 처리
     * 5초마다 실행
     */
    @Scheduled(fixedDelay = 5000)
    public void processActiveQueues() {
        List<WaitingQueue> activeQueues = waitingQueueRepository.findAll().stream()
            .filter(WaitingQueue::getIsActive)
            .toList();

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
```

## 대기열 활성화 (관리자)

```java
@Override
@Transactional
public void activateQueue(String eventType, Long eventId,
                          Integer maxCapacity, Integer entryBatchSize,
                          Integer entryIntervalSeconds) {
    WaitingQueue queue = waitingQueueRepository
        .findByEventTypeAndEventId(eventType, eventId)
        .orElseGet(() -> WaitingQueue.builder()
            .eventType(eventType)
            .eventId(eventId)
            .maxCapacity(maxCapacity)
            .entryBatchSize(entryBatchSize)
            .entryIntervalSeconds(entryIntervalSeconds)
            .build());

    queue.activate();
    waitingQueueRepository.save(queue);

    log.info("Queue activated for {} {}", eventType, eventId);
}
```

## QueueStatusResponse

```java
public record QueueStatusResponse(
    String entryToken,
    QueueStatus status,
    Long position,              // 대기 순번 (1-based)
    Long estimatedWaitSeconds,  // 예상 대기 시간 (초)
    Long totalWaiting           // 총 대기 인원
) {
    public static QueueStatusResponse waiting(String token, Long position,
                                              Long waitSeconds, Long total) {
        return new QueueStatusResponse(token, QueueStatus.WAITING,
                                       position, waitSeconds, total);
    }

    public static QueueStatusResponse entered(String token) {
        return new QueueStatusResponse(token, QueueStatus.ENTERED, null, null, null);
    }

    public static QueueStatusResponse expired(String token) {
        return new QueueStatusResponse(token, QueueStatus.EXPIRED, null, null, null);
    }

    public static QueueStatusResponse left(String token) {
        return new QueueStatusResponse(token, QueueStatus.LEFT, null, null, null);
    }
}
```

## 대기열 검증

타임딜 구매 등 실제 서비스 이용 전 입장 여부 확인:

```java
@Override
@Transactional(readOnly = true)
public boolean validateEntry(String eventType, Long eventId, String userId) {
    Optional<WaitingQueue> queueOpt = waitingQueueRepository
        .findByEventTypeAndEventIdAndIsActiveTrue(eventType, eventId);

    if (queueOpt.isEmpty()) {
        // 대기열이 없으면 바로 통과
        return true;
    }

    WaitingQueue queue = queueOpt.get();
    Optional<QueueEntry> entryOpt = queueEntryRepository
        .findByQueueAndUserIdAndStatus(queue, userId, QueueStatus.ENTERED);

    return entryOpt.isPresent();
}
```

## 타임딜 연동 예시

```java
@Transactional
public TimeDealPurchaseResponse purchaseTimeDeal(String userId, TimeDealPurchaseRequest request) {
    TimeDealProduct product = timeDealProductRepository
        .findByIdWithProductAndDeal(request.timeDealProductId())
        .orElseThrow();

    TimeDeal timeDeal = product.getTimeDeal();

    // 1. 대기열 입장 확인
    if (!queueService.validateEntry("TIMEDEAL", timeDeal.getId(), userId)) {
        throw new CustomBusinessException(QUEUE_ENTRY_REQUIRED);
    }

    // 2. 구매 진행
    // ...
}
```

## 설정 예시

```yaml
# 타임딜 대기열 설정
event_type: TIMEDEAL
event_id: 1
max_capacity: 100       # 동시 100명까지 입장
entry_batch_size: 10    # 한 번에 10명씩 입장
entry_interval_seconds: 5  # 5초 간격으로 처리
```

예상 대기 시간 = (순번 / 10) * 5초
- 50번째: 약 25초
- 100번째: 약 50초

## 관련 파일

- `/queue/service/QueueServiceImpl.java` - 대기열 서비스
- `/queue/service/QueueScheduler.java` - 배치 처리 스케줄러
- `/queue/domain/WaitingQueue.java` - 대기열 설정 엔티티
- `/queue/domain/QueueEntry.java` - 대기열 엔트리 엔티티
- `/queue/domain/QueueStatus.java` - 상태 enum
- `/queue/dto/QueueStatusResponse.java` - 응답 DTO
