# Session Management

## 개요

WebSocket 세션 관리는 클라이언트 연결 상태를 추적하고, 사용자별로 메시지를 라우팅하는 핵심 기능입니다. Spring WebSocket은 `SimpUserRegistry`를 통해 세션 정보를 관리합니다.

## 세션 라이프사이클

```
┌─────────────────────────────────────────────────────────────────────┐
│                    WebSocket Session Lifecycle                       │
│                                                                      │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐          │
│  │ CONNECT │───▶│CONNECTED│───▶│ ACTIVE  │───▶│DISCONNECT│          │
│  └─────────┘    └─────────┘    └─────────┘    └─────────┘          │
│       │              │              │              │                 │
│       ▼              ▼              ▼              ▼                 │
│  핸드셰이크    세션 생성      메시지 송수신   세션 정리             │
│  인증 처리    구독 시작      구독 관리                              │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

## 세션 이벤트 리스너

### ApplicationListener 구현

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final SimpUserRegistry userRegistry;
    private final NotificationService notificationService;

    // 연결 수립 이벤트
    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headers.getSessionId();
        Principal user = headers.getUser();

        log.info("WebSocket connected: sessionId={}, user={}",
                sessionId, user != null ? user.getName() : "anonymous");
    }

    // 구독 이벤트
    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headers.getSessionId();
        String destination = headers.getDestination();

        log.debug("WebSocket subscribe: sessionId={}, destination={}",
                sessionId, destination);
    }

    // 연결 종료 이벤트
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headers.getSessionId();
        Principal user = headers.getUser();

        log.info("WebSocket disconnected: sessionId={}, user={}",
                sessionId, user != null ? user.getName() : "anonymous");

        // 사용자 상태 업데이트
        if (user != null) {
            updateUserOnlineStatus(user.getName(), false);
        }
    }
}
```

## SimpUserRegistry

### 연결된 사용자 조회

```java
@Service
@RequiredArgsConstructor
public class WebSocketUserService {

    private final SimpUserRegistry userRegistry;

    // 전체 연결 사용자 수
    public int getConnectedUserCount() {
        return userRegistry.getUserCount();
    }

    // 특정 사용자 연결 여부 확인
    public boolean isUserConnected(String userId) {
        SimpUser user = userRegistry.getUser(userId);
        return user != null && !user.getSessions().isEmpty();
    }

    // 특정 사용자의 세션 목록
    public Set<SimpSession> getUserSessions(String userId) {
        SimpUser user = userRegistry.getUser(userId);
        return user != null ? user.getSessions() : Collections.emptySet();
    }

    // 모든 연결 사용자 목록
    public List<String> getConnectedUserIds() {
        return userRegistry.getUsers().stream()
                .map(SimpUser::getName)
                .collect(Collectors.toList());
    }

    // 특정 사용자의 구독 정보
    public Set<String> getUserSubscriptions(String userId) {
        SimpUser user = userRegistry.getUser(userId);
        if (user == null) {
            return Collections.emptySet();
        }

        return user.getSessions().stream()
                .flatMap(session -> session.getSubscriptions().stream())
                .map(SimpSubscription::getDestination)
                .collect(Collectors.toSet());
    }
}
```

## 세션 속성 관리

### 연결 시 속성 설정

```java
@Configuration
public class WebSocketSessionConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // 인증 토큰에서 사용자 정보 추출
                    String token = accessor.getFirstNativeHeader("Authorization");
                    UserInfo userInfo = authenticateAndGetUserInfo(token);

                    // 세션 속성에 사용자 정보 저장
                    accessor.getSessionAttributes().put("userId", userInfo.getId());
                    accessor.getSessionAttributes().put("username", userInfo.getName());
                    accessor.getSessionAttributes().put("roles", userInfo.getRoles());

                    // Principal 설정
                    accessor.setUser(new StompPrincipal(userInfo.getId().toString()));
                }

                return message;
            }
        });
    }
}
```

### 세션 속성 조회

```java
@MessageMapping("/notifications/read")
public void markAsRead(
        SimpMessageHeaderAccessor headerAccessor,
        @Payload ReadRequest request) {

    // 세션 속성에서 사용자 정보 조회
    Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
    String username = (String) headerAccessor.getSessionAttributes().get("username");

    log.info("User {} marking notification as read", username);
    notificationService.markAsRead(request.getNotificationId(), userId);
}
```

## 사용자-세션 매핑

### 다중 세션 지원

한 사용자가 여러 기기에서 접속할 수 있습니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│                    User-Session Mapping                              │
│                                                                      │
│  User: user123                                                       │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │ Sessions:                                                    │    │
│  │   ├── session-abc (Desktop Chrome)                          │    │
│  │   │   └── Subscriptions:                                    │    │
│  │   │       ├── /user/queue/notifications                     │    │
│  │   │       └── /topic/announcements                          │    │
│  │   │                                                         │    │
│  │   └── session-xyz (Mobile App)                              │    │
│  │       └── Subscriptions:                                    │    │
│  │           └── /user/queue/notifications                     │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  메시지 전송 시: 모든 세션에 전달                                     │
│  messagingTemplate.convertAndSendToUser("user123", ...)             │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 세션별 전송

