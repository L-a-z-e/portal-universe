---
id: portal-shell-adding-remote
title: Portal Shell - Adding Remote Module
type: guide
status: current
created: 2026-01-18
updated: 2026-01-18
author: Laze
tags: [portal-shell, module-federation, remote, micro-frontend, vue3]
related:
  - portal-shell-getting-started
  - portal-shell-development
---

# Portal Shell - Adding Remote Module

**난이도**: ⭐⭐⭐ | **예상 시간**: 30분 | **카테고리**: Development

> 새로운 Remote 모듈을 Portal Shell에 추가하는 가이드

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **예상 소요 시간** | 30-40분 |
| **난이도** | 중급 |
| **대상** | Portal Shell 개발자 |
| **사전 지식** | Module Federation, Vue Router |

이 가이드는 새로운 마이크로 프론트엔드 Remote 모듈을 Portal Shell에 통합하는 과정을 설명합니다.

---

## 🎯 Remote 모듈 추가 흐름

```
1. Remote 애플리케이션 준비
   ↓
2. remoteRegistry.ts 업데이트
   ↓
3. vite.config.ts 업데이트
   ↓
4. 환경 변수 추가
   ↓
5. 테스트 및 검증
```

---

## ✅ 사전 요구사항

### Remote 애플리케이션이 갖춰야 할 조건

1. **Bootstrap 함수 노출**

Remote 애플리케이션은 다음 형태의 bootstrap 함수를 expose해야 합니다:

```typescript
// remote-app/src/bootstrap.ts
export function mountAppName(
  containerId: string,
  initialPath: string = '/',
  sharedModules?: {
    apiClient: any;
    authStore: any;
    themeStore: any;
  }
) {
  // Vue 앱 마운트 로직
}
```

2. **Module Federation 설정**

Remote의 `vite.config.ts`에 Federation 플러그인이 설정되어 있어야 합니다:

```typescript
// remote-app/vite.config.ts
import federation from "@originjs/vite-plugin-federation";

export default defineConfig({
  plugins: [
    vue(),
    federation({
      name: 'remoteAppName',
      filename: 'remoteEntry.js',
      exposes: {
        './bootstrap': './src/bootstrap.ts',
      },
      shared: ['vue', 'pinia', 'axios'],
    })
  ],
  build: {
    target: 'esnext',
    minify: false,
  }
})
```

3. **독립 실행 가능**

Remote는 독립적으로 개발 및 테스트 가능해야 합니다:

```bash
cd frontend/remote-app
npm run dev  # 독립 실행 (예: 포트 30004)
```

---

## 🔧 Step 1: remoteRegistry.ts 업데이트

**파일 경로:** `src/config/remoteRegistry.ts`

### 1.1 RemoteConfig 추가

각 환경별로 Remote 설정을 추가합니다:

```typescript
// src/config/remoteRegistry.ts

const remoteConfigs: Record<EnvironmentMode, RemoteConfig[]> = {
  dev: [
    // 기존 Remote들...
    {
      name: 'Blog',
      key: 'blog',
      url: 'http://localhost:30001/assets/remoteEntry.js',
      module: 'blog/bootstrap',
      mountFn: 'mountBlogApp',
      basePath: '/blog',
      icon: '📝',
      description: '블로그 서비스'
    },
    // ✅ 새 Remote 추가
    {
      name: 'Payment',           // 표시 이름
      key: 'payment',            // Federation key (고유 식별자)
      url: 'http://localhost:30004/assets/remoteEntry.js',  // remoteEntry.js URL
      module: 'payment/bootstrap',  // 로드할 모듈 경로
      mountFn: 'mountPaymentApp',   // mount 함수 이름
      basePath: '/payment',         // 라우팅 base path
      icon: '💳',                   // 아이콘 (선택)
      description: '결제 서비스'   // 설명 (선택)
    },
  ],
  docker: [
    // 기존 Remote들...
    {
      name: 'Payment',
      key: 'payment',
      url: import.meta.env.VITE_PAYMENT_REMOTE_URL,  // 환경 변수 사용
      module: 'payment/bootstrap',
      mountFn: 'mountPaymentApp',
      basePath: '/payment',
      icon: '💳',
      description: '결제 서비스'
    },
  ],
  k8s: [
    // 기존 Remote들...
    {
      name: 'Payment',
      key: 'payment',
      url: import.meta.env.VITE_PAYMENT_REMOTE_URL,  // 환경 변수 사용
      module: 'payment/bootstrap',
      mountFn: 'mountPaymentApp',
      basePath: '/payment',
      icon: '💳',
      description: '결제 서비스'
    },
  ]
};
```

