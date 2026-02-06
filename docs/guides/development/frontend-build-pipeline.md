# Frontend 빌드 파이프라인 가이드

**난이도**: ⭐⭐ | **예상 시간**: 15분 | **카테고리**: Development

## 개요

Portal Universe의 프론트엔드는 npm workspaces 기반 모노레포입니다. 패키지 간 의존성이 있어 빌드 순서가 중요하며, 이 가이드는 전체 빌드 파이프라인과 각 단계의 역할을 설명합니다.

---

## 빌드 순서

전체 빌드는 `frontend/` 디렉토리에서 `npm run build`로 실행되며, 다음 순서로 진행됩니다:

```
Step 1: build:design
  ├── design-tokens
  ├── design-types
  ├── design-system-vue
  └── design-system-react

Step 2: build:libs
  ├── react-bridge
  └── react-bootstrap

Step 3: build:apps
  ├── portal-shell
  ├── blog-frontend
  ├── shopping-frontend
  └── prism-frontend
```

### 사용 가능한 명령어

```bash
# frontend/ 디렉토리에서 실행
npm run build              # 전체 빌드 (design → libs → apps)
npm run build:design       # 디자인 시스템만
npm run build:libs         # React 공유 라이브러리만
npm run build:apps         # 앱 4개만
```

---

## 의존성 체인

```
design-tokens
  ├── design-system-vue → portal-shell, blog-frontend
  └── design-system-react → shopping-frontend, prism-frontend

react-bridge → react-bootstrap → shopping-frontend, prism-frontend

portal-shell (MF Host) ← blog-frontend, shopping-frontend, prism-frontend (MF Remotes)
```

---

## 각 단계 상세

### Design 단계

| 패키지 | 명령어 | 역할 | 산출물 |
|--------|--------|------|--------|
| design-tokens | `npm run build:tokens` | CSS variables, Tailwind preset 생성 | `design-tokens/dist/` |
| design-types | `npm run build:types` | 공유 TypeScript 타입 정의 | `design-types/dist/` |
| design-system-vue | `npm run build:vue` | Vue 3 컴포넌트 라이브러리 | `design-system-vue/dist/` |
| design-system-react | `npm run build:react` | React 컴포넌트 라이브러리 | `design-system-react/dist/` |

### Libs 단계

| 패키지 | 명령어 | 역할 | 산출물 |
|--------|--------|------|--------|
| react-bridge | `npm run build:react-bridge` | MF bridge (api-registry, bridge-registry) | `react-bridge/dist/` |
| react-bootstrap | `npm run build:react-bootstrap` | React 앱 공통 bootstrap | `react-bootstrap/dist/` |

### Apps 단계

각 앱은 Vite로 개별 빌드되며, 산출물은 `{app}/dist/` 디렉토리에 생성됩니다.

| 앱 | 포트 | 기술 스택 | MF 역할 |
|----|------|----------|---------|
| portal-shell | 30000 | Vue 3 | Host |
| blog-frontend | 30001 | Vue 3 | Remote |
| shopping-frontend | 30002 | React 18 | Remote |
| prism-frontend | 30003 | React 18 | Remote |

---

## 일반적 실패 사례

### "Cannot find module" 에러

| 에러 메시지 | 원인 | 해결 |
|------------|------|------|
| `Cannot find module '@portal/design-tokens/tailwind'` | design-tokens 미빌드 | `npm run build:tokens` 실행 |
| `Cannot find module '@portal/design-system-vue'` | design-system 미빌드 | `npm run build:design` 실행 |
| `Cannot find module '@portal/react-bridge'` | react-bridge 미빌드 | `npm run build:libs` 실행 |

### Module Federation 관련

| 증상 | 원인 | 해결 |
|------|------|------|
| React 버전 충돌 (Error #525) | React 버전 불일치 | root `package.json`의 `overrides`로 React 18.3.1 고정 확인 |
| `react-dom/client` 에러 (#321) | MF shared 설정 누락 | `vite.config.ts` shared 배열에 `react-dom/client` 포함 |
| remoteEntry.js 404 | Remote 앱 미실행 | 해당 Remote 앱 실행 확인 (포트: 30001~30003) |
| axios 에러 | MF shared 누락 | `vite.config.ts` shared 배열에 `axios` 포함 |

---

## 산출물 확인

빌드 완료 후 각 패키지의 `dist/` 디렉토리를 확인합니다:

```bash
# Design 패키지
ls frontend/design-tokens/dist/
ls frontend/design-system-vue/dist/
ls frontend/design-system-react/dist/

# Libs 패키지
ls frontend/react-bridge/dist/
ls frontend/react-bootstrap/dist/

# Apps
ls frontend/portal-shell/dist/
ls frontend/shopping-frontend/dist/
```

---

## 참고 자료

### 개발 모드

- `npm run dev`는 모든 앱을 동시에 실행 (Vite HMR)
- design/libs 변경 시에만 재빌드 필요, 앱 코드는 HMR로 자동 반영
- 최초 한 번은 `npm run build:design && npm run build:libs` 필요

### 관련 문서

- [Execution Guide](../../.claude/rules/execution.md) - 실행 명령어 전체
- [Module Federation 상세](../../.claude/skills/module-federation.md) - MF 설정 및 트러블슈팅
- [Design System Architecture](design-system-architecture.md) - 디자인 시스템 구조
- [Federation Integration](federation-integration.md) - Module Federation 통합

---

작성자: Laze
