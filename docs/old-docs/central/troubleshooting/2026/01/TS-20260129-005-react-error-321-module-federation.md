---
id: TS-20260129-005
title: React Error #321 - Module Federation 듀얼 React 인스턴스 문제
type: troubleshooting
status: resolved
created: 2026-01-29
updated: 2026-01-29
author: Frontend Team
severity: high
resolved: true
affected_services: [prism-frontend, shopping-frontend]
tags: [react, module-federation, react-dom, vite, shared-dependencies]
---

# React Error #321: Module Federation 듀얼 React 인스턴스 문제

## 요약

| 항목 | 내용 |
|------|------|
| **심각도** | 🟠 High |
| **발생일** | 2026-01-29 |
| **해결일** | 2026-01-29 |
| **영향 서비스** | prism-frontend, shopping-frontend (모든 React Remote 모듈) |

## 증상 (Symptoms)

### 현상
- React Remote Module 로드 시 React Error #321 발생
- Standalone 모드에서는 정상 작동, Host에서 로드 시에만 에러
- `createRoot()` 호출 시점에서 에러 발생
- 페이지 렌더링 완전 실패

### 에러 메시지
```
Uncaught Error: Invalid hook call. Hooks can only be called inside of the body of a function component.
This could happen for one of the following reasons:
1. You might have mismatching versions of React and the renderer (such as React DOM)
2. You might be breaking the Rules of Hooks
3. You might have more than one copy of React in the same app
See https://reactjs.org/link/invalid-hook-call for tips about how to debug and fix this problem.
```

### 브라우저 콘솔
```javascript
// Error #321 발생 지점
const root = ReactDOM.createRoot(container);
root.render(<App />);  // ← Error #321
```

## 원인 분석 (Root Cause)

### 초기 추정
- React/React-DOM 버전 불일치 의심
- Module Federation shared 설정 오류 의심
- Vite build 설정 문제 의심

### 실제 원인

**`react-dom/client`가 Module Federation shared에서 누락되어 로컬 CJS 번들로 패키징됨**

#### 기술적 분석

`@originjs/vite-plugin-federation`의 `shared` 설정:

**문제 설정 (Before)**
```typescript
// vite.config.ts
federation({
  name: 'shopping',
  filename: 'remoteEntry.js',
  shared: ['react', 'react-dom'],  // ❌ react-dom/client 누락
})
```

#### 발생 메커니즘

1. **Import 구문 분석**
   ```typescript
   // bootstrap.tsx
   import React from 'react';              // ✅ Shared로 처리
   import ReactDOM from 'react-dom/client'; // ❌ 로컬 번들로 처리
   ```

2. **빌드 산출물 (수정 전)**
   ```javascript
   // __federation_expose_Bootstrap-*.js
   const React = await importShared('react');          // Host의 React 사용 ✅
   import { R as ReactDOM } from './index-*.js';       // 로컬 CJS 번들 ❌

   // index-*.js (로컬 CJS 번들)
   import { r as requireReactDom } from './index-*.js';
   var m = requireReactDom();  // ← 내부에 별도 React 인스턴스 포함!
   ```

3. **듀얼 React 인스턴스 발생**
   - **컴포넌트 hooks**: Host의 shared React 사용
   - **createRoot 내부**: 로컬 번들의 React 사용
   - 두 React 인스턴스가 다름 → Error #321

4. **에러 발생 시점**
   ```javascript
   const root = ReactDOM.createRoot(container);
   root.render(<App />);
   // ↑ App은 shared React의 hooks 사용
   // ↑ createRoot는 로컬 React 인스턴스 사용
   // → React mismatch → Error #321
   ```

### 분석 과정

#### 1단계: 빌드 산출물 분석
```bash
# dist/assets/ 디렉토리의 federation 번들 분석
cat dist/assets/__federation_expose_Bootstrap-*.js
# → react-dom/client이 importShared()가 아닌 로컬 import로 확인
```

#### 2단계: Vite Plugin Federation 동작 확인
- `shared` 배열에 명시된 패키지만 `importShared()` 변환
- `react-dom/client`는 `react-dom`의 서브패스이지만 별도 specifier
- **Module Federation은 정확한 import specifier 매칭 필요**

