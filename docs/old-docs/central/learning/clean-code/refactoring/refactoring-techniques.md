---
id: learning-refactoring-techniques
title: 리팩토링 기법 (Refactoring Techniques)
type: learning
status: current
created: 2026-01-22
updated: 2026-01-22
author: Laze
tags: [refactoring, clean-code, code-quality, best-practices]
difficulty: intermediate
estimated_time: 2-3 hours
---

# 리팩토링 기법 (Refactoring Techniques)

## 📋 학습 목표

이 문서를 학습하고 나면 다음을 할 수 있습니다:

- [ ] 5가지 핵심 리팩토링 기법을 이해하고 적용할 수 있다
- [ ] 코드의 악취(Code Smell)를 식별할 수 있다
- [ ] Before/After 코드의 차이를 분석하고 개선점을 설명할 수 있다
- [ ] Portal Universe 프로젝트에서 리팩토링이 필요한 코드를 찾을 수 있다
- [ ] 리팩토링 후 테스트로 기능 정합성을 검증할 수 있다

## 📚 사전 지식

- Java 또는 TypeScript 기본 문법
- 객체지향 프로그래밍 기초
- Spring Boot 또는 React/Vue 기본 개념
- 단위 테스트(Unit Test) 작성 경험

## ⏱️ 예상 소요 시간

- 이론 학습: 1시간
- 예시 분석: 30분
- 실습 과제: 1-1.5시간

---

## 1️⃣ Extract Method (메서드 추출)

### 📌 개요

긴 메서드나 중복된 코드 블록을 별도의 메서드로 추출하여 가독성과 재사용성을 높입니다.

### 🔴 Code Smell

- 100줄 이상의 긴 메서드
- 주석으로 구분된 논리적 블록
- 동일한 코드 블록이 여러 곳에서 반복

### ✅ 리팩토링 원칙

1. **하나의 메서드는 하나의 책임만 가진다** (SRP - Single Responsibility Principle)
2. **메서드 이름은 의도를 명확히 표현한다**
3. **추출된 메서드는 독립적으로 테스트 가능해야 한다**

### 📊 Before / After

#### ❌ Before: 긴 메서드 (Bad)

```java
@Transactional
public OrderResponse createOrder(String userId, CreateOrderRequest request) {
    // 1. 주문 생성
    Order order = new Order();
    order.setOrderNumber(generateOrderNumber());
    order.setUserId(userId);
    order.setStatus(OrderStatus.PENDING);
    order.setCreatedAt(LocalDateTime.now());

    // 2. 주문 항목 추가
    List<OrderItem> items = new ArrayList<>();
    for (var item : request.getItems()) {
        Product product = productRepository.findById(item.getProductId())
            .orElseThrow(() -> new ProductNotFoundException(item.getProductId()));

        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(product.getId());
        orderItem.setProductName(product.getName());
        orderItem.setPrice(product.getPrice());
        orderItem.setQuantity(item.getQuantity());
        orderItem.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        items.add(orderItem);
    }
    order.setItems(items);

    // 3. 가격 계산
    BigDecimal subtotal = items.stream()
        .map(OrderItem::getSubtotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal shippingFee = subtotal.compareTo(new BigDecimal("50000")) >= 0
        ? BigDecimal.ZERO
        : new BigDecimal("3000");
    BigDecimal totalAmount = subtotal.add(shippingFee);
    order.setTotalAmount(totalAmount);

    // 4. 재고 예약
    for (var item : items) {
        inventoryService.reserveStock(
            item.getProductId(),
            item.getQuantity(),
            "ORDER",
            order.getOrderNumber(),
            userId
        );
    }

    return OrderResponse.from(orderRepository.save(order));
}
```

#### ✅ After: 메서드 추출 (Good)

