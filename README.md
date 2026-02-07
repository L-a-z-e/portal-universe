# Portal Universe

[![CI](https://github.com/L-a-z-e/portal-universe/actions/workflows/ci.yml/badge.svg)](https://github.com/L-a-z-e/portal-universe/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen)

> Spring Boot 3 + Vue 3/React 18 + Kubernetes 기반 마이크로서비스 플랫폼

## Overview

- **마이크로서비스 아키텍처**: 7개 백엔드 서비스 + 마이크로 프론트엔드
- **Phase 1 완료**: 이커머스 핵심 기능 (Saga 패턴, 재고 동시성 제어)
- **관찰성 스택**: Prometheus, Grafana, Zipkin, Loki
- **이벤트 드리븐**: Kafka 기반 비동기 통신

## Quick Start

```bash
git clone https://github.com/L-a-z-e/portal-universe.git
cd portal-universe
docker compose up -d
```

## Services

| Service | Port | Description | API Docs |
|---------|------|-------------|----------|
| API Gateway | 8080 | 라우팅, JWT 검증, Circuit Breaker | - |
| Auth Service | 8081 | 인증/인가 (JWT, OAuth2, RBAC), 멤버십, 셀러 | [📖 API](docs/api/auth-service/) |
| Blog Service | 8082 | 게시글/시리즈/댓글, S3 파일 업로드 | [📖 API](docs/api/blog-service/) |
| Shopping Service | 8083 | 상품/장바구니/주문/결제/재고, Saga 패턴 | [📖 API](docs/api/shopping-service/) |
| Notification Service | 8084 | Kafka 이벤트 소비, 실시간 알림 | [📖 API](docs/api/notification-service/) |
| Prism Service | 8085 | AI 기반 작업 관리, 칸반 보드 | [📖 API](https://www.notion.so/2f73df01028f81868293f88213d1a69c) |
| Chatbot Service | 8086 | AI 챗봇, RAG 기반 대화 | [📖 API](docs/api/chatbot-service/) |

## Frontend (Micro-Frontend)

| Module | Port | Tech | Description |
|--------|------|------|-------------|
| Portal Shell | 30000 | Vue 3 | Host 앱, 인증/라우팅 |
| Blog Frontend | 30001 | Vue 3 | 블로그 마이크로앱 |
| Shopping Frontend | 30002 | React 18 | 쇼핑몰 + Admin |
| Prism Frontend | 30003 | React 18 | AI 작업 관리, 칸반 |
| Admin Frontend | 30004 | Vue 3 | 관리자 대시보드 (MF Remote) |
| Design System | - | Vue 3 + React 18 | 공유 UI 컴포넌트 |

## Monitoring & Tools

| Tool | Port | Description |
|------|------|-------------|
| Prometheus | 9090 | 메트릭 수집 |
| Grafana | 3000 | 대시보드 (admin/password) |
| Zipkin | 9411 | 분산 추적 |
| Loki | 3100 | 로그 집계 |
| Alertmanager | 9093 | 알림 관리 |
| Kibana | 5601 | Elasticsearch UI |
| Dozzle | 9999 | 컨테이너 로그 뷰어 |

## Tech Stack

| Layer | Technologies |
|-------|--------------|
| Backend | Java 17, Spring Boot 3.5.5, Spring Cloud 2025.0.0 |
| Frontend | Vue 3, React 18, Vite, Module Federation |
| Database | MySQL 8.0, PostgreSQL, MongoDB, Redis, Elasticsearch |
| Messaging | Apache Kafka 4.1.0 (KRaft) |
| Infrastructure | Docker, Kubernetes, GitHub Actions |

## Documentation

### 📚 Complete Documentation (Notion)
**[→ Portal Universe 전체 문서](https://www.notion.so/l-a-z-e/Portal-Universe-2f73df01028f802cb03ff36054182571)**

상세한 문서는 Notion에서 확인하실 수 있습니다:
- **[API 문서](https://www.notion.so/l-a-z-e/Portal-Universe-2f73df01028f802cb03ff36054182571)**: Auth, Blog, Shopping, Notification, Prism 서비스 (150+ endpoints)
- **[ADR](https://www.notion.so/2f73df01028f81159c7bc76326526359)**: 아키텍처 결정 기록 (15개 주요 결정사항)
- **[실제 사용 시나리오](https://www.notion.so/2f73df01028f813dba5ccea2f8995bc9)**: 주문 플로우, Saga 패턴, 타임딜, AI 작업 등

### GitHub Docs

| Category | Links |
|----------|-------|
| **Guides** | [Docker Compose](docs/guides/deployment/docker-compose.md) · [Kubernetes](docs/guides/deployment/k8s-deployment-guide.md) · [Getting Started](docs/guides/development/getting-started.md) |
| **Architecture** | [ADR](docs/adr/) · [Auth](docs/architecture/auth-service/) · [Database](docs/architecture/database/) |
| **API** | [Auth](docs/api/auth-service/) · [Shopping](docs/api/shopping-service/) · [Blog](docs/api/blog-service/) · [Notification](docs/api/notification-service/) · [Chatbot](docs/api/chatbot-service/) |
| **Operations** | [Runbooks](docs/runbooks/) · [Troubleshooting](docs/troubleshooting/) |

## Features

### Backend
- **인증/인가**: OAuth2/JWT, 소셜 로그인, 계층적 RBAC, 감사 로그
- **멤버십/셀러**: 서비스별 다중 티어, 셀러 신청/심사 워크플로우
- **팔로우**: 팔로우/언팔로우, 팔로워/팔로잉 관리
- **블로그**: 게시글/시리즈/태그/댓글, 마크다운 에디터
- **이커머스**: 상품/장바구니/주문/결제/배송
- **재고 관리**: Pessimistic Lock 동시성 제어
- **주문 처리**: Saga Orchestration 패턴
- **쿠폰**: Redis + Lua 선착순 발급
- **타임딜**: 플래시세일 기능
- **알림**: Kafka 이벤트 기반 실시간 알림
- **AI 작업 관리**: 칸반 보드, AI 실행 (Prism)
- **AI 챗봇**: RAG 기반 대화 (Chatbot)

### Frontend
- **마이크로 프론트엔드**: Module Federation
- **블로그**: Toast UI 에디터, Syntax Highlighting
- **쇼핑몰**: 상품 조회/장바구니/주문
- **Admin**: 상품/쿠폰/타임딜 관리

### Infrastructure
- **관찰성**: Prometheus, Grafana, Zipkin, Loki
- **메시징**: Apache Kafka (KRaft)
- **컨테이너**: Docker, Kubernetes

## Roadmap

- [ ] Phase 2: 대기열 시스템, 부하 테스트
- [ ] Phase 3: WebSocket 실시간, Elasticsearch 검색
- [ ] Phase 4: CI/CD 고도화, 운영 자동화

## Contributing

1. Fork the repository
2. Create feature branch: `git checkout -b feature/your-feature`
3. Commit changes: `git commit -am 'feat: add feature'`
4. Push branch: `git push origin feature/your-feature`
5. Submit Pull Request

## License

MIT License - see [LICENSE](LICENSE) for details.

---

**Last Updated**: 2026-02
**Version**: 0.0.1-SNAPSHOT
