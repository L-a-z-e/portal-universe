# Zipkin Tracing

분산 추적 시스템인 Zipkin과 Spring Cloud Sleuth를 사용한 트레이싱을 학습합니다.

---

## 1. 분산 추적 개요

### 왜 분산 추적이 필요한가?

마이크로서비스 환경에서는 하나의 요청이 여러 서비스를 거치면서 처리됩니다.

```
┌────────────────────────────────────────────────────────────────────┐
│  User Request: POST /api/v1/orders                                  │
└─────────────────────────────┬──────────────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────────────┐
│  API Gateway                │ Where did it fail?                    │
│      │                      │ Which service is slow?                │
│      ▼                      │ What's the call sequence?             │
│  Auth Service ──────────────┤                                       │
│      │                      │                                       │
│      ▼                      │                                       │
│  Shopping Service ──────────┤                                       │
│      │                      │                                       │
│      ▼                      │                                       │
│  Notification Service       │                                       │
└────────────────────────────────────────────────────────────────────┘
```

### 분산 추적의 핵심 개념

| 개념 | 설명 |
|------|------|
| **Trace** | 전체 요청의 여정 (TraceId로 식별) |
| **Span** | 단일 작업 단위 (SpanId로 식별) |
| **Parent Span** | 현재 Span을 호출한 상위 Span |
| **Tags** | Span에 추가된 메타데이터 |
| **Logs/Events** | Span 내에서 발생한 이벤트 |

### Trace 구조

```
Trace (TraceId: abc123)
│
├── Span A: API Gateway (SpanId: span1, ParentSpan: null)
│   ├── Start: 0ms
│   └── End: 150ms
│
├── Span B: Auth Service (SpanId: span2, ParentSpan: span1)
│   ├── Start: 10ms
│   └── End: 50ms
│
├── Span C: Shopping Service (SpanId: span3, ParentSpan: span1)
│   ├── Start: 60ms
│   └── End: 120ms
│
└── Span D: Notification Service (SpanId: span4, ParentSpan: span3)
    ├── Start: 80ms
    └── End: 100ms
```

---

## 2. Zipkin 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│  Microservices with Tracing                                      │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ auth-service │  │ blog-service │  │  shopping    │          │
│  │  (Reporter)  │  │  (Reporter)  │  │  (Reporter)  │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
│         │                 │                 │                   │
│         └─────────────────┼─────────────────┘                   │
│                           │ HTTP POST /api/v2/spans             │
│                           ▼                                      │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                     Zipkin Server :9411                    │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────────────┐  │  │
│  │  │ Collector  │→ │   Storage  │→ │   Query Service    │  │  │
│  │  │            │  │ (In-Memory │  │   (API & UI)       │  │  │
│  │  │            │  │  /ES/MySQL)│  │                    │  │  │
│  │  └────────────┘  └────────────┘  └────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. Portal Universe Zipkin 설정

### Docker Compose

```yaml
# docker-compose.yml
zipkin:
  image: openzipkin/zipkin:latest
  container_name: zipkin
  ports:
    - "9411:9411"
  environment:
    - STORAGE_TYPE=mem           # 인메모리 저장소 (개발용)
  networks:
    - portal-universe-net
```

### Kubernetes Deployment

```yaml
# k8s/infrastructure/zipkin.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: zipkin
  namespace: portal-universe
spec:
  replicas: 1
  selector:
    matchLabels:
      app: zipkin
  template:
    metadata:
      labels:
        app: zipkin
    spec:
      containers:
        - name: zipkin
          image: openzipkin/zipkin:latest
          ports:
            - containerPort: 9411
          env:
            - name: STORAGE_TYPE
              value: "mem"
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          livenessProbe:
            httpGet:
              path: /health
              port: 9411
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /health
              port: 9411
            initialDelaySeconds: 20
            periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: zipkin
  namespace: portal-universe
spec:
  type: ClusterIP
  ports:
    - port: 9411
      targetPort: 9411
  selector:
    app: zipkin
```

### 프로덕션 저장소 옵션

