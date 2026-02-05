---
id: learning-solid-principles
title: SOLID 원칙 - 객체지향 설계 5원칙
type: learning
status: current
created: 2026-01-22
updated: 2026-01-22
author: Laze
tags: [clean-code, solid, oop, design-principles, architecture]
difficulty: intermediate
estimated_time: 60분
---

# SOLID 원칙 - 객체지향 설계 5원칙

## 📋 학습 목표

- SOLID 5가지 원칙(SRP, OCP, LSP, ISP, DIP)의 개념과 목적 이해
- 각 원칙을 위반하는 코드와 준수하는 코드의 차이점 파악
- Portal Universe 프로젝트에서의 SOLID 원칙 적용 사례 학습
- 유지보수하기 쉽고 확장 가능한 객체지향 설계 역량 습득

## 🎯 사전 지식

- Java 기본 문법 (클래스, 인터페이스, 상속)
- 객체지향 프로그래밍 기본 개념
- Spring Framework 기초 (의존성 주입)

## 📚 SOLID 5원칙 개요

SOLID는 로버트 마틴(Robert C. Martin, Uncle Bob)이 제시한 객체지향 설계의 5가지 기본 원칙입니다.

| 원칙 | 약자 | 핵심 개념 |
|------|------|----------|
| Single Responsibility | SRP | 단일 책임 원칙 |
| Open-Closed | OCP | 개방-폐쇄 원칙 |
| Liskov Substitution | LSP | 리스코프 치환 원칙 |
| Interface Segregation | ISP | 인터페이스 분리 원칙 |
| Dependency Inversion | DIP | 의존관계 역전 원칙 |

---

## 1️⃣ SRP - Single Responsibility Principle (단일 책임 원칙)

### 원칙 설명

> **"하나의 클래스는 하나의 책임만 가져야 한다."**
> **"클래스를 변경하는 이유는 단 하나여야 한다."**

클래스가 여러 책임을 가지면, 한 책임의 변경이 다른 책임에 영향을 미칩니다.

### ❌ Bad Example

```java
// 여러 책임을 가진 클래스 (SRP 위반)
public class OrderProcessor {
    // 책임 1: 주문 처리
    public void processOrder(Order order) {
        // 주문 검증
        if (order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Empty order");
        }

        // 책임 2: 재고 감소
        for (OrderItem item : order.getItems()) {
            reduceInventory(item.getProductId(), item.getQuantity());
        }

        // 책임 3: 결제 처리
        processPayment(order.getTotalAmount(), order.getUserId());

        // 책임 4: 이메일 발송
        sendOrderConfirmationEmail(order.getUserEmail(), order);

        // 책임 5: 로깅
        logOrderProcessing(order.getId());
    }

    private void reduceInventory(Long productId, int quantity) { /* ... */ }
    private void processPayment(BigDecimal amount, String userId) { /* ... */ }
    private void sendOrderConfirmationEmail(String email, Order order) { /* ... */ }
    private void logOrderProcessing(Long orderId) { /* ... */ }
}
```

**문제점:**
- 재고, 결제, 알림, 로깅 등 여러 이유로 변경될 수 있음
- 테스트하기 어려움 (모든 의존성을 Mock 처리해야 함)
- 코드 재사용 불가능

### ✅ Good Example

```java
// 각 클래스가 단일 책임을 가짐 (SRP 준수)
@Service
@RequiredArgsConstructor
public class OrderService {
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        // 이 클래스는 "주문 흐름 조정"만 책임
        Order order = buildOrder(request);

        inventoryService.reserveStock(order);
        paymentService.processPayment(order);
        notificationService.sendOrderConfirmation(order);

        return OrderResponse.from(orderRepository.save(order));
    }
}

@Service
public class InventoryService {
    // "재고 관리"만 책임
    public void reserveStock(Order order) { /* ... */ }
}

@Service
public class PaymentService {
    // "결제 처리"만 책임
    public void processPayment(Order order) { /* ... */ }
}

@Service
public class NotificationService {
    // "알림 발송"만 책임
    public void sendOrderConfirmation(Order order) { /* ... */ }
}
```

