# Admin 상품 관리 구현 학습 가이드

> 이 문서는 Portal Universe 프로젝트의 Admin 상품 관리 기능 구현에 사용된 기술과 패턴을 학습하기 위한 가이드입니다. 프로젝트에 새로 참여하는 개발자나 아키텍처 학습을 원하는 학습자를 대상으로 합니다.

## 📖 목차

1. [프로젝트 개요](#프로젝트-개요)
2. [Backend 학습 포인트](#backend-학습-포인트)
3. [Frontend 학습 포인트](#frontend-학습-포인트)
4. [아키텍처 패턴](#아키텍처-패턴)
5. [실습 과제](#실습-과제)
6. [참고 자료](#참고-자료)

---

## 프로젝트 개요

### Admin 기능의 위치

Portal Universe는 마이크로서비스 기반의 전자상거래 플랫폼입니다. Admin 상품 관리는 다음과 같이 아키텍처에 통합됩니다:

```
┌─────────────────────────────────────────────────────┐
│           Portal Shell (Host App - Vue)             │
│  (포트 30000, 인증 상태 및 라우팅 관리)            │
└──────────────────┬──────────────────────────────────┘
                   │
       ┌───────────┴──────────────┬──────────────┐
       │                          │              │
  ┌────▼─────┐        ┌─────────▼────┐    ┌────▼──────┐
  │   Blog    │        │  Shopping    │    │  Admin    │
  │ Frontend  │        │  Frontend    │    │  Panel    │
  │(Remote)   │        │  (Remote)    │    │(Remote)   │
  │포트30001  │        │  포트30002   │    │포트30002  │
  └───────────┘        └──────────────┘    └─────┬─────┘
                                                  │
                                    ┌─────────────▼─────────────┐
                                    │   API Gateway (8080)      │
                                    │  - JWT 검증               │
                                    │  - 라우팅                 │
                                    │  - CORS                   │
                                    └────────────┬──────────────┘
                                                 │
                                     ┌───────────▼───────────┐
                                     │  Shopping Service     │
                                     │  (포트 8083)          │
                                     │ AdminProductController│
                                     └───────────────────────┘
```

### Admin 기능의 특징

- **권한 기반 접근 제어**: `@PreAuthorize("hasRole('ADMIN')")`로 ADMIN 역할 검증
- **Frontend Route Guard**: React Router와 RequireRole 컴포넌트로 클라이언트 보호
- **선언적 유효성 검사**: Jakarta Validation으로 입력값 검증
- **트랜잭션 관리**: `@Transactional`로 데이터 일관성 보장

---

## Backend 학습 포인트

### 1. Spring Security + Method Security (@PreAuthorize)

#### 개념: 메서드 수준의 접근 제어

Spring Security의 `@PreAuthorize` 어노테이션을 사용하여 메서드 실행 전 권한을 확인합니다.

#### 구현 코드

```java
// services/shopping-service/src/main/java/.../controller/AdminProductController.java
@RestController
@RequestMapping("/api/shopping/admin/products")
@PreAuthorize("hasRole('ADMIN')")  // 클래스 레벨: 모든 메서드에 적용
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    /**
     * 클래스 레벨 @PreAuthorize 때문에 ADMIN 권한이 필요함
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody AdminProductRequest request) {
        ProductResponse response = productService.createProductAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody AdminProductRequest request) {
        ProductResponse response = productService.updateProductAdmin(productId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
```

#### Why? 왜 이 방식을 사용하는가?

1. **선언적 보안**: 복잡한 if 조건문 없이 어노테이션으로 권한 검증
2. **AOP 기반**: Spring이 프록시를 통해 자동으로 권한 체크
3. **중복 제거**: 클래스 레벨 적용으로 모든 메서드에 일괄 적용 가능

#### @PreAuthorize 표현식 이해

```java
@PreAuthorize("hasRole('ADMIN')")          // ROLE_ADMIN 권한 필수
@PreAuthorize("hasAnyRole('ADMIN','MOD')")  // ADMIN 또는 MOD 권한
@PreAuthorize("#userId == authentication.principal.id")  // SpEL을 통한 동적 검증
@PreAuthorize("@authService.canEdit(#id)")  // 빈의 메서드 호출
```

#### Role 네이밍 규칙

Spring Security는 자동으로 `ROLE_` 접두사를 추가합니다:
- `hasRole('ADMIN')` → 실제 권한: `ROLE_ADMIN`
- `hasRole('USER')` → 실제 권한: `ROLE_USER`

#### 주의사항: Spring WebFlux에서의 @PreAuthorize

API Gateway는 Spring Cloud Gateway (WebFlux)를 사용하므로, 일반적인 방식이 작동하지 않을 수 있습니다.
대신 Gateway에서 JWT 토큰을 검증하고 권한 정보를 헤더에 추가합니다.

---

### 2. Jakarta Validation - 선언적 입력값 검증

#### 개념: 어노테이션 기반 유효성 검사

컨트롤러에서 수동으로 검증하는 대신, DTO에 선언적으로 검증 규칙을 정의합니다.

#### 구현 코드

```java
// services/shopping-service/src/main/java/.../dto/AdminProductRequest.java
public record AdminProductRequest(
        @NotBlank(message = "Product name is required")
        @Size(min = 1, max = 200, message = "Product name must be between 1 and 200 characters")
        String name,

        @Size(max = 2000, message = "Product description must not exceed 2000 characters")
        String description,

        @NotNull(message = "Product price is required")
        @Positive(message = "Product price must be greater than 0")
        Double price,

        @NotNull(message = "Product stock is required")
        @Min(value = 0, message = "Product stock must be non-negative")
        Integer stock
) {
}
```

#### 컨트롤러에서 @Valid 사용

```java
@PostMapping
public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
        @Valid @RequestBody AdminProductRequest request) {  // @Valid로 자동 검증
    // 검증 실패 시 MethodArgumentNotValidException 발생
    // GlobalExceptionHandler가 400 Bad Request로 처리
    ProductResponse response = productService.createProductAdmin(request);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response));
}
```

#### 자주 사용되는 Validation Annotations

| 어노테이션 | 용도 | 예시 |
|-----------|------|------|
| `@NotNull` | null 허용 안 함 | `@NotNull Double price` |
| `@NotBlank` | null, 공백 허용 안 함 (String만) | `@NotBlank String name` |
| `@Size` | 크기 범위 검증 | `@Size(min=1, max=200)` |
| `@Positive` | 양수만 허용 | `@Positive Double price` |
| `@Min / @Max` | 최소/최대값 | `@Min(0) Integer stock` |
| `@Email` | 이메일 형식 | `@Email String email` |
| `@Pattern` | 정규식 패턴 | `@Pattern(regexp="\\d{3}-\\d{4}")` |

#### Why? 선언적 검증의 장점

1. **코드 간결성**: 검증 로직을 DTO에 집중
2. **재사용성**: 같은 DTO를 사용하는 모든 엔드포인트에 적용
3. **일관성**: 모든 검증 에러가 동일한 형식으로 처리됨
4. **테스트 용이**: 검증을 독립적으로 테스트 가능

#### 검증 실패 시 응답 예시

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "C001",
    "message": "Validation failed",
    "details": [
      {
        "field": "name",
        "message": "Product name must be between 1 and 200 characters"
      },
      {
        "field": "price",
        "message": "Product price must be greater than 0"
      }
    ]
  }
}
```

---

### 3. 트랜잭션 관리 (@Transactional)

#### 개념: 데이터 일관성을 보장하는 원자적 연산

`@Transactional` 어노테이션으로 메서드를 트랜잭션으로 감싸서 데이터 일관성을 보장합니다.

#### 구현 코드

```java
// services/shopping-service/src/main/java/.../service/ProductServiceImpl.java
@Override
@Transactional  // 메서드 시작 시 트랜잭션 시작, 정상 종료 시 자동 커밋
public ProductResponse createProductAdmin(AdminProductRequest request) {
    // 1. 중복 검사 (비즈니스 규칙)
    if (productRepository.existsByName(request.name())) {
        throw new CustomBusinessException(ShoppingErrorCode.PRODUCT_NAME_ALREADY_EXISTS);
    }

    // 2. 상품 생성
    Product newProduct = Product.builder()
            .name(request.name())
            .description(request.description())
            .price(request.price())
            .stock(request.stock())
            .build();

    // 3. 저장
    Product savedProduct = productRepository.save(newProduct);
    return convertToResponse(savedProduct);
    // 트랜잭션 커밋: 모든 데이터 변경사항이 DB에 반영됨
}

@Override
@Transactional
public ProductResponse updateProductAdmin(Long productId, AdminProductRequest request) {
    // 1. 상품 조회
    Product product = productRepository.findById(productId)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND));

    // 2. 상품명 변경 시에만 중복 체크 (N+1 쿼리 방지)
    if (!product.getName().equals(request.name())) {
        if (productRepository.existsByName(request.name())) {
            throw new CustomBusinessException(ShoppingErrorCode.PRODUCT_NAME_ALREADY_EXISTS);
        }
    }

    // 3. 상품 정보 수정
    product.update(
            request.name(),
            request.description(),
            request.price(),
            request.stock()
    );

    // Dirty Checking: JPA가 자동으로 변경사항을 감지하여 UPDATE 쿼리 실행
    // 명시적 save() 호출 불필요 (선택사항)
    return convertToResponse(product);
}

@Override
@Transactional
public ProductResponse updateProductStock(Long productId, StockUpdateRequest request) {
    // 재고만 업데이트하는 경우 (PATCH)
    Product product = productRepository.findById(productId)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND));

    product.update(
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            request.stock()  // 재고만 변경
    );

    return convertToResponse(product);
}
```

#### Dirty Checking: JPA의 자동 변경 감지

트랜잭션 내에서 엔티티의 상태가 변경되면, 트랜잭션 종료 시 자동으로 UPDATE 쿼리가 실행됩니다:

```java
@Transactional
public void updateProduct(Long id) {
    Product product = productRepository.findById(id).get();  // 1. SELECT
    product.update(...);  // 2. 엔티티 상태 변경 (메모리)
    // 3. 트랜잭션 종료 시 UPDATE 쿼리 자동 실행
}

