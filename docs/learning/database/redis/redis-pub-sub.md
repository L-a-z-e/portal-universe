# Redis Pub/Sub

Redis의 Publish/Subscribe 메시징 패턴과 실시간 애플리케이션 구현을 학습합니다.

## 목차

1. [Pub/Sub 개요](#1-pubsub-개요)
2. [기본 사용법](#2-기본-사용법)
3. [Spring Integration](#3-spring-integration)
4. [WebSocket 연동](#4-websocket-연동)
5. [실시간 메시징 구현](#5-실시간-메시징-구현)
6. [패턴 매칭 구독](#6-패턴-매칭-구독)
7. [Pub/Sub vs Streams](#7-pubsub-vs-streams)
8. [프로덕션 고려사항](#8-프로덕션-고려사항)

---

## 1. Pub/Sub 개요

### 개념

```
Pub/Sub 아키텍처:
                              Redis
+------------+          +-------------+          +------------+
| Publisher  |  PUBLISH |   Channel   | MESSAGE  | Subscriber |
|   (App A)  | -------> | "news:tech" | -------> |   (App B)  |
+------------+          +-------------+          +------------+
                              |
                              | MESSAGE
                              v
                        +------------+
                        | Subscriber |
                        |   (App C)  |
                        +------------+

특징:
- Fire and Forget (비동기)
- 1:N 메시지 브로드캐스트
- 실시간 (저지연)
- 메시지 지속성 없음 (구독자 없으면 메시지 손실)
```

### 사용 사례

| 사용 사례 | 설명 |
|-----------|------|
| 실시간 알림 | 새 메시지, 주문 상태 변경 |
| 채팅 | 실시간 메시지 전달 |
| 실시간 대시보드 | 데이터 업데이트 푸시 |
| 캐시 무효화 | 분산 서버 간 캐시 동기화 |
| 이벤트 브로드캐스트 | 시스템 이벤트 전파 |
| 서버 간 통신 | 클러스터 노드 동기화 |

### 제약사항

```
+------------------------------------------+
|           Pub/Sub 제약사항                |
+------------------------------------------+
| 1. 메시지 지속성 없음                     |
|    - 구독자 없으면 메시지 손실            |
|    - 히스토리 조회 불가                   |
|                                          |
| 2. 메시지 보장 없음                       |
|    - At-most-once 전달                   |
|    - ACK 메커니즘 없음                   |
|                                          |
| 3. 구독자 상태 모름                       |
|    - 구독자 목록 조회 어려움              |
|    - 구독자 연결 상태 알 수 없음          |
+------------------------------------------+

해결책: Redis Streams (메시지 지속성 필요 시)
```

---

## 2. 기본 사용법

### Redis CLI

```bash
# 구독자 (터미널 1)
$ redis-cli
127.0.0.1:6379> SUBSCRIBE news:tech news:sports
Reading messages... (press Ctrl-C to quit)
1) "subscribe"
2) "news:tech"
3) (integer) 1
1) "subscribe"
2) "news:sports"
3) (integer) 2

# 발행자 (터미널 2)
$ redis-cli
127.0.0.1:6379> PUBLISH news:tech "New AI breakthrough!"
(integer) 1  # 메시지를 받은 구독자 수

# 구독자 터미널에 출력:
1) "message"
2) "news:tech"
3) "New AI breakthrough!"

# 패턴 구독
127.0.0.1:6379> PSUBSCRIBE news:*
127.0.0.1:6379> PSUBSCRIBE user:*:notifications
```

### 주요 명령어

| 명령어 | 설명 |
|--------|------|
| `SUBSCRIBE channel [channel ...]` | 채널 구독 |
| `UNSUBSCRIBE [channel ...]` | 구독 취소 |
| `PSUBSCRIBE pattern [pattern ...]` | 패턴 구독 |
| `PUNSUBSCRIBE [pattern ...]` | 패턴 구독 취소 |
| `PUBLISH channel message` | 메시지 발행 |
| `PUBSUB CHANNELS [pattern]` | 활성 채널 목록 |
| `PUBSUB NUMSUB [channel ...]` | 채널별 구독자 수 |

---

## 3. Spring Integration

### 기본 설정

```java
@Configuration
public class RedisPubSubConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter messageListener) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 채널 구독
        container.addMessageListener(messageListener,
            new ChannelTopic("notifications"));

        // 패턴 구독
        container.addMessageListener(messageListener,
            new PatternTopic("events:*"));

        return container;
    }

    @Bean
    public MessageListenerAdapter messageListener(RedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
```

### 메시지 발행자

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 단순 메시지 발행
     */
    public void publish(String channel, Object message) {
        redisTemplate.convertAndSend(channel, message);
        log.info("Published to {}: {}", channel, message);
    }

    /**
     * JSON 메시지 발행
     */
    public <T> void publishJson(String channel, T message) {
        redisTemplate.convertAndSend(channel, message);
    }

    /**
     * 사용자별 알림 발행
     */
    public void publishNotification(String userId, NotificationMessage notification) {
        String channel = "notification:" + userId;
        redisTemplate.convertAndSend(channel, notification);
    }

    /**
     * 브로드캐스트 이벤트
     */
    public void broadcastEvent(String eventType, Object payload) {
        String channel = "events:" + eventType;
        EventMessage event = EventMessage.builder()
            .type(eventType)
            .payload(payload)
            .timestamp(LocalDateTime.now())
            .source(getServerInstance())
            .build();

        redisTemplate.convertAndSend(channel, event);
    }

    private String getServerInstance() {
        return System.getenv("HOSTNAME") != null
            ? System.getenv("HOSTNAME")
            : "localhost";
    }
}

@Data
@Builder
public class EventMessage {
    private String type;
    private Object payload;
    private LocalDateTime timestamp;
    private String source;
}

@Data
@Builder
public class NotificationMessage {
    private String id;
    private String title;
    private String content;
    private String type;
    private LocalDateTime createdAt;
    private Map<String, Object> data;
}
```

### 메시지 구독자

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisSubscriber {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 메시지 수신 핸들러
     */
    public void onMessage(String message, String pattern) {
        try {
            log.info("Received message from pattern '{}': {}", pattern, message);

            if (pattern.startsWith("notification:")) {
                handleNotification(message, pattern);
            } else if (pattern.startsWith("events:")) {
                handleEvent(message, pattern);
            } else {
                handleGenericMessage(message, pattern);
            }

        } catch (Exception e) {
            log.error("Failed to process message: {}", message, e);
        }
    }

    private void handleNotification(String message, String channel) {
        try {
            NotificationMessage notification =
                objectMapper.readValue(message, NotificationMessage.class);

            String userId = channel.substring("notification:".length());

            // 알림 처리 로직
            notificationService.processNotification(userId, notification);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse notification: {}", message, e);
        }
    }

    private void handleEvent(String message, String channel) {
        try {
            EventMessage event = objectMapper.readValue(message, EventMessage.class);

            // Spring 이벤트로 변환하여 발행
            eventPublisher.publishEvent(new RedisEventReceived(event));

        } catch (JsonProcessingException e) {
            log.error("Failed to parse event: {}", message, e);
        }
    }

    private void handleGenericMessage(String message, String pattern) {
        log.info("Generic message received on '{}': {}", pattern, message);
    }
}

/**
 * Redis 이벤트를 Spring 이벤트로 래핑
 */
@Getter
public class RedisEventReceived extends ApplicationEvent {

    private final EventMessage eventMessage;

    public RedisEventReceived(EventMessage eventMessage) {
        super(eventMessage);
        this.eventMessage = eventMessage;
    }
}
```

### 다중 채널 리스너

```java
@Configuration
public class MultiChannelPubSubConfig {

    @Bean
    public RedisMessageListenerContainer container(
            RedisConnectionFactory connectionFactory,
            NotificationMessageListener notificationListener,
            EventMessageListener eventListener,
            CacheInvalidationListener cacheListener) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 알림 채널
        container.addMessageListener(notificationListener,
            new PatternTopic("notification:*"));

        // 이벤트 채널
        container.addMessageListener(eventListener,
            Arrays.asList(
                new ChannelTopic("events:order"),
                new ChannelTopic("events:payment"),
                new ChannelTopic("events:inventory")
            ));

        // 캐시 무효화 채널
        container.addMessageListener(cacheListener,
            new ChannelTopic("cache:invalidation"));

        return container;
    }
}

@Component
@RequiredArgsConstructor
public class NotificationMessageListener implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String body = new String(message.getBody());

        String userId = channel.substring("notification:".length());

        try {
            NotificationMessage notification =
                objectMapper.readValue(body, NotificationMessage.class);

            // WebSocket으로 사용자에게 푸시
            messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/notifications",
                notification
            );

        } catch (Exception e) {
            log.error("Failed to process notification", e);
        }
    }
}
```

---

## 4. WebSocket 연동

### 아키텍처

```
다중 서버 환경에서 WebSocket + Redis Pub/Sub:

+--------+     +--------+     +--------+
| Client |     | Client |     | Client |
+--------+     +--------+     +--------+
    |              |              |
    | WS           | WS           | WS
    v              v              v
+--------+     +--------+     +--------+
| Server |     | Server |     | Server |
|   #1   |     |   #2   |     |   #3   |
+--------+     +--------+     +--------+
    |              |              |
    +------+-------+-------+------+
           |               |
           v               v
      +--------+      +--------+
      | Redis  |      | Redis  |
      |  Pub   | <--> |  Sub   |
      +--------+      +--------+

1. Server #1에 연결된 Client A가 메시지 전송
2. Server #1이 Redis에 PUBLISH
3. 모든 서버(#1, #2, #3)가 메시지 수신
4. 각 서버가 자신에게 연결된 클라이언트들에게 WebSocket 전송
```

### WebSocket 설정

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 구독 대상 prefix
        registry.enableSimpleBroker("/topic", "/queue");

        // 메시지 전송 prefix
        registry.setApplicationDestinationPrefixes("/app");

        // 사용자별 대상 prefix
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOrigins("*")
            .withSockJS();
    }
}
```

### Redis Pub/Sub을 통한 WebSocket 메시지 브로드캐스트

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPushService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 특정 사용자에게 알림 발송
     * (다른 서버에 연결된 사용자도 수신 가능)
     */
    public void sendToUser(String userId, NotificationResponse notification) {
        // 로컬 WebSocket 전송 시도
        messagingTemplate.convertAndSendToUser(
            userId,
            "/queue/notifications",
            notification
        );

        // Redis Pub/Sub으로 다른 서버에도 전파
        String channel = "notification:" + userId;
        try {
            String json = objectMapper.writeValueAsString(notification);
            redisTemplate.convertAndSend(channel, json);
            log.debug("Published notification to Redis: {}", channel);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notification", e);
        }
    }

    /**
     * 전체 브로드캐스트
     */
    public void broadcast(String topic, Object message) {
        messagingTemplate.convertAndSend("/topic/" + topic, message);

        // Redis로도 전파
        redisTemplate.convertAndSend("broadcast:" + topic, message);
    }

    /**
     * 특정 그룹에 메시지 전송
     */
    public void sendToGroup(String groupId, Object message) {
        String channel = "group:" + groupId;
        redisTemplate.convertAndSend(channel, message);
    }
}

/**
 * Redis 메시지를 받아 WebSocket으로 전달
 * (Portal Universe 패턴 참조)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRedisSubscriber {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public void onMessage(String message, String pattern) {
        try {
            // 채널에서 userId 추출 (notification:{userId})
            if (pattern.startsWith("notification:")) {
                String userId = pattern.substring("notification:".length());

                NotificationResponse notification =
                    objectMapper.readValue(message, NotificationResponse.class);

                // WebSocket으로 전달
                messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/notifications",
                    notification
                );

                log.debug("Pushed notification to user {} via WebSocket", userId);
            }
        } catch (Exception e) {
            log.error("Failed to process Redis notification message", e);
        }
    }
}
```

### 채팅 애플리케이션 예제

```java
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatRoomService chatRoomService;

    /**
     * 채팅 메시지 전송
     */
    @MessageMapping("/chat/send")
    public void sendMessage(
            @Payload ChatMessage message,
            @Header("simpSessionId") String sessionId) {

        message.setTimestamp(LocalDateTime.now());
        message.setId(UUID.randomUUID().toString());

        // Redis에 발행 (모든 서버에 전파)
        String channel = "chat:room:" + message.getRoomId();
        redisTemplate.convertAndSend(channel, message);

        // 메시지 저장 (선택적)
        chatRoomService.saveMessage(message);

        log.info("Message sent to room {}: {}", message.getRoomId(), message.getContent());
    }

    /**
     * 채팅방 입장
     */
    @MessageMapping("/chat/join")
    public void joinRoom(
            @Payload JoinRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        String sessionId = headerAccessor.getSessionId();

        // 세션에 사용자 정보 저장
        headerAccessor.getSessionAttributes().put("username", request.getUsername());
        headerAccessor.getSessionAttributes().put("roomId", request.getRoomId());

        // 입장 알림
        ChatMessage joinMessage = ChatMessage.builder()
            .id(UUID.randomUUID().toString())
            .roomId(request.getRoomId())
            .sender("SYSTEM")
            .content(request.getUsername() + " 님이 입장했습니다.")
            .type(MessageType.JOIN)
            .timestamp(LocalDateTime.now())
            .build();

        redisTemplate.convertAndSend("chat:room:" + request.getRoomId(), joinMessage);
    }
}

@Data
@Builder
public class ChatMessage {
    private String id;
    private String roomId;
    private String sender;
    private String content;
    private MessageType type;
    private LocalDateTime timestamp;

    public enum MessageType {
        CHAT, JOIN, LEAVE, SYSTEM
    }
}

/**
 * 채팅 메시지 수신 및 WebSocket 전달
 */
@Component
@RequiredArgsConstructor
public class ChatMessageListener implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String body = new String(message.getBody());

        if (channel.startsWith("chat:room:")) {
            String roomId = channel.substring("chat:room:".length());

            try {
                ChatMessage chatMessage = objectMapper.readValue(body, ChatMessage.class);

                // 해당 방의 모든 구독자에게 전송
                messagingTemplate.convertAndSend(
                    "/topic/chat/" + roomId,
                    chatMessage
                );

            } catch (Exception e) {
                log.error("Failed to process chat message", e);
            }
        }
    }
}
```

---

## 5. 실시간 메시징 구현

### 주문 상태 실시간 업데이트

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderStatusService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderRepository orderRepository;

    /**
     * 주문 상태 변경 및 실시간 알림
     */
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        order.setStatus(newStatus);
        orderRepository.save(order);

        // 실시간 상태 업데이트 발행
        OrderStatusUpdate update = OrderStatusUpdate.builder()
            .orderId(orderId)
            .status(newStatus.name())
            .statusText(newStatus.getDisplayName())
            .updatedAt(LocalDateTime.now())
            .build();

        // 사용자에게 알림
        String userChannel = "order:status:" + order.getUserId();
        redisTemplate.convertAndSend(userChannel, update);

        // 관리자 대시보드 업데이트
        redisTemplate.convertAndSend("admin:orders:updates", update);

        log.info("Order {} status updated to {}", orderId, newStatus);
    }
}

/**
 * 실시간 대시보드 데이터 푸시
 */
@Service
@RequiredArgsConstructor
public class DashboardPushService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Scheduled(fixedRate = 5000)  // 5초마다
    public void pushDashboardStats() {
        DashboardStats stats = calculateStats();

        redisTemplate.convertAndSend("dashboard:stats", stats);
    }

    @EventListener
    public void onSalesEvent(SalesEvent event) {
        // 판매 발생 시 즉시 대시보드 업데이트
        SalesUpdate update = SalesUpdate.builder()
            .amount(event.getAmount())
            .productId(event.getProductId())
            .timestamp(LocalDateTime.now())
            .build();

        redisTemplate.convertAndSend("dashboard:sales", update);
    }

    private DashboardStats calculateStats() {
        // 통계 계산 로직
        return DashboardStats.builder()
            .totalOrders(orderRepository.countToday())
            .totalRevenue(orderRepository.sumRevenueToday())
            .activeUsers(sessionService.countActiveSessions())
            .build();
    }
}
```

### 캐시 무효화 이벤트

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheManager cacheManager;
    private static final String INVALIDATION_CHANNEL = "cache:invalidation";

    /**
     * 캐시 무효화 이벤트 발행
     */
    public void invalidateCache(String cacheName, Object key) {
        CacheInvalidationEvent event = CacheInvalidationEvent.builder()
            .cacheName(cacheName)
            .key(key != null ? key.toString() : null)
            .timestamp(System.currentTimeMillis())
            .source(getServerInstance())
            .build();

        redisTemplate.convertAndSend(INVALIDATION_CHANNEL, event);
        log.info("Cache invalidation event published: {}", event);
    }

    /**
     * 패턴 기반 캐시 무효화
     */
    public void invalidateCacheByPattern(String cacheName, String pattern) {
        CacheInvalidationEvent event = CacheInvalidationEvent.builder()
            .cacheName(cacheName)
            .pattern(pattern)
            .timestamp(System.currentTimeMillis())
            .source(getServerInstance())
            .build();

        redisTemplate.convertAndSend(INVALIDATION_CHANNEL, event);
    }

    private String getServerInstance() {
        return System.getenv("HOSTNAME");
    }
}

/**
 * 캐시 무효화 이벤트 수신
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationListener implements MessageListener {

    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;
    private final String serverInstance = System.getenv("HOSTNAME");

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            CacheInvalidationEvent event = objectMapper.readValue(
                message.getBody(), CacheInvalidationEvent.class);

            // 자신이 발행한 이벤트는 이미 처리됨
            if (serverInstance != null && serverInstance.equals(event.getSource())) {
                return;
            }

            Cache cache = cacheManager.getCache(event.getCacheName());
            if (cache == null) {
                return;
            }

            if (event.getKey() != null) {
                cache.evict(event.getKey());
                log.debug("Cache evicted: {}:{}", event.getCacheName(), event.getKey());
            } else if (event.getPattern() != null) {
                // 패턴 기반 무효화는 전체 캐시 클리어
                cache.clear();
                log.debug("Cache cleared: {}", event.getCacheName());
            }

        } catch (Exception e) {
            log.error("Failed to process cache invalidation event", e);
        }
    }
}

@Data
@Builder
public class CacheInvalidationEvent {
    private String cacheName;
    private String key;
    private String pattern;
    private long timestamp;
    private String source;
}
```

---

## 6. 패턴 매칭 구독

### 패턴 문법

```
패턴 문법:
* : 0개 이상의 모든 문자
? : 정확히 1개의 문자
[...] : 문자 클래스

예시:
news:*           -> news:tech, news:sports, news:politics
user:*:profile   -> user:123:profile, user:456:profile
order:????       -> order:1234, order:5678 (4자리만)
chat:[a-z]*      -> chat:alpha, chat:beta
```

### 패턴 구독 구현

```java
@Configuration
public class PatternSubscriptionConfig {

    @Bean
    public RedisMessageListenerContainer patternContainer(
            RedisConnectionFactory connectionFactory,
            UserEventListener userListener,
            OrderEventListener orderListener) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 사용자 관련 모든 이벤트
        container.addMessageListener(userListener,
            new PatternTopic("user:*"));

        // 주문 관련 모든 이벤트
        container.addMessageListener(orderListener,
            new PatternTopic("order:*:*"));

        // 알림 (사용자별)
        container.addMessageListener(notificationListener,
            new PatternTopic("notification:*"));

        return container;
    }
}

@Component
@Slf4j
public class UserEventListener implements MessageListener {

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String body = new String(message.getBody());
        String patternStr = new String(pattern);

        log.info("Received on pattern '{}', channel '{}': {}",
            patternStr, channel, body);

        // 채널 파싱
        // user:123:login -> userId=123, event=login
        String[] parts = channel.split(":");
        if (parts.length >= 3) {
            String userId = parts[1];
            String eventType = parts[2];

            handleUserEvent(userId, eventType, body);
        }
    }

    private void handleUserEvent(String userId, String eventType, String body) {
        switch (eventType) {
            case "login" -> handleLogin(userId, body);
            case "logout" -> handleLogout(userId, body);
            case "profile" -> handleProfileUpdate(userId, body);
            default -> log.warn("Unknown user event type: {}", eventType);
        }
    }
}
```

### 동적 구독 관리

```java
@Service
@RequiredArgsConstructor
public class DynamicSubscriptionService {

    private final RedisMessageListenerContainer container;
    private final MessageListenerAdapter listenerAdapter;
    private final Map<String, ChannelTopic> activeSubscriptions = new ConcurrentHashMap<>();

    /**
     * 런타임에 채널 구독 추가
     */
    public void subscribe(String channel) {
        if (activeSubscriptions.containsKey(channel)) {
            return;
        }

        ChannelTopic topic = new ChannelTopic(channel);
        container.addMessageListener(listenerAdapter, topic);
        activeSubscriptions.put(channel, topic);

        log.info("Subscribed to channel: {}", channel);
    }

    /**
     * 구독 취소
     */
    public void unsubscribe(String channel) {
        ChannelTopic topic = activeSubscriptions.remove(channel);
        if (topic != null) {
            container.removeMessageListener(listenerAdapter, topic);
            log.info("Unsubscribed from channel: {}", channel);
        }
    }

    /**
     * 사용자별 알림 채널 구독
     */
    public void subscribeUserNotifications(String userId) {
        subscribe("notification:" + userId);
    }

    /**
     * 활성 구독 목록
     */
    public Set<String> getActiveSubscriptions() {
        return Collections.unmodifiableSet(activeSubscriptions.keySet());
    }
}
```

---

## 7. Pub/Sub vs Streams

### 비교

```
+--------------------+--------------------+--------------------+
| 특성               | Pub/Sub            | Streams            |
+--------------------+--------------------+--------------------+
| 메시지 지속성      | X (Fire & Forget)  | O (영구 저장)      |
| 메시지 ACK         | X                  | O                  |
| Consumer Groups    | X                  | O                  |
| 메시지 재처리      | X                  | O                  |
| 히스토리 조회      | X                  | O                  |
| 지연 시간          | 매우 낮음          | 낮음               |
| 구독자 필요        | O (없으면 손실)    | X                  |
| 사용 사례          | 실시간 알림        | 이벤트 소싱        |
|                    | 캐시 무효화        | 작업 큐            |
|                    | 채팅               | 로그 스트리밍      |
+--------------------+--------------------+--------------------+
```

### 선택 가이드

```
메시지 손실이 허용되는가?
      |
+-----+-----+
|           |
Yes         No
|           |
Pub/Sub     |
            |
      지속성/재처리 필요?
            |
      +-----+-----+
      |           |
     Yes         No
      |           |
   Streams    Pub/Sub with
              Acknowledgment
              (Custom 구현)
```

### Streams 간단 예제

```java
@Service
@RequiredArgsConstructor
public class RedisStreamsService {

    private final StringRedisTemplate redisTemplate;

    /**
     * Stream에 메시지 추가
     */
    public RecordId addToStream(String streamKey, Map<String, Object> message) {
        StreamRecords.MapBackedRecord<String, Object> record =
            StreamRecords.newRecord()
                .ofMap(message)
                .withStreamKey(streamKey);

        return redisTemplate.opsForStream().add(record);
    }

    /**
     * Consumer Group으로 읽기
     */
    public List<MapRecord<String, Object, Object>> readFromGroup(
            String streamKey,
            String groupName,
            String consumerName) {

        return redisTemplate.opsForStream().read(
            Consumer.from(groupName, consumerName),
            StreamReadOptions.empty().count(10),
            StreamOffset.create(streamKey, ReadOffset.lastConsumed())
        );
    }

    /**
     * 메시지 ACK
     */
    public void acknowledge(String streamKey, String groupName, RecordId... recordIds) {
        redisTemplate.opsForStream().acknowledge(streamKey, groupName, recordIds);
    }
}
```

---

## 8. 프로덕션 고려사항

### 연결 관리

```java
@Configuration
public class RobustPubSubConfig {

    @Bean
    public RedisMessageListenerContainer robustContainer(
            RedisConnectionFactory connectionFactory) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 에러 핸들러
        container.setErrorHandler(e -> {
            log.error("Redis Pub/Sub error", e);
            // 알림 발송, 메트릭 기록 등
        });

        // 복구 간격 설정
        container.setRecoveryInterval(5000);  // 5초

        // 구독자 실행자 설정
        container.setSubscriptionExecutor(
            Executors.newFixedThreadPool(4));

        // 태스크 실행자 설정
        container.setTaskExecutor(
            Executors.newCachedThreadPool());

        return container;
    }
}
```

### 메시지 손실 대응

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ReliableMessagingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MessageRepository messageRepository;

    /**
     * Pub/Sub + 영구 저장 하이브리드
     */
    public void sendReliableMessage(String channel, Message message) {
        // 1. DB에 먼저 저장
        message.setStatus(MessageStatus.PENDING);
        messageRepository.save(message);

        try {
            // 2. Pub/Sub으로 실시간 전송
            redisTemplate.convertAndSend(channel, message);

            // 3. 상태 업데이트
            message.setStatus(MessageStatus.SENT);
            messageRepository.save(message);

        } catch (Exception e) {
            log.error("Failed to publish message, will retry later", e);
            message.setStatus(MessageStatus.FAILED);
            messageRepository.save(message);
        }
    }

    /**
     * 실패한 메시지 재전송 (스케줄러)
     */
    @Scheduled(fixedDelay = 60000)
    public void retryFailedMessages() {
        List<Message> failed = messageRepository
            .findByStatusAndCreatedAtAfter(
                MessageStatus.FAILED,
                LocalDateTime.now().minusHours(1)
            );

        for (Message message : failed) {
            try {
                redisTemplate.convertAndSend(message.getChannel(), message);
                message.setStatus(MessageStatus.SENT);
            } catch (Exception e) {
                message.setRetryCount(message.getRetryCount() + 1);
            }
            messageRepository.save(message);
        }
    }
}
```

### 모니터링

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class PubSubMonitor {

    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;

    /**
     * 활성 채널 모니터링
     */
    @Scheduled(fixedRate = 30000)
    public void monitorChannels() {
        // 활성 채널 수
        Long numChannels = redisTemplate.execute((RedisCallback<Long>) connection -> {
            Properties result = connection.commands().info("clients");
            // pubsub_channels 파싱
            return 0L;
        });

        meterRegistry.gauge("redis.pubsub.channels", numChannels);
    }

    /**
     * 메시지 카운터
     */
    public void recordPublish(String channel) {
        meterRegistry.counter("redis.pubsub.publish",
            "channel", channel).increment();
    }

    public void recordReceive(String channel) {
        meterRegistry.counter("redis.pubsub.receive",
            "channel", channel).increment();
    }
}

/**
 * 메시지 처리 시간 측정 래퍼
 */
@Component
public class TimedMessageListener implements MessageListener {

    private final MessageListener delegate;
    private final MeterRegistry meterRegistry;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            delegate.onMessage(message, pattern);
        } finally {
            sample.stop(Timer.builder("redis.pubsub.process")
                .tag("channel", new String(message.getChannel()))
                .register(meterRegistry));
        }
    }
}
```

---

## 관련 문서

- [Redis Data Structures](./redis-data-structures.md)
- [Redis Spring Integration](./redis-spring-integration.md)
- [Redis Portal Universe](./redis-portal-universe.md)
- [Redis Troubleshooting](./redis-troubleshooting.md)
