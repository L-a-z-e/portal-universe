---
id: learning-code-review-checklist
title: 코드 리뷰 체크리스트 (Code Review Checklist)
type: learning
status: current
created: 2026-01-22
updated: 2026-01-22
author: Portal Universe Team
tags: [code-review, quality-assurance, best-practices, checklist]
difficulty: intermediate
estimated_time: 1-2 hours
---

# 코드 리뷰 체크리스트 (Code Review Checklist)

## 📋 학습 목표

이 문서를 학습하고 나면 다음을 할 수 있습니다:

- [ ] 코드 리뷰 시 확인해야 할 5가지 핵심 영역을 이해한다
- [ ] 각 영역별 체크리스트를 활용하여 체계적으로 리뷰할 수 있다
- [ ] Portal Universe 프로젝트의 코딩 컨벤션을 이해하고 적용할 수 있다
- [ ] 리뷰 코멘트를 건설적이고 명확하게 작성할 수 있다
- [ ] 자동화 도구를 활용하여 리뷰 효율성을 높일 수 있다

## 📚 사전 지식

- Git Pull Request 기본 개념
- Java/Spring Boot 또는 TypeScript/React/Vue 기본 문법
- 단위 테스트 작성 경험
- REST API 설계 원칙

## ⏱️ 예상 소요 시간

- 체크리스트 학습: 30분
- 예시 분석: 30분
- 실제 PR 리뷰 실습: 1시간

---

## 📐 코드 리뷰의 5가지 핵심 영역

```
┌──────────────────────────────────────────┐
│          Code Review Areas               │
├──────────────────────────────────────────┤
│ 1. 가독성 (Readability)                  │
│ 2. 성능 (Performance)                    │
│ 3. 보안 (Security)                       │
│ 4. 테스트 (Testing)                      │
│ 5. 아키텍처 (Architecture)               │
└──────────────────────────────────────────┘
```

---

## 1️⃣ 가독성 (Readability)

### 📌 개요

코드를 읽는 사람이 빠르게 이해할 수 있는지 확인합니다.

### ✅ 체크리스트

#### 네이밍

- [ ] **변수명이 의도를 명확히 표현하는가?**
  - ❌ `data`, `temp`, `info`, `x`, `flag`
  - ✅ `userProfile`, `orderItems`, `isPaymentCompleted`

- [ ] **메서드명이 동작을 명확히 설명하는가?**
  - ❌ `process()`, `handle()`, `do()`
  - ✅ `validateOrder()`, `calculateTotalPrice()`, `sendNotification()`

- [ ] **클래스명이 책임을 잘 표현하는가?**
  - ❌ `Manager`, `Util`, `Helper`
  - ✅ `OrderService`, `InventoryValidator`, `PriceCalculator`

- [ ] **Boolean 변수/메서드가 is/has/can으로 시작하는가?**
  - ❌ `active`, `permission`, `valid`
  - ✅ `isActive`, `hasPermission`, `canAccess`

#### 메서드 길이

- [ ] **메서드가 50줄 이하인가?**
  - 50줄 초과 시 Extract Method 리팩토링 고려

- [ ] **하나의 메서드가 하나의 책임만 가지는가?** (SRP)
  - "그리고(and)"로 설명되는 메서드는 분리 필요

- [ ] **중첩 깊이가 3단계 이하인가?**
  - ❌ `if { if { if { if { ... } } } }`
  - ✅ Early return, Guard clause 활용

#### 주석

- [ ] **주석 없이도 코드가 이해 가능한가?**
  - 좋은 코드는 스스로 설명한다 (Self-documenting code)

- [ ] **왜(Why)를 설명하는 주석인가?** (무엇(What)이 아닌)
  - ❌ `// 사용자 ID를 가져온다`
  - ✅ `// 동시성 이슈로 인해 분산 락 사용`

- [ ] **주석처리된 코드가 없는가?**
  - Git으로 이력 관리 → 주석처리된 코드 삭제

#### 일관성

- [ ] **프로젝트 코딩 컨벤션을 따르는가?**
  - Portal Universe: `.claude/rules/*.md` 참조