```java
@Transactional
public OrderResponse createOrder(String userId, CreateOrderRequest request) {
    Order order = initializeOrder(userId);
    List<OrderItem> items = createOrderItems(request.getItems());
    order.setItems(items);
    order.setTotalAmount(calculateTotalAmount(items));

    Order savedOrder = orderRepository.save(order);
    reserveInventoryForOrder(savedOrder, userId);

    return OrderResponse.from(savedOrder);
}

private Order initializeOrder(String userId) {
    Order order = new Order();
    order.setOrderNumber(generateOrderNumber());
    order.setUserId(userId);
    order.setStatus(OrderStatus.PENDING);
    order.setCreatedAt(LocalDateTime.now());
    return order;
}

private List<OrderItem> createOrderItems(List<OrderItemRequest> itemRequests) {
    return itemRequests.stream()
        .map(this::createOrderItem)
        .collect(Collectors.toList());
}

private OrderItem createOrderItem(OrderItemRequest itemRequest) {
    Product product = productRepository.findById(itemRequest.getProductId())
        .orElseThrow(() -> new ProductNotFoundException(itemRequest.getProductId()));

    OrderItem orderItem = new OrderItem();
    orderItem.setProductId(product.getId());
    orderItem.setProductName(product.getName());
    orderItem.setPrice(product.getPrice());
    orderItem.setQuantity(itemRequest.getQuantity());
    orderItem.setSubtotal(calculateSubtotal(product.getPrice(), itemRequest.getQuantity()));
    return orderItem;
}

private BigDecimal calculateSubtotal(BigDecimal price, int quantity) {
    return price.multiply(BigDecimal.valueOf(quantity));
}

private BigDecimal calculateTotalAmount(List<OrderItem> items) {
    BigDecimal subtotal = items.stream()
        .map(OrderItem::getSubtotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal shippingFee = calculateShippingFee(subtotal);
    return subtotal.add(shippingFee);
}

private BigDecimal calculateShippingFee(BigDecimal subtotal) {
    return subtotal.compareTo(new BigDecimal("50000")) >= 0
        ? BigDecimal.ZERO
        : new BigDecimal("3000");
}

private void reserveInventoryForOrder(Order order, String userId) {
    Map<Long, Integer> quantities = order.getItems().stream()
        .collect(Collectors.toMap(
            OrderItem::getProductId,
            OrderItem::getQuantity
        ));
    inventoryService.reserveStockBatch(quantities, "ORDER", order.getOrderNumber(), userId);
}
```

### 📈 개선 효과

| 측면 | Before | After | 개선율 |
|------|--------|-------|--------|
| 메서드 라인 수 | 45줄 | 8줄 (주 메서드) | 82% 감소 |
| Cyclomatic Complexity | 8 | 2 | 75% 감소 |
| 테스트 용이성 | 낮음 | 높음 | - |
| 재사용성 | 없음 | 있음 | - |

---

## 2️⃣ Rename Variable/Function (변수/함수 이름 변경)

### 📌 개요

의미 없는 변수명이나 함수명을 의도를 명확히 드러내는 이름으로 변경합니다.

### 🔴 Code Smell

- 의미 없는 이름: `data`, `temp`, `info`, `x`, `y`, `flag`
- 약어: `prod`, `usr`, `qty`, `amt`
- 한 글자 변수명 (루프 인덱스 제외)
- 타입명을 그대로 사용: `string`, `list`, `map`

### ✅ 리팩토링 원칙

1. **이름으로 의도를 표현한다**
2. **검색 가능한 이름을 사용한다**
3. **약어보다는 전체 단어를 사용한다**
4. **도메인 용어를 사용한다**

### 📊 Before / After

#### ❌ Before: 불명확한 이름 (Bad)

```java
// 재고 체크
public boolean check(Long pid, int qty) {
    var inv = repo.findById(pid).orElseThrow();
    int avail = inv.getTotal() - inv.getReserved();
    return avail >= qty;
}

// 가격 계산
public BigDecimal calc(List<Item> items) {
    BigDecimal result = BigDecimal.ZERO;
    for (Item i : items) {
        BigDecimal price = i.getP();
        int q = i.getQ();
        result = result.add(price.multiply(BigDecimal.valueOf(q)));
    }
    return result;
}
```

#### ✅ After: 명확한 이름 (Good)

