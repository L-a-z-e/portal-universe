---
id: learning-clean-code-comments
title: Clean Code - 주석 작성 가이드
type: learning
status: current
created: 2026-01-22
updated: 2026-01-22
author: Laze
tags: [clean-code, comments, documentation, javadoc]
difficulty: beginner
estimated_time: 35분
---

# Clean Code - 주석 작성 가이드

## 📋 학습 목표

- 좋은 주석과 나쁜 주석 구분
- 주석 없이도 이해 가능한 코드 작성법 습득
- JavaDoc 작성 원칙 이해
- Portal Universe 프로젝트의 주석 컨벤션 학습

## 🎯 사전 지식

- Java 기본 문법
- 코드 가독성의 중요성
- JavaDoc 기본 개념

## 📚 주석에 대한 오해

> **"주석은 필요악이다. 코드로 의도를 표현하지 못했기 때문에 주석을 사용한다."**
> **"좋은 코드 > 주석이 달린 나쁜 코드"**

### 주석이 필요한 이유

- 코드만으로 표현할 수 없는 의도나 맥락
- 법적 정보, 경고, TODO
- Public API 문서화

### 주석의 문제점

- 코드는 변하지만 주석은 항상 코드를 따라가지 못함
- 거짓말을 하는 주석이 만들어짐
- 주석 유지보수 비용 발생

---

## 1️⃣ 코드로 의도를 표현하라

### 원칙

> **"주석으로 달려는 설명을 함수로 만들어 표현하라."**

### ❌ Bad Example

```java
// 직원에게 복지 혜택을 받을 자격이 있는지 검사
if ((employee.flags & HOURLY_FLAG) && (employee.age > 65)) {
    // ...
}
```

### ✅ Good Example

```java
// 함수명으로 의도 표현 (주석 불필요)
if (employee.isEligibleForFullBenefits()) {
    // ...
}
```

### 🏗️ Portal Universe 적용 사례

```java
// services/shopping-service/.../coupon/service/CouponServiceImpl.java

// ❌ Bad - 주석으로 설명
// 쿠폰이 사용 가능한지 검사: 미사용, 미만료, 활성화 상태
if (!userCoupon.isUsed() && !userCoupon.isExpired() && coupon.isActive()) {
    // ...
}

// ✅ Good - 메서드명으로 의도 표현 (실제 코드)
public void validateCouponForOrder(Long userCouponId, String userId, BigDecimal orderAmount) {
    UserCoupon userCoupon = getUserCoupon(userCouponId, userId);
    Coupon coupon = userCoupon.getCoupon();

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
```

---

## 2️⃣ 좋은 주석

### 법적 정보

```java
/**
 * Copyright (C) 2026 Portal Universe. All rights reserved.
 * This code is proprietary and confidential.
 */
```

### 정보를 제공하는 주석

```java
// 테스트 중인 Responder 인스턴스를 반환
protected abstract Responder responderInstance();

// kk:mm:ss EEE, MMM dd, yyyy 형식
Pattern timeMatcher = Pattern.compile("\\d*:\\d*:\\d* \\w*, \\w* \\d*, \\d*");
```

### 의도를 설명하는 주석

```java
// 스레드를 많이 생성하여 시스템이 멈추는 것을 막기 위해 인스턴스를 제한함
public static final int MAX_THREAD_COUNT = 100;

// 더 나은 방법이 있을 수 있지만, 이 방법이 가장 단순하다
public int compareTo(Object o) {
    // ...
}
```

### 결과를 경고하는 주석

```java
// 여유 시간이 충분하지 않으면 이 테스트를 실행하지 마라 (10분 소요)
@Test
@Disabled("Takes too long to run")
public void testWithReallyBigFile() {
    // ...
}

// SimpleDateFormat은 스레드에 안전하지 않으므로
// 각 인스턴스를 독립적으로 생성해야 한다
SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
```

### TODO 주석