#### 3단계: 근본 원인 특정
- React 19부터 `createRoot`는 `react-dom/client`에만 존재
- `react-dom` 메인 모듈에는 `createRoot` 없음
- `react-dom/client`를 shared에 추가하지 않으면 로컬 번들에 포함
- 로컬 번들 내 `react-dom/client`는 내부적으로 자체 React 참조

#### 4단계: 검증
```typescript
// shared에 'react-dom/client' 추가 후 빌드
federation({
  shared: ['react', 'react-dom', 'react-dom/client'],
})

// 빌드 산출물 재확인
// → const ReactDOM = await importShared('react-dom/client') ✅
```

## 해결 방법 (Solution)

### 즉시 조치 (Immediate Fix)

**`vite.config.ts`의 shared 배열에 `'react-dom/client'` 추가**

### 영구 조치 (Permanent Fix)

#### 파일 1: `frontend/prism-frontend/vite.config.ts`
```typescript
// Before (문제 코드)
federation({
  name: 'prism',
  filename: 'remoteEntry.js',
  exposes: {
    './bootstrap': './src/bootstrap.tsx',
  },
  shared: ['react', 'react-dom'],  // ❌
})

// After (해결 코드)
federation({
  name: 'prism',
  filename: 'remoteEntry.js',
  exposes: {
    './bootstrap': './src/bootstrap.tsx',
  },
  shared: ['react', 'react-dom', 'react-dom/client'],  // ✅
})
```

#### 파일 2: `frontend/shopping-frontend/vite.config.ts`
```typescript
// Before (문제 코드)
federation({
  name: 'shopping',
  filename: 'remoteEntry.js',
  exposes: {
    './bootstrap': './src/bootstrap.tsx',
  },
  shared: ['react', 'react-dom'],  // ❌
})

// After (해결 코드)
federation({
  name: 'shopping',
  filename: 'remoteEntry.js',
  exposes: {
    './bootstrap': './src/bootstrap.tsx',
  },
  shared: ['react', 'react-dom', 'react-dom/client'],  // ✅
})
```

### 빌드 산출물 증거 (수정 후)

```javascript
// __federation_expose_Bootstrap-*.js (수정 후)
const React = await importShared('react');              // ✅ Shared
const ReactDOM = await importShared('react-dom/client'); // ✅ Shared
```

### 수정된 파일

| 파일 경로 | 수정 내용 |
|----------|----------|
| `frontend/prism-frontend/vite.config.ts` | shared 배열에 'react-dom/client' 추가 |
| `frontend/shopping-frontend/vite.config.ts` | shared 배열에 'react-dom/client' 추가 |

## 기각된 대안 (Rejected Alternatives)

### 대안 1: `react-dom/client` → `react-dom` import 변경
```typescript
// ❌ 시도
import ReactDOM from 'react-dom';
const root = ReactDOM.createRoot(container);
```

**기각 이유**: React 19에서 `react-dom` 메인 모듈에 `createRoot` 없음

### 대안 2: `singleton: true` 설정
```typescript
// ❌ 시도
shared: {
  react: { singleton: true },
  'react-dom': { singleton: true },
  'react-dom/client': { singleton: true },
}
```

**기각 이유**: `@originjs/vite-plugin-federation`의 TypeScript 타입 정의에서 `singleton` 프로퍼티가 주석 처리되어 미지원

## 재발 방지 (Prevention)

### 프로세스 개선

#### 1. 새 React Remote 서비스 생성 시 체크리스트
- [ ] `vite.config.ts`에서 shared 배열 확인
- [ ] `'react'`, `'react-dom'`, `'react-dom/client'` 모두 포함 확인
- [ ] 빌드 후 `dist/assets/__federation_expose_*.js` 검증
- [ ] `importShared()` 호출 확인

#### 2. 표준 Vite Config 템플릿 (React Remote)
```typescript
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import federation from '@originjs/vite-plugin-federation';

export default defineConfig({
  plugins: [
    react(),
    federation({
      name: 'service-name',
      filename: 'remoteEntry.js',
      exposes: {
        './bootstrap': './src/bootstrap.tsx',
      },
      shared: [
        'react',
        'react-dom',
        'react-dom/client',  // ✅ 필수!
      ],
    }),
  ],
});
```

