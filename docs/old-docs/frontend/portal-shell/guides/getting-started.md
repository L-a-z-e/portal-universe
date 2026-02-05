---
id: portal-shell-getting-started
title: Portal Shell - Getting Started
type: guide
status: current
created: 2026-01-18
updated: 2026-01-18
author: Laze
tags: [portal-shell, setup, environment, vue3, vite, module-federation]
related:
  - portal-shell-development
  - portal-shell-adding-remote
---

# Portal Shell - Getting Started

> Portal Shell 개발 환경 설정 및 실행 가이드

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **예상 소요 시간** | 15-20분 |
| **대상** | Portal Shell 신규 개발자 |
| **프레임워크** | Vue 3 (Composition API) |
| **빌드 도구** | Vite 7.x |
| **포트** | 30000 |

Portal Shell은 Module Federation을 통해 마이크로 프론트엔드를 통합하는 Host 애플리케이션입니다.

---

## ✅ 사전 요구사항

### 필수 소프트웨어

| 소프트웨어 | 권장 버전 | 확인 명령어 |
|-----------|----------|------------|
| Node.js | 20.x LTS | `node --version` |
| npm | 10.x | `npm --version` |
| Git | 최신 | `git --version` |

### 선택 사항

| 소프트웨어 | 용도 |
|-----------|------|
| VS Code | 에디터 (권장) |
| Vue.js Devtools | 브라우저 확장 (디버깅) |

---

## 🔧 환경 설정

### Step 1: 저장소 클론 및 디렉토리 이동

```bash
git clone <repository-url>
cd frontend/portal-shell
```

### Step 2: 의존성 설치

**Root 디렉토리에서 전체 워크스페이스 설치 (권장):**

```bash
cd frontend
npm install
```

**또는 portal-shell만 설치:**

```bash
cd frontend/portal-shell
npm install
```

### Step 3: 환경 변수 설정

portal-shell은 환경별로 다른 `.env` 파일을 사용합니다:

| 환경 | 파일 | 용도 |
|------|------|------|
| 로컬 개발 | `.env.dev` | localhost 환경 |
| Docker | `.env.docker` | Docker Compose 환경 |
| Kubernetes | `.env.k8s` | K8s 클러스터 환경 |

**로컬 개발 시 기본 설정 (.env.dev):**

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
```

> ⚠️ **주의**: 환경 변수는 빌드 시점에 번들에 포함됩니다. 변경 후 재빌드가 필요합니다.

---

## 🚀 실행

### 개발 모드 실행

**portal-shell만 실행:**

```bash
cd frontend
npm run dev:portal
```

**또는 직접:**

```bash
cd frontend/portal-shell
npm run dev
```

**전체 마이크로 프론트엔드 실행 (권장):**

```bash
cd frontend
npm run dev
```

이 명령어는 다음을 동시에 실행합니다:
- portal-shell (포트 30000)
- blog-frontend (포트 30001)
- shopping-frontend (포트 30002)
- design-system (포트 30003)

### 빌드

**개발 빌드:**

```bash
npm run build:dev
```

**Docker 빌드:**

```bash
npm run build:docker
```

**Kubernetes 빌드:**

```bash
npm run build:k8s
```

---

## ✅ 실행 확인

### 1. 브라우저 접속

```
http://localhost:30000
```

### 2. 예상 결과

- ✅ Portal Shell 홈 페이지가 표시됨
- ✅ 상단 네비게이션에 "Blog", "Shopping" 메뉴가 표시됨
- ✅ 콘솔에 에러가 없음

### 3. Module Federation 동작 확인

**Blog Remote 확인:**

```
http://localhost:30000/blog
```

Blog 마이크로 프론트엔드가 동적으로 로드되어야 합니다.

**Shopping Remote 확인:**

```
http://localhost:30000/shopping
```

Shopping 마이크로 프론트엔드가 동적으로 로드되어야 합니다.

### 4. 콘솔 로그 확인

정상 동작 시 콘솔에 다음과 같은 로그가 표시됩니다:

```
🔧 [Vite Config] Building for mode: dev
🔧 [Vite Config] Blog Remote URL: http://localhost:30001/assets/remoteEntry.js
🔧 [Vite Config] Shopping Remote URL: http://localhost:30002/assets/remoteEntry.js
✅ [Portal Shell] Initialized
```

---

## ⚠️ 자주 발생하는 문제

### 문제 1: Remote 모듈 로드 실패

**증상:**

```
❌ Failed to fetch dynamically imported module
```

**원인:** Remote 애플리케이션이 실행되지 않았거나 URL이 잘못됨

**해결 방법:**

1. Remote 애플리케이션 실행 확인:

```bash
# blog-frontend 확인
curl http://localhost:30001/assets/remoteEntry.js

