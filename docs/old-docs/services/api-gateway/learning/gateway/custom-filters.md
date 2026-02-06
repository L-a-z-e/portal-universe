# Custom Filters

Spring Cloud Gateway에서 커스텀 필터를 구현하는 방법을 학습합니다.

## 개요

Gateway Filter는 요청/응답을 가로채어 변환하는 컴포넌트입니다.

```
Request → Pre Filter → Route → Backend → Post Filter → Response
              │                                │
         요청 변환                          응답 변환
```

## 필터 유형

| 유형 | 설명 | 예시 |
|------|------|------|
| **Global Filter** | 모든 라우트에 적용 | 로깅, 인증 |
| **Gateway Filter** | 특정 라우트에 적용 | 경로 재작성, 헤더 추가 |
| **WebFilter** | Spring WebFlux 필터 | CORS, Security |

## Portal Universe 커스텀 필터

### 1. GlobalLoggingFilter

모든 요청/응답을 로깅하는 Global Filter입니다.

```java
package com.portal.universe.apigateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 2)  // 필터 실행 순서
public class GlobalLoggingFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startTime = System.currentTimeMillis();

        String requestPath = request.getPath().value();
        String method = request.getMethod().name();
        String clientIp = getClientIp(request, exchange);

        // Pre-filter: 요청 로깅
        log.info("API_REQUEST - Method: {}, Path: {}, IP: {}, Headers: {}",
                method, requestPath, clientIp,
                request.getHeaders().toSingleValueMap());

        // Post-filter: 응답 로깅
        return chain.filter(exchange).then(
                Mono.fromRunnable(() -> {
                    ServerHttpResponse response = exchange.getResponse();
                    long duration = System.currentTimeMillis() - startTime;

                    log.info("API_RESPONSE - Path: {}, Status: {}, Duration: {}ms",
                            requestPath, response.getStatusCode(), duration);
                })
        );
    }

    private String getClientIp(ServerHttpRequest request, ServerWebExchange exchange) {
        XForwardedRemoteAddressResolver resolver =
                XForwardedRemoteAddressResolver.maxTrustedIndex(1);
        InetSocketAddress remoteAddress = resolver.resolve(exchange);

        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return "Unknown";
    }
}
```

### 2. GlobalForwardedHeadersFilter

X-Forwarded 헤더를 추가하는 Global Filter입니다.

```java
package com.portal.universe.apigateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalForwardedHeadersFilter implements GlobalFilter, Ordered {

    private final FrontendProperties frontendProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // FrontendProperties에서 설정값 가져오기
        String forwardedHost = frontendProperties.getHost();
        String forwardedScheme = frontendProperties.getScheme();
        String forwardedPort = String.valueOf(frontendProperties.getPort());
        String forwardedFor = extractClientIp(request);

        // 요청에 X-Forwarded-* 헤더 추가
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-Forwarded-Host", forwardedHost)
                .header("X-Forwarded-Proto", forwardedScheme)
                .header("X-Forwarded-Port", forwardedPort)
                .header("X-Forwarded-For", forwardedFor)
                .build();

        log.debug("Forwarded Headers - Host={}, Proto={}, Port={}, For={}",
                forwardedHost, forwardedScheme, forwardedPort, forwardedFor);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private String extractClientIp(ServerHttpRequest request) {
        // 1. 기존 X-Forwarded-For 헤더
        String existingForwardedFor = request.getHeaders()
                .getFirst("X-Forwarded-For");
        if (existingForwardedFor != null && !existingForwardedFor.isEmpty()) {
            return existingForwardedFor.split(",")[0].trim();
        }

        // 2. X-Real-IP 헤더
        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }

        // 3. Remote Address
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;  // 로깅 필터보다 먼저 실행
    }
}
```

### 3. JwtAuthenticationFilter

JWT 토큰을 검증하는 WebFilter입니다.

```java
package com.portal.universe.apigateway.filter;

import com.portal.universe.apigateway.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtConfig jwtConfig;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtConfig.getSecretKey()
                .getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 공개 경로는 JWT 검증 생략
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // Authorization 헤더에서 토큰 추출
        String authHeader = request.getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return chain.filter(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            // JWT 검증
            Claims claims = validateToken(token);
            String userId = claims.getSubject();
            String roles = claims.get("roles", String.class);

            log.debug("JWT validated for user: {}, roles: {}", userId, roles);

            // Spring Security Context에 인증 정보 설정
            List<SimpleGrantedAuthority> authorities = roles != null
                    ? List.of(new SimpleGrantedAuthority(roles))
                    : Collections.emptyList();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId, null, authorities);

            // 하위 서비스를 위한 헤더 추가
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Roles", roles != null ? roles : "")
                    .build();

            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(mutatedRequest)
                    .build();

            return chain.filter(mutatedExchange)
                    .contextWrite(ReactiveSecurityContextHolder
                            .withAuthentication(authentication));

        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            return handleUnauthorized(exchange, "Token expired");
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return handleUnauthorized(exchange, "Invalid token");
        }
    }

    private Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/auth-service/") ||
               path.startsWith("/api/auth/") ||
               path.startsWith("/api/users/") ||
               path.startsWith("/actuator/");
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("X-Auth-Error", message);
        return exchange.getResponse().setComplete();
    }
}
```

