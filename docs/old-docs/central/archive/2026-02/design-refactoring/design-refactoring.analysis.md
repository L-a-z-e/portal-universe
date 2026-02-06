# Analysis: design-refactoring

> 스크린샷 기반 시각적 이슈 분석 결과

## 1. 스크린샷 수집 현황

| Service | Pages | Dark | Light | Total |
|---------|-------|------|-------|-------|
| portal-shell | 4 (home, settings, status, 404) | 4 | 4 | 8 |
| blog | 6 (list, write, categories, tags, search, my) | 6 | 6 | 12 |
| shopping | 7 (list, cart, orders, timedeals, coupons, admin-products, admin-orders) | 7 | 7 | 14 |
| prism | 3 (main, agents, providers) | 3 | 3 | 6 |
| **Total** | **20** | **20** | **20** | **40** |

---

## 2. 이슈 목록

### P0: 기능/가시성 문제 (필수 해결)

| ID | Service | Page | Theme | Issue | Detail |
|----|---------|------|-------|-------|--------|
| P0-01 | prism | all | both | 커스텀 컴포넌트 사용 | Button, Input, Select, Modal, Textarea 5개 커스텀 컴포넌트가 DS 대신 사용됨. `focus:ring-prism-500` 등 존재하지 않는 토큰 참조 |
| P0-02 | prism | agents | dark | 에러 배너 배경 대비 부족 | "Request failed with status code 404" 배너가 밝은 회색 배경인데 dark mode 본문 배경과 대비가 지나치게 높아 눈에 거슬림. DS Alert 컴포넌트 미사용 |
| P0-03 | prism | all | both | 하드코딩 색상 10건+ | `bg-indigo-600`, `text-purple-500`, `border-gray-700` 등 Tailwind 기본 색상 직접 사용. semantic 토큰 미사용 |

### P1: 시각적 심각 문제 (필수 해결)

| ID | Service | Page | Theme | Issue | Detail |
|----|---------|------|-------|-------|--------|
| P1-01 | blog | series 관련 | both | CSS 변수 폴백 하드코딩 | `var(--semantic-brand-primary, #20c997)` 형태로 8개 파일에서 폴백값 하드코딩 |
| P1-02 | blog | StatsPage | both | 하드코딩 색상 | `#dc2626` 직접 사용 → `text-status-error`로 변환 필요 |
| P1-03 | blog | series | both | hover 색상 혼합 | `bg-status-error hover:bg-red-700` 혼합 사용 → `hover:bg-status-error/80`로 통일 필요 |
| P1-04 | prism | main | both | "+ New Board" 버튼 색상 | `bg-indigo-600 hover:bg-indigo-700` 하드코딩 → `bg-brand-primary hover:bg-brand-primary/90` 필요 |
| P1-05 | prism | agents/providers | both | "+ New Agent/Provider" 버튼 | 동일한 하드코딩 색상 문제 |

### P2: 일관성 문제 (가능하면 해결)

| ID | Service | Page | Theme | Issue | Detail |
|----|---------|------|-------|-------|--------|
| P2-01 | portal-shell | sidebar | both | ADMIN 배지 하드코딩 | `bg-red-500 text-white` → `bg-status-error text-white` |
| P2-02 | portal-shell | sidebar | both | Logout 색상 하드코딩 | `text-red-500 hover:bg-red-500/10` → `text-status-error hover:bg-status-error/10` |
| P2-03 | prism | sidebar | dark | 비활성 메뉴 대비 | Boards/Agents/Providers 비활성 상태 텍스트가 어두운 회색으로 가독성 낮음 |
| P2-04 | portal-shell | home | both | 서비스 카드 호버 효과 일관성 | Blog/Shopping/Prism 카드의 호버 효과가 미세하게 다를 수 있음 |

### P3: 개선 사항 (문서화만)

| ID | Service | Issue | Detail |
|----|---------|-------|--------|
| P3-01 | design-system | Vue/React 불균형 | React에만 Pagination, Table, Tooltip, Popover, Progress 존재 |
| P3-02 | all | 반응형 테스트 미실시 | 768px, 375px 뷰포트 테스트는 이번 사이클에서 제외 |
| P3-03 | prism | prism 테마 토큰 미정의 | design-tokens에 prism theme 없음 (blog, shopping은 있음) |

---

## 3. 서비스별 요약

### portal-shell (DS 활용도: 높음)
- **양호**: 전반적으로 semantic 토큰 사용, dark/light 전환 정상
- **이슈**: Sidebar ADMIN 배지와 Logout에 하드코딩 색상 2건 (P2)
- **참고**: 소셜 로그인 SVG 색상은 브랜드 가이드라인이므로 수정 불필요

### blog-frontend (DS 활용도: 매우 높음)
- **양호**: DS 컴포넌트 전면 사용, 서비스 테마(teal) 정상 적용
- **이슈**: CSS 변수 폴백 하드코딩 8개 파일 (P1), 일부 Tailwind 색상 혼합 사용
- **특이사항**: 글 카드 레이아웃/간격 일관적, dark/light 전환 정상

### shopping-frontend (DS 활용도: 매우 높음)
- **양호**: DS 전면 사용, 하드코딩 0건, 서비스 테마(orange) 정상 적용
- **이슈**: 없음
- **특이사항**: 가장 우수한 DS 활용 사례, pagination/admin 페이지 모두 정상

### prism-frontend (DS 활용도: 매우 낮음)
- **심각**: 5개 커스텀 컴포넌트, 10건+ 하드코딩 색상 (P0)
- **이슈**: DS 컴포넌트 미사용이 전면적, 에러 표시도 커스텀
- **특이사항**: prism-service가 미실행(404)이므로 데이터 표시 상태는 확인 불가

---

## 4. 수정 우선순위 및 예상 범위

### Phase 1: P0 (prism-frontend DS 전환)
- 커스텀 컴포넌트 5개 삭제 → `@portal/design-system-react` 교체
- 하드코딩 색상 → semantic 토큰 전환
- 에러 배너 → DS Alert 컴포넌트 교체
- **파일 수**: ~15개 수정, 5개 삭제

### Phase 2: P1 (blog 색상 정리)
- CSS 변수 폴백값 제거 또는 Tailwind 클래스 전환
- `#dc2626` → `text-status-error`
- `hover:bg-red-700` → `hover:bg-status-error/80`
- **파일 수**: 8개 수정

### Phase 3: P2 (portal-shell + prism 세부 조정)
- Sidebar ADMIN 배지/Logout 토큰화
- Prism 비활성 메뉴 대비 개선
- **파일 수**: 2개 수정

---

## 5. 검증 기준

| 항목 | Before | Target |
|------|--------|--------|
| P0 이슈 | 3건 | 0건 |
| P1 이슈 | 5건 | 0건 |
| P2 이슈 | 4건 | 80%+ 해결 (3건 이상) |
| prism 커스텀 컴포넌트 | 5개 | 0개 |
| 하드코딩 색상 (전체) | 20건+ | 0건 (소셜 SVG 제외) |