#### 3. 빌드 산출물 검증 스크립트
```bash
# dist/assets/ 내 federation expose 파일에서 react-dom/client 검증
grep -r "importShared('react-dom/client')" dist/assets/__federation_expose_*.js
# → 출력 있으면 ✅, 없으면 ❌
```

### 문서화
- `.claude/rules/react.md`에 Module Federation 표준 패턴 추가
- 새 개발자 온보딩 시 필수 체크 항목 포함

## 학습 포인트

### 1. Module Federation의 Import Specifier 엄격성
- **정확한 매칭 필요**: `'react-dom'` ≠ `'react-dom/client'`
- Subpath exports는 별도로 shared에 명시해야 함
- Package.json의 `exports` 필드 각 경로마다 설정 필요

### 2. React 19의 API 변경
- `createRoot`가 `react-dom/client` 전용 API
- Legacy `render()`는 `react-dom`에 존재하지만 deprecated
- React 18+ 프로젝트는 반드시 `react-dom/client` 사용

### 3. 듀얼 React 인스턴스 탐지
- **증상**: Error #321 (Invalid hook call)
- **원인**: React 인스턴스가 2개 이상 존재
- **확인 방법**:
  ```javascript
  // 브라우저 콘솔에서
  window.React1 = require('react');
  window.React2 = require('react-dom/client').React;
  console.log(window.React1 === window.React2);  // false면 듀얼 인스턴스
  ```

### 4. Vite Plugin Federation의 제약
- `@originjs/vite-plugin-federation`은 Webpack Module Federation과 API 차이
- `singleton`, `requiredVersion` 등 일부 옵션 미지원
- 배열 형태의 simple shared 설정 권장

### 5. 디버깅 전략
1. **빌드 산출물 직접 분석**: `dist/assets/` 파일 내용 확인
2. **importShared() 호출 추적**: Federation 변환 여부 확인
3. **로컬 vs Shared 분리**: 어떤 모듈이 로컬 번들에 포함되는지 파악
4. **React DevTools 활용**: 컴포넌트 트리에서 React 버전 확인

## 환경 정보

```
프론트엔드 환경:
├─ React: 18.2.0
├─ React DOM: 18.2.0
├─ Vite: 7.1.12
├─ @vitejs/plugin-react: 4.3.4
├─ @originjs/vite-plugin-federation: 1.3.6
└─ TypeScript: 5.7.3

배포 환경:
├─ Host: portal-shell (localhost:30000) - Vue 3
├─ Remote: prism-frontend (localhost:30004) - React 18
├─ Remote: shopping-frontend (localhost:30002) - React 18
└─ Design System: @portal/design-system (localhost:30003)

Node.js: v20.18.3
pnpm: 9.15.4
```

## 관련 링크

- [React Error Decoder #321](https://react.dev/errors/321)
- [Module Federation Shared Dependencies](https://webpack.js.org/concepts/module-federation/#shared)
- [Vite Plugin Federation](https://github.com/originjs/vite-plugin-federation)
- [React 19 createRoot API](https://react.dev/reference/react-dom/client/createRoot)
- [TS-20260117-001: React Error #31](./TS-20260117-001-react-module-federation.md) - 관련 Module Federation 이슈

## 관련 이슈

- GitHub Issue #321: React Error 321 Module Federation
- 관련 PR: prism-frontend, shopping-frontend Module Federation shared 수정

## 미래 개발자를 위한 Quick Reference

### ✅ 올바른 React Remote 설정
```typescript
shared: ['react', 'react-dom', 'react-dom/client']
```

### ❌ 잘못된 설정
```typescript
shared: ['react', 'react-dom']  // react-dom/client 누락!
```

### 🔍 검증 명령어
```bash
# 빌드 후 확인
grep "importShared('react-dom/client')" dist/assets/__federation_expose_*.js
```

### 📝 체크리스트 (새 React Remote 서비스)
- [ ] `shared`에 `'react'` 포함
- [ ] `shared`에 `'react-dom'` 포함
- [ ] `shared`에 `'react-dom/client'` 포함
- [ ] 빌드 후 `importShared()` 호출 검증
- [ ] Host에서 로드 테스트 완료
