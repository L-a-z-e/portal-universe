# Request/Response Logging

API Gateway에서 요청과 응답을 로깅하는 방법을 학습합니다.

## 개요

Gateway 레벨에서 모든 트래픽을 로깅하면 디버깅, 감사(Audit), 모니터링에 유용합니다.

```
Request → GlobalLoggingFilter → Route → Backend → Response → Log
            ↓                                         ↓
       [요청 로깅]                              [응답 로깅]
```

## Portal Universe 구현

### GlobalLoggingFilter

모든 요청과 응답을 로깅하는 Global Filter입니다.

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
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class GlobalLoggingFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startTime = System.currentTimeMillis();

        String requestPath = request.getPath().value();
        String method = request.getMethod().name();
        String clientIp = getClientIp(request, exchange);

        // 요청 로깅
        log.info("API_REQUEST - Method: {}, Path: {}, IP: {}, Headers: {}",
                method, requestPath, clientIp, request.getHeaders().toSingleValueMap());

        return chain.filter(exchange).then(
                Mono.fromRunnable(() -> {
                    ServerHttpResponse response = exchange.getResponse();
                    long duration = System.currentTimeMillis() - startTime;

                    // 응답 로깅
                    log.info("API_RESPONSE - Path: {}, Status: {}, Duration: {}ms",
                            requestPath, response.getStatusCode(), duration);
                })
        );
    }

    /**
     * X-Forwarded-For 헤더를 고려하여 클라이언트 IP를 추출합니다.
     */
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

### 로그 출력 예시

```
2025-01-22 10:30:45 [reactor-http-nio-2] INFO  [api-gateway,abc123,def456] c.p.u.a.c.GlobalLoggingFilter - API_REQUEST - Method: GET, Path: /api/blog/posts, IP: 192.168.1.100, Headers: {Accept=application/json, Authorization=Bearer eyJ...}

2025-01-22 10:30:46 [reactor-http-nio-2] INFO  [api-gateway,abc123,def456] c.p.u.a.c.GlobalLoggingFilter - API_RESPONSE - Path: /api/blog/posts, Status: 200 OK, Duration: 145ms
```

## 구조화된 로깅 (JSON)

### Logback 설정

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
            <customFields>{"service":"api-gateway"}</customFields>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="JSON"/>
    </root>
</configuration>
```

### 구조화된 로그 출력

```json
{
    "@timestamp": "2025-01-22T10:30:45.123Z",
    "level": "INFO",
    "service": "api-gateway",
    "traceId": "abc123",
    "spanId": "def456",
    "logger": "GlobalLoggingFilter",
    "message": "API_REQUEST",
    "method": "GET",
    "path": "/api/blog/posts",
    "clientIp": "192.168.1.100",
    "duration": 145
}
```

## 상세 로깅 필터

### 요청 Body 로깅 (주의: 성능 영향)

```java
@Component
@Slf4j
public class RequestBodyLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 특정 경로만 Body 로깅 (민감 정보 제외)
        if (shouldLogBody(path)) {
            return DataBufferUtils.join(exchange.getRequest().getBody())
                    .flatMap(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);

                        String body = new String(bytes, StandardCharsets.UTF_8);
                        log.debug("Request Body: {}", maskSensitiveData(body));

                        // Body를 다시 사용할 수 있도록 재구성
                        ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(
                                exchange.getRequest()) {
                            @Override
                            public Flux<DataBuffer> getBody() {
                                return Flux.just(exchange.getResponse()
                                        .bufferFactory()
                                        .wrap(bytes));
                            }
                        };

                        return chain.filter(exchange.mutate()
                                .request(mutatedRequest)
                                .build());
                    });
        }

        return chain.filter(exchange);
    }

    private boolean shouldLogBody(String path) {
        // 로그인 경로는 Body 로깅 제외 (비밀번호 노출 방지)
        return !path.contains("/login") &&
               !path.contains("/password") &&
               !path.contains("/signup");
    }

    private String maskSensitiveData(String body) {
        // 민감 정보 마스킹
        return body.replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"",
                               "\"password\":\"***\"")
                   .replaceAll("\"token\"\\s*:\\s*\"[^\"]*\"",
                               "\"token\":\"***\"");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 3;
    }
}
```

### 응답 Body 로깅

```java
@Component
@Slf4j
public class ResponseBodyLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponseDecorator decoratedResponse =
                new ServerHttpResponseDecorator(exchange.getResponse()) {

            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;

                    return super.writeWith(fluxBody.buffer().map(dataBuffers -> {
                        // 모든 DataBuffer를 합침
                        DataBufferFactory bufferFactory = exchange.getResponse()
                                .bufferFactory();
                        DataBuffer joinedBuffer = bufferFactory.join(dataBuffers);

                        byte[] content = new byte[joinedBuffer.readableByteCount()];
                        joinedBuffer.read(content);
                        DataBufferUtils.release(joinedBuffer);

                        // 응답 로깅
                        String responseBody = new String(content, StandardCharsets.UTF_8);
                        log.debug("Response Body ({}): {}",
                                exchange.getRequest().getPath(),
                                truncate(responseBody, 1000));

                        return bufferFactory.wrap(content);
                    }));
                }
                return super.writeWith(body);
            }
        };

        return chain.filter(exchange.mutate()
                .response(decoratedResponse)
                .build());
    }

    private String truncate(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "... [truncated]";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 4;
    }
}
```

## 분산 추적 (Distributed Tracing)

### Micrometer Tracing 설정

```yaml
# application.yml
management:
  tracing:
    sampling:
      probability: 1.0            # 모든 요청 추적 (운영: 0.1 등 조절)

  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans

logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

### 로그 출력 형식

```
INFO  [api-gateway,65f8c2a1b3d4e5f6,78a9b0c1d2e3f4g5] - API_REQUEST...
              ↑                    ↑
          traceId              spanId
```

## 로그 레벨 제어

### 환경별 로그 설정

```yaml
# application-local.yml (개발 환경)
logging:
  level:
    org.springframework.cloud.gateway: DEBUG
    org.springframework.cloud.gateway.route: INFO
    org.springframework.cloud.gateway.filter: INFO
    com.portal.universe.apigateway: DEBUG

# application-kubernetes.yml (운영 환경)
logging:
  level:
    root: INFO
    org.springframework.cloud.gateway: INFO
    com.portal.universe.apigateway: INFO
```

### 동적 로그 레벨 변경

```bash
# Actuator를 통한 런타임 로그 레벨 변경
curl -X POST "http://localhost:8080/actuator/loggers/com.portal.universe.apigateway" \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel":"DEBUG"}'
```

## 민감 정보 보호

### 로깅 제외 헤더

```java
private Map<String, String> sanitizeHeaders(HttpHeaders headers) {
    Set<String> sensitiveHeaders = Set.of(
            "authorization",
            "cookie",
            "x-api-key",
            "x-auth-token"
    );

    return headers.toSingleValueMap().entrySet().stream()
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> sensitiveHeaders.contains(e.getKey().toLowerCase())
                            ? "***MASKED***"
                            : e.getValue()
            ));
}
```

## 로그 집계

### ELK Stack 연동

```yaml
# logback-spring.xml (Logstash Appender)
<appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
    <destination>logstash:5000</destination>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <customFields>{"application":"api-gateway"}</customFields>
    </encoder>
</appender>
```

### Loki 연동

```yaml
# loki-config.yaml
scrape_configs:
  - job_name: api-gateway
    static_configs:
      - targets:
          - localhost
        labels:
          job: api-gateway
          __path__: /var/log/api-gateway/*.log
```

## 테스트

```bash
# 요청 로깅 확인
curl -v -H "X-Request-Id: test-123" http://localhost:8080/api/blog/posts

# 로그 확인
tail -f logs/api-gateway.log | grep "API_REQUEST\|API_RESPONSE"
```

## 참고 자료

- Portal Universe: `services/api-gateway/src/main/java/.../config/GlobalLoggingFilter.java`
- [Micrometer Tracing](https://micrometer.io/docs/tracing)
- [ELK Stack](https://www.elastic.co/what-is/elk-stack)