- [ ] **들여쓰기가 일관적인가?** (Spaces vs Tabs)
  - Java: 4 spaces
  - TypeScript/JavaScript: 2 spaces

---

### 📊 Before / After: 가독성 개선

#### ❌ Before: 가독성 낮음

```java
public void p(String u, Long p, int q) {
    var prod = pr.findById(p).orElseThrow();
    if(prod.getStock()>=q){
        var inv=is.r(p,q,"ORDER","ORD-"+System.currentTimeMillis(),u);
        // 재고 예약됨
        log.info("reserved");
    }else{
        // 재고 부족
        throw new RuntimeException("not enough");
    }
}
```

#### ✅ After: 가독성 높음

```java
public void reserveInventoryForOrder(String userId, Long productId, int quantity) {
    Product product = productRepository.findById(productId)
        .orElseThrow(() -> new ProductNotFoundException(productId));

    if (!hasEnoughStock(product, quantity)) {
        throw new InsufficientStockException(productId, quantity);
    }

    String orderNumber = generateOrderNumber();
    inventoryService.reserveStock(productId, quantity, "ORDER", orderNumber, userId);

    log.info("Successfully reserved {} units of product {} for user {}",
        quantity, productId, userId);
}

private boolean hasEnoughStock(Product product, int requestedQuantity) {
    return product.getStock() >= requestedQuantity;
}
```

---

## 2️⃣ 성능 (Performance)

### 📌 개요

코드가 효율적으로 실행되는지, 불필요한 리소스 낭비가 없는지 확인합니다.

### ✅ 체크리스트

#### 데이터베이스

- [ ] **N+1 쿼리 문제가 없는가?**
  - Fetch Join, @EntityGraph 사용 여부 확인

- [ ] **불필요한 전체 조회(SELECT *)를 하지 않는가?**
  - 필요한 컬럼만 조회 (Projection 활용)

- [ ] **적절한 인덱스가 설정되어 있는가?**
  - WHERE, JOIN, ORDER BY 절에 사용되는 컬럼

- [ ] **페이징 처리가 되어 있는가?**
  - 대량 데이터 조회 시 `Pageable` 사용

- [ ] **벌크 연산을 활용하는가?**
  - 다건 INSERT/UPDATE 시 Batch 처리

#### 알고리즘

- [ ] **불필요한 반복문이 없는가?**
  - 중첩 루프(O(n²)) 회피

- [ ] **적절한 자료구조를 사용하는가?**
  - List vs Set vs Map 선택 기준 확인

- [ ] **Early return을 활용하는가?**
  - 불필요한 계산 방지

#### 캐싱

- [ ] **반복 조회되는 데이터를 캐싱하는가?**
  - Redis, Spring Cache 활용

- [ ] **적절한 TTL이 설정되어 있는가?**
  - 데이터 특성에 맞는 만료 시간

#### 동시성

- [ ] **동시성 이슈를 고려했는가?**
  - 분산 락, Optimistic Lock, Pessimistic Lock

- [ ] **트랜잭션 범위가 적절한가?**
  - 최소한의 범위로 제한

---

### 📊 Before / After: 성능 개선

#### ❌ Before: N+1 문제

```java
// Controller
@GetMapping("/orders")
public ApiResponse<List<OrderResponse>> getOrders() {
    return ApiResponse.success(orderService.getAllOrders());
}

// Service
public List<OrderResponse> getAllOrders() {
    List<Order> orders = orderRepository.findAll(); // 1번 쿼리

    return orders.stream()
        .map(order -> {
            // 각 주문마다 상품 조회 → N번 쿼리 발생!
            List<Product> products = order.getItems().stream()
                .map(item -> productRepository.findById(item.getProductId()).orElseThrow())
                .collect(Collectors.toList());

            return OrderResponse.from(order, products);
        })
        .collect(Collectors.toList());
}
```

#### ✅ After: Fetch Join + 페이징

