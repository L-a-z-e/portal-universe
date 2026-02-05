---
id: learning-dry-kiss-yagni
title: DRY, KISS, YAGNI - 실용적 설계 원칙
type: learning
status: current
created: 2026-01-22
updated: 2026-01-22
author: Portal Universe Team
tags: [clean-code, dry, kiss, yagni, design-principles, pragmatic]
difficulty: beginner
estimated_time: 40분
---

# DRY, KISS, YAGNI - 실용적 설계 원칙

## 📋 학습 목표

- DRY(Don't Repeat Yourself), KISS(Keep It Simple, Stupid), YAGNI(You Aren't Gonna Need It) 원칙 이해
- 각 원칙을 위반하는 코드의 문제점과 준수하는 코드의 장점 파악
- Portal Universe 프로젝트에서의 실제 적용 사례 학습
- 과도한 추상화와 적절한 단순함의 균형 감각 습득

## 🎯 사전 지식

- Java 기본 문법
- 코드 중복의 개념
- 리팩토링 기초

## 📚 원칙 개요

| 원칙 | 핵심 개념 | 목적 |
|------|----------|------|
| **DRY** | Don't Repeat Yourself | 중복 제거 |
| **KISS** | Keep It Simple, Stupid | 단순함 유지 |
| **YAGNI** | You Aren't Gonna Need It | 불필요한 기능 방지 |

---

## 1️⃣ DRY - Don't Repeat Yourself

### 원칙 설명

> **"모든 지식은 시스템 내에서 단 한 번만, 명확하게, 권위 있게 표현되어야 한다."**

- 같은 코드를 여러 곳에 복사-붙여넣기 하지 마라
- 중복은 버그의 온상이며, 유지보수 비용을 증가시킨다
- "지식의 중복"을 제거하는 것이 핵심 (단순히 코드 줄 수가 아님)

### ❌ Bad Example - 코드 중복

```java
// 중복된 검증 로직 (DRY 위반)
@RestController
public class ProductController {

    @PostMapping("/products")
    public ResponseEntity<?> createProduct(@RequestBody ProductRequest request) {
        // 검증 로직 1
        if (request.getName() == null || request.getName().isEmpty()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Product price must be positive");
        }
        if (request.getStock() == null || request.getStock() < 0) {
            throw new IllegalArgumentException("Product stock must be non-negative");
        }

        // ...
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody ProductRequest request) {
        // 동일한 검증 로직 복사-붙여넣기! (DRY 위반)
        if (request.getName() == null || request.getName().isEmpty()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Product price must be positive");
        }
        if (request.getStock() == null || request.getStock() < 0) {
            throw new IllegalArgumentException("Product stock must be non-negative");
        }

        // ...
    }
}
```

**문제점:**
- 검증 로직이 변경되면 여러 곳을 수정해야 함
- 한 곳만 수정하면 버그 발생 (일관성 깨짐)
- 테스트 코드도 중복

### ✅ Good Example - DRY 준수

```java
// Jakarta Validation으로 중복 제거 (DRY 준수)
public record ProductRequest(
    @NotBlank(message = "Product name is required")
    String name,

    @NotNull(message = "Product price is required")
    @Positive(message = "Product price must be positive")
    BigDecimal price,

    @NotNull(message = "Product stock is required")
    @Min(value = 0, message = "Product stock must be non-negative")
    Integer stock
) {}

@RestController
public class ProductController {

    @PostMapping("/products")
    public ResponseEntity<?> createProduct(@Valid @RequestBody ProductRequest request) {
        // @Valid가 자동으로 검증 수행
        return ResponseEntity.ok(productService.createProduct(request));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        // 동일한 검증 로직이 자동으로 적용됨
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }
}
```

### 🏗️ Portal Universe 적용 사례

**1. DTO에서 중복 제거 - Jakarta Validation 활용**

```java
// services/shopping-service/.../product/dto/AdminProductRequest.java
public record AdminProductRequest(
    @NotBlank(message = "상품명은 필수입니다")
    @Size(max = 100, message = "상품명은 100자 이내여야 합니다")
    String name,

    @NotBlank(message = "상품 설명은 필수입니다")
    String description,

    @NotNull(message = "가격은 필수입니다")
    @Positive(message = "가격은 양수여야 합니다")
    BigDecimal price,

    @NotNull(message = "재고는 필수입니다")
    @Min(value = 0, message = "재고는 0 이상이어야 합니다")
    Integer stock
) {}
```

**DRY 준수 포인트:**
- 검증 로직이 DTO에 한 번만 정의됨
- 모든 Controller에서 `@Valid`만 붙이면 자동 검증
- 검증 규칙 변경 시 DTO만 수정하면 됨

**2. ErrorCode 체계 - 중복 메시지 제거**

