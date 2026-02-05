---
id: learning-error-handling-patterns
title: Clean Code - 에러 처리 패턴
type: learning
status: current
created: 2026-01-22
updated: 2026-01-22
author: Portal Universe Team
tags: [clean-code, error-handling, exception, patterns]
difficulty: intermediate
estimated_time: 50분
---

# Clean Code - 에러 처리 패턴

## 📋 학습 목표

- 깔끔한 예외 처리 방법 습득
- Portal Universe의 에러 처리 아키텍처 이해
- ErrorCode Enum 패턴 학습
- GlobalExceptionHandler의 중앙 집중식 에러 처리 이해
- 체크 예외 vs 런타임 예외의 트레이드오프 분석

## 🎯 사전 지식

- Java Exception 기본 개념
- Spring Boot Exception Handling
- HTTP 상태 코드

## 📚 에러 처리의 중요성

> **"에러 처리는 중요하다. 하지만 에러 처리로 인해 프로그램 논리를 이해하기 어려워진다면 잘못된 것이다."**

잘 설계된 에러 처리:
- 비즈니스 로직과 에러 처리를 분리
- 일관된 에러 응답 형식
- 디버깅과 모니터링이 쉬움
- 클라이언트가 에러를 처리하기 쉬움

---

## 1️⃣ Portal Universe 에러 처리 아키텍처

### 전체 흐름

```
Controller
    ↓ (비즈니스 예외 발생)
Service → throw new CustomBusinessException(ErrorCode)
    ↓ (예외 전파)
GlobalExceptionHandler
    ↓ (중앙 집중 처리)
ApiResponse<ErrorResponse>
    ↓ (JSON 응답)
Client
```

### 핵심 컴포넌트

| 컴포넌트 | 역할 | 위치 |
|----------|------|------|
| `ErrorCode` | 에러 코드 인터페이스 | common-library |
| `ShoppingErrorCode` | Shopping 서비스 에러 코드 Enum | shopping-service |
| `CustomBusinessException` | 비즈니스 예외 클래스 | common-library |
| `GlobalExceptionHandler` | 중앙 예외 핸들러 | common-library |
| `ApiResponse` | 통합 응답 래퍼 | common-library |

---

## 2️⃣ ErrorCode 인터페이스 패턴

### 설계 원칙

- 모든 에러 코드는 `ErrorCode` 인터페이스 구현
- 각 서비스는 자신의 ErrorCode Enum 정의
- HTTP 상태, 에러 코드, 메시지를 함께 관리

### ErrorCode 인터페이스

```java
// services/common-library/.../exception/ErrorCode.java
public interface ErrorCode {
    HttpStatus getStatus();  // HTTP 상태 코드
    String getCode();        // 서비스별 에러 코드 (예: S001, A001)
    String getMessage();     // 에러 메시지
}
```

### ShoppingErrorCode Enum

```java
// services/shopping-service/.../exception/ShoppingErrorCode.java

/**
 * Shopping 서비스의 비즈니스 예외 에러 코드입니다.
 *
 * <p>에러코드 체계:
 * <ul>
 *   <li>S0XX: Product (S001-S010)</li>
 *   <li>S1XX: Cart (S101-S110)</li>
 *   <li>S2XX: Order (S201-S220)</li>
 *   <li>S3XX: Payment (S301-S315)</li>
 *   <li>S4XX: Inventory (S401-S410)</li>
 *   <li>S5XX: Delivery (S501-S510)</li>
 *   <li>S6XX: Coupon (S601-S620)</li>
 *   <li>S7XX: TimeDeal (S701-S710)</li>
 *   <li>S8XX: Queue (S801-S810)</li>
 *   <li>S9XX: Saga/System (S901-S910)</li>
 * </ul>
 * </p>
 */
@Getter
public enum ShoppingErrorCode implements ErrorCode {

    // Product Errors (S0XX)
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "Product not found"),
    PRODUCT_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "S008", "Product name already exists"),
    INVALID_PRODUCT_PRICE(HttpStatus.BAD_REQUEST, "S004", "Product price must be greater than 0"),

    // Cart Errors (S1XX)
    CART_NOT_FOUND(HttpStatus.NOT_FOUND, "S101", "Cart not found"),
    CART_EMPTY(HttpStatus.BAD_REQUEST, "S104", "Cart is empty"),

    // Order Errors (S2XX)
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "S201", "Order not found"),
    ORDER_CANNOT_BE_CANCELLED(HttpStatus.BAD_REQUEST, "S203", "Order cannot be cancelled in current status"),

    // Inventory Errors (S4XX)
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "S402", "Insufficient stock available"),
    INVENTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "S401", "Inventory not found for product"),

    // Coupon Errors (S6XX)
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "S601", "Coupon not found"),
    USER_COUPON_ALREADY_USED(HttpStatus.BAD_REQUEST, "S609", "User coupon has already been used"),
    USER_COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "S610", "User coupon has expired"),

    // ... (생략)

    private final HttpStatus status;
    private final String code;
    private final String message;

    ShoppingErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
```

