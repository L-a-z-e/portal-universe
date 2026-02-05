# 리팩토링 진행 로그

> **이 파일을 보고 다음 세션에서 이어서 진행하세요**

---

## 🚀 빠른 재개 명령어

```
다음 세션 시작할 때 이렇게 말하세요:

"리팩토링 이어서 하자. PROGRESS-LOG.md 확인해줘"
```

---

## 📍 현재 상태 (2026-02-05)

### 전체 로드맵 위치

```
Week 1 (Phase 0): ✅ 완료
  └─ 브랜치, 테스트 기준선, 분석 완료

Phase 2: ✅ Bootstrap 리팩토링 완료 + ✅ API Client 통합 완료
  └─ react-bridge, react-bootstrap Library Mode 빌드 완료
  └─ react-bridge api-registry 추가, shopping/prism API 리팩토링 완료

Phase 1: ⏳ 대기 (Logback, 타입 공유)
```

### Bootstrap 리팩토링 진행률

```
[██████████] 100%

✅ 완료:
  - react-bridge: Vite Library Mode 빌드 설정
  - react-bootstrap: Vite Library Mode 빌드 설정
  - shopping-frontend/bootstrap.tsx 수정 (287줄 → 32줄)
  - prism-frontend/bootstrap.tsx 수정 (235줄 → 32줄)
  - 빌드 체인 검증 완료
```

### API Client 통합 진행률

```
[██████████] 100%

✅ 완료:
  - react-bridge/api-registry.ts 생성 (portal/api resolve + 캐싱)
  - PortalBridgeProvider에 initPortalApi 병렬 호출 추가
  - shopping-frontend client.ts 리팩토링 (portal/api 우선, local fallback)
  - prism-frontend api.ts 리팩토링 (lazy getter, portal/api 우선)
  - vite.config.ts shared에 axios 추가 (shopping, prism)
  - 죽은 코드 정리 (create-api-client.ts, __PORTAL_API_CLIENT__)
  - Gap Analysis: 98% Match Rate

💡 계획 변경:
  - 원래: frontend/api-client/ 패키지 신규 생성
  - 변경: portal-shell의 apiClient가 Vue 독립적(순수 JS) → MF portal/api로 직접 공유
```

### Clean Code 학습 진행률

```
[██████████] 100%

✅ 학습 완료:
  - createAppBootstrap.tsx 전체 (팩토리 함수)
  - createAppInstance 함수 (앱 인스턴스 생성)
  - cleanupInstance 함수 (정리 로직)
  - Vite Library Mode 빌드 패턴
  - react-bridge 주요 파일 (bridge-registry, api-registry, PortalBridgeProvider)

⏳ 다음 학습:
  - Clean Code Ch 5-6 (형식, 객체/자료구조)
```

---

## 📂 생성/수정된 파일 목록

### 세션 1 (2026-02-05): Bootstrap 리팩토링

| 파일 | 작업 | 설명 |
|------|------|------|
| `frontend/react-bridge/vite.config.ts` | 생성 | Library Mode 빌드 설정 |
| `frontend/react-bridge/tsconfig.json` | 수정 | design-system-react 패턴 |
| `frontend/react-bridge/tsconfig.node.json` | 생성 | vite.config.ts용 |
| `frontend/react-bridge/package.json` | 수정 | dist 경로, 빌드 스크립트 |
| `frontend/react-bootstrap/vite.config.ts` | 생성 | Library Mode 빌드 설정 |
| `frontend/react-bootstrap/tsconfig.json` | 수정 | design-system-react 패턴 |
| `frontend/react-bootstrap/tsconfig.node.json` | 생성 | vite.config.ts용 |
| `frontend/react-bootstrap/package.json` | 수정 | dist 경로, 빌드 스크립트 |
| `frontend/react-bootstrap/src/index.ts` | 생성 | export 파일 |
| `frontend/shopping-frontend/src/bootstrap.tsx` | 수정 | 287줄 → 32줄 |
| `frontend/shopping-frontend/vite.config.ts` | 수정 | alias 제거 |
| `frontend/prism-frontend/src/bootstrap.tsx` | 수정 | 235줄 → 32줄 |
| `frontend/prism-frontend/vite.config.ts` | 수정 | alias 제거 |
| `frontend/package.json` | 수정 | build:libs 스크립트 추가 |

### 세션 2 (2026-02-05): API Client 통합 리팩토링

