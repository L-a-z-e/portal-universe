# JPA Locking - 동시성 제어

## 개요

쇼핑몰에서 재고 관리는 동시성 문제가 핵심입니다. 여러 사용자가 동시에 같은 상품을 주문할 때 재고가 음수가 되는 것을 방지해야 합니다. shopping-service에서는 Pessimistic Lock과 Optimistic Lock을 조합하여 사용합니다.

---

## 1. 동시성 문제 시나리오

### Lost Update 문제

```
상품 재고: 10개

시간 t1: 사용자 A가 재고 조회 (10개)
시간 t2: 사용자 B가 재고 조회 (10개)
시간 t3: 사용자 A가 3개 주문 → 10 - 3 = 7개로 업데이트
시간 t4: 사용자 B가 5개 주문 → 10 - 5 = 5개로 업데이트 (A의 변경 무시!)

결과: 재고 5개 (실제로는 2개여야 함)
```

---

## 2. Pessimistic Lock (비관적 잠금)

### 2.1 개념

- 충돌이 발생할 것이라 가정하고 미리 락을 획득
- `SELECT ... FOR UPDATE` SQL 사용
- 다른 트랜잭션이 해당 행을 수정하지 못하도록 차단

### 2.2 shopping-service 구현

#### InventoryRepository

```java
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /**
     * 상품 ID로 재고를 조회합니다 (비관적 쓰기 락 적용).
     * 동시 수정을 방지하기 위해 SELECT ... FOR UPDATE를 사용합니다.
     * 락 획득 타임아웃은 3초로 설정됩니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
    })
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> findByProductIdWithLock(@Param("productId") Long productId);

    /**
     * 여러 상품 ID로 재고를 조회합니다 (비관적 쓰기 락 적용).
     * 데드락 방지를 위해 상품 ID 순서로 정렬하여 조회합니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
    })
    @Query("SELECT i FROM Inventory i WHERE i.productId IN :productIds ORDER BY i.productId")
    List<Inventory> findByProductIdsWithLock(@Param("productIds") List<Long> productIds);
}
```

### 2.3 LockModeType 종류

| 모드 | SQL | 설명 |
|------|-----|------|
| `PESSIMISTIC_READ` | `SELECT ... FOR SHARE` | 공유 잠금 (읽기 허용, 쓰기 차단) |
| `PESSIMISTIC_WRITE` | `SELECT ... FOR UPDATE` | 배타적 잠금 (읽기/쓰기 모두 차단) |
| `PESSIMISTIC_FORCE_INCREMENT` | `FOR UPDATE` + version 증가 | 버전 강제 증가 |

### 2.4 Service에서 사용

```java
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    @Override
    @Transactional
    public InventoryResponse reserveStock(Long productId, int quantity,
                                          String referenceType, String referenceId, String userId) {
        // 비관적 락으로 재고 조회 (다른 트랜잭션 대기)
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.INVENTORY_NOT_FOUND));

        int previousAvailable = inventory.getAvailableQuantity();
        int previousReserved = inventory.getReservedQuantity();

        // 재고 예약 (가용 재고에서 예약 재고로 이동)
        inventory.reserve(quantity);
        Inventory savedInventory = inventoryRepository.save(inventory);

        // 재고 이동 이력 기록
        recordMovement(savedInventory, MovementType.RESERVE, quantity, ...);

        log.info("Reserved {} units for product {} (ref: {})", quantity, productId, referenceId);
        return InventoryResponse.from(savedInventory);
    }
}
```

### 2.5 락 타임아웃 설정

```java
@QueryHints({
    @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")  // 3초
})
```

타임아웃 초과 시:
```java
try {
    inventoryRepository.findByProductIdWithLock(productId);
} catch (PessimisticLockException e) {
    throw new CustomBusinessException(ShoppingErrorCode.CONCURRENT_UPDATE_FAILED);
}
```

---

## 3. 데드락 방지

### 3.1 문제 상황

```
트랜잭션 A: 상품 1 락 → 상품 2 락 시도 (대기)
트랜잭션 B: 상품 2 락 → 상품 1 락 시도 (대기)
→ 데드락 발생!
```