```java
// Controller
@GetMapping("/orders")
public ApiResponse<Page<OrderResponse>> getOrders(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
) {
    Pageable pageable = PageRequest.of(page, size);
    return ApiResponse.success(orderService.getAllOrders(pageable));
}

// Service
public Page<OrderResponse> getAllOrders(Pageable pageable) {
    // Fetch Join으로 N+1 문제 해결
    Page<Order> orders = orderRepository.findAllWithItems(pageable);
    return orders.map(OrderResponse::from);
}

// Repository
@Query("SELECT DISTINCT o FROM Order o " +
       "LEFT JOIN FETCH o.items " +
       "WHERE o.status = 'ACTIVE'")
Page<Order> findAllWithItems(Pageable pageable);
```

**성능 개선**:
- 쿼리 수: `1 + N` → `1`
- 응답 시간: 약 80% 감소
- 페이징 추가로 메모리 사용량 감소

---

## 3️⃣ 보안 (Security)

### 📌 개요

보안 취약점이 없는지, 민감한 정보가 노출되지 않는지 확인합니다.

### ✅ 체크리스트

#### 인증/인가

- [ ] **인증이 필요한 API에 @PreAuthorize가 적용되어 있는가?**
  - ❌ 공개 API와 보호 API 혼재
  - ✅ 명시적인 권한 체크

- [ ] **사용자 ID 검증이 올바른가?**
  - Request의 userId와 JWT의 userId 일치 여부 확인

- [ ] **권한 체크가 비즈니스 로직에서도 수행되는가?**
  - Controller뿐만 아니라 Service 레이어에서도 검증

#### 입력 검증

- [ ] **모든 입력값에 대해 검증이 수행되는가?**
  - `@Valid`, `@NotNull`, `@Size` 등 활용

- [ ] **SQL Injection 방어가 되어 있는가?**
  - Prepared Statement, JPA 사용 (문자열 concat 금지)

- [ ] **XSS 공격 방어가 되어 있는가?**
  - 사용자 입력 HTML 이스케이프 처리

#### 민감 정보

- [ ] **비밀번호가 평문으로 저장되지 않는가?**
  - BCrypt, PBKDF2 등 해싱 알고리즘 사용

- [ ] **로그에 민감 정보가 출력되지 않는가?**
  - 비밀번호, 카드번호, 주민번호 등 마스킹 처리

- [ ] **API 응답에 불필요한 정보가 포함되지 않는가?**
  - DTO로 필요한 필드만 노출

#### 에러 처리

- [ ] **에러 메시지에 시스템 정보가 노출되지 않는가?**
  - ❌ Stack trace, 파일 경로, SQL 쿼리
  - ✅ 사용자 친화적인 에러 메시지

---

### 📊 Before / After: 보안 개선

#### ❌ Before: 보안 취약

```java
@GetMapping("/users/{userId}/orders")
public ApiResponse<List<OrderResponse>> getUserOrders(@PathVariable String userId) {
    // 문제 1: 인증/인가 없음 - 다른 사용자의 주문 조회 가능
    // 문제 2: 페이징 없음 - 대량 데이터 노출
    // 문제 3: 민감 정보 노출 가능

    List<Order> orders = orderRepository.findByUserId(userId);
    return ApiResponse.success(
        orders.stream().map(OrderResponse::from).collect(Collectors.toList())
    );
}

// OrderResponse에 민감 정보 포함
public record OrderResponse(
    String orderNumber,
    String userId,
    String userName,
    String userPhone,        // 민감 정보
    String shippingAddress,  // 민감 정보
    String creditCardNumber, // 민감 정보!
    List<OrderItemResponse> items
) {}
```

#### ✅ After: 보안 강화

```java
@GetMapping("/users/{userId}/orders")
@PreAuthorize("hasRole('USER')")
public ApiResponse<Page<OrderResponse>> getUserOrders(
    @PathVariable String userId,
    @AuthenticationPrincipal JwtUserDetails userDetails,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
) {
    // 본인의 주문만 조회 가능
    if (!userId.equals(userDetails.getUserId()) && !userDetails.hasRole("ADMIN")) {
        throw new ForbiddenException("다른 사용자의 주문을 조회할 수 없습니다");
    }

    Pageable pageable = PageRequest.of(page, size);
    return ApiResponse.success(orderService.getUserOrders(userId, pageable));
}

// 민감 정보 제거/마스킹
public record OrderResponse(
    String orderNumber,
    String userName,
    String maskedPhone,         // "010-****-1234"
    String maskedAddress,       // "서울시 강남구 ***"
    String paymentMethod,       // "신용카드" (번호 제외)
    List<OrderItemResponse> items
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
            order.getOrderNumber(),
            order.getUserName(),
            maskPhone(order.getUserPhone()),
            maskAddress(order.getShippingAddress()),
            order.getPaymentMethod().getDisplayName(),
            order.getItems().stream()
                .map(OrderItemResponse::from)
                .collect(Collectors.toList())
        );
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) return "***-****-****";
        return phone.substring(0, 3) + "-****-" + phone.substring(phone.length() - 4);
    }

    private static String maskAddress(String address) {
        if (address == null) return "***";
        String[] parts = address.split(" ");
        if (parts.length <= 2) return "***";
        return String.join(" ", parts[0], parts[1], "***");
    }
}
```

