# Transaction Management

## 개요

트랜잭션은 데이터베이스 작업의 논리적 단위입니다. shopping-service에서는 Spring의 `@Transactional`을 활용하여 선언적 트랜잭션 관리를 수행합니다.

---

## 1. @Transactional 기본

### 1.1 기본 사용법

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // 클래스 레벨: 기본 읽기 전용
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    // 읽기 전용 (클래스 레벨 상속)
    @Override
    public InventoryResponse getInventory(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.INVENTORY_NOT_FOUND));
        return InventoryResponse.from(inventory);
    }

    // 쓰기 작업: readOnly = false 오버라이드
    @Override
    @Transactional  // readOnly = false (기본값)
    public InventoryResponse initializeInventory(Long productId, int initialStock, String userId) {
        // 트랜잭션 내 INSERT 수행
        Inventory inventory = Inventory.builder()
                .productId(productId)
                .initialQuantity(initialStock)
                .build();
        return InventoryResponse.from(inventoryRepository.save(inventory));
    }
}
```

### 1.2 주요 속성

| 속성 | 설명 | 기본값 |
|------|------|--------|
| `readOnly` | 읽기 전용 여부 | `false` |
| `propagation` | 전파 수준 | `REQUIRED` |
| `isolation` | 격리 수준 | `DEFAULT` |
| `timeout` | 타임아웃 (초) | `-1` (무제한) |
| `rollbackFor` | 롤백 대상 예외 | RuntimeException |

---

## 2. readOnly 최적화

### 2.1 읽기 전용의 이점

```java
@Transactional(readOnly = true)
public List<OrderResponse> getUserOrders(String userId) {
    return orderRepository.findByUserId(userId)
            .stream()
            .map(OrderResponse::from)
            .collect(Collectors.toList());
}
```

이점:
- **Flush 모드 MANUAL**: Dirty Checking 비활성화
- **성능 향상**: 변경 감지 오버헤드 제거
- **DB 힌트**: 일부 DB에서 읽기 최적화 힌트

### 2.2 쓰기 작업 분리

```java
@Service
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    // 읽기 전용
    @Override
    public OrderResponse getOrder(String userId, String orderNumber) {
        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.ORDER_NOT_FOUND));
        return OrderResponse.from(order);
    }

    // 쓰기 작업
    @Override
    @Transactional
    public OrderResponse createOrder(String userId, CreateOrderRequest request) {
        Order order = Order.builder()
                .userId(userId)
                .shippingAddress(request.toAddress())
                .build();
        return OrderResponse.from(orderRepository.save(order));
    }
}
```

---

## 3. Propagation (전파 수준)

### 3.1 전파 수준 종류

| 전파 수준 | 설명 | 사용 케이스 |
|----------|------|------------|
| `REQUIRED` | 기존 트랜잭션 참여, 없으면 새로 생성 | 기본값, 대부분의 경우 |
| `REQUIRES_NEW` | 항상 새 트랜잭션 생성 (기존 일시 중단) | 독립 로깅, 감사 |
| `SUPPORTS` | 기존 트랜잭션 있으면 참여, 없으면 없이 실행 | 조회 작업 |
| `MANDATORY` | 반드시 기존 트랜잭션 필요 | 서브 서비스 |
| `NOT_SUPPORTED` | 트랜잭션 없이 실행 (기존 일시 중단) | 외부 API 호출 |
| `NEVER` | 트랜잭션이 있으면 예외 | 검증 |
| `NESTED` | 중첩 트랜잭션 (savepoint) | 부분 롤백 |

### 3.2 REQUIRED (기본)

```java
@Service
public class OrderService {

    @Transactional  // REQUIRED (기본)
    public void processOrder(Order order) {
        // 주문 저장
        orderRepository.save(order);

        // 재고 차감 (같은 트랜잭션 참여)
        inventoryService.reserveStock(order.getItems());

        // 둘 다 성공해야 커밋
    }
}

@Service
public class InventoryService {

    @Transactional  // 기존 트랜잭션 참여
    public void reserveStock(List<OrderItem> items) {
        // 같은 트랜잭션 내에서 실행
    }
}
```

### 3.3 REQUIRES_NEW

```java
@Service
public class InventoryServiceImpl implements InventoryService {

    private final StockMovementRepository stockMovementRepository;

