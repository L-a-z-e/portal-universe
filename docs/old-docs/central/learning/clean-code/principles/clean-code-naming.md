---
id: learning-clean-code-naming
title: Clean Code - 의미 있는 이름 짓기
type: learning
status: current
created: 2026-01-22
updated: 2026-01-22
author: Laze
tags: [clean-code, naming, readability, convention]
difficulty: beginner
estimated_time: 45분
---

# Clean Code - 의미 있는 이름 짓기

## 📋 학습 목표

- 의미 있는 이름의 중요성 이해
- 변수, 함수, 클래스 이름 짓기의 원칙 습득
- 잘못된 이름과 좋은 이름의 차이점 파악
- Portal Universe 프로젝트의 네이밍 컨벤션 학습

## 🎯 사전 지식

- Java 기본 문법
- 변수, 메서드, 클래스 개념

## 📚 네이밍의 중요성

> **"좋은 이름은 코드를 읽는 사람에게 의도를 명확히 전달한다."**

코드는 작성하는 시간보다 읽는 시간이 10배 이상 많습니다. 따라서 **읽기 쉬운 이름**이 생산성을 크게 향상시킵니다.

---

## 1️⃣ 의도를 분명히 밝혀라

### 원칙

변수, 함수, 클래스의 **존재 이유**, **수행 기능**, **사용 방법**이 이름만으로 드러나야 합니다.

### ❌ Bad Example

```java
// 의도가 불명확한 이름
int d;  // 무엇을 의미하는가?
List<int[]> list1;  // 무엇을 담고 있는가?
int[] x;  // x가 무엇인가?

public List<int[]> getThem() {
    List<int[]> list1 = new ArrayList<>();
    for (int[] x : theList) {
        if (x[0] == 4) {
            list1.add(x);
        }
    }
    return list1;
}
```

**문제점:**
- `d`, `list1`, `x`가 무엇인지 알 수 없음
- 주석이 필요함
- 코드 의도 파악에 시간 소요

### ✅ Good Example

```java
// 의도가 명확한 이름
int elapsedTimeInDays;
List<Product> availableProducts;
Order currentOrder;

public List<Product> getAvailableProducts() {
    List<Product> availableProducts = new ArrayList<>();
    for (Product product : allProducts) {
        if (product.isAvailable()) {
            availableProducts.add(product);
        }
    }
    return availableProducts;
}
```

**개선점:**
- 변수명만으로 의도 파악 가능
- 주석 불필요
- 가독성 향상

### 🏗️ Portal Universe 적용 사례

```java
// services/shopping-service/.../order/service/OrderServiceImpl.java

// ❌ Bad
Cart c = cr.find(u, s).orElseThrow(() -> new CBE(SEC.CNF));

// ✅ Good - 실제 코드
Cart cart = cartRepository.findByUserIdAndStatusWithItems(userId, CartStatus.CHECKED_OUT)
        .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.CART_NOT_FOUND));
```

---

## 2️⃣ 그릇된 정보를 피하라

### 원칙

- 널리 쓰이는 의미와 다른 단어를 사용하지 마라
- 약어보다는 전체 단어를 사용하라
- 유사한 개념은 유사한 표기법을 사용하라

### ❌ Bad Example

```java
// 그릇된 정보를 제공하는 이름
String accountList;  // List 타입이 아닌데 List라고 명명 (혼란)
int hp;  // hypotenuse? horse power? health point?
int XYZControllerForEfficientHandlingOfStrings;  // 너무 김

// 비슷하지만 다른 네이밍
class ProductManager { }
class ProductHandler { }
class ProductProcessor { }
// Manager, Handler, Processor의 차이는?
```

**문제점:**
- `accountList`는 실제로 `String` 타입인데 `List`로 오해
- 약어는 의미 불명확
- 일관성 없는 네이밍은 혼란 야기

### ✅ Good Example

```java
// 명확하고 정확한 이름
String accountGroup;  // 또는 accountCollection
int hypotenuse;
int horsepower;
int healthPoint;

// 일관된 네이밍
class ProductService { }  // 비즈니스 로직
class ProductRepository { }  // 데이터 접근
class ProductController { }  // HTTP 요청 처리
```

### 🏗️ Portal Universe 적용 사례

