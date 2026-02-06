---
id: onboarding-path
title: Portal Universe - 문서 읽기 순서 가이드
type: guide
status: current
created: 2026-01-19
updated: 2026-02-06
author: Laze
tags: [onboarding, guide, getting-started]
---

# Portal Universe - 문서 읽기 순서 가이드

**난이도**: ⭐ | **예상 시간**: 10분 | **카테고리**: Development

> 프로젝트를 빠르게 파악하고, 기존 서비스 개발이나 신규 서비스 추가 시 어떤 문서를 어떤 순서로 읽어야 하는지 안내합니다.

---

## A: 프로젝트 전체 이해

신규 합류 시 가장 먼저 읽을 순서입니다.

### Step 1. 프로젝트 개요

| 순서 | 문서 | 핵심 내용 |
|------|------|----------|
| 1 | [`/README.md`](../../README.md) | 기술 스택, 서비스 포트, 모니터링 |

### Step 2. 아키텍처 이해

| 순서 | 문서 | 핵심 내용 |
|------|------|----------|
| 4 | [`/docs/adr/README.md`](../../adr/README.md) | 14개 아키텍처 결정 기록 (훑어보기) |
| 6 | [`/docs/old-docs/diagrams/`](../../old-docs/diagrams/) | 시스템 전체 구조, 서비스 간 통신 다이어그램 |

### Step 3. 환경 구성 & 실행

| 순서 | 문서 | 핵심 내용 |
|------|------|----------|
| 7 | [`/docs/guides/deployment/docker-compose.md`](../deployment/docker-compose.md) | Docker Compose 로컬 개발 환경 구성 |
| 8 | [`/docs/guides/operations/environment-variables.md`](../operations/environment-variables.md) | 환경 변수 설정 |

### Step 4. 심화 (선택)

| 문서 | 핵심 내용 |
|------|----------|
| [`/docs/prd/`](../../prd/) | 제품 요구사항 문서 |

---

## B: 백엔드 서비스 개발

개발할 서비스를 선택하고 해당 경로를 따르세요.

### 공통 (모든 백엔드)

| 순서 | 문서 | 핵심 내용 |
|------|------|----------|
| 1 | [`/docs/guides/development/jwt-rbac-setup.md`](jwt-rbac-setup.md) | JWT 인증, RBAC 권한 설정 |

### B-1. Auth Service

| 순서 | 문서 | 핵심 내용 |
|------|------|----------|
| 4 | [Auth Service README](../../api/) | 서비스 개요, 아키텍처 | <!-- TODO: verify auth service docs location -->
| 5 | [Auth API](../../api/) | 11개 컨트롤러, 38+ API 엔드포인트 | <!-- TODO: verify auth API location -->
| 6 | [`/docs/adr/ADR-003-authorization-strategy.md`](../../adr/ADR-003-authorization-strategy.md) | 권한 검증 전략 |
| 7 | [`/docs/adr/ADR-004-jwt-rbac-auto-configuration.md`](../../adr/ADR-004-jwt-rbac-auto-configuration.md) | JWT 자동 설정 |

### B-2. Shopping Service

| 순서 | 문서 | 핵심 내용 |
|------|------|----------|
| 4 | [Shopping Service README](../../api/shopping-frontend/) | 서비스 개요, Saga 패턴 |
| 5 | [Shopping API](../../api/shopping-frontend/) | 상품 CRUD API |
| 6 | [Shopping API Reference](../../api/shopping-service/README.md) | 전체 Shopping API 레퍼런스 |

### B-3. Blog Service

| 순서 | 문서 | 핵심 내용 |
|------|------|----------|
| 4 | [Blog Service README](../../api/blog-frontend/) | 서비스 개요, MongoDB |
| 5 | [Blog API](../../api/blog-frontend/) | Blog API 명세 |

### B-4. Notification Service

