# ADR-002: Admin API 엔드포인트 설계

**Status**: Accepted
**Date**: 2026-01-17

## Context
Admin 상품 관리 기능을 구현하기 위해 backend API 설계가 필요합니다. Shopping Service에는 이미 완전한 상품 CRUD API(`/api/shopping/product`)가 구현되어 있으나, `@PreAuthorize("hasRole('ADMIN')")` 어노테이션은 아직 추가되지 않았습니다. Admin 전용 엔드포인트를 새로 만들 것인지, 기존 API에 권한 검증을 추가할 것인지 결정해야 합니다.

## Decision
기존 API를 활용하되, 명시적인 권한 검증을 추가합니다.

### 구현 방식
```
Admin UI → 기존 API + @PreAuthorize → ProductController → Shopping Service
```

## Rationale
- **개발 효율성**: 이미 구현된 CRUD API 재사용으로 개발 시간 약 40% 단축
- **코드 중복 방지**: Service 레이어 로직 재사용, 버그 수정 시 한 곳만 수정
- **명확한 보안**: Controller 레벨 `@PreAuthorize`로 권한 명시, API Gateway(JWT 검증) + Service(권한 검증) 심층 방어
- **일관성**: Admin과 고객 모두 동일한 ApiResponse 형식 사용
- **확장성**: 필요 시 Admin 전용 기능은 별도 엔드포인트로 추가 (예: `/admin/list`)

## Trade-offs
✅ **장점**:
- 즉시 Admin 기능 제공 가능
- 이미 검증된 코드 사용으로 버그 위험 낮음
- API Gateway + Service 이중 검증으로 보안 강화

⚠️ **단점 및 완화**:
- 공개 API와 Admin API 혼재 → (완화: OpenAPI/Swagger에 권한 정보 명확히 표시)
- 향후 Admin 전용 로직 추가 시 리팩토링 필요 → (완화: Admin 고급 기능은 `/admin/*` 별도 엔드포인트로 분리)
- URL 구조 변경 불가 → (완화: 필요시 API v2 도입 계획, 하위호환성 유지)

## Implementation
**변경사항**:
```java
@RestController
@RequestMapping("/api/shopping/product")
public class ProductController {
  // 공개 API (인증 불필요)
  @GetMapping("/{productId}")
  public ApiResponse<ProductResponse> getProduct(@PathVariable Long productId) { ... }

  // Admin 전용 (ADMIN 권한 필수)
  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  public ResponseEntity<ApiResponse<ProductResponse>> createProduct(...) { ... }

  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/{productId}")
  public ApiResponse<ProductResponse> updateProduct(...) { ... }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{productId}")
  public ApiResponse<Void> deleteProduct(...) { ... }
}
```

**추가 API 제안** (향후):
- `GET /api/shopping/product/admin/list` - 비활성/삭제 상품 포함 목록
- `PATCH /api/shopping/product/{id}/status` - 상품 상태 변경
- `GET /api/shopping/product/admin/statistics` - Dashboard 통계

## References
- 참고 문서: `/Users/laze/Laze/Project/portal-universe/docs/api/admin-products-api.md`
- Backend 가이드: `/Users/laze/Laze/Project/portal-universe/CLAUDE.md`

---

📂 상세: [old-docs/central/adr/ADR-002-api-endpoint-design.md](../old-docs/central/adr/ADR-002-api-endpoint-design.md)
