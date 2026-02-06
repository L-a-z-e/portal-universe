# ADR-003: Admin 권한 검증 전략

**Status**: Accepted
**Date**: 2026-01-17

## Context
Admin 기능 구현 시 권한 검증이 필수적입니다. 다음 시나리오를 고려해야 합니다: (1) Admin이 아닌 사용자가 `/admin` 페이지 접근 시도, (2) 권한 없는 사용자가 API를 직접 호출, (3) 네트워크 오류로 권한 검증 실패, (4) 권한 변경으로 기존 사용자 권한 박탈. 권한 검증은 **심층 방어(Defense in Depth)** 원칙을 따라야 합니다.

## Decision
Frontend Route Guard + Backend @PreAuthorize를 조합한 심층 방어 전략을 채택합니다.

### 계층별 구현
```
Frontend Route Guard (UX 보호, 즉각 피드백)
  ↓
API Gateway (JWT 토큰 검증, 인증)
  ↓
Backend Service (@PreAuthorize, 인가)
  ↓
Business Logic (Resource Owner 검증, 본인 확인)
```

## Rationale
- **보안 보장**: Backend `@PreAuthorize`가 최종 방어선, Frontend 우회 불가능
- **우수한 UX**: Frontend Route Guard로 불필요한 페이지 이동 방지, 비권한 사용자에게 빠른 피드백
- **명확한 책임 분리**: Frontend는 사용자 경험, Backend는 실제 보안 보장
- **장애 대응**: API 직접 호출 시에도 Backend 차단, 401/403 에러 로깅으로 감사 추적
- **확장성**: 세분화된 권한 추가 시 Frontend/Backend 동시 업데이트 용이

## Trade-offs
✅ **장점**:
- 다층 방어로 권한 우회 불가능
- Frontend 변조 시에도 Backend 검증으로 보호
- Admin 메뉴 자동 숨김으로 UX 개선
- 불필요한 API 호출 최소화

⚠️ **단점 및 완화**:
- 구현 복잡도 증가 → (완화: `RequireRole` 고차 컴포넌트로 패턴화)
- 권한 동기화 필요 → (완화: JWT 토큰에 권한 정보 포함, Backend 발급 시 정확한 권한 반영)
- 성능 영향 → (완화: Frontend 체크는 메모리 연산, Backend는 필요 시만 DB 조회)

## Implementation
**Frontend Route Guard**:
```typescript
// src/components/guards/RequireRole.tsx
export const RequireRole: React.FC<{ children: React.ReactNode; roles: string[] }> =
  ({ children, roles }) => {
    const { user } = useAuthStore();
    const hasRole = user?.roles?.some(role => roles.includes(role));
    if (!hasRole) return <Navigate to="/403" replace />;
    return <>{children}</>;
  };

// 라우트 적용
{ path: 'admin/products', element: <RequireRole roles={['ROLE_ADMIN']}><AdminProductListPage /></RequireRole> }
```

**Backend 권한 검증**:
```java
@PreAuthorize("hasRole('ADMIN')")
@PostMapping
public ResponseEntity<ApiResponse<ProductResponse>> createProduct(...) { ... }
```

**에러 처리 흐름**:
```
Frontend RequireRole → (권한 없음) → /403 리다이렉트
API Gateway JWT 검증 → (토큰 없음/만료) → 401 Unauthorized
Backend @PreAuthorize → (권한 없음) → 403 Forbidden
Axios Interceptor → 401/403 에러 토스트 표시
```

## References
- 참고 문서: `/Users/laze/Laze/Project/portal-universe/docs/architecture/admin-authorization-strategy.md`
- OWASP: Broken Access Control (A01:2021)
- 관련 ADR: [ADR-002](./ADR-002-api-endpoint-design.md)

---

📂 상세: [old-docs/central/adr/ADR-003-authorization-strategy.md](../old-docs/central/adr/ADR-003-authorization-strategy.md)
