# ADR-011: 계층적 RBAC + 멤버십 기반 인증/인가 시스템

**Status**: Superseded by [ADR-021](./ADR-021-role-based-membership-restructure.md)
**Date**: 2026-01-28

## Context

현재 시스템은 `ROLE_USER`, `ROLE_ADMIN` 2단계 Role만 지원하여 Shopping 서비스의 판매자(Seller) 역할을 구분할 수 없고, 서비스별 관리자 분리와 멤버십 티어 기반 기능 차별화가 불가능합니다. 새 서비스 추가 시 역할/권한 확장이 어렵고, JWT claims.roles가 단일 문자열로 다중 역할을 지원하지 못합니다.

## Decision

**계층적 RBAC + Membership 하이브리드 모델**을 채택합니다. 핵심 원칙은 **Lean Token, Rich Resolution** — JWT에는 Role과 Membership만 포함하고, Permission은 서비스 레벨에서 Redis 캐시 기반으로 해석합니다.

## Rationale

- **계층적 Role**: `SUPER_ADMIN` → `SERVICE_ADMIN` → `SELLER` → `USER` 구조로 상위 Role이 하위 Permission 상속
- **다중 Role**: 한 사용자가 복수 Role 보유 가능 (예: USER + SELLER)
- **Permission 모델**: `{service}:{resource}:{action}` 형식 (예: `shopping:product:create`)
- **Membership 티어**: 서비스별 4단계(FREE/BASIC/PREMIUM/VIP), 기능 차별화 및 수익 모델 확보
- **JWT v2**: roles 배열, memberships Map으로 확장, 하위 호환 유지

## Trade-offs

✅ **장점**:
- 서비스별 관리자 분리, Seller 역할 추가로 최소 권한 원칙 적용
- Permission 기반 세밀한 접근 제어
- 멤버십 기반 기능 차별화로 수익 모델 확보
- 새 서비스 추가 시 Role/Permission 확장 용이

⚠️ **단점 및 완화**:
- 구현 복잡도 증가 (8개 신규 테이블, Redis 의존성) → (완화: Phase별 점진적 전환, common-library 자동 설정)
- 모든 서비스에 걸친 변경 필요 → (완화: Gateway v1/v2 dual format 지원, 하위 호환 유지)
- Permission Resolution 레이턴시 (캐시 미스 시 5-15ms) → (완화: Redis 캐시 히트 < 1ms, Short TTL 5분)

## Implementation

### 1. JWT Claims 구조 변경
**Before (v1)**: `{"roles": "ROLE_USER"}`
**After (v2)**: `{"roles": ["ROLE_USER", "ROLE_SELLER"], "memberships": {"shopping": "PREMIUM"}}`

### 2. Role 계층 구조
```
SUPER_ADMIN (전체 시스템)
├── BLOG_ADMIN (블로그 관리)
├── SHOPPING_ADMIN (쇼핑 전체 관리)
│   └── SELLER (상품 등록/수정)
└── USER (기본 역할)
```

### 3. Permission 예시
- `blog:post:create` (USER)
- `shopping:product:create` (SELLER)
- `shopping:order:manage` (SHOPPING_ADMIN)
- `system:role:manage` (SUPER_ADMIN)

### 4. DB 스키마 (8개 테이블)
- `roles`, `permissions`, `user_roles`, `role_permissions`
- `membership_tiers`, `membership_tier_permissions`, `user_memberships`
- `auth_audit_log`

### 5. 권한 검증 흐름
Gateway JWT 검증 → `X-User-Roles` (배열), `X-User-Memberships` (JSON) → 각 서비스 `PermissionResolver` (Redis 캐시) → SecurityContext에 Authority 설정 → `@PreAuthorize` 검증

### 6. 구현 Phase (5단계)
- Phase 1: DB 스키마 + 마이그레이션
- Phase 2: JWT v2 + Gateway dual format
- Phase 3: Permission Resolution + Membership API
- Phase 4: Frontend 업데이트 (usePermission hook)
- Phase 5: Full RBAC 적용, User.role enum 제거

## References

- [ADR-003 Admin 권한 검증 전략](./ADR-003-authorization-strategy.md) - 본 ADR에 의해 확장됨
- [ADR-004 JWT RBAC 자동 설정 전략](./ADR-004-jwt-rbac-auto-config.md)
- [ADR-010 보안 강화 아키텍처](./ADR-010-security-enhancement-architecture.md)

---

📂 상세: [old-docs/central/adr/ADR-011-hierarchical-rbac-membership-system.md](../old-docs/central/adr/ADR-011-hierarchical-rbac-membership-system.md)
