# 동시성 제어 분석

## 개요

재고 관리 시스템에서 발생할 수 있는 동시성 문제와 이를 해결하기 위한 비관적 락(Pessimistic Lock) 구현을 분석합니다.

---

## 1. 동시성 문제란?

### 재고 차감 시나리오

```
초기 재고: 10개

User A: 5개 구매 시도           User B: 7개 구매 시도
    │                              │
    ▼                              ▼
읽기: 재고 = 10                읽기: 재고 = 10
    │                              │
    ▼                              ▼
검증: 10 >= 5 (OK)            검증: 10 >= 7 (OK)
    │                              │
    ▼                              ▼
차감: 10 - 5 = 5              차감: 10 - 7 = 3
    │                              │
    ▼                              ▼
저장: 재고 = 5                저장: 재고 = 3  ← 데이터 불일치!
```

**문제:**
- 두 사용자 모두 성공했지만, 총 12개가 판매됨
- 실제 재고는 10개였으므로 2개 초과 판매

---

## 2. 락(Lock) 종류

### 2.1 낙관적 락 (Optimistic Lock)

```java
@Entity
public class Inventory {
    @Version
    private Long version;
}
```

**동작 원리:**
1. 읽기: `version = 1`
2. 수정 시 `UPDATE ... WHERE version = 1`
3. 성공 시 `version = 2`로 갱신
4. 동시 수정 시 `OptimisticLockException` 발생

**장점:**
- 락 대기 없음, 높은 처리량
- 충돌이 적은 환경에 적합

**단점:**
- 충돌 시 재시도 필요
- 동시 요청이 많으면 재시도 폭발

### 2.2 비관적 락 (Pessimistic Lock)

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
Optional<Inventory> findByProductIdWithLock(@Param("productId") Long productId);
```

**동작 원리:**
1. `SELECT ... FOR UPDATE` 실행
2. 해당 행에 배타적 락 획득
3. 트랜잭션 완료까지 다른 쓰기 차단
4. 트랜잭션 커밋 시 락 해제

**장점:**
- 확실한 데이터 일관성 보장
- 충돌이 많은 환경에 적합

**단점:**
- 락 대기로 인한 지연
- 데드락 가능성

### 프로젝트 선택: Pessimistic Lock

shopping-service는 **비관적 락**을 선택했습니다:

1. 재고는 동시 접근이 빈번함 (플래시 세일 등)
2. 재고 불일치는 비즈니스적으로 치명적
3. 재시도 로직 복잡도 감소

---

## 3. 비관적 락 구현

**파일**: `inventory/repository/InventoryRepository.java`

### 단일 상품 락

```java
/**
 * 상품 ID로 재고를 조회합니다 (비관적 쓰기 락 적용).
 * SELECT ... FOR UPDATE를 사용합니다.
 * 락 획득 타임아웃은 3초로 설정됩니다.
 */
@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints({
    @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
})
@Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
Optional<Inventory> findByProductIdWithLock(@Param("productId") Long productId);
```

### 복수 상품 락 (배치)

```java
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
```

---

## 4. Lock Timeout 설정

### 목적

```java
@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")  // 3초
```

**이유:**
1. 무한 대기 방지
2. 데드락 상황 감지
3. 빠른 실패로 사용자 경험 개선

### 타임아웃 시 동작

```java
try {
    inventory = inventoryRepository.findByProductIdWithLock(productId);
} catch (PessimisticLockException e) {
    // 락 획득 실패 (타임아웃)
    throw new CustomBusinessException(ShoppingErrorCode.STOCK_UPDATE_CONFLICT);
}
```

---

## 5. 데드락 방지 전략

### 문제: 순서 없는 락 획득

```
Transaction A:                    Transaction B:
    락 획득: Product 1               락 획득: Product 2
           ↓                              ↓
    대기: Product 2                  대기: Product 1
           ↓                              ↓
         교착 상태 (Deadlock!)
```

### 해결: 일관된 락 순서

**파일**: `inventory/service/InventoryServiceImpl.java`

```java
@Transactional
public List<InventoryResponse> reserveStockBatch(
        Map<Long, Integer> quantities,
        String referenceType,
        String referenceId,
        String userId) {

    // 데드락 방지: 상품 ID 순서로 정렬
    Map<Long, Integer> sortedQuantities = new TreeMap<>(quantities);
    List<Long> productIds = new ArrayList<>(sortedQuantities.keySet());

    // 정렬된 순서로 락 획득
    List<Inventory> inventories = inventoryRepository.findByProductIdsWithLock(productIds);

    // ... 비즈니스 로직 실행
}
```

### TreeMap 사용 이유

```java
// TreeMap: 키를 자동으로 정렬
Map<Long, Integer> sortedQuantities = new TreeMap<>(quantities);

// 입력: {3: 2개, 1: 5개, 2: 3개}
// 결과: {1: 5개, 2: 3개, 3: 2개}

// ORDER BY i.productId와 일치
```

### 데드락 방지 원리

```
Transaction A:                    Transaction B:
    정렬: [1, 2, 3]                   정렬: [1, 2, 3]
           ↓                              ↓
    락 획득: Product 1               대기: Product 1 (A가 보유)
           ↓                              │
    락 획득: Product 2                    │
           ↓                              │
    락 획득: Product 3                    │
           ↓                              │
    커밋 → 락 해제                       ↓
                                   락 획득: Product 1
                                         ↓
                                   순차 진행