| 순서 | 문서 | 핵심 내용 |
|------|------|----------|
| 4 | [Notification Service](../../api/) | Kafka 이벤트, WebSocket | <!-- TODO: verify notification docs location -->
| 5 | [Notification API](../../api/) | 알림 API (6개 엔드포인트) | <!-- TODO: verify notification API location -->

### B-5. API Gateway

| 순서 | 문서 | 핵심 내용 |
|------|------|----------|
| 4 | [API Gateway](../../architecture/) | 라우팅, JWT 검증, Circuit Breaker | <!-- TODO: verify api gateway docs location -->
| 5 | [`/docs/guides/security-implementation-spec.md`](security-implementation-spec.md) | Rate Limiting, 감사 로깅 |

---

## C: 프론트엔드 개발

### 공통 (모든 프론트엔드)

| 순서 | 문서 | 핵심 내용 |
|------|------|----------|
| 1 | [Module Federation Architecture](../../architecture/portal-shell/module-federation.md) | Host-Remote 아키텍처 전체 이해 |

### C-1. Portal Shell (Vue 3 - Host)

| 순서 | 문서 | 핵심 내용 |
|------|------|----------|
| 2 | [Portal Shell](../../architecture/portal-shell/) | Host 개요, 라우팅, 인증 |
| 6 | [Portal Shell API](../../api/) | apiClient, authStore, themeStore 명세 | <!-- TODO: verify portal shell API location -->

### C-2. Blog Frontend (Vue 3 - Remote)

| 순서 | 문서 | 핵심 내용 |
|------|------|----------|
| 2 | [Blog Frontend](../../api/blog-frontend/) | Remote 개요 |
| 6 | [Blog Federation](../../api/blog-frontend/) | Module Federation 설정 | <!-- TODO: verify blog federation docs location -->
| 7 | [Blog Components](../../api/blog-frontend/) | 27개 컴포넌트 레퍼런스 | <!-- TODO: verify blog components docs location -->

### C-3. Shopping Frontend (React 18 - Remote)

| 순서 | 문서 | 핵심 내용 |
|------|------|----------|
| 2 | [Shopping Frontend](../../api/shopping-frontend/) | Remote 개요 |
| 6 | [Shopping Federation](../../api/shopping-frontend/) | Federation 연동 | <!-- TODO: verify shopping federation docs location -->
| 7 | [React Learning](../../learning/) | React 학습 시리즈 (01~07) | <!-- TODO: verify react learning docs location -->

### C-4. Prism Frontend (React 18 - Remote)

| 순서 | 문서 | 핵심 내용 |
|------|------|----------|
| 2 | [Prism Frontend](../../api/) | AI 칸반 대시보드 개요 | <!-- TODO: verify prism frontend docs location -->

### C-5. Design System

| 순서 | 문서 | 핵심 내용 |
|------|------|----------|
| 4 | [Design System Vue](../../api/) | Vue 컴포넌트 라이브러리 | <!-- TODO: verify design system vue docs location -->
| 5 | [Design System React](../../api/) | React 컴포넌트 라이브러리 (30개) | <!-- TODO: verify design system react docs location -->
| 6 | [Vue Components](../../api/) | Vue 컴포넌트 상세 Props/Events | <!-- TODO: verify vue components docs location -->

---

## D: 신규 백엔드 서비스 추가

### Step 1. 설계

| 순서 | 문서 | 왜 읽는가 |
|------|------|----------|
| 1 | [`/docs/adr/README.md`](../../adr/README.md) | 기존 아키텍처 결정 검토 |
| 2 | 기존 서비스 하나 선택하여 `README.md` + 코드 구조 분석 | 디렉토리 레이아웃 참고 |

### Step 2. 구현

```
services/new-service/
├── src/main/java/com/portal/universe/newservice/
│   ├── controller/           # REST 엔드포인트
│   │   └── dto/              # Request/Response DTO
│   ├── service/              # 비즈니스 로직
│   │   └── impl/
│   ├── repository/           # 데이터 접근
│   ├── entity/               # JPA 엔티티
│   ├── config/               # 서비스 설정
│   └── exception/            # 서비스 에러 코드
├── src/main/resources/
│   └── application.yml       # 프로필별 설정
├── docs/
│   ├── README.md
│   ├── api/                  # API 명세
│   └── architecture/         # 아키텍처 문서
├── build.gradle
└── Dockerfile
```

