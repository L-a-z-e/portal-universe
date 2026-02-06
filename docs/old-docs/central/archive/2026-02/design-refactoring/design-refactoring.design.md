# Design: design-refactoring

> 전체 프론트엔드 디자인 시스템 점검 및 시각적 품질 개선 설계

## 1. 현황 분석 요약

### 1.1 서비스별 디자인 시스템 활용도

| Service | Framework | DS 활용도 | 커스텀 컴포넌트 | 하드코딩 색상 |
|---------|-----------|----------|---------------|-------------|
| portal-shell | Vue 3 | 높음 | 0개 | 3건 (소셜 SVG, Sidebar) |
| blog-frontend | Vue 3 | 매우 높음 | 0개 | 8건 (CSS 폴백, red 직접 사용) |
| shopping-frontend | React 18 | 매우 높음 | 0개 | 0건 |
| prism-frontend | React 18 | **매우 낮음** | **5개** | **10건+** |

### 1.2 디자인 시스템 패키지 현황

| 패키지 | Vue 컴포넌트 | React 컴포넌트 | 불균형 |
|--------|------------|--------------|--------|
| Form | 8개 | 9개 (Textarea 별도) | 경미 |
| Layout | 3개 | 4개 (Card 포함) | - |
| Navigation | 4개 | 5개 (Pagination) | React만 |
| Feedback | 5개 | 6개 (Progress) | React만 |
| Data Display | 4개 | 6개 (Table, Tooltip, Popover) | React만 |
| **합계** | **27개** | **30개** | React +3 |

---

## 2. 점검 프로세스 설계

### 2.1 Phase 1: 서비스 실행

**실행 순서:**
```
1. docker compose -f docker-compose-local.yml up -d  (infra)
2. api-gateway    → ./gradlew bootRun --args='--spring.profiles.active=local'
3. auth-service   → (이미 실행 중)
4. blog-service   → ./gradlew bootRun --args='--spring.profiles.active=local'
5. shopping-service → ./gradlew bootRun --args='--spring.profiles.active=local'
6. npm run build  (frontend 전체 빌드)
7. npm run dev:portal / dev:blog / dev:shopping / dev:prism
```

### 2.2 Phase 2: Playwright 스크린샷 수집

**수집 대상 매트릭스:**

| Service | Page | URL | Dark | Light |
|---------|------|-----|------|-------|
| **portal-shell** | 홈 (비로그인) | / | O | O |
| | 홈 (로그인/대시보드) | / (logged in) | O | O |
| | 로그인 모달 | / → Login click | O | O |
| | 회원가입 | /signup | O | O |
| | 설정 | /settings | O | O |
| | 서비스 상태 | /status | O | O |
| | 채팅 패널 | / → Chat click | O | O |
| | 프로필 | /my/profile | O | O |
| | 404 | /nonexistent | O | O |
| **blog** | 글 목록 | /blog | O | O |
| | 글 상세 | /blog/posts/:id | O | O |
| | 글 작성 | /blog/write | O | O |
| | 카테고리 | /blog/categories | O | O |
| | 태그 | /blog/tags | O | O |
| | 검색 | /blog/search | O | O |
| | 마이페이지 | /blog/my | O | O |
| **shopping** | 상품 목록 | /shopping | O | O |
| | 상품 상세 | /shopping/products/:id | O | O |
| | 장바구니 | /shopping/cart | O | O |
| | 주문 내역 | /shopping/orders | O | O |
| | 관리자 상품 | /shopping/admin/products | O | O |
| | 관리자 주문 | /shopping/admin/orders | O | O |
| | 타임딜 | /shopping/timedeals | O | O |
| | 쿠폰 | /shopping/coupons | O | O |
| **prism** | 메인 | /prism | O | O |
| | 에이전트 | /prism/agents | O | O |
| | 프로바이더 | /prism/providers | O | O |

**스크린샷 저장:** `docs/pdca/03-analysis/screenshots/design-refactoring/`

### 2.3 Phase 3: 시각적 이슈 식별 기준

#### 점검 체크리스트

**A. 대비(Contrast) 문제**
- [ ] 배경색과 텍스트색 대비 충분한가 (WCAG AA 기준 4.5:1)
- [ ] 버튼 텍스트가 버튼 배경과 구분되는가
- [ ] disabled 상태가 시각적으로 구분되는가
- [ ] placeholder 텍스트가 읽히는가
- [ ] 다크모드에서 모든 텍스트가 보이는가
- [ ] 라이트모드에서 모든 텍스트가 보이는가

**B. 색상 일관성(Color Consistency)**
- [ ] 같은 역할의 요소가 같은 색상인가 (예: primary 버튼 전부 동일)
- [ ] status 색상이 일관적인가 (success=green, error=red, warning=yellow)
- [ ] 서비스별 brand color가 올바르게 적용되는가
- [ ] hover/focus 상태 색상이 일관적인가

