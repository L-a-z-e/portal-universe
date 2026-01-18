# Architecture Documentation

Portal Shell의 아키텍처 문서 모음입니다.

---

## 📄 문서 목록

| 문서 | 설명 | 상태 |
|------|------|------|
| [System Overview](./system-overview.md) | 전체 시스템 구조 및 컴포넌트 설명 | ✅ Current |
| [Module Federation](./module-federation.md) | Module Federation 상세 아키텍처 | ✅ Current |
| [Authentication](./authentication.md) | OAuth2 PKCE 인증 흐름 | ✅ Current |

---

## 🏗️ 아키텍처 개요

Portal Shell은 마이크로 프론트엔드 아키텍처의 Host 애플리케이션으로, 다음과 같은 핵심 기능을 제공합니다:

### 1. Module Federation Host
- Remote 모듈(blog, shopping) 동적 로딩
- apiClient, authStore, themeStore 노출
- 환경별 Remote URL 관리 (dev/docker/k8s)

### 2. 인증 시스템
- OAuth2 Authorization Code + PKCE Flow
- Silent Renewal (자동 토큰 갱신)
- JWT 기반 인증

### 3. 라우팅
- Vue Router 4
- Shell 라우트 + Remote 라우트 통합
- 동적 라우트 생성 (remoteRegistry 기반)

### 4. 상태 관리
- Pinia Store (auth, theme)
- Remote 모듈과 상태 공유

---

## 📊 기술 스택

| 계층 | 기술 |
|------|------|
| **프레임워크** | Vue 3 (Composition API + `<script setup>`) |
| **빌드 도구** | Vite 7.x |
| **언어** | TypeScript 5.9 |
| **Module Federation** | @originjs/vite-plugin-federation |
| **라우팅** | Vue Router 4 |
| **상태 관리** | Pinia |
| **인증** | oidc-client-ts |
| **HTTP 클라이언트** | Axios |
| **스타일링** | TailwindCSS, @portal/design-system |

---

## 🔄 주요 흐름

### 애플리케이션 초기화

```
User → Portal Shell → Theme Store 초기화
                  → OIDC 메타데이터 로드
                  → Auth Store 확인
                  → (토큰 있으면) 자동 로그인
```

### Remote 모듈 로딩

```
/blog 라우트 → RemoteWrapper
            → remoteRegistry에서 config 조회
            → remoteEntry.js 동적 로드
            → bootstrap 함수 호출
            → Remote 앱 마운트
```

### 인증 흐름

```
로그인 클릭 → Auth Service로 리다이렉트
           → 사용자 인증
           → Code 발급
           → Token 교환 (PKCE)
           → Auth Store에 사용자 정보 저장
```

---

## 📦 디렉토리 구조

```
src/
├── api/                   # API 클라이언트 (axios)
│   └── apiClient.ts       # Exposed to Remote
├── components/            # 공통 컴포넌트
│   ├── RemoteWrapper.vue  # Remote 모듈 래퍼
│   ├── LoginModal.vue     # 로그인 모달
│   └── ThemeToggle.vue    # 테마 전환
├── config/
│   └── remoteRegistry.ts  # Remote 설정 (dev/docker/k8s)
├── router/
│   └── index.ts           # Vue Router 설정
├── services/
│   ├── authService.ts     # OAuth2 인증 서비스
│   └── remoteLoader.ts    # Remote 동적 로딩
├── store/
│   ├── auth.ts            # Exposed to Remote
│   └── theme.ts           # Exposed to Remote
├── types/
│   └── user.ts            # TypeScript 타입 정의
├── utils/
│   └── jwt.ts             # JWT 파싱
└── views/                 # 페이지 컴포넌트
    ├── HomePage.vue
    ├── SignupPage.vue
    ├── CallbackPage.vue   # OAuth Callback
    └── NotFound.vue
```

---

## 🌐 포트 및 URL

| 서비스 | 포트 | URL |
|--------|------|-----|
| Portal Shell | 30000 | http://localhost:30000 |
| Blog Remote | 30001 | http://localhost:30001 |
| Shopping Remote | 30002 | http://localhost:30002 |
| API Gateway | 8080 | http://localhost:8080 |
| Auth Service | 8081 | http://localhost:8081 |

---

## 🔐 보안

### 인증 방식
- OAuth2 Authorization Code + PKCE Flow
- JWT Access Token (Bearer Token)
- Silent Renewal (자동 토큰 갱신)

### 토큰 저장
- localStorage (WebStorageStateStore)
- 만료 시 자동 로그아웃

### CORS 정책
- API Gateway에서 CORS 처리
- Vite Proxy: /auth-service, /api

---

## 📈 성능 목표

| 지표 | 목표 |
|------|------|
| 초기 로드 시간 | < 1s |
| Remote 로드 시간 | < 500ms |
| 인증 처리 시간 | < 300ms |
| 라우팅 전환 시간 | < 100ms |

---

## 🔗 관련 문서

### 프로젝트 문서
- [API 명세](../api/)
- [가이드](../guides/)
- [Blog Frontend Architecture](../../blog-frontend/docs/architecture/)
- [Shopping Frontend Architecture](../../shopping-frontend/docs/architecture/)

### 백엔드 문서
- [Auth Service Architecture](../../../services/auth-service/docs/architecture/)
- [API Gateway Architecture](../../../services/api-gateway/docs/architecture/)

### 외부 참고 자료
- [Module Federation 공식 문서](https://module-federation.github.io/)
- [Vue 3 공식 문서](https://vuejs.org/)
- [oidc-client-ts GitHub](https://github.com/authts/oidc-client-ts)
- [OAuth 2.0 PKCE RFC](https://datatracker.ietf.org/doc/html/rfc7636)

---

**최종 업데이트**: 2026-01-18