| 저장소 | 환경 변수 | 용도 |
|--------|----------|------|
| In-Memory | `STORAGE_TYPE=mem` | 개발/테스트 |
| Elasticsearch | `STORAGE_TYPE=elasticsearch` | 프로덕션 (권장) |
| MySQL | `STORAGE_TYPE=mysql` | 소규모 프로덕션 |
| Cassandra | `STORAGE_TYPE=cassandra` | 대규모 프로덕션 |

```yaml
# Elasticsearch 연동 예시
zipkin:
  environment:
    - STORAGE_TYPE=elasticsearch
    - ES_HOSTS=http://elasticsearch:9200
```

---

## 4. Spring Boot Tracing 설정

### Spring Boot 3.x (Micrometer Tracing)

Spring Boot 3.x부터는 Spring Cloud Sleuth 대신 **Micrometer Tracing**을 사용합니다.

#### 의존성 추가

```kotlin
// build.gradle.kts
dependencies {
    // Micrometer Tracing
    implementation("io.micrometer:micrometer-tracing-bridge-brave")

    // Zipkin Reporter
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")

    // Spring Boot Actuator
    implementation("org.springframework.boot:spring-boot-starter-actuator")
}
```

#### application.yml 설정

```yaml
# application.yml
spring:
  application:
    name: auth-service

management:
  tracing:
    sampling:
      probability: 1.0              # 100% 샘플링 (개발용)
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans

logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

### Profile별 설정

```yaml
# application-local.yml
management:
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans

# application-docker.yml
management:
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans

# application-kubernetes.yml
management:
  zipkin:
    tracing:
      endpoint: http://zipkin.portal-universe:9411/api/v2/spans
```

---

## 5. Trace Propagation

### HTTP Header Propagation

서비스 간 호출 시 Trace 정보가 HTTP 헤더를 통해 전파됩니다.

```
┌────────────────────────────────────────────────────────────────────┐
│  HTTP Request Headers                                               │
│                                                                     │
│  X-B3-TraceId: 80f198ee56343ba864fe8b2a57d3eff7                    │
│  X-B3-SpanId: e457b5a2e4d86bd1                                      │
│  X-B3-ParentSpanId: 05e3ac9a4f6e3b90                                │
│  X-B3-Sampled: 1                                                    │
└────────────────────────────────────────────────────────────────────┘
```

### W3C Trace Context (기본값)

Spring Boot 3.x에서는 W3C Trace Context가 기본입니다.

```
traceparent: 00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01
tracestate: congo=t61rcWkgMzE
```

### RestTemplate / WebClient 설정

Micrometer Tracing은 자동으로 RestTemplate과 WebClient에 Trace Context를 주입합니다.

```java
@Configuration
public class HttpClientConfig {

    // RestTemplate은 자동으로 계측됨
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    // WebClient도 자동으로 계측됨
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }
}
```

### Feign Client 설정

```java
@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/api/v1/users/{id}")
    User getUser(@PathVariable Long id);
    // TraceId/SpanId가 자동으로 전파됨
}
```

---

## 6. 커스텀 Span 생성

### @NewSpan 어노테이션

```java
@Service
public class OrderService {

    private final Tracer tracer;

    // 새로운 Span 생성
    @NewSpan("process-order")
    public void processOrder(Order order) {
        // Span 내에서 실행
    }

