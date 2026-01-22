# WebSocket Architecture

## 개요

WebSocket은 클라이언트와 서버 간의 양방향 실시간 통신을 제공하는 프로토콜입니다. Notification Service는 WebSocket을 통해 사용자에게 실시간으로 알림을 푸시합니다.

## HTTP vs WebSocket

```
┌─────────────────────────────────────────────────────────────────────┐
│                    HTTP (Request-Response)                          │
│                                                                      │
│  Client                                    Server                    │
│    │                                         │                       │
│    │────── GET /notifications ──────────────▶│                       │
│    │◀───── Response: [notifications] ────────│                       │
│    │                                         │                       │
│    │  (연결 종료, 새 알림 확인하려면 다시 요청)   │                       │
│    │                                         │                       │
│    │────── GET /notifications ──────────────▶│                       │
│    │◀───── Response: [notifications] ────────│                       │
│                                                                      │
├─────────────────────────────────────────────────────────────────────┤
│                    WebSocket (Full-Duplex)                          │
│                                                                      │
│  Client                                    Server                    │
│    │                                         │                       │
│    │══════ Connection Established ══════════│  (핸드셰이크)          │
│    │                                         │                       │
│    │◀────── New Notification ────────────────│  (서버 → 클라이언트)  │
│    │                                         │                       │
│    │◀────── New Notification ────────────────│                       │
│    │                                         │                       │
│    │─────── Mark as Read ───────────────────▶│  (클라이언트 → 서버)  │
│    │                                         │                       │
│    │  (연결 유지, 실시간 양방향 통신)            │                       │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

## Portal Universe WebSocket 아키텍처

### 전체 흐름

```
┌─────────────────────────────────────────────────────────────────────┐
│                 Notification Push Architecture                       │
│                                                                      │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────────┐  │
│  │   Kafka     │───▶│  Consumer   │───▶│ NotificationService     │  │
│  │   Topic     │    │             │    │ (Create & Save)         │  │
│  └─────────────┘    └─────────────┘    └───────────┬─────────────┘  │
│                                                    │                 │
│                                                    ▼                 │
│                                        ┌─────────────────────────┐  │
│                                        │ NotificationPushService │  │
│                                        └───────────┬─────────────┘  │
│                                                    │                 │
│                          ┌─────────────────────────┴─────────────┐  │
│                          │                                       │  │
│                          ▼                                       ▼  │
│               ┌─────────────────────┐            ┌──────────────────┐│
│               │   WebSocket Push    │            │  Redis Pub/Sub   ││
│               │ (Single Instance)   │            │ (Multi Instance) ││
│               └──────────┬──────────┘            └────────┬─────────┘│
│                          │                                │          │
│                          │     ┌──────────────────────────┘          │
│                          │     │                                     │
│                          ▼     ▼                                     │
│               ┌───────────────────────┐                              │
│               │   SimpMessagingTemplate│                             │
│               │   (Spring STOMP)      │                              │
│               └───────────┬───────────┘                              │
│                           │                                          │
│                           ▼                                          │
│               ┌───────────────────────┐                              │
│               │   WebSocket Session   │                              │
│               │   /user/queue/notifications                          │
│               └───────────┬───────────┘                              │
│                           │                                          │
│                           ▼                                          │
│               ┌───────────────────────┐                              │
│               │      Browser          │                              │
│               │   (SockJS Client)     │                              │
│               └───────────────────────┘                              │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### WebSocketConfig 분석

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 구독 경로: 클라이언트가 메시지를 받는 곳
        registry.enableSimpleBroker("/topic", "/queue");

        // 발행 경로: 클라이언트가 메시지를 보내는 곳
        registry.setApplicationDestinationPrefixes("/app");

        // 사용자별 구독 접두사
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 연결 엔드포인트
        registry.addEndpoint("/ws/notifications")
                .setAllowedOriginPatterns("*")  // CORS 설정
                .withSockJS();                   // SockJS 폴백 지원
    }
}
```

## 메시지 경로 구조

### 경로 접두사

| 접두사 | 용도 | 예시 |
|--------|------|------|
| `/app` | 클라이언트 → 서버 메시지 | `/app/notifications/read` |
| `/topic` | 브로드캐스트 구독 | `/topic/notifications` |
| `/queue` | 개인 메시지 구독 | `/queue/notifications` |
| `/user` | 사용자별 개인 구독 | `/user/queue/notifications` |

### 메시지 라우팅

```
클라이언트 발행                      서버 처리
─────────────                      ─────────────
/app/message     ──────▶  @MessageMapping("/message")

