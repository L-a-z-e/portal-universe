# API Documentation

> Portal Universe 전체 서비스의 API 명세서 인덱스

---

## Backend Services

| 서비스 | 디렉토리 | 설명 | 문서 수 |
|--------|----------|------|---------|
| API Gateway | [api-gateway/](./api-gateway/) | 라우팅, 필터 체인, Rate Limiting | 1 |
| Auth Service | [auth-service/](./auth-service/) | OAuth2/JWT 인증, 회원가입 | 1 |
| Blog Service | [blog-service/](./blog-service/) | 게시물, 댓글, 시리즈, 태그, 좋아요 | 1 |
| Shopping Service | [shopping-service/](./shopping-service/) | 상품, 주문, 결제, 배송, 쿠폰, 타임딜, 대기열 | 10 |
| Notification Service | [notification-service/](./notification-service/) | 알림 CRUD, WebSocket, Kafka Events, Redis Pub/Sub | 2 |
| Chatbot Service | [chatbot-service/](./chatbot-service/) | RAG 기반 AI 채팅, 문서 관리, SSE 스트리밍 | 1 |

## Frontend Applications

| 서비스 | 디렉토리 | 설명 | 문서 수 |
|--------|----------|------|---------|
| Portal Shell | [portal-shell/](./portal-shell/) | Module Federation Host API (apiClient, authStore, themeStore) | 3 |
| Blog Frontend | [blog-frontend/](./blog-frontend/) | Blog API Client 명세 | 1 |
| Shopping Frontend | [shopping-frontend/](./shopping-frontend/) | Shopping API Client 명세 | 1 |

## Design System & Shared

| 서비스 | 디렉토리 | 설명 | 문서 수 |
|--------|----------|------|---------|
| Design System Vue | [design-system-vue/](./design-system-vue/) | Vue 3 컴포넌트 및 Composables API | 7 |
| Design System React | [design-system-react/](./design-system-react/) | React 18 컴포넌트 및 Hooks API | 4 |
| Design Tokens | [design-tokens/](./design-tokens/) | CSS Custom Properties (3-tier 토큰) | 1 |
| Design Types | [design-types/](./design-types/) | 공유 TypeScript 타입 정의 | 2 |
| Common Library | [common-library/](./common-library/) | Java 공통 라이브러리 API | 1 |

---

## 에러 코드 범위

| 서비스 | Prefix | 범위 |
|--------|--------|------|
| Common | C | C001 ~ C099 |
| Auth | A | A001 ~ A099 |
| Blog | B | B001 ~ B099 |
| Shopping | S | S001 ~ S099 |
| Notification | N | N001 ~ N099 |
| Chatbot | CB | CB001 ~ CB099 |

---

## 관련 문서

- [ADR 목록](../adr/) - Architecture Decision Records
- [아키텍처 문서](../architecture/) - 서비스별 시스템 아키텍처

---

**최종 업데이트**: 2026-02-06