**C. 레이아웃/정렬(Layout/Alignment)**
- [ ] 사이드바 아이템 정렬이 맞는가
- [ ] 그리드 간격이 균일한가
- [ ] 카드 크기/높이가 일관적인가
- [ ] 텍스트 정렬이 일관적인가 (좌/중/우)
- [ ] 아이콘과 텍스트 수직 정렬이 맞는가

**D. 간격(Spacing)**
- [ ] 같은 레벨 요소의 padding/margin이 동일한가
- [ ] 섹션 간 간격이 일관적인가
- [ ] 버튼 내부 여백이 충분한가
- [ ] 입력 필드 간격이 균일한가

**E. 타이포그래피(Typography)**
- [ ] 같은 레벨 제목의 폰트 크기가 동일한가
- [ ] 본문 텍스트 크기가 일관적인가
- [ ] 폰트 굵기(weight)가 역할별로 일관적인가
- [ ] line-height가 적절한가 (텍스트 겹침 없음)

**F. 컴포넌트 상태(Component States)**
- [ ] 버튼: default/hover/active/disabled/loading 상태 구분
- [ ] 입력필드: default/focus/error/disabled 상태 구분
- [ ] 링크: default/hover/visited 구분
- [ ] 체크박스/스위치: on/off 시각적 구분

**G. 반응형(Responsive)** — 선택적
- [ ] 1280px (데스크탑)
- [ ] 768px (태블릿)
- [ ] 375px (모바일)

---

## 3. 코드 수정 설계

### 3.1 prism-frontend: 디자인 시스템 전환 (P0)

prism-frontend는 5개 커스텀 컴포넌트를 사용 중이며, 이를 `@portal/design-system-react`로 교체.

**교체 대상:**

| 커스텀 컴포넌트 | 파일 | DS 대체 |
|---------------|------|---------|
| Button | `components/common/Button.tsx` | `@portal/design-system-react` Button |
| Input | `components/common/Input.tsx` | `@portal/design-system-react` Input |
| Select | `components/common/Select.tsx` | `@portal/design-system-react` Select |
| Modal | `components/common/Modal.tsx` | `@portal/design-system-react` Modal |
| Textarea | `components/common/Textarea.tsx` | `@portal/design-system-react` Textarea |

**교체 절차:**
1. 각 커스텀 컴포넌트의 Props 인터페이스 비교
2. DS 컴포넌트로 import 변경
3. Props 매핑 조정 (필요 시)
4. 커스텀 컴포넌트 파일 삭제
5. `prism-500` 등 존재하지 않는 토큰 → semantic 토큰으로 교체

**주의사항:**
- prism-frontend는 `prism-500` 같은 서비스 전용 색상 사용 중
- `design-tokens`에 prism theme이 정의되어 있는지 확인 후, 없으면 추가
- `focus:ring-prism-500` → `focus:ring-brand-primary`로 변환

### 3.2 blog-frontend: CSS 변수 폴백 정리 (P1)

**문제:** `var(--semantic-brand-primary, #20c997)` 형태로 하드코딩 폴백 사용

**수정 방향:**
- 폴백값 제거: `var(--semantic-brand-primary)` 만 사용
- 또는 Tailwind 토큰 클래스로 변환: `text-brand-primary`
- `bg-status-error hover:bg-red-700` 혼합 사용 → `bg-status-error hover:bg-status-error/80`

**대상 파일:**
- `views/SeriesDetailPage.vue` (5건)
- `views/PostDetailPage.vue` (1건)
- `views/StatsPage.vue` (1건: `#dc2626` → `text-status-error`)
- `views/TagDetailPage.vue` (1건)
- `views/TagListPage.vue` (1건)
- `components/SeriesCard.vue` (1건)
- `components/SeriesBox.vue` (4건)
- `components/MySeriesList.vue` (1건)

### 3.3 portal-shell: 사이드바 색상 토큰화 (P2)

**대상:**
- `Sidebar.vue:256` — `bg-red-500 text-white` (ADMIN 배지) → `bg-status-error text-white`
- `Sidebar.vue:302` — `text-red-500 hover:bg-red-500/10` (Logout) → `text-status-error hover:bg-status-error/10`
- `LoginModal.vue:187-223` — 소셜 로그인 SVG 색상은 브랜드 가이드라인 준수이므로 **유지** (수정 불필요)

### 3.4 디자인 시스템 구조 개선 (P2)

**Vue/React 컴포넌트 불균형 해소 검토:**

| React에만 있는 | 필요성 | 결정 |
|---------------|--------|------|
| Pagination | blog에 필요할 수 있음 | Vue 추가 검토 |
| Table | blog/admin에 필요 | Vue 추가 검토 |
| Tooltip | UX 향상 | Vue 추가 검토 |
| Popover | 복잡도 대비 사용처 적음 | 보류 |
| Progress | shopping 전용 | 보류 |