## 커스텀 GatewayFilterFactory

라우트별로 적용 가능한 필터를 만들려면 GatewayFilterFactory를 구현합니다.

### 예시: Request ID 추가 필터

```java
package com.portal.universe.apigateway.filter;

import lombok.Getter;
import lombok.Setter;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class RequestIdGatewayFilterFactory
        extends AbstractGatewayFilterFactory<RequestIdGatewayFilterFactory.Config> {

    public RequestIdGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String requestId = exchange.getRequest()
                    .getHeaders()
                    .getFirst(config.getHeaderName());

            if (requestId == null) {
                requestId = UUID.randomUUID().toString();
            }

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header(config.getHeaderName(), requestId)
                    .build();

            // 응답에도 Request ID 추가
            exchange.getResponse().getHeaders()
                    .add(config.getHeaderName(), requestId);

            return chain.filter(exchange.mutate()
                    .request(mutatedRequest)
                    .build());
        };
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("headerName");
    }

    @Getter
    @Setter
    public static class Config {
        private String headerName = "X-Request-Id";
    }
}
```

### YAML에서 사용

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: blog-service-route
          uri: ${services.blog.url}
          predicates:
            - Path=/api/blog/**
          filters:
            - RequestId=X-Correlation-Id   # 커스텀 필터 적용
```

## 필터 실행 순서

```
┌─────────────────────────────────────────────────────────────┐
│                      Filter Order                           │
├─────────────────────────────────────────────────────────────┤
│  HIGHEST_PRECEDENCE (-∞)                                    │
│    │                                                        │
│    ├─ requestPathLoggingFilter (HIGHEST + 0)               │
│    ├─ corsWebFilter (HIGHEST + 1)                          │
│    ├─ GlobalForwardedHeadersFilter (HIGHEST + 1)           │
│    ├─ GlobalLoggingFilter (HIGHEST + 2)                    │
│    │                                                        │
│    ├─ ... (Default Filters)                                │
│    │                                                        │
│    ├─ Route-specific Filters (order by YAML definition)    │
│    │                                                        │
│    └─ NettyWriteResponseFilter (LOWEST - 1)                │
│                                                             │
│  LOWEST_PRECEDENCE (+∞)                                     │
└─────────────────────────────────────────────────────────────┘
```

### 순서 제어 방법

```java
// 1. @Order 어노테이션
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class MyFilter implements GlobalFilter { }

// 2. Ordered 인터페이스 구현
public class MyFilter implements GlobalFilter, Ordered {
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
```

## Pre/Post 필터 패턴

```java
@Component
public class TimingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // === PRE-FILTER (요청 처리 전) ===
        long startTime = System.nanoTime();
        exchange.getAttributes().put("startTime", startTime);

        return chain.filter(exchange)
                // === POST-FILTER (응답 처리 후) ===
                .then(Mono.fromRunnable(() -> {
                    Long start = exchange.getAttribute("startTime");
                    if (start != null) {
                        long duration = (System.nanoTime() - start) / 1_000_000;
                        exchange.getResponse().getHeaders()
                                .add("X-Response-Time", duration + "ms");
                    }
                }));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;  // 마지막에 실행
    }
}
```

## 조건부 필터 적용

```java
@Component
public class ConditionalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 특정 경로에만 적용
        if (path.startsWith("/api/admin/")) {
            // Admin 전용 로직
            log.info("Admin API accessed: {}", path);
        }

        // 특정 헤더가 있을 때만 적용
        String debugHeader = exchange.getRequest()
                .getHeaders()
                .getFirst("X-Debug-Mode");
        if ("true".equals(debugHeader)) {
            // 디버그 모드 로직
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
```

## 테스트

### 필터 단위 테스트

```java
@ExtendWith(MockitoExtension.class)
class GlobalLoggingFilterTest {

    @InjectMocks
    private GlobalLoggingFilter filter;

    @Test
    void shouldLogRequestAndResponse() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/blog/posts")
                .header("Authorization", "Bearer token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // When
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Then
        verify(chain).filter(any());
    }
}
```

## 참고 자료

- Portal Universe: `services/api-gateway/src/main/java/.../config/`
- [Spring Cloud Gateway Filters](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway/gatewayfilter-factories.html)
- [Custom Filters Guide](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway/developer-guide.html)
