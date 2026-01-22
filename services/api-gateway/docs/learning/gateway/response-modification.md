# Response Modification

API Gateway에서 응답을 수정하는 방법을 학습합니다.

## 개요

Gateway에서 Backend 응답을 가공하여 클라이언트에게 전달할 수 있습니다.

```
Backend Response → Response Filter → Modified Response → Client
                        │
                   헤더 추가/삭제
                   Body 변환
                   상태 코드 변경
```

## 응답 헤더 수정

### 내장 필터 사용

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
            # 응답 헤더 추가
            - AddResponseHeader=X-Response-Gateway, api-gateway
            - AddResponseHeader=X-Powered-By, Portal-Universe

            # 응답 헤더 삭제 (민감 정보 제거)
            - RemoveResponseHeader=Server
            - RemoveResponseHeader=X-Powered-By-Backend

            # 응답 헤더 재작성
            - RewriteResponseHeader=X-Request-Id, , $\{requestId}

            # 중복 응답 헤더 제거
            - DedupeResponseHeader=Access-Control-Allow-Origin, RETAIN_FIRST
```

### 커스텀 헤더 추가 필터

```java
package com.portal.universe.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
public class ResponseHeaderFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            // 응답 헤더 추가
            exchange.getResponse().getHeaders()
                    .add("X-Gateway-Timestamp", Instant.now().toString());

            // 요청 시작 시간에서 처리 시간 계산
            Long startTime = exchange.getAttribute("startTime");
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;
                exchange.getResponse().getHeaders()
                        .add("X-Response-Time", duration + "ms");
            }
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }
}
```

## 응답 Body 수정

### ModifyResponseBody 필터

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
            - name: ModifyResponseBody
              args:
                inClass: java.lang.String
                outClass: java.lang.String
                rewriteFunction: com.portal.universe.apigateway.filter.ResponseRewriter
```

### ResponseRewriter 구현

```java
package com.portal.universe.apigateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
@Slf4j
@RequiredArgsConstructor
public class ResponseRewriter implements RewriteFunction<String, String> {

    private final ObjectMapper objectMapper;

    @Override
    public Publisher<String> apply(ServerWebExchange exchange, String body) {
        try {
            // JSON 파싱
            JsonNode root = objectMapper.readTree(body);

            if (root instanceof ObjectNode objectNode) {
                // 메타데이터 추가
                ObjectNode metadata = objectMapper.createObjectNode();
                metadata.put("gateway", "api-gateway");
                metadata.put("timestamp", Instant.now().toString());
                metadata.put("requestId", exchange.getRequest().getId());

                objectNode.set("_metadata", metadata);

                return Mono.just(objectMapper.writeValueAsString(objectNode));
            }

            return Mono.just(body);

        } catch (Exception e) {
            log.warn("Failed to modify response body: {}", e.getMessage());
            return Mono.just(body);
        }
    }
}
```

### 응답 변환 예시

**원본 응답:**
```json
{
    "id": 1,
    "title": "Hello World",
    "content": "..."
}
```

**변환된 응답:**
```json
{
    "id": 1,
    "title": "Hello World",
    "content": "...",
    "_metadata": {
        "gateway": "api-gateway",
        "timestamp": "2025-01-22T10:30:00Z",
        "requestId": "abc123"
    }
}
```

## Fallback 응답

### FallbackController

Circuit Breaker가 열렸을 때 반환되는 Fallback 응답입니다.

```java
package com.portal.universe.apigateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
public class FallbackController {

    @GetMapping("/fallback/blog")
    public Mono<ResponseEntity<Map<String, Object>>> blogServiceFallback() {
        Map<String, Object> response = Map.of(
                "status", "error",
                "code", "SERVICE_UNAVAILABLE",
                "message", "Blog Service is currently unavailable. Please try again later.",
                "timestamp", Instant.now().toString(),
                "retry_after", 30
        );

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response));
    }

    @GetMapping("/fallback/shopping")
    public Mono<ResponseEntity<Map<String, Object>>> shoppingServiceFallback() {
        Map<String, Object> response = Map.of(
                "status", "error",
                "code", "SERVICE_UNAVAILABLE",
                "message", "Shopping Service is temporarily unavailable.",
                "timestamp", Instant.now().toString(),
                "fallback_data", Map.of(
                        "products", List.of(),
                        "cached_at", Instant.now().minus(5, ChronoUnit.MINUTES).toString()
                )
        );

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response));
    }

    @GetMapping("/fallback/auth")
    public Mono<ResponseEntity<Map<String, Object>>> authServiceFallback() {
        Map<String, Object> response = Map.of(
                "status", "error",
                "code", "AUTH_SERVICE_UNAVAILABLE",
                "message", "Authentication service is temporarily unavailable.",
                "timestamp", Instant.now().toString()
        );

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response));
    }
}
```

