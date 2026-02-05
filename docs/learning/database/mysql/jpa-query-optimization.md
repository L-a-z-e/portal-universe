# JPA Query Optimization

## 개요

JPA를 사용할 때 가장 흔히 발생하는 성능 문제는 N+1 쿼리 문제입니다. shopping-service에서는 Fetch Join, @EntityGraph, 적절한 페이지네이션을 활용하여 쿼리를 최적화합니다.

---

## 1. N+1 문제

### N+1 문제란?

연관 엔티티를 조회할 때 발생하는 성능 문제입니다:
- 1개의 쿼리로 N개의 부모 엔티티 조회
- 각 부모 엔티티의 자식을 조회하기 위해 N개의 추가 쿼리 실행

### 발생 예시

```java
// 주문 목록 조회 (1번 쿼리)
List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);

// 각 주문의 아이템 접근 시 추가 쿼리 발생 (N번 쿼리)
for (Order order : orders) {
    // LAZY 로딩으로 인해 각 주문마다 별도 쿼리 실행
    int itemCount = order.getItems().size();  // SELECT from order_items WHERE order_id = ?
}
```

실행되는 쿼리:
```sql
-- 1번: 주문 조회
SELECT * FROM orders WHERE user_id = ?

-- N번: 각 주문의 아이템 조회
SELECT * FROM order_items WHERE order_id = 1
SELECT * FROM order_items WHERE order_id = 2
SELECT * FROM order_items WHERE order_id = 3
-- ... N번 반복
```

---

## 2. Fetch Join

### 2.1 기본 Fetch Join

shopping-service의 OrderRepository 예시:

```java
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 주문 번호로 주문을 조회합니다 (항목과 함께 Fetch Join).
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithItems(@Param("orderNumber") String orderNumber);
}
```

실행되는 쿼리:
```sql
SELECT o.*, oi.*
FROM orders o
LEFT JOIN order_items oi ON o.id = oi.order_id
WHERE o.order_number = ?
```

### 2.2 여러 연관관계 Fetch Join

```java
@Query("SELECT d FROM Delivery d " +
       "LEFT JOIN FETCH d.histories " +
       "WHERE d.trackingNumber = :trackingNumber")
Optional<Delivery> findByTrackingNumberWithHistories(
    @Param("trackingNumber") String trackingNumber);
```

### 2.3 Fetch Join 주의사항

#### 컬렉션 Fetch Join + 페이징 불가

```java
// 잘못된 사용 - 메모리에서 페이징 발생 (WARNING 로그)
@Query("SELECT o FROM Order o LEFT JOIN FETCH o.items")
Page<Order> findAllWithItems(Pageable pageable);
```

Hibernate 경고:
```
WARN  HHH90003004: firstResult/maxResults specified with collection fetch; applying in memory!
```

#### 해결책 1: Batch Size 설정

```yaml
# application.yml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 100
```

#### 해결책 2: @BatchSize 어노테이션

```java
@Entity
public class Order {

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
}
```

---

## 3. @EntityGraph

### 3.1 기본 사용법

Fetch Join의 선언적 대안:

```java
@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    @EntityGraph(attributePaths = {"items"})
    Optional<Cart> findByUserId(String userId);
}
```

### 3.2 여러 연관관계 로딩

```java
@EntityGraph(attributePaths = {"items", "shippingAddress"})
@Query("SELECT o FROM Order o WHERE o.orderNumber = :orderNumber")
Optional<Order> findByOrderNumberWithDetails(@Param("orderNumber") String orderNumber);
```

### 3.3 Named EntityGraph

```java
@Entity
@NamedEntityGraph(
    name = "Order.withItems",
    attributeNodes = @NamedAttributeNode("items")
)
@NamedEntityGraph(
    name = "Order.full",
    attributeNodes = {
        @NamedAttributeNode("items"),
        @NamedAttributeNode("shippingAddress")
    }
)
public class Order { }

// Repository에서 사용
@EntityGraph(value = "Order.withItems")
Optional<Order> findByOrderNumber(String orderNumber);
```

---

## 4. 페이지네이션 최적화

### 4.1 기본 페이지네이션

shopping-service Repository 사례:

```java
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 사용자의 주문 목록을 조회합니다 (최신순 정렬).
     */
    Page<Order> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * 사용자의 특정 상태 주문 목록을 조회합니다.
     */
    Page<Order> findByUserIdAndStatusOrderByCreatedAtDesc(
        String userId,
        OrderStatus status,
        Pageable pageable
    );
}
```

### 4.2 Service에서 페이지네이션 사용