**1. 일관된 Layer 네이밍**

```java
// Controller Layer
ProductController
OrderController
CartController

// Service Layer
ProductService / ProductServiceImpl
OrderService / OrderServiceImpl
CartService / CartServiceImpl

// Repository Layer
ProductRepository
OrderRepository
CartRepository
```

**일관성 포인트:**
- Controller는 `~Controller`
- Service 인터페이스는 `~Service`, 구현체는 `~ServiceImpl`
- Repository는 `~Repository`

**2. DTO 네이밍 규칙**

```java
// Request DTO
ProductCreateRequest
ProductUpdateRequest
CreateOrderRequest

// Response DTO
ProductResponse
OrderResponse
CartResponse
```

---

## 3️⃣ 의미 있게 구분하라

### 원칙

- 숫자나 불용어(noise word)를 추가하지 마라
- 읽는 사람이 차이를 알 수 있도록 이름을 지어라

### ❌ Bad Example

```java
// 의미 없는 구분
public void copyChars(char[] a1, char[] a2) {
    for (int i = 0; i < a1.length; i++) {
        a2[i] = a1[i];
    }
}

// 불용어 사용
class ProductInfo { }
class ProductData { }
class Product { }
// Info, Data의 차이는?

String nameString;  // name이면 충분
int priceVariable;  // price면 충분
```

**문제점:**
- `a1`, `a2`는 의미 전달 못함
- `Info`, `Data`는 의미 없는 불용어
- `String`, `Variable`은 타입 정보 중복

### ✅ Good Example

```java
// 의미 있는 구분
public void copyChars(char[] source, char[] destination) {
    for (int i = 0; i < source.length; i++) {
        destination[i] = source[i];
    }
}

// 명확한 역할 구분
class Product { }  // 엔티티
class ProductRequest { }  // 요청 DTO
class ProductResponse { }  // 응답 DTO

String name;
BigDecimal price;
```

### 🏗️ Portal Universe 적용 사례

```java
// services/shopping-service/.../product/service/ProductServiceImpl.java

// 명확한 구분
private final ProductRepository productRepository;  // 데이터 접근
private final BlogServiceClient blogServiceClient;  // 외부 서비스 호출

// Entity vs DTO 명확한 구분
Product product = productRepository.findById(id).orElseThrow(...);  // Entity
ProductResponse response = convertToResponse(product);  // Response DTO
```

---

## 4️⃣ 발음하기 쉬운 이름을 사용하라

### 원칙

- 프로그래밍은 사회 활동이다
- 발음하기 어려운 이름은 토론하기 어렵다

### ❌ Bad Example

```java
// 발음하기 어려운 이름
class DtaRcrd102 {
    private Date genymdhms;  // generation year month day hour minute second
    private Date modymdhms;  // modification year month day hour minute second
    private final String pszqint = "102";
}
```

**문제점:**
- 팀원과 "gen-y-m-d-h-m-s"를 어떻게 발음?
- 코드 리뷰 시 대화 불가능

### ✅ Good Example

```java
// 발음하기 쉬운 이름
class Customer {
    private LocalDateTime generationTimestamp;
    private LocalDateTime modificationTimestamp;
    private final String recordId = "102";
}
```

**개선점:**
- "generation timestamp"로 자연스럽게 발음
- 팀원과 대화하기 쉬움

### 🏗️ Portal Universe 적용 사례

```java
// services/shopping-service/.../order/domain/Order.java

// 발음하기 쉬운 필드명
private String userId;
private OrderStatus status;
private LocalDateTime orderedAt;
private LocalDateTime confirmedAt;
private LocalDateTime cancelledAt;

// ❌ Bad: usrId, ordAt, cnfAt, cnlAt
```

---

## 5️⃣ 검색하기 쉬운 이름을 사용하라

### 원칙

- 상수는 의미 있는 이름으로 선언하라
- 한 글자 이름은 검색이 어렵다

### ❌ Bad Example

```java
// 매직 넘버와 한 글자 변수
for (int j = 0; j < 34; j++) {
    s += (t[j] * 4) / 5;
}

if (product.getStock() < 10) {  // 10은 무엇?
    // ...
}
```

**문제점:**
- `34`, `4`, `5`, `10`이 무엇을 의미하는지 알 수 없음
- IDE에서 검색 불가능
- 변경 시 모든 곳을 찾아야 함

