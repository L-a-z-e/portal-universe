# Bulkhead 패턴

## 학습 목표
- Bulkhead 패턴의 원리와 필요성 이해
- 리소스 격리 전략과 스레드풀 분리 학습
- Resilience4j Bulkhead 및 Rate Limiter 구현
- Portal Universe에서의 적용 방안 분석

---

## 1. Bulkhead 패턴 개요

### 1.1 비유: 선박의 격벽 (Bulkhead)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    BULKHEAD METAPHOR                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   선박의 격벽 (Bulkhead):                                                    │
│   ┌───────────────────────────────────────────────────────────────────┐    │
│   │                                                                    │    │
│   │   ┌────────┬────────┬────────┬────────┬────────┐                  │    │
│   │   │ 구역 1 │ 구역 2 │ 구역 3 │ 구역 4 │ 구역 5 │  ← 격벽으로 분리  │    │
│   │   │        │ 침수!  │        │        │        │                  │    │
│   │   │        │ ~~~~   │        │        │        │                  │    │
│   │   └────────┴────────┴────────┴────────┴────────┘                  │    │
│   │                                                                    │    │
│   │   한 구역이 침수되어도 다른 구역은 안전!                             │    │
│   │                                                                    │    │
│   └───────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│   소프트웨어에서의 Bulkhead:                                                 │
│   ┌───────────────────────────────────────────────────────────────────┐    │
│   │                                                                    │    │
│   │   ┌────────────┐  ┌────────────┐  ┌────────────┐                  │    │
│   │   │ Thread Pool│  │ Thread Pool│  │ Thread Pool│  ← 리소스 격리   │    │
│   │   │ Service A  │  │ Service B  │  │ Service C  │                  │    │
│   │   │ [10 threads]  │ [EXHAUSTED]│  │ [10 threads]                  │    │
│   │   └────────────┘  └────────────┘  └────────────┘                  │    │
│   │                                                                    │    │
│   │   Service B의 스레드가 고갈되어도 A, C는 정상 동작!                  │    │
│   │                                                                    │    │
│   └───────────────────────────────────────────────────────────────────┘    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 왜 Bulkhead가 필요한가?

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    THE PROBLEM: RESOURCE EXHAUSTION                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   공유 리소스 사용 시 문제:                                                   │
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                     Shared Thread Pool (100 threads)                │  │
│   │   ┌──────────────────────────────────────────────────────────────┐  │  │
│   │   │ ████████████████████████████████████████████████████████████ │  │  │
│   │   │ Service A   │  Service B (SLOW!)   │ Service C │ Service D   │  │  │
│   │   │    10개     │       80개 점유!      │   5개    │   5개       │  │  │
│   │   └──────────────────────────────────────────────────────────────┘  │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   Service B가 느려지면:                                                      │
│   • B 요청이 스레드를 오래 점유                                              │
│   • 다른 서비스(A, C, D)의 요청도 대기                                       │
│   • 전체 시스템 응답 지연                                                    │
│   • 결국 모든 서비스 장애 (!)                                                │
│                                                                             │
│   Bulkhead 적용 후:                                                          │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                                                                      │  │
│   │   ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐           │  │
│   │   │ Pool A   │  │ Pool B   │  │ Pool C   │  │ Pool D   │           │  │
│   │   │ 25 thrds │  │ 25 thrds │  │ 25 thrds │  │ 25 thrds │           │  │
│   │   │ ████░░░  │  │ ████████ │  │ ███░░░░  │  │ ██░░░░░  │           │  │
│   │   │ (정상)   │  │ (FULL!)  │  │ (정상)   │  │ (정상)   │           │  │
│   │   └──────────┘  └──────────┘  └──────────┘  └──────────┘           │  │
│   │                                                                      │  │
│   │   Service B의 풀만 가득 차고, 다른 서비스는 영향 없음!                │  │
│   │                                                                      │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.3 Bulkhead 유형

| 유형 | 설명 | 적합한 상황 |
|------|------|-----------|
| **Thread Pool** | 서비스별 전용 스레드풀 | 블로킹 I/O 작업 |
| **Semaphore** | 동시 실행 수 제한 | 논블로킹, 가벼운 격리 |
| **Connection Pool** | DB/HTTP 연결 풀 분리 | 외부 시스템 연결 |
| **Process/Container** | 별도 프로세스/컨테이너 | 완전한 격리 필요 시 |