**설계 포인트:**
- ✅ 서비스별 prefix (Shopping = S, Auth = A, Blog = B)
- ✅ 도메인별로 범위 구분 (Product = S0XX, Cart = S1XX)
- ✅ HTTP 상태 코드와 연결
- ✅ 검색 가능한 에러 코드 (S001, S101 등)

---

## 3️⃣ CustomBusinessException 패턴

### 설계 원칙

- RuntimeException 상속 (Unchecked Exception)
- ErrorCode를 포함하여 에러 정보 전달
- 비즈니스 로직에서 예측 가능한 예외 표현

### CustomBusinessException

```java
// services/common-library/.../exception/CustomBusinessException.java

/**
 * 시스템 전반에서 사용될 커스텀 비즈니스 예외 클래스입니다.
 *
 * <p>서비스 로직에서 예측 가능한 예외 상황이 발생했을 때 사용됩니다.
 * 이 예외는 {@link ErrorCode}를 포함하여, 예외 발생 시 상태 코드, 에러 코드, 메시지를
 * 일관되게 처리할 수 있도록 합니다.</p>
 */
@Getter
public class CustomBusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * ErrorCode를 인자로 받는 생성자입니다.
     *
     * @param errorCode 발생한 예외에 해당하는 ErrorCode Enum 값
     */
    public CustomBusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

### 사용 예시

```java
// services/shopping-service/.../product/service/ProductServiceImpl.java

@Override
public ProductResponse getProductById(Long id) {
    Product product = productRepository.findById(id)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND));

    return convertToResponse(product);
}

@Override
@Transactional
public ProductResponse createProductAdmin(AdminProductRequest request) {
    // 중복된 상품명 체크
    if (productRepository.existsByName(request.name())) {
        throw new CustomBusinessException(ShoppingErrorCode.PRODUCT_NAME_ALREADY_EXISTS);
    }

    Product newProduct = Product.builder()
            .name(request.name())
            .description(request.description())
            .price(request.price())
            .stock(request.stock())
            .build();

    return convertToResponse(productRepository.save(newProduct));
}
```

**장점:**
- ✅ 에러 코드가 Enum으로 관리되어 오타 방지
- ✅ IDE 자동완성 지원
- ✅ HTTP 상태 코드와 에러 메시지가 자동 매핑
- ✅ 일관된 에러 응답 형식

---

## 4️⃣ GlobalExceptionHandler - 중앙 집중식 처리

### 설계 원칙

- `@RestControllerAdvice`로 모든 컨트롤러 예외 처리
- 예외 타입별로 다른 응답 반환
- 일관된 `ApiResponse` 형식 유지

### GlobalExceptionHandler

```java
// services/common-library/.../exception/GlobalExceptionHandler.java