```java
// TODO: 현재는 단일 쿠폰만 지원하지만, 향후 여러 쿠폰 동시 사용 기능 추가 예정
public void applyCoupon(Long userCouponId, BigDecimal discountAmount) {
    this.userCouponId = userCouponId;
    this.discountAmount = discountAmount;
}

// TODO-PERF: 배치 처리로 최적화 필요 (현재 N+1 쿼리 발생)
for (OrderItem item : order.getItems()) {
    Product product = productRepository.findById(item.getProductId()).orElseThrow();
    // ...
}
```

### Public API 문서화 (JavaDoc)

```java
/**
 * 상품을 생성합니다.
 *
 * @param request 생성할 상품 정보
 * @return 생성된 상품 응답 DTO
 * @throws CustomBusinessException 상품명이 중복되는 경우
 */
@Transactional
public ProductResponse createProduct(ProductCreateRequest request) {
    // ...
}
```

### 🏗️ Portal Universe 적용 사례

```java
// services/shopping-service/.../order/service/OrderServiceImpl.java

/**
 * 주문 관리 서비스 구현체입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final OrderSagaOrchestrator orderSagaOrchestrator;

    /**
     * 새로운 주문을 생성합니다.
     *
     * <p>장바구니에서 체크아웃된 상품들을 기반으로 주문을 생성하며,
     * 선택적으로 쿠폰을 적용할 수 있습니다. 주문 생성 후 Saga 패턴을 통해
     * 재고 예약 및 결제 처리가 진행됩니다.</p>
     *
     * @param userId 사용자 ID
     * @param request 주문 생성 요청 DTO
     * @return 생성된 주문 응답 DTO
     * @throws CustomBusinessException 장바구니가 없거나 비어있는 경우
     */
    @Override
    @Transactional
    public OrderResponse createOrder(String userId, CreateOrderRequest request) {
        // 1. 체크아웃된 장바구니 조회
        Cart cart = cartRepository.findByUserIdAndStatusWithItems(userId, CartStatus.CHECKED_OUT)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.CART_NOT_FOUND));

        // ...
    }
}
```

---

## 3️⃣ 나쁜 주석

### 주절거리는 주석

```java
// ❌ Bad
public void loadProperties() {
    try {
        String propertiesPath = propertiesLocation + "/" + PROPERTIES_FILE;
        FileInputStream propertiesStream = new FileInputStream(propertiesPath);
        loadedProperties.load(propertiesStream);
    } catch (IOException e) {
        // 속성 파일이 없다면 기본값을 모두 메모리로 읽어 들였다는 의미다
        // 무슨 뜻인지 이해 불가
    }
}
```

### 같은 이야기를 중복하는 주석

```java
// ❌ Bad - 코드와 주석이 동일한 정보
// this.closed가 true일 때 반환되는 유틸리티 메서드다.
// 타임아웃에 도달하면 예외를 던진다.
public synchronized void waitForClose(final long timeoutMillis) throws Exception {
    if (!closed) {
        wait(timeoutMillis);
        if (!closed) {
            throw new Exception("MockResponseSender could not be closed");
        }
    }
}
```

### 오해할 여지가 있는 주석

```java
// ❌ Bad - 주석이 정확하지 않음
// this.closed가 true로 변하는 순간 메서드는 반환된다
// 실제로는 this.closed가 true가 아니면 무조건 타임아웃을 기다린다!
public synchronized void waitForClose(final long timeoutMillis) throws Exception {
    if (!closed) {
        wait(timeoutMillis);
        if (!closed) {
            throw new Exception("MockResponseSender could not be closed");
        }
    }
}
```

### 의무적으로 다는 주석

```java
// ❌ Bad - 모든 함수에 JavaDoc을 다는 규칙
/**
 * @param title CD 제목
 * @param author CD 저자
 * @param tracks CD 트랙 수
 * @param durationInMinutes CD 길이(분)
 */
public void addCD(String title, String author, int tracks, int durationInMinutes) {
    CD cd = new CD();
    cd.title = title;
    cd.author = author;
    cd.tracks = tracks;
    cd.duration = durationInMinutes;
    cdList.add(cd);
}
// 오히려 코드만 복잡하고 정보 제공 없음
```

### 이력을 기록하는 주석

