---
id: arch-admin-frontend
title: Admin Frontend System Overview
type: architecture
status: current
created: 2026-02-07
updated: 2026-02-07
author: Laze
tags: [architecture, vue, admin, rbac, membership, module-federation]
---

# Admin Frontend System Overview

## 개요

Admin Frontend는 Portal Universe의 RBAC(Role-Based Access Control) 및 멤버십 관리를 위한 관리자 전용 대시보드입니다. Vue 3 기반 Module Federation Remote 앱으로, portal-shell(Host)에 embedded되어 실행되며, standalone 모드도 지원합니다.

### 핵심 역할

- **사용자 관리**: email/username/nickname 검색, 페이지네이션 목록, 역할 조회/할당/해제, 권한 확인
- **역할 관리**: 역할 CRUD(생성/조회/수정/활성화·비활성화), 역할-권한 할당/해제, 권한 목록 조회
- **멤버십 관리**: 그룹별 티어 설정, 사용자 멤버십 변경
- **판매자 승인**: 판매자 신청 목록 조회, 승인/거부
- **감사 로그**: RBAC 이벤트 로그 조회 및 필터링

---

## 핵심 특징

- **Module Federation Remote**: portal-shell Host에 `./bootstrap` expose
- **Embedded/Standalone 듀얼 모드**: `window.__POWERED_BY_PORTAL_SHELL__` 플래그로 분기
- **Admin Role 제한**: portal-shell Sidebar에서 `authStore.isAdmin` 체크로 메뉴 조건부 표시
- **Portal Shell 인증 공유**: 별도 로그인 없음, portal-shell의 `portal/api` (토큰 갱신, 401 재시도) 사용
- **Portal Shell 스토어 공유**: `portal/stores` (useAuthStore, useThemeStore) MF 공유

---

## 기술 스택

| 영역 | 기술 |
|------|------|
| Framework | Vue 3.5 (Composition API) |
| Build | Vite 7 + @originjs/vite-plugin-federation |
| Language | TypeScript 5.9 (strict) |
| State | Pinia (portal-shell 공유) |
| Routing | Vue Router 4 (Memory History: embedded, Web History: standalone) |
| HTTP | portal/api apiClient (MF 공유) |
| UI Components | @portal/design-system-vue (Card, Button, Badge, Spinner, Alert, SearchBar, Select, Avatar) |
| Styling | Tailwind CSS 3 + @portal/design-tokens |
| Port | 30004 |

---

## 디렉토리 구조

```
admin-frontend/
├── src/
│   ├── bootstrap.ts       # mountAdminApp() - MF 진입점
│   ├── main.ts            # Embedded/Standalone 분기
│   ├── App.vue            # data-service="admin", 테마 동기화
│   ├── style.css          # Design system + Tailwind directives
│   ├── api/
│   │   ├── index.ts       # portal/api re-export
│   │   └── admin.ts       # Admin API 함수 (fetchRoles, assignRole 등)
│   ├── dto/
│   │   └── admin.ts       # Admin DTO 타입 정의
│   ├── layouts/
│   │   └── AdminLayout.vue # 사이드바 (standalone) + router-view
│   ├── router/
│   │   └── index.ts       # 듀얼 라우터 (Memory/Web History)
│   ├── types/
│   │   └── federation.d.ts # portal/api, portal/stores 모듈 선언
│   └── views/
│       ├── DashboardPage.vue
│       ├── UsersPage.vue
│       ├── RolesPage.vue
│       ├── MembershipsPage.vue
│       ├── SellerApprovalsPage.vue
│       └── AuditLogPage.vue
├── index.html
├── vite.config.ts         # Federation 설정 (name: 'admin')
├── tailwind.config.js
├── tsconfig.json
└── package.json
```

---

## Design System 통합

`@portal/design-system-vue` 컴포넌트를 사용하여 UI 일관성을 보장합니다.

### 사용 컴포넌트

| 컴포넌트 | 용도 | 사용 페이지 |
|----------|------|------------|
| `Card` (outlined) | 섹션 컨테이너, KPI 카드 | Dashboard, Memberships |
| `Button` (outline, ghost, primary) | 액션, 네비게이션, 그룹 탭 | 전체 |
| `Badge` (default, info, success, danger) | 이벤트 타입, 티어/상태 표시 | Dashboard, AuditLog, Memberships |
| `Spinner` | 로딩 상태 | 전체 |
| `Alert` (error, success) | 에러/성공 상태 표시 | 전체 |
| `Input` | 역할 생성/수정 폼 | Roles |
| `Select` (searchable, clearable) | 권한 할당, 부모 역할 선택, 티어 변경 | Roles, Memberships |
| `SearchBar` | 사용자 검색 | Users, Memberships |
| `SearchBar` | 역할 검색 | Roles |
| `Select` (searchable) | 역할 선택 드롭다운 | Users |
| `Avatar` | 사용자 프로필 이미지/이니셜 | Users |

