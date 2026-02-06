# Plan: design-refactoring

> 전체 프론트엔드 서비스 디자인 시스템 점검 및 시각적 품질 개선

## 1. 개요

### 배경
portal-universe는 4개 프론트엔드 서비스(portal-shell, blog, shopping, prism)가 Module Federation으로 통합된 구조. 공통 디자인 시스템(`design-tokens`, `design-types`, `design-system-vue`, `design-system-react`)이 존재하지만, 각 서비스의 실제 화면에서 디자인 일관성/품질에 대한 체계적인 점검이 이루어지지 않았음.

### 목표
1. **시각적 품질 점검**: Playwright로 전 서비스 스크린샷 → 디자인 이슈 전수 식별
2. **디자인 시스템 구조 점검**: 4개 디자인 패키지 구성의 적절성 검토
3. **컴포넌트 사용 일관성**: 공통 컴포넌트 미사용/오사용 식별
4. **이슈 수정**: 식별된 모든 문제 해결

### 범위

| 영역 | 대상 |
|------|------|
| **프론트엔드 서비스** | portal-shell, blog-frontend, shopping-frontend, prism-frontend |
| **디자인 시스템** | design-tokens, design-types, design-system-vue, design-system-react |
| **점검 환경** | Local (docker-compose-local.yml + gradlew bootRun + npm run dev) |
| **도구** | Playwright (스크린샷 + 접근성 스냅샷) |

---

## 2. 점검 항목

### Phase 1: 서비스 실행 및 스크린샷 수집

모든 서비스를 local 환경에서 실행하고 각 화면의 스크린샷을 수집한다.

**실행 대상:**

| Service | Type | Port | Command |
|---------|------|------|---------|
| Infra (MySQL, Redis, Kafka) | Docker | - | `docker compose -f docker-compose-local.yml up -d` |
| api-gateway | Spring Boot | 8080 | `./gradlew bootRun --args='--spring.profiles.active=local'` |
| auth-service | Spring Boot | 8081 | `./gradlew bootRun --args='--spring.profiles.active=local'` |
| blog-service | Spring Boot | 8082 | `./gradlew bootRun --args='--spring.profiles.active=local'` |
| shopping-service | Spring Boot | 8083 | `./gradlew bootRun --args='--spring.profiles.active=local'` |
| portal-shell | Vite (Vue) | 30000 | `npm run dev:portal` |
| blog-frontend | Vite (Vue) | 30001 | `npm run dev:blog` |
| shopping-frontend | Vite (React) | 30002 | `npm run dev:shopping` |
| prism-frontend | Vite (React) | 30003/30004 | `npm run dev:prism` |

**스크린샷 수집 대상 (페이지별):**

| Service | Pages |
|---------|-------|
| portal-shell | 홈(비로그인), 홈(로그인), 로그인 모달, 설정, 상태, 채팅 패널 |
| blog | 글 목록, 글 상세, 글 작성/편집, 카테고리, 검색 |
| shopping | 상품 목록, 상품 상세, 장바구니, 주문/결제, 주문 내역, 위시리스트 |
| prism | 메인 페이지, 에이전트 실행 |

각 페이지에서 **Dark mode + Light mode** 양쪽 스크린샷 수집.

### Phase 2: 시각적 이슈 식별

스크린샷 + 접근성 스냅샷 분석으로 다음 문제들을 식별:

| Category | 점검 항목 | 예시 |
|----------|----------|------|
| **대비 문제** | 배경-텍스트 대비 부족 | 흰 배경에 흰 글씨, 어두운 배경에 어두운 버튼 |
| **색상 일관성** | 서비스 내 색상 통일성 | 같은 역할의 버튼인데 다른 색상 |
| **정렬 문제** | 요소 간 정렬 불일치 | 사이드바 아이콘 들쭉날쭉, 그리드 불균형 |
| **간격 문제** | 패딩/마진 불일치 | 같은 레벨 카드의 여백이 다름 |
| **타이포그래피** | 폰트 크기/굵기 불일치 | 같은 레벨 제목인데 다른 폰트 크기 |
| **반응형** | 레이아웃 깨짐 | 좁은 화면에서 요소 겹침/잘림 |
| **다크/라이트** | 테마 전환 시 깨짐 | 다크모드에서 보이지 않는 요소 |
| **상태 표시** | hover/focus/disabled | disabled 버튼이 구분 안됨 |
| **아이콘/이미지** | 크기/정렬 이상 | 아이콘 크기 불균형, 이미지 비율 깨짐 |

### Phase 3: 디자인 시스템 구조 점검

**3-1. 패키지 구성 적절성:**

| 패키지 | 점검 항목 |
|--------|----------|
| **design-tokens** | 토큰 계층 (base → semantic → theme) 적절한지, 누락된 토큰 |
| **design-types** | Vue/React 공통 타입 정의 완전한지, 누락된 Props |
| **design-system-vue** | 27개 컴포넌트 구성, 누락된 컴포넌트, Vue 전용 기능 |
| **design-system-react** | 30개 컴포넌트 구성, Vue와의 불균형(Pagination, Tooltip, Popover, Table, Progress 등 React에만 존재) |

**3-2. 프레임워크 간 동등성:**

| Vue Only | React Only | 공통 |
|----------|-----------|------|
| - | Pagination | Button, Input, Select, Checkbox, Radio, Switch |
| - | Tooltip | Alert, Spinner, Skeleton, Badge, Tag, Avatar |
| - | Popover | Card, Container, Stack, Divider |
| - | Table | Tabs, Breadcrumb, Link, Dropdown |
| - | Progress | Toast, Modal, FormField, SearchBar |
| - | Textarea(별도) | - |