| 파일 | 작업 | 설명 |
|------|------|------|
| `frontend/react-bridge/src/api-registry.ts` | 생성 | portal/api resolve + 캐싱 |
| `frontend/react-bridge/src/PortalBridgeProvider.tsx` | 수정 | initPortalApi 병렬 호출 |
| `frontend/react-bridge/src/index.ts` | 수정 | api-registry export |
| `frontend/react-bridge/src/portal-modules.d.ts` | 수정 | portal/api declare, __PORTAL_API_CLIENT__ 제거 |
| `frontend/shopping-frontend/src/api/client.ts` | 수정 | portal/api 우선, local fallback |
| `frontend/shopping-frontend/vite.config.ts` | 수정 | shared에 axios 추가 |
| `frontend/shopping-frontend/src/types/portal-modules.d.ts` | 수정 | __PORTAL_API_CLIENT__ 제거 |
| `frontend/shopping-frontend/docs/architecture/system-overview.md` | 수정 | 구 패턴 → 신 패턴 문서 반영 |
| `frontend/prism-frontend/src/services/api.ts` | 수정 | lazy getter, portal/api 우선 |
| `frontend/prism-frontend/vite.config.ts` | 수정 | shared에 axios 추가 |
| `frontend/prism-frontend/src/types/portal-modules.d.ts` | 수정 | __PORTAL_API_CLIENT__ 제거 |
| `frontend/portal-shell/src/types/global.d.ts` | 수정 | __PORTAL_API_CLIENT__ 제거 |

### 삭제된 파일

| 파일 | 이유 |
|------|------|
| `react-bootstrap/src/createAppBootstrap.jsx` | tsx와 충돌 (vite resolve 문제) |
| `react-bridge/src/api/create-api-client.ts` | 사용처 없는 죽은 코드 |

---

## 🎯 다음 세션 TODO

### 1. Phase 1: Foundation (Logback 설정 통합)

```
- common-library에 logback-base.xml 생성
- 5개 Java 서비스에 include로 전환
- 효과: 920줄 → 200줄 (78% 감소)
```

### 2. Phase 1: Foundation (타입 공유 체계)

```
- design-types에 api.ts 생성 (ApiResponse, ErrorDetails)
- portal-shell, shopping, prism 타입 import 통일
```

### 3. E2E 테스트 (선택)

```
- shopping-frontend Embedded/Standalone 동작 확인
- prism-frontend Embedded/Standalone 동작 확인
```

### 4. prism-frontend minify 재활성화 (선택)

```
- vite.config.ts: minify: true, sourcemap: false
```

---

## 📚 학습 노트

### Vite Library Mode 패턴

```typescript
// vite.config.ts 핵심 설정
export default defineConfig({
  plugins: [react(), dts({ insertTypesEntry: true })],
  build: {
    lib: {
      entry: resolve(__dirname, 'src/index.ts'),
      formats: ['es', 'cjs'],
    },
    rollupOptions: {
      external: ['react', 'react-dom', ...],
    },
  },
});
```

### package.json exports 패턴

```json
{
  "main": "./dist/index.cjs",
  "module": "./dist/index.js",
  "types": "./dist/index.d.ts",
  "exports": {
    ".": {
      "types": "./dist/index.d.ts",
      "import": "./dist/index.js",
      "require": "./dist/index.cjs"
    }
  }
}
```

### 빌드 순서

```
1. build:design (tokens → types → vue → react)
2. build:libs (react-bridge → react-bootstrap)
3. build:apps (shell → blog → shopping → prism)
```

### API Client 통합 패턴 (portal/api via MF)

```
Embedded 모드:
  PortalBridgeProvider
    → initBridge() + initPortalApi() 병렬 실행
    → api-registry가 import('portal/api').apiClient를 캐싱
    → getPortalApiClient()로 반환

  shopping: getApiClient() = getPortalApiClient() ?? getLocalClient()
  prism:    get client()   = getPortalApiClient() ?? this._client (lazy)

Standalone 모드:
  portal/api import 실패 → getPortalApiClient() = null → local fallback
```

---

## 🐛 해결된 이슈

### ajv/dist/core 에러
- **원인**: vite-plugin-dts의 의존성 해석 문제
- **해결**: `npm install ajv@8 --save-dev`

### createAppBootstrap not exported 에러
- **원인**: `createAppBootstrap.jsx` 파일이 `.tsx`와 충돌
- **해결**: `.jsx` 파일 삭제

### API 클라이언트 분산 (React 서비스)
- **원인**: shopping/prism이 자체 간이 apiClient 사용 → 토큰 갱신, 401/429 재시도 누락
- **발견**: react-bridge 학습 중 portal-shell의 apiClient가 Vue 독립적임을 확인
- **해결**: react-bridge에 api-registry 추가, MF portal/api로 apiClient 직접 공유

---

## 🔖 현재 Git 브랜치

```
refactor/phase0-setup
```

커밋 대기 중. 작업 확인 후 커밋 예정.