서버 발행                           클라이언트 수신
─────────────                      ─────────────
/topic/broadcast ──────▶  모든 구독자에게 전달
/user/{id}/queue/notifications ──▶ 특정 사용자에게 전달
```

## NotificationPushService 구현

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper redisObjectMapper;

    public void push(Notification notification) {
        NotificationResponse response = NotificationResponse.from(notification);

        // 1. 직접 WebSocket 전송 (같은 인스턴스의 사용자)
        messagingTemplate.convertAndSendToUser(
                notification.getUserId().toString(),
                "/queue/notifications",
                response
        );

        // 2. Redis Pub/Sub으로 발행 (다른 인스턴스의 사용자)
        String channel = "notification:" + notification.getUserId();
        try {
            String jsonPayload = redisObjectMapper.writeValueAsString(response);
            redisTemplate.convertAndSend(channel, jsonPayload);
            log.debug("Published notification to Redis channel: {}", channel);
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

## 클라이언트 연결

### JavaScript 클라이언트

```javascript
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

class NotificationClient {
    constructor(userId) {
        this.userId = userId;
        this.stompClient = null;
    }

    connect() {
        const socket = new SockJS('/ws/notifications');
        this.stompClient = Stomp.over(socket);

        this.stompClient.connect({}, (frame) => {
            console.log('Connected:', frame);

            // 개인 알림 구독
            this.stompClient.subscribe(
                '/user/queue/notifications',
                (message) => this.handleNotification(JSON.parse(message.body))
            );

            // 전체 공지 구독
            this.stompClient.subscribe(
                '/topic/notifications',
                (message) => this.handleBroadcast(JSON.parse(message.body))
            );
        });
    }

    handleNotification(notification) {
        console.log('New notification:', notification);
        // UI 업데이트
    }

    handleBroadcast(notification) {
        console.log('Broadcast notification:', notification);
        // UI 업데이트
    }

    disconnect() {
        if (this.stompClient) {
            this.stompClient.disconnect();
        }
    }
}
```

## WebSocket 핸드셰이크

```
1. HTTP Upgrade 요청
GET /ws/notifications HTTP/1.1
Host: notification-service:8084
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
Sec-WebSocket-Version: 13

2. 서버 응답
HTTP/1.1 101 Switching Protocols
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=

3. WebSocket 연결 수립
[WebSocket 프레임 교환 시작]
```

## SockJS 폴백

SockJS는 WebSocket을 지원하지 않는 환경에서 대체 프로토콜을 제공합니다.

```
우선순위:
1. WebSocket (ws://)
2. XHR Streaming
3. XHR Polling
4. EventSource (SSE)
5. iframe 기반 기술
```

```java
// SockJS 활성화
registry.addEndpoint("/ws/notifications")
        .withSockJS();  // SockJS 폴백 지원
```

## 보안 고려사항

### 인증 처리

```java
@Configuration
public class WebSocketSecurityConfig {

    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager() {
        MessageMatcherDelegatingAuthorizationManager.Builder messages =
            MessageMatcherDelegatingAuthorizationManager.builder();

        messages
            .nullDestMatcher().authenticated()
            .simpDestMatchers("/user/**").authenticated()
            .simpDestMatchers("/topic/**").authenticated()
            .anyMessage().denyAll();

        return messages.build();
    }
}
```

### CORS 설정

```java
registry.addEndpoint("/ws/notifications")
        .setAllowedOriginPatterns("https://*.portal-universe.com")
        .withSockJS();
```

## 성능 특성

| 특성 | HTTP Polling | WebSocket |
|------|-------------|-----------|
| 연결 오버헤드 | 요청마다 발생 | 최초 1회 |
| 지연 시간 | 폴링 간격 의존 | 실시간 (밀리초) |
| 서버 리소스 | 요청마다 처리 | 연결 유지 비용 |
| 대역폭 | 헤더 반복 전송 | 프레임 헤더만 |
| 양방향 통신 | 불가 | 가능 |

## 모니터링 포인트

```java
// 연결 수 모니터링
@Component
@RequiredArgsConstructor
public class WebSocketMetrics {

    private final SimpUserRegistry userRegistry;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedRate = 30000)
    public void recordMetrics() {
        int connectedUsers = userRegistry.getUserCount();
        meterRegistry.gauge("websocket.connected.users", connectedUsers);
    }
}
```

## 관련 문서

- [stomp-integration.md](./stomp-integration.md) - STOMP 프로토콜
- [session-management.md](./session-management.md) - 세션 관리
- [broadcast-pattern.md](./broadcast-pattern.md) - 브로드캐스트 패턴
- [scaling-websocket.md](./scaling-websocket.md) - WebSocket 스케일링
