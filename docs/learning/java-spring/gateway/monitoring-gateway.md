# Gateway Monitoring

API Gateway의 메트릭 수집과 모니터링 방법을 학습합니다.

## 개요

Gateway 모니터링은 전체 시스템의 상태를 파악하는 핵심 지점입니다.

```
                    ┌─────────────────┐
                    │   API Gateway   │
                    │  /actuator/*    │
                    └────────┬────────┘
                             │
            ┌────────────────┼────────────────┐
            │                │                │
            ▼                ▼                ▼
     ┌──────────┐     ┌──────────┐     ┌──────────┐
     │Prometheus│     │  Zipkin  │     │   Loki   │
     │ Metrics  │     │ Tracing  │     │  Logging │
     └──────────┘     └──────────┘     └──────────┘
            │                │                │
            └────────────────┼────────────────┘
                             │
                      ┌──────▼──────┐
                      │   Grafana   │
                      └─────────────┘
```

## Portal Universe 구성

### 의존성

```gradle
dependencies {
    // Actuator
    implementation 'org.springframework.boot:spring-boot-starter-actuator'

    // Prometheus Metrics
    implementation 'io.micrometer:micrometer-registry-prometheus'

    // Distributed Tracing
    implementation 'io.micrometer:micrometer-tracing-bridge-brave'
    implementation 'io.zipkin.reporter2:zipkin-reporter-brave'
}
```

### Actuator 설정

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics,env
      base-path: /actuator
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true            # K8s Liveness/Readiness
      liveness:
        enabled: true
      readiness:
        enabled: true
  prometheus:
    metrics:
      export:
        enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active:local}
```

## Health Check

### Endpoint 구성

| Endpoint | 설명 |
|----------|------|
| `/actuator/health` | 전체 헬스 상태 |
| `/actuator/health/liveness` | K8s Liveness Probe |
| `/actuator/health/readiness` | K8s Readiness Probe |

### Health 응답 예시

```bash
curl http://localhost:8080/actuator/health
```

```json
{
    "status": "UP",
    "components": {
        "circuitBreakers": {
            "status": "UP",
            "details": {
                "blogCircuitBreaker": {
                    "status": "UP",
                    "details": {
                        "failureRate": "0.0%",
                        "slowCallRate": "0.0%",
                        "bufferedCalls": 5,
                        "failedCalls": 0,
                        "state": "CLOSED"
                    }
                }
            }
        },
        "diskSpace": {
            "status": "UP"
        },
        "ping": {
            "status": "UP"
        }
    }
}
```

### Kubernetes Probe 설정

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-gateway
spec:
  template:
    spec:
      containers:
        - name: api-gateway
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 30
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
            failureThreshold: 3
```

## Prometheus Metrics

### 주요 Gateway Metrics

```bash
curl http://localhost:8080/actuator/prometheus
```

| Metric | 설명 |
|--------|------|
| `spring_cloud_gateway_requests_seconds` | 요청 처리 시간 |
| `gateway_requests_total` | 총 요청 수 |
| `resilience4j_circuitbreaker_*` | Circuit Breaker 상태 |
| `jvm_memory_*` | JVM 메모리 |
| `http_server_requests_seconds` | HTTP 요청 히스토그램 |

### 출력 예시

```
# HELP spring_cloud_gateway_requests_seconds
# TYPE spring_cloud_gateway_requests_seconds summary
spring_cloud_gateway_requests_seconds_count{outcome="SUCCESS",routeId="blog-service-route",status="200"} 1234
spring_cloud_gateway_requests_seconds_sum{outcome="SUCCESS",routeId="blog-service-route",status="200"} 56.789

# HELP resilience4j_circuitbreaker_state
# TYPE resilience4j_circuitbreaker_state gauge
resilience4j_circuitbreaker_state{name="blogCircuitBreaker",state="closed"} 1
resilience4j_circuitbreaker_state{name="blogCircuitBreaker",state="open"} 0

# HELP http_server_requests_seconds
# TYPE http_server_requests_seconds histogram
http_server_requests_seconds_bucket{method="GET",uri="/api/blog/posts",le="0.1"} 950
http_server_requests_seconds_bucket{method="GET",uri="/api/blog/posts",le="0.5"} 1100
http_server_requests_seconds_bucket{method="GET",uri="/api/blog/posts",le="1.0"} 1234
```

### 커스텀 Metrics 추가

```java
package com.portal.universe.apigateway.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class MetricsFilter implements GlobalFilter, Ordered {

    private final MeterRegistry meterRegistry;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        // 요청 카운터
        Counter.builder("gateway.requests")
                .tag("method", method)
                .tag("path", normalizePath(path))
                .register(meterRegistry)
                .increment();

        // 타이머
        Timer.Sample sample = Timer.start(meterRegistry);

        return chain.filter(exchange).doFinally(signalType -> {
            String status = String.valueOf(
                    exchange.getResponse().getStatusCode().value());

            sample.stop(Timer.builder("gateway.request.duration")
                    .tag("method", method)
                    .tag("path", normalizePath(path))
                    .tag("status", status)
                    .register(meterRegistry));
        });
    }

    private String normalizePath(String path) {
        // /api/blog/posts/123 -> /api/blog/posts/{id}
        return path.replaceAll("/\\d+", "/{id}");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
```

## Distributed Tracing

### Zipkin 설정

```yaml
management:
  tracing:
    sampling:
      probability: 1.0          # 100% 샘플링 (운영: 0.1 등)

  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
```

### 로그 패턴에 Trace ID 포함

```yaml
logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
    console: "%d{HH:mm:ss.SSS} %5p [%t] [%X{traceId:-},%X{spanId:-}] %logger{36} - %msg%n"
```