// 실행되는 SQL
// SELECT * FROM products WHERE id = ?;
// UPDATE products SET name = ?, price = ? WHERE id = ?;
```

#### @Transactional 속성 이해

```java
@Transactional(
    readOnly = false,        // 읽기 전용 여부 (성능 최적화)
    isolation = Isolation.DEFAULT,  // 트랜잭션 격리 수준
    propagation = Propagation.REQUIRED,  // 기존 트랜잭션과의 관계
    timeout = -1             // 타임아웃 시간 (-1 = 무제한)
)
public void updateProduct(Long id, AdminProductRequest request) { ... }
```

| 속성 | 설명 | 사용 사례 |
|------|------|---------|
| `readOnly=true` | SELECT만 실행, 쓰기 최적화 비활성화 | 데이터 조회 메서드 |
| `readOnly=false` | INSERT/UPDATE/DELETE 가능 | 데이터 변경 메서드 |
| `isolation` | 동시성 제어 수준 | 동시 접근이 많은 경우 조정 |
| `propagation=REQUIRED` | 기존 트랜잭션 있으면 참여, 없으면 생성 | 기본값, 대부분의 경우 사용 |

#### Why? 트랜잭션이 필요한 이유

1. **원자성 (Atomicity)**: 모두 성공하거나 모두 실패 (Half-done 방지)
2. **일관성 (Consistency)**: 데이터 규칙 준수 보장
3. **격리성 (Isolation)**: 동시 요청 간 간섭 방지 (Dirty Read 방지)
4. **지속성 (Durability)**: 커밋된 데이터는 永続

#### Race Condition 예시

```
// 트랜잭션 없는 위험한 코드
Product product = getProduct(1);
if (product.getStock() > 0) {  // 조회
    product.setStock(product.getStock() - 1);  // 수정
    save(product);  // 저장
}

