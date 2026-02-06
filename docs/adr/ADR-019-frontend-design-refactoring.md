# ADR-019: Frontend Design Refactoring - 일관된 컴포넌트 사용

**Status**: Accepted
**Date**: 2026-02-01
**Source**: PDCA archive (frontend-design-refactoring)

## Context

3개 소비자 앱(portal-shell, blog-frontend, shopping-frontend)에서 design-system 컴포넌트 사용이 비일관적이었다. shopping-frontend는 Button, Input, Modal 등을 design-system과 중복 구현하여 사용했고, portal-shell/blog-frontend는 여전히 raw HTML `<button>`, `<input>`을 다수 사용 중이었다. 이로 인해 UI 일관성 저하, 유지보수 이중화, design-system 개선 사항 미반영 등의 문제가 발생했다.

## Decision

**Raw HTML을 design-system 컴포넌트로 전면 교체**하고, **중복 구현 컴포넌트를 제거**한다.

## Rationale

- **중복 제거 우선**: shopping-frontend의 Button, Input, Textarea, ConfirmModal, Pagination을 design-system-react로 교체
- **Props 호환성 확보**: 로컬 컴포넌트의 `error: string` → DS의 `error: boolean + errorMessage: string`로 마이그레이션 패턴 정의
- **ConfirmModal은 인터페이스 유지**: DS Modal을 래핑하여 기존 Props 그대로 사용 가능하도록 재작성
- **portal-shell Switch/Checkbox 도입**: raw toggle → Switch, raw checkbox → Checkbox로 교체
- **blog-frontend Tabs/Select 도입**: raw 탭 버튼 → Tabs, raw select → Select로 교체
- **단계적 적용**: Phase 1(CRITICAL) → Phase 6까지 우선순위별 진행, 각 Phase마다 빌드 검증

## Trade-offs

✅ **장점**:
- 3개 앱의 UI 일관성 확보 (동일한 Button, Input 사용)
- design-system 개선 사항 자동 전파 (중앙 집중식)
- 중복 코드 제거로 유지보수 포인트 감소
- 접근성(a11y) 자동 개선 (design-system이 ARIA 지원)

⚠️ **단점 및 완화**:
- 대량 변경(~46개 파일)으로 regression 위험 → Phase별 빌드 검증 + Playwright 시각적 검증
- Props 불일치로 인한 빌드 실패 → Props 호환성 매핑표 사전 작성, 패턴화된 변환
- Module Federation에서 새 컴포넌트 shared 이슈 → design-system은 alias 해석으로 동작하므로 추가 설정 불필요

## Implementation

- **Phase 1**: `shopping-frontend/components/` - Button, Input, Textarea, Pagination 삭제, ConfirmModal 재작성
- **Phase 2**: `portal-shell/` - Sidebar, MyProfilePage, SettingsPage, QuickActions, CallbackPage, ForbiddenPage 리팩토링
- **Phase 4**: `blog-frontend/` - PostListPage, MyPage, PostWritePage (Tabs, Select 도입)
- **Phase 7**: Playwright 12개 URL 시각적 검증 (`browser_snapshot`, `browser_take_screenshot`)
- **Props 변환**: `error={!!formErrors.email} errorMessage={formErrors.email}` 패턴 일괄 적용

## References

- PDCA: `pdca/archive/2026-02/frontend-design-refactoring/`
- 변경 규모: 42개 파일 수정, 4개 파일 삭제, 7개 신규 컴포넌트 import
- E2E 검증: Playwright 12개 주요 URL
