# Portal Shell

마이크로 프론트엔드의 Host 애플리케이션입니다.

## 개요

Module Federation을 사용하여 Blog, Shopping Remote 모듈을 통합하고, 인증/테마 등 공통 기능을 제공합니다.

## 포트

- 개발 서버: `30000`

## 주요 기능

| 기능 | 설명 |
|------|------|
| Remote 로딩 | Blog/Shopping 동적 로딩 |
| 인증 관리 | OAuth2 PKCE 로그인 |
| 테마 시스템 | Light/Dark, 서비스별 테마 |
| 라우팅 | 중앙 집중식 라우팅 |

## 기술 스택

- **Framework**: Vue 3
- **Build**: Vite + Module Federation
- **State**: Pinia
- **Router**: Vue Router
- **UI**: Design System (@portal/design-system)

## 프로젝트 구조

```
portal-shell/
├── src/
│   ├── components/
│   │   ├── LoginModal.vue
│   │   ├── ThemeToggle.vue
│   │   └── RemoteWrapper.vue
│   ├── views/
│   │   ├── HomePage.vue
│   │   ├── CallbackPage.vue
│   │   ├── SignupPage.vue
│   │   └── NotFound.vue
│   ├── store/
│   │   ├── auth.ts
│   │   └── theme.ts
│   ├── services/
│   │   ├── authService.ts
│   │   └── remoteLoader.ts
│   ├── api/
│   │   ├── apiClient.ts
│   │   └── users.ts
│   ├── config/
│   │   └── remoteRegistry.ts
│   ├── router/
│   │   └── index.ts
│   ├── App.vue
│   └── main.ts
└── vite.config.ts
```

## Module Federation 설정

```typescript
// vite.config.ts
export default defineConfig({
  plugins: [
    vue(),
    federation({
      name: 'portal-shell',
      remotes: {
        blogFrontend: 'http://localhost:30001/assets/remoteEntry.js',
        shoppingFrontend: 'http://localhost:30002/assets/remoteEntry.js'
      },
      exposes: {
        './apiClient': './src/api/apiClient.ts',
        './authStore': './src/store/auth.ts'
      },
      shared: ['vue', 'vue-router', 'pinia']
    })
  ]
})
```

## 라우팅

| 경로 | 컴포넌트 | 설명 |
|------|----------|------|
| `/` | HomePage | 메인 페이지 |
| `/signup` | SignupPage | 회원가입 |
| `/callback` | CallbackPage | OAuth 콜백 |
| `/blog/*` | RemoteWrapper | Blog Remote |
| `/shopping/*` | RemoteWrapper | Shopping Remote |

## 환경 변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `VITE_API_BASE_URL` | API Gateway URL | http://localhost:8080 |
| `VITE_AUTH_URL` | Auth Service URL | http://localhost:8081 |
| `VITE_BLOG_REMOTE_URL` | Blog Remote URL | http://localhost:30001 |
| `VITE_SHOPPING_REMOTE_URL` | Shopping Remote URL | http://localhost:30002 |

## 실행

```bash
cd frontend
npm run dev:portal
```

## 관련 문서

- [ARCHITECTURE.md](./ARCHITECTURE.md) - 아키텍처 상세
- [FEDERATION.md](./FEDERATION.md) - Module Federation 가이드
