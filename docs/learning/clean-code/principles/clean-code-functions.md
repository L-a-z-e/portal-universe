---
id: learning-clean-code-functions
title: Clean Code - 함수 설계 원칙
type: learning
status: current
created: 2026-01-22
updated: 2026-01-22
author: Portal Universe Team
tags: [clean-code, function, method, design, refactoring]
difficulty: intermediate
estimated_time: 50분
---

# Clean Code - 함수 설계 원칙

## 📋 학습 목표

- 작고 명확한 함수 작성법 습득
- 함수의 단일 책임 원칙(SRP) 이해
- 함수 인자 개수와 부수 효과 관리 방법 학습
- Portal Universe 프로젝트의 함수 설계 패턴 분석

## 🎯 사전 지식

- Java 메서드 기본 문법
- 객체지향 프로그래밍 기초
- SOLID 원칙 (특히 SRP)

## 📚 함수의 중요성

> **"함수는 프로그램의 가장 기본적인 구성 요소다."**

잘 작성된 함수는:
- 읽기 쉽고 이해하기 쉽다
- 테스트하기 쉽다
- 재사용하기 쉽다
- 버그가 적다

---

## 1️⃣ 작게 만들어라

### 원칙

> **"함수의 첫 번째 규칙은 작게 만드는 것이다. 두 번째 규칙은 더 작게 만드는 것이다."**

- 함수는 20줄 이내가 이상적
- 들여쓰기 레벨은 1~2단계가 적당
- 한 화면에 다 보여야 함

### ❌ Bad Example

```java
// 너무 긴 함수 (여러 책임을 가짐)
@Transactional
public OrderResponse processOrder(String userId, CreateOrderRequest request) {
    // 1. 장바구니 조회 및 검증
    Cart cart = cartRepository.findByUserIdAndStatusWithItems(userId, CartStatus.CHECKED_OUT)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.CART_NOT_FOUND));

    if (cart.getItems().isEmpty()) {
        throw new CustomBusinessException(ShoppingErrorCode.CART_EMPTY);
    }

    // 2. 재고 확인 및 예약
    for (CartItem item : cart.getItems()) {
        Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.INVENTORY_NOT_FOUND));

        if (inventory.getAvailableStock() < item.getQuantity()) {
            throw new CustomBusinessException(ShoppingErrorCode.INSUFFICIENT_STOCK);
        }

        inventory.reserve(item.getQuantity());
        inventoryRepository.save(inventory);
    }

    // 3. 주문 생성
    Order order = Order.builder()
            .userId(userId)
            .shippingAddress(request.shippingAddress().toEntity())
            .build();

    for (CartItem cartItem : cart.getItems()) {
        order.addItem(
                cartItem.getProductId(),
                cartItem.getProductName(),
                cartItem.getPrice(),
                cartItem.getQuantity()
        );
    }

    // 4. 쿠폰 적용
    if (request.userCouponId() != null) {
        UserCoupon userCoupon = userCouponRepository.findById(request.userCouponId())
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.USER_COUPON_NOT_FOUND));

        if (userCoupon.isUsed()) {
            throw new CustomBusinessException(ShoppingErrorCode.USER_COUPON_ALREADY_USED);
        }

        if (userCoupon.isExpired()) {
            throw new CustomBusinessException(ShoppingErrorCode.USER_COUPON_EXPIRED);
        }

        BigDecimal discountAmount = calculateDiscount(userCoupon, order.getTotalAmount());
        order.applyCoupon(request.userCouponId(), discountAmount);

        userCoupon.use(order.getId());
        userCouponRepository.save(userCoupon);
    }

    // 5. 결제 처리
    Payment payment = Payment.builder()
            .orderId(order.getId())
            .amount(order.getFinalAmount())
            .paymentMethod(request.paymentMethod())
            .build();

    // PG 연동 로직
    // ...

    order.confirm();
    return OrderResponse.from(orderRepository.save(order));
}
```

**문제점:**
- 80줄이 넘는 긴 함수
- 여러 책임을 가짐 (장바구니 검증, 재고 예약, 주문 생성, 쿠폰 처리, 결제)
- 테스트하기 어려움
- 재사용 불가능

### ✅ Good Example