### 3.2 해결: 정렬된 순서로 락 획득

shopping-service의 Batch 예약 메서드:

```java
@Override
@Transactional
public List<InventoryResponse> reserveStockBatch(
        Map<Long, Integer> quantities,
        String referenceType,
        String referenceId,
        String userId) {

    // 1. 데드락 방지: 상품 ID 순서로 정렬
    Map<Long, Integer> sortedQuantities = new TreeMap<>(quantities);
    List<Long> productIds = new ArrayList<>(sortedQuantities.keySet());

    // 2. 정렬된 순서로 락 획득 (ORDER BY productId)
    List<Inventory> inventories = inventoryRepository.findByProductIdsWithLock(productIds);

    if (inventories.size() != productIds.size()) {
        throw new CustomBusinessException(ShoppingErrorCode.INVENTORY_NOT_FOUND);
    }

    // 3. 순서대로 재고 예약
    List<InventoryResponse> responses = new ArrayList<>();
    for (Inventory inventory : inventories) {
        int quantity = sortedQuantities.get(inventory.getProductId());
        inventory.reserve(quantity);
        responses.add(InventoryResponse.from(inventory));
    }

    inventoryRepository.saveAll(inventories);
    return responses;
}
```

Repository 쿼리:
```java
@Query("SELECT i FROM Inventory i WHERE i.productId IN :productIds ORDER BY i.productId")
List<Inventory> findByProductIdsWithLock(@Param("productIds") List<Long> productIds);
```

---

## 4. Optimistic Lock (낙관적 잠금)

### 4.1 개념

- 충돌이 드물 것이라 가정
- 실제 업데이트 시점에 충돌 감지
- `@Version` 필드 사용

### 4.2 shopping-service 구현

#### Inventory Entity

```java
@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    /**
     * 낙관적 락을 위한 버전 (Pessimistic Lock의 백업용)
     */
    @Version
    private Long version;
}
```

### 4.3 동작 원리

```sql
-- 조회 시: version 함께 가져옴
SELECT id, product_id, available_quantity, version
FROM inventory
WHERE product_id = 1;
-- 결과: id=1, available=10, version=5

-- 업데이트 시: version 조건 추가
UPDATE inventory
SET available_quantity = 7, version = 6
WHERE id = 1 AND version = 5;
-- 영향받은 행이 0이면 OptimisticLockException 발생
```

### 4.4 충돌 처리

```java
@Transactional
public void updateInventory(Long productId, int quantity) {
    int maxRetries = 3;
    int retryCount = 0;

    while (retryCount < maxRetries) {
        try {
            Inventory inventory = inventoryRepository.findByProductId(productId)
                    .orElseThrow();
            inventory.reserve(quantity);
            inventoryRepository.save(inventory);
            return;
        } catch (OptimisticLockingFailureException e) {
            retryCount++;
            if (retryCount >= maxRetries) {
                throw new CustomBusinessException(ShoppingErrorCode.CONCURRENT_UPDATE_FAILED);
            }
            // 짧은 지연 후 재시도
            Thread.sleep(100 * retryCount);
        }
    }
}
```

---

## 5. 재고 차감 플로우

### 5.1 전체 흐름

```
1. 주문 생성 → reserve() 호출
   └─ 가용 재고 감소, 예약 재고 증가

2. 결제 완료 → deduct() 호출
   └─ 예약 재고 감소, 전체 재고 감소

3. 주문 취소 → release() 호출
   └─ 예약 재고 감소, 가용 재고 증가
```

### 5.2 Inventory Entity 메서드