### Phase 4: 컴포넌트 사용 점검

각 프론트엔드 서비스에서:

1. **공통 컴포넌트 미사용**: 디자인 시스템에 있는데 직접 만들어 쓰는 경우
2. **불필요한 커스텀 컴포넌트**: 공통화 가능한데 서비스 내부에 중복 구현
3. **import 누락**: `@portal/design-system-*` 대신 직접 HTML/CSS로 작성
4. **토큰 미사용**: 하드코딩된 색상/간격 값 (예: `#333` 대신 `text-body` 사용해야)
5. **타입 미사용**: `@portal/design-types` 미참조

### Phase 5: 이슈 수정

식별된 이슈를 우선순위별로 분류하고 수정:

| 우선순위 | 기준 | 예시 |
|---------|------|------|
| **P0 - Critical** | 사용 불가 | 글자 안 보임, 버튼 클릭 불가 |
| **P1 - High** | 시각적 심각 | 배경-텍스트 대비 부족, 레이아웃 깨짐 |
| **P2 - Medium** | 일관성 문제 | 색상 불일치, 간격 불균형 |
| **P3 - Low** | 개선 사항 | 미세 정렬, 토큰 하드코딩 |

---

## 3. 기술 접근

### Playwright 활용 방식

```
1. browser_navigate → 각 서비스 페이지
2. browser_snapshot → 접근성 트리 분석 (구조적 이슈)
3. browser_take_screenshot → 시각적 분석 (색상, 대비, 정렬)
4. browser_evaluate → 테마 전환 (dark ↔ light)
5. 반복: 모든 페이지 × 모든 테마
```

### 스크린샷 저장 경로

```
docs/pdca/03-analysis/screenshots/design-refactoring/
├── portal-shell/
│   ├── home-dark.png
│   ├── home-light.png
│   ├── login-modal.png
│   └── ...
├── blog/
│   ├── list-dark.png
│   └── ...
├── shopping/
│   └── ...
└── prism/
    └── ...
```

### 이슈 추적

발견된 이슈는 analysis 문서에 테이블 형태로 기록:

```markdown
| ID | Service | Page | Category | Description | Priority | Status |
|----|---------|------|----------|-------------|----------|--------|
| D001 | portal-shell | home | 대비 | 카드 제목 대비 부족 | P1 | Open |
| D002 | shopping | product-list | 정렬 | 가격 정렬 불일치 | P2 | Open |
```

---

## 4. 작업 순서

```
Step 1: 전체 서비스 실행 (infra + backend + frontend)
   ↓
Step 2: Playwright 스크린샷 수집 (모든 페이지 × 다크/라이트)
   ↓
Step 3: 시각적 이슈 식별 및 분류 (스크린샷 분석)
   ↓
Step 4: 디자인 시스템 구조 코드 리뷰
   ↓
Step 5: 컴포넌트 사용 패턴 분석 (import 점검)
   ↓
Step 6: 이슈 목록 작성 (우선순위 분류)
   ↓
Step 7: P0/P1 수정
   ↓
Step 8: P2/P3 수정
   ↓
Step 9: 수정 후 재검증 (스크린샷 재수집)
```

---

## 5. 성공 기준

| 항목 | 기준 |
|------|------|
| 스크린샷 수집 | 모든 서비스, 모든 주요 페이지, 다크/라이트 |
| P0 이슈 | 0개 (전부 해결) |
| P1 이슈 | 0개 (전부 해결) |
| P2 이슈 | 90% 이상 해결 |
| 디자인 시스템 구조 | 불균형/누락 해소 또는 개선안 문서화 |
| 컴포넌트 사용 | 공통 컴포넌트 미사용 케이스 해소 |

---

## 6. 위험 요소

| 위험 | 대응 |
|------|------|
| 전체 서비스 동시 실행 시 리소스 부족 | 필요한 서비스만 순차 실행 |
| Module Federation 빌드 에러 | 먼저 `npm run build` 후 dev 서버 실행 |
| Prism 서비스 아직 미완성 | 있는 범위 내에서만 점검 |
| 이슈가 너무 많을 경우 | P0/P1만 이번 사이클에서 해결, P2/P3는 다음 사이클 |

---

## 7. 현재 서비스 상태

| Service | Port | Status |
|---------|------|--------|
| api-gateway | 8080 | UP |
| auth-service | 8081 | UP |
| blog-service | 8082 | **DOWN** (실행 필요) |
| shopping-service | 8083 | **DOWN** (실행 필요) |
| chatbot-service | 8086 | UP |
| portal-shell | 30000 | UP |
| blog-frontend | 30001 | **DOWN** (실행 필요) |
| shopping-frontend | 30002 | **DOWN** (실행 필요) |
| prism-frontend | 30003/30004 | **DOWN** (실행 필요) |

---

## 8. 디자인 시스템 현황 요약

| 패키지 | 역할 | 컴포넌트 수 |
|--------|------|------------|
| @portal/design-tokens | 토큰 (색상, 간격, 타이포) | - |
| @portal/design-types | 공유 타입 (Props, API 등) | 70+ interfaces |
| @portal/design-system-vue | Vue 3 컴포넌트 | 27개 |
| @portal/design-system-react | React 18 컴포넌트 | 30개 |

**테마:** Portal(Dark-first, Indigo), Blog(Light, Teal), Shopping(Light, Orange), Prism(TBD)
**토큰 계층:** Base → Semantic → Theme (3-tier)
**Tailwind:** 공통 preset (`@portal/design-tokens/tailwind`) 기반
