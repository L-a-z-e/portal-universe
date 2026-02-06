# Reconnection Strategy

## 개요

WebSocket 연결은 네트워크 불안정, 서버 재시작, 클라이언트 이동 등으로 인해 끊어질 수 있습니다. 안정적인 실시간 서비스를 위해서는 자동 재연결 전략이 필수입니다.

## 연결 끊김 원인

```
┌─────────────────────────────────────────────────────────────────────┐
│                 Connection Drop Causes                               │
│                                                                      │
│  Client Side                   │  Server Side                        │
│  ────────────────────          │  ────────────────────               │
│  • 네트워크 변경 (WiFi→LTE)      │  • 서버 재시작/배포                   │
│  • 일시적 네트워크 단절           │  • 로드밸런서 세션 타임아웃           │
│  • 브라우저 탭 백그라운드 전환    │  • 리소스 부족으로 연결 종료          │
│  • 디바이스 절전 모드            │  • 하트비트 타임아웃                  │
│  • 프록시/방화벽 타임아웃         │  • 스케일 인 (인스턴스 축소)          │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

## 클라이언트 재연결 전략

### Exponential Backoff

연결 실패 시 재시도 간격을 지수적으로 증가시킵니다.

```javascript
class ReconnectableWebSocket {
    constructor(url) {
        this.url = url;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 10;
        this.baseDelay = 1000;        // 1초
        this.maxDelay = 30000;        // 30초
        this.connect();
    }

    connect() {
        this.socket = new SockJS(this.url);
        this.stompClient = Stomp.over(this.socket);

        this.stompClient.connect({},
            (frame) => this.onConnected(frame),
            (error) => this.onError(error)
        );
    }

    onConnected(frame) {
        console.log('Connected:', frame);
        this.reconnectAttempts = 0;  // 성공시 리셋
        this.setupSubscriptions();
    }

    onError(error) {
        console.error('Connection error:', error);
        this.scheduleReconnect();
    }

    scheduleReconnect() {
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.error('Max reconnect attempts reached');
            this.onMaxRetriesExceeded();
            return;
        }

        // Exponential backoff with jitter
        const delay = Math.min(
            this.baseDelay * Math.pow(2, this.reconnectAttempts)
                + Math.random() * 1000,  // jitter
            this.maxDelay
        );

        this.reconnectAttempts++;
        console.log(`Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts})`);

        setTimeout(() => this.connect(), delay);
    }

    onMaxRetriesExceeded() {
        // 사용자에게 수동 재연결 옵션 제공
        showReconnectButton();
    }
}
```

### STOMP.js 내장 재연결

```javascript
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const client = new Client({
    webSocketFactory: () => new SockJS('/ws/notifications'),

    // 재연결 설정
    reconnectDelay: 5000,          // 5초 대기 후 재연결
    heartbeatIncoming: 10000,      // 서버 하트비트 타임아웃
    heartbeatOutgoing: 10000,      // 클라이언트 하트비트 간격

    // 연결 콜백
    onConnect: (frame) => {
        console.log('Connected');
        setupSubscriptions();
    },

    // 에러 콜백
    onStompError: (frame) => {
        console.error('STOMP error:', frame.headers['message']);
    },

    // 연결 끊김 콜백
    onWebSocketClose: (event) => {
        console.log('WebSocket closed:', event.code, event.reason);
        // reconnectDelay 후 자동 재연결 시도
    },

    // 디버그 로깅 (개발 환경)
    debug: (str) => {
        if (process.env.NODE_ENV === 'development') {
            console.log(str);
        }
    }
});

client.activate();
```

## 재연결 시 상태 복구

### 누락 메시지 처리

```javascript
class NotificationClient {
    constructor() {
        this.lastReceivedId = null;
        this.pendingSync = false;
    }

    onConnected() {
        // 재연결 시 누락 메시지 동기화
        if (this.lastReceivedId) {
            this.syncMissedNotifications();
        }
        this.setupSubscriptions();
    }

    async syncMissedNotifications() {
        try {
            // REST API로 누락 알림 조회
            const response = await fetch(
                `/api/notifications?since=${this.lastReceivedId}`
            );
            const notifications = await response.json();

            notifications.forEach(notification => {
                this.handleNotification(notification);
            });

            console.log(`Synced ${notifications.length} missed notifications`);
        } catch (error) {
            console.error('Failed to sync notifications:', error);
        }
    }

    handleNotification(notification) {
        this.lastReceivedId = notification.id;
        // UI 업데이트
        displayNotification(notification);
    }
}
```

### 구독 재설정

```javascript
class SubscriptionManager {
    constructor(stompClient) {
        this.client = stompClient;
        this.subscriptions = new Map();
    }

    subscribe(destination, callback) {
        const subscription = this.client.subscribe(destination, callback);
        this.subscriptions.set(destination, { callback, subscription });
        return subscription;
    }

