# Product Domain

## 1. 개요

Product 도메인은 쇼핑 서비스의 핵심 Entity로, 판매되는 상품 정보를 관리합니다.

## 2. Entity 구조

### Product Entity

```java
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;           // 상품명

    private String description;    // 상품 설명

    @Column(nullable = false)
    private Double price;          // 가격

    @Column(nullable = false)
    private Integer stock;         // 재고 수량
}
```

### 필드 설명

| 필드 | 타입 | 설명 | 제약조건 |
|------|------|------|----------|
| `id` | Long | 상품 고유 ID | Auto Increment |
| `name` | String | 상품명 | Not Null |
| `description` | String | 상품 설명 | Nullable |
| `price` | Double | 가격 | Not Null, > 0 |
| `stock` | Integer | 재고 수량 | Not Null, >= 0 |

## 3. 상품 상태 관리

### 현재 구현

현재 Product Entity는 별도의 상태 필드가 없으며, `stock` 필드로 판매 가능 여부를 판단합니다.

```java
// 재고가 0이면 품절 상태로 간주
if (product.getStock() <= 0) {
    // 품절 처리
}
```

### 권장 확장 구조

복잡한 상품 상태 관리가 필요한 경우:

```java
public enum ProductStatus {
    ACTIVE,        // 판매 중
    INACTIVE,      // 비활성
    OUT_OF_STOCK,  // 품절
    DISCONTINUED,  // 단종
    PENDING        // 승인 대기
}

@Enumerated(EnumType.STRING)
@Column(name = "status", nullable = false)
private ProductStatus status = ProductStatus.ACTIVE;
```

## 4. 비즈니스 메서드

### update 메서드

```java
/**
 * 상품 정보를 수정합니다.
 */
public void update(String name, String description, Double price, Integer stock) {
    this.name = name;
    this.description = description;
    this.price = price;
    this.stock = stock;
}
```

### 주의사항

- 상품 가격과 재고 변경은 주문/결제 진행 중인 상품에 영향을 줄 수 있음
- Order, Cart에서는 상품 정보를 **스냅샷**으로 저장하여 가격 변동에 영향받지 않도록 설계됨

## 5. 다른 도메인과의 관계

```
Product (1) ─────── (N) OrderItem
    │                      │
    │ productId 참조       │ 스냅샷 저장
    │                      │ (productName, price)
    │
Product (1) ─────── (N) CartItem
    │                      │
    │ productId 참조       │ 스냅샷 저장
    │
Product (1) ─────── (1) Inventory
    │                      │
    │ productId 참조       │ 재고 관리 분리
    │
Product (1) ─────── (N) TimeDealProduct
                           │
                           │ 타임딜 가격/수량
```

## 6. 재고 관리 패턴

### 분리된 Inventory 도메인

실제 재고 관리는 `Inventory` Entity로 분리되어 있습니다:

```java
@Entity
@Table(name = "inventory")
public class Inventory {
    private Long productId;
    private Integer availableQuantity;  // 가용 재고
    private Integer reservedQuantity;   // 예약 재고
    private Integer totalQuantity;      // 전체 재고
}
```

### 재고 흐름

```
[가용 재고] ──reserve──> [예약 재고] ──deduct──> [차감]
     ^                       │
     │                       │
     └───────release────────┘
           (취소/실패)
```

## 7. 검색 연동

Product는 Elasticsearch와 연동되어 검색 기능을 제공합니다:

```java
// ProductDocument - Elasticsearch 문서
@Document(indexName = "products")
public class ProductDocument {
    @Id
    private String id;
    private String name;
    private String description;
    private Double price;
    private Integer stock;
}
```

## 8. Error Codes

| 코드 | 설명 |
|------|------|
| `S001` | PRODUCT_NOT_FOUND - 상품을 찾을 수 없음 |
| `S002` | PRODUCT_ALREADY_EXISTS - 상품이 이미 존재함 |
| `S003` | PRODUCT_INACTIVE - 상품이 비활성 상태 |
| `S004` | INVALID_PRODUCT_PRICE - 유효하지 않은 가격 |
| `S005` | INVALID_PRODUCT_QUANTITY - 유효하지 않은 수량 |
| `S008` | PRODUCT_NAME_ALREADY_EXISTS - 상품명 중복 |
| `S009` | CANNOT_DELETE_PRODUCT_WITH_ORDERS - 주문이 있는 상품 삭제 불가 |

## 9. 소스 위치

- Entity: `product/domain/Product.java`
- Repository: `product/repository/ProductRepository.java`
- Service: `product/service/ProductService.java`, `ProductServiceImpl.java`
- Controller: `product/controller/ProductController.java`, `AdminProductController.java`
- DTO: `product/dto/ProductCreateRequest.java`, `ProductResponse.java` 등
