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

Week 2~: ✅ Bootstrap 리팩토링 완료
  └─ react-bridge, react-bootstrap Library Mode 빌드 완료
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

⚠️ 참고:
  - prism-frontend에 기존 타입 에러 있음 (리팩토링과 무관)
```

### Clean Code 학습 진행률

```
[██████████] 100%

✅ 학습 완료:
  - createAppBootstrap.tsx 전체 (팩토리 함수)
  - createAppInstance 함수 (앱 인스턴스 생성)
  - cleanupInstance 함수 (정리 로직)
  - Vite Library Mode 빌드 패턴

⏳ 다음 학습:
  - react-bridge 주요 파일 살펴보기
```

---

## 📂 생성/수정된 파일 목록

### 이번 세션 (2026-02-05)

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

### 삭제된 파일

| 파일 | 이유 |
|------|------|
| `react-bootstrap/src/createAppBootstrap.jsx` | tsx와 충돌 (vite resolve 문제) |

---

## 🎯 다음 세션 TODO

### 1. react-bridge 주요 파일 학습

```
- PortalBridgeProvider.tsx (Provider 패턴)
- hooks/usePortalAuth.ts (인증 훅)
- hooks/usePortalTheme.ts (테마 훅)
- bridge-registry.ts (Module Federation 연결)
```

### 2. prism-frontend 타입 에러 수정 (선택)

```
- 암시적 any 타입 수정
- 빌드 통과 확인
```

### 3. E2E 테스트 (선택)

```
npm run test:e2e
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

---

## 🐛 해결된 이슈

### ajv/dist/core 에러
- **원인**: vite-plugin-dts의 의존성 해석 문제
- **해결**: `npm install ajv@8 --save-dev`

### createAppBootstrap not exported 에러
- **원인**: `createAppBootstrap.jsx` 파일이 `.tsx`와 충돌
- **해결**: `.jsx` 파일 삭제

---

## 🔖 현재 Git 브랜치

```
refactor/phase0-setup
```

커밋 대기 중. 작업 확인 후 커밋 예정.
