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

#### Kubernetes 배포 가이드 (간단 버전)
**파일**: [kubernetes.md](./kubernetes.md)

Kubernetes 환경에서 Portal Universe를 배포하는 기본 방법을 설명합니다.

**주요 내용**:
- 클러스터 설정
- 배포 방법
- 서비스 관리

---

#### Kubernetes 배포 가이드 (상세 버전) ⭐
**파일**: [k8s-deployment-guide.md](./k8s-deployment-guide.md) | **작성일**: 2026-01-31

Kind(Kubernetes) 환경에서 Portal Universe 전체를 배포하는 완전한 가이드입니다. 실제 배포 과정에서 발생한 모든 이슈와 해결 방법을 포함합니다.

**주요 내용**:
- 원래 계획 vs 실제 수행 내용 (8가지 추가 이슈 해결)
- 아키텍처 다이어그램 (21개 Pod)
- 단계별 배포 가이드 (사전 준비 → TLS 인증서 → 빌드 → 배포 → 접속)
- 상태 확인 및 에러 대처 (CrashLoopBackOff, ImagePullBackOff, 503 등)
- 서비스 관리 (개별/전체 재시작, 스케일링, 이미지 업데이트)
- 서비스 포트 매핑 테이블 (21개 서비스)
- 파일 변경 목록 (31개 파일)
- FAQ 및 트러블슈팅 체크리스트

**해결한 주요 이슈**:
- nginx DNS resolve 실패 → resolver + set variable 패턴
- nginx URI 미전달 → rewrite 지시문
- Mixed Content 에러 → relative path 변경
- HSTS HTTP 차단 → mkcert TLS 인증서
- prism-service DB 연결 실패 → 환경변수명 수정
- probe 403 Forbidden → 경로 수정

**관련 문서**:
- [계획 파일](.claude/plans/cheerful-giggling-hedgehog.md)
- [Docker Compose 배포 가이드](./docker-compose.md)

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

#### Swagger/Actuator 보안 설정 가이드
**파일**: [swagger-actuator-security.md](./swagger-actuator-security.md) | **작성일**: 2026-01-23

환경별 Swagger UI 및 Actuator 엔드포인트 보안 정책과 접근 방법을 설명합니다.

**주요 내용**:
- 환경별 보안 정책 (Local/Docker/Kubernetes)
- 서비스별 포트 및 접근 경로
- Spring Security 필터 체인 구조
- API Gateway 라우팅 정책
- Prometheus 메트릭 수집 설정

**관련 문서**:
- [JWT RBAC 설정 가이드](./jwt-rbac-setup.md)
- [보안 강화 구현 명세서](./security-implementation-spec.md)

---

#### 보안 강화 구현 명세서
**파일**: [security-implementation-spec.md](./security-implementation-spec.md) | **작성일**: 2026-01-23

Portal Universe 프로젝트의 보안을 강화하기 위한 구현 명세서입니다.

**주요 내용**:
- Rate Limiting 구현 (Gateway + Redis)
- 보안 감사 로깅 구현 (AOP + Database)
- 로그인 보안 강화 (Brute-force 방지)
- 보안 헤더 설정 (CSP, HSTS 등)
- 파일 위치 및 의존성 가이드

**관련 문서**:
- [JWT RBAC 설정 가이드](./jwt-rbac-setup.md)
- [환경 변수 설정 가이드](./environment-variables.md)
- [ADR-004: JWT RBAC 자동 설정 전략](../adr/ADR-004-jwt-rbac-auto-configuration.md)
- [ADR-005: 민감 데이터 관리 전략](../adr/ADR-005-sensitive-data-management.md)

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

#### Shopping Frontend-Backend Gap 구현 완료 보고서
**파일**: [shopping-frontend-gap-implementation.md](./shopping-frontend-gap-implementation.md) | **작성일**: 2026-01-28

shopping-service 백엔드 API와 shopping-frontend 간 10개 Gap 구현 완료 보고서입니다.

**주요 내용**:
- 검색 자동완성, 인기/최근 검색어 (Gap 1-3)
- 재고 실시간 SSE 스트림 (Gap 4)
- 상품 리뷰 Blog 연동 (Gap 5)
- Admin 결제 환불 (Gap 6)
- Admin 대기열/배송/재고이동/주문 관리 (Gap 7-10)
- Frontend 17개 파일 생성, Backend 3개 파일 생성
- EventSource, Debounce, React 18 Hooks 패턴 활용

**관련 문서**:
- [Admin 상품 관리 가이드](./admin-product-guide.md)
- [ADR-002: API 엔드포인트 설계](../adr/ADR-002-api-endpoint-design.md)

---

### 크로스 서비스

#### RBAC 리팩토링 가이드
**파일**: [rbac-refactoring.md](./rbac-refactoring.md) | **작성일**: 2026-01-30

전체 서비스(8개)의 RBAC(Role-Based Access Control) 리팩토링 가이드를 통합한 문서입니다.

**주요 내용**:
- Auth Service: DB 스키마, TokenService, 관리 API, Kafka 이벤트
- Common Library: GatewayAuthenticationFilter, PermissionResolver
- API Gateway: JwtAuthenticationFilter, SecurityConfig
- Blog/Shopping Service: 도메인별 권한 설정
- Frontend (Portal Shell, Blog, Shopping): Store 확장, 네비게이션 가드

**관련 문서**:
- [JWT RBAC 설정 가이드](./jwt-rbac-setup.md)

---

### 온보딩

#### 문서 읽기 순서 가이드
**파일**: [onboarding-path.md](./onboarding-path.md) | **작성일**: 2026-01-30

프로젝트를 빠르게 파악하고, 기존 서비스 개발이나 신규 서비스 추가 시 어떤 문서를 읽어야 하는지 단계별로 안내합니다.

**주요 내용**:
- A: 프로젝트 전체 이해
- B: 백엔드 서비스 개발 (서비스별 경로)
- C: 프론트엔드 개발 (프레임워크별 경로)
- D: 신규 백엔드 서비스 추가
- E: 신규 프론트엔드 Remote 추가

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
author: Laze
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

**최종 업데이트**: 2026-01-31
**관리자**: Documenter Agent
