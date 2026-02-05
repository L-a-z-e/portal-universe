---
id: design-system-docs
title: Design System Documentation
type: index
status: current
created: 2026-01-18
updated: 2026-01-18
author: documenter
tags: [design-system, documentation, index]
---

# Design System - Portal Universe

## 개요

Portal Universe Design System은 마이크로 프론트엔드 아키텍처 환경에서 일관된 UI/UX를 제공하기 위한 Vue 3 컴포넌트 라이브러리입니다. Blog, Shopping 등 다양한 서비스 모듈이 공유할 수 있는 통일된 디자인 언어를 정의하고 구현합니다.

### 핵심 특징

- **Vue 3 Composition API**: `<script setup>` 문법으로 간결한 컴포넌트 구현
- **Tailwind CSS 기반**: 유틸리티 우선 CSS 프레임워크로 효율적인 스타일링
- **3-계층 토큰 시스템**: Base → Semantic → Component 계층으로 체계화된 디자인 토큰
- **서비스별 테마**: `data-service` 속성으로 Blog/Shopping 테마 동적 전환
- **명암 모드 지원**: Light/Dark 테마 자동 감지 및 수동 전환
- **Storybook 통합**: 모든 컴포넌트의 상호작용형 문서 및 테스트
- **타입 안전성**: TypeScript 완전 지원으로 개발자 경험 향상

---

## 📚 문서 구조

### Architecture (아키텍처)

| 문서 | 설명 |
|------|------|
| [Architecture Index](./architecture/README.md) | 아키텍처 문서 인덱스 |
| [System Overview](./architecture/system-overview.md) | 전체 시스템 구조 개요 |
| [Token System](./architecture/token-system.md) | 3계층 토큰 시스템 상세 |
| [Theming](./architecture/theming.md) | 테마 시스템 아키텍처 |

### API (API 명세)

| 문서 | 설명 |
|------|------|
| [API Index](./api/README.md) | API 문서 인덱스 |
| [Input Components](./api/components-input.md) | 입력 컴포넌트 API |
| [Feedback Components](./api/components-feedback.md) | 피드백 컴포넌트 API |
| [Layout Components](./api/components-layout.md) | 레이아웃 컴포넌트 API |
| [Composables](./api/composables.md) | Vue Composables API |

### Guides (가이드)

| 문서 | 설명 |
|------|------|
| [Guides Index](./guides/README.md) | 가이드 문서 인덱스 |
| [Getting Started](./guides/getting-started.md) | 빠른 시작 가이드 |
| [Using Components](./guides/using-components.md) | 컴포넌트 사용 가이드 |
| [Components Catalog](./guides/components.md) | 전체 Vue 컴포넌트 카탈로그 |
| [Theming Guide](./guides/theming-guide.md) | 테마 적용 가이드 |
| [Theming 상세](./guides/theming.md) | 서비스별 커스터마이징 및 다크 모드 관리 |
| [Design Tokens](./guides/tokens.md) | 3계층 디자인 토큰 체계 상세 |
| [Usage Guide](./guides/usage.md) | 프로젝트 통합 및 효과적 사용법 |
| [Contributing](./guides/contributing.md) | 기여 가이드 |

---

## 빠른 시작

### 로컬 개발

```bash
cd frontend/design-system
npm install
npm run dev                 # http://localhost:30003
npm run storybook          # http://localhost:6006
npm run build              # 빌드
npm test                   # 테스트
```

### 다른 모듈에서 사용

```typescript
import { Button, Input, Modal } from '@portal/design-system'
import '@portal/design-system/style.css'
```

---

## 컴포넌트 분류

**입력 (8)**: Button, Input, Textarea, Select, Checkbox, Radio, Switch, SearchBar
**피드백 (7)**: Modal, Toast, Badge, Tag, Alert, Spinner, Skeleton
**레이아웃 (6)**: Card, Container, Stack, Divider, FormField, Breadcrumb
**기타**: Avatar, Link, Tabs, Dropdown

---

## 기술 스택

- Vue 3.5 / TypeScript 5.9
- Tailwind CSS 3.4 / Vite 7.x
- Storybook 9.x / Vitest 4.x

---

## 백업 문서

기존 문서는 [backup/](./backup/) 폴더에 보관되어 있습니다.

---

**최종 업데이트**: 2026-01-18
