# ADR-001: Admin 컴포넌트 구조

**Status**: Accepted
**Date**: 2026-01-17

## Context
E-commerce Admin 기능을 구현하기 위해 shopping-frontend(React)에 상품 관리 UI를 추가해야 합니다. Admin 페이지는 고객용 페이지와 별도의 인터페이스가 필요하며, 상품 CRUD, 데이터 테이블, 폼, 모달 등 여러 UI 컴포넌트와 권한 검증 로직을 포함하고, 향후 재고 관리, 주문 관리 등으로 확장 가능해야 합니다.

## Decision
Admin 페이지는 3계층 컴포넌트 구조(Pages → Containers → UI Components)를 채택합니다.

### 구조 정의
```
Pages (라우트 연결, 페이지 레이아웃)
  ↓
Containers (비즈니스 로직, 상태 관리, API 호출)
  ↓
UI Components (순수 프레젠테이션, Props 기반)
```

## Rationale
- **재사용성**: UI 컴포넌트를 다양한 페이지에서 재사용 가능 (DataTable → 상품/주문/재고 목록)
- **테스트 용이성**: 각 계층을 독립적으로 테스트 (UI는 Props만, Container는 로직만, Pages는 E2E)
- **관심사 분리**: 각 계층의 책임이 명확해 버그 수정 및 디자인 변경 시 영향 범위 최소화
- **확장성**: 새로운 Admin 기능 추가 시 일관된 패턴 유지
- **유지보수성**: 계층별 독립 수정 가능, 버그 위치 빠른 파악

## Trade-offs
✅ **장점**:
- UI 컴포넌트 재사용으로 개발 속도 향상
- 단위/통합/E2E 테스트 전략 수립 용이
- 디자인 변경 시 UI 레이어만 수정

⚠️ **단점 및 완화**:
- 초기 구현 시간 증가 → (완화: 타입스크립트 + 스토리북으로 생산성 보완)
- 파일 구조 복잡도 증가 → (완화: 개발 가이드 문서 제공)
- 팀 학습 곡선 → (완화: Container/Presentational 패턴 교육)

## Implementation
**폴더 구조**:
```
src/
├── components/
│   ├── admin/          # Admin 전용 (ProductTable, ProductForm)
│   ├── common/         # 공용 UI (DataTable, Pagination, SearchInput, Modal, Toast)
│   ├── form/           # 폼 관련 (FormField, Input, Select)
│   └── guards/         # Route Guard (RequireAuth, RequireRole)
├── pages/admin/        # Admin 페이지 (AdminProductListPage, AdminProductFormPage)
└── hooks/              # Container 로직 (useAdminProducts, useConfirm)
```

**핵심 패턴**:
- UI 컴포넌트: Props로 모든 데이터/콜백 수신, 상태 비관리
- Container (Hooks): React Query + React Hook Form + Zod
- Pages: Container 조합 + Layout 구성

## References
- 참고 문서: `/Users/laze/Laze/Project/portal-universe/docs/architecture/admin-product-management.md`
- 패턴: Container/Presentational Component Pattern

---

📂 상세: [old-docs/central/adr/ADR-001-admin-component-structure.md](../old-docs/central/adr/ADR-001-admin-component-structure.md)
