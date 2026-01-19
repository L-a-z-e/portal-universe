---
id: portal-shell-docs
title: Portal Shell Documentation
type: index
status: current
created: 2026-01-18
updated: 2026-01-18
author: Documenter Agent
tags: [portal-shell, documentation, index, vue3, module-federation]
---

# Portal Shell Documentation

마이크로 프론트엔드 Host 애플리케이션 Portal Shell의 기술 문서입니다.

## 개요

| 항목 | 내용 |
|------|------|
| **Framework** | Vue 3 (Composition API) |
| **Build Tool** | Vite 7.x + Module Federation |
| **State** | Pinia |
| **Router** | Vue Router 4 |
| **Port** | 30000 |

```
┌────────────────────────────────────────┐
│          Portal Shell (Host)           │
│              :30000                    │
│  ┌──────────────────────────────────┐  │
│  │  Header (Auth, Theme, Nav)       │  │
│  ├──────────────────────────────────┤  │
│  │  <router-view>                   │  │
│  │    ┌─────────┐  ┌─────────────┐  │  │
│  │    │ HomePage │  │RemoteWrapper│  │  │
│  │    └─────────┘  └─────────────┘  │  │
│  └──────────────────────────────────┘  │
└────────────────────────────────────────┘
         │                    │
         ▼                    ▼
   ┌──────────┐        ┌──────────────┐
   │   Blog   │        │   Shopping   │
   │  :30001  │        │    :30002    │
   └──────────┘        └──────────────┘
```

---

## 문서 구조

### [Architecture](./architecture/)

시스템 아키텍처 및 설계 문서

| 문서 | 설명 |
|------|------|
| [System Overview](./architecture/system-overview.md) | 시스템 전체 구조 |
| [Module Federation](./architecture/module-federation.md) | Module Federation 상세 |
| [Authentication](./architecture/authentication.md) | OAuth2 PKCE 인증 흐름 |

### [API Reference](./api/)

Module Federation으로 노출되는 API 문서

| 모듈 | 설명 |
|------|------|
| [apiClient](./api/api-client.md) | Axios HTTP 클라이언트 |
| [authStore](./api/auth-store.md) | 인증 상태 관리 (Pinia) |
| [themeStore](./api/theme-store.md) | 테마 상태 관리 (Pinia) |

### [Guides](./guides/)

개발자 가이드

| 문서 | 대상 | 설명 |
|------|------|------|
| [Getting Started](./guides/getting-started.md) | 신규 개발자 | 설치 및 실행 |
| [Adding Remote](./guides/adding-remote.md) | 중급 개발자 | 새 Remote 모듈 추가 |
| [Development](./guides/development.md) | 모든 개발자 | 개발 워크플로우 |

---

## 빠른 시작

### 1. 의존성 설치

```bash
cd frontend
npm install
```

### 2. 환경 변수 설정

```bash
cp portal-shell/.env.dev.example portal-shell/.env.dev
```

### 3. 개발 서버 실행

```bash
# Portal Shell만 실행
npm run dev:portal

# 전체 마이크로 프론트엔드 실행
npm run dev
```

### 4. 브라우저에서 확인

```
http://localhost:30000
```

---

## 기술 스택

| 카테고리 | 기술 |
|----------|------|
| **Framework** | Vue 3.5+ |
| **Build** | Vite 7.x |
| **Federation** | @originjs/vite-plugin-federation |
| **State** | Pinia 2.x |
| **Router** | Vue Router 4.x |
| **HTTP** | Axios |
| **Auth** | oidc-client-ts |
| **Styling** | TailwindCSS + Design System |
| **TypeScript** | 5.9+ |

---

## 디렉토리 구조

```
portal-shell/
├── docs/                    # 📚 기술 문서
│   ├── architecture/        # 아키텍처 문서
│   ├── api/                 # API 레퍼런스
│   ├── guides/              # 개발자 가이드
│   └── backup/              # 이전 문서 백업
├── src/
│   ├── api/                 # API 클라이언트
│   ├── components/          # 공통 컴포넌트
│   ├── config/              # Remote 레지스트리
│   ├── router/              # Vue Router
│   ├── services/            # 비즈니스 로직
│   ├── store/               # Pinia 스토어
│   ├── types/               # TypeScript 타입
│   ├── utils/               # 유틸리티
│   └── views/               # 페이지 컴포넌트
├── vite.config.ts           # Vite + Federation 설정
└── package.json
```

---

## 관련 문서

### 프로젝트 전체 문서
- [프로젝트 CLAUDE.md](/CLAUDE.md) - 프로젝트 전체 가이드

### 연관 모듈 문서
- [Blog Frontend](/frontend/blog-frontend/docs/) - Blog Remote 모듈
- [Shopping Frontend](/frontend/shopping-frontend/docs/) - Shopping Remote 모듈
- [Design System](/frontend/design-system/docs/) - 공유 컴포넌트

### 백엔드 서비스 문서
- [API Gateway](/services/api-gateway/docs/) - 라우팅 및 인증
- [Auth Service](/services/auth-service/docs/) - OAuth2 서버

---

## 문서 업데이트 이력

| 날짜 | 변경 내용 |
|------|----------|
| 2026-01-18 | 문서 구조 개편 및 규칙 적용 |

---

## 기여

문서 개선 제안이나 오류 신고는 GitHub Issue로 등록해 주세요.
