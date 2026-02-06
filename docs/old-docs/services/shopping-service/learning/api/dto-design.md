# Request/Response DTO 설계

## 개요

DTO(Data Transfer Object)는 계층 간 데이터 전송을 위한 객체입니다. Portal Universe는 Java Record를 활용하여 불변성과 간결함을 추구합니다.

## DTO 설계 원칙

### 1. Request/Response 분리

```java
// Request DTO
public record ProductCreateRequest(...) {}
public record ProductUpdateRequest(...) {}

// Response DTO
public record ProductResponse(...) {}
public record ProductListResponse(...) {}
```

### 2. Entity와 분리

```
Client <--DTO--> Controller <--DTO--> Service <--Entity--> Repository
```

- **Request DTO**: 클라이언트 입력 검증 및 변환
- **Response DTO**: 노출할 필드만 선택적으로 포함
- **Entity**: 비즈니스 로직과 영속성 담당

## Java Record 활용

### 기본 Record DTO

```java
/**
 * 주문 생성 요청 DTO
 */
public record CreateOrderRequest(
    @NotNull(message = "Shipping address is required")
    @Valid
    AddressRequest shippingAddress,

    Long userCouponId  // Optional
) {}
```

### Record의 장점

1. **불변성**: final 필드, setter 없음
2. **간결함**: getter, equals, hashCode, toString 자동 생성
3. **명확함**: 데이터 캐리어 의도가 명확함

## Request DTO 패턴

### AddressRequest

```java
/**
 * 주소 요청 DTO
 */
public record AddressRequest(
    @NotBlank(message = "Receiver name is required")
    @Size(max = 100, message = "Receiver name must be at most 100 characters")
    String receiverName,

    @NotBlank(message = "Receiver phone is required")
    @Size(max = 20, message = "Receiver phone must be at most 20 characters")
    String receiverPhone,

    @NotBlank(message = "Zip code is required")
    @Size(max = 10, message = "Zip code must be at most 10 characters")
    String zipCode,

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address must be at most 255 characters")
    String address1,

    @Size(max = 255, message = "Detail address must be at most 255 characters")
    String address2  // Optional
) {
    /**
     * Entity로 변환
     */
    public Address toEntity() {
        return Address.builder()
            .receiverName(receiverName)
            .receiverPhone(receiverPhone)
            .zipCode(zipCode)
            .address1(address1)
            .address2(address2)
            .build();
    }
}
```

### 복잡한 Request DTO

```java
public record ProductCreateRequest(
    @NotBlank(message = "상품명은 필수입니다")
    @Size(min = 2, max = 100, message = "상품명은 2-100자 사이여야 합니다")
    String name,

    @Size(max = 2000, message = "설명은 2000자 이내여야 합니다")
    String description,

    @NotNull(message = "가격은 필수입니다")
    @Positive(message = "가격은 양수여야 합니다")
    BigDecimal price,

    @NotNull(message = "재고 수량은 필수입니다")
    @PositiveOrZero(message = "재고 수량은 0 이상이어야 합니다")
    Integer stock,

    @NotNull(message = "카테고리는 필수입니다")
    Long categoryId,

    List<String> tags,

    List<AttributeRequest> attributes
) {
    public Product toEntity(Category category) {
        return Product.builder()
            .name(name)
            .description(description)
            .price(price)
            .stock(stock)
            .category(category)
            .tags(tags != null ? tags : List.of())
            .build();
    }
}
```

## Response DTO 패턴

### OrderResponse

```java
/**
 * 주문 조회 응답 DTO
 */
public record OrderResponse(
    Long id,
    String orderNumber,
    String userId,
    OrderStatus status,
    String statusDescription,
    List<OrderItemResponse> items,
    int itemCount,
    int totalQuantity,
    BigDecimal totalAmount,
    BigDecimal discountAmount,
    BigDecimal finalAmount,
    Long appliedUserCouponId,
    AddressResponse shippingAddress,
    String cancelReason,
    LocalDateTime cancelledAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    /**
     * Entity에서 변환
     */
    public static OrderResponse from(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
            .map(OrderItemResponse::from)
            .toList();

        AddressResponse addressResponse = null;
        if (order.getShippingAddress() != null) {
            addressResponse = AddressResponse.from(order.getShippingAddress());
        }

        return new OrderResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getUserId(),
            order.getStatus(),
            order.getStatus().getDescription(),
            itemResponses,
            order.getItems().size(),
            order.getTotalQuantity(),
            order.getTotalAmount(),
            order.getDiscountAmount(),
            order.getFinalAmount(),
            order.getAppliedUserCouponId(),
            addressResponse,
            order.getCancelReason(),
            order.getCancelledAt(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }
}
```

