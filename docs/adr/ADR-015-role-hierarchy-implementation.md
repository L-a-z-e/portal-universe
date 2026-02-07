# ADR-015: Role Hierarchy 구현 방안

**Status**: Accepted (Option A: Gateway 중심 Role Hierarchy — [ADR-021](./ADR-021-role-based-membership-restructure.md))
**Date**: 2026-02-07

## Context
`RoleEntity`에 `parentRole` 필드가 존재하지만 `RbacService`에서 활용하지 않고 있습니다. 현재 권한 검사는 각 역할을 flat하게 비교하는 방식입니다.

### 문제점
- `ROLE_SUPER_ADMIN`이 하위 역할 권한을 갖지만 Gateway `SecurityConfig`에서 `hasAnyAuthority()`로 수동 나열
- 새 역할 추가 시 모든 `hasAnyAuthority()` 호출을 찾아서 수정 필요
- `parentRole` 필드가 사용되지 않아 데이터 모델과 실제 동작 불일치

## Decision
Gateway 중심 Role Hierarchy 해석을 채택합니다 (Option A: Spring Security RoleHierarchy 기반). Gateway가 DB `parentRole` 관계를 resolve하여 `X-User-Effective-Roles` 헤더로 하위 서비스에 전달합니다.

## Rationale
- 현재 역할 수가 적어 수동 관리 가능 (5개: USER, SELLER, BLOG_ADMIN, SHOPPING_ADMIN, SUPER_ADMIN)
- 구현 복잡도 대비 당장의 효과 낮음
- 향후 역할 수 증가 시 재검토 필요

## Trade-offs
✅ **장점**:
- 구현 복잡도 최소화
- 기존 코드 안정성 유지

⚠️ **단점 및 완화**:
- 새 역할 추가 시 수동 업데이트 필요 → (완화: 역할 수 증가 시 Option A 구현 검토)
- `parentRole` 필드 미사용 → (현재 상태 유지, 향후 사용 예정)

## Implementation
### Option A: Spring Security RoleHierarchy (권장, 향후)
- Spring Security `RoleHierarchyImpl`을 활용
- DB의 `parentRole` 관계 기반으로 동적 생성
- Gateway와 Auth-service 모두 적용

### Option B: DB 기반 Permission 확장 (고급)
- `parentRole` 탐색하여 상위 역할 permission 포함
- 더 세밀한 제어 가능하나 구현 복잡도 높음

## References
- [ADR-011: Hierarchical RBAC & Membership System](./ADR-011-hierarchical-rbac.md)
- [Spring Security RoleHierarchy](https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html#authz-hierarchical-roles)

---

📂 상세: [old-docs/central/adr/ADR-015-role-hierarchy-implementation.md](../old-docs/central/adr/ADR-015-role-hierarchy-implementation.md)