    // 현재 Span에 태그 추가
    @SpanTag("order.id")
    public void processOrder(@SpanTag("order.id") String orderId) {
        // ...
    }
}
```

### 프로그래밍 방식

```java
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final Tracer tracer;

    public void processPayment(Payment payment) {
        // 새 Span 시작
        Span newSpan = tracer.nextSpan().name("payment-processing").start();

        try (Tracer.SpanInScope ws = tracer.withSpan(newSpan)) {
            // 태그 추가
            newSpan.tag("payment.id", payment.getId());
            newSpan.tag("payment.amount", payment.getAmount().toString());

            // 이벤트 기록
            newSpan.event("Payment validation started");

            validatePayment(payment);

            newSpan.event("Payment processing completed");

        } catch (Exception e) {
            // 에러 기록
            newSpan.error(e);
            throw e;
        } finally {
            newSpan.end();
        }
    }
}
```

---

## 7. Kafka Tracing

Kafka 메시지에도 Trace Context를 전파할 수 있습니다.

### 의존성 추가

```kotlin
dependencies {
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.brave:brave-instrumentation-kafka-clients")
}
```

### Kafka Producer 설정

```java
@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, String> producerFactory(Tracing tracing) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:29092");
        // ...

        DefaultKafkaProducerFactory<String, String> factory =
            new DefaultKafkaProducerFactory<>(configs);

        // Tracing 데코레이터 추가
        factory.addPostProcessor(new TracingProducerPostProcessor<>(tracing));

        return factory;
    }
}
```

---

## 8. Zipkin UI 사용법

### 접속 URL

| 환경 | URL |
|------|-----|
| Docker Compose | http://localhost:9411 |
| Kubernetes | http://portal-universe/zipkin |

### 주요 기능

#### Trace 검색

```
1. Service Name 선택 (예: auth-service)
2. Operation Name 선택 (예: POST /api/v1/login)
3. Tags 필터 (예: http.status_code=500)
4. Time Range 설정
5. "Find Traces" 클릭
```

#### Trace 상세 보기

```
┌────────────────────────────────────────────────────────────────────┐
│  Trace View: abc123def456                                           │
│                                                                     │
│  ┌─ api-gateway ─────────────────────────────────────────┐ 150ms   │
│  │  POST /api/v1/orders                                   │         │
│  │                                                        │         │
│  │  ┌─ auth-service ───────────────────┐ 40ms            │         │
│  │  │  GET /api/v1/users/123           │                 │         │
│  │  └──────────────────────────────────┘                 │         │
│  │                                                        │         │
│  │  ┌─ shopping-service ─────────────────────────┐ 80ms  │         │
│  │  │  POST /api/v1/orders                        │       │         │
│  │  │                                             │       │         │
│  │  │  ┌─ notification-service ─────┐ 20ms       │       │         │
│  │  │  │  POST /api/v1/notify       │            │       │         │
│  │  │  └────────────────────────────┘            │       │         │
│  │  └────────────────────────────────────────────┘       │         │
│  └────────────────────────────────────────────────────────┘         │
└────────────────────────────────────────────────────────────────────┘
```

### Dependency Graph

서비스 간 의존성을 시각적으로 확인할 수 있습니다.

```
  api-gateway
       │
       ├───────────────┬───────────────┐
       ▼               ▼               ▼
  auth-service   blog-service   shopping-service
                                       │
                                       ▼
                              notification-service
```

---

## 9. 로그와 Trace 연동

### 로그 패턴에 TraceId 포함

```yaml
logging:
  pattern:
    level: "%5p [${spring.application.name},%X{traceId:-},%X{spanId:-}]"
```

### 로그 출력 예시

```
INFO  [auth-service,abc123def456,span789xyz] - Processing login request
INFO  [auth-service,abc123def456,span789xyz] - User authenticated successfully
```

### Grafana Loki 연동

TraceId를 사용하여 Loki에서 관련 로그를 검색할 수 있습니다.

```
{application="auth-service"} |= "abc123def456"
```

---

## 10. 성능 최적화

### 샘플링 설정

```yaml
management:
  tracing:
    sampling:
      probability: 0.1        # 프로덕션: 10% 샘플링
```

### 샘플링 전략

| 전략 | 설정 | 사용 사례 |
|------|------|----------|
| 전체 샘플링 | `probability: 1.0` | 개발/디버깅 |
| 확률 샘플링 | `probability: 0.1` | 프로덕션 |
| Rate Limiting | 커스텀 Sampler | 고트래픽 환경 |

### 커스텀 Sampler

```java
@Bean
public Sampler customSampler() {
    return new Sampler() {
        @Override
        public SamplingDecision shouldSample(SamplingRequest request) {
            // 에러 요청은 항상 샘플링
            if (request.tags().containsKey("error")) {
                return new SamplingDecision(true, Collections.emptyMap());
            }
            // 나머지는 10% 확률
            return new SamplingDecision(
                Math.random() < 0.1,
                Collections.emptyMap()
            );
        }
    };
}
```

---

## 11. 관련 문서

- [Prometheus & Grafana](./prometheus-grafana.md) - 메트릭 모니터링
- [Loki Logging](./loki-logging.md) - 로그 수집
- [Portal Universe Infra Guide](./portal-universe-infra-guide.md) - 전체 인프라 가이드
