# Blog Frontend 문서

Portal Universe의 Vue 3 기반 블로그 마이크로 프론트엔드 모듈 문서입니다.

---

## 📋 프로젝트 개요

| 항목 | 내용 |
|------|------|
| **프레임워크** | Vue 3 (Composition API) |
| **빌드 도구** | Vite 7.x |
| **언어** | TypeScript 5.9 |
| **포트** | 30001 (개발 서버) |
| **모듈 타입** | Module Federation Remote |

---

## 📚 문서 카테고리

### 📊 현황
| 문서 | 설명 | 상태 |
|------|------|------|
| [STATUS](./STATUS.md) | 구현 상태 대시보드 (페이지/컴포넌트/API 현황) | ✅ |

### 🏗️ Architecture (아키텍처)
시스템 구조와 설계 문서

| 문서 | 설명 | 상태 |
|------|------|------|
| [ARCHITECTURE](./ARCHITECTURE.md) | 계층 구조, 상태 관리, 데이터 플로우, Module Federation | ✅ |
| [System Overview](./architecture/system-overview.md) | 시스템 전체 구조, Dual Mode, Module Federation | ✅ |
| [Data Flow](./architecture/data-flow.md) | 데이터 흐름, API 통신, 상태 관리 | ✅ |

### 📡 API (API 명세)
API 클라이언트 및 통신 관련 문서

| 문서 | 설명 | 상태 |
|------|------|------|
| [API Guide](./API.md) | API 클라이언트 가이드 (8개 모듈, 64개 함수) | ✅ |
| [Client API](./api/client-api.md) | axios 기반 API 클라이언트 상세 명세 | ✅ |

### 🧩 Components & Federation
컴포넌트 및 Module Federation 관련 문서

| 문서 | 설명 | 상태 |
|------|------|------|
| [COMPONENTS](./COMPONENTS.md) | 컴포넌트 가이드 (14개 컴포넌트 + 9개 페이지) | ✅ |
| [FEDERATION](./FEDERATION.md) | Module Federation 설정, 통신, KeepAlive | ✅ |

### 📖 Guides (가이드)
개발자 가이드 및 튜토리얼

| 문서 | 설명 | 상태 |
|------|------|------|
| [Getting Started](./guides/getting-started.md) | 개발 환경 설정 및 실행 방법 | ✅ |

---

## 🚀 Quick Start

### 설치 및 실행 (Standalone 모드)

```bash
cd frontend
npm install
npm run dev:blog    # 포트 30001
```

### Portal Shell 통합 모드

```bash
cd frontend
npm run dev         # 전체 앱 실행 (shell + remotes)
```

### 빌드

```bash
npm run build       # 전체 빌드
```

> 상세한 설정 방법은 [Getting Started](./guides/getting-started.md) 참조

---

## 🔗 핵심 링크

### 내부 링크
- [Architecture](./architecture/README.md)
- [API](./api/README.md)
- [Guides](./guides/README.md)

### 외부 참조
- [Portal Shell 문서](../../portal-shell/docs/README.md)
- [Design System 문서](../../design-system/docs/README.md)
- [Blog Service API](../../../services/blog-service/docs/api/README.md)

---

## 📝 기술 스택

- **프론트엔드**: Vue 3, TypeScript, Vite
- **상태관리**: Pinia
- **라우팅**: Vue Router 4
- **스타일**: TailwindCSS
- **HTTP**: axios (portal-shell 공유)
- **인증**: oidc-client-ts (portal-shell 공유)

---

**최종 업데이트**: 2026-01-18