// 시나리오: 2명이 동시에 마지막 1개 상품 구매
Thread 1: stock=1 → stock=0 → save (성공)
Thread 2: stock=1 → stock=0 → save (성공)
// 결과: 2명이 구매했는데 재고만 -1 (오류!)

// @Transactional로 해결
@Transactional
public void buyProduct(Long productId) {
    Product product = findById(productId);  // 행 잠금
    if (product.getStock() > 0) {
        product.setStock(product.getStock() - 1);
        // 트랜잭션 커밋 시까지 다른 스레드 대기
    }
}
```

---

### 4. 에러 코드 패턴 (ErrorCode + CustomBusinessException)

#### 개념: 통일된 에러 처리 아키텍처

서비스별 에러코드를 Enum으로 정의하고, 모든 비즈니스 예외를 `CustomBusinessException`으로 발생시킵니다.

#### 에러코드 정의

```java
// services/shopping-service/src/main/java/.../exception/ShoppingErrorCode.java
public enum ShoppingErrorCode implements ErrorCode {

    // Product Errors (S0XX)
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "Product not found"),
    PRODUCT_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "S008", "Product name already exists"),
    INVALID_PRODUCT_PRICE(HttpStatus.BAD_REQUEST, "S004", "Product price must be greater than 0"),

    // ... 다른 에러코드들

    private final HttpStatus status;
    private final String code;
    private final String message;

    ShoppingErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
```

#### 에러코드 접두사 규칙

서비스별로 고유한 접두사를 사용하여 에러 출처를 즉시 파악:

| 서비스 | 접두사 | 범위 | 예시 |
|--------|--------|------|------|
| Common | C | C001-C999 | C001: Invalid input |
| Auth | A | A001-A999 | A001: User not found |
| Blog | B | B001-B999 | B001: Post not found |
| Shopping | S | S001-S999 | S001: Product not found |

쇼핑 서비스 내 세부 분류:
- S0XX: Product (S001-S010)
- S1XX: Cart (S101-S110)
- S2XX: Order (S201-S220)
- S3XX: Payment (S301-S315)

#### 예외 발생

```java
// Service에서 비즈니스 규칙 위반 시 예외 발생
@Transactional
public ProductResponse createProductAdmin(AdminProductRequest request) {
    // 중복 검사
    if (productRepository.existsByName(request.name())) {
        // CustomBusinessException 발생
        throw new CustomBusinessException(ShoppingErrorCode.PRODUCT_NAME_ALREADY_EXISTS);
    }

    // 계속 진행...
}

// Repository에서 조회 실패 시 예외 발생
public ProductResponse getProductById(Long id) {
    Product product = productRepository.findById(id)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND));
    return convertToResponse(product);
}
```

#### GlobalExceptionHandler에서 처리

```java
// common-library/src/main/java/.../exception/GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomBusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(
            CustomBusinessException e) {
        ErrorCode errorCode = e.getErrorCode();

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(
                    errorCode.getCode(),
                    errorCode.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
            MethodArgumentNotValidException e) {
        // 검증 실패 에러 처리
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("C001", "Validation failed"));
    }
}
```

#### 응답 형식 (ApiResponse)

```json
// 성공
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Product Name",
    "price": 100.0,
    "stock": 50
  },
  "error": null
}

// 실패 - 비즈니스 규칙 위반 (S008: 상품명 중복)
{
  "success": false,
  "data": null,
  "error": {
    "code": "S008",
    "message": "Product name already exists"
  }
}

// 실패 - 검증 에러 (입력값 형식 오류)
{
  "success": false,
  "data": null,
  "error": {
    "code": "C001",
    "message": "Validation failed",
    "details": [
      {
        "field": "price",
        "message": "Product price must be greater than 0"
      }
    ]
  }
}
```

#### Why? 에러코드 패턴의 장점

1. **추적 용이**: 에러코드로 즉시 원인 파악 가능
2. **다국어 지원**: 클라이언트에서 에러코드 기반 메시지 표시 가능
3. **일관성**: 모든 에러가 동일한 형식으로 처리됨
4. **문서화**: 에러코드 목록만으로 API 에러 케이스 이해 가능

---

## Frontend 학습 포인트

### 1. React Query - 서버 상태 관리

#### 개념: 백엔드 데이터를 효율적으로 캐싱하고 동기화

React Query는 서버 상태(백엔드 데이터)와 클라이언트 상태(UI 상태)를 분리하여 관리합니다.

#### 기본 개념: Query vs Mutation

```typescript
// Query: 데이터 조회 (GET)
const { data, isLoading, error } = useQuery({
  queryKey: ['products'],
  queryFn: () => api.getProducts()
})

