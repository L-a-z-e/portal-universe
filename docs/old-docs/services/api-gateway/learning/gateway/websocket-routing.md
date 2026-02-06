# WebSocket Routing

API Gateway를 통해 WebSocket 연결을 프록시하는 방법을 학습합니다.

## 개요

Spring Cloud Gateway는 WebSocket 프록시를 지원합니다.

```
Client                  API Gateway                 Backend
  │                          │                          │
  │  WebSocket Upgrade       │                          │
  ├─────────────────────────>│  WebSocket Upgrade       │
  │                          ├─────────────────────────>│
  │                          │<─────────────────────────│
  │<─────────────────────────│  101 Switching Protocols │
  │                          │                          │
  │  Message Frame           │                          │
  ├─────────────────────────>│─────────────────────────>│
  │<─────────────────────────│<─────────────────────────│
  │                          │                          │
```

## WebSocket Route 설정

### 기본 설정

```yaml
spring:
  cloud:
    gateway:
      routes:
        # WebSocket Route
        - id: notification-websocket
          uri: ws://notification-service:8084     # ws:// 또는 wss://
          predicates:
            - Path=/ws/notifications/**
          filters:
            - StripPrefix=1

        # STOMP over WebSocket (SockJS Fallback 포함)
        - id: chat-websocket
          uri: ws://chat-service:8085
          predicates:
            - Path=/ws/chat/**
          filters:
            - StripPrefix=1
```

### 환경별 WebSocket URL

```yaml
# application-local.yml
services:
  notification:
    url: "http://localhost:8084"
    ws-url: "ws://localhost:8084"

# application-kubernetes.yml
services:
  notification:
    url: "http://notification-service"
    ws-url: "ws://notification-service"
```

### Load Balancer와 함께 사용

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: notification-websocket
          uri: lb:ws://notification-service       # lb: prefix로 로드밸런싱
          predicates:
            - Path=/ws/notifications/**
```

## WebSocket 인증

### JWT 토큰 전달

WebSocket은 커스텀 헤더를 지원하지 않으므로, 쿼리 파라미터나 첫 메시지로 토큰을 전달합니다.

```java
package com.portal.universe.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class WebSocketAuthFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // WebSocket 경로인지 확인
        if (!path.startsWith("/ws/")) {
            return chain.filter(exchange);
        }

        // Upgrade 헤더 확인
        String upgrade = request.getHeaders().getFirst(HttpHeaders.UPGRADE);
        if (!"websocket".equalsIgnoreCase(upgrade)) {
            return chain.filter(exchange);
        }

        // 쿼리 파라미터에서 토큰 추출
        String token = request.getQueryParams().getFirst("token");

        if (token == null || token.isEmpty()) {
            log.warn("WebSocket connection without token: {}", path);
            exchange.getResponse().setRawStatusCode(401);
            return exchange.getResponse().setComplete();
        }

        // 토큰 검증 후 사용자 정보 헤더 추가
        try {
            String userId = validateTokenAndGetUserId(token);

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .build();

            return chain.filter(exchange.mutate()
                    .request(mutatedRequest)
                    .build());

        } catch (Exception e) {
            log.error("WebSocket auth failed: {}", e.getMessage());
            exchange.getResponse().setRawStatusCode(401);
            return exchange.getResponse().setComplete();
        }
    }

    private String validateTokenAndGetUserId(String token) {
        // JWT 검증 로직
        // JwtAuthenticationFilter의 validateToken 메서드 재사용 가능
        return "user-id";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }
}
```

### 클라이언트 연결 예시

```javascript
// JavaScript 클라이언트
const token = localStorage.getItem('accessToken');
const ws = new WebSocket(`wss://api.portal-universe.com/ws/notifications?token=${token}`);

ws.onopen = () => {
    console.log('WebSocket connected');
};

ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    console.log('Received:', data);
};

ws.onerror = (error) => {
    console.error('WebSocket error:', error);
};

ws.onclose = (event) => {
    if (event.code === 1001) {
        // 정상 종료
    } else if (event.code === 4001) {
        // 인증 실패
        console.log('Authentication failed');
    }
};
```

## STOMP over WebSocket

### SockJS Fallback 지원

```yaml
spring:
  cloud:
    gateway:
      routes:
        # STOMP WebSocket
        - id: stomp-websocket
          uri: ws://chat-service:8085
          predicates:
            - Path=/ws/stomp/**

        # SockJS HTTP Fallback (xhr-polling, xhr-streaming 등)
        - id: sockjs-fallback
          uri: http://chat-service:8085
          predicates:
            - Path=/ws/stomp/info
            - Path=/ws/stomp/**/{transport}   # xhr, eventsource 등
```

### Backend STOMP 설정 예시

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/stomp")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}
```

## WebSocket 연결 제한