### 1.2 RemoteConfig 필드 설명

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| `name` | string | ✅ | 사용자에게 표시될 이름 | `Payment` |
| `key` | string | ✅ | Federation key (고유 식별자) | `payment` |
| `url` | string | ✅ | remoteEntry.js URL | `http://localhost:30004/assets/remoteEntry.js` |
| `module` | string | ✅ | 로드할 모듈 경로 (key/bootstrap) | `payment/bootstrap` |
| `mountFn` | string | ✅ | mount 함수 이름 | `mountPaymentApp` |
| `basePath` | string | ✅ | 라우팅 base path | `/payment` |
| `icon` | string | ⭕ | 네비게이션 아이콘 (emoji) | `💳` |
| `description` | string | ⭕ | 서비스 설명 | `결제 서비스` |

---

## 🔧 Step 2: vite.config.ts 업데이트

**파일 경로:** `vite.config.ts`

### 2.1 Remotes 설정 추가

```typescript
// vite.config.ts
import {defineConfig, loadEnv} from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from "@originjs/vite-plugin-federation";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');

  console.log('🔧 [Vite Config] Building for mode:', mode);
  console.log('🔧 [Vite Config] Blog Remote URL:', env.VITE_BLOG_REMOTE_URL);
  console.log('🔧 [Vite Config] Shopping Remote URL:', env.VITE_SHOPPING_REMOTE_URL);
  console.log('🔧 [Vite Config] Payment Remote URL:', env.VITE_PAYMENT_REMOTE_URL);  // ✅ 추가

  return {
    plugins: [
      vue(),
      federation({
        name: 'portal',
        filename: 'shellEntry.js',
        remotes: {
          blog: env.VITE_BLOG_REMOTE_URL,
          shopping: env.VITE_SHOPPING_REMOTE_URL,
          payment: env.VITE_PAYMENT_REMOTE_URL,  // ✅ 추가
        },
        exposes: {
          './apiClient': './src/api/apiClient.ts',
          './authStore': './src/store/auth.ts',
          './themeStore': './src/store/theme.ts',
        },
        shared: ['vue', 'pinia', 'axios'],
      })
    ],
    // ... 나머지 설정
  }
})
```

---

## 🔧 Step 3: 환경 변수 추가

### 3.1 .env.dev 업데이트

**파일 경로:** `.env.dev`

```bash
# Vite 프로필
VITE_PROFILE=dev

# API Gateway URL
VITE_API_BASE_URL=http://localhost:8080

# Auth Service URL
VITE_AUTH_URL=http://localhost:8081

# Remote Module URLs
VITE_BLOG_REMOTE_URL=http://localhost:30001/assets/remoteEntry.js
VITE_SHOPPING_REMOTE_URL=http://localhost:30002/assets/remoteEntry.js
VITE_PAYMENT_REMOTE_URL=http://localhost:30004/assets/remoteEntry.js  # ✅ 추가
```

### 3.2 .env.docker 업데이트

**파일 경로:** `.env.docker`

```bash
VITE_PROFILE=docker

VITE_API_BASE_URL=http://api-gateway:8080
VITE_AUTH_URL=http://auth-service:8081

VITE_BLOG_REMOTE_URL=http://blog-frontend:30001/assets/remoteEntry.js
VITE_SHOPPING_REMOTE_URL=http://shopping-frontend:30002/assets/remoteEntry.js
VITE_PAYMENT_REMOTE_URL=http://payment-frontend:30004/assets/remoteEntry.js  # ✅ 추가
```

### 3.3 .env.k8s 업데이트