```java
// 재고 체크
public boolean isStockAvailable(Long productId, int requestedQuantity) {
    Inventory inventory = inventoryRepository.findById(productId)
        .orElseThrow(() -> new InventoryNotFoundException(productId));

    int availableStock = inventory.getTotalStock() - inventory.getReservedStock();
    return availableStock >= requestedQuantity;
}

// 가격 계산
public BigDecimal calculateTotalPrice(List<OrderItem> orderItems) {
    BigDecimal totalPrice = BigDecimal.ZERO;

    for (OrderItem item : orderItems) {
        BigDecimal unitPrice = item.getPrice();
        int quantity = item.getQuantity();
        BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        totalPrice = totalPrice.add(itemTotal);
    }

    return totalPrice;
}
```

### 🎯 네이밍 가이드

| 목적 | Bad | Good | 이유 |
|------|-----|------|------|
| Boolean | `flag`, `check` | `isActive`, `hasPermission` | 의도 명확 |
| Collection | `list`, `data` | `products`, `orderItems` | 내용 명확 |
| 개수/횟수 | `cnt`, `num` | `productCount`, `retryAttempts` | 검색 가능 |
| 메서드 | `do()`, `proc()` | `validateOrder()`, `calculateTotal()` | 동작 명확 |

---

## 3️⃣ Replace Magic Number with Constant (매직 넘버를 상수로 변경)

### 📌 개요

코드에 직접 작성된 리터럴 숫자를 의미 있는 상수로 변경합니다.

### 🔴 Code Smell

- 숫자의 의미를 알 수 없음
- 같은 숫자가 여러 곳에 중복
- 변경 시 모든 위치를 찾아야 함

### ✅ 리팩토링 원칙

1. **매직 넘버는 상수로 정의한다**
2. **상수명은 의미를 표현한다**
3. **관련 상수는 그룹화한다** (Enum, Constants 클래스)

### 📊 Before / After

#### ❌ Before: 매직 넘버 (Bad)

```java
public BigDecimal calculateShippingFee(BigDecimal orderAmount) {
    if (orderAmount.compareTo(new BigDecimal("50000")) >= 0) {
        return BigDecimal.ZERO;
    }
    return new BigDecimal("3000");
}

public boolean isEligibleForDiscount(int loyaltyPoints) {
    return loyaltyPoints >= 1000;
}

public void processOrder(Order order) {
    if (order.getItems().size() > 10) {
        // 대량 주문 처리
    }

    // 24시간 이내 결제 필요
    LocalDateTime deadline = order.getCreatedAt().plusHours(24);
}
```

#### ✅ After: 상수 사용 (Good)

```java
public class ShippingConstants {
    public static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("50000");
    public static final BigDecimal STANDARD_SHIPPING_FEE = new BigDecimal("3000");
}

public class LoyaltyConstants {
    public static final int DISCOUNT_ELIGIBILITY_POINTS = 1000;
}

public class OrderConstants {
    public static final int BULK_ORDER_THRESHOLD = 10;
    public static final int PAYMENT_DEADLINE_HOURS = 24;
}

// 사용
public BigDecimal calculateShippingFee(BigDecimal orderAmount) {
    if (orderAmount.compareTo(ShippingConstants.FREE_SHIPPING_THRESHOLD) >= 0) {
        return BigDecimal.ZERO;
    }
    return ShippingConstants.STANDARD_SHIPPING_FEE;
}

public boolean isEligibleForDiscount(int loyaltyPoints) {
    return loyaltyPoints >= LoyaltyConstants.DISCOUNT_ELIGIBILITY_POINTS;
}

public void processOrder(Order order) {
    if (order.getItems().size() > OrderConstants.BULK_ORDER_THRESHOLD) {
        // 대량 주문 처리
    }

    LocalDateTime deadline = order.getCreatedAt()
        .plusHours(OrderConstants.PAYMENT_DEADLINE_HOURS);
}
```

### 🎯 상수 관리 전략

#### Enum 사용 (관련된 상수 그룹)

```java
public enum OrderStatus {
    PENDING("대기중", 0),
    PAYMENT_COMPLETED("결제완료", 1),
    PREPARING("준비중", 2),
    SHIPPED("배송중", 3),
    DELIVERED("배송완료", 4),
    CANCELLED("취소됨", 9);

    private final String description;
    private final int priority;

    OrderStatus(String description, int priority) {
        this.description = description;
        this.priority = priority;
    }

    // Getters...
}
```

