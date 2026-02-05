# Read Status Tracking

## 개요

읽음 상태 추적은 사용자가 어떤 알림을 확인했는지 관리하는 기능입니다. Notification Service는 개별 알림 읽음과 전체 읽음 처리를 지원하며, 미읽은 알림 개수를 효율적으로 조회합니다.

## 상태 모델

### NotificationStatus

```java
public enum NotificationStatus {
    UNREAD,  // 읽지 않음 (기본값)
    READ     // 읽음
}
```

### 상태 전이

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Read Status State Machine                         │
│                                                                      │
│                                                                      │
│    [생성]                                                            │
│       │                                                              │
│       ▼                                                              │
│  ┌─────────────┐                                                     │
│  │   UNREAD    │ ← 알림 생성 시 기본 상태                            │
│  │             │                                                     │
│  │ readAt: null│                                                     │
│  └──────┬──────┘                                                     │
│         │                                                            │
│         │ markAsRead()                                               │
│         │                                                            │
│         ▼                                                            │
│  ┌─────────────┐                                                     │
│  │    READ     │ ← 되돌릴 수 없음 (단방향)                           │
│  │             │                                                     │
│  │ readAt: 시간│                                                     │
│  └─────────────┘                                                     │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

## API 엔드포인트

### 읽음 상태 관련 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/notifications/unread` | 읽지 않은 알림 목록 |
| GET | `/api/notifications/unread/count` | 읽지 않은 알림 개수 |
| PUT | `/api/notifications/{id}/read` | 개별 알림 읽음 처리 |
| PUT | `/api/notifications/read-all` | 전체 알림 읽음 처리 |

### Controller 구현

```java
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // 읽지 않은 알림 목록
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getUnreadNotifications(
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getUnreadNotifications(userId, pageable)));
    }

    // 읽지 않은 알림 개수
    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getUnreadCount(userId)));
    }

    // 개별 읽음 처리
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.markAsRead(id, userId)));
    }

    // 전체 읽음 처리
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.markAllAsRead(userId)));
    }
}
```

## 개별 읽음 처리

### Domain Method

```java
@Entity
public class Notification {

    public void markAsRead() {
        // 멱등성 보장: 이미 읽은 알림은 변경하지 않음
        if (this.status == NotificationStatus.UNREAD) {
            this.status = NotificationStatus.READ;
            this.readAt = LocalDateTime.now();
        }
    }
}
```

### Service 구현

```java
@Service
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        // 사용자 ID 검증 포함 조회
        Notification notification = notificationRepository
                .findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notification not found: " + notificationId));

        // 도메인 메서드로 상태 변경
        notification.markAsRead();

        // Dirty Checking으로 자동 UPDATE
        return NotificationResponse.from(notification);
    }
}
```

### 실행되는 쿼리

```sql
-- 1. 알림 조회
SELECT * FROM notifications
WHERE id = ? AND user_id = ?;

-- 2. 상태 업데이트 (Dirty Checking)
UPDATE notifications
SET status = 'READ', read_at = '2024-01-15 10:30:00'
WHERE id = ?;
```

## 전체 읽음 처리

### Repository Method

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

### Service 구현

```java
@Override
@Transactional
public int markAllAsRead(Long userId) {
    return notificationRepository.markAllAsRead(
            userId,
            NotificationStatus.READ,
            LocalDateTime.now()
    );
}
```

### 실행되는 쿼리

```sql
UPDATE notifications
SET status = 'READ', read_at = '2024-01-15 10:30:00'
WHERE user_id = ? AND status = 'UNREAD';
```

## 미읽은 알림 개수

### 카운트 쿼리

```java
// Repository
long countByUserIdAndStatus(Long userId, NotificationStatus status);

// Service
@Override
public long getUnreadCount(Long userId) {
    return notificationRepository.countByUserIdAndStatus(
            userId,
            NotificationStatus.UNREAD
    );
}
```

### 인덱스 활용

```sql
-- idx_notification_user_status (user_id, status) 사용
SELECT COUNT(*) FROM notifications
WHERE user_id = ? AND status = 'UNREAD';
```

## 실시간 카운트 업데이트

### WebSocket으로 카운트 전송

```java
@Service
@RequiredArgsConstructor
public class NotificationCountService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    // 알림 생성 후 카운트 업데이트 전송
    public void sendCountUpdate(Long userId) {
        long count = notificationService.getUnreadCount(userId);

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications/count",
                Map.of("unreadCount", count)
        );
    }
}
```

### 클라이언트 처리

```javascript
// WebSocket 구독
client.subscribe('/user/queue/notifications/count', (message) => {
    const { unreadCount } = JSON.parse(message.body);
    updateBadgeCount(unreadCount);
});

// 배지 업데이트
function updateBadgeCount(count) {
    const badge = document.querySelector('.notification-badge');
    if (count > 0) {
        badge.textContent = count > 99 ? '99+' : count;
        badge.style.display = 'block';
    } else {
        badge.style.display = 'none';
    }
}
```

