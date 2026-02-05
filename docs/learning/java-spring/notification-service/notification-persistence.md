# Notification Persistence

## 개요

알림 영속성(Persistence)은 알림 데이터를 데이터베이스에 저장하고 조회하는 계층입니다. Notification Service는 JPA/Hibernate를 사용하여 MySQL에 알림을 저장합니다.

## Repository 인터페이스

### NotificationRepository

```java
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 사용자별 알림 목록 (최신순)
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 사용자별 특정 상태 알림 (최신순)
    Page<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId,
            NotificationStatus status,
            Pageable pageable
    );

    // 사용자별 특정 상태 알림 개수
    long countByUserIdAndStatus(Long userId, NotificationStatus status);

    // ID와 사용자 ID로 알림 조회
    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    // 사용자의 모든 읽지 않은 알림을 읽음으로 변경
    @Modifying
    @Query("UPDATE Notification n SET n.status = :status, n.readAt = :readAt " +
           "WHERE n.userId = :userId AND n.status = 'UNREAD'")
    int markAllAsRead(
            @Param("userId") Long userId,
            @Param("status") NotificationStatus status,
            @Param("readAt") LocalDateTime readAt
    );

    // 알림 삭제
    void deleteByUserIdAndId(Long userId, Long id);
}
```

## Service 계층

### NotificationService 인터페이스

```java
public interface NotificationService {

    Notification create(Long userId, NotificationType type, String title, String message,
                        String link, String referenceId, String referenceType);

    Page<NotificationResponse> getNotifications(Long userId, Pageable pageable);

    Page<NotificationResponse> getUnreadNotifications(Long userId, Pageable pageable);

    long getUnreadCount(Long userId);

    NotificationResponse markAsRead(Long notificationId, Long userId);

    int markAllAsRead(Long userId);

    void delete(Long notificationId, Long userId);
}
```

### NotificationServiceImpl

```java
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public Notification create(Long userId, NotificationType type, String title, String message,
                               String link, String referenceId, String referenceType) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .link(link)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification created: userId={}, type={}, id={}", userId, type, saved.getId());
        return saved;
    }

    @Override
    public Page<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::from);
    }

    @Override
    public Page<NotificationResponse> getUnreadNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                userId, NotificationStatus.UNREAD, pageable)
                .map(NotificationResponse::from);
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.UNREAD);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

        notification.markAsRead();
        return NotificationResponse.from(notification);
    }

    @Override
    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsRead(userId, NotificationStatus.READ, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void delete(Long notificationId, Long userId) {
        notificationRepository.deleteByUserIdAndId(userId, notificationId);
    }
}
```

## 데이터 흐름

### 알림 생성 흐름

```
┌─────────────────────────────────────────────────────────────────────┐
│                   Notification Creation Flow                         │
│                                                                      │
│  Kafka Event                                                         │
│      │                                                               │
│      ▼                                                               │
│  NotificationConsumer.handleOrderCreated()                           │
│      │                                                               │
│      ▼                                                               │
│  NotificationService.create()                                        │
│      │                                                               │
│      ├──▶ Notification.builder()...build()                          │
│      │                                                               │
│      ▼                                                               │
│  NotificationRepository.save()                                       │
│      │                                                               │
│      ├──▶ INSERT INTO notifications (...)                           │
│      │                                                               │
│      ▼                                                               │
│  NotificationPushService.push()                                      │
│      │                                                               │
│      ├──▶ WebSocket 전송                                            │
│      └──▶ Redis Pub/Sub 발행                                        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 알림 조회 흐름

```
┌─────────────────────────────────────────────────────────────────────┐
│                   Notification Query Flow                            │
│                                                                      │
│  GET /api/notifications?page=0&size=20                              │
│      │                                                               │
│      ▼                                                               │
│  NotificationController.getNotifications()                           │
│      │                                                               │
│      ▼                                                               │
│  NotificationService.getNotifications(userId, pageable)              │
│      │                                                               │
│      ▼                                                               │
│  NotificationRepository.findByUserIdOrderByCreatedAtDesc()           │
│      │                                                               │
│      ├──▶ SELECT * FROM notifications                               │
│      │    WHERE user_id = ?                                         │
│      │    ORDER BY created_at DESC                                  │
│      │    LIMIT 20 OFFSET 0                                         │
│      │                                                               │
│      ▼                                                               │
│  Page<Notification> → Page<NotificationResponse>                     │
│      │                                                               │
│      ▼                                                               │
│  ApiResponse.success(page)                                           │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

## 페이지네이션

### Spring Data Pageable

```java
// Controller
@GetMapping
public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
        @RequestHeader("X-User-Id") Long userId,
        @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(ApiResponse.success(
            notificationService.getNotifications(userId, pageable)));
}
```