### 미사용 (design-system 미구현)

| 요소 | 대안 |
|------|------|
| Table | `<table>` + Tailwind (Table 컴포넌트 미제공) |
| Typography (h1~h6) | Tailwind 클래스 직접 사용 |

---

## Module Federation 구성

```
portal-shell (Host, Vue 3)
├── blog-frontend (Remote, Vue 3)
├── shopping-frontend (Remote, React 18)
├── prism-frontend (Remote, React 18)
└── admin-frontend (Remote, Vue 3)    ← 이 서비스
```

### Expose / Consume

| 방향 | 모듈 | 설명 |
|------|------|------|
| Expose | `./bootstrap` | `mountAdminApp()` 함수 |
| Consume | `portal/api` | apiClient (토큰 갱신, 401/429 재시도) |
| Consume | `portal/stores` | useAuthStore, useThemeStore |

### Shared Dependencies

```
vue, pinia, axios
```

---

## 인증 플로우

```
[Portal Shell 로그인] → JWT 발급 → authStore에 저장
                                      ↓
[Sidebar] → authStore.isAdmin 확인 → Admin 메뉴 표시
                                      ↓
[Admin Remote 로드] → portal/api (Bearer 토큰 자동 첨부)
                                      ↓
                      401 응답 → portal/api 자동 refresh
                                      ↓
                      실패 → portal-shell 로그인 리다이렉트
```

### Standalone 모드 인증

Standalone 실행 시 별도 로그인 플로우는 미구현. portal-shell embedded 모드에서만 인증이 동작합니다.

---

## 라우팅

### Embedded (Portal Shell 내부)

| Path | Page |
|------|------|
| `/admin` | DashboardPage |
| `/admin/users` | UsersPage |
| `/admin/roles` | RolesPage |
| `/admin/memberships` | MembershipsPage |
| `/admin/seller-approvals` | SellerApprovalsPage |
| `/admin/audit-log` | AuditLogPage |

### Standalone (독립 실행)

| Path | Page |
|------|------|
| `/` | DashboardPage |
| `/users` | UsersPage |
| `/roles` | RolesPage |
| `/memberships` | MembershipsPage |
| `/seller-approvals` | SellerApprovalsPage |
| `/audit-log` | AuditLogPage |

---

## API 엔드포인트 의존성

| Page | API Endpoint | Method |
|------|-------------|--------|
| Dashboard | `/admin/rbac/roles` | GET |
| Users | `/admin/rbac/users` | GET (검색, 페이지네이션) |
| Users | `/admin/rbac/users/{id}/roles` | GET |
| Users | `/admin/rbac/users/{id}/permissions` | GET |
| Users | `/admin/rbac/roles/assign` | POST |
| Users | `/admin/rbac/users/{id}/roles/{roleKey}` | DELETE |
| Roles | `/admin/rbac/roles` | GET (목록) |
| Roles | `/admin/rbac/roles/{roleKey}` | GET (상세+권한) |
| Roles | `/admin/rbac/roles` | POST (생성) |
| Roles | `/admin/rbac/roles/{roleKey}` | PUT (수정) |
| Roles | `/admin/rbac/roles/{roleKey}/status` | PATCH (활성/비활성) |
| Roles | `/admin/rbac/roles/{roleKey}/permissions` | GET/POST (권한 조회/할당) |
| Roles | `/admin/rbac/roles/{roleKey}/permissions/{pKey}` | DELETE (권한 해제) |
| Roles | `/admin/rbac/permissions` | GET (전체 권한 목록) |
| Memberships | `/admin/memberships/groups` | GET (그룹 목록) |
| Memberships | `/memberships/tiers/{group}` | GET (그룹별 티어) |
| Memberships | `/admin/memberships/users/{id}` | GET/PUT (조회/변경) |
| Memberships | `/admin/rbac/users` | GET (사용자 검색 재활용) |
| Seller Approvals | `/admin/seller/applications/pending` | GET |
| Seller Approvals | `/admin/seller/applications/{id}/review` | POST |
| Audit Log | `/admin/rbac/audit` | GET |
| Audit Log | `/admin/rbac/users/{id}/audit` | GET |

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-07 | 초기 문서 작성 (React Standalone) | Laze |
| 2026-02-07 | Vue 3 MF Remote 전환 반영 | Laze |
| 2026-02-07 | design-system-vue 컴포넌트 통합 반영 | Laze |
| 2026-02-07 | UsersPage 검색/목록/상세 기능 추가, SearchBar/Select/Avatar 컴포넌트 통합 | Laze |
| 2026-02-07 | RolesPage CRUD 기능 추가 (역할 생성/수정/활성화, 권한 관리), Input/Select/SearchBar 컴포넌트 통합 | Laze |
| 2026-02-07 | MembershipsPage 전면 재작성: design-system 컴포넌트 통합, 사용자 검색, 그룹별 멤버십 카드, 티어 변경 | Laze |
