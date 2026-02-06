# Domain Services

## 1. 개요

Domain Service는 특정 Entity에 속하지 않는 도메인 로직을 캡슐화합니다. 여러 Aggregate를 조율하거나 외부 시스템과의 연동이 필요한 비즈니스 로직을 처리합니다.

## 2. Inventory Service (재고 관리)

### 역할

- 재고 예약/차감/해제/추가
- 동시성 제어 (비관적 락)
- 재고 이동 이력 관리
- 실시간 재고 변동 알림 (Redis Pub/Sub)

### 인터페이스

```java
public interface InventoryService {

    // 재고 조회
    InventoryResponse getInventory(Long productId);

    // 재고 초기화
    InventoryResponse initializeInventory(Long productId, int initialStock, String userId);

    // 재고 예약 (주문 생성 시)
    InventoryResponse reserveStock(Long productId, int quantity,
                                    String referenceType, String referenceId, String userId);

    // 일괄 재고 예약 (데드락 방지)
    List<InventoryResponse> reserveStockBatch(Map<Long, Integer> quantities,
                                               String referenceType, String referenceId, String userId);

    // 재고 차감 (결제 완료 시)
    InventoryResponse deductStock(Long productId, int quantity,
                                   String referenceType, String referenceId, String userId);

    // 일괄 재고 차감
    List<InventoryResponse> deductStockBatch(Map<Long, Integer> quantities,
                                              String referenceType, String referenceId, String userId);

    // 재고 해제 (주문 취소/결제 실패 시)
    InventoryResponse releaseStock(Long productId, int quantity,
                                    String referenceType, String referenceId, String userId);

    // 일괄 재고 해제
    List<InventoryResponse> releaseStockBatch(Map<Long, Integer> quantities,
                                               String referenceType, String referenceId, String userId);

    // 재고 추가 (입고)
    InventoryResponse addStock(Long productId, int quantity, String reason, String userId);

    // 재고 이동 이력 조회
    Page<StockMovementResponse> getStockMovements(Long productId, Pageable pageable);
}
```

### 비관적 락 사용

```java
@Override
@Transactional
public InventoryResponse reserveStock(Long productId, int quantity,
                                       String referenceType, String referenceId, String userId) {
    // 비관적 락으로 재고 조회
    Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
        .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.INVENTORY_NOT_FOUND));

    int previousAvailable = inventory.getAvailableQuantity();
    int previousReserved = inventory.getReservedQuantity();

    // 재고 예약
    inventory.reserve(quantity);
    Inventory savedInventory = inventoryRepository.save(inventory);

    // 이력 기록
    recordMovement(savedInventory, MovementType.RESERVE, quantity,
        previousAvailable, savedInventory.getAvailableQuantity(),
        previousReserved, savedInventory.getReservedQuantity(),
        referenceType, referenceId, "Stock reserved for order", userId);

    // Redis Pub/Sub으로 변동 알림
    publishInventoryUpdate(savedInventory);

    return InventoryResponse.from(savedInventory);
}
```

### 데드락 방지 (일괄 처리)

```java
@Override
@Transactional
public List<InventoryResponse> reserveStockBatch(Map<Long, Integer> quantities,
                                                  String referenceType, String referenceId, String userId) {
    // 1. 상품 ID 정렬로 일관된 락 순서 보장
    Map<Long, Integer> sortedQuantities = new TreeMap<>(quantities);
    List<Long> productIds = new ArrayList<>(sortedQuantities.keySet());

    // 2. 정렬된 순서로 락 획득
    List<Inventory> inventories = inventoryRepository.findByProductIdsWithLock(productIds);

    // 3. 재고 예약 처리
    for (Inventory inventory : inventories) {
        int quantity = sortedQuantities.get(inventory.getProductId());
        inventory.reserve(quantity);
    }

    inventoryRepository.saveAll(inventories);
    return inventories.stream().map(InventoryResponse::from).toList();
}
```

## 3. Coupon Service (쿠폰 관리)

### 역할

- 쿠폰 생성/조회/비활성화
- 쿠폰 발급 (Redis Lua Script)
- 할인 금액 계산
- 쿠폰 사용 검증 및 처리

### 인터페이스

