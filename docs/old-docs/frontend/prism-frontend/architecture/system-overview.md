---
id: prism-arch-system-overview
title: Prism Frontend System Overview
type: architecture
status: current
created: 2026-01-30
updated: 2026-01-30
author: Portal Universe Team
tags: [prism-frontend, architecture, module-federation, react]
---

# Prism Frontend - System Overview

## 개요

Prism Frontend는 React 18 + TypeScript 기반의 AI Agent Orchestration Kanban Board입니다. Module Federation Remote로 동작하며, Portal Shell(Host)에 통합됩니다.

## 아키텍처

```
Portal Shell (Host, :30000) - Vue 3
└── Prism Frontend (Remote, :30003) - React 18
    ├── Kanban Board (DnD Kit)
    ├── Zustand Store
    └── React Router
```

## 기술 스택

| 카테고리 | 기술 | 용도 |
|----------|------|------|
| Framework | React 18 | UI 렌더링 |
| Language | TypeScript 5.9 | 타입 안전성 |
| Build | Vite 7.x | 빌드 및 개발 서버 |
| MFA | @originjs/vite-plugin-federation | Module Federation |
| State | Zustand | 클라이언트 상태 관리 |
| Router | React Router 7.x | SPA 라우팅 |
| DnD | @dnd-kit | 드래그 앤 드롭 |
| HTTP | Axios | API 통신 |
| Styling | Tailwind CSS | 유틸리티 CSS |

## Module Federation 설정

```typescript
federation({
  name: 'prism',
  filename: 'remoteEntry.js',
  exposes: {
    './bootstrap': './src/bootstrap.tsx'
  },
  shared: ['react', 'react-dom', 'react-dom/client'],
})
```

- **Embedded Mode**: Portal Shell에서 동적 로드
- **Standalone Mode**: 독립 실행 (개발 전용, `:30003`)

## 디렉토리 구조

```
prism-frontend/src/
├── components/    # 재사용 컴포넌트
├── hooks/         # Custom Hooks
├── pages/         # 페이지 컴포넌트
├── router/        # React Router 설정
├── services/      # API 서비스
├── stores/        # Zustand 스토어
├── types/         # TypeScript 타입
├── App.tsx        # 메인 앱
├── bootstrap.tsx  # MFA 마운트 함수
└── main.tsx       # 엔트리포인트
```

## 관련 문서

- [Portal Shell - Module Federation](../../portal-shell/docs/architecture/module-federation.md)
- [Getting Started](../guides/getting-started.md)