---

## 2. Resilience4j Bulkhead 구현

### 2.1 의존성 설정

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.2.0</version>
</dependency>

<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-bulkhead</artifactId>
</dependency>
```

### 2.2 Semaphore Bulkhead 설정

```yaml
# application.yml
resilience4j:
  bulkhead:
    configs:
      default:
        maxConcurrentCalls: 25           # 최대 동시 호출 수
        maxWaitDuration: 500ms           # 대기 시간

    instances:
      inventoryService:
        baseConfig: default
        maxConcurrentCalls: 20           # 재고 서비스 전용

      paymentService:
        baseConfig: default
        maxConcurrentCalls: 10           # 결제는 더 제한적
        maxWaitDuration: 1s              # 대기 시간 증가

      authService:
        maxConcurrentCalls: 50           # 인증은 빈번하므로 넉넉히
        maxWaitDuration: 100ms
```

### 2.3 Thread Pool Bulkhead 설정

```yaml
resilience4j:
  thread-pool-bulkhead:
    configs:
      default:
        maxThreadPoolSize: 10             # 최대 스레드 수
        coreThreadPoolSize: 5             # 코어 스레드 수
        queueCapacity: 100                # 대기 큐 크기
        keepAliveDuration: 20ms           # 유휴 스레드 유지 시간
        writableStackTraceEnabled: true

    instances:
      inventoryService:
        baseConfig: default
        maxThreadPoolSize: 15
        coreThreadPoolSize: 10
        queueCapacity: 50

      paymentService:
        maxThreadPoolSize: 5              # 결제는 제한적
        coreThreadPoolSize: 3
        queueCapacity: 20

      externalApiService:
        maxThreadPoolSize: 20             # 외부 API는 넉넉히
        coreThreadPoolSize: 10
        queueCapacity: 200
