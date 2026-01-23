# Notification Domain Model

## 개요

알림 도메인 모델은 사용자에게 전달되는 알림의 구조와 생명주기를 정의합니다. Notification Service의 핵심 엔티티로서, 알림 생성부터 읽음 처리까지의 모든 상태를 관리합니다.

## 도메인 모델

### Notification Entity

```java
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_user_status", columnList = "user_id, status"),
    @Index(name = "idx_notification_user_created", columnList = "user_id, created_at DESC")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(length = 500)
    private String link;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.UNREAD;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public void markAsRead() {
        if (this.status == NotificationStatus.UNREAD) {
            this.status = NotificationStatus.READ;
            this.readAt = LocalDateTime.now();
        }
    }
}
```

### 필드 설명

| 필드 | 타입 | 설명 | 제약조건 |
|------|------|------|----------|
| `id` | Long | 알림 고유 식별자 | PK, Auto Increment |
| `userId` | Long | 수신 대상 사용자 ID | Not Null |
| `type` | NotificationType | 알림 유형 | Not Null |
| `title` | String | 알림 제목 | Not Null |
| `message` | String | 알림 내용 | Not Null, TEXT |
| `link` | String | 연결 URL | Max 500자 |
| `status` | NotificationStatus | 읽음 상태 | Default: UNREAD |
| `referenceId` | String | 참조 ID (주문번호 등) | Max 100자 |
| `referenceType` | String | 참조 타입 (ORDER 등) | Max 50자 |
| `createdAt` | LocalDateTime | 생성 시간 | Default: Now |
| `readAt` | LocalDateTime | 읽은 시간 | Nullable |

## 상태 모델

### NotificationStatus Enum

```java
public enum NotificationStatus {
    UNREAD,  // 읽지 않음
    READ     // 읽음
}
```

### 상태 전이 다이어그램

```
┌─────────────────────────────────────────────────────────────────────┐
│                  Notification State Transition                       │
│                                                                      │
│                                                                      │
│      ┌──────────────────────────────────────────────────────┐       │
│      │                                                      │       │
│      │    ┌────────────┐   markAsRead()   ┌────────────┐   │       │
│      │    │            │─────────────────▶│            │   │       │
│      │    │   UNREAD   │                  │    READ    │   │       │
│      │    │            │                  │            │   │       │
│      │    └────────────┘                  └────────────┘   │       │
│      │         │                               │           │       │
│      │         │                               │           │       │
│      │         └───────────────────────────────┘           │       │
│      │                       │                             │       │
│      │                       ▼                             │       │
│      │                  ┌────────────┐                     │       │
│      │                  │  DELETED   │ (논리적 삭제)        │       │
│      │                  └────────────┘                     │       │
│      │                                                      │       │
│      └──────────────────────────────────────────────────────┘       │
│                                                                      │
│  Notes:                                                             │
│  • UNREAD → READ: 단방향 전이 (되돌릴 수 없음)                       │
│  • 삭제는 물리적 삭제 (레코드 제거)                                   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

## 도메인 행위 (Behavior)

### markAsRead()

```java
public void markAsRead() {
    // 멱등성 보장: 이미 읽은 알림은 변경하지 않음
    if (this.status == NotificationStatus.UNREAD) {
        this.status = NotificationStatus.READ;
        this.readAt = LocalDateTime.now();
    }
}
```

**특징:**
- 멱등성(Idempotency) 보장
- 읽은 시간 자동 기록
- 상태 전이 불변성 (READ → UNREAD 불가)

## 참조 데이터

### Reference 패턴

알림이 참조하는 원본 데이터를 추적합니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│                      Reference Pattern                               │
│                                                                      │
│  Notification                                                        │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │ id: 1                                                        │    │
│  │ type: ORDER_CREATED                                          │    │
│  │ title: "주문이 접수되었습니다"                                 │    │
│  │ message: "주문번호 ORD-2024-001..."                          │    │
│  │ link: "/orders/ORD-2024-001"                                 │    │
│  │ referenceId: "ORD-2024-001"  ──────────────┐                 │    │
│  │ referenceType: "ORDER"                     │                 │    │
│  └────────────────────────────────────────────┼─────────────────┘    │
│                                               │                      │
│                                               ▼                      │
│                                    ┌─────────────────────┐           │
│                                    │ Order (Shopping)    │           │
│                                    │ id: ORD-2024-001    │           │
│                                    └─────────────────────┘           │
│                                                                      │
│  활용:                                                               │
│  • 중복 알림 방지 (같은 referenceId로 검색)                          │
│  • 원본 데이터로 네비게이션                                          │
│  • 알림 그룹화                                                       │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Reference Type 예시

| Type | 설명 | Reference ID 예시 |
|------|------|------------------|
| ORDER | 주문 | ORD-2024-001 |
| PAYMENT | 결제 | PAY-2024-001 |
| DELIVERY | 배송 | DLV-2024-001 |
| COUPON | 쿠폰 | CPN-2024-001 |
| TIMEDEAL | 타임딜 | TD-2024-001 |

## 인덱스 전략

### 정의된 인덱스

```java
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_user_status", columnList = "user_id, status"),
    @Index(name = "idx_notification_user_created", columnList = "user_id, created_at DESC")
})
```

### 인덱스 용도

| 인덱스 | 컬럼 | 최적화 쿼리 |
|--------|------|------------|
| `idx_notification_user_status` | user_id, status | 읽지 않은 알림 조회, 읽지 않은 알림 개수 |
| `idx_notification_user_created` | user_id, created_at DESC | 최신 알림 목록 조회 |

### 쿼리 예시

```sql
-- idx_notification_user_status 사용
SELECT * FROM notifications
WHERE user_id = ? AND status = 'UNREAD';