### 🏗️ Portal Universe 적용 사례

**1. OrderServiceImpl - 주문 흐름 조정만 책임**

```java
// services/shopping-service/.../order/service/OrderServiceImpl.java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final OrderSagaOrchestrator orderSagaOrchestrator;  // Saga 오케스트레이션 위임
    private final InventoryService inventoryService;            // 재고 관리 위임
    private final CouponService couponService;                  // 쿠폰 관리 위임

    @Transactional
    public OrderResponse createOrder(String userId, CreateOrderRequest request) {
        // 1. 장바구니 조회
        Cart cart = cartRepository.findByUserIdAndStatusWithItems(userId, CartStatus.CHECKED_OUT)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.CART_NOT_FOUND));

        // 2. 주문 생성 (엔티티 생성 로직만)
        Order order = Order.builder()
                .userId(userId)
                .shippingAddress(request.shippingAddress().toEntity())
                .build();

        // 3. 쿠폰 적용 (CouponService에 위임)
        if (request.userCouponId() != null) {
            couponService.validateCouponForOrder(request.userCouponId(), userId, order.getTotalAmount());
            BigDecimal discountAmount = couponService.calculateDiscount(request.userCouponId(), order.getTotalAmount());
            order.applyCoupon(request.userCouponId(), discountAmount);
        }

        // 4. Saga 시작 (Saga 로직은 OrderSagaOrchestrator에 위임)
        orderSagaOrchestrator.startSaga(order);

        return OrderResponse.from(order);
    }
}
```

**SRP 준수 포인트:**
- `OrderService`: 주문 흐름 조정
- `CouponService`: 쿠폰 검증/할인 계산
- `OrderSagaOrchestrator`: Saga 패턴 오케스트레이션
- `InventoryService`: 재고 관리

**2. ProductServiceImpl - 상품 관리만 책임**

```java
// services/shopping-service/.../product/service/ProductServiceImpl.java
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final BlogServiceClient blogServiceClient;  // 리뷰 조회는 FeignClient에 위임

    @Override
    public ProductResponse createProduct(ProductCreateRequest request) {
        // 상품 생성 로직만 담당
        Product newProduct = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .stock(request.stock())
                .build();

        return convertToResponse(productRepository.save(newProduct));
    }

    // 리뷰 조회는 BlogService에 위임 (서비스 오케스트레이션)
    public ProductWithReviewsResponse getProductWithReviews(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND));

        List<BlogResponse> reviews = blogServiceClient.getPostByProductId(String.valueOf(productId));

        return new ProductWithReviewsResponse(/* ... */);
    }
}
```

---

## 2️⃣ OCP - Open-Closed Principle (개방-폐쇄 원칙)

### 원칙 설명

> **"소프트웨어 엔티티(클래스, 모듈, 함수)는 확장에는 열려 있어야 하고, 수정에는 닫혀 있어야 한다."**

새로운 기능을 추가할 때 기존 코드를 수정하지 않고, 확장으로 대응해야 합니다.

### ❌ Bad Example

```java
// 새로운 결제 수단 추가 시 기존 코드 수정 필요 (OCP 위반)
public class PaymentProcessor {
    public void processPayment(Order order, String paymentType) {
        if (paymentType.equals("CARD")) {
            // 카드 결제 처리
            processCardPayment(order);
        } else if (paymentType.equals("BANK_TRANSFER")) {
            // 계좌이체 처리
            processBankTransfer(order);
        } else if (paymentType.equals("KAKAO_PAY")) {
            // 카카오페이 추가 시 기존 코드 수정 필요! (OCP 위반)
            processKakaoPay(order);
        }
    }
}
```

**문제점:**
- 새로운 결제 수단 추가 시 `processPayment()` 메서드 수정 필요
- if-else 분기가 계속 늘어남
- 단위 테스트 시 모든 케이스를 다시 검증해야 함