    // 재연결 후 모든 구독 복원
    restoreSubscriptions() {
        this.subscriptions.forEach((sub, destination) => {
            if (sub.subscription) {
                sub.subscription.unsubscribe();
            }
            sub.subscription = this.client.subscribe(destination, sub.callback);
        });
        console.log(`Restored ${this.subscriptions.size} subscriptions`);
    }
}
```

## 서버 측 지원

### 연결 상태 추적

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class ConnectionStateTracker {

    private final ConcurrentHashMap<String, ConnectionInfo> connectionStates =
            new ConcurrentHashMap<>();

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String userId = headers.getUser().getName();
        String sessionId = headers.getSessionId();

        connectionStates.put(userId, new ConnectionInfo(
                sessionId,
                LocalDateTime.now(),
                ConnectionStatus.CONNECTED
        ));

        log.info("User connected: userId={}, sessionId={}", userId, sessionId);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String userId = headers.getUser() != null ? headers.getUser().getName() : null;

        if (userId != null) {
            ConnectionInfo info = connectionStates.get(userId);
            if (info != null) {
                info.setStatus(ConnectionStatus.DISCONNECTED);
                info.setDisconnectedAt(LocalDateTime.now());
            }
        }

        log.info("User disconnected: userId={}", userId);
    }

    public boolean isConnected(String userId) {
        ConnectionInfo info = connectionStates.get(userId);
        return info != null && info.getStatus() == ConnectionStatus.CONNECTED;
    }

    @Data
    @AllArgsConstructor
    private static class ConnectionInfo {
        private String sessionId;
        private LocalDateTime connectedAt;
        private ConnectionStatus status;
        private LocalDateTime disconnectedAt;
    }

    private enum ConnectionStatus {
        CONNECTED, DISCONNECTED
    }
}
```

### 누락 알림 API

```java
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationSyncController {

    private final NotificationRepository notificationRepository;

    // 특정 ID 이후의 알림 조회
    @GetMapping(params = "since")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotificationsSince(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("since") Long sinceId) {

        List<Notification> notifications = notificationRepository
                .findByUserIdAndIdGreaterThanOrderByIdAsc(userId, sinceId);

        List<NotificationResponse> responses = notifications.stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    // 마지막 연결 이후 알림 조회
    @GetMapping(params = "sinceTime")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotificationsSinceTime(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("sinceTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                LocalDateTime sinceTime) {

        List<Notification> notifications = notificationRepository
                .findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(userId, sinceTime);

        List<NotificationResponse> responses = notifications.stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
```

## 재연결 타임라인

```
┌─────────────────────────────────────────────────────────────────────┐
│                  Reconnection Timeline                               │
│                                                                      │
│  Time: 0s    1s    3s    7s    15s   31s        ...                 │
│        │     │     │     │     │     │                              │
│        ▼     ▼     ▼     ▼     ▼     ▼                              │
│     연결끊김 시도1 시도2 시도3 시도4 시도5                             │
│        │     X     X     X     X     ✓ (성공)                        │
│        │                             │                              │
│        │                             ▼                              │
│        │                        구독 복원                            │
│        │                             │                              │
│        │                             ▼                              │
│        │                        누락 메시지 동기화                    │
│        │                             │                              │
│        └─────────────────────────────┴─── 정상 운영 ──────────▶      │
│                                                                      │
│  Delay: 1s    2s    4s    8s    16s  (Exponential Backoff)          │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

## 네트워크 상태 감지

### Online/Offline 이벤트

```javascript
class NetworkAwareClient {
    constructor() {
        this.isOnline = navigator.onLine;
        this.setupNetworkListeners();
    }

    setupNetworkListeners() {
        window.addEventListener('online', () => {
            console.log('Network online');
            this.isOnline = true;
            this.attemptReconnect();
        });

        window.addEventListener('offline', () => {
            console.log('Network offline');
            this.isOnline = false;
            // 재연결 시도 중단
            clearTimeout(this.reconnectTimer);
        });
    }

    attemptReconnect() {
        if (this.isOnline && !this.isConnected()) {
            this.connect();
        }
    }
}
```

### Visibility API

```javascript
class VisibilityAwareClient {
    constructor() {
        this.setupVisibilityListener();
    }

    setupVisibilityListener() {
        document.addEventListener('visibilitychange', () => {
            if (document.visibilityState === 'visible') {
                console.log('Tab visible');
                // 탭이 활성화되면 연결 상태 확인
                this.checkConnectionHealth();
            } else {
                console.log('Tab hidden');
                // 배경 탭에서는 하트비트 간격 늘리기 가능
            }
        });
    }

    checkConnectionHealth() {
        if (!this.isConnected()) {
            this.connect();
        } else {
            // 연결 상태 검증을 위한 ping
            this.sendPing();
        }
    }
}
```

## Best Practices

1. **Exponential Backoff** - 서버 과부하 방지
2. **Jitter 추가** - Thundering Herd 방지
3. **최대 재시도 제한** - 무한 재시도 방지
4. **상태 복구** - 재연결 후 누락 데이터 동기화
5. **네트워크 상태 감지** - 오프라인 시 재시도 중단
6. **사용자 피드백** - 연결 상태 UI 표시
7. **수동 재연결 옵션** - 최대 재시도 초과 시 제공

## 관련 문서

- [websocket-architecture.md](./websocket-architecture.md) - WebSocket 아키텍처
- [session-management.md](./session-management.md) - 세션 관리
- [scaling-websocket.md](./scaling-websocket.md) - WebSocket 스케일링