```

---

## 6. 전체 흐름 (재고 예약)

```java
@Transactional
public List<InventoryResponse> reserveStockBatch(...) {

    // 1. 정렬
    Map<Long, Integer> sortedQuantities = new TreeMap<>(quantities);
    List<Long> productIds = new ArrayList<>(sortedQuantities.keySet());

    // 2. 일괄 락 획득 (ORDER BY id)
    List<Inventory> inventories = inventoryRepository.findByProductIdsWithLock(productIds);

    // 3. 검증
    if (inventories.size() != productIds.size()) {
        throw new CustomBusinessException(ShoppingErrorCode.INVENTORY_NOT_FOUND);
    }

    // 4. 비즈니스 로직 실행
    for (Inventory inventory : inventories) {
        int quantity = sortedQuantities.get(inventory.getProductId());
        int previousAvailable = inventory.getAvailableQuantity();
        int previousReserved = inventory.getReservedQuantity();

        inventory.reserve(quantity);  // 가용 → 예약

        // 5. 이력 기록
        recordMovement(inventory, MovementType.RESERVE, quantity, ...);
    }

    // 6. 일괄 저장
    inventoryRepository.saveAll(inventories);

    // 7. 트랜잭션 커밋 → 락 해제
}
```

---

## 7. 락 모드 비교

| 모드 | SQL | 읽기 | 쓰기 | 용도 |
|------|-----|------|------|------|
| `PESSIMISTIC_READ` | `FOR SHARE` | 허용 | 차단 | 읽기 일관성 보장 |
| `PESSIMISTIC_WRITE` | `FOR UPDATE` | 차단 | 차단 | 쓰기 동시성 제어 |
| `PESSIMISTIC_FORCE_INCREMENT` | `FOR UPDATE` + version++ | 차단 | 차단 | 낙관적+비관적 혼합 |

### 프로젝트 선택: PESSIMISTIC_WRITE

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
```

**이유:**
- 재고 변경은 쓰기 작업
- 읽는 동안에도 다른 쓰기 차단 필요
- 가장 강력한 일관성 보장

---

## 8. 성능 고려사항

### 8.1 락 범위 최소화

```java
// 나쁜 예: 전체 테이블 락
@Query("SELECT i FROM Inventory i")
List<Inventory> findAllWithLock();

// 좋은 예: 필요한 행만 락
@Query("SELECT i FROM Inventory i WHERE i.productId IN :productIds")
List<Inventory> findByProductIdsWithLock(@Param("productIds") List<Long> productIds);
```

### 8.2 트랜잭션 시간 최소화

```java
@Transactional
public void processOrder(Order order) {
    // 1. 락 획득
    List<Inventory> inventories = repository.findByProductIdsWithLock(productIds);

    // 2. 빠르게 처리 (외부 호출 X)
    for (Inventory inventory : inventories) {
        inventory.reserve(quantity);
    }

    // 3. 저장 (락 해제는 커밋 시)
    repository.saveAll(inventories);

    // ❌ 여기서 외부 API 호출하면 락 유지 시간 증가
}
```

### 8.3 타임아웃 적절히 설정

```java
// 너무 짧으면: 정상 요청도 실패
// 너무 길면: 데드락 감지 지연

@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")  // 3초 권장
```

---

## 9. @Version (낙관적 락) 백업

**파일**: `inventory/domain/Inventory.java`

```java
@Entity
public class Inventory {
    @Version
    private Long version;  // 낙관적 락을 위한 버전 (Pessimistic Lock의 백업용)
}
```

**용도:**
1. 비관적 락이 실패한 경우 추가 검증
2. 동시 수정 감지
3. 분산 환경에서의 추가 안전장치

---

## 10. 실제 SQL 분석

### 락 획득 쿼리

```sql
SELECT i.id, i.product_id, i.available_quantity, i.reserved_quantity, ...
FROM inventory i
WHERE i.product_id IN (1, 2, 3)
ORDER BY i.product_id
FOR UPDATE
```

### MySQL InnoDB 동작

```
1. WHERE 조건에 맞는 행 검색
2. 인덱스 레코드 락 획득 (idx_inventory_product_id)
3. 실제 행 데이터 락 획득
4. 트랜잭션 완료까지 락 유지
```

---

## 11. 핵심 파일 요약

| 파일 | 역할 | 핵심 코드 |
|------|------|----------|
| `InventoryRepository.java` | 락 쿼리 정의 | `@Lock(PESSIMISTIC_WRITE)` |
| `InventoryServiceImpl.java` | 락 순서 보장 | `new TreeMap<>()` |
| `Inventory.java` | 백업용 버전 | `@Version` |

---

## 12. 핵심 요약

1. **비관적 락 선택**: 재고는 동시성 충돌이 빈번, 일관성 필수
2. **Lock Timeout 3초**: 무한 대기 방지, 빠른 실패
3. **TreeMap + ORDER BY**: 일관된 락 순서로 데드락 방지
4. **배치 처리**: 한 번에 여러 상품 락 획득으로 효율성 향상
5. **트랜잭션 최소화**: 락 유지 시간 줄이기
6. **@Version 백업**: 낙관적 락을 추가 안전장치로 유지
