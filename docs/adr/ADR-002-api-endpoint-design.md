# ADR-002: Admin API 엔드포인트 설계

## 상태
**Accepted**

## 날짜
2026-01-17

---

## 컨텍스트

Admin 상품 관리 기능을 구현하기 위해 backend API 설계가 필요합니다. 다음 두 가지 옵션을 검토했습니다:

1. **기존 API 재사용**: 현재 구현된 상품 CRUD API(`/api/shopping/product`)를 Admin에서도 사용
2. **Admin 전용 엔드포인트 분리**: Admin만 사용하는 별도의 엔드포인트(`/api/shopping/admin/products`) 구현

### 현황

- Shopping Service에는 이미 완전한 상품 CRUD API가 구현되어 있음
- ProductController: `POST`, `PUT`, `DELETE` 메서드 존재
- `@PreAuthorize("hasRole('ADMIN')")` 어노테이션은 아직 추가 필요
- Admin과 고객 모두 같은 데이터 모델 사용 가능

---

## 결정

**기존 API를 활용하되, 명시적인 권한 검증을 추가합니다.**

### 구현 방식

```
Admin UI (shopping-frontend)
    ↓
기존 API + @PreAuthorize 추가
    ↓
ProductController
    ↓
Shopping Service (8083)
```

### 구체적 변경사항

#### 1. Controller에 권한 검증 추가

```java
@RestController
@RequestMapping("/api/shopping/product")
@RequiredArgsConstructor
public class ProductController {

  // ========== 공개 API (인증 불필요) ==========

  @GetMapping("/{productId}")
  public ApiResponse<ProductResponse> getProduct(@PathVariable Long productId) {
      return ApiResponse.success(productService.getProduct(productId));
  }

  // ========== Admin 전용 API (ADMIN 권한 필수) ==========

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
      @Valid @RequestBody ProductCreateRequest request) {
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(ApiResponse.success(productService.createProduct(request)));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/{productId}")
  public ApiResponse<ProductResponse> updateProduct(
      @PathVariable Long productId,
      @Valid @RequestBody ProductUpdateRequest request) {
      return ApiResponse.success(productService.updateProduct(productId, request));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{productId}")
  public ApiResponse<Void> deleteProduct(@PathVariable Long productId) {
      productService.deleteProduct(productId);
      return ApiResponse.success(null);
  }
}
```

#### 2. RequestBody Validation 추가

```java
public record ProductCreateRequest(
    @NotBlank(message = "Product name is required")
    @Size(min = 1, max = 200, message = "Product name must be between 1 and 200 characters")
    String name,

    @NotBlank(message = "Product description is required")
    @Size(max = 2000, message = "Product description must not exceed 2000 characters")
    String description,

    @NotNull(message = "Product price is required")
    @Positive(message = "Product price must be greater than 0")
    Double price,

    @NotNull(message = "Product stock is required")
    @Min(value = 0, message = "Product stock must be non-negative")
    Integer stock
) {}
```

#### 3. 응답 타입 수정

```java
public record ProductResponse(
    Long id,
    String name,          // 누락된 필드 추가
    String description,
    Double price,
    Integer stock
) {
    public static ProductResponse from(Product entity) {
        return new ProductResponse(
            entity.getId(),
            entity.getName(),
            entity.getDescription(),
            entity.getPrice(),
            entity.getStock()
        );
    }
}
```

---

## 대안 검토

| 대안 | 장점 | 단점 | 평가 |
|------|------|------|------|
| **기존 API 활용** | 코드 중복 없음, 구현 빠름, 유지보수 간단 | API 버전 관리 복잡 가능성 | ✅ **선택** |
| **Admin 전용 분리** | API 명확한 구분, 향후 확장 유연함 | 코드 중복, 유지보수 비용 증가 | ❌ |
| **API 버전 관리 추가** | 명확한 버전 관리 가능 | 복잡도 증가, 기존 API에 영향 | ❌ |

### 선택 근거

1. **개발 효율성**
   - 이미 구현된 CRUD API 재사용으로 개발 시간 단축
   - 새로운 엔드포인트 구현 불필요

2. **코드 중복 방지**
   - 상품 생성/수정/삭제 로직 중복 제거
   - Service 레이어 로직 재사용

3. **유지보수 용이**
   - 버그 수정 시 한 곳만 수정
   - 비즈니스 로직 변경 시 영향 범위 최소화

