# Admin Frontend Phase 4 — API 연동 완성

- **Author**: Laze
- **Date**: 2026-02-18
- **Status**: Implemented
- **Prerequisite**: Phase 1 (HTML→Vue), Phase 2 (Role DAG 백엔드), Phase 3 (Role-Membership 매핑 백엔드)

## Overview

백엔드에 구현되어 있으나 프론트엔드에서 미연동된 API들을 모두 연결하고,
Roles 페이지를 서브 컴포넌트로 분리하여 유지보수성을 높였다.

## Changes Summary

### 1. DTO + API Functions

신규 타입/함수 추가 (`dto/admin.ts`, `api/admin.ts`):

| Type/Function | Endpoint | Description |
|---------------|----------|-------------|
| `RoleDefaultMappingResponse` | - | Role-Default 매핑 응답 DTO |
| `RoleDefaultMappingRequest` | - | Role-Default 매핑 요청 DTO |
| `CreateMembershipTierRequest` | - | Tier 생성 요청 DTO |
| `fetchAllRoleDefaults()` | `GET /admin/memberships/role-defaults` | 전체 매핑 조회 |
| `fetchRoleDefaults(roleKey)` | `GET /admin/memberships/role-defaults/{roleKey}` | 역할별 매핑 조회 |
| `addRoleDefault(data)` | `POST /admin/memberships/role-defaults` | 매핑 추가 |
| `removeRoleDefault(roleKey, group)` | `DELETE /admin/memberships/role-defaults/{roleKey}/{group}` | 매핑 삭제 |
| `createMembershipTier(data)` | `POST /admin/memberships/tiers` | Tier 생성 |
| `deleteMembershipTier(tierId)` | `DELETE /admin/memberships/tiers/{tierId}` | Tier 삭제 |

### 2. RolesPage Component Decomposition

560줄 단일 파일 → 4개 모듈로 분리 (동작 변경 없음):

```
src/composables/useRoleManagement.ts  ← 상태 + CRUD 액션
src/components/roles/
  RoleListPanel.vue                    ← 좌측 역할 목록 + 검색
  RoleDetailPanel.vue                  ← 우측 상세 (header, permissions, includes, info)
  RoleCreateForm.vue                   ← 역할 생성 폼
```

`RolesPage.vue`는 레이아웃 orchestrator (~120줄).

### 3. DAG Visualization

커스텀 SVG 기반 (외부 라이브러리 없음):

```
src/composables/useRoleDag.ts          ← adjacency list → BFS 레벨 → x/y 좌표
src/components/roles/RoleDagView.vue   ← SVG 렌더링, 노드 클릭 → 역할 선택
```

- `fetchRoleHierarchy()` API 호출
- BFS 레벨 할당 → 수평 분배 → Bezier curve edge
- 접기/펼치기 토글
- 선택된 역할 하이라이트

### 4. Resolved Permissions

```
src/components/roles/ResolvedPermissionsTable.vue
```

- `fetchResolvedRole(roleKey)` 호출
- Own permissions와 비교하여 `Own`/`Inherited` 구분 표시
- Role Detail Panel 내 Effective Roles 아래에 배치

### 5. Role-Default Memberships (Read-only, Roles Page)

```
src/components/roles/RoleDefaultMappings.vue
```

- `fetchRoleDefaults(roleKey)` 호출
- `membershipGroup → defaultTierKey` 태그 형태로 읽기 전용 표시
- Role Detail Panel 내 Info 위에 배치

### 6. Tier Create/Delete (Memberships Page)

- **Add Tier**: 헤더에 "Add Tier" 버튼, Modal form (tierKey, displayName, prices, sortOrder)
- **Delete Tier**: Detail 패널에 "Delete" 버튼 + 확인 Alert

### 7. Role-Default Mapping CRUD (Memberships Page)

```
src/components/memberships/RoleDefaultMappingSection.vue
```

- 전체 매핑 테이블 (roleKey | membershipGroup | defaultTierKey | Actions)
- 추가 폼: Select(role) + Select(group) + Select(tier) → Add
- Memberships 페이지 하단 별도 섹션

### 8. E2E Tests

`e2e-tests/tests/admin-frontend/roles.spec.ts`:
- 7 tests: page load, search filter, detail panel, create role, system role protection, DAG visualization, resolved permissions

`e2e-tests/tests/admin-frontend/memberships.spec.ts`:
- 6 tests: page load, group/tier selection, tier detail, tier edit, tier create, tier delete

## File Structure

```
frontend/admin-frontend/src/
├── api/admin.ts               (updated: +6 functions)
├── dto/admin.ts               (updated: +3 interfaces)
├── composables/
│   ├── useRoleManagement.ts   (new)
│   └── useRoleDag.ts          (new)
├── components/
│   ├── roles/
│   │   ├── RoleListPanel.vue          (new)
│   │   ├── RoleDetailPanel.vue        (new)
│   │   ├── RoleCreateForm.vue         (new)
│   │   ├── RoleDagView.vue            (new)
│   │   ├── ResolvedPermissionsTable.vue (new)
│   │   └── RoleDefaultMappings.vue    (new)
│   └── memberships/
│       └── RoleDefaultMappingSection.vue (new)
└── views/
    ├── RolesPage.vue          (rewritten: orchestrator)
    └── MembershipsPage.vue    (updated: create/delete/mapping)
```