// Mutation: 데이터 변경 (POST, PUT, DELETE)
const { mutate, isPending } = useMutation({
  mutationFn: (data) => api.createProduct(data),
  onSuccess: () => { /* 성공 시 */ },
  onError: () => { /* 실패 시 */ }
})
```

#### Query Key와 캐싱 전략

```typescript
// frontend/shopping-frontend/src/hooks/useAdminProducts.ts

// Query Key 계층 구조 정의
export const adminProductKeys = {
  all: ['adminProducts'] as const,              // 최상위 키
  lists: () => [...adminProductKeys.all, 'list'] as const,  // 목록 관련
  list: (filters) => [...adminProductKeys.lists(), filters], // 필터별 목록
  details: () => [...adminProductKeys.all, 'detail'] as const,  // 상세 관련
  detail: (id) => [...adminProductKeys.details(), id],  // 특정 상품 상세
}

// 목록 조회 Hook
export const useAdminProducts = (filters: ProductFilters) => {
  return useQuery({
    queryKey: adminProductKeys.list(filters),  // 필터별로 캐시 분리
    queryFn: () => adminProductApi.getProducts(filters),
    staleTime: 5 * 60 * 1000,  // 5분: 캐시된 데이터가 신선한 것으로 간주
    gcTime: 30 * 60 * 1000,    // 30분: 메모리에서 제거될 때까지 대기
  })
}

// 상세 조회 Hook
export const useAdminProduct = (id: number) => {
  return useQuery({
    queryKey: adminProductKeys.detail(id),
    queryFn: () => adminProductApi.getProduct(id),
    enabled: !!id && id > 0,  // id가 유효할 때만 쿼리 실행
    staleTime: 1 * 60 * 1000,  // 1분 (자주 변경될 수 있음)
  })
}
```

| 용어 | 설명 | 예시 |
|------|------|------|
| `staleTime` | 캐시 데이터가 신선한 것으로 간주되는 시간 | 5분 |
| `gcTime` | 미사용 캐시 메모리 제거 시간 | 30분 |
| `enabled` | 쿼리 실행 여부를 제어하는 조건 | `!!id` |
| `refetchInterval` | 자동 재요청 주기 | 10000 (10초) |

#### Mutation과 캐시 무효화

```typescript
// 상품 생성 Hook
export const useCreateProduct = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: ProductFormData) => adminProductApi.createProduct(data),
    onSuccess: () => {
      // 목록 캐시 무효화 → 다시 조회
      queryClient.invalidateQueries({
        queryKey: adminProductKeys.lists()
      })
    },
  })
}

// 상품 수정 Hook
export const useUpdateProduct = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: ProductFormData }) =>
      adminProductApi.updateProduct(id, data),
    onSuccess: (_, variables) => {
      // 상세 정보 캐시 무효화
      queryClient.invalidateQueries({
        queryKey: adminProductKeys.detail(variables.id)
      })
      // 목록 캐시 무효화
      queryClient.invalidateQueries({
        queryKey: adminProductKeys.lists()
      })
    },
  })
}

// 상품 삭제 Hook
export const useDeleteProduct = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) => adminProductApi.deleteProduct(id),
    onSuccess: () => {
      // 목록 캐시 무효화
      queryClient.invalidateQueries({
        queryKey: adminProductKeys.lists()
      })
    },
  })
}
```

#### 컴포넌트에서 사용

```typescript
// frontend/shopping-frontend/src/pages/admin/AdminProductListPage.tsx
export const AdminProductListPage: React.FC = () => {
  const [filters, setFilters] = useState<ProductFilters>({
    page: 0,
    size: 10,
    keyword: '',
    sortBy: 'createdAt',
    sortOrder: 'desc'
  })

  // 목록 데이터 조회
  const { data, isLoading, error } = useAdminProducts(filters)

  // 삭제 함수
  const deleteMutation = useDeleteProduct()

  const handleDelete = async (id: number) => {
    try {
      // mutateAsync: 비동기 대기 가능
      await deleteMutation.mutateAsync(id)
      // 성공 시 자동으로 캐시 무효화 → 목록 재조회
    } catch (error) {
      console.error('Failed to delete product:', error)
    }
  }

  if (isLoading) return <LoadingSpinner />
  if (error) return <ErrorMessage error={error} />

  return (
    <table>
      {data?.data.content.map(product => (
        <tr key={product.id}>
          <td>{product.name}</td>
          <td>{product.price}</td>
          <td>
            <button onClick={() => handleDelete(product.id)}>Delete</button>
          </td>
        </tr>
      ))}
    </table>
  )
}
```

#### Why? React Query를 사용하는 이유

1. **자동 캐싱**: 중복 요청 방지, 네트워크 절약
2. **백그라운드 동기화**: `refetchInterval`로 실시간 데이터 업데이트
3. **낙관적 업데이트**: 응답 전에 UI 업데이트하여 사용성 향상
4. **상태 관리 단순화**: useReducer 없이도 복잡한 비동기 상태 관리

---

### 2. React Hook Form + Zod - 타입 안전 폼 검증

#### 개념: 선언적 스키마 기반 폼 유효성 검사

Zod로 검증 스키마를 정의하고, React Hook Form으로 효율적인 폼 상태 관리를 합니다.

#### 스키마 정의

```typescript
// frontend/shopping-frontend/src/pages/admin/AdminProductFormPage.tsx
import { z } from 'zod'