### 페이지네이션 응답 구조

```json
{
    "success": true,
    "data": {
        "content": [
            {
                "id": 100,
                "type": "ORDER_CREATED",
                "title": "주문 완료",
                "message": "주문이 접수되었습니다.",
                "status": "UNREAD",
                "createdAt": "2024-01-15T10:30:00"
            }
        ],
        "pageable": {
            "sort": { "sorted": true, "unsorted": false },
            "pageNumber": 0,
            "pageSize": 20
        },
        "totalElements": 45,
        "totalPages": 3,
        "last": false,
        "first": true
    },
    "message": null
}
```

## 벌크 연산

### markAllAsRead

```java
@Modifying
@Query("UPDATE Notification n SET n.status = :status, n.readAt = :readAt " +
       "WHERE n.userId = :userId AND n.status = 'UNREAD'")
int markAllAsRead(
        @Param("userId") Long userId,
        @Param("status") NotificationStatus status,
        @Param("readAt") LocalDateTime readAt
);
```

**특징:**
- 단일 쿼리로 다수 레코드 업데이트
- 영속성 컨텍스트 우회 (성능 향상)
- 변경된 레코드 수 반환

**주의사항:**
- 1차 캐시와 불일치 발생 가능
- `@Modifying(clearAutomatically = true)` 고려

## 인덱스 활용

### 정의된 인덱스

```sql
CREATE INDEX idx_notification_user_status
ON notifications (user_id, status);

CREATE INDEX idx_notification_user_created
ON notifications (user_id, created_at DESC);
```

### 쿼리별 인덱스 매칭

| 쿼리 | 사용 인덱스 |
|------|-----------|
| findByUserIdOrderByCreatedAtDesc | idx_notification_user_created |
| findByUserIdAndStatusOrderByCreatedAtDesc | idx_notification_user_status + created_at |
| countByUserIdAndStatus | idx_notification_user_status |
| findByIdAndUserId | PRIMARY + user_id |

## 트랜잭션 관리

### 읽기 전용 트랜잭션

```java
@Service
@Transactional(readOnly = true)  // 기본값: 읽기 전용
public class NotificationServiceImpl {

    // 조회 메서드 - 읽기 전용 트랜잭션 사용
    public Page<NotificationResponse> getNotifications(...) { }

    // 쓰기 메서드 - 별도 트랜잭션
    @Transactional  // readOnly = false
    public Notification create(...) { }

    @Transactional
    public NotificationResponse markAsRead(...) { }
}
```

### Dirty Checking

```java
@Transactional
public NotificationResponse markAsRead(Long notificationId, Long userId) {
    Notification notification = notificationRepository.findByIdAndUserId(...)
            .orElseThrow(() -> new IllegalArgumentException("Not found"));

    notification.markAsRead();  // 상태 변경

    // save() 호출 없이도 트랜잭션 종료 시 자동 UPDATE
    return NotificationResponse.from(notification);
}
```

## 데이터 정리

### 오래된 알림 삭제

```java
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 특정 날짜 이전의 읽은 알림 삭제
    @Modifying
    @Query("DELETE FROM Notification n " +
           "WHERE n.status = 'READ' AND n.createdAt < :cutoffDate")
    int deleteOldReadNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);
}

@Service
@RequiredArgsConstructor
public class NotificationCleanupService {

    private final NotificationRepository repository;

    @Scheduled(cron = "0 0 3 * * ?")  // 매일 새벽 3시
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        int deleted = repository.deleteOldReadNotifications(cutoffDate);
        log.info("Deleted {} old notifications", deleted);
    }
}
```

## 성능 최적화

### 배치 인서트 설정

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
```

### N+1 문제 방지

```java
// 단순 조회 - N+1 문제 없음 (연관 엔티티 없음)
@Query("SELECT n FROM Notification n WHERE n.userId = :userId")
Page<Notification> findByUserId(@Param("userId") Long userId, Pageable pageable);
```

## 감사 로깅

### 생성/수정 시간 자동 관리

```java
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

// 활성화
@Configuration
@EnableJpaAuditing
public class JpaConfig { }
```

## Best Practices

1. **읽기 전용 트랜잭션** - 조회에는 `@Transactional(readOnly = true)`
2. **벌크 연산** - 대량 업데이트/삭제는 JPQL 사용
3. **인덱스 설계** - 조회 패턴에 맞는 인덱스
4. **페이지네이션** - 전체 조회 대신 페이징 사용
5. **정기 정리** - 오래된 데이터 삭제
6. **배치 설정** - 대량 삽입 성능 향상

## 관련 문서

- [notification-domain.md](./notification-domain.md) - 알림 도메인 모델
- [read-status-tracking.md](./read-status-tracking.md) - 읽음 상태 추적
