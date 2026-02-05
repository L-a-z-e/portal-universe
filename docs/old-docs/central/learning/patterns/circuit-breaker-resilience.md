# Circuit Breaker 및 Resilience 패턴

## 학습 목표
- Circuit Breaker 패턴의 원리와 상태 전이 이해
- Resilience4j를 활용한 구현 방법 습득
- Fallback 전략 설계 및 구현
- Portal Universe 마이크로서비스에서의 적용

---

## 1. Circuit Breaker 개요

### 1.1 왜 Circuit Breaker가 필요한가?

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    THE PROBLEM: CASCADE FAILURE                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   서비스 간 연쇄 장애:                                                       │
│                                                                             │
│   User ──► Shopping ──► Inventory ──► [Database DOWN!]                      │
│                │                                                            │
│                │ 타임아웃 대기 (30초)                                        │
│                │ 스레드 점유                                                 │
│                │ 리소스 고갈                                                 │
│                ▼                                                            │
│            Shopping도 장애!                                                  │
│                │                                                            │
│                ▼                                                            │
│            User 요청 실패!                                                   │
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                                                                      │  │
│   │   [Inventory DOWN] ──────────────────────────────────────────────►   │  │
│   │         │                                                            │  │
│   │         ▼            전파                      전파                   │  │
│   │   [Shopping 지연] ────────► [Gateway 지연] ────────► [User 실패]     │  │
│   │         │                        │                      │            │  │
│   │         │                        │                      │            │  │
│   │   연쇄적인 서비스 장애 (Cascading Failure)                            │  │
│   │                                                                      │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Circuit Breaker 개념

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       CIRCUIT BREAKER CONCEPT                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   전기 회로 차단기에서 영감:                                                  │
│                                                                             │
│   정상 상태:        [전원] ──●──●── [장치]  (전류 흐름)                       │
│                              ON                                             │
│                                                                             │
│   과부하 감지:      [전원] ──●  ●── [장치]  (차단!)                          │
│                              OFF                                            │
│                                                                             │
│   소프트웨어에서:                                                            │
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                                                                      │  │
│   │   [Service A] ──► [Circuit Breaker] ──► [Service B]                 │  │
│   │                          │                                           │  │
│   │                          │                                           │  │
│   │                    실패율 모니터링                                     │  │
│   │                    임계치 초과 시 차단                                 │  │
│   │                    일정 시간 후 재시도                                 │  │
│   │                                                                      │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.3 Circuit Breaker 상태

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    CIRCUIT BREAKER STATES                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                        ┌─────────────────┐                                  │
│                        │     CLOSED      │                                  │
│                        │   (정상 운영)    │                                  │
│                        └────────┬────────┘                                  │
│                                 │                                           │
│                    실패율 임계치 초과                                        │
│                    (예: 50% 이상 실패)                                       │
│                                 │                                           │
│                                 ▼                                           │
│                        ┌─────────────────┐                                  │
│                        │      OPEN       │                                  │
│                        │   (회로 차단)    │  ←── 즉시 실패 반환              │
│                        └────────┬────────┘                                  │
│                                 │                                           │
│                    대기 시간 경과                                            │
│                    (예: 60초)                                               │
│                                 │                                           │
│                                 ▼                                           │
│                        ┌─────────────────┐                                  │
│                        │   HALF_OPEN     │                                  │
│                        │  (부분 테스트)   │  ←── 제한된 요청 허용            │
│                        └────────┬────────┘                                  │
│                                 │                                           │
│                    ┌────────────┴────────────┐                              │
│                    │                         │                              │
│               성공률 충족               실패 발생                            │
│                    │                         │                              │
│                    ▼                         ▼                              │
│               CLOSED로                   OPEN으로                           │
│               전이                       전이                               │
│                                                                             │
│   상태 요약:                                                                 │
│   ┌──────────┬──────────────────────────────────────────────────────────┐  │
│   │ CLOSED   │ 정상 운영. 모든 요청 통과. 실패율 모니터링               │  │
│   │ OPEN     │ 회로 차단. 모든 요청 즉시 실패. 대기 시간 카운트         │  │
│   │ HALF_OPEN│ 테스트 모드. 일부 요청만 통과. 결과에 따라 전이          │  │
│   └──────────┴──────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Resilience4j 소개