# shopping-frontend 확인
curl http://localhost:30002/assets/remoteEntry.js
```

2. 환경 변수 확인:

```bash
cat .env.dev
```

3. Remote 애플리케이션 재시작:

```bash
cd frontend
npm run dev
```

### 문제 2: 포트 충돌

**증상:**

```
Port 30000 is already in use
```

**해결 방법:**

1. 실행 중인 프로세스 확인:

```bash
lsof -i :30000
```

2. 프로세스 종료:

```bash
kill -9 <PID>
```

3. 또는 다른 포트 사용:

```bash
vite preview --port 30010 --strictPort
```

### 문제 3: 환경 변수가 적용되지 않음

**증상:** 환경 변수 변경 후에도 이전 값이 사용됨

**원인:** Vite는 환경 변수를 빌드 시점에 번들에 포함

**해결 방법:**

1. 개발 서버 재시작:

```bash
# Ctrl+C로 종료 후
npm run dev
```

2. 캐시 삭제 후 재시작:

```bash
rm -rf node_modules/.vite
npm run dev
```

### 문제 4: TypeScript 타입 에러

**증상:**

```
Cannot find module '@portal/design-system' or its corresponding type declarations
```

**해결 방법:**

1. TypeScript 빌드 실행:

```bash
vue-tsc -b
```

2. 타입 선언 파일 확인:

```bash
cat design-system.d.ts
```

내용이 다음과 같아야 합니다:

```typescript
declare module '@portal/design-system' {
  const content: any;
  export default content;
}
```

### 문제 5: CORS 에러

**증상:**

```
Access to fetch at 'http://localhost:8080/api/...' has been blocked by CORS policy
```

**원인:** API Gateway가 실행되지 않았거나 CORS 설정 문제

**해결 방법:**

1. API Gateway 실행 확인:

```bash
curl http://localhost:8080/actuator/health
```

2. Vite 프록시 설정 확인 (vite.config.ts):

```typescript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  }
}
```

---

## 📁 프로젝트 구조

```
portal-shell/
├── src/
│   ├── api/                  # API 클라이언트
│   │   ├── apiClient.ts      # Axios 인스턴스 (Remote에 노출)
│   │   └── authApi.ts        # 인증 API
│   ├── components/           # 공통 컴포넌트
│   │   ├── Header.vue        # 헤더 (네비게이션)
│   │   ├── Footer.vue        # 푸터
│   │   └── RemoteWrapper.vue # Remote 모듈 래퍼
│   ├── config/               # 설정
│   │   └── remoteRegistry.ts # Remote 모듈 레지스트리
│   ├── router/               # Vue Router
│   │   └── index.ts          # 라우터 설정 (동적 Remote 라우트)
│   ├── services/             # 비즈니스 로직
│   │   └── authService.ts    # 인증 서비스 (oidc-client-ts)
│   ├── store/                # Pinia 스토어
│   │   ├── auth.ts           # 인증 상태 (Remote에 노출)
│   │   └── theme.ts          # 테마 상태 (Remote에 노출)
│   ├── types/                # TypeScript 타입
│   │   └── index.ts          # 공통 타입
│   ├── utils/                # 유틸리티
│   │   └── logger.ts         # 로거
│   ├── views/                # 페이지 컴포넌트
│   │   ├── HomePage.vue      # 홈 페이지
│   │   ├── SignupPage.vue    # 회원가입
│   │   ├── CallbackPage.vue  # OAuth 콜백
│   │   └── NotFound.vue      # 404 페이지
│   ├── App.vue               # 루트 컴포넌트
│   └── main.ts               # 진입점
├── public/                   # 정적 파일
├── .env.dev                  # 로컬 환경 변수
├── .env.docker               # Docker 환경 변수
├── .env.k8s                  # K8s 환경 변수
├── vite.config.ts            # Vite 설정 (Module Federation)
├── tsconfig.json             # TypeScript 설정
├── package.json              # 의존성 및 스크립트
└── README.md                 # 프로젝트 개요
```

---

## 🔌 Module Federation 구성

### Exposes (노출)

Portal Shell은 다음 모듈을 Remote에 노출합니다:

| 모듈 | 경로 | 용도 |
|------|------|------|
| `./apiClient` | `src/api/apiClient.ts` | Axios 인스턴스 (API 호출) |
| `./authStore` | `src/store/auth.ts` | 인증 상태 (Pinia Store) |
| `./themeStore` | `src/store/theme.ts` | 테마 상태 (Pinia Store) |

### Remotes (소비)

Portal Shell은 다음 Remote 모듈을 소비합니다:

| Remote | URL | 모듈 | 라우팅 |
|--------|-----|------|--------|
| blog | `http://localhost:30001/assets/remoteEntry.js` | `blog/bootstrap` | `/blog/*` |
| shopping | `http://localhost:30002/assets/remoteEntry.js` | `shopping/bootstrap` | `/shopping/*` |

### Shared (공유)

다음 라이브러리가 Remote와 공유됩니다:

- `vue` - Vue 3 런타임
- `pinia` - 상태 관리
- `axios` - HTTP 클라이언트

---

## 🧪 디버깅

### Vue Devtools 사용

1. Chrome/Firefox에 Vue.js Devtools 설치
2. 개발자 도구 열기 (F12)
3. "Vue" 탭 선택
4. 컴포넌트 트리, Pinia Store, Router 확인 가능

### Vite 디버그 모드

```bash
DEBUG=vite:* npm run dev
```

### Module Federation 디버그

브라우저 콘솔에서:

```javascript
// 로드된 Remote 확인
window.__FEDERATION__

// Remote 모듈 로드 상태
console.log(__FEDERATION__.instances)
```

---

## ➡️ 다음 단계

1. **개발 워크플로우 익히기**: [development.md](./development.md)
2. **새 Remote 모듈 추가하기**: [adding-remote.md](./adding-remote.md)
3. **API 문서 참조**: [../api/](../api/)
4. **Architecture 이해하기**: [../architecture/](../architecture/)

---

## 🔗 관련 문서

- [Portal Shell Architecture](../architecture/system-overview.md)
- [Remote Registry 설계](../architecture/remote-registry.md)
- [API 명세](../api/api-spec.md)

---

**최종 업데이트**: 2026-01-18