```java
@Service
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    @Override
    public Page<StockMovementResponse> getStockMovements(Long productId, Pageable pageable) {
        return stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(StockMovementResponse::from);
    }
}
```

### 4.3 Slice vs Page

| 타입 | 설명 | 추가 쿼리 |
|------|------|----------|
| `Page<T>` | 전체 개수 포함 | COUNT 쿼리 실행 |
| `Slice<T>` | 다음 페이지 존재 여부만 | COUNT 쿼리 없음 |

```java
// Page - 전체 개수가 필요할 때
Page<Order> findByUserId(String userId, Pageable pageable);

// Slice - 무한 스크롤 등에 적합
Slice<Order> findByStatus(OrderStatus status, Pageable pageable);
```

### 4.4 Count 쿼리 최적화

```java
@Query(value = "SELECT o FROM Order o WHERE o.userId = :userId",
       countQuery = "SELECT count(o.id) FROM Order o WHERE o.userId = :userId")
Page<Order> findByUserIdOptimized(@Param("userId") String userId, Pageable pageable);
```

---

## 5. Projection 활용

### 5.1 Interface-based Projection

필요한 필드만 조회:

```java
public interface OrderSummary {
    String getOrderNumber();
    String getUserId();
    BigDecimal getTotalAmount();
    OrderStatus getStatus();
    LocalDateTime getCreatedAt();
}

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<OrderSummary> findSummaryByUserId(String userId);
}
```

### 5.2 Class-based Projection (DTO)

```java
public record OrderDto(
    String orderNumber,
    String userId,
    BigDecimal totalAmount,
    OrderStatus status
) {}

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT new com.portal.universe.shoppingservice.order.dto.OrderDto(" +
           "o.orderNumber, o.userId, o.totalAmount, o.status) " +
           "FROM Order o WHERE o.userId = :userId")
    List<OrderDto> findDtoByUserId(@Param("userId") String userId);
}
```

---

## 6. Batch 처리 최적화

### 6.1 application.yml 설정

shopping-service 설정:

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 20         # Batch Insert/Update 크기
        order_inserts: true      # Insert 문 정렬
        order_updates: true      # Update 문 정렬
```

### 6.2 Batch Insert 예시

```java
@Transactional
public void createBulkOrders(List<CreateOrderRequest> requests) {
    List<Order> orders = requests.stream()
            .map(this::createOrder)
            .collect(Collectors.toList());

    // Batch Insert 실행 (batch_size 단위로 묶음)
    orderRepository.saveAll(orders);
}
```

실행되는 쿼리 (batch_size=20):
```sql
INSERT INTO orders (order_number, user_id, ...) VALUES
  (?, ?, ...),
  (?, ?, ...),
  -- 20개씩 묶음
  (?, ?, ...);
```

---

## 7. 쿼리 최적화 체크리스트

### 7.1 성능 점검 항목

- [ ] N+1 쿼리 발생 여부 확인
- [ ] 불필요한 컬럼 조회 여부 (SELECT * 지양)
- [ ] 인덱스 활용 여부
- [ ] 페이지네이션 적용 여부
- [ ] Fetch Join과 페이징 혼용 여부

### 7.2 쿼리 로그 설정

```yaml
# 개발 환경
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true

logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.orm.jdbc.bind: TRACE  # 바인딩 파라미터 출력
```

### 7.3 성능 측정

```java
@Slf4j
@Service
public class OrderQueryService {

    public List<Order> getOrdersWithTiming(String userId) {
        long startTime = System.currentTimeMillis();

        List<Order> orders = orderRepository.findByUserIdWithItems(userId);

        long endTime = System.currentTimeMillis();
        log.info("Query execution time: {} ms", endTime - startTime);

        return orders;
    }
}
```

---

## 8. 실제 적용 패턴

### 8.1 단건 조회 - Fetch Join

```java
// 주문 상세 조회 (아이템 포함)
@Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.orderNumber = :orderNumber")
Optional<Order> findByOrderNumberWithItems(@Param("orderNumber") String orderNumber);
```

### 8.2 목록 조회 - 페이지네이션

```java
// 주문 목록 조회 (아이템 제외, 페이지네이션)
Page<Order> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
```

### 8.3 집계 쿼리

```java
@Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId AND o.status = :status")
long countByUserIdAndStatus(@Param("userId") String userId, @Param("status") OrderStatus status);
```

---

## 관련 문서

- [JPA Entity Mapping](./jpa-entity-mapping.md) - 엔티티 설계
- [JPA Locking](./jpa-locking.md) - 동시성 제어
- [Database Indexing](./database-indexing.md) - 인덱스 전략