```

### 2.4 어노테이션 기반 구현

```java
/**
 * Inventory Service Client with Bulkhead
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceClient {

    private final RestClient restClient;

    /**
     * Semaphore Bulkhead 적용
     */
    @Bulkhead(name = "inventoryService", fallbackMethod = "getStockFallback")
    public StockInfo getStock(String productId) {
        log.debug("Checking stock for product: {}", productId);

        return restClient.get()
            .uri("/api/v1/inventory/{productId}/stock", productId)
            .retrieve()
            .body(StockInfo.class);
    }

    /**
     * Thread Pool Bulkhead 적용 (비동기)
     */
    @Bulkhead(name = "inventoryService",
              type = Bulkhead.Type.THREADPOOL,
              fallbackMethod = "getStockAsyncFallback")
    public CompletableFuture<StockInfo> getStockAsync(String productId) {
        return CompletableFuture.supplyAsync(() ->
            restClient.get()
                .uri("/api/v1/inventory/{productId}/stock", productId)
                .retrieve()
                .body(StockInfo.class)
        );
    }

    /**
     * Bulkhead + Circuit Breaker 조합
     */
    @Bulkhead(name = "inventoryService")
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "deductStockFallback")
    @Retry(name = "inventoryService")
    public DeductResult deductStock(String productId, int quantity) {
        return restClient.post()
            .uri("/api/v1/inventory/{productId}/deduct", productId)
            .body(new DeductRequest(quantity))
            .retrieve()
            .body(DeductResult.class);
    }

    // Fallback methods
    private StockInfo getStockFallback(String productId, BulkheadFullException e) {
        log.warn("Bulkhead full for inventory service, product: {}", productId);
        return StockInfo.unavailable(productId, "서비스가 혼잡합니다. 잠시 후 다시 시도해주세요.");
    }

    private CompletableFuture<StockInfo> getStockAsyncFallback(String productId, Exception e) {
        log.warn("Async bulkhead fallback for product: {}", productId);
        return CompletableFuture.completedFuture(
            StockInfo.unavailable(productId, "서비스 처리 지연 중")
        );
    }
}
```

### 2.5 프로그래밍 방식 구현

```java
/**
 * 프로그래밍 방식 Bulkhead 사용
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceClient {

    private final BulkheadRegistry bulkheadRegistry;
    private final RestClient restClient;

    public PaymentResult processPayment(PaymentRequest request) {
        Bulkhead bulkhead = bulkheadRegistry.bulkhead("paymentService");

        // 메트릭 로깅
        log.debug("Bulkhead metrics - available: {}, max: {}",
                 bulkhead.getMetrics().getAvailableConcurrentCalls(),
                 bulkhead.getMetrics().getMaxAllowedConcurrentCalls());

        return bulkhead.executeSupplier(() -> doProcessPayment(request));
    }

    /**
     * Thread Pool Bulkhead (비동기)
     */
    public CompletableFuture<PaymentResult> processPaymentAsync(PaymentRequest request) {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.ofDefaults("paymentService");

        return bulkhead.executeSupplier(() -> doProcessPayment(request));
    }

    /**
     * Bulkhead 데코레이터 사용
     */
    public PaymentResult processPaymentWithDecoration(PaymentRequest request) {
        Bulkhead bulkhead = bulkheadRegistry.bulkhead("paymentService");

        Supplier<PaymentResult> decoratedSupplier = Bulkhead
            .decorateSupplier(bulkhead, () -> doProcessPayment(request));

        return Try.ofSupplier(decoratedSupplier)
            .recover(BulkheadFullException.class, e -> {
                log.warn("Payment bulkhead full: {}", e.getMessage());
                return PaymentResult.rejected("서비스가 혼잡합니다");
            })
            .get();
    }

    private PaymentResult doProcessPayment(PaymentRequest request) {
        return restClient.post()
            .uri("/api/v1/payments/process")
            .body(request)
            .retrieve()
            .body(PaymentResult.class);
    }
}
```

---

## 3. Rate Limiter 구현

### 3.1 Rate Limiter 개념

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        RATE LIMITER CONCEPT                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   목적: 일정 시간 내 요청 수 제한                                             │
│                                                                             │
│   시나리오: 초당 10개 요청 제한                                               │
│                                                                             │
│   시간 ──────────────────────────────────────────────────────────►          │
│         │ 1초 │ 1초 │ 1초 │                                                │
│         ├─────┼─────┼─────┤                                                │
│   요청:  │ 15  │  8  │ 12  │                                                │
│   허용:  │ 10  │  8  │ 10  │                                                │
│   거부:  │  5  │  0  │  2  │                                                │
│                                                                             │
│   구현 방식:                                                                 │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                                                                      │  │
│   │   1. Fixed Window                                                    │  │
│   │      [────────10개────────][────────10개────────]                    │  │
│   │                                                                      │  │
│   │   2. Sliding Window                                                  │  │
│   │      [───────────10개───────────]                                    │  │
│   │           [───────────10개───────────]                               │  │
│   │                                                                      │  │
│   │   3. Token Bucket (Resilience4j 기본)                                │  │
│   │      버킷에 주기적으로 토큰 추가, 요청마다 토큰 소비                    │  │
│   │                                                                      │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Rate Limiter 설정

```yaml
resilience4j:
  ratelimiter:
    configs:
      default:
        limitForPeriod: 50                # 주기당 허용 요청 수
        limitRefreshPeriod: 1s            # 주기 (1초)
        timeoutDuration: 500ms            # 대기 타임아웃
        registerHealthIndicator: true

    instances:
      orderCreation:
        baseConfig: default
        limitForPeriod: 10                # 주문 생성은 초당 10개
        limitRefreshPeriod: 1s

      searchApi:
        limitForPeriod: 100               # 검색은 넉넉히
        limitRefreshPeriod: 1s
        timeoutDuration: 0                # 즉시 거부 (대기 없음)

      externalApi:
        limitForPeriod: 5                 # 외부 API는 엄격히
        limitRefreshPeriod: 1s
        timeoutDuration: 5s               # 오래 대기 가능