```java
// services/shopping-service/.../exception/ShoppingErrorCode.java
@Getter
public enum ShoppingErrorCode implements ErrorCode {
    // 에러 메시지가 Enum에 한 번만 정의됨
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "Product not found"),
    CART_NOT_FOUND(HttpStatus.NOT_FOUND, "S101", "Cart not found"),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "S201", "Order not found"),
    // ...
}

// 사용처 - 에러 메시지 중복 없음
throw new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND);
throw new CustomBusinessException(ShoppingErrorCode.CART_NOT_FOUND);
```

**DRY 준수 포인트:**
- 에러 메시지가 Enum에 집중됨
- `"Product not found"` 문자열이 코드 전체에 흩어지지 않음
- 메시지 변경 시 Enum만 수정

**3. Entity 변환 로직 - Helper 메서드로 중복 제거**

```java
// services/shopping-service/.../product/service/ProductServiceImpl.java
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    // Entity -> DTO 변환 로직을 한 곳에 집중
    private ProductResponse convertToResponse(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getStock()
        );
    }

    @Override
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND));

        return convertToResponse(product);  // 중복 제거
    }

    @Override
    public ProductResponse createProduct(ProductCreateRequest request) {
        Product savedProduct = productRepository.save(/* ... */);
        return convertToResponse(savedProduct);  // 동일한 변환 로직 재사용
    }
}
```

### ⚠️ DRY의 함정 - 우연한 중복 vs 본질적 중복

```java
// 나쁜 DRY - 우연히 같아 보이는 코드를 무리하게 통합
// Bad: 두 기능은 우연히 비슷할 뿐, 본질적으로 다름
public void processOrderPayment(Order order) {
    // 주문 결제 로직
    calculateAmount(order);
}

public void processCouponDiscount(Coupon coupon) {
    // 쿠폰 할인 계산
    calculateAmount(coupon);  // 우연히 이름이 같지만 다른 로직!
}

// Good: 본질적으로 다른 로직은 분리
public void processOrderPayment(Order order) {
    calculateOrderAmount(order);
}

public void processCouponDiscount(Coupon coupon) {
    calculateCouponDiscount(coupon);
}
```

---

## 2️⃣ KISS - Keep It Simple, Stupid

### 원칙 설명

> **"대부분의 시스템은 복잡하게 만드는 것보다 단순하게 유지할 때 최고로 작동한다."**

- 불필요한 복잡성을 추가하지 마라
- 단순한 해결책이 항상 좋은 해결책이다
- "누구나 이해할 수 있는 코드"를 작성하라

### ❌ Bad Example - 불필요한 복잡성

```java
// 과도한 디자인 패턴 적용 (KISS 위반)
// 단순히 사용자 이름을 가져오는 기능에 너무 많은 계층
public interface UserNameExtractor {
    String extract(User user);
}

public class FirstNameExtractor implements UserNameExtractor {
    @Override
    public String extract(User user) {
        return user.getFirstName();
    }
}

public class LastNameExtractor implements UserNameExtractor {
    @Override
    public String extract(User user) {
        return user.getLastName();
    }
}

public class UserNameExtractorFactory {
    public static UserNameExtractor create(String type) {
        return switch (type) {
            case "FIRST" -> new FirstNameExtractor();
            case "LAST" -> new LastNameExtractor();
            default -> throw new IllegalArgumentException("Unknown type");
        };
    }
}

// 사용처
UserNameExtractor extractor = UserNameExtractorFactory.create("FIRST");
String name = extractor.extract(user);
```

**문제점:**
- 단순한 기능에 과도한 추상화
- 코드 이해에 시간이 오래 걸림
- 유지보수 비용 증가

### ✅ Good Example - 단순함 유지

```java
// 단순하고 명확한 코드 (KISS 준수)
public class User {
    private String firstName;
    private String lastName;

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}

// 사용처
String firstName = user.getFirstName();
String fullName = user.getFullName();
```

### 🏗️ Portal Universe 적용 사례

**1. 간결한 Service 메서드**

```java
// services/shopping-service/.../product/service/ProductServiceImpl.java
@Override
public ProductResponse getProductById(Long id) {
    // 복잡한 패턴 없이 직관적인 코드
    Product product = productRepository.findById(id)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND));

    return convertToResponse(product);
}
```

**KISS 준수 포인트:**
- 누구나 이해할 수 있는 직관적인 흐름
- 불필요한 추상화 레이어 없음
- "상품 조회 → 예외 처리 → DTO 변환"이 명확

**2. 단순한 ErrorCode 체계**