### YAML 설정

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
            - name: CircuitBreaker
              args:
                name: blogCircuitBreaker
                fallbackUri: forward:/fallback/blog    # Fallback 경로
```

## 에러 응답 표준화

### GlobalErrorHandler

```java
package com.portal.universe.apigateway.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@Component
@Order(-2)  // DefaultErrorWebExceptionHandler보다 먼저 실행
@Slf4j
@RequiredArgsConstructor
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = determineStatus(ex);
        String message = determineMessage(ex);

        Map<String, Object> errorResponse = Map.of(
                "status", "error",
                "code", status.name(),
                "message", message,
                "path", exchange.getRequest().getPath().value(),
                "timestamp", Instant.now().toString(),
                "traceId", exchange.getRequest().getId()
        );

        log.error("Gateway error - Path: {}, Status: {}, Message: {}",
                exchange.getRequest().getPath(), status, message, ex);

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }

    private HttpStatus determineStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            return HttpStatus.valueOf(rse.getStatusCode().value());
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String determineMessage(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            return rse.getReason() != null ? rse.getReason() : rse.getMessage();
        }
        return "An unexpected error occurred";
    }
}
```

## 응답 압축

### GZIP 압축 설정

```yaml
server:
  compression:
    enabled: true
    mime-types:
      - application/json
      - application/xml
      - text/html
      - text/plain
    min-response-size: 1024    # 1KB 이상만 압축
```

## 응답 캐싱

### 캐시 헤더 추가

```java
@Component
public class CacheHeaderFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            String path = exchange.getRequest().getPath().value();

            // 정적 리소스에 캐시 헤더 추가
            if (isStaticResource(path)) {
                exchange.getResponse().getHeaders()
                        .setCacheControl("public, max-age=86400");  // 1일
            }
            // API 응답은 캐시 비활성화
            else if (path.startsWith("/api/")) {
                exchange.getResponse().getHeaders()
                        .setCacheControl("no-store, no-cache, must-revalidate");
                exchange.getResponse().getHeaders()
                        .setPragma("no-cache");
            }
        }));
    }

    private boolean isStaticResource(String path) {
        return path.endsWith(".js") ||
               path.endsWith(".css") ||
               path.endsWith(".png") ||
               path.endsWith(".jpg");
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 2;
    }
}
```

## 민감 정보 마스킹

### 응답 Body에서 민감 정보 제거

```java
@Component
@RequiredArgsConstructor
public class SensitiveDataMaskingFilter implements RewriteFunction<String, String> {

    private final ObjectMapper objectMapper;

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "ssn", "creditCard", "cardNumber",
            "cvv", "secretKey", "apiKey"
    );

    @Override
    public Publisher<String> apply(ServerWebExchange exchange, String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            maskSensitiveFields(root);
            return Mono.just(objectMapper.writeValueAsString(root));
        } catch (Exception e) {
            return Mono.just(body);
        }
    }

    private void maskSensitiveFields(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            objectNode.fieldNames().forEachRemaining(fieldName -> {
                if (SENSITIVE_FIELDS.contains(fieldName.toLowerCase())) {
                    objectNode.put(fieldName, "***MASKED***");
                } else {
                    maskSensitiveFields(objectNode.get(fieldName));
                }
            });
        } else if (node.isArray()) {
            node.forEach(this::maskSensitiveFields);
        }
    }
}
```

## 응답 변환 체인

```
Backend Response
       │
       ▼
┌──────────────────┐
│ Error Handler    │ → 에러 응답 표준화
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Sensitive Mask   │ → 민감 정보 마스킹
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Response Rewriter│ → 메타데이터 추가
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Header Filter    │ → 헤더 수정
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Cache Header     │ → 캐시 헤더 설정
└────────┬─────────┘
         │
         ▼
    Client Response
```

## 테스트

```bash
# 응답 헤더 확인
curl -v http://localhost:8080/api/blog/posts | head -30

# Fallback 테스트 (서비스 중지 후)
curl http://localhost:8080/api/blog/posts

# 에러 응답 확인
curl http://localhost:8080/api/invalid-path
```

## 참고 자료

- Portal Universe: `services/api-gateway/src/main/java/.../controller/FallbackController.java`
- [Spring Cloud Gateway Response Modification](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway/gatewayfilter-factories/modifyrequestbody-factory.html)