#### Constants 클래스 사용 (도메인별 상수)

```java
public final class OrderBusinessRules {
    private OrderBusinessRules() {} // 인스턴스화 방지

    // 주문 관련
    public static final int MAX_ORDER_ITEMS = 50;
    public static final int MIN_ORDER_AMOUNT = 1000;

    // 배송 관련
    public static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("50000");
    public static final BigDecimal EXPRESS_SHIPPING_FEE = new BigDecimal("5000");

    // 타임아웃
    public static final int PAYMENT_TIMEOUT_MINUTES = 30;
    public static final int INVENTORY_LOCK_TIMEOUT_SECONDS = 10;
}
```

---

## 4️⃣ Introduce Parameter Object (매개변수 객체 도입)

### 📌 개요

여러 개의 매개변수를 하나의 객체로 묶어서 전달합니다.

### 🔴 Code Smell

- 메서드 매개변수가 4개 이상
- 동일한 매개변수 그룹이 여러 메서드에 반복
- 매개변수 순서를 기억하기 어려움

### ✅ 리팩토링 원칙

1. **관련된 매개변수를 객체로 묶는다**
2. **불변 객체(Immutable)로 만든다**
3. **검증 로직을 객체 내부에 캡슐화한다**

### 📊 Before / After

#### ❌ Before: 긴 매개변수 목록 (Bad)

```java
public InventoryResponse reserveStock(
    Long productId,
    int quantity,
    String referenceType,
    String referenceId,
    String userId,
    LocalDateTime requestedAt,
    String reason,
    boolean sendNotification
) {
    // 재고 예약 로직
}

// 호출
inventoryService.reserveStock(
    productId,
    quantity,
    "ORDER",
    orderNumber,
    userId,
    LocalDateTime.now(),
    "주문 생성",
    true
);
```

#### ✅ After: 매개변수 객체 (Good)

```java
// 매개변수 객체 정의
public record StockReservationRequest(
    Long productId,
    int quantity,
    String referenceType,
    String referenceId,
    String userId,
    LocalDateTime requestedAt,
    String reason,
    boolean sendNotification
) {
    // 생성자에서 검증
    public StockReservationRequest {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 상품 ID");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다");
        }
        if (referenceType == null || referenceType.isBlank()) {
            throw new IllegalArgumentException("참조 유형은 필수입니다");
        }
    }

    // Builder 패턴 제공
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long productId;
        private int quantity;
        private String referenceType;
        private String referenceId;
        private String userId;
        private LocalDateTime requestedAt = LocalDateTime.now();
        private String reason = "";
        private boolean sendNotification = false;

        public Builder productId(Long productId) {
            this.productId = productId;
            return this;
        }

        // 나머지 setter 메서드들...

        public StockReservationRequest build() {
            return new StockReservationRequest(
                productId, quantity, referenceType, referenceId,
                userId, requestedAt, reason, sendNotification
            );
        }
    }
}

// 서비스 메서드
public InventoryResponse reserveStock(StockReservationRequest request) {
    // 재고 예약 로직
}

// 호출
StockReservationRequest request = StockReservationRequest.builder()
    .productId(productId)
    .quantity(quantity)
    .referenceType("ORDER")
    .referenceId(orderNumber)
    .userId(userId)
    .reason("주문 생성")
    .sendNotification(true)
    .build();

inventoryService.reserveStock(request);
```

### 📈 개선 효과

| 측면 | Before | After |
|------|--------|-------|
| 매개변수 개수 | 8개 | 1개 |
| 타입 안전성 | 낮음 (순서 실수 가능) | 높음 (빌더 패턴) |
| 확장성 | 낮음 (메서드 시그니처 변경) | 높음 (필드 추가) |
| 검증 위치 | 메서드 내부 분산 | 객체 생성자에 집중 |

---

## 5️⃣ Replace Conditional with Polymorphism (조건문을 다형성으로 변경)

### 📌 개요

복잡한 조건문(if-else, switch-case)을 다형성을 활용한 객체 지향 구조로 변경합니다.