### 중첩 Response DTO

```java
public record OrderItemResponse(
    Long id,
    Long productId,
    String productName,
    int quantity,
    BigDecimal price,
    BigDecimal subtotal
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
            item.getId(),
            item.getProductId(),
            item.getProductName(),
            item.getQuantity(),
            item.getPrice(),
            item.getSubtotal()
        );
    }
}

public record AddressResponse(
    String receiverName,
    String receiverPhone,
    String zipCode,
    String address1,
    String address2
) {
    public static AddressResponse from(Address address) {
        return new AddressResponse(
            address.getReceiverName(),
            address.getReceiverPhone(),
            address.getZipCode(),
            address.getAddress1(),
            address.getAddress2()
        );
    }
}
```

## 페이징 Response

### Page 응답 래핑

```java
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
        );
    }
}
```

### 사용 예시

```java
@GetMapping
public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> list(Pageable pageable) {
    Page<ProductResponse> products = productService.list(pageable);
    return ResponseEntity.ok(
        ApiResponse.success(PageResponse.from(products))
    );
}
```

## DTO 변환 패턴

### 1. 정적 팩토리 메서드

```java
public record ProductResponse(...) {
    // Entity -> DTO
    public static ProductResponse from(Product product) {
        return new ProductResponse(...);
    }

    // 여러 Entity -> DTO
    public static ProductResponse from(Product product, List<Review> reviews) {
        return new ProductResponse(
            ...,
            reviews.stream().map(ReviewResponse::from).toList()
        );
    }
}
```

### 2. toEntity 메서드

```java
public record ProductCreateRequest(...) {
    public Product toEntity() {
        return Product.builder()
            .name(name)
            .price(price)
            .build();
    }

    public Product toEntity(Category category) {
        return Product.builder()
            .name(name)
            .price(price)
            .category(category)
            .build();
    }
}
```

### 3. MapStruct 사용 (대규모 프로젝트)

```java
@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductResponse toResponse(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Product toEntity(ProductCreateRequest request);
}
```

## 조건부 필드

### @JsonInclude

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductResponse(
    Long id,
    String name,
    BigDecimal price,
    String description,      // null이면 제외
    List<String> tags,       // null이면 제외
    DiscountInfo discount    // null이면 제외
) {}
```

### @JsonView (다양한 뷰)

```java
public class Views {
    public static class Summary {}
    public static class Detail extends Summary {}
}

public record ProductResponse(
    @JsonView(Views.Summary.class)
    Long id,

    @JsonView(Views.Summary.class)
    String name,

    @JsonView(Views.Summary.class)
    BigDecimal price,

    @JsonView(Views.Detail.class)
    String description,

    @JsonView(Views.Detail.class)
    List<ReviewResponse> reviews
) {}

// Controller
@GetMapping
@JsonView(Views.Summary.class)
public ResponseEntity<ApiResponse<List<ProductResponse>>> list() { ... }

@GetMapping("/{id}")
@JsonView(Views.Detail.class)
public ResponseEntity<ApiResponse<ProductResponse>> detail() { ... }
```

## Best Practices

### 1. 명명 규칙

| 용도 | 네이밍 패턴 | 예시 |
|------|------------|------|
| 생성 요청 | Create{Entity}Request | CreateOrderRequest |
| 수정 요청 | Update{Entity}Request | UpdateProductRequest |
| 조회 응답 | {Entity}Response | OrderResponse |
| 목록 응답 | {Entity}ListResponse | ProductListResponse |

### 2. 불변성 유지

- Record 사용
- List는 `List.of()` 또는 `Collections.unmodifiableList()` 사용
- 방어적 복사

### 3. Validation은 Request DTO에

```java
public record CreateOrderRequest(
    @NotNull @Valid AddressRequest shippingAddress,
    @Size(max = 500) String memo
) {}
```

### 4. 민감 정보 제외

```java
// UserResponse에서 password 제외
public record UserResponse(
    Long id,
    String email,
    String name
    // password는 포함하지 않음
) {}
```

### 5. API 버전별 DTO

```java
// V1
public record ProductResponseV1(Long id, String name, BigDecimal price) {}

// V2
public record ProductResponseV2(Long id, String name, PriceInfo price) {}
```

## 관련 문서

- [Validation](./validation.md) - Bean Validation
- [RESTful API Design](./restful-api-design.md) - REST 설계 원칙
- [API Versioning](./api-versioning.md) - 버저닝 전략