## 읽음 이벤트 흐름

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Read Event Flow                                   │
│                                                                      │
│  사용자 클릭                                                          │
│      │                                                               │
│      ▼                                                               │
│  PUT /api/notifications/{id}/read                                    │
│      │                                                               │
│      ▼                                                               │
│  NotificationController.markAsRead()                                 │
│      │                                                               │
│      ▼                                                               │
│  NotificationService.markAsRead()                                    │
│      │                                                               │
│      ├──▶ DB 업데이트 (status: READ, readAt: now)                   │
│      │                                                               │
│      ▼                                                               │
│  카운트 업데이트 전송 (선택적)                                        │
│      │                                                               │
│      ├──▶ WebSocket: /user/{userId}/queue/notifications/count       │
│      │                                                               │
│      ▼                                                               │
│  클라이언트 UI 업데이트                                               │
│      │                                                               │
│      ├──▶ 배지 카운트 감소                                           │
│      └──▶ 알림 항목 스타일 변경 (회색 처리 등)                        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

## 클라이언트 UI 패턴

### 읽음 상태 표시

```javascript
function renderNotification(notification) {
    const isUnread = notification.status === 'UNREAD';

    return `
        <div class="notification ${isUnread ? 'unread' : 'read'}"
             onclick="markAsRead(${notification.id})">
            <div class="notification-icon ${notification.type.toLowerCase()}">
                ${getIcon(notification.type)}
            </div>
            <div class="notification-content">
                <h4>${notification.title}</h4>
                <p>${notification.message}</p>
                <span class="time">${formatTime(notification.createdAt)}</span>
            </div>
            ${isUnread ? '<div class="unread-dot"></div>' : ''}
        </div>
    `;
}
```

### 스타일 예시

```css
.notification {
    padding: 12px;
    border-bottom: 1px solid #eee;
    cursor: pointer;
}

.notification.unread {
    background-color: #f0f7ff;
    font-weight: 500;
}

.notification.read {
    background-color: #fff;
    opacity: 0.8;
}

.unread-dot {
    width: 8px;
    height: 8px;
    background-color: #3b82f6;
    border-radius: 50%;
}
```

### 낙관적 업데이트

```javascript
async function markAsRead(notificationId) {
    // 1. UI 즉시 업데이트 (낙관적)
    updateNotificationUI(notificationId, 'READ');
    decrementBadgeCount();

    try {
        // 2. API 호출
        await api.put(`/api/notifications/${notificationId}/read`);
    } catch (error) {
        // 3. 실패 시 롤백
        updateNotificationUI(notificationId, 'UNREAD');
        incrementBadgeCount();
        showErrorToast('읽음 처리에 실패했습니다');
    }
}
```

## 캐싱 전략

### 읽지 않은 개수 캐싱

```java
@Service
@RequiredArgsConstructor
public class CachedNotificationCountService {

    private final NotificationRepository repository;
    private final RedisTemplate<String, Long> redisTemplate;

    private static final String COUNT_KEY_PREFIX = "notification:unread:count:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    public long getUnreadCount(Long userId) {
        String key = COUNT_KEY_PREFIX + userId;

        Long cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }

        long count = repository.countByUserIdAndStatus(userId, NotificationStatus.UNREAD);
        redisTemplate.opsForValue().set(key, count, CACHE_TTL);
        return count;
    }

    // 알림 생성/읽음 시 캐시 무효화
    public void invalidateCount(Long userId) {
        redisTemplate.delete(COUNT_KEY_PREFIX + userId);
    }
}
```

## 보안 고려사항

### 사용자 검증

```java
// 다른 사용자의 알림 접근 방지
Optional<Notification> findByIdAndUserId(Long id, Long userId);

// Service에서 검증
Notification notification = repository.findByIdAndUserId(notificationId, userId)
        .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
```

### 대량 요청 방지

```java
// 전체 읽음 처리에 Rate Limiting 적용 고려
@RateLimiter(name = "markAllAsRead")
@PutMapping("/read-all")
public ResponseEntity<ApiResponse<Integer>> markAllAsRead(...) { }
```

## Best Practices

1. **멱등성 보장** - 이미 읽은 알림 재처리 시 안전
2. **벌크 연산 사용** - 전체 읽음은 단일 쿼리로
3. **인덱스 활용** - 카운트 쿼리 최적화
4. **낙관적 업데이트** - UI 즉시 반영
5. **사용자 검증** - 권한 없는 접근 차단
6. **캐싱 고려** - 빈번한 카운트 조회 최적화

## 관련 문서

- [notification-domain.md](./notification-domain.md) - 알림 도메인 모델
- [notification-persistence.md](./notification-persistence.md) - 알림 저장
- [../websocket/websocket-architecture.md](../websocket/websocket-architecture.md) - WebSocket 아키텍처
