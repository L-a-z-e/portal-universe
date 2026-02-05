---
id: prism-guide-getting-started
title: Prism Frontend Getting Started
type: guide
status: current
created: 2026-01-30
updated: 2026-01-30
author: Portal Universe Team
tags: [prism-frontend, guide, getting-started]
---

# Prism Frontend - Getting Started

## 사전 요구사항

| 도구 | 버전 |
|------|------|
| Node.js | 18+ |
| npm | 9+ |

## 설치 및 실행

### Standalone 모드

```bash
# 프론트엔드 루트에서 의존성 설치
cd frontend
npm install

# Prism Frontend 개발 모드 실행
npm run dev:prism

# 브라우저에서 확인
# http://localhost:30003
```

### Portal Shell 통합 모드

```bash
# 전체 마이크로 프론트엔드 실행
cd frontend
npm run dev
# http://localhost:30000/prism
```

## 빌드

```bash
# 프로덕션 빌드
npm run build

# Docker 환경 빌드
npm run build:docker

# Kubernetes 환경 빌드
npm run build:k8s
```

## 주요 스크립트

| 스크립트 | 설명 |
|----------|------|
| `dev` | 개발 서버 실행 (watch + preview) |
| `build` | 프로덕션 빌드 |
| `lint` | ESLint 검사 |
| `type-check` | TypeScript 타입 검사 |

## 관련 문서

- [System Overview](../architecture/system-overview.md)
- [Portal Shell - Adding Remote](../../portal-shell/docs/guides/adding-remote.md)