### ✅ Good Example

```java
// 의미 있는 상수
private static final int WORK_DAYS_PER_WEEK = 5;
private static final int NUMBER_OF_TASKS = 34;
private static final int MINIMUM_STOCK_THRESHOLD = 10;

for (int taskIndex = 0; taskIndex < NUMBER_OF_TASKS; taskIndex++) {
    realDaysPerIdealDay += (taskEstimate[taskIndex] * 4) / WORK_DAYS_PER_WEEK;
}

if (product.getStock() < MINIMUM_STOCK_THRESHOLD) {
    // 재고 부족 알림
}
```

**개선점:**
- 상수의 의미 명확
- IDE에서 `MINIMUM_STOCK_THRESHOLD` 검색 가능
- 값 변경 시 한 곳만 수정

### 🏗️ Portal Universe 적용 사례

```java
// services/shopping-service/.../exception/ShoppingErrorCode.java

// 에러 코드를 Enum으로 관리 (검색 가능)
@Getter
public enum ShoppingErrorCode implements ErrorCode {
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "Product not found"),
    CART_NOT_FOUND(HttpStatus.NOT_FOUND, "S101", "Cart not found"),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "S201", "Order not found"),
    // ...
}

// 사용처 - Enum 상수로 검색 가능
throw new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND);
```

---

## 6️⃣ 클래스 이름과 메서드 이름

### 원칙

- **클래스 이름**: 명사 또는 명사구
- **메서드 이름**: 동사 또는 동사구
- 접근자(getter), 변경자(setter), 조건자(is)는 관례를 따라라

### ✅ Good Example

```java
// 클래스 이름 - 명사 또는 명사구
class Product { }
class OrderService { }
class PaymentProcessor { }
class UserRepository { }

// 메서드 이름 - 동사 또는 동사구
public void createOrder() { }
public Product getProduct(Long id) { }
public boolean isAvailable() { }
public void setPrice(BigDecimal price) { }
```

### 🏗️ Portal Universe 적용 사례

**1. Service 메서드 네이밍**

```java
// services/shopping-service/.../product/service/ProductService.java

// 조회 - get/find로 시작
ProductResponse getProductById(Long id);
Page<ProductResponse> getAllProducts(Pageable pageable);

// 생성 - create로 시작
ProductResponse createProduct(ProductCreateRequest request);

// 수정 - update로 시작
ProductResponse updateProduct(Long productId, ProductUpdateRequest request);

// 삭제 - delete로 시작
void deleteProduct(Long productId);

// 검증 - validate로 시작
void validateProduct(Long productId);

// 조건 확인 - is/has로 시작
boolean isAvailable(Long productId);
```

**2. Repository 메서드 네이밍**

```java
// Spring Data JPA 메서드 네이밍 규칙
Optional<Product> findById(Long id);
List<Product> findByName(String name);
boolean existsByName(String name);
void deleteById(Long id);
```

---

## 7️⃣ 한 개념에 한 단어를 사용하라

### 원칙

- 추상적인 개념 하나에 단어 하나를 선택하여 일관되게 사용하라
- `fetch`, `retrieve`, `get`을 혼용하지 마라

### ❌ Bad Example

```java
// 같은 개념에 다른 단어 사용
class ProductController {
    public ProductResponse fetchProduct(Long id) { }
}

class OrderController {
    public OrderResponse retrieveOrder(Long id) { }
}

class CartController {
    public CartResponse getCart(String userId) { }
}
// fetch? retrieve? get?
```

**문제점:**
- 개발자가 어떤 단어를 써야 할지 혼란
- 일관성 없음

### ✅ Good Example

```java
// 일관된 단어 사용
class ProductController {
    public ProductResponse getProduct(Long id) { }
}

class OrderController {
    public OrderResponse getOrder(Long id) { }
}

class CartController {
    public CartResponse getCart(String userId) { }
}
```

### 🏗️ Portal Universe 적용 사례

**일관된 CRUD 동사 사용**