```

### 3.3 Rate Limiter 구현

```java
/**
 * Rate Limiter 적용
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 주문 생성 - Rate Limiter 적용
     */
    @PostMapping
    @RateLimiter(name = "orderCreation", fallbackMethod = "createOrderFallback")
    public ResponseEntity<ApiResponse<CreateOrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        CreateOrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response));
    }

    /**
     * 검색 - Rate Limiter (높은 제한)
     */
    @GetMapping("/search")
    @RateLimiter(name = "searchApi", fallbackMethod = "searchOrdersFallback")
    public ResponseEntity<ApiResponse<Page<OrderSummary>>> searchOrders(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<OrderSummary> results = orderService.searchOrders(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    // Fallback methods
    private ResponseEntity<ApiResponse<CreateOrderResponse>> createOrderFallback(
            CreateOrderRequest request, RequestNotPermitted e) {

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(ApiResponse.error(
                CommonErrorCode.TOO_MANY_REQUESTS,
                "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."
            ));
    }

    private ResponseEntity<ApiResponse<Page<OrderSummary>>> searchOrdersFallback(
            String keyword, int page, int size, RequestNotPermitted e) {

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(ApiResponse.error(
                CommonErrorCode.TOO_MANY_REQUESTS,
                "검색 요청이 제한되었습니다."
            ));
    }
}
```

### 3.4 사용자별 Rate Limiting

```java
/**
 * 사용자별 Rate Limiter
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserRateLimitService {

    private final RateLimiterRegistry rateLimiterRegistry;

    private static final RateLimiterConfig USER_RATE_CONFIG = RateLimiterConfig.custom()
        .limitForPeriod(100)              // 사용자당 분당 100개
        .limitRefreshPeriod(Duration.ofMinutes(1))
        .timeoutDuration(Duration.ZERO)   // 즉시 거부
        .build();

    /**
     * 사용자별 Rate Limiter 획득
     */
    public RateLimiter getRateLimiterForUser(String userId) {
        String limiterName = "user-" + userId;

        return rateLimiterRegistry.rateLimiter(limiterName, USER_RATE_CONFIG);
    }

    /**
     * 요청 허용 여부 확인
     */
    public boolean isAllowed(String userId) {
        RateLimiter limiter = getRateLimiterForUser(userId);
        return limiter.acquirePermission();
    }

    /**
     * 요청 실행 (Rate Limiter 적용)
     */
    public <T> T executeWithRateLimit(String userId, Supplier<T> supplier) {
        RateLimiter limiter = getRateLimiterForUser(userId);

        return RateLimiter.decorateSupplier(limiter, supplier).get();
    }
}

/**
 * Rate Limiter Interceptor
 */
@Component
@RequiredArgsConstructor
public class RateLimiterInterceptor implements HandlerInterceptor {

    private final UserRateLimitService rateLimitService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                            HttpServletResponse response,
                            Object handler) throws Exception {

        String userId = extractUserId(request);

        if (userId != null && !rateLimitService.isAllowed(userId)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                "{\"error\":\"TOO_MANY_REQUESTS\",\"message\":\"요청 한도를 초과했습니다\"}"
            );
            return false;
        }

        return true;
    }

    private String extractUserId(HttpServletRequest request) {
        // JWT 토큰에서 userId 추출 또는 IP 주소 사용
        return request.getHeader("X-User-Id");
    }
}
```

---

## 4. 스레드풀 분리 전략

### 4.1 서비스별 스레드풀

```java
/**
 * 서비스별 스레드풀 설정
 */
@Configuration
public class ThreadPoolConfiguration {

    /**
     * 기본 비동기 스레드풀
     */
    @Bean("defaultExecutor")
    public Executor defaultExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("default-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 외부 API 호출용 스레드풀
     */
    @Bean("externalApiExecutor")
    public Executor externalApiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("external-api-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 이벤트 처리용 스레드풀
     */
    @Bean("eventExecutor")
    public Executor eventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);  // 이벤트는 큐가 커야 함
        executor.setThreadNamePrefix("event-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 배치/보고서용 스레드풀
     */
    @Bean("batchExecutor")
    public Executor batchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("batch-");
        executor.initialize();
        return executor;
    }
}

/**
 * 스레드풀 사용 예시
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncOrderService {

    @Async("externalApiExecutor")  // 외부 API 스레드풀 사용
    public CompletableFuture<PaymentResult> processPaymentAsync(Order order) {
        log.info("Processing payment on thread: {}", Thread.currentThread().getName());
        // 결제 처리
        return CompletableFuture.completedFuture(/* result */);
    }

    @Async("eventExecutor")  // 이벤트 스레드풀 사용
    public void publishOrderEvent(OrderCreatedEvent event) {
        log.info("Publishing event on thread: {}", Thread.currentThread().getName());
        // 이벤트 발행
    }

    @Async("batchExecutor")  // 배치 스레드풀 사용
    public void generateDailyReport(LocalDate date) {
        log.info("Generating report on thread: {}", Thread.currentThread().getName());
        // 보고서 생성
    }
}
```

### 4.2 HTTP 클라이언트별 연결 풀

```java
/**
 * 서비스별 RestClient 연결 풀 분리
 */
