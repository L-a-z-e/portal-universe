# Soft Delete & Audit

## 개요

엔터프라이즈 애플리케이션에서는 데이터 추적과 복구를 위해 Soft Delete와 감사(Audit) 기능이 필수입니다. shopping-service에서는 JPA Auditing을 활용하여 엔티티의 생성/수정 시간을 자동 관리하고, 재고 이동 이력을 통해 변경 추적을 구현합니다.

---

## 1. JPA Auditing

### 1.1 Auditing 활성화

```java
// JpaConfig.java
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
```

### 1.2 @CreatedDate / @LastModifiedDate

shopping-service의 Order 엔티티:

```java
@Entity
@Table(name = "orders")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ... 비즈니스 필드들 ...

    /**
     * 생성 일시 (자동 설정, 수정 불가)
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 일시 (자동 갱신)
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

### 1.3 shopping-service 엔티티별 Auditing

| 엔티티 | @CreatedDate | @LastModifiedDate | 비고 |
|--------|--------------|-------------------|------|
| Order | createdAt | updatedAt | 주문 생성/수정 추적 |
| Cart | createdAt | updatedAt | 장바구니 활동 추적 |
| CartItem | addedAt | - | 장바구니 추가 시점만 |
| Inventory | createdAt | updatedAt | 재고 변경 시점 |
| StockMovement | createdAt | - | 이력 생성 시점만 |
| Delivery | createdAt | updatedAt | 배송 상태 변경 추적 |
| DeliveryHistory | createdAt | - | 이력 생성 시점만 |

### 1.4 실제 구현 예시

```java
// Cart.java
@Entity
@Table(name = "carts")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

// CartItem.java - 생성 시점만 기록
@Entity
@Table(name = "cart_items")
@EntityListeners(AuditingEntityListener.class)
public class CartItem {

