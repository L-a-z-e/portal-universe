# Architecture Documentation

> Portal Universe 전체 서비스의 아키텍처 문서 인덱스

---

## Backend Services

| 서비스 | 디렉토리 | 설명 | 문서 수 |
|--------|----------|------|---------|
| API Gateway | [api-gateway/](./api-gateway/) | 라우팅, 필터 체인, Rate Limiting | 3 |
| Auth Service | [auth-service/](./auth-service/) | OAuth2/JWT 인증, RBAC | 4 |
| Blog Service | [blog-service/](./blog-service/) | 게시물, 댓글, 시리즈, 태그 | 1 |
| Shopping Service | [shopping-service/](./shopping-service/) | 장바구니, 주문, 결제, 배송 (Buyer) | 8 |
| Shopping Seller Service | [shopping-seller-service/](./shopping-seller-service/) | 판매자, 상품, 재고 (Seller) | 1 |
| Shopping Settlement Service | [shopping-settlement-service/](./shopping-settlement-service/) | 정산 배치, Spring Batch | 1 |
| Notification Service | [notification-service/](./notification-service/) | 알림, WebSocket, Kafka Events | 3 |
| Prism Service | [prism-service/](./prism-service/) | AI 프롬프트 관리 | 1 |
| Chatbot Service | [chatbot-service/](./chatbot-service/) | RAG 기반 AI 채팅 | 1 |

## Frontend Applications

| 서비스 | 디렉토리 | 설명 | 문서 수 |
|--------|----------|------|---------|
| Portal Shell | [portal-shell/](./portal-shell/) | Module Federation Host | 6 |
| Blog Frontend | [blog-frontend/](./blog-frontend/) | Blog 프론트엔드 | 4 |
| Shopping Frontend | [shopping-frontend/](./shopping-frontend/) | Shopping 프론트엔드 | 4 |
| Prism Frontend | [prism-frontend/](./prism-frontend/) | Prism 프론트엔드 | 4 |
| Admin Frontend | [admin-frontend/](./admin-frontend/) | 관리자 페이지 | 1 |
| Shopping Seller Frontend | [shopping-seller-frontend/](./shopping-seller-frontend/) | 판매자 관리 (React MF Remote) | 1 |

## Design System

| 서비스 | 디렉토리 | 설명 | 문서 수 |
|--------|----------|------|---------|
| Design System | [design-system/](./design-system/) | Vue/React 디자인 시스템 통합 | 7 |

## Infrastructure

| 서비스 | 디렉토리 | 설명 | 문서 수 |
|--------|----------|------|---------|
| Database | [database/](./database/) | ERD, Migration 전략 | 8 |
| System | [system/](./system/) | 통합 시스템 아키텍처, 보안, 모니터링 | 7 |

---

## 관련 문서

- [API 문서](../api/) - 서비스별 API 명세서
- [ADR 목록](../adr/) - Architecture Decision Records

---

**최종 업데이트**: 2026-02-18