### 2.1 Resilience4j란?

Resilience4j는 Java 8+ 함수형 프로그래밍을 위해 설계된 경량 내결함성 라이브러리입니다.

| 모듈 | 설명 |
|------|------|
| **CircuitBreaker** | 회로 차단기 패턴 |
| **RateLimiter** | 요청 속도 제한 |
| **Retry** | 자동 재시도 |
| **Bulkhead** | 리소스 격리 |
| **TimeLimiter** | 타임아웃 처리 |
| **Cache** | 결과 캐싱 |

### 2.2 의존성 설정

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.2.0</version>
</dependency>

<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-circuitbreaker</artifactId>
</dependency>

<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-bulkhead</artifactId>
</dependency>

<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-ratelimiter</artifactId>
</dependency>

<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-timelimiter</artifactId>
</dependency>

<!-- Actuator for metrics -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-micrometer</artifactId>
</dependency>
```

---

## 3. Circuit Breaker 구현

### 3.1 설정

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    configs:
      default:
        # 슬라이딩 윈도우 설정
        slidingWindowType: COUNT_BASED      # COUNT_BASED 또는 TIME_BASED
        slidingWindowSize: 10               # 최근 10개 호출 기준
        minimumNumberOfCalls: 5             # 최소 5번 호출 후 평가

        # 실패율 임계치
        failureRateThreshold: 50            # 50% 이상 실패 시 OPEN
        slowCallRateThreshold: 100          # 느린 호출 비율 임계치
        slowCallDurationThreshold: 2s       # 2초 이상이면 느린 호출

        # OPEN 상태 대기 시간
        waitDurationInOpenState: 60s        # 60초 후 HALF_OPEN

        # HALF_OPEN 설정
        permittedNumberOfCallsInHalfOpenState: 3  # 테스트 호출 수
        automaticTransitionFromOpenToHalfOpenEnabled: true

        # 기록할 예외
        recordExceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
          - org.springframework.web.client.HttpServerErrorException

        # 무시할 예외 (실패로 카운트하지 않음)
        ignoreExceptions:
          - com.portal.exception.BusinessException

    instances:
      inventoryService:
        baseConfig: default
        failureRateThreshold: 60            # 인스턴스별 오버라이드

      paymentService:
        baseConfig: default
        waitDurationInOpenState: 30s
        slidingWindowSize: 20
```

### 3.2 어노테이션 기반 구현

```java
/**
 * Inventory Service Client with Circuit Breaker
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceClient {

    private final RestClient restClient;

    /**
     * 재고 조회 - Circuit Breaker 적용
     */
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "getStockFallback")
    public StockInfo getStock(String productId) {
        log.debug("Calling inventory service for product: {}", productId);

        return restClient.get()
            .uri("/api/v1/inventory/{productId}/stock", productId)
            .retrieve()
            .body(StockInfo.class);
    }

    /**
     * Fallback 메서드 - 원본 메서드와 동일한 파라미터 + Exception
     */
    private StockInfo getStockFallback(String productId, Exception e) {
        log.warn("Circuit breaker fallback for product: {}, error: {}",
                 productId, e.getMessage());

        // 기본 응답 반환 (서비스 가용성 유지)
        return StockInfo.builder()
            .productId(productId)
            .available(false)
            .quantity(0)
            .message("재고 정보를 일시적으로 확인할 수 없습니다")
            .build();
    }

    /**
     * 재고 차감 - Circuit Breaker + Retry
     */
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "deductStockFallback")
    @Retry(name = "inventoryService", fallbackMethod = "deductStockFallback")
    public DeductResult deductStock(String productId, int quantity) {
        return restClient.post()
            .uri("/api/v1/inventory/{productId}/deduct", productId)
            .body(new DeductRequest(quantity))
            .retrieve()
            .body(DeductResult.class);
    }

    private DeductResult deductStockFallback(String productId, int quantity, Exception e) {
        log.error("Failed to deduct stock for product: {}, qty: {}, error: {}",
                  productId, quantity, e.getMessage());

        // 재고 차감 실패는 중요 비즈니스 로직이므로 예외 발생
        throw new CustomBusinessException(
            ShoppingErrorCode.INVENTORY_SERVICE_UNAVAILABLE,
            "재고 차감에 실패했습니다. 잠시 후 다시 시도해주세요."
        );
    }
}
```

