# STOMP Integration

## 개요

STOMP(Simple Text Oriented Messaging Protocol)는 WebSocket 위에서 동작하는 메시징 프로토콜입니다. Spring WebSocket은 STOMP를 기본 지원하며, 메시지 라우팅과 구독 관리를 간편하게 처리합니다.

## STOMP 프로토콜

### 기본 구조

STOMP 메시지는 텍스트 기반의 프레임으로 구성됩니다.

```
COMMAND
header1:value1
header2:value2

Body content^@
```

### 프레임 유형

| Command | 방향 | 설명 |
|---------|------|------|
| `CONNECT` | Client → Server | 연결 수립 |
| `CONNECTED` | Server → Client | 연결 확인 |
| `SUBSCRIBE` | Client → Server | 구독 시작 |
| `UNSUBSCRIBE` | Client → Server | 구독 해제 |
| `SEND` | Client → Server | 메시지 전송 |
| `MESSAGE` | Server → Client | 메시지 수신 |
| `DISCONNECT` | Client → Server | 연결 종료 |
| `ERROR` | Server → Client | 오류 알림 |

### 프레임 예시

```
# CONNECT (클라이언트 → 서버)
CONNECT
accept-version:1.1,1.0
heart-beat:10000,10000

^@

# CONNECTED (서버 → 클라이언트)
CONNECTED
version:1.1
heart-beat:0,0

^@

# SUBSCRIBE (클라이언트 → 서버)
SUBSCRIBE
id:sub-0
destination:/user/queue/notifications

^@

# MESSAGE (서버 → 클라이언트)
MESSAGE
destination:/user/queue/notifications
content-type:application/json

{"id":1,"title":"새 알림","message":"주문이 완료되었습니다"}^@
```

## Spring STOMP 설정

### WebSocketMessageBrokerConfigurer

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Simple In-Memory Broker 활성화
        // /topic: 1:N 브로드캐스트
        // /queue: 1:1 메시지
        registry.enableSimpleBroker("/topic", "/queue");

        // 클라이언트 → 서버 메시지 접두사
        registry.setApplicationDestinationPrefixes("/app");

        // 사용자별 목적지 접두사
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/notifications")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

## 메시지 브로커

### Simple Broker (In-Memory)

```java
// 기본 제공되는 인메모리 브로커
registry.enableSimpleBroker("/topic", "/queue");
```

**특징:**
- 단일 서버 환경에 적합
- 설정이 간단
- 서버 재시작 시 구독 정보 유실

### External Broker (RabbitMQ/ActiveMQ)

```java
// 외부 메시지 브로커 연동
registry.enableStompBrokerRelay("/topic", "/queue")
        .setRelayHost("rabbitmq.example.com")
        .setRelayPort(61613)
        .setClientLogin("guest")
        .setClientPasscode("guest");
```

**특징:**
- 다중 서버 환경 지원
- 메시지 영속성
- 고가용성 구성 가능

## 메시지 핸들링

### @MessageMapping

클라이언트에서 `/app/*`로 전송한 메시지를 처리합니다.

```java
@Controller
public class NotificationWebSocketController {

    private final NotificationService notificationService;

    // /app/notifications/read 로 전송된 메시지 처리
    @MessageMapping("/notifications/read")
    public void markAsRead(
            @Payload ReadRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        String sessionId = headerAccessor.getSessionId();
        // 처리 로직
    }

    // 응답을 특정 목적지로 전송
    @MessageMapping("/notifications/count")
    @SendTo("/topic/count")
    public Long getUnreadCount(@Payload CountRequest request) {
        return notificationService.getUnreadCount(request.getUserId());
    }
}
```

### @SubscribeMapping

구독 시점에 초기 데이터를 전송합니다.

```java
@Controller
public class NotificationSubscriptionController {

    // 클라이언트가 /app/notifications/init 구독 시 호출
    @SubscribeMapping("/notifications/init")
    public List<NotificationResponse> handleSubscription() {
        // 구독 시점에 읽지 않은 알림 목록 전송
        return notificationService.getRecentUnread();
    }
}
```

## SimpMessagingTemplate

프로그래밍 방식으로 메시지를 전송합니다.

### 기본 사용법

```java
@Service
@RequiredArgsConstructor
public class NotificationPushService {

    private final SimpMessagingTemplate messagingTemplate;

    // 특정 사용자에게 전송
    public void sendToUser(Long userId, NotificationResponse notification) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),          // 사용자 ID
                "/queue/notifications",     // 목적지
                notification                // 페이로드
        );
        // 실제 전송 경로: /user/{userId}/queue/notifications
    }

    // 전체 브로드캐스트
    public void broadcast(NotificationResponse notification) {
        messagingTemplate.convertAndSend(
                "/topic/notifications",
                notification
        );
    }

    // 헤더 포함 전송
    public void sendWithHeaders(Long userId, NotificationResponse notification) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("type", notification.getType().name());
        headers.put("priority", "high");

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                notification,
                headers
        );
    }
}
```

