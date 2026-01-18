---
id: config-service-docs
title: Config Service 문서
type: index
status: current
created: 2026-01-18
updated: 2026-01-18
author: Portal Universe Team
tags: [config-service, documentation, spring-cloud-config]
---

# Config Service 문서

> Spring Cloud Config Server - 중앙 집중식 설정 관리 서비스

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **서비스명** | Config Service |
| **포트** | 8888 |
| **역할** | 모든 마이크로서비스의 설정을 중앙에서 관리 |
| **기술 스택** | Spring Boot 3.5.5, Spring Cloud 2025.0.0, Spring Cloud Config Server |

---

## 📚 문서 목록

### 🏗️ Architecture

시스템 구조와 데이터 흐름을 설명하는 문서입니다.

| 문서 | 설명 |
|------|------|
| [System Overview](./architecture/system-overview.md) | Config Service 전체 아키텍처 |
| [Data Flow](./architecture/data-flow.md) | 설정 데이터 흐름 및 갱신 프로세스 |

### 📡 API

API 명세 및 사용법을 설명하는 문서입니다.

| 문서 | 설명 |
|------|------|
| [Config API](./api/config-api.md) | 설정 조회, 암호화/복호화, Actuator API 명세 |

### 📖 Guides

개발자를 위한 가이드 문서입니다.

| 문서 | 설명 |
|------|------|
| [Getting Started](./guides/getting-started.md) | Config Service 시작 가이드 |
| [Client Configuration](./guides/client-configuration.md) | 클라이언트 서비스 설정 방법 |

### 🔧 Runbooks

운영 절차서입니다.

| 문서 | 설명 |
|------|------|
| [Deployment](./runbooks/deployment.md) | 배포 절차 (로컬, Docker, K8s) |
| [Incident Response](./runbooks/incident-response.md) | 장애 대응 절차 |
| [Config Refresh](./runbooks/config-refresh.md) | 설정 갱신 절차 |

---

## 🚀 빠른 시작

### 1. 서비스 실행

```bash
# 로컬 실행
./gradlew :services:config-service:bootRun

# Docker 실행
docker-compose up config-service
```

### 2. 상태 확인

```bash
curl http://localhost:8888/actuator/health
```

### 3. 설정 조회

```bash
# auth-service의 local 프로파일 설정 조회
curl http://localhost:8888/auth-service/local
```

---

## 📁 문서 구조

```
docs/
├── README.md              # 현재 문서 (인덱스)
├── architecture/          # 아키텍처 문서
│   ├── README.md
│   ├── system-overview.md
│   └── data-flow.md
├── api/                   # API 명세
│   ├── README.md
│   └── config-api.md
├── guides/                # 개발자 가이드
│   ├── README.md
│   ├── getting-started.md
│   └── client-configuration.md
├── runbooks/              # 운영 절차서
│   ├── README.md
│   ├── deployment.md
│   ├── incident-response.md
│   └── config-refresh.md
└── backup/                # 기존 문서 백업
    ├── ARCHITECTURE.md
    └── README.md
```

---

## 🔗 관련 링크

- [Portal Universe CLAUDE.md](/CLAUDE.md) - 프로젝트 전체 가이드
- [Config Repository](https://github.com/L-a-z-e/portal-universe-config-repo.git) - 설정 저장소
- [Spring Cloud Config 공식 문서](https://docs.spring.io/spring-cloud-config/docs/current/reference/html/)

---

## 📞 담당자

| 역할 | 담당 |
|------|------|
| 서비스 관리 | Portal Universe Team |
| 문서 관리 | Portal Universe Team |

---

**최종 업데이트**: 2026-01-18