```java
// 복잡한 상속 구조 대신 단순한 Enum
@Getter
public enum ShoppingErrorCode implements ErrorCode {
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "Product not found"),
    CART_NOT_FOUND(HttpStatus.NOT_FOUND, "S101", "Cart not found"),
    // ...

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

**KISS 준수 포인트:**
- Enum 하나로 모든 에러 코드 관리
- 복잡한 상속 구조 없음
- 새 에러 코드 추가 시 단순히 상수 추가

**3. Controller 구조 - RESTful의 단순함**

```java
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    // 단순하고 명확한 API 구조
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(productService.createProduct(request)));
    }
}
```

**KISS 준수 포인트:**
- RESTful 규약을 따르는 직관적인 엔드포인트
- 복잡한 URL 매핑 없음
- HTTP 메서드만으로 의도 파악 가능

### ⚠️ KISS의 오해 - "단순함 ≠ 기능 부족"

```java
// 나쁜 KISS - 너무 단순해서 확장 불가능
// Bad: 하드코딩으로 단순하게 만들었지만 유연성 없음
public BigDecimal calculateDiscount(BigDecimal price) {
    return price.multiply(BigDecimal.valueOf(0.1));  // 항상 10% 고정
}

// Good: 단순하면서도 확장 가능
public BigDecimal calculateDiscount(BigDecimal price, BigDecimal discountRate) {
    return price.multiply(discountRate);
}
```

---

## 3️⃣ YAGNI - You Aren't Gonna Need It

### 원칙 설명

> **"지금 필요하지 않은 기능은 구현하지 마라."**

- "나중에 필요할 것 같아서" 미리 만들지 마라
- 실제로 필요해질 때 추가하라
- 미래를 예측하는 코드는 대부분 쓰이지 않는다

### ❌ Bad Example - 불필요한 기능 추가

```java
// 현재 필요하지 않은 기능까지 구현 (YAGNI 위반)
@Entity
public class Product {
    private Long id;
    private String name;
    private BigDecimal price;

    // 현재 사용하지 않는 필드들 (나중에 필요할 것 같아서 추가)
    private String manufacturer;  // 아직 제조사 정보는 필요 없음
    private LocalDate manufactureDate;  // 제조일자도 필요 없음
    private String barcode;  // 바코드도 필요 없음
    private String sku;  // SKU도 필요 없음
    private Integer reorderLevel;  // 재주문 레벨도 필요 없음
    private String warehouseLocation;  // 창고 위치도 필요 없음

    // 복잡한 비즈니스 로직 (아직 요구사항 없음)
    public boolean shouldReorder() {
        // 미래를 위한 복잡한 로직
        return this.stock < this.reorderLevel;
    }
}
```

**문제점:**
- 사용하지 않는 필드가 코드와 DB를 복잡하게 만듦
- 유지보수 비용 증가 (사용하지 않는 코드도 관리해야 함)
- 실제 필요할 때는 요구사항이 달라질 수 있음

### ✅ Good Example - 현재 필요한 것만 구현

```java
// 현재 필요한 기능만 구현 (YAGNI 준수)
@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;

    // 현재 필요한 메서드만
    public void update(String name, String description, BigDecimal price, Integer stock) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
    }
}

// 나중에 제조사 정보가 필요하면 그때 추가
// 나중에 재주문 로직이 필요하면 그때 추가
```

### 🏗️ Portal Universe 적용 사례

**1. Product Entity - 필요한 필드만**

```java
// services/shopping-service/.../product/domain/Product.java
@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;

    // 현재 필요한 메서드만 구현
    public void update(String name, String description, BigDecimal price, Integer stock) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
    }
}
```

**YAGNI 준수 포인트:**
- "나중에 필요할 것 같은" 필드 없음
- 복잡한 비즈니스 로직 없음
- 실제 요구사항에 집중

**2. OrderService - 필요한 기능만 제공**

```java
// services/shopping-service/.../order/service/OrderService.java
public interface OrderService {
    // 현재 요구사항에 맞는 메서드만 정의
    OrderResponse createOrder(String userId, CreateOrderRequest request);
    OrderResponse getOrderById(Long orderId, String userId);
    Page<OrderResponse> getMyOrders(String userId, Pageable pageable);
    OrderResponse cancelOrder(Long orderId, String userId, CancelOrderRequest request);