@Configuration
public class RestClientConfiguration {

    /**
     * Inventory Service용 RestClient
     */
    @Bean
    public RestClient inventoryRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
            .executor(Executors.newFixedThreadPool(20))  // 전용 스레드풀
            .connectTimeout(Duration.ofSeconds(5))
            .build();

        return RestClient.builder()
            .baseUrl("http://inventory-service:8080")
            .requestFactory(new JdkClientHttpRequestFactory(httpClient))
            .build();
    }

    /**
     * Payment Service용 RestClient
     */
    @Bean
    public RestClient paymentRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
            .executor(Executors.newFixedThreadPool(10))  // 작은 풀
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        return RestClient.builder()
            .baseUrl("http://payment-service:8080")
            .requestFactory(new JdkClientHttpRequestFactory(httpClient))
            .build();
    }

    /**
     * 외부 API용 RestClient
     */
    @Bean
    public RestClient externalApiRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
            .executor(Executors.newFixedThreadPool(30))
            .connectTimeout(Duration.ofSeconds(15))
            .build();

        return RestClient.builder()
            .baseUrl("https://external-api.example.com")
            .requestFactory(new JdkClientHttpRequestFactory(httpClient))
            .build();
    }
}
```

---

## 5. Portal Universe 적용

### 5.1 Shopping Service Bulkhead 구조

```
┌─────────────────────────────────────────────────────────────────────────────┐
│              PORTAL UNIVERSE - BULKHEAD ARCHITECTURE                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   Shopping Service 리소스 격리:                                              │
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                      Shopping Service                                │  │
│   │                                                                      │  │
│   │   ┌─────────────────────────────────────────────────────────────┐   │  │
│   │   │                   API Layer                                  │   │  │
│   │   │  [Rate Limiter: orderCreation=10/s, search=100/s]           │   │  │
│   │   └─────────────────────────────────────────────────────────────┘   │  │
│   │                           │                                          │  │
│   │         ┌─────────────────┼─────────────────┐                       │  │
│   │         │                 │                 │                       │  │
│   │         ▼                 ▼                 ▼                       │  │
│   │   ┌───────────┐   ┌───────────┐   ┌───────────┐                    │  │
│   │   │ Inventory │   │ Payment   │   │ Auth      │                    │  │
│   │   │ Bulkhead  │   │ Bulkhead  │   │ Bulkhead  │                    │  │
│   │   │ [20 conn] │   │ [10 conn] │   │ [50 conn] │                    │  │
│   │   └─────┬─────┘   └─────┬─────┘   └─────┬─────┘                    │  │
│   │         │               │               │                           │  │
│   │         │               │               │                           │  │
│   │   ┌─────▼─────┐   ┌─────▼─────┐   ┌─────▼─────┐                    │  │
│   │   │ Thread    │   │ Thread    │   │ Thread    │                    │  │
│   │   │ Pool      │   │ Pool      │   │ Pool      │                    │  │
│   │   │ [15 thrds]│   │ [5 thrds] │   │ [10 thrds]│                    │  │
│   │   └───────────┘   └───────────┘   └───────────┘                    │  │
│   │                                                                      │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   장점:                                                                     │
│   • Inventory 서비스 장애 시 Payment, Auth는 영향 없음                       │
│   • 서비스별 적절한 리소스 할당                                              │
│   • Rate Limiter로 과부하 방지                                              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 설정 예시

