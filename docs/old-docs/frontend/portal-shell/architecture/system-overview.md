---
id: arch-portal-shell-system-overview
title: Portal Shell System Overview
type: architecture
status: current
created: 2026-01-18
updated: 2026-01-18
author: Laze
tags: [architecture, portal-shell, vue3, module-federation, host-application]
related:
  - arch-portal-shell-module-federation
  - arch-portal-shell-authentication
---

# Portal Shell System Overview

## 📋 개요

Portal Shell은 마이크로 프론트엔드 아키텍처의 Host 애플리케이션입니다. Vue 3와 Module Federation을 기반으로 여러 Remote 모듈(blog, shopping)을 런타임에 동적으로 통합하고, 인증, 라우팅, 테마 등 공통 기능을 제공합니다.

---

## 🎯 핵심 특징

- **Module Federation Host**: Remote 모듈 동적 로딩 및 통합
- **OAuth2 PKCE 인증**: Spring Authorization Server와 연동한 표준 인증
- **Shared Resources**: apiClient, authStore, themeStore를 Remote 모듈에 노출
- **Isolated Environment**: Remote 모듈은 독립적으로 개발/배포 가능
- **Service Theming**: data-service 속성으로 서비스별 테마 전환

---

## 🏗️ High-Level Architecture

```mermaid
graph TB
    subgraph "Portal Shell (Host)"
        PS[Portal Shell App<br/>Vue 3 + Vite<br/>Port 30000]

        subgraph "Exposed Modules"
            API[apiClient]
            AUTH[authStore]
            THEME[themeStore]
        end

        subgraph "Core Components"
            RW[RemoteWrapper]
            RR[Router]
            LOGIN[LoginModal]
        end

        PS --> API
        PS --> AUTH
        PS --> THEME
        PS --> RW
        PS --> RR
    end

    subgraph "Remote Modules"
        BLOG[Blog Frontend<br/>Port 30001]
        SHOP[Shopping Frontend<br/>Port 30002]
    end

    subgraph "Backend Services"
        GW[API Gateway<br/>Port 8080]
        AS[Auth Service<br/>Port 8081]
    end

    RW -.->|Dynamic Load| BLOG
    RW -.->|Dynamic Load| SHOP

    BLOG -.->|Use| API
    BLOG -.->|Use| AUTH
    BLOG -.->|Use| THEME

    SHOP -.->|Use| API
    SHOP -.->|Use| AUTH

    API -->|HTTP Proxy| GW
    AUTH -->|OAuth2 PKCE| AS

    classDef host fill:#e1f5ff,stroke:#0288d1
    classDef remote fill:#fff9c4,stroke:#fbc02d
    classDef backend fill:#ffebee,stroke:#c62828

    class PS,API,AUTH,THEME,RW,RR,LOGIN host
    class BLOG,SHOP remote
    class GW,AS backend
```

---

## 📦 컴포넌트 상세

### Portal Shell (Host Application)

| 항목 | 내용 |
|------|------|
| **역할** | MFA Host, 공통 기능 제공 |
| **기술 스택** | Vue 3, Vite 7.x, TypeScript 5.9, Pinia, Vue Router 4 |
| **포트** | 30000 |
| **의존성** | @originjs/vite-plugin-federation, oidc-client-ts |

### RemoteWrapper

| 항목 | 내용 |
|------|------|
| **역할** | Remote 모듈 동적 로딩 및 마운트 |
| **타입** | Vue 3 Component |
| **주요 기능** | remoteEntry.js 로드, bootstrap 함수 호출, 서비스별 테마 적용 |

### Router

