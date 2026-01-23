# Cart Domain

## 1. 개요

Cart 도메인은 사용자의 장바구니를 관리하며, 상품 추가/수정/삭제 및 체크아웃 기능을 제공합니다.

## 2. Entity 구조

### Cart Entity (Aggregate Root)

```java
@Entity
@Table(name = "carts", indexes = {
    @Index(name = "idx_cart_user_status", columnList = "user_id, status")
})
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;                // 사용자 ID

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CartStatus status;            // 장바구니 상태

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();  // 장바구니 항목

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

### CartItem Entity

```java
@Entity
@Table(name = "cart_items", indexes = {
    @Index(name = "idx_cart_item_cart_id", columnList = "cart_id"),
    @Index(name = "idx_cart_item_product_id", columnList = "product_id")
})
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;                    // 소속 장바구니

    @Column(name = "product_id", nullable = false)
    private Long productId;               // 상품 ID

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;           // 상품명 (스냅샷)

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;             // 단가 (스냅샷)

    @Column(name = "quantity", nullable = false)
    private Integer quantity;             // 수량

    @CreatedDate
    private LocalDateTime addedAt;        // 추가 시간
}
```

## 3. 장바구니 상태 (CartStatus)

```java
public enum CartStatus {
    ACTIVE("활성"),        // 상품 추가/수정/삭제 가능
    CHECKED_OUT("체크아웃"), // 주문 생성됨
    ABANDONED("포기"),      // 일정 기간 미사용
    MERGED("병합")          // 다른 장바구니와 병합됨
}
```

### 상태 전이 다이어그램

```
┌──────────┐     checkout()    ┌─────────────┐
│  ACTIVE  │──────────────────>│ CHECKED_OUT │
└────┬─────┘                   └─────────────┘
     │
     │ 일정 기간 미사용
     v
┌────────────┐
│ ABANDONED  │
└────────────┘

┌──────────┐     merge()       ┌──────────┐
│  ACTIVE  │──────────────────>│  MERGED  │
└──────────┘                   └──────────┘
```

## 4. 스냅샷 패턴

CartItem은 상품 정보를 **스냅샷**으로 저장합니다:

```java
// 장바구니에 추가 시점의 가격 저장
CartItem cartItem = CartItem.builder()
    .cart(this)
    .productId(productId)
    .productName(productName)  // 스냅샷
    .price(price)              // 스냅샷
    .quantity(quantity)
    .build();
```

**이유:**
- 상품 가격이 변경되어도 장바구니에 담긴 가격은 유지
- 사용자가 담았던 시점의 가격으로 구매 결정 가능
- 필요시 `updateProductInfo()` 메서드로 관리자가 갱신 가능

## 5. 비즈니스 메서드

### Cart 메서드

```java
// 상품 추가
public CartItem addItem(Long productId, String productName, BigDecimal price, int quantity) {
    validateActive();

    // 동일 상품 중복 체크
    Optional<CartItem> existingItem = findItemByProductId(productId);
    if (existingItem.isPresent()) {
        throw new CustomBusinessException(ShoppingErrorCode.CART_ITEM_ALREADY_EXISTS);
    }

    CartItem cartItem = CartItem.builder()
        .cart(this)
        .productId(productId)
        .productName(productName)
        .price(price)
        .quantity(quantity)
        .build();

    this.items.add(cartItem);
    return cartItem;
}

// 수량 변경
public CartItem updateItemQuantity(Long itemId, int newQuantity) {
    validateActive();

    CartItem item = findItemById(itemId)
        .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.CART_ITEM_NOT_FOUND));

    item.updateQuantity(newQuantity);
    return item;
}

// 항목 제거
public void removeItem(Long itemId) {
    validateActive();

    CartItem item = findItemById(itemId)
        .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.CART_ITEM_NOT_FOUND));

    this.items.remove(item);
}

// 장바구니 비우기
public void clear() {
    validateActive();
    this.items.clear();
}

// 체크아웃
public void checkout() {
    validateActive();

    if (this.items.isEmpty()) {
        throw new CustomBusinessException(ShoppingErrorCode.CART_EMPTY);
    }

    this.status = CartStatus.CHECKED_OUT;
}
```

### CartItem 메서드

```java
// 수량 변경
public void updateQuantity(int newQuantity) {
    validateQuantity(newQuantity);
    this.quantity = newQuantity;
}

// 수량 증가
public void increaseQuantity(int additionalQuantity) {
    if (additionalQuantity <= 0) {
        throw new CustomBusinessException(ShoppingErrorCode.INVALID_CART_ITEM_QUANTITY);
    }
    this.quantity += additionalQuantity;
}

// 소계 계산
public BigDecimal getSubtotal() {
    return price.multiply(BigDecimal.valueOf(quantity));
}

// 상품 정보 갱신 (관리자/시스템)
public void updateProductInfo(BigDecimal newPrice, String newProductName) {
    if (newPrice != null && newPrice.compareTo(BigDecimal.ZERO) > 0) {
        this.price = newPrice;
    }
    if (newProductName != null && !newProductName.isBlank()) {
        this.productName = newProductName;
    }
}
```

## 6. 계산 메서드

```java
// 총액 계산
public BigDecimal getTotalAmount() {
    return items.stream()
        .map(CartItem::getSubtotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
}

// 항목 수
public int getItemCount() {
    return items.size();
}

// 총 수량 (모든 항목의 수량 합계)
public int getTotalQuantity() {
    return items.stream()
        .mapToInt(CartItem::getQuantity)
        .sum();
}
```

## 7. 설계 원칙

### Aggregate Root 패턴

Cart는 CartItem의 **Aggregate Root**입니다:

```java
// CartItem은 Cart를 통해서만 접근/수정
cart.addItem(productId, productName, price, quantity);
cart.updateItemQuantity(itemId, newQuantity);
cart.removeItem(itemId);

// CartItem 직접 생성/수정 X
// CartItemRepository 직접 사용 X
```

### Cascade 설정

```java
@OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
private List<CartItem> items = new ArrayList<>();
```

- `CascadeType.ALL`: Cart 저장 시 CartItem도 함께 저장
- `orphanRemoval = true`: Cart에서 제거된 CartItem은 자동 삭제

## 8. Error Codes

| 코드 | 설명 |
|------|------|
| `S101` | CART_NOT_FOUND - 장바구니를 찾을 수 없음 |
| `S102` | CART_ITEM_NOT_FOUND - 장바구니 항목을 찾을 수 없음 |
| `S103` | CART_ALREADY_CHECKED_OUT - 이미 체크아웃된 장바구니 |
| `S104` | CART_EMPTY - 빈 장바구니 |
| `S105` | CART_ITEM_QUANTITY_EXCEEDED - 재고 초과 수량 |
| `S106` | CART_ITEM_ALREADY_EXISTS - 이미 존재하는 상품 |
| `S107` | INVALID_CART_ITEM_QUANTITY - 유효하지 않은 수량 |

## 9. 사용자당 하나의 활성 장바구니

```java
// Repository 쿼리
Optional<Cart> findByUserIdAndStatus(String userId, CartStatus status);

// Service에서 사용
Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
    .orElseGet(() -> createNewCart(userId));
```

## 10. 소스 위치

- Entity: `cart/domain/Cart.java`, `CartItem.java`
- Enum: `cart/domain/CartStatus.java`
- Repository: `cart/repository/CartRepository.java`
- Service: `cart/service/CartService.java`
- DTO: `cart/dto/AddCartItemRequest.java`, `CartResponse.java` 등
