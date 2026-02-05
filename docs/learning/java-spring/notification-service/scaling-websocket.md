# Scaling WebSocket

## 개요

WebSocket 스케일링은 다수의 동시 연결을 처리하고, 다중 서버 환경에서도 메시지를 정확히 전달하기 위한 전략입니다. Notification Service는 Redis Pub/Sub을 활용하여 다중 인스턴스 환경을 지원합니다.

## 스케일링 과제

### 단일 서버 한계

```
┌─────────────────────────────────────────────────────────────────────┐
│                  Single Server Limitation                            │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐     │
│  │                    Notification Service                     │     │
│  │                                                            │     │
│  │    WebSocket Connections                                   │     │
│  │    ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ... ┌──┐  (10,000+ 연결)      │     │
│  │    │C1│ │C2│ │C3│ │C4│ │C5│     │Cn│                      │     │
│  │    └──┘ └──┘ └──┘ └──┘ └──┘     └──┘                      │     │
│  │                                                            │     │
│  │    문제점:                                                  │     │
│  │    • 단일 서버 리소스 한계 (메모리, CPU, 소켓)               │     │
│  │    • 단일 장애점 (SPOF)                                    │     │
│  │    • 배포 시 모든 연결 끊김                                 │     │
│  │                                                            │     │
│  └────────────────────────────────────────────────────────────┘     │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 다중 서버 문제

```
┌─────────────────────────────────────────────────────────────────────┐
│             Multi-Server Challenge: Message Routing                  │
│                                                                      │
│                       Load Balancer                                  │
│                           │                                          │
│            ┌──────────────┼──────────────┐                          │
│            │              │              │                          │
│            ▼              ▼              ▼                          │
│     ┌──────────┐   ┌──────────┐   ┌──────────┐                      │
│     │Instance 1│   │Instance 2│   │Instance 3│                      │
│     │          │   │          │   │          │                      │
│     │ User A   │   │ User B   │   │ User C   │                      │
│     │ User D   │   │ User E   │   │ User F   │                      │
│     └──────────┘   └──────────┘   └──────────┘                      │
│                                                                      │
│     문제: User A에게 알림을 보내려면?                                │
│     → Instance 1에만 연결되어 있음                                  │
│     → Instance 2, 3에서는 User A를 알 수 없음                       │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

## Redis Pub/Sub 솔루션

### 아키텍처