// Zod 스키마: 타입과 검증을 동시에 정의
const productFormSchema = z.object({
  name: z
    .string()
    .min(1, 'Product name is required')
    .max(200, 'Product name must be less than 200 characters'),

  description: z
    .string()
    .min(1, 'Description is required')
    .max(2000, 'Description must be less than 2000 characters'),

  price: z
    .number({ invalid_type_error: 'Price must be a number' })
    .min(0, 'Price must be greater than or equal to 0'),

  stock: z
    .number({ invalid_type_error: 'Stock must be a number' })
    .int('Stock must be an integer')
    .min(0, 'Stock must be greater than or equal to 0'),

  imageUrl: z.string().optional(),
  category: z.string().optional(),
})

// TypeScript 타입 자동 추론
type ProductFormData = z.infer<typeof productFormSchema>
// 결과:
// {
//   name: string
//   description: string
//   price: number
//   stock: number
//   imageUrl?: string
//   category?: string
// }
```

#### React Hook Form과 연결

```typescript
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'

export const AdminProductFormPage: React.FC = () => {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const isEdit = !!id && id !== 'new'

  // React Hook Form 초기화
  const {
    register,       // 입력 필드 등록
    handleSubmit,   // 폼 제출 처리
    reset,          // 폼 데이터 리셋
    formState: { errors, isSubmitting },  // 폼 상태
  } = useForm<ProductFormData>({
    resolver: zodResolver(productFormSchema),  // Zod 검증 연결
    defaultValues: {
      name: '',
      description: '',
      price: 0,
      stock: 0,
      imageUrl: '',
      category: '',
    },
  })

  // 폼 제출 처리
  const onSubmit = async (data: ProductFormData) => {
    try {
      if (isEdit) {
        await updateMutation.mutateAsync({ id: productId, data })
      } else {
        await createMutation.mutateAsync(data)
      }
      navigate('/admin/products')  // 성공 시 목록 페이지로 이동
    } catch (error) {
      console.error('Failed to save product:', error)
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      {/* 입력 필드 */}
      <Input
        label="Product Name"
        required
        error={errors.name?.message}  // 에러 메시지 표시
        {...register('name')}          // 폼에 등록
        placeholder="Enter product name"
      />

      <Input
        label="Price"
        type="number"
        required
        error={errors.price?.message}
        {...register('price', { valueAsNumber: true })}  // 숫자로 변환
        placeholder="0.00"
      />

      <button type="submit" disabled={isSubmitting}>
        {isEdit ? 'Update' : 'Create'}
      </button>
    </form>
  )
}
```

#### 컴포넌트 구조 (리얼 폼 컴포넌트)

```typescript
// Input 컴포넌트
export const Input = React.forwardRef<
  HTMLInputElement,
  InputProps & ReturnType<UseFormRegisterReturn>
>(({ label, required, error, ...props }, ref) => (
  <div>
    <label>
      {label}
      {required && <span className="text-red-500">*</span>}
    </label>
    <input
      ref={ref}
      {...props}
      className={error ? 'border-red-500' : ''}
    />
    {error && <p className="text-red-500 text-sm">{error}</p>}
  </div>
))

// 사용: {...register('name')}로 자동 연결
```

#### 에러 처리 예시

```typescript
// 폼 제출 시 에러 표시
if (errors.name) {
  console.log(errors.name.message)  // "Product name is required"
}

if (errors.price) {
  console.log(errors.price.message)  // "Price must be greater than or equal to 0"
}

// 전체 에러 확인
console.log(errors)  // { name: { message: "..." }, price: { message: "..." } }
```

#### Why? Zod + React Hook Form의 장점

1. **타입 안전성**: 런타임 타입 검증으로 버그 방지
2. **성능**: 필드 단위 렌더링으로 불필요한 재렌더링 방지
3. **DRY**: 스키마 정의 하나로 타입과 검증 동시 관리
4. **일관성**: 백엔드 검증과 프론트엔드 검증 동일 규칙 적용 가능

---

### 3. Route Guard와 권한 기반 라우팅

#### 개념: 인증/인가 상태에 따른 페이지 접근 제어

컴포지션 패턴으로 중첩된 Route Guard를 구현하여 계층적 접근 제어를 합니다.

#### 인증 상태 확인 (RequireAuth)

```typescript
// 인증 상태 확인 컴포넌트
export const RequireAuth: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const authStore = useAuthStore()  // Pinia 스토어에서 인증 상태 조회

  if (!authStore.isAuthenticated) {
    // 로그인되지 않은 경우 로그인 페이지로 리다이렉트
    return <Navigate to="/login" replace />
  }

  return <>{children}</>
}
```

#### 역할 기반 라우팅 (RequireRole)

```typescript
export interface RequireRoleProps {
  roles: string[]  // 허용할 역할 배열
  children: React.ReactNode
}

export const RequireRole: React.FC<RequireRoleProps> = ({ roles, children }) => {
  const authStore = useAuthStore()

  // 사용자 역할 확인
  const hasRequiredRole = authStore.user?.roles.some(role =>
    // 역할 정규화: 'admin' → 'ROLE_ADMIN' 변환
    roles.includes(normalizeRole(role))
  )

  if (!hasRequiredRole) {
    // 권한 없음 → 403 Forbidden 페이지로 리다이렉트
    return <Navigate to="/forbidden" replace />
  }

  return <>{children}</>
}