### 3.3 프로그래밍 방식 구현

```java
/**
 * 프로그래밍 방식 Circuit Breaker 구현
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceClient {

    private final RestClient restClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * 결제 처리 - 프로그래밍 방식
     */
    public PaymentResult processPayment(PaymentRequest request) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry
            .circuitBreaker("paymentService");

        // 상태 로깅
        log.info("Circuit breaker state: {}", circuitBreaker.getState());

        return circuitBreaker.executeSupplier(() -> {
            return restClient.post()
                .uri("/api/v1/payments/process")
                .body(request)
                .retrieve()
                .body(PaymentResult.class);
        });
    }

    /**
     * Circuit Breaker + Fallback 조합
     */
    public PaymentResult processPaymentWithFallback(PaymentRequest request) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry
            .circuitBreaker("paymentService");

        Supplier<PaymentResult> decoratedSupplier = CircuitBreaker
            .decorateSupplier(circuitBreaker, () -> callPaymentService(request));

        return Try.ofSupplier(decoratedSupplier)
            .recover(CallNotPermittedException.class,
                     e -> handleCircuitOpen(request, e))
            .recover(Exception.class,
                     e -> handlePaymentError(request, e))
            .get();
    }

    private PaymentResult callPaymentService(PaymentRequest request) {
        return restClient.post()
            .uri("/api/v1/payments/process")
            .body(request)
            .retrieve()
            .body(PaymentResult.class);
    }

    private PaymentResult handleCircuitOpen(PaymentRequest request, Exception e) {
        log.warn("Circuit is open, payment service unavailable");
        return PaymentResult.pending(request.getOrderId(),
            "결제 시스템 점검 중입니다. 잠시 후 자동으로 처리됩니다.");
    }

    private PaymentResult handlePaymentError(PaymentRequest request, Exception e) {
        log.error("Payment failed: {}", e.getMessage());
        return PaymentResult.failed(request.getOrderId(),
            "결제 처리 중 오류가 발생했습니다.");
    }
}
```

### 3.4 이벤트 리스너

```java
/**
 * Circuit Breaker 이벤트 모니터링
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CircuitBreakerEventListener {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final MeterRegistry meterRegistry;
    private final NotificationService notificationService;

    @PostConstruct
    public void registerEventConsumers() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            cb.getEventPublisher()
                .onStateTransition(this::onStateTransition)
                .onError(this::onError)
                .onSuccess(this::onSuccess)
                .onCallNotPermitted(this::onCallNotPermitted);
        });
    }

    private void onStateTransition(CircuitBreakerOnStateTransitionEvent event) {
        String name = event.getCircuitBreakerName();
        StateTransition transition = event.getStateTransition();

        log.warn("Circuit Breaker [{}] state changed: {} -> {}",
                name,
                transition.getFromState(),
                transition.getToState());

        // OPEN 상태 전이 시 알림
        if (transition.getToState() == CircuitBreaker.State.OPEN) {
            notificationService.sendAlert(
                AlertLevel.WARNING,
                "Circuit Breaker OPEN",
                String.format("서비스 [%s]의 Circuit Breaker가 OPEN 상태로 전환되었습니다.", name)
            );
        }

        // CLOSED 상태 복귀 시 알림
        if (transition.getToState() == CircuitBreaker.State.CLOSED &&
            transition.getFromState() == CircuitBreaker.State.HALF_OPEN) {
            notificationService.sendAlert(
                AlertLevel.INFO,
                "Circuit Breaker CLOSED",
                String.format("서비스 [%s]가 정상 복구되었습니다.", name)
            );
        }

        // 메트릭 기록
        meterRegistry.counter("circuit_breaker.state_transition",
            "name", name,
            "from", transition.getFromState().name(),
            "to", transition.getToState().name()
        ).increment();
    }

    private void onError(CircuitBreakerOnErrorEvent event) {
        log.debug("Circuit Breaker [{}] recorded error: {}",
                 event.getCircuitBreakerName(),
                 event.getThrowable().getMessage());
    }

    private void onSuccess(CircuitBreakerOnSuccessEvent event) {
        log.trace("Circuit Breaker [{}] recorded success, duration: {}ms",
                 event.getCircuitBreakerName(),
                 event.getElapsedDuration().toMillis());
    }

    private void onCallNotPermitted(CircuitBreakerOnCallNotPermittedEvent event) {
        log.warn("Circuit Breaker [{}] rejected call (state: OPEN)",
                event.getCircuitBreakerName());
    }
}
```

