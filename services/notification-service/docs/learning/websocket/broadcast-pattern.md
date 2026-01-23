# Broadcast Pattern

## 개요

브로드캐스트 패턴은 메시지를 다수의 클라이언트에게 동시에 전달하는 통신 방식입니다. Notification Service에서는 시스템 공지, 타임딜 알림 등을 모든 사용자 또는 특정 그룹에게 전송할 때 사용합니다.

## 브로드캐스트 유형

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Broadcast Types                                  │
│                                                                      │
│  1. Global Broadcast (전체 전송)                                    │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │               /topic/notifications                           │    │
│  │                      │                                       │    │
│  │        ┌─────────────┼─────────────┐                        │    │
│  │        ▼             ▼             ▼                        │    │
│  │     User A        User B        User C                      │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  2. User-Specific (개인 전송)                                       │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │           /user/{userId}/queue/notifications                 │    │
│  │                      │                                       │    │
│  │                      ▼                                       │    │
│  │                   User A (only)                              │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  3. Group Broadcast (그룹 전송)                                     │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │               /topic/group/{groupId}                         │    │
│  │                      │                                       │    │
│  │        ┌─────────────┼─────────────┐                        │    │
│  │        ▼             ▼             │                        │    │
│  │     User A        User B          │  (그룹 멤버만)           │    │
│  │     (member)     (member)         │                         │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

## Portal Universe 구현

### NotificationPushService

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper redisObjectMapper;

    // 개인 알림 전송
    public void push(Notification notification) {
        NotificationResponse response = NotificationResponse.from(notification);

        // 1. WebSocket 직접 전송
        messagingTemplate.convertAndSendToUser(
                notification.getUserId().toString(),
                "/queue/notifications",
                response
        );

        // 2. Redis Pub/Sub (다중 인스턴스 지원)
        String channel = "notification:" + notification.getUserId();
        try {
            String jsonPayload = redisObjectMapper.writeValueAsString(response);
            redisTemplate.convertAndSend(channel, jsonPayload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notification for Redis", e);
        }
    }

    // 전체 브로드캐스트
    public void pushToAll(NotificationResponse response) {
        messagingTemplate.convertAndSend("/topic/notifications", response);
        log.debug("Broadcast notification to all users");
    }
}
```

## 전체 브로드캐스트

### 시스템 공지

```java
@Service
@RequiredArgsConstructor
public class SystemAnnouncementService {

    private final SimpMessagingTemplate messagingTemplate;

    // 긴급 공지
    public void sendUrgentAnnouncement(String title, String message) {
        NotificationResponse announcement = NotificationResponse.builder()
                .type(NotificationType.SYSTEM)
                .title(title)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/announcements", announcement);
        log.info("Urgent announcement sent: {}", title);
    }