### 동시 연결 수 제한

```java
@Component
@Slf4j
public class WebSocketConnectionLimiter implements GlobalFilter, Ordered {

    private final AtomicInteger connectionCount = new AtomicInteger(0);
    private static final int MAX_CONNECTIONS = 10000;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String upgrade = exchange.getRequest().getHeaders()
                .getFirst(HttpHeaders.UPGRADE);

        if ("websocket".equalsIgnoreCase(upgrade)) {
            int current = connectionCount.get();

            if (current >= MAX_CONNECTIONS) {
                log.warn("WebSocket connection limit reached: {}", current);
                exchange.getResponse().setRawStatusCode(503);
                return exchange.getResponse().setComplete();
            }

            connectionCount.incrementAndGet();
            log.debug("WebSocket connections: {}", connectionCount.get());

            // 연결 종료 시 카운트 감소 (실제로는 WebSocket 이벤트 리스너 필요)
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 3;
    }
}
```

### 사용자별 연결 수 제한

```java
@Component
public class PerUserConnectionLimiter implements GlobalFilter, Ordered {

    private final Map<String, AtomicInteger> userConnections = new ConcurrentHashMap<>();
    private static final int MAX_CONNECTIONS_PER_USER = 5;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String upgrade = exchange.getRequest().getHeaders()
                .getFirst(HttpHeaders.UPGRADE);

        if (!"websocket".equalsIgnoreCase(upgrade)) {
            return chain.filter(exchange);
        }

        String userId = exchange.getRequest().getHeaders()
                .getFirst("X-User-Id");

        if (userId != null) {
            AtomicInteger count = userConnections
                    .computeIfAbsent(userId, k -> new AtomicInteger(0));

            if (count.get() >= MAX_CONNECTIONS_PER_USER) {
                log.warn("User {} exceeded WebSocket connection limit", userId);
                exchange.getResponse().setRawStatusCode(429);
                return exchange.getResponse().setComplete();
            }

            count.incrementAndGet();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 6;
    }
}
```

## WebSocket Timeout 설정

### Netty 설정

```yaml
server:
  netty:
    connection-timeout: 30s
    idle-timeout: 60s

spring:
  cloud:
    gateway:
      httpclient:
        websocket:
          max-frame-payload-length: 65536    # 64KB
          proxy-ping: true                    # Ping 프레임 프록시
```

### 프로그래밍 방식 설정

```java
@Configuration
public class WebSocketTimeoutConfig {

    @Bean
    public WebClientCustomizer webClientCustomizer() {
        return webClientBuilder -> {
            webClientBuilder.defaultHeader("Connection", "Upgrade");
        };
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(60))
                            .addHandlerLast(new WriteTimeoutHandler(60))
                );
    }
}
```

## Secure WebSocket (WSS)

### TLS 설정

```yaml
# HTTPS/WSS 환경
spring:
  cloud:
    gateway:
      routes:
        - id: secure-websocket
          uri: wss://notification-service:8084   # WSS 프로토콜
          predicates:
            - Path=/wss/notifications/**

server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

## 모니터링

### WebSocket 메트릭

```java
@Component
@RequiredArgsConstructor
public class WebSocketMetricsFilter implements GlobalFilter, Ordered {

    private final MeterRegistry meterRegistry;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String upgrade = exchange.getRequest().getHeaders()
                .getFirst(HttpHeaders.UPGRADE);

        if ("websocket".equalsIgnoreCase(upgrade)) {
            Counter.builder("websocket.connections")
                    .tag("path", exchange.getRequest().getPath().value())
                    .register(meterRegistry)
                    .increment();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
```

## 테스트

### WebSocket 연결 테스트

```bash
# wscat 설치
npm install -g wscat

# WebSocket 연결 테스트
wscat -c "ws://localhost:8080/ws/notifications?token=your-jwt-token"

# 메시지 전송
> {"type": "subscribe", "channel": "orders"}
```

### cURL로 Upgrade 요청 테스트

```bash
curl -v \
  -H "Connection: Upgrade" \
  -H "Upgrade: websocket" \
  -H "Sec-WebSocket-Version: 13" \
  -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" \
  "http://localhost:8080/ws/notifications?token=test"
```

## 주의 사항

1. **Sticky Session**: 로드 밸런싱 시 같은 사용자는 같은 Pod로 연결되어야 할 수 있음
2. **Connection Timeout**: 유휴 연결 타임아웃 설정 필요
3. **Reconnection**: 클라이언트에서 재연결 로직 구현 필요
4. **Scaling**: WebSocket 상태 공유를 위해 Redis Pub/Sub 등 고려

## 참고 자료

- [Spring Cloud Gateway WebSocket](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway/websocket-routing.html)
- [Spring WebSocket](https://docs.spring.io/spring-framework/reference/web/websocket.html)