---

## 4. Fallback 전략

### 4.1 Fallback 패턴 유형

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       FALLBACK STRATEGIES                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   1. 기본값 반환 (Default Value)                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │  return StockInfo.builder()                                          │  │
│   │      .available(false)                                               │  │
│   │      .message("서비스 점검 중")                                       │  │
│   │      .build();                                                       │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   2. 캐시 데이터 사용 (Cached Data)                                          │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │  return cacheService.getLastKnownStock(productId)                    │  │
│   │      .orElse(StockInfo.unknown());                                   │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   3. 대체 서비스 호출 (Alternative Service)                                   │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │  return backupInventoryService.getStock(productId);                  │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   4. 지연 처리 (Deferred Processing)                                         │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │  eventPublisher.publish(new DeferredPaymentEvent(request));          │  │
│   │  return PaymentResult.pending("나중에 처리됩니다");                    │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   5. 예외 발생 (Fail Fast)                                                   │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │  throw new ServiceUnavailableException("결제 서비스 장애");          │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 계층적 Fallback 구현

```java
/**
 * 계층적 Fallback 전략
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductInfoService {

    private final ProductServiceClient productClient;
    private final CacheService cacheService;
    private final BackupProductClient backupClient;

    @CircuitBreaker(name = "productService", fallbackMethod = "getProductFromCache")
    public ProductInfo getProduct(String productId) {
        log.debug("Fetching product from primary service: {}", productId);
        ProductInfo product = productClient.getProduct(productId);

        // 성공 시 캐시 업데이트
        cacheService.cacheProduct(productId, product);
        return product;
    }

    /**
     * Fallback Level 1: 캐시에서 조회
     */
    private ProductInfo getProductFromCache(String productId, Exception e) {
        log.warn("Primary service failed, trying cache: {}", e.getMessage());

        return cacheService.getCachedProduct(productId)
            .orElseGet(() -> getProductFromBackup(productId, e));
    }

    /**
     * Fallback Level 2: 백업 서비스
     */
    private ProductInfo getProductFromBackup(String productId, Exception originalError) {
        log.warn("Cache miss, trying backup service for: {}", productId);

        try {
            return backupClient.getProduct(productId);
        } catch (Exception backupError) {
            log.error("Backup service also failed: {}", backupError.getMessage());
            return getProductStub(productId);
        }
    }

    /**
     * Fallback Level 3: 기본 응답
     */
    private ProductInfo getProductStub(String productId) {
        log.warn("All fallbacks exhausted, returning stub for: {}", productId);

        return ProductInfo.builder()
            .productId(productId)
            .name("상품 정보를 불러올 수 없습니다")
            .available(false)
            .isStub(true)
            .build();
    }
}
```

### 4.3 Feign Client + Circuit Breaker