```java
@Service
@RequiredArgsConstructor
public class SessionSpecificNotificationService {

    private final SimpUserRegistry userRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    // 특정 세션에만 전송
    public void sendToSession(String sessionId, NotificationResponse notification) {
        messagingTemplate.convertAndSend(
                "/queue/notifications-" + sessionId,
                notification
        );
    }

    // 특정 사용자의 모든 세션에 전송
    public void sendToAllUserSessions(String userId, NotificationResponse notification) {
        messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/notifications",
                notification
        );
    }

    // 특정 조건의 세션에만 전송
    public void sendToMobileSessions(String userId, NotificationResponse notification) {
        SimpUser user = userRegistry.getUser(userId);
        if (user == null) return;

        user.getSessions().stream()
                .filter(session -> isMobileSession(session))
                .forEach(session -> {
                    // 세션별 전송 로직
                });
    }
}
```

## 세션 타임아웃 관리

### 서버 측 타임아웃 설정

```java
@Configuration
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry
                .setMessageSizeLimit(128 * 1024)     // 메시지 크기 제한 (128KB)
                .setSendBufferSizeLimit(512 * 1024)  // 버퍼 크기 제한 (512KB)
                .setSendTimeLimit(20000);            // 전송 타임아웃 (20초)
    }
}
```

### SockJS 타임아웃 설정

```java
@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws/notifications")
            .setAllowedOriginPatterns("*")
            .withSockJS()
            .setHeartbeatTime(25000)       // 하트비트 간격 (25초)
            .setDisconnectDelay(5000)      // 연결 끊김 감지 지연 (5초)
            .setSessionCookieNeeded(false);
}
```

## 세션 상태 모니터링

### 메트릭 수집

```java
@Component
@RequiredArgsConstructor
public class WebSocketSessionMetrics {

    private final SimpUserRegistry userRegistry;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedRate = 30000)
    public void recordMetrics() {
        // 연결 사용자 수
        meterRegistry.gauge("websocket.sessions.total",
                userRegistry.getUserCount());

        // 구독 수
        long totalSubscriptions = userRegistry.getUsers().stream()
                .flatMap(user -> user.getSessions().stream())
                .mapToLong(session -> session.getSubscriptions().size())
                .sum();

        meterRegistry.gauge("websocket.subscriptions.total", totalSubscriptions);
    }
}
```

### 세션 상태 API

```java
@RestController
@RequestMapping("/admin/websocket")
@RequiredArgsConstructor
public class WebSocketAdminController {

    private final SimpUserRegistry userRegistry;

    @GetMapping("/sessions")
    public Map<String, Object> getSessionInfo() {
        Map<String, Object> info = new HashMap<>();

        info.put("totalUsers", userRegistry.getUserCount());

        List<Map<String, Object>> users = userRegistry.getUsers().stream()
                .map(user -> {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("userId", user.getName());
                    userInfo.put("sessionCount", user.getSessions().size());
                    userInfo.put("sessions", user.getSessions().stream()
                            .map(session -> Map.of(
                                    "sessionId", session.getId(),
                                    "subscriptions", session.getSubscriptions().stream()
                                            .map(SimpSubscription::getDestination)
                                            .collect(Collectors.toList())
                            ))
                            .collect(Collectors.toList()));
                    return userInfo;
                })
                .collect(Collectors.toList());

        info.put("users", users);

        return info;
    }
}
```

## 세션 정리

### 비정상 종료 처리

```java
@Component
@Slf4j
public class WebSocketSessionCleaner {

    @Scheduled(fixedRate = 60000)  // 1분마다
    public void cleanupStaleSessions() {
        // 비정상 종료된 세션 정리 로직
        // (대부분 Spring이 자동 처리하지만, 추가 정리가 필요한 경우)
    }
}
```

### 명시적 세션 종료

```java
@Service
@RequiredArgsConstructor
public class WebSocketSessionService {

    private final SimpUserRegistry userRegistry;
    private final WebSocketHandler webSocketHandler;

    // 특정 사용자의 모든 세션 강제 종료
    public void forceDisconnectUser(String userId) {
        SimpUser user = userRegistry.getUser(userId);
        if (user != null) {
            user.getSessions().forEach(session -> {
                // 세션 종료 로직
                log.info("Force disconnecting session: {}", session.getId());
            });
        }
    }
}
```

## Best Practices

1. **세션 속성 최소화** - 필요한 정보만 저장
2. **이벤트 리스너 활용** - 연결/해제 시점 추적
3. **다중 세션 고려** - 사용자당 여러 세션 가능
4. **타임아웃 적절히 설정** - 리소스 관리
5. **모니터링 구현** - 연결 상태 추적
6. **정리 로직 구현** - 비정상 종료 대응

## 관련 문서

- [websocket-architecture.md](./websocket-architecture.md) - WebSocket 아키텍처
- [stomp-integration.md](./stomp-integration.md) - STOMP 프로토콜
- [reconnection-strategy.md](./reconnection-strategy.md) - 재연결 전략