    // "나중에 필요할 것 같은" 메서드는 추가하지 않음
    // void scheduleRecurringOrder(Long orderId, RecurrencePattern pattern);  // 정기 배송 - 아직 필요 없음
    // List<OrderRecommendation> getRecommendedProducts(String userId);  // 추천 상품 - 아직 필요 없음
}
```

**YAGNI 준수 포인트:**
- 실제 사용 중인 기능만 인터페이스에 정의
- 미래를 위한 "확장 포인트" 없음
- 필요해지면 그때 추가

**3. ErrorCode - 실제 발생하는 에러만 정의**

```java
@Getter
public enum ShoppingErrorCode implements ErrorCode {
    // 실제로 발생하는 에러만 정의
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "Product not found"),
    CART_NOT_FOUND(HttpStatus.NOT_FOUND, "S101", "Cart not found"),

    // "나중에 필요할 것 같은" 에러 코드는 추가하지 않음
    // PRODUCT_TEMPORARILY_UNAVAILABLE(...),  // 임시 품절 - 아직 요구사항 없음
    // PRODUCT_REVIEW_PENDING(...),  // 리뷰 승인 대기 - 아직 요구사항 없음
}
```

### ⚠️ YAGNI와 확장성의 균형

```java
// 나쁜 YAGNI - 확장을 전혀 고려하지 않음
// Bad: 나중에 확장 불가능한 하드코딩
public void sendNotification(String message) {
    EmailSender.send(message);  // 이메일만 가능, SMS는 불가능
}

// Good: YAGNI를 지키면서도 기본적인 확장성 확보
public interface NotificationSender {
    void send(String message);
}

@Service
public class NotificationService {
    private final NotificationSender emailSender;

    public void sendNotification(String message) {
        emailSender.send(message);
    }
}

// SMS가 실제로 필요해지면 그때 SmsSender 추가
@Service
public class SmsSender implements NotificationSender {
    @Override
    public void send(String message) {
        // SMS 발송 로직
    }
}
```

---

## ✅ DRY, KISS, YAGNI 체크리스트

### DRY (Don't Repeat Yourself)
- [ ] 동일한 로직이 여러 곳에 복사-붙여넣기 되어 있지 않은가?
- [ ] 검증 로직이 DTO나 공통 유틸리티로 분리되어 있는가?
- [ ] 에러 메시지가 Enum이나 상수로 관리되는가?
- [ ] Entity ↔ DTO 변환 로직이 Helper 메서드로 추출되어 있는가?

### KISS (Keep It Simple, Stupid)
- [ ] 불필요한 디자인 패턴이나 추상화가 없는가?
- [ ] 신입 개발자도 이해할 수 있는 코드인가?
- [ ] 메서드가 한 눈에 파악 가능한 길이인가?
- [ ] 복잡한 조건문을 단순한 메서드로 분리할 수 있는가?

### YAGNI (You Aren't Gonna Need It)
- [ ] 현재 요구사항에 없는 기능을 미리 구현하지 않았는가?
- [ ] "나중에 필요할 것 같아서" 추가한 필드나 메서드가 있는가?
- [ ] 실제로 호출되지 않는 코드가 있는가?
- [ ] 과도한 확장성을 위한 복잡한 구조가 있는가?

---

## 📊 원칙 비교

| 상황 | DRY | KISS | YAGNI |
|------|-----|------|-------|
| 중복 코드 발견 | ✅ 제거해야 함 | 🤔 단순한 방법으로 | 🤔 정말 필요한가? |
| 추상화 고려 | ✅ 중복 제거 위해 | ⚠️ 과도하지 않게 | ⚠️ 필요할 때만 |
| 새 기능 추가 | 🤔 기존 코드 재사용? | ✅ 가장 단순한 방법 | ✅ 필요한 것만 |
| 리팩토링 | ✅ 중복 제거 우선 | ✅ 복잡성 제거 | ✅ 불필요한 코드 제거 |

---

## 🎯 실전 적용 가이드

### 1단계: DRY 먼저 확인
- 동일한 코드가 3번 이상 반복되면 리팩토링 고려
- 단, "우연한 중복"은 무리하게 통합하지 말 것

### 2단계: KISS 적용
- 가장 단순한 해결책부터 시작
- 복잡한 디자인 패턴은 정말 필요할 때만

### 3단계: YAGNI 검증
- "나중에 필요할 것 같은" 코드 제거
- 실제 요구사항에 집중

---

## 📚 관련 문서

- [SOLID 원칙](./solid-principles.md)
- [Clean Code - 함수 설계 원칙](./clean-code-functions.md)
- [Clean Code - 의미 있는 이름 짓기](./clean-code-naming.md)
- [에러 처리 패턴](./error-handling-patterns.md)

---

## 📖 추가 학습 자료

| 자료 | 난이도 | 설명 |
|------|--------|------|
| [The Pragmatic Programmer](https://pragprog.com/titles/tpp20/) | ⭐⭐⭐ | DRY 원칙의 바이블 |
| [Clean Code](https://www.amazon.com/Clean-Code-Handbook-Software-Craftsmanship/dp/0132350882) | ⭐⭐⭐⭐ | KISS, YAGNI 실전 예제 |
| [Martin Fowler - Refactoring](https://refactoring.com/) | ⭐⭐⭐ | 중복 제거 기법 |

---

**마지막 업데이트:** 2026-01-22