// 역할 정규화 함수
function normalizeRole(role: string): string {
  // 백엔드에서 'ROLE_ADMIN' 형식으로 오므로 그대로 사용
  return role.startsWith('ROLE_') ? role : `ROLE_${role.toUpperCase()}`
}
```

#### 라우터 설정

```typescript
// 라우팅 구조
<Routes>
  {/* 공개 페이지 */}
  <Route path="/products" element={<ProductListPage />} />
  <Route path="/login" element={<LoginPage />} />

  {/* 관리자 페이지 (권한 필요) */}
  <Route element={
    <RequireAuth>
      <RequireRole roles={['ROLE_ADMIN']}>
        <AdminLayout />
      </RequireRole>
    </RequireAuth>
  }>
    <Route path="/admin/products" element={<AdminProductListPage />} />
    <Route path="/admin/products/new" element={<AdminProductFormPage />} />
    <Route path="/admin/products/:id" element={<AdminProductFormPage />} />
  </Route>

  {/* 권한 없음 */}
  <Route path="/forbidden" element={<ForbiddenPage />} />
</Routes>
```

#### 계층적 보호 구조

```
Frontend Route Guard
    ↓
Backend @PreAuthorize
    ↓
Database Row-Level Security (미구현)
```

이렇게 3계층으로 보호하는 이유:

1. **Frontend Route Guard**: 사용자 경험 - 권한 없으면 페이지 접근 못함
2. **Backend @PreAuthorize**: 보안 - API 직접 호출 시에도 권한 검증
3. **Database Row-Level Security**: 궁극 보안 - 권한 우회 시에도 데이터 보호

#### Auth Store에서 역할 관리 (Pinia)

```typescript
import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)

  // 사용자 로그인
  const login = async (credentials: LoginRequest) => {
    const response = await authApi.login(credentials)
    // 토큰 저장
    localStorage.setItem('accessToken', response.accessToken)

    // 사용자 정보 저장 (역할 포함)
    user.value = {
      id: response.userId,
      email: response.email,
      name: response.name,
      roles: response.roles,  // ['ROLE_ADMIN', 'ROLE_USER']
      avatar: response.avatar
    }
  }

  // 역할 확인 메서드
  const hasRole = (role: string): boolean => {
    return user.value?.roles.includes(normalizeRole(role)) ?? false
  }

  const hasAnyRole = (roles: string[]): boolean => {
    return roles.some(role => hasRole(role))
  }

  return {
    user,
    isAuthenticated: computed(() => !!user.value),
    login,
    hasRole,
    hasAnyRole
  }
})

// 컴포넌트에서 사용
const authStore = useAuthStore()
if (authStore.hasRole('ADMIN')) {
  // 관리자 메뉴 표시
}
```

#### Why? 컴포지션 패턴을 사용하는 이유

1. **재사용성**: `RequireAuth`, `RequireRole` 독립적으로 조합 가능
2. **유연성**: 여러 조건을 겹쳐서 표현 가능 (AND 조건)
3. **가독성**: 선언적으로 보호 규칙을 표현
4. **테스트 용이**: 각 Guard를 독립적으로 테스트 가능

---

### 4. TailwindCSS 3계층 Design Tokens

#### 개념: 일관된 디자인 시스템을 위한 토큰 계층 구조

Design System에서 정의한 토큰을 3계층으로 나누어 재사용합니다.

#### 계층 구조

```
┌─────────────────────────────────────┐
│ Layer 3: Component                  │
│ (구체적 사용처)                     │
│ className="bg-bg-card text-text-body" │
├─────────────────────────────────────┤
│ Layer 2: Semantic                   │
│ (역할 기반)                         │
│ bg-bg-card (배경)                   │
│ text-text-body (본문 텍스트)       │
├─────────────────────────────────────┤
│ Layer 1: Base                       │
│ (원시 값)                           │
│ @apply bg-white                     │
│ @apply text-gray-700                │
└─────────────────────────────────────┘
```

#### 토큰 정의 (TailwindCSS 설정)

```javascript
// frontend/design-system/tailwind.config.js
export default {
  theme: {
    extend: {
      colors: {
        // Layer 1: Base - 원시 색상값
        'gray': {
          50: '#f9fafb',
          600: '#4b5563',
          700: '#374151',
          // ...
        },

        // Layer 2: Semantic - 역할 기반 색상
        'bg': {
          'card': 'var(--color-bg-card, white)',
          'hover': 'var(--color-bg-hover, #f9fafb)',
          'subtle': 'var(--color-bg-subtle, #f3f4f6)',
        },
        'text': {
          'heading': 'var(--color-text-heading, #1f2937)',
          'body': 'var(--color-text-body, #374151)',
          'meta': 'var(--color-text-meta, #6b7280)',
        },
        'border': {
          'default': 'var(--color-border-default, #e5e7eb)',
        },
        'brand': {
          'primary': 'var(--color-brand-primary, #3b82f6)',
        },
        'status': {
          'error': 'var(--color-status-error, #ef4444)',
          'error-bg': 'var(--color-status-error-bg, #fee2e2)',
        }
      },
      spacing: {
        // Layer 1: Base - 기본 간격
        4: '1rem',
        6: '1.5rem',
        8: '2rem',
      }
    }
  }
}
```

#### Layer 2: Semantic 토큰 사용

```typescript
// 통일된 스타일 적용
export const AdminProductListPage: React.FC = () => {
  return (
    <div>
      {/* 헤더 */}
      <h1 className="text-2xl font-bold text-text-heading">Products</h1>

      {/* 카드 */}
      <div className="bg-bg-card border border-border-default rounded-lg shadow-sm">
        {/* 테이블 */}
        <table className="w-full">
          <thead className="bg-bg-subtle border-b border-border-default">
            <tr>
              <th className="px-6 py-4 text-left text-xs font-medium text-text-meta">
                ID
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border-default">
            <tr className="hover:bg-bg-hover transition-colors">
              <td className="px-6 py-4 text-sm text-text-body">
                Product Name
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      {/* 버튼 */}
      <button className="bg-brand-primary text-white px-4 py-2 rounded">
        New Product
      </button>

      {/* 에러 상태 */}
      <div className="bg-status-error-bg border border-status-error/20 rounded-lg p-4">
        <p className="text-status-error">Failed to load products</p>
      </div>
    </div>
  )
}
```

#### 서비스별 테마 전환

```typescript
// data-service 속성으로 테마 전환
<div data-service="admin">
  {/* Admin 페이지 */}