    // 서비스 점검 공지
    public void sendMaintenanceNotice(LocalDateTime startTime, Duration duration) {
        Map<String, Object> notice = Map.of(
                "type", "MAINTENANCE",
                "startTime", startTime,
                "duration", duration.toMinutes(),
                "message", String.format(
                        "서비스 점검 예정: %s부터 약 %d분간",
                        startTime.format(DateTimeFormatter.ofPattern("MM월 dd일 HH:mm")),
                        duration.toMinutes()
                )
        );

        messagingTemplate.convertAndSend("/topic/maintenance", notice);
    }
}
```

### 타임딜 알림

```java
@Service
@RequiredArgsConstructor
public class TimeDealBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    // 타임딜 시작 알림
    public void broadcastTimeDealStarted(TimeDeal timeDeal) {
        NotificationResponse notification = NotificationResponse.builder()
                .type(NotificationType.TIMEDEAL_STARTED)
                .title("타임딜 시작!")
                .message(String.format(
                        "%s - %d%% 할인",
                        timeDeal.getProductName(),
                        timeDeal.getDiscountPercent()
                ))
                .link("/timedeal/" + timeDeal.getId())
                .createdAt(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/timedeal", notification);
        log.info("TimeDeal broadcast: {}", timeDeal.getId());
    }

    // 타임딜 종료 임박 알림
    public void broadcastTimeDealEnding(TimeDeal timeDeal, int minutesLeft) {
        Map<String, Object> alert = Map.of(
                "type", "TIMEDEAL_ENDING",
                "dealId", timeDeal.getId(),
                "productName", timeDeal.getProductName(),
                "minutesLeft", minutesLeft,
                "message", String.format(
                        "%s 타임딜이 %d분 후 종료됩니다!",
                        timeDeal.getProductName(),
                        minutesLeft
                )
        );

        messagingTemplate.convertAndSend("/topic/timedeal/ending", alert);
    }
}
```

## 그룹 브로드캐스트

### 그룹 기반 전송

```java
@Service
@RequiredArgsConstructor
public class GroupNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserGroupRepository userGroupRepository;

    // 특정 그룹에 알림 전송 (토픽 기반)
    public void notifyGroup(String groupId, NotificationResponse notification) {
        String destination = "/topic/group/" + groupId;
        messagingTemplate.convertAndSend(destination, notification);
        log.info("Group notification sent: groupId={}", groupId);
    }

    // VIP 고객 전용 알림
    public void notifyVIPUsers(NotificationResponse notification) {
        List<Long> vipUserIds = userGroupRepository.findVIPUserIds();

        vipUserIds.forEach(userId -> {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    notification
            );
        });

        log.info("VIP notification sent to {} users", vipUserIds.size());
    }

    // 지역 기반 알림
    public void notifyByRegion(String regionCode, NotificationResponse notification) {
        String destination = "/topic/region/" + regionCode;
        messagingTemplate.convertAndSend(destination, notification);
    }
}
```

### 클라이언트 그룹 구독

```javascript
// 클라이언트에서 그룹 구독
const groupId = 'vip-members';

client.subscribe(`/topic/group/${groupId}`, (message) => {
    const notification = JSON.parse(message.body);
    handleGroupNotification(notification);
});

// 지역 기반 구독
const regionCode = 'seoul';

client.subscribe(`/topic/region/${regionCode}`, (message) => {
    const notification = JSON.parse(message.body);
    handleRegionalNotification(notification);
});
```

## 선택적 브로드캐스트

### 조건 기반 전송

```java
@Service
@RequiredArgsConstructor
public class ConditionalBroadcastService {

    private final SimpUserRegistry userRegistry;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserPreferenceService preferenceService;

    // 알림 설정에 따른 선택적 전송
    public void broadcastWithPreference(NotificationResponse notification) {
        userRegistry.getUsers().forEach(user -> {
            String userId = user.getName();

            // 사용자 알림 설정 확인
            if (preferenceService.isNotificationEnabled(userId, notification.getType())) {
                messagingTemplate.convertAndSendToUser(
                        userId,
                        "/queue/notifications",
                        notification
                );
            }
        });
    }