    /**
     * 장바구니에 추가된 시간
     */
    @CreatedDate
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;
}
```

---

## 2. Audit Entity (감사 이력)

### 2.1 StockMovement - 재고 변경 이력

shopping-service의 핵심 감사 엔티티:

```java
@Entity
@Table(name = "stock_movements", indexes = {
    @Index(name = "idx_stock_movement_inventory_id", columnList = "inventory_id"),
    @Index(name = "idx_stock_movement_product_id", columnList = "product_id"),
    @Index(name = "idx_stock_movement_reference", columnList = "reference_type, reference_id"),
    @Index(name = "idx_stock_movement_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 재고 엔티티 ID
     */
    @Column(name = "inventory_id", nullable = false)
    private Long inventoryId;

    /**
     * 상품 ID (빠른 조회를 위해 비정규화)
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * 이동 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private MovementType movementType;

    /**
     * 이동 수량
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * 이동 전 가용 재고
     */
    @Column(name = "previous_available", nullable = false)
    private Integer previousAvailable;

    /**
     * 이동 후 가용 재고
     */
    @Column(name = "after_available", nullable = false)
    private Integer afterAvailable;

    /**
     * 이동 전 예약 재고
     */
    @Column(name = "previous_reserved", nullable = false)
    private Integer previousReserved;

    /**
     * 이동 후 예약 재고
     */
    @Column(name = "after_reserved", nullable = false)
    private Integer afterReserved;

    /**
     * 참조 유형 (ORDER, PAYMENT, RETURN, ADMIN 등)
     */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    /**
     * 참조 ID (주문번호, 결제번호 등)
     */
    @Column(name = "reference_id", length = 100)
    private String referenceId;

    /**
     * 이동 사유
     */
    @Column(name = "reason", length = 500)
    private String reason;

    /**
     * 작업 수행자 (사용자 ID 또는 시스템)
     */
    @Column(name = "performed_by", length = 100)
    private String performedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

### 2.2 MovementType Enum

```java
public enum MovementType {
    INITIAL,    // 초기 설정
    INBOUND,    // 입고
    OUTBOUND,   // 출고
    RESERVE,    // 예약 (주문 생성)
    RELEASE,    // 예약 해제 (주문 취소)
    DEDUCT,     // 차감 (결제 완료)
    ADJUST,     // 관리자 조정
    RETURN      // 반품
}
```

### 2.3 이력 기록 서비스

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;

    @Override
    @Transactional
    public InventoryResponse reserveStock(Long productId, int quantity,
                                          String referenceType, String referenceId, String userId) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.INVENTORY_NOT_FOUND));

        int previousAvailable = inventory.getAvailableQuantity();
        int previousReserved = inventory.getReservedQuantity();

        inventory.reserve(quantity);
        Inventory savedInventory = inventoryRepository.save(inventory);

        // 재고 이동 이력 기록
        recordMovement(savedInventory, MovementType.RESERVE, quantity,
                previousAvailable, savedInventory.getAvailableQuantity(),
                previousReserved, savedInventory.getReservedQuantity(),
                referenceType, referenceId, "Stock reserved for order", userId);

        return InventoryResponse.from(savedInventory);
    }

    /**
     * 재고 이동 이력을 기록합니다.
     */
    private void recordMovement(Inventory inventory, MovementType movementType, int quantity,
                                int previousAvailable, int afterAvailable,
                                int previousReserved, int afterReserved,
                                String referenceType, String referenceId,
                                String reason, String performedBy) {
        StockMovement movement = StockMovement.builder()
                .inventoryId(inventory.getId())
                .productId(inventory.getProductId())
                .movementType(movementType)
                .quantity(quantity)
                .previousAvailable(previousAvailable)
                .afterAvailable(afterAvailable)
                .previousReserved(previousReserved)
                .afterReserved(afterReserved)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .reason(reason)
                .performedBy(performedBy)
                .build();

        stockMovementRepository.save(movement);
    }
}
```

---

## 3. DeliveryHistory - 배송 상태 이력

### 3.1 Entity 구현

```java
@Entity
@Table(name = "delivery_histories", indexes = {
    @Index(name = "idx_delivery_history_delivery_id", columnList = "delivery_id"),
    @Index(name = "idx_delivery_history_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 소속 배송
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    /**
     * 배송 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeliveryStatus status;

    /**
     * 위치 정보
     */
    @Column(name = "location", length = 255)
    private String location;

    /**
     * 상세 설명
     */
    @Column(name = "description", length = 500)
    private String description;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

### 3.2 자동 이력 기록

```java
@Entity
public class Delivery {

    @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<DeliveryHistory> histories = new ArrayList<>();

    /**
     * 배송 상태를 변경합니다.
     */
    public void updateStatus(DeliveryStatus newStatus, String location, String description) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new CustomBusinessException(ShoppingErrorCode.INVALID_DELIVERY_STATUS);
        }

        this.status = newStatus;

        // 배송 완료 시 실제 배송일 기록
        if (newStatus == DeliveryStatus.DELIVERED) {
            this.actualDeliveryDate = LocalDate.now();
        }

        // 이력 자동 추가
        addHistory(newStatus, location, description);
    }

    /**
     * 배송 이력을 추가합니다.
     */
    private void addHistory(DeliveryStatus status, String location, String description) {
        DeliveryHistory history = DeliveryHistory.builder()
                .delivery(this)
                .status(status)
                .location(location)
                .description(description)
                .build();

        this.histories.add(history);
    }
}
```

---

## 4. Soft Delete 패턴

### 4.1 기본 구현

```java
@Entity
@Table(name = "products")
@SQLDelete(sql = "UPDATE products SET deleted = true, deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted = false")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ... 비즈니스 필드들 ...

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
```

### 4.2 shopping-service에서의 접근 방식

현재 shopping-service에서는 Hard Delete 대신 상태 관리를 사용합니다:

```java
// 주문 취소 - 삭제 대신 상태 변경
public void cancel(String reason) {
    if (!this.status.isCancellable()) {
        throw new CustomBusinessException(ShoppingErrorCode.ORDER_CANNOT_BE_CANCELLED);
    }
    this.status = OrderStatus.CANCELLED;
    this.cancelReason = reason;
    this.cancelledAt = LocalDateTime.now();  // 취소 시점 기록
}

// 장바구니 - 상태 변경
public void checkout() {
    validateActive();
    if (this.items.isEmpty()) {
        throw new CustomBusinessException(ShoppingErrorCode.CART_EMPTY);
    }
    this.status = CartStatus.CHECKED_OUT;  // 삭제 대신 상태 변경
}

// 쿠폰 - 상태 변경
public void deactivate() {
    this.status = CouponStatus.INACTIVE;
    this.updatedAt = LocalDateTime.now();
}
```

### 4.3 Soft Delete MappedSuperclass

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class SoftDeletableEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    public void softDelete(String deletedBy) {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }

    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
    }
}
```

---

## 5. AuditorAware - 작성자 자동 기록

### 5.1 AuditorAware 구현

```java
@Component
public class SecurityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .or(() -> Optional.of("SYSTEM"));
    }
}
```

### 5.2 설정 적용

```java
@Configuration
@EnableJpaAuditing(auditorAwareRef = "securityAuditorAware")
public class JpaConfig {
}
```

### 5.3 Entity에서 사용

```java
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
}
```

---

## 6. 이력 조회 API

### 6.1 Repository

```java
@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    List<StockMovement> findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(
        String referenceType, String referenceId);
}
```

### 6.2 Service

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

### 6.3 Response DTO

```java
public record StockMovementResponse(
    Long id,
    Long productId,
    String movementType,
    Integer quantity,
    Integer previousAvailable,
    Integer afterAvailable,
    Integer previousReserved,
    Integer afterReserved,
    String referenceType,
    String referenceId,
    String reason,
    String performedBy,
    LocalDateTime createdAt
) {
    public static StockMovementResponse from(StockMovement movement) {
        return new StockMovementResponse(
            movement.getId(),
            movement.getProductId(),
            movement.getMovementType().name(),
            movement.getQuantity(),
            movement.getPreviousAvailable(),
            movement.getAfterAvailable(),
            movement.getPreviousReserved(),
            movement.getAfterReserved(),
            movement.getReferenceType(),
            movement.getReferenceId(),
            movement.getReason(),
            movement.getPerformedBy(),
            movement.getCreatedAt()
        );
    }
}
```

---

## 7. 감사 로그 베스트 프랙티스

### 7.1 무엇을 기록할 것인가

| 항목 | 필수 | 설명 |
|------|------|------|
| 변경 전 값 | O | 롤백/분석용 |
| 변경 후 값 | O | 현재 상태 확인 |
| 변경 시점 | O | 타임라인 추적 |
| 변경 주체 | O | 책임 추적 |
| 참조 ID | O | 관련 작업 연결 |
| 변경 사유 | 권장 | 컨텍스트 이해 |

### 7.2 성능 고려사항

1. **비동기 로깅**: 메인 트랜잭션 성능 영향 최소화
2. **배치 Insert**: 대량 이력 기록 시
3. **인덱스 설계**: 자주 조회되는 컬럼에 인덱스
4. **파티셔닝**: 대용량 이력 테이블 관리

### 7.3 보존 정책

```sql
-- 오래된 이력 아카이브 (예: 1년 이상)
INSERT INTO stock_movements_archive
SELECT * FROM stock_movements
WHERE created_at < DATE_SUB(NOW(), INTERVAL 1 YEAR);

DELETE FROM stock_movements
WHERE created_at < DATE_SUB(NOW(), INTERVAL 1 YEAR);
```

---

## 관련 문서

- [JPA Entity Mapping](./jpa-entity-mapping.md) - 엔티티 설계
- [Transaction Management](./transaction-management.md) - 트랜잭션 관리
- [Database Indexing](./database-indexing.md) - 이력 테이블 인덱스