```java
/**
 * Feign Client with Circuit Breaker
 */
@FeignClient(
    name = "auth-service",
    fallbackFactory = AuthServiceFallbackFactory.class
)
public interface AuthServiceClient {

    @GetMapping("/api/v1/users/{userId}")
    UserInfo getUser(@PathVariable String userId);

    @PostMapping("/api/v1/auth/validate")
    TokenValidationResult validateToken(@RequestBody String token);
}

/**
 * Fallback Factory - 예외 정보 접근 가능
 */
@Component
@Slf4j
public class AuthServiceFallbackFactory implements FallbackFactory<AuthServiceClient> {

    @Override
    public AuthServiceClient create(Throwable cause) {
        return new AuthServiceClient() {

            @Override
            public UserInfo getUser(String userId) {
                log.warn("Auth service unavailable, error: {}", cause.getMessage());

                // 캐시된 사용자 정보 또는 기본값 반환
                return UserInfo.builder()
                    .userId(userId)
                    .username("Unknown")
                    .authenticated(false)
                    .build();
            }

            @Override
            public TokenValidationResult validateToken(String token) {
                log.error("Token validation failed due to service error: {}",
                         cause.getMessage());

                // 토큰 검증 실패는 보안상 예외 발생
                if (cause instanceof CircuitBreakerException) {
                    throw new ServiceUnavailableException(
                        "인증 서비스가 일시적으로 불가합니다"
                    );
                }

                return TokenValidationResult.invalid("서비스 오류");
            }
        };
    }
}
```

---

## 5. Retry 패턴

### 5.1 Retry 설정

```yaml
resilience4j:
  retry:
    configs:
      default:
        maxAttempts: 3                      # 최대 3회 시도
        waitDuration: 500ms                 # 재시도 간격
        enableExponentialBackoff: true      # 지수 백오프
        exponentialBackoffMultiplier: 2     # 500ms -> 1s -> 2s
        retryExceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
        ignoreExceptions:
          - com.portal.exception.BusinessException

    instances:
      inventoryService:
        baseConfig: default
        maxAttempts: 5

      paymentService:
        baseConfig: default
        maxAttempts: 2                      # 결제는 빠른 실패
        waitDuration: 1s
```

### 5.2 Retry 구현

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceClient {

    private final RestClient restClient;

    /**
     * Retry + CircuitBreaker 조합
     * 순서: Retry -> CircuitBreaker (Retry가 외부에서 CircuitBreaker를 감싸)
     */
    @Retry(name = "inventoryService")
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "reserveStockFallback")
    public ReservationResult reserveStock(String productId, int quantity) {
        log.info("Attempting to reserve stock: product={}, qty={}", productId, quantity);

        return restClient.post()
            .uri("/api/v1/inventory/{productId}/reserve", productId)
            .body(new ReserveRequest(quantity))
            .retrieve()
            .body(ReservationResult.class);
    }

    private ReservationResult reserveStockFallback(String productId, int quantity,
                                                   Exception e) {
        log.error("Stock reservation failed after retries: product={}, error={}",
                 productId, e.getMessage());

        return ReservationResult.failed(productId,
            "재고 예약에 실패했습니다. 다시 시도해주세요.");
    }
}
```

---

## 6. Portal Universe 적용

### 6.1 서비스 간 통신 구조

```
┌─────────────────────────────────────────────────────────────────────────────┐
│              PORTAL UNIVERSE - RESILIENCE ARCHITECTURE                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                          ┌─────────────────┐                                │
│                          │   API Gateway   │                                │
│                          │ (Circuit Breaker│                                │
│                          │  per service)   │                                │
│                          └────────┬────────┘                                │
│                                   │                                         │
│         ┌─────────────────────────┼─────────────────────────┐               │
│         │                         │                         │               │
│         ▼                         ▼                         ▼               │
│   ┌───────────┐           ┌───────────┐           ┌───────────┐            │
│   │ Shopping  │──CB+Retry─│ Inventory │           │   Auth    │            │
│   │ Service   │           │ Service   │           │ Service   │            │
│   └─────┬─────┘           └───────────┘           └───────────┘            │
│         │                                                                   │
│         │ CB                                                                │
│         ▼                                                                   │
│   ┌───────────┐                                                            │
│   │ Payment   │                                                            │
│   │ Service   │  ← Timeout + Retry                                         │
│   └───────────┘                                                            │
│                                                                             │
│   CB = Circuit Breaker                                                      │
│                                                                             │
│   각 서비스 간 통신에 Resilience4j 적용:                                     │
│   • Shopping → Inventory: Circuit Breaker + Retry                          │
│   • Shopping → Payment: Circuit Breaker + TimeLimiter                      │
│   • Shopping → Auth: Circuit Breaker (Feign)                               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 Shopping Service 설정