### ✅ Good Example

```java
// 인터페이스를 통한 확장 (OCP 준수)
public interface PaymentStrategy {
    PaymentResult process(Order order);
}

@Component
public class CardPaymentStrategy implements PaymentStrategy {
    @Override
    public PaymentResult process(Order order) {
        // 카드 결제 로직
        return new PaymentResult(/* ... */);
    }
}

@Component
public class KakaoPayStrategy implements PaymentStrategy {
    @Override
    public PaymentResult process(Order order) {
        // 카카오페이 로직 (기존 코드 수정 없이 새 클래스 추가)
        return new PaymentResult(/* ... */);
    }
}

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final Map<String, PaymentStrategy> strategies;

    public PaymentResult processPayment(Order order, String paymentType) {
        PaymentStrategy strategy = strategies.get(paymentType);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported payment type: " + paymentType);
        }
        return strategy.process(order);
    }
}
```

### 🏗️ Portal Universe 적용 사례

**1. ErrorCode 인터페이스 - 확장 가능한 에러 코드 체계**

```java
// services/common-library/.../exception/ErrorCode.java
public interface ErrorCode {
    HttpStatus getStatus();
    String getCode();
    String getMessage();
}

// 각 서비스별로 ErrorCode 구현 (OCP 준수)
@Getter
public enum ShoppingErrorCode implements ErrorCode {
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "Product not found"),
    CART_NOT_FOUND(HttpStatus.NOT_FOUND, "S101", "Cart not found"),
    // ...
}

@Getter
public enum AuthErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "A001", "User not found"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A002", "Invalid credentials"),
    // ...
}
```

**OCP 준수 포인트:**
- 새로운 서비스 추가 시 `ErrorCode` 인터페이스 구현만 하면 됨
- `GlobalExceptionHandler`는 수정할 필요 없음
- 각 서비스의 에러 코드는 독립적으로 관리

**2. Spring Service Interface - 구현체 교체 가능**

```java
// 인터페이스 정의 (변경 닫힘)
public interface ProductService {
    ProductResponse createProduct(ProductCreateRequest request);
    ProductResponse getProductById(Long id);
}

// 구현체 1 (확장 열림)
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;

    @Override
    public ProductResponse createProduct(ProductCreateRequest request) {
        // MySQL 기반 구현
    }
}

// 구현체 2 (향후 추가 가능, 기존 코드 수정 불필요)
@Service
@Profile("mongodb")
public class ProductMongoServiceImpl implements ProductService {
    private final ProductMongoRepository productMongoRepository;

    @Override
    public ProductResponse createProduct(ProductCreateRequest request) {
        // MongoDB 기반 구현
    }
}
```

---

## 3️⃣ LSP - Liskov Substitution Principle (리스코프 치환 원칙)

### 원칙 설명

> **"서브타입은 언제나 기반 타입으로 교체할 수 있어야 한다."**
> **"부모 클래스가 들어갈 자리에 자식 클래스를 넣어도 계획대로 잘 동작해야 한다."**

### ❌ Bad Example

```java
// LSP 위반 예시
class Rectangle {
    protected int width;
    protected int height;

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getArea() {
        return width * height;
    }
}

class Square extends Rectangle {
    @Override
    public void setWidth(int width) {
        this.width = width;
        this.height = width;  // 정사각형은 width = height여야 함
    }

    @Override
    public void setHeight(int height) {
        this.width = height;
        this.height = height;
    }
}

// 테스트
Rectangle rect = new Square();
rect.setWidth(5);
rect.setHeight(4);
System.out.println(rect.getArea());  // 예상: 20, 실제: 16 (LSP 위반!)
```

**문제점:**
- `Square`는 `Rectangle`의 행동 규약을 위반
- 클라이언트 코드가 예상하지 못한 동작 발생

### ✅ Good Example