---

## 4️⃣ 테스트 (Testing)

### 📌 개요

코드 변경이 기존 기능을 깨뜨리지 않는지, 새로운 기능이 제대로 동작하는지 확인합니다.

### ✅ 체크리스트

#### 테스트 커버리지

- [ ] **핵심 비즈니스 로직에 단위 테스트가 있는가?**
  - Service 레이어 메서드 테스트

- [ ] **엣지 케이스(Edge Case)를 테스트하는가?**
  - 빈 리스트, null, 경계값, 음수 등

- [ ] **예외 케이스를 테스트하는가?**
  - 잘못된 입력, 권한 없음, 리소스 없음 등

#### 테스트 품질

- [ ] **테스트가 독립적인가?** (다른 테스트에 의존하지 않음)
  - `@BeforeEach`로 초기화

- [ ] **테스트 이름이 무엇을 테스트하는지 명확한가?**
  - `should_ThrowException_When_StockIsInsufficient()`

- [ ] **Given-When-Then 구조를 따르는가?**
  - 준비 - 실행 - 검증

#### 통합 테스트

- [ ] **API 엔드포인트 테스트가 있는가?**
  - MockMvc, WebTestClient 활용

- [ ] **데이터베이스 통합 테스트가 있는가?**
  - @DataJpaTest, Testcontainers 활용

---

### 📊 Before / After: 테스트 개선

#### ❌ Before: 불충분한 테스트

```java
@Test
void testReserveStock() {
    // 단순한 성공 케이스만 테스트
    InventoryResponse response = inventoryService.reserveStock(1L, 10, "ORDER", "ORD-001", "user1");
    assertNotNull(response);
}
```

#### ✅ After: 포괄적인 테스트

```java
@DisplayName("재고 예약 테스트")
@Nested
class ReserveStockTest {

    @Test
    @DisplayName("충분한 재고가 있으면 예약에 성공한다")
    void should_ReserveStock_When_StockIsSufficient() {
        // Given
        Long productId = 1L;
        int requestedQuantity = 10;
        Inventory inventory = createInventory(productId, 100, 0);
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(inventory));

        // When
        InventoryResponse response = inventoryService.reserveStock(
            productId, requestedQuantity, "ORDER", "ORD-001", "user1"
        );

        // Then
        assertThat(response.getReservedStock()).isEqualTo(10);
        assertThat(response.getAvailableStock()).isEqualTo(90);
        verify(stockMovementRepository).save(any(StockMovement.class));
    }

    @Test
    @DisplayName("재고가 부족하면 InsufficientStockException을 발생시킨다")
    void should_ThrowException_When_StockIsInsufficient() {
        // Given
        Long productId = 1L;
        int requestedQuantity = 150;
        Inventory inventory = createInventory(productId, 100, 0);
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(inventory));

        // When & Then
        assertThatThrownBy(() ->
            inventoryService.reserveStock(productId, requestedQuantity, "ORDER", "ORD-001", "user1")
        )
        .isInstanceOf(InsufficientStockException.class)
        .hasMessageContaining("재고가 부족합니다");
    }

    @Test
    @DisplayName("존재하지 않는 상품이면 ProductNotFoundException을 발생시킨다")
    void should_ThrowException_When_ProductNotFound() {
        // Given
        Long nonExistentProductId = 999L;
        when(inventoryRepository.findById(nonExistentProductId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
            inventoryService.reserveStock(nonExistentProductId, 10, "ORDER", "ORD-001", "user1")
        )
        .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("음수 수량이면 IllegalArgumentException을 발생시킨다")
    void should_ThrowException_When_QuantityIsNegative() {
        // When & Then
        assertThatThrownBy(() ->
            inventoryService.reserveStock(1L, -10, "ORDER", "ORD-001", "user1")
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("수량은 1 이상이어야 합니다");
    }

    @Test
    @DisplayName("동시에 여러 요청이 들어와도 재고가 정확히 예약된다")
    void should_HandleConcurrentRequests_Correctly() throws InterruptedException {
        // Given
        Long productId = 1L;
        Inventory inventory = createInventory(productId, 100, 0);
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(inventory));

        int threadCount = 10;
        int quantityPerThread = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // When
        for (int i = 0; i < threadCount; i++) {
            int finalI = i;
            executorService.submit(() -> {
                try {
                    inventoryService.reserveStock(
                        productId, quantityPerThread, "ORDER", "ORD-" + finalI, "user1"
                    );
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // Then
        assertThat(inventory.getReservedStock()).isEqualTo(50); // 10 * 5
    }
}
```