4. **향후 확장성**
   - 고객용/Admin용 공통 API이므로 확장 용이
   - 필요 시 Admin 전용 필터링(예: 비활성 상품 조회) 추가 가능

---

## 결과

### 긍정적 영향

1. **빠른 구현**
   - 기존 코드 활용으로 개발 기간 단축 (약 40% 이상)
   - 즉시 Admin 기능 제공 가능

2. **낮은 버그 위험**
   - 이미 검증된 코드 사용
   - ProductService 로직 신뢰도 높음

3. **명확한 보안**
   - Controller 레벨 `@PreAuthorize`로 권한 명시
   - API Gateway에서 JWT 검증 + Service에서 권한 검증 (심층 방어)

4. **일관된 응답 형식**
   - Admin과 고객 모두 동일한 ApiResponse 형식
   - 클라이언트 통일성 확보

### 부정적 영향

1. **API 혼재 가능성**
   - 공개 API와 Admin API가 동일한 엔드포인트 사용
   - API 문서화 시 명확한 권한 표시 필수

2. **향후 분리 어려움**
   - 나중에 Admin 전용 로직 추가 시 리팩토링 필요 가능성
   - URL 구조 변경 불가 (기존 클라이언트 영향)

### 완화 방안

1. **명확한 API 문서화**
   - OpenAPI/Swagger에 권한 정보 명확히 표시
   - Admin과 공개 엔드포인트 구분 가능하도록 주석 추가

2. **향후 확장을 고려한 설계**
   - Admin 전용 기능은 별도 엔드포인트로 추가 (예: `/api/shopping/product/admin/list`)
   - 기본 CRUD는 공유, 고급 기능은 분리

3. **버전 관리 계획**
   - 필요시 API v2 도입 (하위호환성 유지)
   - 현재는 v1로 시작, 추후 v2 계획

---

## 추가 API 제안

기존 API만으로는 부족한 Admin 기능:

### 1. Admin 전용 목록 조회 (페이징 + 필터링)

```
GET /api/shopping/product/admin/list?page=0&size=20&status=ACTIVE
```

**필요성**:
- 일반 사용자 API는 활성 상품만 반환
- Admin은 비활성, 삭제 상품도 관리 필요
- 재고 현황 필터링

### 2. 상품 상태 변경 (ACTIVE/INACTIVE)

```
PATCH /api/shopping/product/{productId}/status
{ "status": "INACTIVE" }
```

**필요성**:
- Hard Delete 대신 Soft Delete 관리
- 판매 중지 상품 임시 숨김

### 3. 재고 일괄 수정

```
PATCH /api/shopping/product/admin/stock-batch
{
  "updates": [
    { "productId": 1, "stock": 100 },
    { "productId": 2, "stock": 50 }
  ]
}
```

**필요성**:
- 대량 입고 시 효율적 처리
- API 호출 횟수 감소

### 4. 상품 통계 조회

```
GET /api/shopping/product/admin/statistics
```

**필요성**:
- Dashboard 데이터 제공
- 총 상품수, 재고 부족, 매출액 등

---

## 구현 계획

### Phase 1 (즉시): 기존 API 보안 강화
- [ ] `@PreAuthorize("hasRole('ADMIN')")` 추가
- [ ] RequestBody Validation 추가
- [ ] ProductResponse에 name 필드 추가 (버그 수정)
- [ ] 에러 코드 정의 (S006-S010)

### Phase 2 (1주일): Admin 기본 기능
- [ ] Admin 전용 목록 조회 API 구현
- [ ] Frontend Admin UI 구현 (기존 API 활용)

### Phase 3 (2주일): 고급 기능
- [ ] 상품 상태 관리 기능
- [ ] 재고 일괄 수정 기능
- [ ] 상품 통계 API

---

## 참고 자료

- 참고 문서: `/Users/laze/Laze/Project/portal-universe/docs/api/admin-products-api.md`
- Backend 가이드: `/Users/laze/Laze/Project/portal-universe/CLAUDE.md` (서비스 간 통신 패턴)
- API 응답 형식: `common-library/ApiResponse.java`

---

## 다음 단계

1. ProductController에 `@PreAuthorize` 추가
2. RequestBody Validation 구현
3. ProductResponse 수정 (name 필드 추가)
4. Frontend에서 기존 API 호출
5. Admin 기능 구현 (Phase 2)

---

**문서 버전**: 1.0
**작성자**: Documenter Agent
**최종 업데이트**: 2026-01-17