```java
// 작은 함수들로 분리 (각 함수는 한 가지 일만)
@Transactional
public OrderResponse createOrder(String userId, CreateOrderRequest request) {
    Cart cart = validateAndGetCart(userId);
    reserveInventory(cart);

    Order order = buildOrder(userId, cart, request);
    applyCouponIfPresent(order, request.userCouponId());

    orderSagaOrchestrator.startSaga(order);

    return OrderResponse.from(orderRepository.save(order));
}

private Cart validateAndGetCart(String userId) {
    Cart cart = cartRepository.findByUserIdAndStatusWithItems(userId, CartStatus.CHECKED_OUT)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.CART_NOT_FOUND));

    if (cart.getItems().isEmpty()) {
        throw new CustomBusinessException(ShoppingErrorCode.CART_EMPTY);
    }

    return cart;
}

private void reserveInventory(Cart cart) {
    for (CartItem item : cart.getItems()) {
        inventoryService.reserveStock(item.getProductId(), item.getQuantity());
    }
}

private Order buildOrder(String userId, Cart cart, CreateOrderRequest request) {
    Order order = Order.builder()
            .userId(userId)
            .shippingAddress(request.shippingAddress().toEntity())
            .build();

    cart.getItems().forEach(item ->
            order.addItem(item.getProductId(), item.getProductName(), item.getPrice(), item.getQuantity())
    );

    return order;
}

private void applyCouponIfPresent(Order order, Long userCouponId) {
    if (userCouponId != null) {
        couponService.validateCouponForOrder(userCouponId, order.getUserId(), order.getTotalAmount());
        BigDecimal discountAmount = couponService.calculateDiscount(userCouponId, order.getTotalAmount());
        order.applyCoupon(userCouponId, discountAmount);
        couponService.useCoupon(userCouponId, order.getId());
    }
}
```

**개선점:**
- 메인 함수는 10줄 이하
- 각 함수는 한 가지 일만 수행
- 추상화 레벨이 일관됨
- 테스트하기 쉬움

### 🏗️ Portal Universe 적용 사례

```java
// services/shopping-service/.../order/service/OrderServiceImpl.java

@Override
@Transactional
public OrderResponse createOrder(String userId, CreateOrderRequest request) {
    // 1. 체크아웃된 장바구니 조회
    Cart cart = cartRepository.findByUserIdAndStatusWithItems(userId, CartStatus.CHECKED_OUT)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.CART_NOT_FOUND));

    if (cart.getItems().isEmpty()) {
        throw new CustomBusinessException(ShoppingErrorCode.CART_EMPTY);
    }

    // 2. 주문 생성
    Order order = Order.builder()
            .userId(userId)
            .shippingAddress(request.shippingAddress().toEntity())
            .build();

    // 장바구니 항목을 주문 항목으로 변환
    for (CartItem cartItem : cart.getItems()) {
        order.addItem(
                cartItem.getProductId(),
                cartItem.getProductName(),
                cartItem.getPrice(),
                cartItem.getQuantity()
        );
    }

    // 3. 쿠폰 적용 (선택 사항)
    if (request.userCouponId() != null) {
        couponService.validateCouponForOrder(request.userCouponId(), userId, order.getTotalAmount());
        BigDecimal discountAmount = couponService.calculateDiscount(request.userCouponId(), order.getTotalAmount());
        order.applyCoupon(request.userCouponId(), discountAmount);
    }

    order.confirm();
    Order savedOrder = orderRepository.save(order);

    // 4. 쿠폰 사용 처리
    if (request.userCouponId() != null) {
        couponService.useCoupon(request.userCouponId(), savedOrder.getId());
    }

    // 5. Saga 시작 (재고 예약)
    orderSagaOrchestrator.startSaga(savedOrder);

    return OrderResponse.from(savedOrder);
}
```

**적용 포인트:**
- 메인 로직 50줄 이하
- 쿠폰 검증/할인 계산은 `CouponService`에 위임
- Saga 오케스트레이션은 `OrderSagaOrchestrator`에 위임
- 주석으로 단계 구분

---

## 2️⃣ 한 가지만 해라

### 원칙

> **"함수는 한 가지를 해야 한다. 그 한 가지를 잘 해야 한다. 그 한 가지만을 해야 한다."**

### ❌ Bad Example

```java
// 여러 가지 일을 하는 함수 (SRP 위반)
public void processUser(User user) {
    // 1. 데이터 검증
    if (user.getEmail() == null || !user.getEmail().contains("@")) {
        throw new IllegalArgumentException("Invalid email");
    }

    // 2. 비즈니스 로직
    user.setStatus(UserStatus.ACTIVE);
    user.setLastLoginDate(LocalDateTime.now());

    // 3. 데이터 저장
    userRepository.save(user);

    // 4. 외부 알림
    emailService.sendWelcomeEmail(user.getEmail());
    slackService.notifyAdmins("New user: " + user.getName());

    // 5. 로깅
    log.info("User processed: {}", user.getId());
}
```

