---
id: api-design-system
title: Design System API Documentation
type: api
status: current
created: 2026-01-18
updated: 2026-01-18
author: documenter
tags: [design-system, api, components, vue3, typescript]
related:
  - arch-design-system-index
  - guide-getting-started
---

# Design System API Documentation

> Portal Universe Design System의 전체 컴포넌트 및 Composables API 명세

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **버전** | 1.0.0 |
| **프레임워크** | Vue 3 (Composition API) |
| **언어** | TypeScript |
| **패키지명** | `@portal/design-system` |

---

## 📚 문서 구성

### 컴포넌트 API

| 문서 | 설명 | 컴포넌트 수 |
|------|------|-------------|
| [입력 컴포넌트](./components-input.md) | Button, Input, Textarea, Select, Checkbox, Radio, Switch, SearchBar | 8개 |
| [피드백 컴포넌트](./components-feedback.md) | Modal, Toast, Badge, Tag, Alert, Spinner, Skeleton | 7개 |
| [레이아웃 컴포넌트](./components-layout.md) | Card, Container, Stack, Divider, FormField, Breadcrumb | 6개 |

### Composables API

| 문서 | 설명 |
|------|------|
| [Composables](./composables.md) | useTheme, useToast |

---

## 🚀 빠른 시작

### 설치

```bash
npm install @portal/design-system
```

### 기본 사용법

```vue
<script setup lang="ts">
import { Button, Input, Modal } from '@portal/design-system';
import type { ButtonProps } from '@portal/design-system';
import { ref } from 'vue';

const email = ref('');
const isModalOpen = ref(false);
</script>

<template>
  <div>
    <Input
      v-model="email"
      type="email"
      placeholder="your@email.com"
      label="이메일"
      required
    />

    <Button
      variant="primary"
      size="md"
      @click="isModalOpen = true"
    >
      제출
    </Button>

    <Modal v-model:open="isModalOpen" title="확인">
      <p>{{ email }}로 전송하시겠습니까?</p>
      <template #footer>
        <Button variant="secondary" @click="isModalOpen = false">취소</Button>
        <Button variant="primary">확인</Button>
      </template>
    </Modal>
  </div>
</template>
```

---

## 🎯 공통 규칙

### v-model 지원

대부분의 입력 컴포넌트는 `v-model`을 통한 양방향 바인딩을 지원합니다.

```vue
<!-- Input, Textarea, Select, SearchBar -->
<Input v-model="value" />

<!-- Checkbox, Switch -->
<Checkbox v-model="isChecked" />

<!-- Modal -->
<Modal v-model:open="isOpen" />
```

### 공통 Props 패턴

| Prop | 타입 | 사용 컴포넌트 | 설명 |
|------|------|---------------|------|
| `variant` | string | Button, Badge, Alert, Tag, Card | 스타일 변형 |
| `size` | string | Button, Input, Badge, Avatar, Spinner | 크기 |
| `disabled` | boolean | 입력 컴포넌트 전체 | 비활성 상태 |
| `error` | boolean | Input, Textarea, Select, Checkbox | 오류 상태 |
| `label` | string | Input, Textarea, Select, Checkbox, Switch | 라벨 텍스트 |
| `required` | boolean | Input, Textarea, Select | 필수 입력 |

### 공통 크기 (size)

```typescript
type Size = 'xs' | 'sm' | 'md' | 'lg' | 'xl';
```

### 공통 변형 (variant)

```typescript
// Button
type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'outline' | 'danger';

// Badge, Alert, Toast, Tag
type StatusVariant = 'default' | 'primary' | 'success' | 'warning' | 'error' | 'info';

// Card
type CardVariant = 'elevated' | 'outlined' | 'flat' | 'glass' | 'interactive';
```

---

## 📐 TypeScript 지원

모든 컴포넌트와 Composables는 완전한 TypeScript 타입 정의를 제공합니다.

```typescript
import type {
  ButtonProps,
  InputProps,
  ModalProps,
  BadgeProps,
  CardProps
} from '@portal/design-system';

// Composables 타입
import type { UseToast } from '@portal/design-system';
```

---

## 🎨 테마 시스템

### 서비스별 테마

```typescript
import { useTheme } from '@portal/design-system';

const { setService } = useTheme();

// 서비스 테마 전환
setService('portal');   // Portal 테마
setService('blog');     // Blog 테마
setService('shopping'); // Shopping 테마
```

### 다크 모드

```typescript
import { useTheme } from '@portal/design-system';

const { setTheme, toggleTheme, currentTheme } = useTheme();

setTheme('dark');      // 다크 모드
setTheme('light');     // 라이트 모드
toggleTheme();         // 토글
```

---

## 🧪 Storybook

모든 컴포넌트는 Storybook에서 대화형으로 테스트할 수 있습니다.

```bash
cd frontend/design-system
npm run storybook
```

Storybook URL: `http://localhost:6006`

---

## 🔗 관련 문서

- [Architecture Overview](../../architecture/design-system/vue-system-overview.md)
- [Usage Guide](../../guides/development/getting-started.md)
- [Theme Customization Guide](../../guides/development/theming-guide.md)

---

**최종 업데이트**: 2026-01-18