**파일 경로:** `.env.k8s`

```bash
VITE_PROFILE=k8s

VITE_API_BASE_URL=http://api-gateway.default.svc.cluster.local:8080
VITE_AUTH_URL=http://auth-service.default.svc.cluster.local:8081

VITE_BLOG_REMOTE_URL=http://blog-frontend.default.svc.cluster.local:30001/assets/remoteEntry.js
VITE_SHOPPING_REMOTE_URL=http://shopping-frontend.default.svc.cluster.local:30002/assets/remoteEntry.js
VITE_PAYMENT_REMOTE_URL=http://payment-frontend.default.svc.cluster.local:30004/assets/remoteEntry.js  # ✅ 추가
```

---

## 🔧 Step 4: TypeScript 타입 추가 (선택)

Remote 모듈의 타입을 추가하려면:

**파일 경로:** `src/types/remotes.d.ts` (신규 생성)

```typescript
// src/types/remotes.d.ts

declare module 'blog/bootstrap' {
  export function mountBlogApp(
    containerId: string,
    initialPath?: string,
    sharedModules?: any
  ): void;
}

declare module 'shopping/bootstrap' {
  export function mountShoppingApp(
    containerId: string,
    initialPath?: string,
    sharedModules?: any
  ): void;
}

// ✅ 새 Remote 추가
declare module 'payment/bootstrap' {
  export function mountPaymentApp(
    containerId: string,
    initialPath?: string,
    sharedModules?: any
  ): void;
}
```

---

## ✅ Step 5: 테스트 및 검증

### 5.1 Remote 애플리케이션 빌드

```bash
cd frontend/payment-frontend
npm run build:dev
```

### 5.2 Remote 애플리케이션 실행

```bash
npm run dev
```

**확인:**

```bash
curl http://localhost:30004/assets/remoteEntry.js
```

성공 시 JavaScript 파일 내용이 반환됩니다.

### 5.3 Portal Shell 재시작

```bash
cd frontend/portal-shell

# 캐시 삭제
rm -rf node_modules/.vite

# 재시작
npm run dev
```

### 5.4 동작 확인

**1. 브라우저 접속:**

```
http://localhost:30000
```

**2. 네비게이션 확인:**

상단 헤더에 "Payment" 메뉴가 추가되었는지 확인

**3. Remote 라우팅 확인:**

```
http://localhost:30000/payment
```

Payment 마이크로 프론트엔드가 로드되어야 합니다.

**4. 콘솔 로그 확인:**

```
🔧 [Vite Config] Payment Remote URL: http://localhost:30004/assets/remoteEntry.js
✅ [RemoteWrapper] Loading remote: payment
✅ [RemoteWrapper] Remote module loaded successfully: payment
✅ [RemoteWrapper] Mount function called: mountPaymentApp
```

**5. 브라우저 DevTools 확인:**

Network 탭에서 `remoteEntry.js` 로드 확인:

```
http://localhost:30004/assets/remoteEntry.js  [Status: 200]
```

---

## ⚠️ 자주 발생하는 문제

### 문제 1: Remote 모듈을 찾을 수 없음

**증상:**

```
❌ Failed to fetch dynamically imported module: http://localhost:30004/assets/remoteEntry.js
```

**해결 방법:**

1. Remote 애플리케이션이 실행 중인지 확인:

```bash
lsof -i :30004
```

2. remoteEntry.js 생성 확인:

```bash
ls frontend/payment-frontend/dist/assets/remoteEntry.js
```

3. Remote 빌드 모드 확인:

```typescript
// payment-frontend/vite.config.ts
build: {
  target: 'esnext',  // ✅ 필수
  minify: false,     // ✅ 개발 시 false 권장
}
```

### 문제 2: Mount 함수가 호출되지 않음

**증상:**

```
✅ Remote module loaded successfully: payment
❌ TypeError: module[mountPaymentApp] is not a function
```

**원인:** Remote 애플리케이션이 bootstrap 함수를 expose하지 않았거나 함수명이 다름

**해결 방법:**

1. Remote의 vite.config.ts 확인:

