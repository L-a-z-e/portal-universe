---
id: guide-design-system-index
title: Design System 개발자 가이드
type: guide
status: current
created: 2026-01-18
updated: 2026-01-18
author: documenter
tags: [design-system, guide, vue3, index]
---

# Design System 개발자 가이드

> @portal/design-system 사용을 위한 개발자 가이드 문서입니다.

---

## 📚 문서 목록

| ID | 문서 | 설명 | 상태 |
|----|------|------|------|
| guide-getting-started | [빠른 시작 가이드](./getting-started.md) | 설치 및 기본 설정 | ✅ Current |
| guide-using-components | [컴포넌트 사용 가이드](./using-components.md) | Vue 컴포넌트 사용법 | ✅ Current |
| guide-theming | [테마 적용 가이드](./theming-guide.md) | Light/Dark 모드, 서비스별 테마 | ✅ Current |
| guide-contributing | [기여 가이드](./contributing.md) | 새 컴포넌트 추가 방법 | ✅ Current |

---

## 🎯 대상 독자

- **프론트엔드 개발자**: Vue 3 Composition API 기반 앱 개발자
- **UI 개발자**: Design System을 활용한 인터페이스 구축
- **기여자**: Design System에 새로운 컴포넌트를 추가하고자 하는 개발자

---

## 🏗️ Design System 개요

### 아키텍처

```
@portal/design-system
├── Vue 3 컴포넌트 (Composition API)
├── 3계층 디자인 토큰 (Base → Semantic → Component)
├── Tailwind CSS 통합
├── 서비스별 테마 (Blog, Shopping, Portal)
└── Light/Dark 모드 지원
```

### 핵심 기능

1. **통합 컴포넌트 라이브러리**: Button, Card, Badge, Input, Modal, Tag, Avatar, SearchBar 등
2. **테마 시스템**: useTheme composable을 통한 동적 테마 전환
3. **디자인 토큰**: CSS Variables 기반 일관된 스타일링
4. **Storybook**: 컴포넌트 카탈로그 및 문서화

---

## 📖 가이드 읽는 순서

### 신규 사용자

```
1. 빠른 시작 가이드 (getting-started.md)
   ↓
2. 컴포넌트 사용 가이드 (using-components.md)
   ↓
3. 테마 적용 가이드 (theming-guide.md)
```

### 기여자

```
1. 빠른 시작 가이드 (getting-started.md)
   ↓
2. 기여 가이드 (contributing.md)
```

---

## 🚀 빠른 참조

### 설치

```bash
cd frontend
npm install
```

### Import

```vue
<script setup>
import { Button, Input } from '@portal/design-system'
import '@portal/design-system/style.css'
</script>
```

### 테마 전환

```vue
<script setup>
import { useTheme } from '@portal/design-system'

const { toggleTheme, setService } = useTheme()
</script>

<template>
  <button @click="toggleTheme">Dark/Light</button>
  <button @click="setService('blog')">Blog 테마</button>
</template>
```

---

## 🔗 관련 문서

- [Architecture](../architecture/README.md) - 시스템 아키텍처
- [API Reference](../api/README.md) - 컴포넌트 API 명세
- [Storybook](http://localhost:6006) - 컴포넌트 카탈로그

---

**최종 업데이트**: 2026-01-18
