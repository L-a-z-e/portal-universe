# ADR-044: Role Multi-Include DAG + JWT Effective Roles

**Status**: Accepted
**Date**: 2026-02-18
**Author**: Laze
**Supersedes**: ADR-015 Role Hierarchy Implementation (단일 parentRole FK 구조)

## Context

현재 `RoleEntity.parentRole`이 단일 FK(`@ManyToOne`)로 구현되어 트리 구조만 가능하다.
`ROLE_SUPER_ADMIN`이 SHOPPING_ADMIN과 BLOG_ADMIN을 동시에 포함할 수 없는 근본적 한계가 있다.
현재 SUPER_ADMIN은 `parentRole=null`로 어떤 역할도 상속하지 않는 상태이다.

추가로 Gateway가 매 요청마다 auth-service Internal API를 호출(Redis 캐시 5분)하여 effective roles를 resolve하는 비효율이 있다.

## Decision

1. `parentRole` 단일 FK를 `role_includes` 다대다(MtM) 테이블로 전환하여 DAG(Directed Acyclic Graph) 구조를 지원한다.
2. JWT access token에 `effectiveRoles` claim을 추가하여 Gateway에서 auth-service API 호출 없이 effective roles를 직접 사용한다.

## Alternatives

| 대안 | 장점 | 단점 |
|------|------|------|
| ① 현행 유지 (parentRole FK) | 변경 불필요 | 다중 상속 불가, SUPER_ADMIN 한계 |
| ② role_includes MtM + JWT effectiveRoles (채택) | DAG 지원, Gateway 호출 제거 | 마이그레이션 비용, cycle detection 필요 |
| ③ Spring Security RoleHierarchy 활용 | 프레임워크 기본 기능 | DB 기반 동적 변경 어려움, 단방향만 지원 |

## Rationale

- **다중 포함 필수**: SUPER_ADMIN이 SHOPPING_ADMIN과 BLOG_ADMIN을 동시에 포함해야 전체 시스템 관리가 가능
- **DAG 구조**: cycle detection으로 순환 참조를 방지하면서 유연한 역할 계층 구성 가능
- **Gateway 성능**: JWT에 effectiveRoles를 내장하면 매 요청마다 auth-service 호출 + Redis 캐시 의존성 제거
- **하위 호환**: 기존 JWT의 `roles` claim은 유지하고 `effectiveRoles`만 추가하여 구형 JWT fallback 지원

## Trade-offs

✅ **장점**:
- SUPER_ADMIN → [SHOPPING_ADMIN, BLOG_ADMIN] 다중 포함 가능
- Gateway에서 auth-service API 호출 제거 (요청당 latency 감소)
- cycle detection으로 안전한 DAG 관리
- ROLE_GUEST 도입으로 권한 체계 세분화

⚠️ **단점 및 완화**:
- DB 마이그레이션 필요 → V4 Flyway로 자동 처리, 기존 parentRole 데이터 자동 마이그레이션
- JWT 크기 약간 증가 → effectiveRoles는 문자열 배열로 최소한의 크기
- 역할 변경 시 JWT 즉시 반영 불가 → access token 만료 시 갱신 (기존과 동일)

## Implementation

### DB 스키마

```sql
CREATE TABLE role_includes (
  id bigint NOT NULL AUTO_INCREMENT,
  role_id bigint NOT NULL,
  included_role_id bigint NOT NULL,
  created_at datetime(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_include (role_id, included_role_id),
  CONSTRAINT fk_ri_role FOREIGN KEY (role_id) REFERENCES roles(id),
  CONSTRAINT fk_ri_included_role FOREIGN KEY (included_role_id) REFERENCES roles(id)
);

ALTER TABLE roles DROP FOREIGN KEY fk_role_parent;
ALTER TABLE roles DROP COLUMN parent_role_id;
```

### 역할 계층 (DAG)

```
ROLE_SUPER_ADMIN
├── includes → ROLE_SHOPPING_ADMIN
│   └── includes → ROLE_SHOPPING_SELLER
│       └── includes → ROLE_USER
│           └── includes → ROLE_GUEST
└── includes → ROLE_BLOG_ADMIN
    └── includes → ROLE_USER
        └── includes → ROLE_GUEST
```

### JWT Claims

```json
{
  "roles": ["ROLE_SUPER_ADMIN", "ROLE_USER"],
  "effectiveRoles": ["ROLE_SUPER_ADMIN", "ROLE_SHOPPING_ADMIN", "ROLE_BLOG_ADMIN", "ROLE_SHOPPING_SELLER", "ROLE_USER", "ROLE_GUEST"]
}
```

### API 변경

| HTTP | 경로 | 설명 |
|------|------|------|
| GET | `/roles/{roleKey}/includes` | direct includes 조회 |
| POST | `/roles/{roleKey}/includes` | include 추가 (cycle detection) |
| DELETE | `/roles/{roleKey}/includes/{includedRoleKey}` | include 제거 |
| GET | `/roles/{roleKey}/resolved` | effective roles + permissions |
| GET | `/roles/hierarchy` | 전체 DAG 구조 |

### Gateway 변경

```
JWT effectiveRoles claim 존재?
  ├─ YES → 직접 사용 (auth-service API 호출 안함)
  └─ NO (구형 JWT) → RoleHierarchyResolver fallback
```

## References

- ADR-015: Role Hierarchy Implementation
- ADR-011: Hierarchical RBAC Membership System
- ADR-004: JWT RBAC Auto Configuration

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-18 | 초안 작성 (Accepted) | Laze |