**문제점:**
- 검증, 비즈니스 로직, 저장, 알림, 로깅 등 5가지 책임
- 한 부분을 수정하려면 전체 함수를 이해해야 함
- 테스트 시 모든 의존성을 Mock 해야 함

### ✅ Good Example

```java
// 한 가지 일만 하는 함수들
public void processUser(User user) {
    validateUser(user);
    activateUser(user);
    saveUser(user);
    notifyUserActivation(user);
}

private void validateUser(User user) {
    if (user.getEmail() == null || !user.getEmail().contains("@")) {
        throw new IllegalArgumentException("Invalid email");
    }
}

private void activateUser(User user) {
    user.setStatus(UserStatus.ACTIVE);
    user.setLastLoginDate(LocalDateTime.now());
}

private void saveUser(User user) {
    userRepository.save(user);
    log.info("User saved: {}", user.getId());
}

private void notifyUserActivation(User user) {
    emailService.sendWelcomeEmail(user.getEmail());
    slackService.notifyAdmins("New user: " + user.getName());
}
```

### 🏗️ Portal Universe 적용 사례

```java
// services/shopping-service/.../product/service/ProductServiceImpl.java

// 각 메서드는 한 가지 책임만
@Override
public ProductResponse getProductById(Long id) {
    // 1. 상품 조회만
    Product product = productRepository.findById(id)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND));

    return convertToResponse(product);
}

@Override
public ProductResponse createProduct(ProductCreateRequest request) {
    // 2. 상품 생성만
    Product newProduct = Product.builder()
            .name(request.name())
            .description(request.description())
            .price(request.price())
            .stock(request.stock())
            .build();

    return convertToResponse(productRepository.save(newProduct));
}

private ProductResponse convertToResponse(Product product) {
    // 3. Entity -> DTO 변환만
    return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getStock()
    );
}
```

---

## 3️⃣ 함수 인자

### 원칙

> **"이상적인 인자 개수는 0개다. 다음은 1개, 그 다음은 2개다. 3개 이상은 피하라."**

### 인자 개수별 특징

| 개수 | 이름 | 복잡도 | 권장 |
|------|------|--------|------|
| 0개 | Niladic | 가장 이해하기 쉬움 | ✅ 권장 |
| 1개 | Monadic | 이해하기 쉬움 | ✅ 권장 |
| 2개 | Dyadic | 약간 복잡 | 🤔 허용 |
| 3개 | Triadic | 복잡 | ⚠️ 특별한 경우만 |
| 3개+ | Polyadic | 매우 복잡 | ❌ 피하기 |

### ❌ Bad Example

```java
// 인자가 너무 많음 (5개)
public Order createOrder(
    String userId,
    String productName,
    BigDecimal price,
    int quantity,
    String shippingAddress,
    String paymentMethod
) {
    // ...
}

// 사용처 - 인자 순서 헷갈림
Order order = createOrder(
    "user123",
    "Laptop",
    new BigDecimal("1000"),
    2,
    "123 Main St",
    "CARD"
);
```

**문제점:**
- 인자 순서를 기억하기 어려움
- `productName`과 `shippingAddress`를 혼동 가능
- 새로운 인자 추가 시 모든 호출부 수정 필요

### ✅ Good Example

```java
// 객체로 묶어서 전달 (1개 인자)
public Order createOrder(CreateOrderRequest request) {
    // ...
}

// Request DTO
public record CreateOrderRequest(
    String userId,
    List<OrderItem> items,
    ShippingAddress shippingAddress,
    String paymentMethod
) {}

// 사용처 - 명확함
Order order = createOrder(new CreateOrderRequest(
    "user123",
    List.of(new OrderItem("Laptop", new BigDecimal("1000"), 2)),
    new ShippingAddress("123 Main St"),
    "CARD"
));
```

### 🏗️ Portal Universe 적용 사례

```java
// services/shopping-service/.../order/service/OrderService.java

// ✅ Good - Request DTO로 인자 묶기
OrderResponse createOrder(String userId, CreateOrderRequest request);

// ❌ Bad - 인자가 많음
// OrderResponse createOrder(String userId, String address, String paymentMethod, Long couponId);
```

### 플래그 인자는 피하라