    @Transactional
    public InventoryResponse reserveStock(Long productId, int quantity, ...) {
        // 재고 예약 로직...

        // 이력 기록은 독립 트랜잭션
        recordMovement(inventory, MovementType.RESERVE, quantity, ...);

        return InventoryResponse.from(savedInventory);
    }

    // 메인 트랜잭션 실패해도 이력은 유지
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordMovementIndependently(Inventory inventory, ...) {
        StockMovement movement = StockMovement.builder()
                .inventoryId(inventory.getId())
                .productId(inventory.getProductId())
                .build();
        stockMovementRepository.save(movement);
    }
}
```

### 3.4 NESTED

```java
@Transactional
public void processOrderWithPartialRollback(Order order) {
    // 주문 저장
    orderRepository.save(order);

    try {
        // 중첩 트랜잭션: 포인트 적립 시도
        pointService.earnPoints(order);
    } catch (PointException e) {
        // 포인트 실패해도 주문은 완료
        log.warn("Point earning failed, but order continues");
    }

    // 주문은 커밋됨
}

@Service
public class PointService {

    @Transactional(propagation = Propagation.NESTED)
    public void earnPoints(Order order) {
        // 실패 시 savepoint까지만 롤백
    }
}
```

---

## 4. Isolation (격리 수준)

### 4.1 격리 수준 종류

| 격리 수준 | Dirty Read | Non-Repeatable Read | Phantom Read |
|----------|------------|---------------------|--------------|
| `READ_UNCOMMITTED` | O | O | O |
| `READ_COMMITTED` | X | O | O |
| `REPEATABLE_READ` | X | X | O |
| `SERIALIZABLE` | X | X | X |

> MySQL InnoDB 기본값: `REPEATABLE_READ`

### 4.2 사용 예시

```java
// 재고 조회 시 높은 격리 수준
@Transactional(isolation = Isolation.SERIALIZABLE)
public int checkInventory(Long productId) {
    return inventoryRepository.findByProductId(productId)
            .map(Inventory::getAvailableQuantity)
            .orElse(0);
}

// 일반 조회는 기본 격리 수준
@Transactional(readOnly = true)
public List<ProductResponse> getProducts() {
    return productRepository.findAll().stream()
            .map(ProductResponse::from)
            .collect(Collectors.toList());
}
```

---

## 5. 예외 처리와 롤백

### 5.1 기본 롤백 규칙

```java
// 기본: RuntimeException, Error에서 롤백
@Transactional
public void createOrder() {
    throw new RuntimeException();  // 롤백 O
}

// Checked Exception은 롤백 안 함
@Transactional
public void createOrderWithChecked() throws IOException {
    throw new IOException();  // 롤백 X
}
```

### 5.2 rollbackFor / noRollbackFor

```java
// Checked Exception도 롤백
@Transactional(rollbackFor = Exception.class)
public void createOrderWithCheckedRollback() throws IOException {
    throw new IOException();  // 롤백 O
}

// 특정 RuntimeException 롤백 제외
@Transactional(noRollbackFor = CustomWarningException.class)
public void processWithWarning() {
    throw new CustomWarningException();  // 롤백 X
}

// shopping-service: 비즈니스 예외 롤백
@Transactional(rollbackFor = CustomBusinessException.class)
public void reserveStock(Long productId, int quantity) {
    if (inventory.getAvailableQuantity() < quantity) {
        throw new CustomBusinessException(ShoppingErrorCode.INSUFFICIENT_STOCK);
        // 롤백 발생
    }
}
```

---

## 6. 트랜잭션 패턴

### 6.1 Facade 패턴

```java
@Service
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;

    @Transactional
    public OrderResponse processOrder(String userId, CreateOrderRequest request) {
        // 1. 주문 생성
        Order order = orderService.createOrder(userId, request);

        // 2. 재고 예약 (같은 트랜잭션)
        inventoryService.reserveStockBatch(order.getItemQuantities(), "ORDER", order.getOrderNumber(), userId);

        // 3. 결제 처리 (같은 트랜잭션)
        paymentService.initiatePayment(order);

        return OrderResponse.from(order);
    }
}
```

### 6.2 Saga 패턴 (보상 트랜잭션)

shopping-service의 주문 처리:

```java
@Service
@RequiredArgsConstructor
public class OrderSagaOrchestrator {

    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;