### Step 3. 인프라 연결

| 순서 | 작업 | 참고 문서 |
|------|------|----------|
| 3 | `docker-compose.yml`에 서비스 추가 | [Docker Compose Guide](../deployment/docker-compose.md) |
| 4 | API Gateway에 라우트 추가 | [API Gateway](../../architecture/) | <!-- TODO: verify api gateway docs location -->
| 5 | K8s Deployment 작성 (필요시) | [K8s 배포 가이드](../deployment/k8s-deployment-guide.md) |

### Step 4. 문서 작성

| 순서 | 작업 | 참고 |
|------|------|------|
| 8 | `docs/README.md` 작성 | 기존 서비스 README 형식 참고 |
| 9 | `docs/api/*.md` 작성 | [API 템플릿](../../templates/api-template.md) |
| 10 | ADR 작성 (주요 결정이 있었다면) | [ADR 템플릿](../../templates/adr-template.md) |

---

## E: 신규 프론트엔드 Remote 추가

### Step 1. Module Federation 이해

| 순서 | 문서 | 왜 읽는가 |
|------|------|----------|
| 1 | [Module Federation Architecture](../../architecture/portal-shell/module-federation.md) | Host 설정, exposes, remotes 구조 |

### Step 2. 구현

**React Remote 기준:**
```typescript
// vite.config.ts 필수 설정
federation({
  name: 'new-remote',
  filename: 'remoteEntry.js',
  exposes: {
    './bootstrap': './src/bootstrap.tsx',
  },
  shared: [
    'react',
    'react-dom',
    'react-dom/client',  // 필수! 누락 시 Error #321
  ],
})
```

### Step 3. Host 등록

| 순서 | 작업 | 파일 |
|------|------|------|
| 3 | Portal Shell의 `vite.config.ts` remotes에 추가 | `frontend/portal-shell/vite.config.ts` |
| 4 | `.env` 파일에 Remote URL 환경변수 추가 | `.env.local`, `.env.docker` |
| 5 | Portal Shell 라우트에 경로 추가 | `frontend/portal-shell/src/router/` |
| 6 | Sidebar에 메뉴 항목 추가 | `frontend/portal-shell/src/components/Sidebar.vue` |

### Step 4. 검증

- [ ] `shared`에 `react`, `react-dom`, `react-dom/client` 포함 (React)
- [ ] 빌드 후 `importShared()` 호출 확인
- [ ] Host에서 Remote 로드 테스트
- [ ] Standalone 모드 테스트 (`/standalone` 경로)
- [ ] 다크모드 동기화 확인

---

## 전체 문서 맵 Quick Reference

```
README.md .......................... 프로젝트 개요
.claude/CLAUDE.md .................. 아키텍처 원칙, 포트
.claude/rules/ ..................... 개발 규칙 (Spring, Vue, React, TS, Tailwind)

docs/
├── guides/ ........................ 실행 가이드 (Docker, K8s, 환경변수, 보안)
├── architecture/ .................. 시스템 설계 문서
├── adr/ ........................... 14개 아키텍처 결정 기록
├── scenarios/ ..................... 13개 업무 시나리오
├── learning/ ...................... PART 0~11 학습 자료
├── api/ ........................... API 레퍼런스
├── prd/ ........................... 제품 요구사항
├── old-docs/ ..................... 기존 문서 아카이브 (다이어그램 포함)
├── runbooks/ ...................... 운영 절차서
├── troubleshooting/ ............... 문제 해결 기록
└── testing/ ....................... 테스트 전략

services/*/docs/ ................... 서비스별 문서 (README, API, Architecture)
frontend/*/docs/ ................... 프론트엔드별 문서 (README, Components, Federation)
```