```yaml
# services/shopping-service/src/main/resources/application.yml

resilience4j:
  circuitbreaker:
    instances:
      inventoryService:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 3
        recordExceptions:
          - java.io.IOException
          - feign.FeignException.ServiceUnavailable
          - feign.RetryableException

      paymentService:
        slidingWindowType: TIME_BASED
        slidingWindowSize: 60                 # 60초 기준
        minimumNumberOfCalls: 10
        failureRateThreshold: 40              # 결제는 더 민감하게
        slowCallRateThreshold: 50
        slowCallDurationThreshold: 3s
        waitDurationInOpenState: 60s

      authService:
        slidingWindowSize: 20
        failureRateThreshold: 60
        waitDurationInOpenState: 10s          # 인증은 빠른 복구

  retry:
    instances:
      inventoryService:
        maxAttempts: 3
        waitDuration: 500ms
        enableExponentialBackoff: true

      paymentService:
        maxAttempts: 2
        waitDuration: 1s

  timelimiter:
    instances:
      paymentService:
        timeoutDuration: 5s
        cancelRunningFuture: true

# Actuator endpoints for monitoring
management:
  endpoints:
    web:
      exposure:
        include: health,circuitbreakers,metrics
  health:
    circuitbreakers:
      enabled: true
```

### 6.3 구현 예시

```java
/**
 * Shopping Service - Order Creation with Resilience
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final InventoryServiceClient inventoryClient;
    private final PaymentServiceClient paymentClient;
    private final AuthServiceClient authClient;
    private final OrderRepository orderRepository;

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        // 1. 사용자 인증 확인 (Circuit Breaker)
        UserInfo user = authClient.getUser(request.getUserId());
        if (!user.isAuthenticated()) {
            throw new CustomBusinessException(AuthErrorCode.UNAUTHORIZED);
        }

        // 2. 재고 확인 및 예약 (Circuit Breaker + Retry)
        for (OrderItemRequest item : request.getItems()) {
            ReservationResult result = inventoryClient.reserveStock(
                item.getProductId(),
                item.getQuantity()
            );

            if (!result.isSuccess()) {
                throw new CustomBusinessException(
                    ShoppingErrorCode.STOCK_RESERVATION_FAILED,
                    result.getMessage()
                );
            }
        }

        // 3. 주문 생성
        Order order = Order.create(request, user);
        orderRepository.save(order);

        // 4. 결제 처리 (Circuit Breaker + TimeLimiter)
        try {
            PaymentResult paymentResult = paymentClient.processPayment(
                PaymentRequest.from(order)
            );

            if (paymentResult.isSuccess()) {
                order.confirmPayment(paymentResult.getTransactionId());
            } else if (paymentResult.isPending()) {
                order.markPaymentPending();
            } else {
                // 결제 실패 시 재고 예약 취소
                compensateStockReservation(request.getItems());
                throw new CustomBusinessException(
                    ShoppingErrorCode.PAYMENT_FAILED
                );
            }
        } catch (ServiceUnavailableException e) {
            // 결제 서비스 장애 시 비동기 처리로 전환
            order.markPaymentPending();
            scheduleAsyncPayment(order);
        }

        orderRepository.save(order);
        return CreateOrderResponse.from(order);
    }

    private void compensateStockReservation(List<OrderItemRequest> items) {
        for (OrderItemRequest item : items) {
            try {
                inventoryClient.releaseReservation(
                    item.getProductId(),
                    item.getQuantity()
                );
            } catch (Exception e) {
                // 보상 실패는 로깅하고 이후 배치 처리
                log.error("Failed to release stock reservation: {}", e.getMessage());
            }
        }
    }

    @Async
    protected void scheduleAsyncPayment(Order order) {
        // 비동기 결제 처리 스케줄링
    }
}
```