    @Transactional
    public OrderResponse executeOrderSaga(String userId, CreateOrderRequest request) {
        Order order = null;
        boolean inventoryReserved = false;

        try {
            // Step 1: 주문 생성
            order = orderService.createOrderInternal(userId, request);

            // Step 2: 재고 예약
            inventoryService.reserveStockBatch(
                order.getItemQuantities(),
                "ORDER",
                order.getOrderNumber(),
                userId
            );
            inventoryReserved = true;

            // Step 3: 주문 확정
            order.confirm();

            return OrderResponse.from(order);

        } catch (Exception e) {
            // 보상 트랜잭션
            if (inventoryReserved && order != null) {
                compensateInventory(order, userId);
            }
            throw e;
        }
    }

    private void compensateInventory(Order order, String userId) {
        try {
            inventoryService.releaseStockBatch(
                order.getItemQuantities(),
                "COMPENSATION",
                order.getOrderNumber(),
                userId
            );
        } catch (Exception e) {
            log.error("Compensation failed for order: {}", order.getOrderNumber(), e);
        }
    }
}
```

---

## 7. 주의사항

### 7.1 Self-Invocation 문제

```java
@Service
public class OrderService {

    // 같은 클래스 내 메서드 호출 시 @Transactional 무시됨!
    public void processOrders(List<Long> orderIds) {
        for (Long orderId : orderIds) {
            processOrder(orderId);  // 트랜잭션 적용 안 됨
        }
    }

    @Transactional
    public void processOrder(Long orderId) {
        // 개별 트랜잭션 기대했지만 적용 안 됨
    }
}
```

해결책:
```java
// 1. 별도 서비스로 분리
@Service
public class OrderService {
    private final OrderProcessor orderProcessor;

    public void processOrders(List<Long> orderIds) {
        for (Long orderId : orderIds) {
            orderProcessor.processOrder(orderId);  // 트랜잭션 적용됨
        }
    }
}

@Service
public class OrderProcessor {
    @Transactional
    public void processOrder(Long orderId) {
        // 트랜잭션 적용
    }
}

// 2. Self-injection
@Service
public class OrderService {
    @Lazy
    @Autowired
    private OrderService self;

    public void processOrders(List<Long> orderIds) {
        for (Long orderId : orderIds) {
            self.processOrder(orderId);  // 프록시 통해 호출
        }
    }

    @Transactional
    public void processOrder(Long orderId) { }
}
```

### 7.2 트랜잭션과 예외 전파

```java
@Transactional
public void outerMethod() {
    try {
        innerMethod();
    } catch (RuntimeException e) {
        // 예외를 잡아도 트랜잭션은 이미 rollback-only 마킹됨!
        log.warn("Inner failed, but continuing...");
    }
    // 커밋 시도 시 UnexpectedRollbackException 발생
}

@Transactional
public void innerMethod() {
    throw new RuntimeException("fail");
}
```

해결책:
```java
@Transactional
public void outerMethod() {
    try {
        innerMethodNew();  // 새 트랜잭션
    } catch (RuntimeException e) {
        log.warn("Inner failed, outer continues");
    }
    // 외부 트랜잭션은 정상 커밋
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void innerMethodNew() {
    throw new RuntimeException("fail");
    // 이 트랜잭션만 롤백
}
```

---

## 8. 테스트에서의 트랜잭션

### 8.1 @Transactional 테스트

```java
@SpringBootTest
@Transactional  // 테스트 후 자동 롤백
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Test
    void createOrder_success() {
        // 테스트 데이터 생성
        OrderResponse response = orderService.createOrder(userId, request);

        // 검증
        assertThat(response.getOrderNumber()).isNotNull();

        // 테스트 종료 시 자동 롤백 (DB 상태 복원)
    }
}
```

### 8.2 롤백 방지

```java
@Test
@Rollback(false)  // 커밋 유지 (주의!)
void createOrder_withoutRollback() {
    // 테스트 데이터가 DB에 남음
}

@Test
@Commit  // @Rollback(false)와 동일
void createOrder_commit() {
    // 테스트 데이터가 DB에 남음
}
```

---

## 관련 문서

- [JPA Locking](./jpa-locking.md) - 락과 트랜잭션
- [JPA Query Optimization](./jpa-query-optimization.md) - 쿼리 최적화
- [Soft Delete & Audit](./soft-delete-audit.md) - 감사 로그