```java
// ❌ Bad - 플래그 인자 (함수가 여러 일을 한다는 신호)
public void processOrder(Order order, boolean isUrgent) {
    if (isUrgent) {
        // 긴급 주문 처리
    } else {
        // 일반 주문 처리
    }
}

// ✅ Good - 함수 분리
public void processUrgentOrder(Order order) {
    // 긴급 주문 처리
}

public void processNormalOrder(Order order) {
    // 일반 주문 처리
}
```

---

## 4️⃣ 부수 효과를 일으키지 마라

### 원칙

> **"함수 이름에서 예상할 수 없는 일을 하지 마라."**

부수 효과(Side Effect):
- 함수 이름이 약속한 것 이외의 일
- 전역 변수 수정
- 인자로 받은 객체 수정
- 예상치 못한 상태 변경

### ❌ Bad Example

```java
// 부수 효과가 있는 함수
public boolean checkPassword(String username, String password) {
    User user = userRepository.findByUsername(username);

    if (user != null && user.getPassword().equals(password)) {
        // 부수 효과 1: 세션 초기화 (함수명에서 예상 불가)
        Session.initialize(user);

        // 부수 효과 2: 로그인 날짜 업데이트 (함수명에서 예상 불가)
        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);

        return true;
    }

    return false;
}
```

**문제점:**
- `checkPassword`는 검사만 할 것으로 예상
- 실제로는 세션 초기화, DB 업데이트 수행
- 함수명이 거짓말을 하고 있음

### ✅ Good Example

```java
// 부수 효과 없는 함수
public boolean checkPassword(String username, String password) {
    User user = userRepository.findByUsername(username);
    return user != null && user.getPassword().equals(password);
}

// 로그인 처리는 별도 함수
public void login(String username, String password) {
    if (checkPassword(username, password)) {
        User user = userRepository.findByUsername(username);
        Session.initialize(user);
        updateLastLoginDate(user);
    }
}

private void updateLastLoginDate(User user) {
    user.setLastLoginDate(LocalDateTime.now());
    userRepository.save(user);
}
```

### 🏗️ Portal Universe 적용 사례

```java
// services/shopping-service/.../product/service/ProductServiceImpl.java

// ✅ Good - 부수 효과 없는 조회 함수
@Override
public ProductResponse getProductById(Long id) {
    // 조회만 수행, 상태 변경 없음
    Product product = productRepository.findById(id)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND));

    return convertToResponse(product);
}

// ✅ Good - 명확히 상태를 변경하는 함수
@Override
@Transactional
public ProductResponse updateProduct(Long productId, ProductUpdateRequest request) {
    // 함수명에서 상태 변경을 예상 가능
    Product product = productRepository.findById(productId)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND));

    product.update(request.name(), request.description(), request.price(), request.stock());

    return convertToResponse(productRepository.save(product));
}
```

---

## 5️⃣ 명령과 조회를 분리하라 (CQS)

### 원칙

> **"함수는 무언가를 수행하거나(Command), 무언가를 답하거나(Query), 둘 중 하나만 해야 한다."**

### ❌ Bad Example

```java
// 명령과 조회를 섞음 (CQS 위반)
public boolean setActive(String username, boolean active) {
    User user = userRepository.findByUsername(username);

    if (user == null) {
        return false;  // 조회 결과 반환
    }

    user.setActive(active);  // 상태 변경
    userRepository.save(user);

    return true;  // 성공 여부 반환
}

// 사용처 - 혼란스러움
if (setActive("user123", true)) {
    // true는 설정 성공? 아니면 원래 active 상태?
}
```

**문제점:**
- `setActive`가 설정과 조회를 동시에 수행
- 반환값이 무엇을 의미하는지 불명확

### ✅ Good Example

```java
// 명령과 조회 분리 (CQS 준수)

// Command (명령) - void 반환
public void setActive(String username, boolean active) {
    User user = userRepository.findByUsername(username);

    if (user == null) {
        throw new UserNotFoundException(username);
    }

    user.setActive(active);
    userRepository.save(user);
}

// Query (조회) - 상태 변경 없음
public boolean isActive(String username) {
    User user = userRepository.findByUsername(username);
    return user != null && user.isActive();
}

// 사용처 - 명확함
setActive("user123", true);  // 설정만
if (isActive("user123")) {  // 조회만
    // ...
}
```

### 🏗️ Portal Universe 적용 사례