→ **이번 사이클에서는 불균형 문서화만 진행, 실제 컴포넌트 추가는 별도 PDCA로 분리**

### 3.5 Prism 테마 토큰 점검 (P1)

`design-tokens/src/tokens/themes/prism.json` 존재 여부 확인 후:
- 없으면: 기본 prism 테마 토큰 생성 (brand color 정의)
- 있으면: `prism-500` 등이 토큰으로 정의되어 있는지 확인

---

## 4. 이슈 분류 기준

| Priority | 기준 | 예시 | 이번 사이클 |
|----------|------|------|-----------|
| **P0** | 기능/가시성 문제 | 글자 안보임, DS 미사용 (prism) | 필수 해결 |
| **P1** | 시각적 심각 | 대비 부족, 색상 혼합 사용 | 필수 해결 |
| **P2** | 일관성 문제 | 토큰 하드코딩, 정렬 미세 차이 | 가능하면 해결 |
| **P3** | 개선 사항 | DS 불균형, 문서화 | 문서화만 |

---

## 5. 작업 순서 (Do Phase)

```
Task 1: 나머지 서비스 실행
  - blog-service (8082)
  - shopping-service (8083)
  - blog-frontend (30001), shopping-frontend (30002), prism-frontend (30003)

Task 2: Playwright 스크린샷 수집 (Dark mode)
  - portal-shell 모든 페이지
  - blog-frontend 모든 페이지
  - shopping-frontend 모든 페이지
  - prism-frontend 모든 페이지

Task 3: Playwright 스크린샷 수집 (Light mode)
  - 테마 전환 후 동일 페이지 수집

Task 4: 스크린샷 분석 → 이슈 목록 작성
  - 체크리스트 기반 식별
  - 우선순위 분류 (P0~P3)

Task 5: P0 수정 — prism-frontend DS 전환
  - 커스텀 컴포넌트 → @portal/design-system-react
  - prism 테마 토큰 정리

Task 6: P1 수정 — 색상/대비 문제
  - blog CSS 폴백 정리
  - 하드코딩 색상 토큰 전환
  - 스크린샷에서 발견된 대비 문제 수정

Task 7: P2 수정 — 일관성/정렬
  - portal-shell Sidebar 토큰화
  - 간격/정렬 미세 조정

Task 8: 수정 후 재검증
  - 동일 페이지 재스크린샷
  - Before/After 비교
```

---

## 6. 성공 기준

| 항목 | 기준 |
|------|------|
| P0 이슈 | 0개 (전부 해결) |
| P1 이슈 | 0개 (전부 해결) |
| P2 이슈 | 80% 이상 해결 |
| prism-frontend | 커스텀 컴포넌트 0개, DS 활용 |
| 하드코딩 색상 | 전 서비스 0건 (소셜 SVG 제외) |
| 스크린샷 | 전 서비스, 전 페이지, dark/light 수집 |
| 이슈 목록 | 전수 기록 (analysis 문서) |

---

## 7. 파일 변경 예상

### 삭제 예정
```
frontend/prism-frontend/src/components/common/Button.tsx
frontend/prism-frontend/src/components/common/Input.tsx
frontend/prism-frontend/src/components/common/Select.tsx
frontend/prism-frontend/src/components/common/Modal.tsx
frontend/prism-frontend/src/components/common/Textarea.tsx
```

### 수정 예정
```
# prism-frontend (DS 전환)
frontend/prism-frontend/src/pages/AgentsPage.tsx
frontend/prism-frontend/src/pages/ProvidersPage.tsx
frontend/prism-frontend/src/components/kanban/TaskCard.tsx
frontend/prism-frontend/src/components/ErrorBoundary.tsx
(+ prism 커스텀 컴포넌트를 사용하는 모든 파일)

# blog-frontend (CSS 폴백 정리)
frontend/blog-frontend/src/views/SeriesDetailPage.vue
frontend/blog-frontend/src/views/PostDetailPage.vue
frontend/blog-frontend/src/views/StatsPage.vue
frontend/blog-frontend/src/views/TagDetailPage.vue
frontend/blog-frontend/src/views/TagListPage.vue
frontend/blog-frontend/src/components/SeriesCard.vue
frontend/blog-frontend/src/components/SeriesBox.vue
frontend/blog-frontend/src/components/MySeriesList.vue

# portal-shell (토큰화)
frontend/portal-shell/src/components/Sidebar.vue

# design-tokens (prism 테마 확인/추가)
frontend/design-tokens/src/tokens/themes/prism.json (확인 필요)
```

### 생성 예정
```
docs/pdca/03-analysis/screenshots/design-refactoring/  (스크린샷 디렉토리)
```
