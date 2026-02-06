# ADR-015: Role Hierarchy 구현 방안

## Status
Proposed

## Context
`RoleEntity`에 `parentRole` 필드가 존재하지만, `RbacService`에서 이를 활용하지 않고 있습니다.
현재 권한 검사는 각 역할을 flat하게 비교하는 방식으로 동작합니다.

```java
// RoleEntity.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "parent_role_id")
private RoleEntity parentRole;
```

### 현재 역할 구조
- `ROLE_USER` - 기본 사용자
- `ROLE_SELLER` - 판매자
- `ROLE_BLOG_ADMIN` - 블로그 관리자
- `ROLE_SHOPPING_ADMIN` - 쇼핑 관리자
- `ROLE_SUPER_ADMIN` - 시스템 전체 관리자

### 문제점
1. `ROLE_SUPER_ADMIN`이 `ROLE_SHOPPING_ADMIN`의 권한을 갖지만, 이를 Gateway의 `SecurityConfig`에서 `hasAnyAuthority()`로 수동 나열
2. 새 역할 추가 시 관련된 모든 `hasAnyAuthority()` 호출을 찾아서 수정해야 함
3. `parentRole` 필드가 사용되지 않아 데이터 모델과 실제 동작이 불일치

## Decision
현재 단계에서는 Role Hierarchy를 구현하지 않고, 향후 필요 시 다음 방안을 고려합니다.

### Option A: Spring Security RoleHierarchy (권장)
Spring Security의 `RoleHierarchy` 인터페이스를 활용하여 계층적 권한을 정의합니다.
- DB의 `parentRole` 관계를 기반으로 `RoleHierarchyImpl`을 동적 생성
- Gateway와 Auth-service 모두에 적용 가능

### Option B: DB 기반 Permission 확장
`parentRole`을 탐색하여 상위 역할의 permission을 자동으로 포함합니다.
- 더 세밀한 permission 제어 가능
- 구현 복잡도 높음

## Consequences
- `parentRole` 필드는 현재 상태로 유지
- 새 역할 추가 시 `SecurityConfig`의 `hasAnyAuthority()`에 수동으로 추가 필요
- 역할 수가 증가하면 Option A 구현을 재검토

## References
- ADR-011: Hierarchical RBAC & Membership System
- Spring Security RoleHierarchy: https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html#authz-hierarchical-roles
