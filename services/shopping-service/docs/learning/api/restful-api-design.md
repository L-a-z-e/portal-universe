# RESTful API 설계 원칙

## 개요

Portal Universe는 RESTful 원칙을 따르는 API를 설계합니다. 이 문서에서는 REST 설계 원칙과 Shopping Service의 적용 사례를 설명합니다.

## REST 핵심 원칙

### 1. Resource-Oriented Design

API는 리소스(Resource)를 중심으로 설계합니다:

```
# 좋은 예시 (명사 사용)
GET    /api/v1/products
GET    /api/v1/products/{id}
POST   /api/v1/products
PUT    /api/v1/products/{id}
DELETE /api/v1/products/{id}

# 나쁜 예시 (동사 사용)
GET    /api/v1/getProducts
POST   /api/v1/createProduct
POST   /api/v1/deleteProduct
```

### 2. HTTP Methods

| Method | 용도 | 멱등성 | 안전성 |
|--------|------|--------|--------|
| GET | 리소스 조회 | O | O |
| POST | 리소스 생성 | X | X |
| PUT | 리소스 전체 수정 | O | X |
| PATCH | 리소스 부분 수정 | X | X |
| DELETE | 리소스 삭제 | O | X |

### 3. HTTP Status Codes

| 코드 | 의미 | 사용 케이스 |
|------|------|------------|
| 200 OK | 성공 | 조회, 수정 성공 |
| 201 Created | 생성됨 | POST로 리소스 생성 |
| 204 No Content | 내용 없음 | DELETE 성공 |
| 400 Bad Request | 잘못된 요청 | 유효성 검증 실패 |
| 401 Unauthorized | 인증 필요 | 로그인 필요 |
| 403 Forbidden | 권한 없음 | 접근 권한 부족 |
| 404 Not Found | 없음 | 리소스 없음 |
| 409 Conflict | 충돌 | 중복 데이터 |
| 500 Internal Server Error | 서버 오류 | 예외 발생 |

## URL 설계 가이드

### 명명 규칙

1. **복수형 사용**: `/products`, `/orders`, `/users`
2. **소문자와 하이픈**: `/order-items` (not `orderItems`)
3. **리소스 계층 표현**: `/orders/{orderId}/items`
4. **쿼리 파라미터로 필터링**: `/products?category=electronics&minPrice=10000`

### Shopping Service URL 구조

```
/api/v1/products                    # 상품
/api/v1/products/{id}
/api/v1/products/{id}/reviews

/api/v1/carts                       # 장바구니
/api/v1/carts/items
/api/v1/carts/items/{itemId}

/api/v1/orders                      # 주문
/api/v1/orders/{orderNumber}
/api/v1/orders/{orderNumber}/cancel

/api/v1/payments                    # 결제
/api/v1/payments/{paymentNumber}

/api/v1/search/products             # 검색
/api/v1/search/suggest
/api/v1/search/popular
```

## Controller 패턴

### 기본 CRUD Controller

```java
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 상품 목록 조회 (페이징)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort.split(",")[0]));
        Page<ProductResponse> products = productService.list(pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * 상품 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> get(@PathVariable Long id) {
        ProductResponse product = productService.get(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    /**
     * 상품 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @Valid @RequestBody ProductCreateRequest request
    ) {
        ProductResponse product = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(product));
    }

    /**
     * 상품 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        ProductResponse product = productService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    /**
     * 상품 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### 중첩 리소스 Controller

```java
@RestController
@RequestMapping("/api/v1/orders/{orderNumber}")
@RequiredArgsConstructor
public class OrderItemController {

    private final OrderItemService orderItemService;

    /**
     * 주문 항목 목록 조회
     */
    @GetMapping("/items")
    public ResponseEntity<ApiResponse<List<OrderItemResponse>>> listItems(
            @PathVariable String orderNumber
    ) {
        List<OrderItemResponse> items = orderItemService.listByOrderNumber(orderNumber);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    /**
     * 주문 항목 상세 조회
     */
    @GetMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<OrderItemResponse>> getItem(
            @PathVariable String orderNumber,
            @PathVariable Long itemId
    ) {
        OrderItemResponse item = orderItemService.get(orderNumber, itemId);
        return ResponseEntity.ok(ApiResponse.success(item));
    }
}
```

## 응답 구조

### 통일된 ApiResponse

```java
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final ErrorResponse error;