### 🔴 Code Smell

- 타입/상태에 따른 긴 switch-case 문
- 같은 조건문이 여러 메서드에 반복
- 새로운 타입 추가 시 모든 조건문 수정 필요

### ✅ 리팩토링 원칙

1. **전략 패턴(Strategy Pattern)을 활용한다**
2. **인터페이스로 행동을 정의한다**
3. **구현체에서 구체적인 행동을 구현한다**

### 📊 Before / After

#### ❌ Before: 조건문 (Bad)

```java
public BigDecimal calculateDiscount(Order order, String customerType) {
    BigDecimal discount = BigDecimal.ZERO;
    BigDecimal totalAmount = order.getTotalAmount();

    switch (customerType) {
        case "REGULAR":
            // 일반 고객: 할인 없음
            break;

        case "VIP":
            // VIP 고객: 10% 할인
            if (totalAmount.compareTo(new BigDecimal("100000")) >= 0) {
                discount = totalAmount.multiply(new BigDecimal("0.10"));
            } else {
                discount = totalAmount.multiply(new BigDecimal("0.05"));
            }
            break;

        case "VVIP":
            // VVIP 고객: 15% 할인 + 무료 배송
            discount = totalAmount.multiply(new BigDecimal("0.15"));
            order.setShippingFee(BigDecimal.ZERO);
            break;

        case "EMPLOYEE":
            // 직원: 20% 할인 + 무료 배송
            discount = totalAmount.multiply(new BigDecimal("0.20"));
            order.setShippingFee(BigDecimal.ZERO);
            break;

        default:
            throw new IllegalArgumentException("Unknown customer type: " + customerType);
    }

    return discount;
}
```

#### ✅ After: 다형성 (Good)

```java
// 1. 인터페이스 정의
public interface DiscountPolicy {
    BigDecimal calculateDiscount(Order order);
    void applyBenefits(Order order);
}

// 2. 구현체들
public class RegularCustomerPolicy implements DiscountPolicy {
    @Override
    public BigDecimal calculateDiscount(Order order) {
        return BigDecimal.ZERO; // 할인 없음
    }

    @Override
    public void applyBenefits(Order order) {
        // 추가 혜택 없음
    }
}

public class VipCustomerPolicy implements DiscountPolicy {
    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("100000");
    private static final BigDecimal HIGH_AMOUNT_DISCOUNT_RATE = new BigDecimal("0.10");
    private static final BigDecimal STANDARD_DISCOUNT_RATE = new BigDecimal("0.05");

    @Override
    public BigDecimal calculateDiscount(Order order) {
        BigDecimal totalAmount = order.getTotalAmount();
        BigDecimal discountRate = totalAmount.compareTo(HIGH_AMOUNT_THRESHOLD) >= 0
            ? HIGH_AMOUNT_DISCOUNT_RATE
            : STANDARD_DISCOUNT_RATE;

        return totalAmount.multiply(discountRate);
    }

    @Override
    public void applyBenefits(Order order) {
        // VIP는 추가 혜택 없음
    }
}

public class VvipCustomerPolicy implements DiscountPolicy {
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.15");

    @Override
    public BigDecimal calculateDiscount(Order order) {
        return order.getTotalAmount().multiply(DISCOUNT_RATE);
    }

    @Override
    public void applyBenefits(Order order) {
        order.setShippingFee(BigDecimal.ZERO); // 무료 배송
    }
}

public class EmployeeCustomerPolicy implements DiscountPolicy {
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.20");

    @Override
    public BigDecimal calculateDiscount(Order order) {
        return order.getTotalAmount().multiply(DISCOUNT_RATE);
    }

    @Override
    public void applyBenefits(Order order) {
        order.setShippingFee(BigDecimal.ZERO); // 무료 배송
    }
}

// 3. Factory로 정책 객체 생성
public class DiscountPolicyFactory {
    private static final Map<String, DiscountPolicy> POLICIES = Map.of(
        "REGULAR", new RegularCustomerPolicy(),
        "VIP", new VipCustomerPolicy(),
        "VVIP", new VvipCustomerPolicy(),
        "EMPLOYEE", new EmployeeCustomerPolicy()
    );

    public static DiscountPolicy getPolicy(String customerType) {
        DiscountPolicy policy = POLICIES.get(customerType);
        if (policy == null) {
            throw new IllegalArgumentException("Unknown customer type: " + customerType);
        }
        return policy;
    }
}

// 4. 사용
public BigDecimal calculateDiscount(Order order, String customerType) {
    DiscountPolicy policy = DiscountPolicyFactory.getPolicy(customerType);
    policy.applyBenefits(order);
    return policy.calculateDiscount(order);
}
```