```java
// LSP 준수 - 인터페이스로 공통 행동 정의
interface Shape {
    int getArea();
}

class Rectangle implements Shape {
    private final int width;
    private final int height;

    public Rectangle(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public int getArea() {
        return width * height;
    }
}

class Square implements Shape {
    private final int side;

    public Square(int side) {
        this.side = side;
    }

    @Override
    public int getArea() {
        return side * side;
    }
}
```

### 🏗️ Portal Universe 적용 사례

**1. CustomBusinessException - 일관된 예외 처리**

```java
// services/common-library/.../exception/CustomBusinessException.java
@Getter
public class CustomBusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public CustomBusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}

// 어디서든 RuntimeException으로 치환 가능
public void someMethod() {
    try {
        // ...
    } catch (RuntimeException e) {  // CustomBusinessException도 동일하게 처리
        log.error("Error occurred", e);
    }
}
```

**LSP 준수 포인트:**
- `CustomBusinessException`은 `RuntimeException`의 모든 행동을 준수
- `RuntimeException` 대신 사용해도 동작 변경 없음

---

## 4️⃣ ISP - Interface Segregation Principle (인터페이스 분리 원칙)

### 원칙 설명

> **"클라이언트는 자신이 사용하지 않는 메서드에 의존하지 않아야 한다."**
> **"범용 인터페이스 하나보다 구체적인 여러 인터페이스가 낫다."**

### ❌ Bad Example

```java
// 비대한 인터페이스 (ISP 위반)
public interface ProductService {
    // 일반 사용자용
    ProductResponse getProduct(Long id);
    List<ProductResponse> searchProducts(String keyword);

    // 관리자용 (일반 사용자는 사용하지 않음!)
    void deleteProduct(Long id);
    void updateStock(Long id, int stock);
    void setProductActive(Long id, boolean active);

    // 통계용 (대부분의 클라이언트가 사용하지 않음!)
    ProductStatistics getStatistics(Long id);
    List<ProductSalesReport> getSalesReport(LocalDate from, LocalDate to);
}

// 일반 사용자 컨트롤러 - 불필요한 메서드까지 의존
@RestController
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;  // 관리자/통계 메서드까지 노출됨

    @GetMapping("/products/{id}")
    public ProductResponse getProduct(@PathVariable Long id) {
        return productService.getProduct(id);
    }
}
```

**문제점:**
- 클라이언트가 사용하지 않는 메서드까지 의존
- 인터페이스 변경 시 영향 범위가 큼
- 역할 분리가 불명확

### ✅ Good Example

```java
// 인터페이스 분리 (ISP 준수)
public interface ProductQueryService {
    ProductResponse getProduct(Long id);
    List<ProductResponse> searchProducts(String keyword);
}

public interface ProductAdminService {
    void deleteProduct(Long id);
    void updateStock(Long id, int stock);
    void setProductActive(Long id, boolean active);
}

public interface ProductStatisticsService {
    ProductStatistics getStatistics(Long id);
    List<ProductSalesReport> getSalesReport(LocalDate from, LocalDate to);
}

// 일반 사용자 컨트롤러 - 필요한 인터페이스만 의존
@RestController
@RequiredArgsConstructor
public class ProductController {
    private final ProductQueryService productQueryService;  // 조회 기능만 의존

    @GetMapping("/products/{id}")
    public ProductResponse getProduct(@PathVariable Long id) {
        return productQueryService.getProduct(id);
    }
}

// 관리자 컨트롤러
@RestController
@RequiredArgsConstructor
public class AdminProductController {
    private final ProductQueryService productQueryService;
    private final ProductAdminService productAdminService;  // 관리 기능만 의존

    @DeleteMapping("/admin/products/{id}")
    public void deleteProduct(@PathVariable Long id) {
        productAdminService.deleteProduct(id);
    }
}
```

### 🏗️ Portal Universe 적용 사례

**1. Service 인터페이스 분리**

Portal Universe는 대부분의 Service에서 단일 인터페이스를 사용하지만, 복잡한 도메인에서는 ISP를 준수합니다.