</div>

<div data-service="blog">
  {/* Blog 페이지 */}
</div>

// CSS Variables로 동적 전환
:root {
  --color-brand-primary: #3b82f6;  /* Admin 파란색 */
  --color-text-heading: #1f2937;
}

[data-service="blog"] {
  --color-brand-primary: #10b981;  /* Blog 녹색 */
}
```

#### Why? 3계층 토큰 시스템의 이점

1. **일관성**: 전사 표준 디자인 시스템 적용
2. **유지보수성**: 색상 변경 시 한 곳에서만 수정
3. **접근성**: 명확한 의도 (text-body vs text-meta)
4. **테마 전환**: CSS Variables로 다크모드 등 쉽게 구현

---

## 아키텍처 패턴

### 1. Defense in Depth (심층 방어)

Admin 기능은 여러 레이어에서 보호됩니다:

```
┌─────────────────────────────────┐
│ Frontend Route Guard            │
│ (RequireRole 컴포넌트)         │
│ → 미인증 사용자 페이지 접근 차단│
└────────────────┬────────────────┘
                 ↓
┌─────────────────────────────────┐
│ Backend @PreAuthorize           │
│ (Spring Security)               │
│ → 직접 API 호출 시 권한 검증   │
└────────────────┬────────────────┘
                 ↓
┌─────────────────────────────────┐
│ Database Row-Level Security     │
│ (미구현)                       │
│ → DB 쿼리 시 권한 필터링      │
└─────────────────────────────────┘
```

**각 레이어의 역할:**

1. **Frontend (UX)**: 권한 없으면 UI 표시 안 함 → 혼동 방지
2. **Backend (보안)**: API 호출 항상 검증 → 우회 방지
3. **Database (최후 방어)**: 권한 없는 데이터 조회 차단 → 데이터 누출 방지

---

### 2. 3계층 컴포넌트 구조

```
Page Layer (비즈니스 로직)
    ↓
Container Layer (상태 관리)
    ↓
UI Component Layer (순수 렌더링)
```

#### 실제 구현

```typescript
// Layer 1: Page (비즈니스 로직, 부수효과 처리)
// frontend/shopping-frontend/src/pages/admin/AdminProductListPage.tsx
export const AdminProductListPage: React.FC = () => {
  const [filters, setFilters] = useState<ProductFilters>(...)
  const { data, isLoading } = useAdminProducts(filters)
  const deleteMutation = useDeleteProduct()

  // 비즈니스 로직
  const handleDelete = async (id: number) => {
    await deleteMutation.mutateAsync(id)
  }

  return <AdminProductListContainer
    products={data?.data.content}
    isLoading={isLoading}
    onDelete={handleDelete}
  />
}

// Layer 2: Container (상태 관리, 프로퍼티 조합)
// (일반적으로 Page와 Container가 합쳐짐)

// Layer 3: UI Component (순수 렌더링)
interface AdminProductListContainerProps {
  products: Product[]
  isLoading: boolean
  onDelete: (id: number) => void
}

export const AdminProductListContainer: React.FC<AdminProductListContainerProps> = ({
  products,
  isLoading,
  onDelete
}) => {
  return (
    <div>
      <table>
        {products?.map(product => (
          <tr key={product.id}>
            <td>{product.name}</td>
            <td>{product.price}</td>
            <td>
              <Button onClick={() => onDelete(product.id)}>
                Delete
              </Button>
            </td>
          </tr>
        ))}
      </table>
    </div>
  )
}

// Button: 최하위 UI 컴포넌트
interface ButtonProps {
  onClick: () => void
  children: React.ReactNode
}

