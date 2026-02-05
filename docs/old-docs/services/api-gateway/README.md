---
id: api-gateway-docs-index
title: API Gateway 문서 인덱스
type: index
status: current
created: 2026-01-18
updated: 2026-01-18
author: Laze
tags: [api-gateway, documentation, index]
related: []
---

# API Gateway 문서

Portal Universe 플랫폼의 API Gateway 서비스 문서입니다.

## 개요

API Gateway는 Spring Cloud Gateway 기반의 중앙 진입점으로, 모든 클라이언트 요청을 처리합니다.

### 핵심 기능

| 기능 | 설명 |
|------|------|
| **JWT 검증** | OAuth2 Resource Server를 통한 토큰 검증 |
| **서비스 라우팅** | 경로 기반 마이크로서비스 라우팅 |
| **CORS 처리** | 마이크로 프론트엔드 Cross-Origin 지원 |
| **Circuit Breaker** | Resilience4j 기반 장애 격리 |
| **분산 추적** | Zipkin 연동 트레이싱 |

### 기술 스택

- **Framework**: Spring Boot 3.5.5, Spring Cloud 2025.0.0
- **Gateway**: Spring Cloud Gateway (WebFlux)
- **Security**: OAuth2 Resource Server, JWT
- **Resilience**: Resilience4j Circuit Breaker
- **Observability**: Micrometer, Prometheus, Zipkin

### 포트

- 개발 환경: `8080`
- Docker: `api-gateway:8080`

---

## 문서 목록

### Architecture (아키텍처)

| 문서 | 설명 |
|------|------|
| [시스템 아키텍처](./architecture/system-overview.md) | API Gateway 시스템 구조, 필터 체인, 보안 흐름 |

### API (API 명세)

| 문서 | 설명 |
|------|------|
| [라우팅 명세](./api/routing-specification.md) | 라우팅 규칙, 엔드포인트 명세, CORS 설정 |

### Guides (가이드)

| 문서 | 설명 |
|------|------|
| [로컬 개발 가이드](./guides/local-development.md) | 로컬 환경 설정, 빌드, 실행, 테스트 방법 |
| [Rate Limiting](./guides/rate-limiting.md) | Redis 기반 Rate Limiting 설정 가이드 |
| [보안 헤더 테스트](./guides/security-headers-testing.md) | CSP, HSTS 등 보안 헤더 테스트 가이드 |

### Runbooks (운영 절차서)

| 문서 | 설명 |
|------|------|
| [배포 운영 절차서](./runbooks/deployment.md) | 배포 절차, 모니터링, 장애 대응 |

---

## 빠른 시작

### 빌드 및 실행

```bash
# 빌드
./gradlew :services:api-gateway:build

# 실행
./gradlew :services:api-gateway:bootRun

# 테스트
./gradlew :services:api-gateway:test
```

### 헬스 체크

```bash
curl http://localhost:8080/actuator/health
```

### 라우팅 규칙

| 경로 | 대상 서비스 | 인증 |
|------|------------|------|
| `/api/v1/auth/**` | auth-service | 불필요 |
| `/api/v1/blog/**` | blog-service | 필요 |
| `/api/v1/shopping/**` | shopping-service | 필요 |
| `/api/v1/notifications/**` | notification-service | 필요 |
| `/auth-service/**` | auth-service | 불필요 |
| `/actuator/**` | self | 불필요 |

---

## 관련 링크

- [Portal Universe CLAUDE.md](../../../CLAUDE.md) - 프로젝트 전체 개요
- [Config Service](../../config-service/docs/) - 설정 서버 문서
- [Auth Service](../../auth-service/docs/) - 인증 서비스 문서

---

## 백업 문서

이전 버전의 문서는 `backup/` 디렉토리에 보관되어 있습니다.

- [README.md.bak](./backup/README.md.bak)
- [ARCHITECTURE.md.bak](./backup/ARCHITECTURE.md.bak)