```java
@Entity
public class Inventory {

    /**
     * 재고를 예약합니다 (주문 생성 시).
     * 가용 재고에서 예약 재고로 이동합니다.
     */
    public void reserve(int quantity) {
        if (quantity <= 0) {
            throw new CustomBusinessException(ShoppingErrorCode.INVALID_STOCK_QUANTITY);
        }
        if (this.availableQuantity < quantity) {
            throw new CustomBusinessException(ShoppingErrorCode.INSUFFICIENT_STOCK);
        }
        this.availableQuantity -= quantity;
        this.reservedQuantity += quantity;
    }

    /**
     * 예약된 재고를 실제로 차감합니다 (결제 완료 시).
     * 예약 재고와 전체 재고를 감소시킵니다.
     */
    public void deduct(int quantity) {
        if (quantity <= 0) {
            throw new CustomBusinessException(ShoppingErrorCode.INVALID_STOCK_QUANTITY);
        }
        if (this.reservedQuantity < quantity) {
            throw new CustomBusinessException(ShoppingErrorCode.STOCK_DEDUCTION_FAILED);
        }
        this.reservedQuantity -= quantity;
        this.totalQuantity -= quantity;
    }

    /**
     * 예약된 재고를 해제합니다 (주문 취소, 결제 실패 시).
     * 예약 재고에서 가용 재고로 복원합니다.
     */
    public void release(int quantity) {
        if (quantity <= 0) {
            throw new CustomBusinessException(ShoppingErrorCode.INVALID_STOCK_QUANTITY);
        }
        if (this.reservedQuantity < quantity) {
            throw new CustomBusinessException(ShoppingErrorCode.STOCK_RELEASE_FAILED);
        }
        this.reservedQuantity -= quantity;
        this.availableQuantity += quantity;
    }
}
```

---

## 6. Pessimistic vs Optimistic Lock 비교

| 특성 | Pessimistic Lock | Optimistic Lock |
|------|------------------|-----------------|
| 충돌 예상 | 높음 | 낮음 |
| 락 시점 | 조회 시 | 업데이트 시 |
| DB 부하 | 높음 | 낮음 |
| 처리량 | 낮음 (대기 발생) | 높음 |
| 실패 처리 | 타임아웃 | 재시도 필요 |
| 적합한 상황 | 재고 관리, 결제 | 일반 CRUD |

### shopping-service 전략

```java
// 재고 관리 - Pessimistic Lock (충돌 빈번)
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Inventory> findByProductIdWithLock(Long productId);

// 백업으로 @Version도 함께 사용
@Version
private Long version;
```

---

## 7. 분산 환경에서의 락

### 7.1 분산 락 필요성

여러 인스턴스가 있는 경우 DB 레벨 락만으로는 부족할 수 있습니다.

### 7.2 Redis 분산 락 (shopping-service 구현)

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    String key();
    long waitTime() default 5;
    long leaseTime() default 10;
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
```

```java
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final RedissonClient redissonClient;

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock)
            throws Throwable {

        String key = distributedLock.key();
        RLock lock = redissonClient.getLock(key);

        try {
            boolean acquired = lock.tryLock(
                distributedLock.waitTime(),
                distributedLock.leaseTime(),
                distributedLock.timeUnit()
            );

            if (!acquired) {
                throw new CustomBusinessException(ShoppingErrorCode.LOCK_ACQUISITION_FAILED);
            }

            return joinPoint.proceed();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### 7.3 사용 예시

```java
@DistributedLock(key = "'inventory:' + #productId")
public void updateInventoryWithDistributedLock(Long productId, int quantity) {
    // Redis 락 획득 후 실행
    Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
            .orElseThrow();
    inventory.reserve(quantity);
    inventoryRepository.save(inventory);
}
```

---

## 8. 베스트 프랙티스

### 8.1 락 선택 가이드

| 상황 | 권장 락 |
|------|--------|
| 재고 차감 (높은 충돌) | Pessimistic Write |
| 쿠폰 발급 (높은 충돌) | Pessimistic Write |
| 주문 상태 변경 | Optimistic Lock |
| 일반 업데이트 | Optimistic Lock |
| 분산 환경 | DB Lock + Redis 분산 락 |

### 8.2 주의사항

1. **락 범위 최소화**: 락을 잡는 시간을 최소화
2. **타임아웃 설정**: 무한 대기 방지
3. **데드락 방지**: 항상 같은 순서로 락 획득
4. **트랜잭션 범위**: 락과 트랜잭션 범위 일치

---

## 관련 문서

- [Transaction Management](./transaction-management.md) - 트랜잭션 관리
- [JPA Entity Mapping](./jpa-entity-mapping.md) - @Version 설정
- [Database Indexing](./database-indexing.md) - 락 성능 향상
