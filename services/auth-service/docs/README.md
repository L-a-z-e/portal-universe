---
id: auth-service-docs
title: Auth Service Documentation
type: index
status: current
created: 2026-01-18
updated: 2026-01-18
author: Claude
tags: [auth-service, documentation, index]
---

# Auth Service Documentation

> Portal Universe 플랫폼의 인증/인가 서비스 문서

---

## 📋 개요

Auth Service는 Spring Authorization Server 기반 OAuth2 Authorization Server로, JWT 토큰 발급 및 사용자 인증을 담당합니다.

| 항목 | 내용 |
|------|------|
| **프레임워크** | Spring Boot 3.5.5 |
| **보안** | Spring Authorization Server |
| **데이터베이스** | MySQL |
| **메시지 큐** | Kafka |
| **포트** | 8081 |

---

## 📚 문서 목록

### 🏗️ Architecture

시스템 구조 및 설계 문서입니다.

| 문서 | 설명 |
|------|------|
| [System Overview](./architecture/system-overview.md) | 전체 시스템 아키텍처 개요 |
| [Data Flow](./architecture/data-flow.md) | OAuth2 인증 플로우, JWT 구조, Kafka 이벤트 |

### 📡 API

API 명세서입니다.

| 문서 | 설명 |
|------|------|
| [Auth API](./api/auth-api.md) | 회원가입, OAuth2, 토큰 발급/갱신 API |

### 📖 Guides

개발자 가이드입니다.

| 문서 | 설명 |
|------|------|
| [Getting Started](./guides/getting-started.md) | 개발 환경 설정 및 실행 가이드 |

---

## 🔗 핵심 기능

- **OAuth2 Authorization Code Flow with PKCE**: 프론트엔드 Public Client 지원
- **JWT 토큰 발급**: Access Token (2분), Refresh Token (7일)
- **소셜 로그인**: Google OAuth2 연동
- **이벤트 기반**: Kafka를 통한 사용자 가입 이벤트 발행

---

## 🚀 빠른 시작

```bash
# 1. 의존성 서비스 실행
docker-compose up -d mysql kafka

# 2. 애플리케이션 실행
./gradlew :services:auth-service:bootRun

# 3. 확인
curl http://localhost:8081/actuator/health
```

자세한 내용은 [Getting Started](./guides/getting-started.md)를 참조하세요.

---

## 📂 디렉토리 구조

```
docs/
├── README.md                 # 이 파일 (문서 인덱스)
├── architecture/             # 아키텍처 문서
│   ├── system-overview.md    # 시스템 개요
│   └── data-flow.md          # 데이터 흐름
├── api/                      # API 명세
│   └── auth-api.md           # Auth API
├── guides/                   # 개발자 가이드
│   └── getting-started.md    # 시작 가이드
└── backup/                   # 기존 문서 백업
```

---

## 🔗 관련 문서

- [Portal Universe CLAUDE.md](../../../CLAUDE.md) - 프로젝트 전체 가이드
- [API Gateway](../../api-gateway/docs/) - JWT 검증 및 라우팅
- [Notification Service](../../notification-service/docs/) - Kafka 이벤트 구독

---

**최종 업데이트**: 2026-01-18