SELECT COUNT(*) FROM notifications
WHERE user_id = ? AND status = 'UNREAD';

-- idx_notification_user_created 사용
SELECT * FROM notifications
WHERE user_id = ?
ORDER BY created_at DESC
LIMIT 20;
```

## DTO 구조

### NotificationEvent (수신용)

```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {
    private Long userId;
    private NotificationType type;
    private String title;
    private String message;
    private String link;
    private String referenceId;
    private String referenceType;
}
```

### NotificationResponse (응답용)

```java
@Getter
@Builder
public class NotificationResponse {
    private Long id;
    private Long userId;
    private NotificationType type;
    private String title;
    private String message;
    private String link;
    private NotificationStatus status;
    private String referenceId;
    private String referenceType;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .link(notification.getLink())
                .status(notification.getStatus())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
```

## 도메인 불변식 (Invariants)

### 유효성 규칙

```java
public class NotificationDomainRules {

    // 1. 알림은 반드시 수신자가 있어야 함
    public static void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID is required");
        }
    }

    // 2. 알림 유형은 필수
    public static void validateType(NotificationType type) {
        if (type == null) {
            throw new IllegalArgumentException("Notification type is required");
        }
    }

    // 3. 제목과 내용은 필수
    public static void validateContent(String title, String message) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message is required");
        }
    }

    // 4. 읽은 시간은 읽음 상태에서만 유효
    public static boolean isValidReadAt(NotificationStatus status, LocalDateTime readAt) {
        return (status == NotificationStatus.READ) == (readAt != null);
    }
}
```

## Aggregate 경계

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Notification Aggregate                            │
│                                                                      │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │              Notification (Aggregate Root)                   │   │
│   │                                                             │   │
│   │  ┌─────────────┐  ┌────────────────┐  ┌─────────────────┐  │   │
│   │  │ NotificationType│ │NotificationStatus│ │  Value Objects │  │   │
│   │  │   (Enum)    │  │    (Enum)      │  │  (title, msg)  │  │   │
│   │  └─────────────┘  └────────────────┘  └─────────────────┘  │   │
│   │                                                             │   │
│   │  경계:                                                      │   │
│   │  • Notification은 독립적으로 생성/조회/삭제                   │   │
│   │  • User, Order 등 외부 엔티티는 ID로만 참조                  │   │
│   │  • 트랜잭션 경계 = Notification 단위                        │   │
│   │                                                             │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

## Best Practices

1. **Protected 생성자** - Builder 패턴 강제
2. **불변 ID** - 생성 후 변경 불가
3. **도메인 메서드** - 상태 변경은 메서드로만
4. **멱등성** - 같은 작업 반복해도 결과 동일
5. **적절한 인덱스** - 조회 패턴에 맞는 인덱스
6. **Reference 패턴** - 외부 도메인과 느슨한 결합

## 관련 문서

- [notification-types.md](./notification-types.md) - 알림 유형
- [notification-persistence.md](./notification-persistence.md) - 알림 저장
- [read-status-tracking.md](./read-status-tracking.md) - 읽음 상태 추적