---

## 7. 모니터링

### 7.1 Actuator Endpoints

```bash
# Circuit Breaker 상태 조회
curl http://localhost:8080/actuator/health/circuitbreakers

# 결과 예시
{
  "status": "UP",
  "details": {
    "inventoryService": {
      "status": "UP",
      "details": {
        "state": "CLOSED",
        "failureRate": "0.0%",
        "slowCallRate": "0.0%",
        "bufferedCalls": 10,
        "failedCalls": 0,
        "slowCalls": 0,
        "notPermittedCalls": 0
      }
    },
    "paymentService": {
      "status": "CIRCUIT_OPEN",
      "details": {
        "state": "OPEN",
        "failureRate": "60.0%",
        "bufferedCalls": 10,
        "failedCalls": 6
      }
    }
  }
}

# Circuit Breaker 이벤트 조회
curl http://localhost:8080/actuator/circuitbreakerevents
```

### 7.2 메트릭 대시보드

```java
/**
 * Prometheus 메트릭 예시
 */
// resilience4j_circuitbreaker_state{name="inventoryService"} 0  // CLOSED
// resilience4j_circuitbreaker_state{name="paymentService"} 1    // OPEN

// resilience4j_circuitbreaker_calls_total{name="inventoryService",kind="successful"} 150
// resilience4j_circuitbreaker_calls_total{name="inventoryService",kind="failed"} 5

// resilience4j_circuitbreaker_failure_rate{name="inventoryService"} 3.2
```

---

## 8. 모범 사례

### 8.1 설정 가이드라인

| 설정 | 권장값 | 설명 |
|------|-------|------|
| `slidingWindowSize` | 10-100 | 트래픽에 따라 조정 |
| `failureRateThreshold` | 50% | 서비스 중요도에 따라 |
| `waitDurationInOpenState` | 30-60s | 복구 시간 고려 |
| `permittedNumberOfCallsInHalfOpenState` | 3-10 | 테스트 충분히 |

### 8.2 주의사항

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    CIRCUIT BREAKER BEST PRACTICES                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   DO:                                                                       │
│   ✓ 서비스별로 별도의 Circuit Breaker 인스턴스 사용                          │
│   ✓ 비즈니스 예외는 ignoreExceptions에 추가                                  │
│   ✓ 적절한 Fallback 전략 구현                                               │
│   ✓ 메트릭 모니터링 및 알림 설정                                             │
│   ✓ 부하 테스트로 임계치 검증                                                │
│                                                                             │
│   DON'T:                                                                    │
│   ✗ 모든 서비스에 동일한 설정 사용                                           │
│   ✗ Fallback 없이 Circuit Breaker만 사용                                    │
│   ✗ 너무 낮은 임계치 (불필요한 차단)                                         │
│   ✗ 너무 긴 waitDuration (복구 지연)                                        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 9. 연관 패턴

| 패턴 | 관계 |
|------|------|
| **Bulkhead** | 리소스 격리와 함께 사용 |
| **Timeout** | Circuit Breaker와 함께 사용 권장 |
| **Retry** | Circuit Breaker 전에 적용 |
| **Fallback** | Circuit Breaker의 필수 동반자 |

---

## 10. 참고 자료

- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Martin Fowler - Circuit Breaker](https://martinfowler.com/bliki/CircuitBreaker.html)
- [Release It! - Michael Nygard](https://pragprog.com/titles/mnee2/release-it-second-edition/)
- Portal Universe: `services/shopping-service/` 예제 참조