| 항목 | 내용 |
|------|------|
| **역할** | 라우팅 관리 (Shell + Remote) |
| **타입** | Vue Router 4 |
| **라우트** | /, /signup, /callback, /blog/*, /shopping/* |

### Auth Store (Pinia)

| 항목 | 내용 |
|------|------|
| **역할** | 사용자 인증 상태 관리 |
| **State** | user (PortalUser), isAuthenticated, displayName |
| **Actions** | setUser, logout, hasRole |

### Theme Store (Pinia)

| 항목 | 내용 |
|------|------|
| **역할** | Light/Dark 모드 관리 |
| **State** | isDark |
| **Actions** | toggle, initialize |

---

## 💾 데이터 저장소

| 저장소 | 용도 | 기술 |
|--------|------|------|
| localStorage | OIDC 토큰, 테마 설정 | Browser API |
| Pinia Store | 런타임 상태 (user, theme) | Vue Reactive State |

---

## 🔗 외부 연동

| 시스템 | 용도 | 프로토콜 | URL |
|--------|------|----------|-----|
| API Gateway | 백엔드 API 호출 | HTTP Proxy | http://localhost:8080 |
| Auth Service | OAuth2 인증 | OIDC PKCE | http://localhost:8081 |
| Blog Remote | Remote 모듈 로딩 | Module Federation | http://localhost:30001 |
| Shopping Remote | Remote 모듈 로딩 | Module Federation | http://localhost:30002 |

---

## 📂 소스 구조

```
src/
├── api/                   # API 클라이언트 (axios)
│   └── apiClient.ts       # Exposed to Remote
├── components/            # 공통 컴포넌트
│   ├── RemoteWrapper.vue  # Remote 모듈 래퍼
│   ├── LoginModal.vue     # 로그인 모달
│   └── ThemeToggle.vue    # 테마 전환
├── config/
│   └── remoteRegistry.ts  # Remote 설정 (dev/docker/k8s)
├── router/
│   └── index.ts           # Vue Router 설정
├── services/
│   ├── authService.ts     # OAuth2 인증 서비스
│   └── remoteLoader.ts    # Remote 동적 로딩
├── store/
│   ├── auth.ts            # Exposed to Remote
│   └── theme.ts           # Exposed to Remote
├── types/
│   └── user.ts            # TypeScript 타입 정의
├── utils/
│   └── jwt.ts             # JWT 파싱
├── views/                 # 페이지 컴포넌트
│   ├── HomePage.vue
│   ├── SignupPage.vue
│   ├── CallbackPage.vue   # OAuth Callback
│   └── NotFound.vue
├── App.vue
└── main.ts
```

---

## 📊 성능 목표

| 지표 | 목표 | 현재 |
|------|------|------|
| 초기 로드 시간 | < 1s | - |
| Remote 로드 시간 | < 500ms | - |
| 인증 처리 시간 | < 300ms | - |
| 라우팅 전환 시간 | < 100ms | - |

---

## 🔐 보안

### 인증 방식
- OAuth2 Authorization Code + PKCE Flow
- JWT Access Token (Bearer Token)
- Silent Renewal (자동 토큰 갱신)

### 토큰 저장
- localStorage (WebStorageStateStore)
- 만료 시 자동 로그아웃

### CORS 정책
- API Gateway에서 CORS 처리
- Vite Proxy: /auth-service, /api

---

## 🌐 환경별 설정

| 환경 | VITE_PROFILE | Remote URL |
|------|--------------|------------|
| Local Dev | dev | http://localhost:3000X |
| Docker | docker | 환경변수 VITE_BLOG_REMOTE_URL |
| Kubernetes | k8s | 환경변수 VITE_BLOG_REMOTE_URL |

---

## 🔄 주요 흐름

### 1. 애플리케이션 초기화

```mermaid
sequenceDiagram
    participant User
    participant PS as Portal Shell
    participant AS as Auth Service
    participant Store as Pinia Store

    User->>PS: 앱 접속
    PS->>PS: main.ts 실행
    PS->>Store: Theme Store 초기화
    PS->>AS: OIDC 메타데이터 로드
    PS->>Store: Auth Store 확인
    alt 토큰 있음
        PS->>AS: 토큰 검증
        AS-->>PS: 유효
        PS->>Store: setUser()
        PS-->>User: 로그인 상태
    else 토큰 없음
        PS-->>User: 로그아웃 상태
    end
```

### 2. Remote 모듈 로딩

```mermaid
sequenceDiagram
    participant User
    participant Router
    participant RW as RemoteWrapper
    participant Remote as Blog Remote

    User->>Router: /blog 이동
    Router->>RW: route.path 전달
    RW->>RW: config 조회 (remoteRegistry)
    RW->>Remote: remoteEntry.js 로드
    Remote-->>RW: bootstrap 함수 반환
    RW->>RW: DOM 컨테이너 생성
    RW->>Remote: bootstrap(container, config)
    Remote->>Remote: Vue 앱 마운트
    Remote-->>User: 블로그 화면 렌더링
```

---

## 🔗 관련 문서

- [Module Federation 상세](./module-federation.md)
- [Authentication 흐름](./authentication.md)
- [API 명세](../api/)
- [가이드](../guides/)

---

**최종 업데이트**: 2026-01-18