### 📈 개선 효과

| 측면 | Before | After |
|------|--------|-------|
| Cyclomatic Complexity | 7 | 1 (메인 메서드) |
| OCP (Open-Closed Principle) | 위반 | 준수 |
| 신규 고객 타입 추가 | 모든 조건문 수정 | 새 클래스 추가만 |
| 단위 테스트 | 어려움 (모든 분기 테스트) | 쉬움 (클래스별 독립 테스트) |

---

## 🏢 Portal Universe 적용 사례

### 사례 1: OrderService의 Extract Method

위치: `services/shopping-service/src/main/java/com/portal/universe/shoppingservice/order/service/`

현재 `OrderService` 인터페이스는 매우 잘 설계되어 있습니다:

```java
public interface OrderService {
    OrderResponse createOrder(String userId, CreateOrderRequest request);
    OrderResponse getOrder(String userId, String orderNumber);
    Page<OrderResponse> getUserOrders(String userId, Pageable pageable);
    OrderResponse cancelOrder(String userId, String orderNumber, CancelOrderRequest request);
    OrderResponse completeOrderAfterPayment(String orderNumber);
}
```

각 메서드가 **하나의 명확한 책임**을 가지고 있습니다.

### 사례 2: InventoryService의 명확한 네이밍

위치: `services/shopping-service/src/main/java/com/portal/universe/shoppingservice/inventory/service/InventoryService.java`

```java
// ✅ 좋은 예: 의도가 명확한 메서드명
InventoryResponse reserveStock(Long productId, int quantity, ...);
InventoryResponse deductStock(Long productId, int quantity, ...);
InventoryResponse releaseStock(Long productId, int quantity, ...);
InventoryResponse addStock(Long productId, int quantity, ...);
```

각 메서드명이 **정확히 무엇을 하는지** 표현합니다.

### 사례 3: ProductController의 @Deprecated 활용

위치: `services/shopping-service/src/main/java/com/portal/universe/shoppingservice/product/controller/ProductController.java`

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

리팩토링 중 하위 호환성을 유지하면서 새로운 구조로 전환하는 좋은 예시입니다.

---

## 🎯 실습 과제

### 과제 1: Extract Method 연습

다음 코드를 리팩토링하세요:

```java
@Transactional
public CouponResponse issueCoupon(String userId, Long couponTemplateId) {
    // 쿠폰 템플릿 조회
    CouponTemplate template = couponTemplateRepository.findById(couponTemplateId)
        .orElseThrow(() -> new CouponTemplateNotFoundException(couponTemplateId));

    // 발급 가능 여부 확인
    if (!template.isActive()) {
        throw new CouponNotActiveException(couponTemplateId);
    }
    if (template.getIssueStartDate().isAfter(LocalDateTime.now())) {
        throw new CouponNotYetAvailableException(couponTemplateId);
    }
    if (template.getIssueEndDate().isBefore(LocalDateTime.now())) {
        throw new CouponExpiredException(couponTemplateId);
    }

    // 중복 발급 확인
    boolean alreadyIssued = couponRepository.existsByUserIdAndTemplateId(userId, couponTemplateId);
    if (alreadyIssued && !template.isMultipleIssueAllowed()) {
        throw new CouponAlreadyIssuedException(couponTemplateId);
    }

    // 쿠폰 생성
    Coupon coupon = new Coupon();
    coupon.setUserId(userId);
    coupon.setTemplateId(couponTemplateId);
    coupon.setCouponCode(generateCouponCode());
    coupon.setDiscountType(template.getDiscountType());
    coupon.setDiscountValue(template.getDiscountValue());
    coupon.setValidFrom(LocalDateTime.now());
    coupon.setValidUntil(LocalDateTime.now().plusDays(template.getValidityDays()));
    coupon.setStatus(CouponStatus.ISSUED);

    return CouponResponse.from(couponRepository.save(coupon));
}
```