```java
// ❌ Bad - 변경 이력을 주석으로 (Git이 있으니 불필요)
/**
 * 변경 이력 (2024-01-01부터)
 * ---------------------
 * 2024-01-11: 재고 검증 로직 추가
 * 2024-02-15: 쿠폰 적용 기능 추가
 * 2024-03-20: 결제 모듈 연동
 */
```

### 있으나 마나 한 주석

```java
// ❌ Bad - 불필요한 주석
/**
 * 기본 생성자
 */
public Product() {
}

/**
 * 상품 ID를 반환한다
 * @return 상품 ID
 */
public Long getId() {
    return id;
}

// 월 중 일자를 반환한다
int dayOfMonth = date.getDayOfMonth();
```

### 무서운 잡음

```java
// ❌ Bad - 복사-붙여넣기로 발생한 오류
/** The name. */
private String name;

/** The version. */
private String version;

/** The licenseName. */
private String licenseName;

/** The version. */  // 중복! 실제로는 info인데 주석은 version
private String info;
```

### 함수나 변수로 표현할 수 있다면 주석을 달지 마라

```java
// ❌ Bad - 주석으로 설명
// 전역 목록 <smodule>에 속하는 모듈이 우리가 속한 하위 시스템에 의존하는가?
if (smodule.getDependSubsystems().contains(subSysMod.getSubSystem())) {
    // ...
}

// ✅ Good - 함수로 표현
ArrayList<String> moduleDependees = smodule.getDependSubsystems();
String ourSubSystem = subSysMod.getSubSystem();
if (moduleDependees.contains(ourSubSystem)) {
    // ...
}
```

### 닫는 괄호에 다는 주석

```java
// ❌ Bad - 함수가 너무 길다는 신호
public static void main(String[] args) {
    try {
        while (true) {
            // ...
        } // while
    } catch (Exception e) {
        // ...
    } // catch
} // main
```

### 주석으로 처리한 코드

```java
// ❌ Bad - 주석 처리된 코드는 삭제하라 (Git이 기억함)
public void processOrder(Order order) {
    // validateOrder(order);  // 2024-01-15: 검증 로직 제거
    // notifyWarehouse(order);  // 2024-02-20: 알림 기능 제거

    saveOrder(order);

    // if (order.isPriority()) {  // 우선 주문 기능은 향후 구현
    //     processPriorityOrder(order);
    // }
}

// ✅ Good - 불필요한 코드는 삭제
public void processOrder(Order order) {
    saveOrder(order);
}
```

---

## 4️⃣ JavaDoc 작성 가이드

### 작성 대상

| 대상 | 필요성 | 이유 |
|------|--------|------|
| Public API | ✅ 필수 | 외부에서 사용 |
| Interface | ✅ 필수 | 구현체가 따라야 할 규약 |
| Public Class | ✅ 권장 | 클래스 목적 설명 |
| Public Method | ✅ 권장 | 사용법 명시 |
| Private Method | ❌ 불필요 | 코드로 설명 |
| Getter/Setter | ❌ 불필요 | 자명함 |

### 좋은 JavaDoc 예시

```java
/**
 * 상품 관리 서비스입니다.
 *
 * <p>상품의 생성, 조회, 수정, 삭제(CRUD) 기능을 제공하며,
 * 재고 관리 및 검색 기능을 포함합니다.</p>
 *
 * <p>모든 상품 변경 작업은 트랜잭션 내에서 수행되며,
 * 실패 시 자동으로 롤백됩니다.</p>
 *
 * @see ProductRepository
 * @see Product
 * @since 1.0
 */
public interface ProductService {

    /**
     * 상품을 ID로 조회합니다.
     *
     * @param id 조회할 상품의 고유 식별자
     * @return 조회된 상품 정보
     * @throws CustomBusinessException 상품이 존재하지 않는 경우 (PRODUCT_NOT_FOUND)
     */
    ProductResponse getProductById(Long id);

    /**
     * 새로운 상품을 생성합니다.
     *
     * <p>중복된 상품명이 있는지 검증한 후 상품을 생성합니다.
     * 재고는 초기값으로 설정되며, 상태는 ACTIVE로 시작됩니다.</p>
     *
     * @param request 생성할 상품 정보를 담은 요청 객체
     * @return 생성된 상품 정보
     * @throws CustomBusinessException 상품명이 중복되는 경우 (PRODUCT_NAME_ALREADY_EXISTS)
     */
    ProductResponse createProduct(ProductCreateRequest request);
}
```