```yaml
# services/shopping-service/src/main/resources/application.yml

# Bulkhead 설정
resilience4j:
  bulkhead:
    instances:
      inventoryService:
        maxConcurrentCalls: 20
        maxWaitDuration: 500ms

      paymentService:
        maxConcurrentCalls: 10
        maxWaitDuration: 2s

      authService:
        maxConcurrentCalls: 50
        maxWaitDuration: 100ms

  thread-pool-bulkhead:
    instances:
      inventoryService:
        maxThreadPoolSize: 15
        coreThreadPoolSize: 10
        queueCapacity: 50

      paymentService:
        maxThreadPoolSize: 5
        coreThreadPoolSize: 3
        queueCapacity: 20

  ratelimiter:
    instances:
      orderCreation:
        limitForPeriod: 10
        limitRefreshPeriod: 1s
        timeoutDuration: 500ms

      searchApi:
        limitForPeriod: 100
        limitRefreshPeriod: 1s
        timeoutDuration: 0

# Circuit Breaker + Bulkhead 조합
resilience4j:
  circuitbreaker:
    instances:
      inventoryService:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s

      paymentService:
        slidingWindowSize: 10
        failureRateThreshold: 40
        waitDurationInOpenState: 60s

# Actuator 엔드포인트
management:
  endpoints:
    web:
      exposure:
        include: health,bulkheads,ratelimiters,metrics
  health:
    bulkheads:
      enabled: true
    ratelimiters:
      enabled: true
```

### 5.3 통합 구현 예시

```java
/**
 * 모든 Resilience 패턴 통합 적용
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCreationService {

    private final InventoryServiceClient inventoryClient;
    private final PaymentServiceClient paymentClient;
    private final OrderRepository orderRepository;

    /**
     * 주문 생성 - 전체 Resilience 패턴 적용
     *
     * 적용 순서 (외부 → 내부):
     * 1. RateLimiter: 요청 속도 제한
     * 2. Retry: 일시적 실패 재시도
     * 3. CircuitBreaker: 연쇄 장애 방지
     * 4. Bulkhead: 리소스 격리
     */
    @RateLimiter(name = "orderCreation")
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());

        // 1. 재고 확인 (Bulkhead + CircuitBreaker + Retry)
        validateAndReserveStock(request.getItems());

        // 2. 주문 생성
        Order order = Order.create(request);
        orderRepository.save(order);

        // 3. 결제 처리 (Bulkhead + CircuitBreaker)
        processPayment(order);

        return CreateOrderResponse.from(order);
    }

    @Bulkhead(name = "inventoryService", fallbackMethod = "stockCheckFallback")
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "stockCheckFallback")
    @Retry(name = "inventoryService")
    private void validateAndReserveStock(List<OrderItemRequest> items) {
        for (OrderItemRequest item : items) {
            ReservationResult result = inventoryClient.reserveStock(
                item.getProductId(),
                item.getQuantity()
            );

            if (!result.isSuccess()) {
                throw new CustomBusinessException(
                    ShoppingErrorCode.STOCK_RESERVATION_FAILED
                );
            }
        }
    }

    @Bulkhead(name = "paymentService", fallbackMethod = "paymentFallback")
    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    private void processPayment(Order order) {
        PaymentResult result = paymentClient.processPayment(
            PaymentRequest.from(order)
        );

        if (result.isSuccess()) {
            order.confirmPayment(result.getTransactionId());
        } else {
            throw new CustomBusinessException(ShoppingErrorCode.PAYMENT_FAILED);
        }
    }

    // Fallback methods
    private void stockCheckFallback(List<OrderItemRequest> items, Exception e) {
        log.error("Stock check failed: {}", e.getMessage());
        throw new CustomBusinessException(
            ShoppingErrorCode.INVENTORY_SERVICE_UNAVAILABLE
        );
    }

    private void paymentFallback(Order order, Exception e) {
        log.error("Payment processing failed: {}", e.getMessage());
        order.markPaymentPending();
        // 비동기 처리로 전환
    }
}
```

---

## 6. 모니터링

### 6.1 Actuator 엔드포인트

```bash
# Bulkhead 상태 조회
curl http://localhost:8080/actuator/bulkheads

# 결과 예시
{
  "inventoryService": {
    "availableConcurrentCalls": 15,
    "maxAllowedConcurrentCalls": 20
  },
  "paymentService": {
    "availableConcurrentCalls": 8,
    "maxAllowedConcurrentCalls": 10
  }
}

# Rate Limiter 상태 조회
curl http://localhost:8080/actuator/ratelimiters

# 결과 예시
{
  "orderCreation": {
    "availablePermissions": 8,
    "numberOfWaitingThreads": 0
  }
}

# 메트릭 조회
curl http://localhost:8080/actuator/metrics/resilience4j.bulkhead.available.concurrent.calls
```

