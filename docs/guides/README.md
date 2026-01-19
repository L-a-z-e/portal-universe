# Developer Guides

Portal Universe 프로젝트의 개발자를 위한 가이드 문서입니다.

## 가이드 목록

### 환경 구성

#### Docker Compose 배포 가이드
**파일**: [docker-compose.md](./docker-compose.md)

Docker Compose를 사용한 로컬 개발 환경 구성 방법을 설명합니다.

**주요 내용**:
- 사전 요구사항 및 빠른 시작
- 서비스별 시작 방법 (인프라, 백엔드, 모니터링)
- 서비스 포트 매핑
- 트러블슈팅

---

#### Kubernetes 배포 가이드
**파일**: [kubernetes.md](./kubernetes.md)

Kubernetes 환경에서 Portal Universe를 배포하는 방법을 설명합니다.

**주요 내용**:
- 클러스터 설정
- 배포 방법
- 서비스 관리

---

#### 환경 변수 설정 가이드
**파일**: [environment-variables.md](./environment-variables.md) | **작성일**: 2026-01-19

프로젝트 전체에서 사용하는 환경 변수를 설명합니다.

**주요 내용**:
- 환경별 설정 (Local, Docker, Kubernetes)
- 필수 환경 변수 목록
- 보안 관련 설정
- OAuth2 설정

**관련 문서**:
- [ADR-005: 민감 데이터 관리 전략](../adr/ADR-005-sensitive-data-management.md)

---

### 보안

#### JWT RBAC 설정 가이드
**파일**: [jwt-rbac-setup.md](./jwt-rbac-setup.md) | **작성일**: 2026-01-19

JWT 기반 역할 기반 접근 제어(RBAC) 설정 방법을 상세히 설명합니다.

**주요 내용**:
- Common Library 자동 설정 동작 원리
- JWT 토큰 구조 및 roles 클레임
- Servlet 환경 (Spring MVC) 사용법
- Reactive 환경 (Spring WebFlux) 사용법
- 커스터마이징 방법
- 트러블슈팅

**관련 문서**:
- [ADR-004: JWT RBAC 자동 설정 전략](../adr/ADR-004-jwt-rbac-auto-configuration.md)
- [ADR-003: Admin 권한 검증 전략](../adr/ADR-003-authorization-strategy.md)

---

#### Configuration 설정 가이드
**파일**: [configuration.md](./configuration.md)

Spring Cloud Config를 사용한 설정 관리 방법을 설명합니다.

**주요 내용**:
- Config Service 구성
- 프로파일별 설정 관리

---

### 기능별 가이드

#### Admin 상품 관리 가이드
**파일**: [admin-product-guide.md](./admin-product-guide.md)

Admin 페이지에서 상품을 관리하는 방법을 설명합니다.

**주요 내용**:
- 상품 CRUD 기능
- 카테고리 관리
- 재고 관리
- UI 컴포넌트 사용법

**관련 문서**:
- [ADR-001: Admin 컴포넌트 구조](../adr/ADR-001-admin-component-structure.md)
- [ADR-002: Admin API 엔드포인트 설계](../adr/ADR-002-api-endpoint-design.md)
- [Admin 상품 관리 API 명세서](../api/admin-products-api.md)

---

## 가이드 작성 규칙

### 파일 명명 규칙

```
[kebab-case].md
예) jwt-rbac-setup.md, docker-compose.md
```

### 문서 구조

가이드 문서는 다음 구조를 따릅니다:

```markdown
# [가이드 제목]

## 사전 요구사항
- 필요한 도구, 지식, 환경

## 빠른 시작
- 최소한의 단계로 시작하는 방법

## 상세 설정
- 세부 설정 및 옵션 설명

## 트러블슈팅
- 자주 발생하는 문제 및 해결 방법

## 참고
- 관련 문서 링크
```

### 메타데이터

모든 가이드 문서는 상단에 YAML frontmatter를 포함해야 합니다:

```yaml
---
id: [고유 ID]
title: [문서 제목]
type: guide
status: current | draft | deprecated
created: YYYY-MM-DD
updated: YYYY-MM-DD
author: [작성자]
tags: [태그 배열]
related:
  - [관련문서 ID]
---
```

---

## 프로젝트 컨텍스트

**Repository**: Portal Universe
**관련 디렉토리**:
- `/docs/adr/` - 아키텍처 결정 기록
- `/docs/architecture/` - 시스템 구조
- `/docs/api/` - API 명세서
- `/docs/troubleshooting/` - 문제 해결 기록

---

## 관련 문서

- [Architecture Decision Records](../adr/README.md)
- [API Documentation](../api/README.md)
- [Troubleshooting](../troubleshooting/README.md)
- [프로젝트 로드맵](../ROADMAP.md)

---

**최종 업데이트**: 2026-01-19
**관리자**: Documenter Agent