    // 온라인 사용자에게만 전송
    public void broadcastToOnlineUsers(NotificationResponse notification) {
        userRegistry.getUsers().forEach(user -> {
            messagingTemplate.convertAndSendToUser(
                    user.getName(),
                    "/queue/notifications",
                    notification
            );
        });

        log.info("Broadcast to {} online users", userRegistry.getUserCount());
    }
}
```

## Redis Pub/Sub 연동

### 다중 인스턴스 브로드캐스트

```
┌─────────────────────────────────────────────────────────────────────┐
│               Multi-Instance Broadcast with Redis                    │
│                                                                      │
│  ┌──────────────┐                      ┌──────────────┐             │
│  │ Instance 1   │                      │ Instance 2   │             │
│  │              │                      │              │             │
│  │ User A ◄─────┼─── WebSocket ───────▶│ User C       │             │
│  │ User B       │         │            │ User D       │             │
│  └──────────────┘         │            └──────────────┘             │
│                           │                                          │
│                           ▼                                          │
│                    ┌─────────────┐                                   │
│                    │   Redis     │                                   │
│                    │  Pub/Sub    │                                   │
│                    └─────────────┘                                   │
│                           │                                          │
│              ┌────────────┴────────────┐                            │
│              │                         │                            │
│              ▼                         ▼                            │
│      Instance 1 receives       Instance 2 receives                   │
│      → forwards to A, B        → forwards to C, D                   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Redis Subscriber

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRedisSubscriber {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper redisObjectMapper;

    public void onMessage(String message, String pattern) {
        try {
            // 채널에서 userId 추출 (notification:{userId})
            String channel = pattern;
            if (channel.startsWith("notification:")) {
                String userId = channel.substring("notification:".length());

                NotificationResponse notification =
                        redisObjectMapper.readValue(message, NotificationResponse.class);

                // WebSocket으로 전달
                messagingTemplate.convertAndSendToUser(
                        userId,
                        "/queue/notifications",
                        notification
                );

                log.debug("Forwarded Redis message to user: {}", userId);
            }
        } catch (Exception e) {
            log.error("Failed to process Redis notification message", e);
        }
    }
}
```

### 브로드캐스트 Redis 채널

```java
@Service
@RequiredArgsConstructor
public class RedisBroadcastService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // 전체 브로드캐스트를 Redis로 발행
    public void publishBroadcast(NotificationResponse notification) {
        try {
            String payload = objectMapper.writeValueAsString(notification);
            redisTemplate.convertAndSend("notification:broadcast", payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize broadcast notification", e);
        }
    }

    // 그룹 브로드캐스트를 Redis로 발행
    public void publishGroupBroadcast(String groupId, NotificationResponse notification) {
        try {
            String payload = objectMapper.writeValueAsString(notification);
            redisTemplate.convertAndSend("notification:group:" + groupId, payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize group broadcast notification", e);
        }
    }
}
```

## 성능 고려사항

### 대량 브로드캐스트

```java
@Service
@RequiredArgsConstructor
public class BulkBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry userRegistry;

    // 배치 처리로 대량 전송
    @Async
    public CompletableFuture<Integer> broadcastInBatches(
            NotificationResponse notification,
            int batchSize) {

        List<SimpUser> users = new ArrayList<>(userRegistry.getUsers());
        int totalSent = 0;

        for (int i = 0; i < users.size(); i += batchSize) {
            List<SimpUser> batch = users.subList(
                    i,
                    Math.min(i + batchSize, users.size())
            );

            batch.forEach(user -> {
                messagingTemplate.convertAndSendToUser(
                        user.getName(),
                        "/queue/notifications",
                        notification
                );
            });

            totalSent += batch.size();

            // 배치 간 짧은 지연으로 부하 분산
            Thread.sleep(10);
        }

        return CompletableFuture.completedFuture(totalSent);
    }
}
```

### 메시지 압축

```java
@Configuration
public class WebSocketMessageConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(128 * 1024);
        // 대용량 메시지 압축 고려
    }
}
```

## Best Practices

1. **토픽 계층 구조** - `/topic/category/subcategory` 형태
2. **Redis Pub/Sub 활용** - 다중 인스턴스 환경
3. **배치 전송** - 대량 브로드캐스트 시 부하 분산
4. **선택적 브로드캐스트** - 사용자 설정 존중
5. **메시지 크기 제한** - 대용량 데이터는 REST API
6. **모니터링** - 브로드캐스트 성능 추적

## 관련 문서

- [websocket-architecture.md](./websocket-architecture.md) - WebSocket 아키텍처
- [scaling-websocket.md](./scaling-websocket.md) - WebSocket 스케일링
- [stomp-integration.md](./stomp-integration.md) - STOMP 프로토콜