```
┌─────────────────────────────────────────────────────────────────────┐
│                Redis Pub/Sub Architecture                            │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                        Redis Cluster                          │   │
│  │                                                               │   │
│  │    Channels:                                                  │   │
│  │    • notification:broadcast    (전체 브로드캐스트)             │   │
│  │    • notification:{userId}     (사용자별 메시지)               │   │
│  │    • notification:group:{gid}  (그룹 메시지)                  │   │
│  │                                                               │   │
│  └──────────────────────────────────────────────────────────────┘   │
│        │ Subscribe            │ Subscribe            │ Subscribe    │
│        ▼                      ▼                      ▼              │
│  ┌──────────┐           ┌──────────┐           ┌──────────┐        │
│  │Instance 1│           │Instance 2│           │Instance 3│        │
│  │          │           │          │           │          │        │
│  │ User A ◄─┼───────────┼──────────┼───────────┼──────────│        │
│  │ User D   │           │ User B   │           │ User C   │        │
│  └──────────┘           │ User E   │           │ User F   │        │
│                         └──────────┘           └──────────┘        │
│                                                                      │
│  흐름:                                                              │
│  1. Instance 2가 User A에게 알림 발송 요청 수신                     │
│  2. Redis channel 'notification:userA'로 Publish                    │
│  3. 모든 Instance가 Subscribe하고 있으므로 수신                     │
│  4. Instance 1이 User A 연결을 가지고 있으므로 WebSocket 전송       │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Portal Universe 구현

```java
// RedisConfig.java
@Configuration
public class RedisConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter listenerAdapter) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // notification:* 패턴의 모든 채널 구독
        container.addMessageListener(listenerAdapter, new PatternTopic("notification:*"));
        return container;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(NotificationRedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }
}
```

```java
// NotificationRedisSubscriber.java
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRedisSubscriber {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper redisObjectMapper;

    public void onMessage(String message, String pattern) {
        try {
            String channel = pattern;
            if (channel.startsWith("notification:")) {
                String userId = channel.substring("notification:".length());

                NotificationResponse notification =
                        redisObjectMapper.readValue(message, NotificationResponse.class);

                // 이 인스턴스에 연결된 사용자에게 전송
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

```java
// NotificationPushService.java
@Service
@RequiredArgsConstructor
public class NotificationPushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper redisObjectMapper;

    public void push(Notification notification) {
        NotificationResponse response = NotificationResponse.from(notification);

        // 1. 로컬 WebSocket 전송 (같은 인스턴스 사용자)
        messagingTemplate.convertAndSendToUser(
                notification.getUserId().toString(),
                "/queue/notifications",
                response
        );

        // 2. Redis Pub/Sub 발행 (다른 인스턴스 사용자)
        String channel = "notification:" + notification.getUserId();
        try {
            String jsonPayload = redisObjectMapper.writeValueAsString(response);
            redisTemplate.convertAndSend(channel, jsonPayload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notification for Redis", e);
        }
    }
}
```

## Sticky Session vs Redis Pub/Sub

### Sticky Session

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Sticky Session Approach                          │
│                                                                      │
│  Load Balancer (Session Affinity)                                    │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │ User A → Instance 1 (항상)                                   │    │
│  │ User B → Instance 2 (항상)                                   │    │
│  │ User C → Instance 3 (항상)                                   │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  장점:                          │  단점:                             │
│  • 구현 간단                     │  • 불균등 부하 분산                 │
│  • 추가 인프라 불필요             │  • 인스턴스 장애 시 세션 유실       │
│                                │  • 스케일링 유연성 저하             │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Redis Pub/Sub (Portal Universe 선택)

```
┌─────────────────────────────────────────────────────────────────────┐
│                   Redis Pub/Sub Approach                             │
│                                                                      │
│  Load Balancer (Round Robin)                                         │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │ 요청은 어느 인스턴스로든 가능                                 │    │
│  │ Redis가 메시지를 올바른 인스턴스로 라우팅                     │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  장점:                          │  단점:                             │
│  • 균등 부하 분산                │  • Redis 의존성 추가               │
│  • 높은 가용성                   │  • 네트워크 지연 증가               │
│  • 유연한 스케일링               │  • Redis 장애 시 영향               │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

## Kubernetes 스케일링

### Deployment 설정

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: notification-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: notification-service
  template:
    metadata:
      labels:
        app: notification-service
    spec:
      containers:
        - name: notification-service
          image: notification-service:latest
          ports:
            - containerPort: 8084
          env:
            - name: SPRING_REDIS_HOST
              value: "redis-cluster"
            - name: SPRING_KAFKA_BOOTSTRAP_SERVERS
              value: "kafka:9092"
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "500m"
```

### HPA (Horizontal Pod Autoscaler)

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: notification-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: notification-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 70
    - type: Pods
      pods:
        metric:
          name: websocket_connections
        target:
          type: AverageValue
          averageValue: "5000"  # 인스턴스당 평균 5000 연결
```

### Service 설정

```yaml
apiVersion: v1
kind: Service
metadata:
  name: notification-service
spec:
  selector:
    app: notification-service
  ports:
    - port: 8084
      targetPort: 8084
  # WebSocket은 sessionAffinity 불필요 (Redis Pub/Sub 사용)
  sessionAffinity: None
```

## 모니터링

### 주요 메트릭

```java
@Component
@RequiredArgsConstructor
public class WebSocketScalingMetrics {

    private final SimpUserRegistry userRegistry;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedRate = 30000)
    public void recordMetrics() {
        // 현재 인스턴스의 연결 수
        meterRegistry.gauge("websocket.connections",
                userRegistry.getUserCount());

        // 현재 인스턴스의 구독 수
        long subscriptions = userRegistry.getUsers().stream()
                .flatMap(user -> user.getSessions().stream())
                .mapToLong(session -> session.getSubscriptions().size())
                .sum();
        meterRegistry.gauge("websocket.subscriptions", subscriptions);
    }
}
```

### Grafana 대시보드 쿼리

```promql
# 전체 인스턴스의 총 연결 수
sum(websocket_connections)

# 인스턴스별 연결 분포
websocket_connections{instance=~"notification-service.*"}

# 연결 증가율
rate(websocket_connections[5m])

# Redis Pub/Sub 메시지 처리량
rate(redis_pubsub_messages_total[5m])
```

## 용량 계획

### 단일 인스턴스 용량

| 리소스 | 권장 사양 | 연결 수 |
|--------|----------|---------|
| CPU | 2 Core | ~5,000 |
| Memory | 2GB | ~5,000 |
| Network | 1Gbps | ~10,000 |

### 스케일링 공식

```
필요 인스턴스 수 = ceil(예상 동시 연결 수 / 인스턴스당 최대 연결 수)

예시:
- 예상 동시 연결: 20,000
- 인스턴스당 최대: 5,000
- 필요 인스턴스 = ceil(20,000 / 5,000) = 4개
- 여유 확보: 4 * 1.5 = 6개 권장
```

## External Message Broker

### RabbitMQ STOMP Relay

대규모 환경에서는 외부 메시지 브로커를 사용할 수 있습니다.

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 외부 브로커 사용 (Redis 대신)
        registry.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost("rabbitmq.example.com")
                .setRelayPort(61613)
                .setClientLogin("guest")
                .setClientPasscode("guest")
                .setSystemLogin("system")
                .setSystemPasscode("system");

        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }
}
```

## Best Practices

1. **Redis Pub/Sub 활용** - 다중 인스턴스 메시지 라우팅
2. **Stateless 설계** - 세션 정보를 외부 저장소에 저장
3. **연결 수 모니터링** - 스케일링 결정 기준
4. **점진적 스케일링** - 급격한 확장 방지
5. **연결 분산 확인** - 인스턴스간 균등 분포
6. **장애 대비** - Redis 클러스터, 인스턴스 다중화

## 관련 문서

- [websocket-architecture.md](./websocket-architecture.md) - WebSocket 아키텍처
- [broadcast-pattern.md](./broadcast-pattern.md) - 브로드캐스트 패턴
- [session-management.md](./session-management.md) - 세션 관리