**테스트 커버리지**: 1개 → 5개 (성공, 재고부족, 상품없음, 유효성검증, 동시성)

---

## 5️⃣ 아키텍처 (Architecture)

### 📌 개요

코드가 프로젝트의 아키텍처 원칙을 따르는지, 레이어 분리가 올바른지 확인합니다.

### ✅ 체크리스트

#### 레이어 분리

- [ ] **Controller는 비즈니스 로직을 포함하지 않는가?**
  - 요청 검증, DTO 변환, Service 호출만 담당

- [ ] **Service는 데이터 접근 로직을 직접 작성하지 않는가?**
  - Repository를 통한 간접 접근

- [ ] **Entity가 Controller에 직접 노출되지 않는가?**
  - DTO 변환 필수

#### 의존성 방향

- [ ] **상위 레이어가 하위 레이어에만 의존하는가?**
  - Controller → Service → Repository

- [ ] **순환 의존성이 없는가?**
  - A → B → A 구조 금지

#### Portal Universe 아키텍처

- [ ] **ApiResponse로 응답을 감싸는가?**
  - 모든 Controller는 `ApiResponse.success()` 사용

- [ ] **CustomBusinessException을 사용하는가?**
  - ErrorCode를 활용한 일관된 에러 처리

- [ ] **Kafka 이벤트를 활용하는가?**
  - 서비스 간 비동기 통신 우선

---

### 📊 Before / After: 아키텍처 개선

#### ❌ Before: 레이어 위반

```java
// Controller가 비즈니스 로직 + 데이터 접근을 직접 수행
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;  // ❌ Repository 직접 의존

    @PostMapping
    public Product createProduct(@RequestBody Product product) {  // ❌ Entity 노출
        // ❌ 비즈니스 로직이 Controller에 있음
        if (product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("가격은 0보다 커야 합니다");
        }

        // ❌ Repository 직접 호출
        return productRepository.save(product);
    }
}
```

#### ✅ After: 레이어 분리

```java
// 1. Controller: 요청 처리만 담당
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ApiResponse<ProductResponse> createProduct(
        @Valid @RequestBody ProductCreateRequest request
    ) {
        return ApiResponse.success(productService.createProduct(request));
    }
}

// 2. Service: 비즈니스 로직 처리
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductValidator productValidator;
    private final KafkaEventPublisher eventPublisher;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        // 검증
        productValidator.validateCreateRequest(request);

        // Entity 생성
        Product product = request.toEntity();

        // 저장
        Product savedProduct = productRepository.save(product);

        // 이벤트 발행 (비동기)
        eventPublisher.publishProductCreated(savedProduct);

        // DTO 변환
        return ProductResponse.from(savedProduct);
    }
}

// 3. Repository: 데이터 접근만 담당
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByName(String name);
}

// 4. DTO: API 계약
public record ProductCreateRequest(
    @NotBlank(message = "상품명은 필수입니다")
    String name,

    @NotNull(message = "가격은 필수입니다")
    @Positive(message = "가격은 0보다 커야 합니다")
    BigDecimal price
) {
    public Product toEntity() {
        return Product.builder()
            .name(name)
            .price(price)
            .build();
    }
}

public record ProductResponse(
    Long id,
    String name,
    BigDecimal price,
    LocalDateTime createdAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getCreatedAt()
        );
    }
}
```