```typescript
exposes: {
  './bootstrap': './src/bootstrap.ts',  // ✅ 경로 확인
}
```

2. bootstrap.ts에서 함수 export 확인:

```typescript
export function mountPaymentApp(...) { ... }  // ✅ 함수명 일치 확인
```

3. remoteRegistry.ts의 mountFn 확인:

```typescript
mountFn: 'mountPaymentApp',  // ✅ 함수명 일치
```

### 문제 3: Shared 모듈 버전 충돌

**증상:**

```
⚠️ Shared module version mismatch: vue
```

**해결 방법:**

1. Portal Shell과 Remote의 package.json에서 Vue 버전 확인:

```bash
# Portal Shell
grep "vue" frontend/portal-shell/package.json

# Remote
grep "vue" frontend/payment-frontend/package.json
```

2. 버전 일치시키기:

```json
// 양쪽 package.json
"dependencies": {
  "vue": "^3.5.21"  // ✅ 동일한 버전 사용
}
```

3. 재설치:

```bash
cd frontend
npm install
```

### 문제 4: 환경 변수가 적용되지 않음

**증상:** 환경 변수 변경 후에도 이전 URL이 사용됨

**해결 방법:**

1. Vite 캐시 삭제:

```bash
rm -rf node_modules/.vite
```

2. 환경 변수 로드 확인:

```typescript
// vite.config.ts
const env = loadEnv(mode, process.cwd(), '');
console.log('Payment Remote URL:', env.VITE_PAYMENT_REMOTE_URL);
```

3. 개발 서버 재시작 (환경 변수는 빌드 시점에 번들에 포함됨):

```bash
npm run dev
```

---

## 📋 체크리스트

추가 완료 전 다음을 확인하세요:

### Remote 애플리케이션

- [ ] Bootstrap 함수가 올바르게 expose되어 있음
- [ ] Module Federation 플러그인 설정 완료
- [ ] 독립 실행 가능 (포트 충돌 없음)
- [ ] remoteEntry.js 생성 확인

### Portal Shell

- [ ] `remoteRegistry.ts`에 모든 환경(dev/docker/k8s) 추가
- [ ] `vite.config.ts`의 remotes에 추가
- [ ] 모든 `.env.*` 파일에 환경 변수 추가
- [ ] TypeScript 타입 선언 추가 (선택)
- [ ] 캐시 삭제 후 재시작

### 테스트

- [ ] Remote 애플리케이션 독립 실행 확인
- [ ] remoteEntry.js URL 접근 확인
- [ ] Portal Shell에서 Remote 라우팅 동작 확인
- [ ] 콘솔에 에러 없음
- [ ] Network 탭에서 remoteEntry.js 로드 확인

---

## 🎯 고급 주제

### 동적 Remote 로딩

Runtime에 Remote를 동적으로 추가하려면:

```typescript
// src/utils/dynamicRemote.ts
export async function loadDynamicRemote(url: string, scope: string, module: string) {
  await __webpack_init_sharing__('default');
  const container = window[scope];
  await container.init(__webpack_share_scopes__.default);
  const factory = await container.get(module);
  return factory();
}
```

### Remote 간 통신

Remote끼리 직접 통신하지 말고 Portal Shell을 통해 통신하세요:

```typescript
// Remote A에서 이벤트 발행
sharedModules.eventBus.emit('payment:success', { orderId: 123 });

// Remote B에서 이벤트 구독
sharedModules.eventBus.on('payment:success', (data) => {
  console.log('Order paid:', data.orderId);
});
```

---

## ➡️ 다음 단계

1. **개발 워크플로우**: [portal-shell-workflow.md](./portal-shell-workflow.md)
2. **Remote 애플리케이션 개발 가이드**: [../../../blog-frontend/docs/guides/](../../../blog-frontend/docs/guides/)


---

## 🔗 관련 문서

- [Module Federation 공식 문서](https://webpack.js.org/concepts/module-federation/)
- [Vite Plugin Federation](https://github.com/originjs/vite-plugin-federation)
- [Portal Shell Architecture](../../architecture/portal-shell/system-overview.md)

---

**최종 업데이트**: 2026-01-18
