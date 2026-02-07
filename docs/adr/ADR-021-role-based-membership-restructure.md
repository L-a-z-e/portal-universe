# ADR-021: 역할+서비스 복합 멤버십 재구조화

**Status**: Accepted
**Date**: 2026-02-07
**Supersedes**: ADR-011 (Hierarchical RBAC + Membership System)

## Context

현재 시스템의 권한/멤버십 체계에 구조적 문제가 있습니다:
1. `users.role` enum과 RBAC `user_roles` 테이블이 이중으로 존재
2. `membership_tiers`가 `service_name`으로만 구분되어 동일 서비스 내 역할별 차등 티어 불가
3. Gateway `SecurityConfig`에 `hasAnyAuthority()` 수동 나열 (하드코딩)
4. `parentRole` 필드 미사용으로 Role Hierarchy 미구현
5. Admin UI 부재로 역할/권한/멤버십 관리 불가

## Decision

**역할+서비스 복합 멤버십 모델로 전환**합니다. `membership_group = {role_scope}:{service}` 형태의 복합 키를 사용하고, Gateway 중심 Role Hierarchy 해석, clean migration(old/new 공존 없음)을 적용합니다.

## Rationale

- **복합 멤버십 그룹**: `user:blog`, `seller:shopping` 등으로 역할과 서비스 조합별 독립 티어 체계 가능
- **Gateway 중심 권한 해석**: 복잡한 Role Hierarchy resolve를 Gateway가 완료 → 하위 서비스(Java/NestJS/Python)는 단순 헤더 체크만 수행, Polyglot 대비
- **Enriched JWT/Header**: `sort_order` 포함으로 하위 서비스가 별도 API 호출 없이 티어 비교 가능
- **Clean migration**: old/new 공존 없이 단일 마이그레이션으로 전환, 코드 복잡도 최소화
- **ROLE_SELLER → ROLE_SHOPPING_SELLER**: 향후 다른 서비스의 Seller 역할 추가 시 네이밍 충돌 방지

## Trade-offs

✅ **장점**:
- 역할별, 서비스별 독립 티어 체계 (blog PRO ≠ shopping PRO)
- Gateway 해석으로 non-Java 서비스(NestJS, Python)에 Role Hierarchy 로직 불필요
- `TIER_ORDER` 하드코딩 제거, JWT `sort_order`로 동적 비교
- Admin UI로 역할/권한/멤버십 운영 관리 가능

⚠️ **단점 및 완화**:
- Clean migration으로 롤백 어려움 → (완화: 마이그레이션 전 DB 풀 백업, 로컬 검증)
- JWT 포맷 변경으로 기존 세션 깨짐 → (완화: Refresh Token 일괄 무효화 + Access Token 15분 TTL 자연 만료)
- ROLE_SELLER 개명으로 일시적 403 가능 → (완화: 배포 순서 준수: common-library → auth → gateway → downstream)

## Implementation

### 새 역할 체계
```
SUPER_ADMIN (전체 시스템)
├── BLOG_ADMIN → USER
└── SHOPPING_ADMIN → SHOPPING_SELLER → USER
```

### 멤버십 그룹
| membership_group | 대상 역할 | 티어 |
|-----------------|----------|------|
| `user:blog` | USER | FREE, PRO, MAX |
| `user:shopping` | USER | (서비스별 정의) |
| `seller:shopping` | SHOPPING_SELLER | BRONZE, SILVER, GOLD, PLATINUM |

### JWT Claims
```json
{
  "roles": ["ROLE_USER", "ROLE_SHOPPING_SELLER"],
  "memberships": {
    "user:blog": {"tier": "PRO", "order": 2},
    "seller:shopping": {"tier": "GOLD", "order": 3}
  }
}
```

### DB 스키마 변경
- `membership_tiers`: `service_name` → `membership_group`, UNIQUE(membership_group, tier_key)
- `user_memberships`: `service_name` → `membership_group`, UNIQUE(user_id, membership_group)
- `users`: `role` enum 컬럼 삭제
- `roles`: `membership_group` 컬럼 추가 (nullable)

### Gateway 중심 해석 흐름
```
Gateway: JWT → Role Hierarchy resolve → X-User-Effective-Roles, X-User-Memberships 헤더
하위 서비스: 헤더 읽기 → roles.contains() / sort_order 비교
```

### 구현 순서
1. DB Migration (V2)
2. Auth Service (Domain, Service, DTO, Controller, Repository)
3. Gateway + Common Library + Downstream Services
4. Admin Frontend (신규 React SPA)
5. Runbook + 최종 검증

## References

- [ADR-011: Hierarchical RBAC + Membership System](./ADR-011-hierarchical-rbac-membership-system.md) — Superseded by this ADR
- [ADR-015: Role Hierarchy Implementation](./ADR-015-role-hierarchy-implementation.md) — Accepted (Option A)
- [ADR-004: JWT RBAC Auto-Config](./ADR-004-jwt-rbac-auto-config.md)

---

### 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-07 | 초안 작성 | Laze |
