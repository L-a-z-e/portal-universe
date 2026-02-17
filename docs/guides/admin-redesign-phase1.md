# Admin Redesign Phase 1

- **Author**: Laze
- **Date**: 2026-02-17
- **Status**: Implemented

## Overview

admin-frontend를 Monochrome Chic 디자인 시스템으로 리디자인.
6개 Stitch HTML 프로토타입 기반 Vue 3 SFC 변환.

## Design System Changes

### Monochrome Chic Palette

`design-core/tokens/base/colors.json`에 `monochromeChic` 팔레트 추가:

| Scale | Hex     | Usage           |
|-------|---------|-----------------|
| 900   | #2d232e | Sidebar bg      |
| 800   | #3a2f3b | Dark accents    |
| 700   | #474448 | Body text       |
| 600   | #534b52 | Meta text       |
| 500   | #6b5b6e | Brand primary   |
| 400   | #8a7b8d | Brand secondary |
| 300   | #a09a9e | Sidebar text    |
| 200   | #e0ddcf | Muted bg        |
| 100   | #ede9e1 | Hover bg        |
| 50    | #f1f0ea | Page bg         |

### Admin Theme Token Changes

`admin.json`의 모든 semantic 토큰을 Indigo에서 Monochrome Chic으로 교체:
- **Brand**: indigo.400/500/300 → monochromeChic.500/600/400
- **Text**: gray 기반 → monochromeChic 기반
- **Background**: neutral.white → monochromeChic.50 (#f1f0ea)
- **Status**: 순색 계열 → 채도 낮은 muted 톤 (#4a7c59, #a3453a 등)

### Sidebar Tokens

Admin sidebar는 light mode에서도 dark background (#2d232e) 사용.
`sidebar.text`, `sidebar.textActive`, `sidebar.bgActive`, `sidebar.accent` 토큰 추가.

Tailwind preset에 `sidebar.*` 색상 매핑 추가.

### CSS Components

`admin.css`에 추가/갱신된 컴포넌트 스타일:
- `.stat-card`: KPI 카드 (아이콘, 트렌드, 경고 변형)
- `.admin-timeline`: 수직 타임라인
- `.admin-table-expandable`: 확장 가능 행
- `.admin-nav-item`: 다크 사이드바 네비게이션
- `.admin-filter-bar`: 필터 바
- `.event-badge--*`: 이벤트 타입별 컬러 배지
- `.admin-progress`: 프로그레스 바
- `.admin-section-label`: 사이드바 섹션 구분

## Page Changes

### AdminLayout.vue
- 다크 사이드바 (#2d232e 배경, sidebar 전용 토큰)
- Material Symbols 아이콘 추가
- 네비게이션 섹션 구분 (Main / System)
- 사용자 프로필 + Sign Out 하단 배치
- Embedded 모드: 사이드바 숨김 유지

### DashboardPage.vue
- KPI 카드 4개: 아이콘, 트렌드 표시, 경고 스타일 (Pending)
- Recent Activity: 테이블 → 수직 타임라인 + 이벤트 배지
- Role Distribution: 프로그레스 바
- Membership Groups: admin-table 스타일

### UsersPage.vue
- 테이블 → Master-Detail Split (w-2/5 | flex-1)
- 리스트: 아바타 + 상태 인디케이터 + 마지막 활동 시간
- 상세 패널: 프로필 헤더, Assigned Roles (태그+X), Effective Permissions (체크), Memberships (카드)

### RolesPage.vue
- 테이블 → 좌측 리스트 (w-80) + 우측 상세
- 리스트: active/inactive 뱃지, system 뱃지
- 상세: Own Permissions 목록 + 추가/삭제, Role Info

### MembershipsPage.vue
- 그룹 탭: 버튼 → 탭 스타일 (border-bottom)
- 확장 가능 행: 클릭하면 tier 상세 표시
- Phase 1: 읽기 전용 안내

### SellerApprovalsPage.vue
- 필터 바: Status 드롭다운 + Sort 드롭다운
- 확장 행: Applicant Details + Application Reason + Review Comments textarea
- `prompt()` 제거 → 인라인 textarea 리뷰

### AuditLogPage.vue
- 필터 바: Actor 검색 + Event Type 드롭다운 + Export CSV
- 2열 레이아웃: Timestamp | Event Detail (배지 + actor→target + 설명)

## Migration Notes

- Dark mode는 기존 Linear-style 유지, brand만 monochromeChic으로 교체
- 기존 API 호출/DTO 구조 변경 없음
- 라우터 구조 변경 없음