```java
// services/shopping-service/.../product/service/ProductService.java

// Query - 조회만, 상태 변경 없음
ProductResponse getProductById(Long id);
Page<ProductResponse> getAllProducts(Pageable pageable);

// Command - 상태 변경, void 반환 또는 생성된 객체 반환
ProductResponse createProduct(ProductCreateRequest request);
ProductResponse updateProduct(Long productId, ProductUpdateRequest request);
void deleteProduct(Long productId);
```

---

## 6️⃣ 오류 코드보다 예외를 사용하라

### 원칙

- 오류 코드를 반환하면 호출자는 즉시 처리해야 함
- 예외를 던지면 호출 코드가 깔끔해짐

### ❌ Bad Example

```java
// 오류 코드 반환
public int deleteProduct(Long productId) {
    if (!productRepository.existsById(productId)) {
        return -1;  // NOT_FOUND
    }

    if (hasActiveOrders(productId)) {
        return -2;  // HAS_ORDERS
    }

    productRepository.deleteById(productId);
    return 0;  // SUCCESS
}

// 사용처 - 복잡함
int result = deleteProduct(productId);
if (result == -1) {
    System.out.println("Product not found");
} else if (result == -2) {
    System.out.println("Cannot delete, has active orders");
} else {
    System.out.println("Deleted successfully");
}
```

**문제점:**
- 오류 코드 의미를 기억해야 함
- 호출부가 복잡해짐
- 오류 처리를 강제할 수 없음

### ✅ Good Example

```java
// 예외 던지기
public void deleteProduct(Long productId) {
    if (!productRepository.existsById(productId)) {
        throw new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND);
    }

    if (hasActiveOrders(productId)) {
        throw new CustomBusinessException(ShoppingErrorCode.CANNOT_DELETE_PRODUCT_WITH_ORDERS);
    }

    productRepository.deleteById(productId);
}

// 사용처 - 깔끔함
try {
    deleteProduct(productId);
    System.out.println("Deleted successfully");
} catch (CustomBusinessException e) {
    System.out.println("Error: " + e.getErrorCode().getMessage());
}
```

### 🏗️ Portal Universe 적용 사례

```java
// services/shopping-service/.../product/service/ProductServiceImpl.java

@Override
public void deleteProduct(Long productId) {
    // 예외 던지기 (오류 코드 반환 안 함)
    if (!productRepository.existsById(productId)) {
        throw new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND);
    }

    productRepository.deleteById(productId);
}

// GlobalExceptionHandler가 중앙에서 처리
```

---

## ✅ 함수 설계 체크리스트

### 크기
- [ ] 함수가 20줄 이하인가?
- [ ] 들여쓰기 레벨이 1~2단계인가?
- [ ] 한 화면에 다 보이는가?

### 단일 책임
- [ ] 함수가 한 가지 일만 하는가?
- [ ] 함수명이 하는 일을 정확히 표현하는가?
- [ ] 여러 추상화 레벨이 섞여 있지 않은가?

### 인자
- [ ] 인자 개수가 3개 이하인가?
- [ ] 플래그 인자를 사용하지 않는가?
- [ ] 많은 인자를 객체로 묶었는가?

### 부수 효과
- [ ] 함수명에서 예상 가능한 일만 하는가?
- [ ] 숨겨진 상태 변경이 없는가?
- [ ] 입력 인자를 수정하지 않는가?

### CQS (Command Query Separation)
- [ ] 명령(상태 변경)과 조회를 분리했는가?
- [ ] 조회 함수는 부수 효과가 없는가?

### 오류 처리
- [ ] 오류 코드 대신 예외를 사용하는가?
- [ ] Try-Catch 블록을 별도 함수로 분리했는가?

---

## 📚 관련 문서

- [SOLID 원칙](./solid-principles.md)
- [Clean Code - 의미 있는 이름 짓기](./clean-code-naming.md)
- [DRY, KISS, YAGNI 원칙](./dry-kiss-yagni.md)
- [에러 처리 패턴](./error-handling-patterns.md)

---

## 📖 추가 학습 자료

| 자료 | 난이도 | 설명 |
|------|--------|------|
| [Clean Code Chapter 3](https://www.amazon.com/Clean-Code-Handbook-Software-Craftsmanship/dp/0132350882) | ⭐⭐⭐ | 함수 설계 원칙 |
| [Refactoring](https://refactoring.com/) | ⭐⭐⭐ | 함수 리팩토링 기법 |
| [Effective Java Item 49-56](https://www.amazon.com/Effective-Java-Joshua-Bloch/dp/0134685997) | ⭐⭐⭐⭐ | 메서드 설계 |

---

**마지막 업데이트:** 2026-01-22