### 🏗️ Portal Universe 적용 사례

```java
// services/common-library/.../exception/CustomBusinessException.java

/**
 * 시스템 전반에서 사용될 커스텀 비즈니스 예외 클래스입니다.
 *
 * <p>서비스 로직에서 예측 가능한 예외 상황이 발생했을 때 사용됩니다.
 * 이 예외는 {@link ErrorCode}를 포함하여, 예외 발생 시 상태 코드, 에러 코드, 메시지를
 * 일관되게 처리할 수 있도록 합니다.</p>
 *
 * @see ErrorCode
 * @see GlobalExceptionHandler
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

---

## ✅ 주석 작성 체크리스트

### 주석을 쓰기 전에
- [ ] 코드로 표현할 수 있는가?
- [ ] 함수명/변수명을 개선하면 주석이 불필요한가?
- [ ] 정말 필요한 정보인가?

### 주석을 쓸 때
- [ ] 주석이 코드와 일치하는가?
- [ ] 오해의 여지가 없는가?
- [ ] 간결하고 명확한가?
- [ ] JavaDoc 형식이 올바른가? (Public API인 경우)

### 주석을 쓰지 말아야 할 때
- [ ] 코드가 자명한가?
- [ ] 주석이 코드와 같은 내용인가?
- [ ] 변경 이력인가? (Git이 기록함)
- [ ] 닫는 괄호 표시인가? (함수를 짧게 만들 것)
- [ ] 주석 처리된 코드인가? (삭제할 것)

---

## 🎯 주석 개선 연습

### Before (Bad)

```java
/**
 * 상품 정보를 가져온다
 * @param id 상품 ID
 * @return 상품
 */
public Product getProduct(Long id) {
    // DB에서 상품을 조회한다
    Optional<Product> product = productRepository.findById(id);

    // 상품이 없으면 예외를 던진다
    if (product.isEmpty()) {
        throw new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND);
    }

    // 상품을 반환한다
    return product.get();
}
```

### After (Good)

```java
/**
 * 상품을 ID로 조회합니다.
 *
 * @param id 조회할 상품의 고유 식별자
 * @return 조회된 상품 엔티티
 * @throws CustomBusinessException 상품이 존재하지 않는 경우
 */
public Product getProduct(Long id) {
    return productRepository.findById(id)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND));
}
```

---

## 📊 주석 vs 코드

| 상황 | 주석 | 코드 개선 |
|------|------|----------|
| 복잡한 알고리즘 | "왜"를 설명 | 함수로 분리 |
| 비즈니스 규칙 | ❌ | 명확한 메서드명 |
| 정규표현식 | 형식 설명 | 상수화 + 주석 |
| 외부 API | 사용법 설명 | Wrapper 클래스 |
| 임시 해결책 | TODO 주석 | 이슈 트래커 연동 |

---

## 📚 관련 문서

- [Clean Code - 의미 있는 이름 짓기](./clean-code-naming.md)
- [Clean Code - 함수 설계 원칙](./clean-code-functions.md)
- [SOLID 원칙](./solid-principles.md)

---

## 📖 추가 학습 자료

| 자료 | 난이도 | 설명 |
|------|--------|------|
| [Clean Code Chapter 4](https://www.amazon.com/Clean-Code-Handbook-Software-Craftsmanship/dp/0132350882) | ⭐⭐⭐ | 주석 작성 가이드 |
| [JavaDoc Guide](https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html) | ⭐⭐ | JavaDoc 작성법 |
| [Self-Documenting Code](https://martinfowler.com/bliki/CodeAsDocumentation.html) | ⭐⭐ | 코드로 문서화하기 |

---

**마지막 업데이트:** 2026-01-22