---

## 🏢 Portal Universe 적용 사례

### 사례 1: ApiResponse 일관성 (가독성)

모든 Controller는 `ApiResponse.success()`로 응답을 감쌉니다:

```java
// ✅ Good
@GetMapping("/{productId}")
public ApiResponse<ProductResponse> getProductById(@PathVariable Long productId) {
    return ApiResponse.success(productService.getProductById(productId));
}

// ❌ Bad
@GetMapping("/{productId}")
public ProductResponse getProductById(@PathVariable Long productId) {
    return productService.getProductById(productId);
}
```

### 사례 2: Batch 재고 예약 (성능)

위치: `InventoryService.java`

```java
// ✅ Good: 여러 상품을 한 번에 예약 (데드락 방지 포함)
List<InventoryResponse> reserveStockBatch(
    Map<Long, Integer> quantities,  // productId -> quantity
    String referenceType,
    String referenceId,
    String userId
);

// ❌ Bad: 반복문으로 하나씩 예약
for (OrderItem item : items) {
    inventoryService.reserveStock(item.getProductId(), item.getQuantity(), ...);
}
```

### 사례 3: @Deprecated 활용 (아키텍처)

위치: `ProductController.java`

```java
/**
 * @deprecated Admin 전용 API는 AdminProductController를 사용하세요.
 */
@Deprecated
@PostMapping
@PreAuthorize("hasRole('ADMIN')")
public ApiResponse<ProductResponse> createProduct(@RequestBody ProductCreateRequest request) {
    return ApiResponse.success(productService.createProduct(request));
}
```

API를 점진적으로 마이그레이션하는 좋은 예시입니다.

---

## 🎯 실습 과제

### 과제 1: 가독성 리뷰

다음 PR 코드를 리뷰하고 개선 제안을 작성하세요:

```java
@PostMapping
public ApiResponse<CouponResponse> c(@RequestBody CouponRequest req) {
    var t = ct.findById(req.getTid()).orElseThrow();
    if(!t.isA()||t.getS().isAfter(now())||t.getE().isBefore(now())) {
        throw new RuntimeException("err");
    }
    var c = new Coupon();
    c.setU(req.getU());
    c.setT(req.getTid());
    c.setC(gen());
    return ApiResponse.success(CouponResponse.from(cr.save(c)));
}
```

**질문**:
1. 어떤 가독성 문제가 있나요?
2. 어떻게 개선하시겠습니까?

### 과제 2: 성능 리뷰

다음 코드의 성능 문제를 찾으세요:

```java
@GetMapping("/dashboard")
public DashboardResponse getDashboard() {
    List<Order> orders = orderRepository.findAll();  // 전체 주문 조회

    int totalOrders = orders.size();
    BigDecimal totalRevenue = BigDecimal.ZERO;

    for (Order order : orders) {
        totalRevenue = totalRevenue.add(order.getTotalAmount());

        // 각 주문의 상품 정보 조회
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId()).get();
            // ...
        }
    }

    return new DashboardResponse(totalOrders, totalRevenue);
}
```

**질문**:
1. 어떤 성능 문제가 있나요?
2. 어떻게 개선하시겠습니까?

### 과제 3: 보안 리뷰

다음 코드의 보안 취약점을 찾으세요:

```java
@PostMapping("/login")
public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
    User user = userRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

    if (!user.getPassword().equals(request.getPassword())) {
        throw new RuntimeException("비밀번호가 일치하지 않습니다");
    }

    String token = jwtUtil.generateToken(user);

    log.info("User {} logged in with password {}", user.getEmail(), request.getPassword());

    return ApiResponse.success(new LoginResponse(token, user));
}
```

**질문**:
1. 어떤 보안 문제가 있나요?
2. 어떻게 개선하시겠습니까?

---

## 🎨 효과적인 리뷰 코멘트 작성법

### ✅ 좋은 코멘트