export const Button: React.FC<ButtonProps> = ({ onClick, children }) => (
  <button className="bg-blue-500 text-white px-4 py-2" onClick={onClick}>
    {children}
  </button>
)
```

**왜 이 구조인가?**

1. **관심사 분리**: 각 계층이 책임 1개만 담당
2. **테스트 용이**: UI Component는 props만 테스트 (부수효과 없음)
3. **재사용성**: UI Component를 다른 Page에서 재사용 가능
4. **유지보수성**: 비즈니스 로직 변경 시 UI는 수정 불필요

---

## 실습 과제

### 과제 1: ADMIN 권한 부여 API 구현

**난이도**: 중상

**요구사항**:
1. Auth Service에 `/api/auth/admin/grant` 엔드포인트 구현
2. Admin 권한 부여/회수 기능
3. 권한 감사 로그 기록

**구현 힌트**:
```java
@PostMapping("/admin/grant")
@PreAuthorize("hasRole('SUPER_ADMIN')")  // SUPER_ADMIN만 권한 부여 가능
public ResponseEntity<ApiResponse<UserResponse>> grantAdminRole(
    @RequestParam String userId) {
  // 1. 사용자 조회
  // 2. ROLE_ADMIN 권한 추가
  // 3. 감사 로그 저장
  // 4. 응답 반환
}
```

---

### 과제 2: 상품 카테고리 CRUD 추가

**난이도**: 중

**요구사항**:
1. Category 엔티티 생성 (id, name, description, order)
2. AdminCategoryController 구현 (CRUD)
3. 상품과 카테고리 M:1 관계 설정

**구현 순서**:
```
1. Category 엔티티 및 Repository 생성
2. CategoryService 구현
3. AdminCategoryController 구현
4. Frontend에서 카테고리 선택 드롭다운 추가
```

---

### 과제 3: 상품 이미지 업로드 기능 구현

**난이도**: 상

**요구사항**:
1. AWS S3 또는 로컬 스토리지에 이미지 저장
2. `/api/shopping/admin/products/{id}/upload` 엔드포인트
3. 프론트엔드에서 이미지 미리보기 및 업로드

**구현 힌트**:
```java
@PostMapping("/{productId}/upload")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse<ProductResponse>> uploadProductImage(
    @PathVariable Long productId,
    @RequestParam("file") MultipartFile file) {
  // 1. 파일 검증 (타입, 크기)
  // 2. S3에 업로드
  // 3. Product.imageUrl 업데이트
  // 4. 응답 반환
}
```

**Frontend**:
```typescript
const handleImageUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
  const file = event.target.files?.[0]
  if (!file) return

  const formData = new FormData()
  formData.append('file', file)

  const response = await adminProductApi.uploadProductImage(productId, formData)
  setPreviewUrl(response.imageUrl)
}
```

---

## 참고 자료

### 공식 문서
- [Spring Security Reference](https://spring.io/projects/spring-security)
- [Spring Data JPA Reference](https://spring.io/projects/spring-data-jpa)
- [React Query Documentation](https://tanstack.com/query/latest)
- [React Hook Form Documentation](https://react-hook-form.com/)
- [Zod Documentation](https://zod.dev/)
- [TailwindCSS Documentation](https://tailwindcss.com/)

### 프로젝트 내 관련 문서
- **아키텍처**: `/docs/architecture/mfa-architecture.md` (Module Federation)
- **API 명세**: `/docs/api/shopping-api.md` (Admin API 상세)
- **인증**: `/docs/learning-notes/oauth2-flow.md` (OAuth2 플로우)
- **에러 처리**: `/docs/architecture/error-handling.md` (ErrorCode 패턴)

### 핵심 코드 파일
```
Backend
├── /services/shopping-service/src/main/java/
│   ├── controller/AdminProductController.java     # Admin API 엔드포인트
│   ├── service/ProductServiceImpl.java             # 비즈니스 로직
│   ├── dto/AdminProductRequest.java               # 요청 DTO (검증)
│   └── exception/ShoppingErrorCode.java           # 에러 코드 정의

Frontend
├── /frontend/shopping-frontend/src/
│   ├── pages/admin/AdminProductListPage.tsx       # 목록 페이지
│   ├── pages/admin/AdminProductFormPage.tsx       # 폼 페이지
│   ├── hooks/useAdminProducts.ts                  # React Query Hooks
│   ├── api/endpoints.ts                           # API 엔드포인트
│   ├── types/admin.ts                             # Admin 타입 정의
│   └── components/layout/AdminLayout.tsx          # Admin 레이아웃
```

### 학습 경로 추천

**초급 (기초 개념)**:
1. Spring Security 기본 개념 이해
2. Jakarta Validation의 검증 규칙 학습
3. TailwindCSS 토큰 시스템 이해

**중급 (구현)**:
1. @PreAuthorize로 간단한 권한 제어 구현
2. React Query의 Query/Mutation 패턴 학습
3. React Hook Form으로 폼 검증 구현

**상급 (최적화)**:
1. 권한 검증 아키텍처 설계 (Defense in Depth)
2. React Query 캐싱 전략 최적화
3. 성능 프로파일링 및 개선

---

## 자주 묻는 질문 (FAQ)

### Q1: @PreAuthorize가 작동하지 않습니다

**답변**: Spring Security 설정 확인:
```java
@Configuration
@EnableMethodSecurity  // 필수! Method-level 보안 활성화
public class SecurityConfig {
    // ...
}
```

### Q2: React Query 캐시가 업데이트되지 않습니다

**답변**: 캐시 무효화 확인:
```typescript
// mutationFn 후 invalidateQueries 필수
onSuccess: () => {
  queryClient.invalidateQueries({
    queryKey: adminProductKeys.lists()  // 정확한 queryKey 사용
  })
}
```

### Q3: ROLE 접두사 오류가 발생합니다

**답변**: 역할 정규화:
```typescript
// 백엔드에서 'ROLE_ADMIN' 형식이므로 프론트엔드도 동일하게 사용
hasRole('ROLE_ADMIN')  // ✓ 올바름
hasRole('admin')       // ✗ 오류 (자동으로 ROLE_ 추가됨)
```

### Q4: 폼 제출 후 목록 페이지로 이동했는데 데이터가 이전 상태입니다

**답변**: 캐시 무효화 타이밍 확인:
```typescript
// mutationFn 완료 후 invalidateQueries 호출되어야 함
const mutation = useMutation({
  mutationFn: (data) => api.create(data),
  onSuccess: () => {
    queryClient.invalidateQueries(...)  // 이 후 페이지 이동
    navigate('/list')
  }
})
```

---

**마지막 업데이트**: 2025-01-17
**작성자**: Portal Universe Tutor Agent
**대상 독자**: 프로젝트 신규 개발자, 아키텍처 학습자
