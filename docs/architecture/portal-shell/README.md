# Portal Shell Architecture Documentation

Portal Shell의 아키텍처 문서 모음입니다.

---

## 문서 목록

| 문서 | 설명 | 상태 |
|------|------|------|
| [System Overview](./system-overview.md) | 전체 시스템 구조 및 컴포넌트 설명 | Current |
| [Module Federation](./module-federation.md) | Module Federation 상세 아키텍처 | Current |
| [Authentication](./authentication.md) | JWT + HttpOnly Cookie 인증 흐름 | Current |
| [Realtime Communication](./realtime-communication.md) | WebSocket 알림 + AI 챗봇 스트리밍 | Current |
| [Cross-Framework Bridge](./cross-framework-bridge.md) | Vue Pinia → React storeAdapter 브릿지 | Current |

---

## 아키텍처 개요

Portal Shell은 마이크로 프론트엔드 아키텍처의 Host 애플리케이션으로, 다음과 같은 핵심 기능을 제공합니다:

### 1. Module Federation Host
- Remote 모듈(Blog, Shopping, Prism) 동적 로딩
- `portal/api` (apiClient), `portal/stores` (auth, theme, storeAdapter) 노출
- 환경별 Remote URL 관리 (dev/docker/k8s)
- CSS 생명주기 관리 (MutationObserver), keep-alive 지원

### 2. 인증 시스템
- JWT 기반 Direct Authentication
- Access Token: 메모리 저장 (XSS 방어)
- Refresh Token: HttpOnly Cookie (CSRF 방어)
- 소셜 로그인 (Google, Naver, Kakao) - Server Redirect 방식
- 자동 토큰 갱신 (60초 버퍼), 401 재시도

### 3. 실시간 통신
- WebSocket 알림: STOMP/SockJS, 인증 상태 연동
- AI 챗봇: REST + SSE 스트리밍

### 4. Cross-Framework 상태 공유
- storeAdapter: Pinia → React `useSyncExternalStore` 브릿지
- `portal:auth-changed` CustomEvent
- `window.__PORTAL_*` 전역 함수

### 5. 라우팅 및 상태 관리
- Vue Router 4 - Shell 라우트 + Remote 라우트 통합
- Pinia Store (auth, theme, notification, settings)
- 양방향 네비게이션 동기화 (debounce 50ms)

---

## 기술 스택

| 계층 | 기술 |
|------|------|
| **프레임워크** | Vue 3 (Composition API + `<script setup>`) |
| **빌드 도구** | Vite 7.x |
| **언어** | TypeScript 5.9 |
| **Module Federation** | @originjs/vite-plugin-federation |
| **라우팅** | Vue Router 4 |
| **상태 관리** | Pinia |
| **HTTP 클라이언트** | Axios |
| **실시간** | @stomp/stompjs, sockjs-client |
| **스타일링** | TailwindCSS, @portal/design-system-vue |

---

## Remote 앱

| Remote | 포트 | 프레임워크 | basePath | 설명 |
|--------|------|-----------|----------|------|
| Blog | 30001 | Vue 3 | `/blog` | 블로그 서비스 |
| Shopping | 30002 | React 18 | `/shopping` | 쇼핑 서비스 |
| Prism | 30003 | React 18 | `/prism` | AI 에이전트 오케스트레이션 |

---

## 보안

### 인증 방식
- JWT + HttpOnly Cookie (OIDC/PKCE 아님)
- Access Token: JavaScript 메모리 저장
- Refresh Token: HttpOnly Cookie (auth-service Set-Cookie)
- 소셜 로그인: Server-redirect OAuth2 (Google, Naver, Kakao)

### 토큰 갱신
- 만료 60초 전 자동 갱신 (`autoRefreshIfNeeded`)
- 401 응답 시 refresh → 원본 요청 재시도
- `refreshPromise` 중복 방지

### CORS 정책
- API Gateway에서 CORS 처리
- Vite Proxy: 개발 환경 프록시

---

## 포트 및 URL

| 서비스 | 포트 | URL |
|--------|------|-----|
| Portal Shell | 30000 | http://localhost:30000 |
| Blog Remote | 30001 | http://localhost:30001 |
| Shopping Remote | 30002 | http://localhost:30002 |
| Prism Remote | 30003 | http://localhost:30003 |
| API Gateway | 8080 | http://localhost:8080 |
| Auth Service | 8081 | http://localhost:8081 |

---

## 관련 문서

### 프로젝트 문서
- [API 명세](../../api/)
- [가이드](../../guides/)

### 백엔드 문서
- [Auth Service Architecture](../auth-service/system-overview.md)
- [API Gateway Architecture](../api-gateway/system-overview.md)

### 외부 참고 자료
- [Module Federation 공식 문서](https://module-federation.github.io/)
- [Vue 3 공식 문서](https://vuejs.org/)