```
💡 제안: reserveStock 메서드가 50줄이 넘어 가독성이 떨어집니다.
다음과 같이 Extract Method 리팩토링을 고려해보는 건 어떨까요?

- validateStockAvailability()
- createStockMovement()
- updateInventory()

참고: docs/learning/clean-code/refactoring/refactoring-techniques.md
```

### ❌ 나쁜 코멘트

```
이 코드는 너무 길어요. 고치세요.
```

### 코멘트 작성 원칙

1. **건설적이고 구체적으로 작성한다**
   - "이상하네요" ❌
   - "null 체크가 누락되어 NullPointerException이 발생할 수 있습니다" ✅

2. **대안을 제시한다**
   - "잘못됐어요" ❌
   - "Extract Method 패턴을 적용하면 가독성이 개선됩니다" ✅

3. **중요도를 표시한다**
   - `🚨 필수`: 반드시 수정 필요 (보안, 버그)
   - `💡 제안`: 개선 제안 (가독성, 성능)
   - `❓ 질문`: 의도 확인

4. **팀 문서를 참조한다**
   - `.claude/rules/`, `docs/learning/` 링크 제공

---

## 🤖 자동화 도구 활용

### 정적 분석 도구

| 도구 | 언어 | 확인 항목 |
|------|------|----------|
| SonarQube | Java, JS/TS | 코드 품질, 보안 취약점, 중복 코드 |
| ESLint | JavaScript/TypeScript | 코딩 컨벤션, 잠재적 버그 |
| Checkstyle | Java | 코딩 스타일 |
| SpotBugs | Java | 잠재적 버그 패턴 |

### CI/CD 통합

```yaml
# .github/workflows/code-review.yml
name: Code Review

on: [pull_request]

jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run ESLint
        run: npm run lint
      - name: Run Checkstyle
        run: ./gradlew checkstyleMain

  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run Tests
        run: ./gradlew test
      - name: Upload Coverage
        uses: codecov/codecov-action@v3
```

---

## 📚 관련 문서

### 내부 문서

- [리팩토링 기법](./refactoring-techniques.md) - 코드 개선 방법
- [Clean Code 트레이드오프](../trade-offs.md) - 설계 결정 기준
- [Portal Universe 아키텍처](../../../architecture/) - 전체 시스템 구조
- [테스트 전략](../testing/) - 테스트 작성 가이드

### 외부 자료

| 자료 | 난이도 | 설명 |
|------|--------|------|
| [Google Code Review Guidelines](https://google.github.io/eng-practices/review/) | ⭐⭐ | 구글의 코드 리뷰 가이드 |
| [Effective Code Review](https://www.oreilly.com/library/view/effective-code-review/9781492082712/) | ⭐⭐⭐ | 효과적인 코드 리뷰 기법 |
| [OWASP Top 10](https://owasp.org/www-project-top-ten/) | ⭐⭐⭐⭐ | 웹 애플리케이션 보안 취약점 |

---

## ✅ 학습 체크리스트

- [ ] 5가지 리뷰 영역(가독성, 성능, 보안, 테스트, 아키텍처)을 이해했다
- [ ] 각 영역별 체크리스트를 활용할 수 있다
- [ ] Portal Universe의 코딩 컨벤션을 알고 있다
- [ ] 건설적인 리뷰 코멘트를 작성할 수 있다
- [ ] 실습 과제 3개를 완료했다
- [ ] 실제 PR을 리뷰해보았다

---

## 📌 핵심 요약

| 영역 | 핵심 체크 항목 |
|------|---------------|
| **가독성** | 명확한 네이밍, 짧은 메서드, 의미 있는 주석 |
| **성능** | N+1 쿼리 방지, 적절한 인덱스, 페이징, 캐싱 |
| **보안** | 인증/인가, 입력 검증, 민감 정보 보호 |
| **테스트** | 핵심 로직 커버, 엣지 케이스, 독립적인 테스트 |
| **아키텍처** | 레이어 분리, 의존성 방향, Portal Universe 원칙 |

> **"코드 리뷰는 팀의 코드 품질을 높이는 가장 효과적인 방법입니다. 건설적이고 명확한 피드백을 제공하세요!"**