```java
public interface CouponService {

    // 쿠폰 관리
    CouponResponse createCoupon(CouponCreateRequest request);
    CouponResponse getCoupon(Long couponId);
    List<CouponResponse> getAvailableCoupons();
    void deactivateCoupon(Long couponId);

    // 쿠폰 발급
    UserCouponResponse issueCoupon(Long couponId, String userId);

    // 사용자 쿠폰 조회
    List<UserCouponResponse> getUserCoupons(String userId);
    List<UserCouponResponse> getAvailableUserCoupons(String userId);

    // 쿠폰 사용
    void useCoupon(Long userCouponId, Long orderId);

    // 할인 계산 및 검증
    BigDecimal calculateDiscount(Long userCouponId, BigDecimal orderAmount);
    void validateCouponForOrder(Long userCouponId, String userId, BigDecimal orderAmount);
}
```

### 할인 계산 로직

```java
@Override
public BigDecimal calculateDiscount(Long userCouponId, BigDecimal orderAmount) {
    UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
        .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.USER_COUPON_NOT_FOUND));

    return userCoupon.getCoupon().calculateDiscount(orderAmount);
}
```

위임된 계산 (Coupon Entity):

```java
public BigDecimal calculateDiscount(BigDecimal orderAmount) {
    // 최소 주문 금액 검증
    if (minimumOrderAmount != null &&
        orderAmount.compareTo(minimumOrderAmount) < 0) {
        return BigDecimal.ZERO;
    }

    // 할인 타입에 따른 계산
    BigDecimal discount;
    if (discountType == DiscountType.FIXED) {
        discount = discountValue;
    } else { // PERCENTAGE
        discount = orderAmount.multiply(discountValue)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    // 최대 할인 금액 제한
    if (maximumDiscountAmount != null &&
        discount.compareTo(maximumDiscountAmount) > 0) {
        discount = maximumDiscountAmount;
    }

    // 주문 금액 초과 방지
    if (discount.compareTo(orderAmount) > 0) {
        discount = orderAmount;
    }

    return discount;
}
```

### 쿠폰 사용 검증

```java
@Override
public void validateCouponForOrder(Long userCouponId, String userId, BigDecimal orderAmount) {
    UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
        .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.USER_COUPON_NOT_FOUND));

    // 1. 소유자 확인
    if (!userCoupon.getUserId().equals(userId)) {
        throw new CustomBusinessException(ShoppingErrorCode.USER_COUPON_NOT_FOUND);
    }

    // 2. 사용 가능 여부 확인
    if (!userCoupon.isUsable()) {
        if (userCoupon.getStatus() == UserCouponStatus.USED) {
            throw new CustomBusinessException(ShoppingErrorCode.USER_COUPON_ALREADY_USED);
        }
        throw new CustomBusinessException(ShoppingErrorCode.USER_COUPON_EXPIRED);
    }

    // 3. 최소 주문 금액 확인
    Coupon coupon = userCoupon.getCoupon();
    if (coupon.getMinimumOrderAmount() != null &&
        orderAmount.compareTo(coupon.getMinimumOrderAmount()) < 0) {
        throw new CustomBusinessException(ShoppingErrorCode.COUPON_MINIMUM_ORDER_NOT_MET);
    }
}
```

## 4. Order Saga Orchestrator

### 역할

- 주문 생성 Saga 관리
- 분산 트랜잭션 조율
- 보상 트랜잭션 (Compensation) 실행

### Saga 단계

```java
public enum SagaStep {
    RESERVE_INVENTORY(1, "재고 예약", true),    // 보상 가능
    PROCESS_PAYMENT(2, "결제 처리", true),      // 보상 가능
    DEDUCT_INVENTORY(3, "재고 차감", true),     // 보상 가능
    CREATE_DELIVERY(4, "배송 생성", true),      // 보상 가능
    CONFIRM_ORDER(5, "주문 확정", false);       // 최종 단계
}
```

### Saga 시작 (주문 생성)

```java
@Transactional
public SagaState startSaga(Order order) {
    SagaState sagaState = SagaState.builder()
        .orderId(order.getId())
        .orderNumber(order.getOrderNumber())
        .build();

    sagaState = sagaStateRepository.save(sagaState);

    try {
        // Step 1: Reserve Inventory
        executeReserveInventory(order, sagaState);
        sagaState.proceedToNextStep();
        sagaStateRepository.save(sagaState);

        return sagaState;

    } catch (Exception e) {
        compensate(sagaState, e.getMessage());
        throw new CustomBusinessException(ShoppingErrorCode.SAGA_EXECUTION_FAILED);
    }
}
```

### Saga 완료 (결제 후)