### 6.2 Prometheus 메트릭

```java
/**
 * Prometheus 메트릭 예시
 */
// Bulkhead
// resilience4j_bulkhead_available_concurrent_calls{name="inventoryService"} 15
// resilience4j_bulkhead_max_allowed_concurrent_calls{name="inventoryService"} 20

// Thread Pool Bulkhead
// resilience4j_thread_pool_bulkhead_current_thread_pool_size{name="inventoryService"} 10
// resilience4j_thread_pool_bulkhead_queue_depth{name="inventoryService"} 5

// Rate Limiter
// resilience4j_ratelimiter_available_permissions{name="orderCreation"} 8
// resilience4j_ratelimiter_waiting_threads{name="orderCreation"} 0
```

---

## 7. 모범 사례

### 7.1 설정 가이드라인

| 패턴 | 설정 팁 |
|------|--------|
| **Semaphore Bulkhead** | 평균 동시 요청의 2배 정도로 설정 |
| **Thread Pool** | CPU 바운드: CPU 코어 수, I/O 바운드: 높게 |
| **Rate Limiter** | 비즈니스 요구사항 기반, 점진적 조정 |
| **maxWaitDuration** | SLA 기반 (응답 시간 고려) |

### 7.2 주의사항

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      BULKHEAD BEST PRACTICES                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   DO:                                                                       │
│   ✓ 서비스 특성에 맞는 격리 방식 선택                                        │
│   ✓ 적절한 모니터링과 알림 설정                                              │
│   ✓ 부하 테스트로 설정값 검증                                                │
│   ✓ Graceful degradation 전략 수립                                          │
│   ✓ Circuit Breaker와 함께 사용                                             │
│                                                                             │
│   DON'T:                                                                    │
│   ✗ 모든 것에 동일한 Bulkhead 설정                                           │
│   ✗ 너무 작은 풀 크기 (불필요한 대기)                                         │
│   ✗ 너무 큰 풀 크기 (격리 효과 감소)                                          │
│   ✗ 무한 대기 (timeoutDuration 없이)                                        │
│                                                                             │
│   Resilience 패턴 적용 순서 (권장):                                          │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │  Request                                                             │  │
│   │     │                                                                │  │
│   │     ▼                                                                │  │
│   │  [Rate Limiter]  ← 1. 요청 속도 제한                                 │  │
│   │     │                                                                │  │
│   │     ▼                                                                │  │
│   │  [Retry]         ← 2. 재시도 (외부에서)                              │  │
│   │     │                                                                │  │
│   │     ▼                                                                │  │
│   │  [Circuit Breaker] ← 3. 연쇄 장애 방지                              │  │
│   │     │                                                                │  │
│   │     ▼                                                                │  │
│   │  [Bulkhead]      ← 4. 리소스 격리                                   │  │
│   │     │                                                                │  │
│   │     ▼                                                                │  │
│   │  [TimeLimiter]   ← 5. 타임아웃                                      │  │
│   │     │                                                                │  │
│   │     ▼                                                                │  │
│   │  Actual Call                                                         │  │
│   │                                                                      │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 8. 연관 패턴

| 패턴 | 관계 |
|------|------|
| **Circuit Breaker** | Bulkhead와 함께 사용하여 완전한 격리 |
| **Timeout** | Bulkhead 내에서 개별 요청 시간 제한 |
| **Retry** | Bulkhead 리소스 내에서 재시도 |
| **Queue-Based Load Leveling** | 요청을 큐잉하여 부하 분산 |

---

## 9. 참고 자료

- [Resilience4j Bulkhead](https://resilience4j.readme.io/docs/bulkhead)
- [Resilience4j Rate Limiter](https://resilience4j.readme.io/docs/ratelimiter)
- [Microsoft - Bulkhead Pattern](https://docs.microsoft.com/en-us/azure/architecture/patterns/bulkhead)
- [Release It! - Michael Nygard](https://pragprog.com/titles/mnee2/release-it-second-edition/)
- Portal Universe: `services/shopping-service/` 예제 참조