### 로그 출력 예시

```
10:30:45.123 INFO  [reactor-http-nio-2] [65f8c2a1b3d4e5f6,78a9b0c1d2e3f4g5] GlobalLoggingFilter - API_REQUEST...
```

## Circuit Breaker 모니터링

### Resilience4j Metrics

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-type: count_based
        sliding-window-size: 20
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 5
        automatic-transition-from-open-to-half-open-enabled: true
        # 메트릭 활성화
        register-health-indicator: true

    instances:
      blogCircuitBreaker:
        base-config: default
      shoppingCircuitBreaker:
        base-config: default
```

### Circuit Breaker 상태 확인

```bash
# Health endpoint에서 확인
curl http://localhost:8080/actuator/health | jq '.components.circuitBreakers'
```

```json
{
    "status": "UP",
    "details": {
        "blogCircuitBreaker": {
            "status": "UP",
            "details": {
                "failureRate": "0.0%",
                "slowCallRate": "0.0%",
                "bufferedCalls": 15,
                "failedCalls": 0,
                "slowCalls": 0,
                "slowFailedCalls": 0,
                "state": "CLOSED"
            }
        }
    }
}
```

## Grafana Dashboard

### Prometheus 데이터 소스 쿼리

```promql
# 요청 처리량 (RPS)
sum(rate(spring_cloud_gateway_requests_seconds_count[5m])) by (routeId)

# 평균 응답 시간
sum(rate(spring_cloud_gateway_requests_seconds_sum[5m])) /
sum(rate(spring_cloud_gateway_requests_seconds_count[5m])) by (routeId)

# 95th percentile 응답 시간
histogram_quantile(0.95,
    sum(rate(http_server_requests_seconds_bucket{application="api-gateway"}[5m])) by (le, uri)
)

# 에러율
sum(rate(spring_cloud_gateway_requests_seconds_count{status=~"5.."}[5m])) /
sum(rate(spring_cloud_gateway_requests_seconds_count[5m])) * 100

# Circuit Breaker OPEN 상태
resilience4j_circuitbreaker_state{state="open"} == 1
```

### 알림 규칙 (Alertmanager)

```yaml
# prometheus-rules.yaml
groups:
  - name: gateway-alerts
    rules:
      - alert: HighErrorRate
        expr: |
          sum(rate(spring_cloud_gateway_requests_seconds_count{status=~"5.."}[5m])) /
          sum(rate(spring_cloud_gateway_requests_seconds_count[5m])) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate on API Gateway"
          description: "Error rate is {{ $value | humanizePercentage }}"

      - alert: CircuitBreakerOpen
        expr: resilience4j_circuitbreaker_state{state="open"} == 1
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "Circuit Breaker {{ $labels.name }} is OPEN"

      - alert: HighLatency
        expr: |
          histogram_quantile(0.95,
            sum(rate(http_server_requests_seconds_bucket{application="api-gateway"}[5m])) by (le)
          ) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High latency on API Gateway"
          description: "95th percentile latency is {{ $value }}s"
```

## 로깅 (Loki)

### 구조화된 로그 설정

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
            <customFields>
                {"service":"api-gateway","environment":"${SPRING_PROFILES_ACTIVE:-local}"}
            </customFields>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="JSON"/>
    </root>
</configuration>
```

### Loki 쿼리 예시

```logql
# 에러 로그 검색
{service="api-gateway"} |= "ERROR"

# 특정 Trace ID로 검색
{service="api-gateway"} |~ "traceId.*abc123"

# 느린 요청 (500ms 이상)
{service="api-gateway"} | json | duration > 500
```

## 환경별 모니터링 설정

### Local (개발)

```yaml
# application-local.yml
management:
  endpoints:
    web:
      exposure:
        include: "*"           # 모든 endpoint 노출
  endpoint:
    health:
      show-details: always

logging:
  level:
    org.springframework.cloud.gateway: DEBUG
```

### Docker

```yaml
# application-docker.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics,env
  prometheus:
    metrics:
      export:
        enabled: true
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
```

### Kubernetes

```yaml
# application-kubernetes.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      probes:
        enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
      environment: kubernetes
```

## 디버깅 Endpoints

### Routes 확인

```bash
# Gateway Routes 목록 (endpoint 활성화 필요)
curl http://localhost:8080/actuator/gateway/routes | jq
```

```json
[
    {
        "route_id": "blog-service-route",
        "route_definition": {
            "predicates": ["Path=/api/blog/**"],
            "filters": ["StripPrefix=2", "CircuitBreaker"]
        },
        "order": 2
    }
]
```

### Metrics 상세

```bash
# 특정 메트릭 상세
curl http://localhost:8080/actuator/metrics/spring.cloud.gateway.requests

# 태그 기반 필터링
curl "http://localhost:8080/actuator/metrics/spring.cloud.gateway.requests?tag=routeId:blog-service-route"
```

## 테스트

```bash
# Health Check
curl http://localhost:8080/actuator/health

# Prometheus Metrics
curl http://localhost:8080/actuator/prometheus | grep gateway

# Circuit Breaker 상태
curl http://localhost:8080/actuator/health | jq '.components.circuitBreakers'

# 환경 정보
curl http://localhost:8080/actuator/env | jq '.propertySources[] | select(.name | contains("application"))'
```

## 참고 자료

- Portal Universe: `services/api-gateway/src/main/resources/application-*.yml`
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer](https://micrometer.io/)
- [Prometheus Alerting Rules](https://prometheus.io/docs/prometheus/latest/configuration/alerting_rules/)