```java
// 모든 Service에서 일관된 동사 사용

// 생성: create
productService.createProduct(request);
orderService.createOrder(userId, request);
cartService.createCart(userId);

// 조회: get
productService.getProductById(id);
orderService.getOrderById(id);
cartService.getCart(userId);

// 수정: update
productService.updateProduct(id, request);
orderService.updateOrder(id, request);

// 삭제: delete
productService.deleteProduct(id);
orderService.deleteOrder(id);
```

---

## 8️⃣ Portal Universe 네이밍 컨벤션

### Layer별 네이밍 규칙

| Layer | Pattern | Example |
|-------|---------|---------|
| Controller | `{Domain}Controller` | `ProductController` |
| Service | `{Domain}Service` / `{Domain}ServiceImpl` | `ProductService` / `ProductServiceImpl` |
| Repository | `{Domain}Repository` | `ProductRepository` |
| Entity | `{Domain}` | `Product`, `Order` |
| DTO | `{Domain}{Type}` | `ProductResponse`, `CreateOrderRequest` |
| Exception | `{Domain}ErrorCode` | `ShoppingErrorCode` |

### 메서드 네이밍 규칙

| 목적 | 접두사 | Example |
|------|--------|---------|
| 조회 (단건) | `get` | `getProduct(Long id)` |
| 조회 (목록) | `getAll` | `getAllProducts(Pageable pageable)` |
| 생성 | `create` | `createProduct(ProductRequest request)` |
| 수정 | `update` | `updateProduct(Long id, ProductRequest request)` |
| 삭제 | `delete` | `deleteProduct(Long id)` |
| 존재 확인 | `exists` | `existsById(Long id)` |
| 불린 반환 | `is`, `has` | `isAvailable()`, `hasStock()` |
| 검증 | `validate` | `validateCouponForOrder()` |

### 변수 네이밍 규칙

| 타입 | 규칙 | Example |
|------|------|---------|
| 일반 변수 | `camelCase` | `userId`, `orderStatus` |
| 상수 | `UPPER_SNAKE_CASE` | `MAX_RETRY_COUNT`, `DEFAULT_PAGE_SIZE` |
| 불린 변수 | `is` + 형용사 | `isAvailable`, `hasStock` |
| 컬렉션 | 복수형 | `products`, `orders`, `items` |

---

## ✅ 네이밍 체크리스트

- [ ] 변수명만으로 의도를 파악할 수 있는가?
- [ ] 주석 없이도 이해 가능한가?
- [ ] 발음하기 쉬운가?
- [ ] IDE에서 검색 가능한가?
- [ ] 프로젝트의 네이밍 컨벤션을 따르는가?
- [ ] 한 글자 변수(i, j 제외)를 사용하지 않았는가?
- [ ] 매직 넘버 대신 상수를 사용했는가?
- [ ] 클래스는 명사, 메서드는 동사인가?
- [ ] 일관된 단어를 사용했는가?

---

## 🎯 네이밍 개선 연습

### Before (Bad)

```java
public List<int[]> getThem() {
    List<int[]> list1 = new ArrayList<>();
    for (int[] x : theList) {
        if (x[0] == 4) {
            list1.add(x);
        }
    }
    return list1;
}
```

### After (Good)

```java
public List<Product> getAvailableProducts() {
    List<Product> availableProducts = new ArrayList<>();
    for (Product product : allProducts) {
        if (product.getStatus() == ProductStatus.AVAILABLE) {
            availableProducts.add(product);
        }
    }
    return availableProducts;
}
```

---

## 📚 관련 문서

- [SOLID 원칙](./solid-principles.md)
- [Clean Code - 함수 설계 원칙](./clean-code-functions.md)
- [DRY, KISS, YAGNI 원칙](./dry-kiss-yagni.md)
- [Portal Universe 코딩 컨벤션](../../../../.claude/rules/common.md)

---

## 📖 추가 학습 자료

| 자료 | 난이도 | 설명 |
|------|--------|------|
| [Clean Code Chapter 2](https://www.amazon.com/Clean-Code-Handbook-Software-Craftsmanship/dp/0132350882) | ⭐⭐⭐ | 의미 있는 이름 짓기 |
| [Refactoring Guru - Naming](https://refactoring.guru/) | ⭐⭐ | 네이밍 리팩토링 기법 |
| [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) | ⭐⭐ | 네이밍 컨벤션 참고 |

---

**마지막 업데이트:** 2026-01-22