```java
@Transactional
public void completeSagaAfterPayment(String orderNumber) {
    SagaState sagaState = sagaStateRepository.findByOrderNumber(orderNumber)
        .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.SAGA_NOT_FOUND));

    Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
        .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.ORDER_NOT_FOUND));

    try {
        // Step 3: Deduct Inventory
        executeDeductInventory(order, sagaState);
        sagaState.proceedToNextStep();

        // Step 4: Create Delivery (별도 처리)
        sagaState.proceedToNextStep();

        // Step 5: Confirm Order
        order.markAsPaid();
        orderRepository.save(order);

        sagaState.complete();
        sagaStateRepository.save(sagaState);

    } catch (Exception e) {
        compensate(sagaState, e.getMessage());
        throw new CustomBusinessException(ShoppingErrorCode.SAGA_EXECUTION_FAILED);
    }
}
```

### 보상 트랜잭션

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void compensate(SagaState sagaState, String errorMessage) {
    sagaState.startCompensation(errorMessage);
    sagaStateRepository.save(sagaState);

    Order order = orderRepository.findByOrderNumberWithItems(sagaState.getOrderNumber())
        .orElse(null);

    if (order == null) {
        sagaState.markAsFailed("Order not found during compensation");
        return;
    }

    try {
        // 완료된 단계들을 역순으로 보상
        if (sagaState.isStepCompleted(SagaStep.RESERVE_INVENTORY)) {
            compensateReserveInventory(order, sagaState);
        }

        // 주문 취소
        if (order.getStatus().isCancellable()) {
            order.cancel("Saga compensation: " + errorMessage);
            orderRepository.save(order);
        }

        sagaState.markAsFailed(errorMessage);

    } catch (Exception e) {
        sagaState.incrementCompensationAttempts();
        if (sagaState.getCompensationAttempts() >= MAX_COMPENSATION_ATTEMPTS) {
            sagaState.markAsCompensationFailed(e.getMessage());
        }
    }

    sagaStateRepository.save(sagaState);
}
```

## 5. Payment Service

### 역할

- 결제 생성 및 처리
- PG사 연동
- 결제 취소/환불

### 결제 처리 흐름

```java
@Override
@Transactional
public PaymentResponse processPayment(String userId, ProcessPaymentRequest request) {
    // 1. 주문 검증
    Order order = validateOrderForPayment(request.orderNumber(), userId);

    // 2. 결제 생성
    Payment payment = Payment.builder()
        .orderId(order.getId())
        .orderNumber(order.getOrderNumber())
        .userId(userId)
        .amount(order.getFinalAmount())
        .paymentMethod(request.paymentMethod())
        .build();

    payment = paymentRepository.save(payment);
    payment.startProcessing();

    // 3. PG사 결제 요청
    PgResponse pgResponse = mockPGClient.processPayment(
        payment.getPaymentNumber(),
        payment.getAmount(),
        payment.getPaymentMethod(),
        request.cardNumber()
    );

    // 4. 결과 처리
    if (pgResponse.success()) {
        payment.complete(pgResponse.transactionId(), pgResponse.rawResponse());
        paymentRepository.save(payment);

        // 5. Saga 계속 (재고 차감, 주문 완료)
        orderService.completeOrderAfterPayment(order.getOrderNumber());

    } else {
        payment.fail(pgResponse.errorCode() + ": " + pgResponse.message(),
                     pgResponse.rawResponse());
        paymentRepository.save(payment);

        throw new CustomBusinessException(ShoppingErrorCode.PAYMENT_PROCESSING_FAILED);
    }

    return PaymentResponse.from(payment);
}
```

## 6. Domain Service vs Application Service

| 구분 | Domain Service | Application Service |
|------|---------------|---------------------|
| 위치 | 도메인 레이어 | 애플리케이션 레이어 |
| 책임 | 순수 도메인 로직 | 유스케이스 조율 |
| 트랜잭션 | 직접 관리 가능 | 주로 관리 |
| 의존성 | 도메인 객체만 | 인프라 포함 가능 |
| 예시 | 할인 계산, 재고 검증 | 주문 생성 유스케이스 |

## 7. 소스 위치

| Service | 인터페이스 | 구현체 |
|---------|-----------|--------|
| InventoryService | `inventory/service/InventoryService.java` | `InventoryServiceImpl.java` |
| CouponService | `coupon/service/CouponService.java` | `CouponServiceImpl.java` |
| PaymentService | `payment/service/PaymentService.java` | `PaymentServiceImpl.java` |
| OrderSagaOrchestrator | - | `order/saga/OrderSagaOrchestrator.java` |
| TimeDealService | `timedeal/service/TimeDealService.java` | `TimeDealServiceImpl.java` |