/**
 * 전역 예외 처리를 담당하는 핸들러입니다.
 *
 * <p>애플리케이션 전반에서 발생하는 예외를 중앙에서 처리하여
 * 일관된 에러 응답 형식을 클라이언트에게 제공합니다.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * CustomBusinessException을 처리합니다.
     * 비즈니스 로직에서 발생한 예측 가능한 예외를 처리합니다.
     */
    @ExceptionHandler(CustomBusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomBusinessException(
            CustomBusinessException e,
            HttpServletRequest request) {

        ErrorCode errorCode = e.getErrorCode();

        log.warn("Business exception occurred: code={}, message={}, path={}",
                errorCode.getCode(),
                errorCode.getMessage(),
                request.getRequestURI());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    /**
     * MethodArgumentNotValidException을 처리합니다.
     * @Valid 검증 실패 시 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException e) {

        String errorMessage = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation failed: {}", errorMessage);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("VALIDATION_ERROR", errorMessage));
    }

    /**
     * 모든 예상치 못한 예외를 처리합니다.
     * 시스템 에러로 간주하여 500 응답을 반환합니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception e,
            HttpServletRequest request) {

        log.error("Unexpected exception occurred: path={}, message={}",
                request.getRequestURI(),
                e.getMessage(),
                e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "INTERNAL_SERVER_ERROR",
                        "An unexpected error occurred. Please try again later."));
    }
}
```

### 에러 응답 형식

```json
// 성공 응답
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Product Name"
  },
  "error": null
}

// 에러 응답
{
  "success": false,
  "data": null,
  "error": {
    "code": "S001",
    "message": "Product not found"
  }
}
```

---

## 5️⃣ 예외 사용 원칙

### 오류 코드를 반환하지 마라

#### ❌ Bad - 오류 코드 반환

```java
// 호출자가 즉시 오류를 처리해야 함 (복잡함)
public int deleteProduct(Long productId) {
    if (!productRepository.existsById(productId)) {
        return -1;  // NOT_FOUND
    }

    productRepository.deleteById(productId);
    return 0;  // SUCCESS
}

// 사용처
int result = deleteProduct(productId);
if (result == -1) {
    System.out.println("Product not found");
} else {
    System.out.println("Deleted");
}
```

#### ✅ Good - 예외 던지기

```java
// 예외로 처리 (깔끔함)
public void deleteProduct(Long productId) {
    if (!productRepository.existsById(productId)) {
        throw new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND);
    }

    productRepository.deleteById(productId);
}

// 사용처 - Controller에서는 처리하지 않아도 됨 (GlobalExceptionHandler가 처리)
@DeleteMapping("/{id}")
public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
    productService.deleteProduct(id);
    return ResponseEntity.ok(ApiResponse.success(null));
}
```

### Try-Catch-Finally 블록을 먼저 작성하라

```java
// Try 블록을 트랜잭션처럼 사용
@Transactional
public Order createOrder(String userId, CreateOrderRequest request) {
    try {
        // 정상 흐름
        Cart cart = validateAndGetCart(userId);
        Order order = buildOrder(userId, cart, request);
        orderSagaOrchestrator.startSaga(order);
        return orderRepository.save(order);
    } catch (CustomBusinessException e) {
        // 비즈니스 예외는 그대로 전파
        throw e;
    } catch (Exception e) {
        // 예상치 못한 예외는 로깅 후 재포장
        log.error("Failed to create order: userId={}", userId, e);
        throw new CustomBusinessException(ShoppingErrorCode.ORDER_CREATION_FAILED);
    }
}
```

### 미확인 예외를 사용하라 (Unchecked Exception)

#### 체크 예외의 문제점

```java
// ❌ Bad - Checked Exception (상위 메서드가 모두 throws 선언 필요)
public void createOrder(CreateOrderRequest request) throws OrderException, PaymentException {
    try {
        Order order = buildOrder(request);
        processPayment(order);  // throws PaymentException
    } catch (PaymentException e) {
        throw new OrderException("Payment failed", e);
    }
}

// 호출하는 모든 메서드가 예외 처리 강제됨
public void processUserOrder(CreateOrderRequest request) throws OrderException {
    orderService.createOrder(request);  // 반드시 throws 선언 필요
}
```

#### ✅ Portal Universe 방식 - Unchecked Exception

```java
// ✅ Good - RuntimeException (상위 메서드는 처리 선택 가능)
@Transactional
public OrderResponse createOrder(String userId, CreateOrderRequest request) {
    // 예외가 발생하면 자동으로 전파 (throws 선언 불필요)
    Cart cart = cartRepository.findByUserIdAndStatusWithItems(userId, CartStatus.CHECKED_OUT)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.CART_NOT_FOUND));

    // ...
}

// 호출하는 메서드는 예외 처리 강제 안 됨
@PostMapping
public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
        @AuthenticationPrincipal String userId,
        @Valid @RequestBody CreateOrderRequest request) {

    // 예외는 GlobalExceptionHandler가 처리
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(orderService.createOrder(userId, request)));
}
```

### 예외에 의미 있는 정보를 제공하라

```java
// ❌ Bad - 부족한 정보
throw new IllegalArgumentException("Invalid");

// ✅ Good - 명확한 정보
throw new CustomBusinessException(ShoppingErrorCode.INVALID_PRODUCT_PRICE);

// ✅ Better - 추가 컨텍스트 (필요한 경우)
log.error("Failed to create product: duplicated name={}", request.name());
throw new CustomBusinessException(ShoppingErrorCode.PRODUCT_NAME_ALREADY_EXISTS);
```

### 예외 클래스를 잘 활용하라

```java
// ❌ Bad - 모든 예외를 Exception으로 처리
try {
    // ...
} catch (Exception e) {
    // 무슨 예외인지 알 수 없음
}

// ✅ Good - 구체적인 예외 타입 사용
try {
    processPayment(order);
} catch (CustomBusinessException e) {
    // 비즈니스 예외 처리
    log.warn("Payment failed: {}", e.getErrorCode().getMessage());
    throw e;
} catch (DataAccessException e) {
    // DB 예외 처리
    log.error("Database error during payment", e);
    throw new CustomBusinessException(ShoppingErrorCode.PAYMENT_PROCESSING_FAILED);
}
```

---

## 6️⃣ 실전 패턴

### Pattern 1: Optional과 orElseThrow

```java
// 가장 많이 사용되는 패턴
Product product = productRepository.findById(id)
        .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND));
```

### Pattern 2: 비즈니스 규칙 검증

```java
@Transactional
public void issueCoupon(String userId, Long couponId) {
    Coupon coupon = getCoupon(couponId);

    // 1. 비즈니스 규칙 검증
    if (!coupon.isActive()) {
        throw new CustomBusinessException(ShoppingErrorCode.COUPON_INACTIVE);
    }

    if (coupon.isExpired()) {
        throw new CustomBusinessException(ShoppingErrorCode.COUPON_EXPIRED);
    }

    if (coupon.isExhausted()) {
        throw new CustomBusinessException(ShoppingErrorCode.COUPON_EXHAUSTED);
    }

    // 2. 중복 발급 체크
    if (userCouponRepository.existsByCouponIdAndUserId(couponId, userId)) {
        throw new CustomBusinessException(ShoppingErrorCode.COUPON_ALREADY_ISSUED);
    }

    // 3. 정상 흐름
    UserCoupon userCoupon = UserCoupon.create(userId, coupon);
    userCouponRepository.save(userCoupon);

    coupon.decreaseQuantity();
}
```

### Pattern 3: 다중 조건 검증 함수 분리

```java
public void validateCouponForOrder(Long userCouponId, String userId, BigDecimal orderAmount) {
    UserCoupon userCoupon = getUserCoupon(userCouponId, userId);
    Coupon coupon = userCoupon.getCoupon();

    // 각 검증을 별도 메서드로 분리 (가독성 향상)
    validateCouponUsability(userCoupon, coupon);
    validateMinimumOrderAmount(coupon, orderAmount);
}

private void validateCouponUsability(UserCoupon userCoupon, Coupon coupon) {
    if (userCoupon.isUsed()) {
        throw new CustomBusinessException(ShoppingErrorCode.USER_COUPON_ALREADY_USED);
    }
    if (userCoupon.isExpired()) {
        throw new CustomBusinessException(ShoppingErrorCode.USER_COUPON_EXPIRED);
    }
    if (!coupon.isActive()) {
        throw new CustomBusinessException(ShoppingErrorCode.COUPON_INACTIVE);
    }
}

private void validateMinimumOrderAmount(Coupon coupon, BigDecimal orderAmount) {
    if (orderAmount.compareTo(coupon.getMinimumOrderAmount()) < 0) {
        throw new CustomBusinessException(ShoppingErrorCode.COUPON_MINIMUM_ORDER_NOT_MET);
    }
}
```

### Pattern 4: Validation 어노테이션 활용

```java
// DTO에서 검증 (Controller 진입 전 처리)
public record ProductCreateRequest(
    @NotBlank(message = "상품명은 필수입니다")
    @Size(max = 100, message = "상품명은 100자 이내여야 합니다")
    String name,

    @NotNull(message = "가격은 필수입니다")
    @Positive(message = "가격은 양수여야 합니다")
    BigDecimal price,

    @NotNull(message = "재고는 필수입니다")
    @Min(value = 0, message = "재고는 0 이상이어야 합니다")
    Integer stock
) {}

// Controller
@PostMapping
public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
        @Valid @RequestBody ProductCreateRequest request) {  // @Valid로 자동 검증
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(productService.createProduct(request)));
}

// GlobalExceptionHandler가 MethodArgumentNotValidException 처리
```

---

## ✅ 에러 처리 체크리스트

### 설계
- [ ] ErrorCode Enum으로 에러 코드를 관리하는가?
- [ ] 서비스별로 prefix를 구분했는가? (S, A, B 등)
- [ ] HTTP 상태 코드와 에러 코드를 함께 관리하는가?

### 예외 던지기
- [ ] 오류 코드 대신 예외를 사용하는가?
- [ ] Unchecked Exception (RuntimeException)을 사용하는가?
- [ ] Optional.orElseThrow()를 적절히 활용하는가?

### 예외 처리
- [ ] GlobalExceptionHandler에서 중앙 집중 처리하는가?
- [ ] 비즈니스 로직에서 try-catch를 최소화했는가?
- [ ] 예외 로그를 적절히 남기는가?

### 응답 형식
- [ ] 일관된 ApiResponse 형식을 사용하는가?
- [ ] 에러 응답에 충분한 정보를 제공하는가?
- [ ] 민감한 정보(스택 트레이스)를 클라이언트에 노출하지 않는가?

---

## 📊 예외 처리 전략 비교

| 방식 | 장점 | 단점 | Portal Universe |
|------|------|------|-----------------|
| **오류 코드 반환** | 컴파일 타임 체크 | 호출부 복잡, if-else 증가 | ❌ 사용 안 함 |
| **Checked Exception** | 예외 처리 강제 | throws 전파, 상위 레이어 영향 | ❌ 사용 안 함 |
| **Unchecked Exception** | 깔끔한 코드, 유연성 | 예외 처리 누락 가능 | ✅ 사용 (RuntimeException) |
| **중앙 집중 처리** | 일관성, 중복 제거 | 초기 설정 필요 | ✅ 사용 (GlobalExceptionHandler) |

---

## 📚 관련 문서

- [SOLID 원칙](./solid-principles.md)
- [Clean Code - 함수 설계 원칙](./clean-code-functions.md)
- [DRY, KISS, YAGNI 원칙](./dry-kiss-yagni.md)
- [Portal Universe 에러 처리 가이드](../../../../.claude/rules/common.md#error-handling)

---

## 📖 추가 학습 자료

| 자료 | 난이도 | 설명 |
|------|--------|------|
| [Clean Code Chapter 7](https://www.amazon.com/Clean-Code-Handbook-Software-Craftsmanship/dp/0132350882) | ⭐⭐⭐ | 에러 처리 |
| [Effective Java Item 69-77](https://www.amazon.com/Effective-Java-Joshua-Bloch/dp/0134685997) | ⭐⭐⭐⭐ | 예외 사용법 |
| [Spring @ControllerAdvice](https://spring.io/blog/2013/11/01/exception-handling-in-spring-mvc) | ⭐⭐⭐ | Spring 예외 처리 |

---

**마지막 업데이트:** 2026-01-22