```java
// 쿠폰 서비스 - 사용자용 메서드만 노출
public interface CouponService {
    void issueCoupon(String userId, Long couponId);
    void useCoupon(Long userCouponId, Long orderId);
    void validateCouponForOrder(Long userCouponId, String userId, BigDecimal orderAmount);
}

// 관리자용 쿠폰 관리는 별도 컨트롤러/서비스로 분리
@RestController
@RequestMapping("/api/v1/admin/coupons")
public class AdminCouponController {
    // 관리자 전용 기능
}
```

---

## 5️⃣ DIP - Dependency Inversion Principle (의존관계 역전 원칙)

### 원칙 설명

> **"고수준 모듈은 저수준 모듈에 의존해서는 안 된다. 둘 다 추상화에 의존해야 한다."**
> **"추상화는 구체적인 사항에 의존해서는 안 된다. 구체적인 사항이 추상화에 의존해야 한다."**

### ❌ Bad Example

```java
// 구체적인 구현에 직접 의존 (DIP 위반)
@Service
public class OrderService {
    // MySQLOrderRepository라는 구체 클래스에 직접 의존
    private final MySQLOrderRepository orderRepository = new MySQLOrderRepository();

    public void createOrder(Order order) {
        orderRepository.save(order);
    }
}

public class MySQLOrderRepository {
    public void save(Order order) {
        // MySQL 저장 로직
    }
}
```

**문제점:**
- `OrderService`가 `MySQLOrderRepository` 구체 클래스에 강하게 결합
- 데이터베이스 변경 시 `OrderService` 코드 수정 필요
- 테스트 시 Mock 객체 주입 불가능

### ✅ Good Example

```java
// 인터페이스(추상화)에 의존 (DIP 준수)
@Service
@RequiredArgsConstructor
public class OrderService {
    // OrderRepository 인터페이스(추상화)에 의존
    private final OrderRepository orderRepository;

    public void createOrder(Order order) {
        orderRepository.save(order);
    }
}

// 추상화 (고수준)
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByUserId(String userId);
}

// 구체 구현 (저수준) - Spring Data JPA가 자동 생성
// OrderService는 이 구현체를 알 필요 없음
```

### 🏗️ Portal Universe 적용 사례

**1. Spring의 의존성 주입 - DIP의 완벽한 예시**

```java
// services/shopping-service/.../product/service/ProductServiceImpl.java
@Service
@RequiredArgsConstructor  // 생성자 주입으로 DIP 준수
public class ProductServiceImpl implements ProductService {
    // 구체 클래스가 아닌 인터페이스에 의존
    private final ProductRepository productRepository;      // Spring Data JPA 인터페이스
    private final BlogServiceClient blogServiceClient;      // Feign 인터페이스

    @Override
    public ProductResponse createProduct(ProductCreateRequest request) {
        Product newProduct = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .stock(request.stock())
                .build();

        // 구체적인 저장 방식은 ProductRepository 구현체가 결정
        return convertToResponse(productRepository.save(newProduct));
    }
}
```

**DIP 준수 포인트:**
- `ProductServiceImpl`은 `ProductRepository` 인터페이스에만 의존
- 실제 구현체(`SimpleJpaRepository`)는 Spring Data JPA가 런타임에 주입
- 테스트 시 Mock 객체로 쉽게 교체 가능

**2. Feign Client - 외부 서비스 통신도 추상화**

```java
// Feign 인터페이스 (추상화)
@FeignClient(name = "blog-service", url = "${blog-service.url}")
public interface BlogServiceClient {
    @GetMapping("/api/v1/posts/product/{productId}")
    List<BlogResponse> getPostByProductId(@PathVariable String productId);
}

// ProductService는 Feign 구현 방식을 몰라도 됨
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final BlogServiceClient blogServiceClient;  // 인터페이스에 의존

    public ProductWithReviewsResponse getProductWithReviews(Long productId) {
        // HTTP 통신 세부사항은 Feign이 처리
        List<BlogResponse> reviews = blogServiceClient.getPostByProductId(String.valueOf(productId));
        // ...
    }
}
```