**목표**: 3-5개의 작은 메서드로 분리하세요.

### 과제 2: Replace Magic Number with Constant

다음 코드에서 매직 넘버를 상수로 변경하세요:

```java
public boolean canCancelOrder(Order order) {
    // 주문 후 24시간 이내에만 취소 가능
    LocalDateTime deadline = order.getCreatedAt().plusHours(24);
    if (LocalDateTime.now().isAfter(deadline)) {
        return false;
    }

    // 배송 중이거나 배송 완료된 주문은 취소 불가
    if (order.getStatus().getPriority() >= 3) {
        return false;
    }

    // 환불 금액이 100만원 이상이면 관리자 승인 필요
    if (order.getTotalAmount().compareTo(new BigDecimal("1000000")) >= 0) {
        return order.isAdminApproved();
    }

    return true;
}
```

### 과제 3: Introduce Parameter Object

다음 메서드를 Parameter Object 패턴으로 리팩토링하세요:

```java
public void createTimeDeal(
    String name,
    String description,
    Long productId,
    BigDecimal originalPrice,
    BigDecimal discountedPrice,
    int maxQuantity,
    LocalDateTime startTime,
    LocalDateTime endTime,
    int maxPurchasePerUser
) {
    // 타임딜 생성 로직
}
```

---

## 📚 관련 문서

### 내부 문서

- [코드 리뷰 체크리스트](./code-review-checklist.md) - 리팩토링 적용 확인
- [Clean Code 트레이드오프](../trade-offs.md) - 리팩토링 결정 기준
- [테스트 전략](../testing/) - 리팩토링 후 검증 방법

### 외부 자료

| 자료 | 난이도 | 설명 |
|------|--------|------|
| [Refactoring Guru - Refactoring Techniques](https://refactoring.guru/refactoring/techniques) | ⭐⭐⭐ | 리팩토링 기법 종합 가이드 |
| [Martin Fowler - Refactoring](https://martinfowler.com/books/refactoring.html) | ⭐⭐⭐⭐⭐ | 리팩토링의 바이블 (도서) |
| [Clean Code by Robert C. Martin](https://www.oreilly.com/library/view/clean-code-a/9780136083238/) | ⭐⭐⭐⭐ | 클린 코드 원칙 (도서) |
| [Effective Java by Joshua Bloch](https://www.oreilly.com/library/view/effective-java/9780134686097/) | ⭐⭐⭐⭐⭐ | Java 리팩토링 베스트 프랙티스 |

---

## ✅ 학습 체크리스트

- [ ] Extract Method를 적용하여 100줄 이상의 메서드를 분해할 수 있다
- [ ] 의미 없는 변수명을 찾아 개선할 수 있다
- [ ] 코드 내 매직 넘버를 식별하고 상수로 추출할 수 있다
- [ ] 4개 이상의 매개변수를 Parameter Object로 리팩토링할 수 있다
- [ ] 복잡한 조건문을 다형성으로 변경할 수 있다
- [ ] 리팩토링 후 테스트를 통해 기능 정합성을 검증할 수 있다
- [ ] 실습 과제 3개를 완료했다

---

## 📌 핵심 요약

1. **Extract Method**: 긴 메서드를 작은 메서드로 분해 → 가독성 ↑, 재사용성 ↑
2. **Rename**: 명확한 이름 사용 → 의도 전달 ↑, 유지보수성 ↑
3. **Replace Magic Number**: 상수화 → 변경 용이성 ↑, 실수 ↓
4. **Parameter Object**: 매개변수 객체화 → 타입 안전성 ↑, 확장성 ↑
5. **Polymorphism**: 조건문을 다형성으로 → 복잡도 ↓, OCP 준수

> **"리팩토링은 기능 변경 없이 코드 구조를 개선하는 것입니다. 항상 테스트와 함께 진행하세요!"**
