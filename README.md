# Portal Universe

[![CI](https://github.com/L-a-z-e/portal-universe/actions/workflows/ci.yml/badge.svg)](https://github.com/L-a-z-e/portal-universe/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen)

> Spring Boot 3 + Vue 3/React 18 + Kubernetes 기반 마이크로서비스 플랫폼

## Overview

- **마이크로서비스 아키텍처**: 5개 백엔드 서비스 + 마이크로 프론트엔드
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

| Service | Port | Description |
|---------|------|-------------|
| API Gateway | 8080 | 라우팅, JWT 검증, Circuit Breaker |
| Auth Service | 8081 | OAuth2 인증, JWT 발급, 소셜 로그인 |
| Blog Service | 8082 | 게시글/시리즈/댓글, S3 파일 업로드 |
| Shopping Service | 8083 | 상품/장바구니/주문/결제/재고, Saga 패턴 |
| Notification Service | 8084 | Kafka 이벤트 소비, 실시간 알림 |

## Frontend (Micro-Frontend)

| Module | Port | Tech | Description |
|--------|------|------|-------------|
| Portal Shell | 30000 | Vue 3 | Host 앱, 인증/라우팅 |
| Blog Frontend | 30001 | Vue 3 | 블로그 마이크로앱 |
| Shopping Frontend | 30002 | React 18 | 쇼핑몰 + Admin |
| Design System | - | Vue 3 | 공유 UI 컴포넌트 |

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
| Database | MySQL 8.0, MongoDB, Redis, Elasticsearch |
| Messaging | Apache Kafka 4.1.0 (KRaft) |
| Infrastructure | Docker, Kubernetes, GitHub Actions |

## Documentation

| Category | Links |
|----------|-------|
| **Guides** | [Docker Compose](docs/guides/docker-compose.md) · [Kubernetes](docs/guides/kubernetes.md) · [Configuration](docs/guides/configuration.md) |
| **Architecture** | [ADR](docs/adr/) · [Diagrams](docs/diagrams/) |
| **API** | [Shopping](docs/api/shopping-api-reference.md) · [Coupon](docs/api/coupon-api.md) · [TimeDeal](docs/api/timedeal-api.md) · [Admin Products](docs/api/admin-products-api.md) |
| **Operations** | [Runbooks](docs/runbooks/) · [Troubleshooting](docs/troubleshooting/) |
| **Roadmap** | [v2.0 Roadmap](docs/ROADMAP.md) |

## Features

### Backend
- **인증**: OAuth2/JWT, 소셜 로그인
- **블로그**: 게시글/시리즈/태그/댓글, 마크다운 에디터
- **이커머스**: 상품/장바구니/주문/결제/배송
- **재고 관리**: Pessimistic Lock 동시성 제어
- **주문 처리**: Saga Orchestration 패턴
- **쿠폰**: Redis + Lua 선착순 발급
- **타임딜**: 플래시세일 기능
- **알림**: Kafka 이벤트 기반 실시간 알림

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

**Last Updated**: 2026-01
**Version**: 0.0.1-SNAPSHOT