**DIP 준수 포인트:**
- HTTP 통신 세부사항은 Feign 라이브러리가 처리
- `ProductService`는 "블로그 리뷰를 가져온다"는 추상화된 메서드만 사용
- 향후 Feign 대신 RestTemplate이나 WebClient로 변경해도 Service 코드는 불변

---

## ✅ SOLID 원칙 준수 체크리스트

### SRP (Single Responsibility Principle)
- [ ] 각 클래스는 단 하나의 변경 이유만 가지는가?
- [ ] 메서드명이 클래스의 책임을 명확히 표현하는가?
- [ ] 한 클래스에 비즈니스 로직, 데이터 접근, 알림 등이 섞여 있지 않은가?

### OCP (Open-Closed Principle)
- [ ] 새로운 기능 추가 시 기존 코드를 수정하지 않고 확장 가능한가?
- [ ] 인터페이스나 추상 클래스를 활용하여 다형성을 구현했는가?
- [ ] if-else 분기가 과도하게 많지 않은가? (Strategy 패턴 고려)

### LSP (Liskov Substitution Principle)
- [ ] 자식 클래스가 부모 클래스의 행동 규약을 위반하지 않는가?
- [ ] 부모 타입을 자식 타입으로 치환해도 프로그램이 정상 동작하는가?
- [ ] 오버라이드한 메서드가 예상치 못한 동작을 하지 않는가?

### ISP (Interface Segregation Principle)
- [ ] 클라이언트가 사용하지 않는 메서드에 의존하지 않는가?
- [ ] 인터페이스가 단일 역할을 가지고 있는가?
- [ ] 비대한 인터페이스를 더 작은 인터페이스로 분리할 수 있는가?

### DIP (Dependency Inversion Principle)
- [ ] 고수준 모듈이 저수준 모듈에 직접 의존하지 않는가?
- [ ] 구체 클래스 대신 인터페이스나 추상 클래스에 의존하는가?
- [ ] Spring의 `@RequiredArgsConstructor`로 생성자 주입을 사용하는가?

---

## 📊 SOLID 원칙 적용 효과

| 항목 | SOLID 미준수 | SOLID 준수 |
|------|-------------|-----------|
| 유지보수성 | 변경 시 여러 곳 수정 필요 | 변경 영역 최소화 |
| 테스트 용이성 | Mock 객체 주입 어려움 | 쉬운 단위 테스트 |
| 재사용성 | 강한 결합으로 재사용 불가 | 느슨한 결합으로 재사용 가능 |
| 확장성 | 기존 코드 수정 필요 | 새 클래스 추가로 확장 |
| 가독성 | 책임이 불명확 | 명확한 역할 분리 |

---

## 📚 관련 문서

- [Clean Code - 함수 설계 원칙](./clean-code-functions.md)
- [Clean Code - 의미 있는 이름 짓기](./clean-code-naming.md)
- [에러 처리 패턴](./error-handling-patterns.md)
- [DRY, KISS, YAGNI 원칙](./dry-kiss-yagni.md)
- [아키텍처 트레이드오프 분석](../trade-offs.md)

---

## 📖 추가 학습 자료

| 자료 | 난이도 | 설명 |
|------|--------|------|
| [Clean Code (로버트 마틴)](https://www.amazon.com/Clean-Code-Handbook-Software-Craftsmanship/dp/0132350882) | ⭐⭐⭐⭐ | SOLID 원칙의 바이블 |
| [Spring in Action](https://www.manning.com/books/spring-in-action-sixth-edition) | ⭐⭐⭐ | Spring의 DIP 구현 이해 |
| [Refactoring Guru - SOLID](https://refactoring.guru/design-patterns/principles) | ⭐⭐ | 시각적 설명과 예제 |

---

**마지막 업데이트:** 2026-01-22