### 메시지 변환

```java
// JSON 직렬화 자동 처리
messagingTemplate.convertAndSend("/topic/notifications", notification);
// NotificationResponse → JSON 자동 변환

// 명시적 변환
messagingTemplate.send("/topic/notifications",
        MessageBuilder.withPayload(objectMapper.writeValueAsString(notification))
                      .build());
```

## 사용자 식별

### Principal 기반

```java
@Configuration
public class WebSocketAuthConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // 토큰에서 사용자 정보 추출
                    String token = accessor.getFirstNativeHeader("Authorization");
                    Principal user = authenticateUser(token);
                    accessor.setUser(user);
                }

                return message;
            }
        });
    }
}
```

### 세션 속성 활용

```java
@MessageMapping("/notifications/read")
public void markAsRead(
        SimpMessageHeaderAccessor headerAccessor,
        @Payload ReadRequest request) {

    // 세션 속성에서 사용자 ID 조회
    Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");

    notificationService.markAsRead(request.getNotificationId(), userId);
}
```

## 클라이언트 구현

### STOMP.js 사용

```javascript
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const client = new Client({
    // SockJS 사용
    webSocketFactory: () => new SockJS('/ws/notifications'),

    // 재연결 설정
    reconnectDelay: 5000,

    // 하트비트 설정
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,

    // 연결 시 콜백
    onConnect: (frame) => {
        console.log('Connected:', frame);

        // 개인 알림 구독
        client.subscribe('/user/queue/notifications', (message) => {
            const notification = JSON.parse(message.body);
            handleNotification(notification);
        });

        // 브로드캐스트 구독
        client.subscribe('/topic/notifications', (message) => {
            const notification = JSON.parse(message.body);
            handleBroadcast(notification);
        });
    },

    // 에러 콜백
    onStompError: (frame) => {
        console.error('STOMP error:', frame.headers['message']);
    }
});

// 연결 시작
client.activate();

// 메시지 전송
function sendReadConfirmation(notificationId) {
    client.publish({
        destination: '/app/notifications/read',
        body: JSON.stringify({ notificationId }),
        headers: { 'content-type': 'application/json' }
    });
}

// 연결 종료
function disconnect() {
    client.deactivate();
}
```

## 하트비트 메커니즘

```
┌─────────────────────────────────────────────────────────────────────┐
│                      Heartbeat Mechanism                             │
│                                                                      │
│  Client                                    Server                    │
│    │                                         │                       │
│    │──── CONNECT heart-beat:10000,10000 ────▶│                       │
│    │◀─── CONNECTED heart-beat:10000,10000 ───│                       │
│    │                                         │                       │
│    │  10초마다 하트비트 교환                   │                       │
│    │                                         │                       │
│    │─────────── ♥ (ping) ───────────────────▶│                       │
│    │◀────────── ♥ (pong) ────────────────────│                       │
│    │                                         │                       │
│    │  하트비트 미수신 시 연결 끊김으로 간주     │                       │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

## 에러 처리

### 서버 측 예외 처리

```java
@ControllerAdvice
public class WebSocketExceptionHandler {

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public ErrorResponse handleException(Exception ex) {
        return new ErrorResponse("PROCESSING_ERROR", ex.getMessage());
    }

    @MessageExceptionHandler(AuthenticationException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleAuthException(AuthenticationException ex) {
        return new ErrorResponse("AUTH_ERROR", "인증이 필요합니다.");
    }
}
```

### 클라이언트 측 에러 처리

```javascript
client.subscribe('/user/queue/errors', (message) => {
    const error = JSON.parse(message.body);
    console.error('WebSocket error:', error);
    showErrorToast(error.message);
});
```

## Best Practices

1. **목적지 명명 규칙** - `/topic`(브로드캐스트), `/queue`(개인)
2. **하트비트 설정** - 연결 상태 감지
3. **인증 처리** - CONNECT 프레임에서 처리
4. **에러 핸들링** - 전용 에러 큐 사용
5. **메시지 크기 제한** - 대용량 데이터는 REST API 활용
6. **재연결 로직** - 클라이언트에서 자동 재연결

## 관련 문서

- [websocket-architecture.md](./websocket-architecture.md) - WebSocket 아키텍처
- [session-management.md](./session-management.md) - 세션 관리
- [broadcast-pattern.md](./broadcast-pattern.md) - 브로드캐스트 패턴