    // 성공 응답
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    // 실패 응답
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorResponse(code, message));
    }
}
```

### 성공 응답 예시

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "무선 이어폰",
    "price": 89000,
    "stock": 100
  }
}
```

### 실패 응답 예시

```json
{
  "success": false,
  "error": {
    "code": "S001",
    "message": "Product not found"
  }
}
```

### 페이징 응답 구조

```json
{
  "success": true,
  "data": {
    "content": [
      { "id": 1, "name": "상품1" },
      { "id": 2, "name": "상품2" }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "first": true,
    "last": false
  }
}
```

## Query Parameters

### 페이징

```
GET /api/v1/products?page=0&size=20&sort=createdAt,desc
```

### 필터링

```
GET /api/v1/products?category=electronics&minPrice=10000&maxPrice=50000
```

### 검색

```
GET /api/v1/products?q=이어폰
```

### 필드 선택 (Sparse Fieldsets)

```
GET /api/v1/products?fields=id,name,price
```

### 확장 (Embedding)

```
GET /api/v1/orders?expand=items,payment
```

## HATEOAS (선택적)

응답에 관련 링크 포함:

```json
{
  "success": true,
  "data": {
    "id": 1,
    "orderNumber": "ORD-20250122-001",
    "status": "PENDING",
    "_links": {
      "self": { "href": "/api/v1/orders/ORD-20250122-001" },
      "items": { "href": "/api/v1/orders/ORD-20250122-001/items" },
      "cancel": { "href": "/api/v1/orders/ORD-20250122-001/cancel", "method": "POST" },
      "payment": { "href": "/api/v1/payments?orderNumber=ORD-20250122-001" }
    }
  }
}
```

## 액션 엔드포인트

CRUD로 표현하기 어려운 액션:

```java
// 주문 취소
@PostMapping("/orders/{orderNumber}/cancel")
public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
        @PathVariable String orderNumber,
        @Valid @RequestBody CancelOrderRequest request
) {
    OrderResponse order = orderService.cancel(orderNumber, request);
    return ResponseEntity.ok(ApiResponse.success(order));
}

// 장바구니 비우기
@DeleteMapping("/carts/items")
public ResponseEntity<Void> clearCart() {
    cartService.clear();
    return ResponseEntity.noContent().build();
}

// 검색어 저장
@PostMapping("/search/recent")
public ResponseEntity<Void> saveRecentKeyword(@RequestBody KeywordRequest request) {
    searchService.saveRecentKeyword(request.keyword());
    return ResponseEntity.status(HttpStatus.CREATED).build();
}
```

## Best Practices

### 1. 일관성

- 모든 API에 동일한 응답 구조 사용
- 네이밍 컨벤션 일관되게 적용
- 에러 코드 체계화

### 2. 자기 설명적

- 명확한 URL 구조
- 적절한 HTTP 메서드 사용
- 상세한 에러 메시지

### 3. 보안

- HTTPS 필수
- 인증/인가 적절히 적용
- 민감 정보 노출 금지

### 4. 버전 관리

- URL Path 버전: `/api/v1/products`
- Header 버전: `Accept: application/vnd.portal.v1+json`

## Anti-Patterns

### 피해야 할 패턴

```
# 동사 사용
GET /api/getProducts
POST /api/createOrder

# 복잡한 URL
GET /api/products/category/electronics/brand/samsung/price/under/100000

# 불필요한 중첩
GET /api/users/1/orders/2/items/3/reviews/4

# HTTP 메서드 무시
POST /api/products/delete/1
GET /api/orders/create
```

## 관련 문서

- [API Versioning](./api-versioning.md) - 버저닝 전략
- [DTO Design](./dto-design.md) - Request/Response DTO
- [Validation](./validation.md) - Bean Validation
- [Error Handling](./error-handling.md) - 예외 처리
