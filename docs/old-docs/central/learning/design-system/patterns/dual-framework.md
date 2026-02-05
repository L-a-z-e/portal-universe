---
id: design-pattern-003
title: Dual Framework - Vue & React 공존
type: learning
created: 2026-01-22
updated: 2026-01-22
author: Laze
tags:
  - design-system
  - vue
  - react
  - module-federation
  - architecture
related:
  - design-token-001
  - design-pattern-001
---

# Dual Framework - Vue & React 공존

## 학습 목표

- Dual Framework Design System의 필요성과 장점 이해
- Vue와 React 컴포넌트의 구조적 차이 학습
- 동일한 디자인을 다른 프레임워크로 구현하는 방법 습득
- Design Token을 통한 일관성 유지 방법 이해
- Module Federation 환경에서의 Design System 활용 분석

## 1. Dual Framework Design System

### 1.1 개념

Portal Universe는 **Vue와 React 두 프레임워크를 동시에 지원**하는 Design System을 운영합니다.

```
Design Tokens (공통)
    ↓
┌───────────────┬───────────────┐
│ design-system-vue │ design-system-react │
│ (Vue 3)        │ (React 18)    │
└───────────────┴───────────────┘
    ↓               ↓
portal-shell     shopping-frontend
blog-frontend
```

### 1.2 왜 Dual Framework인가?

| 이유 | 설명 |
|------|------|
| **점진적 마이그레이션** | Vue에서 React로 (또는 반대로) 점진적 전환 |
| **팀 자율성** | 각 팀이 익숙한 프레임워크 선택 |
| **Module Federation** | Host(Vue)에 Remote(React) 통합 가능 |
| **인재 풀 확장** | Vue 개발자와 React 개발자 모두 채용 가능 |
| **최적 도구 선택** | 각 서비스 특성에 맞는 프레임워크 선택 |

### 1.3 Portal Universe 구성

| 서비스 | 프레임워크 | 역할 |
|--------|-----------|------|
| **portal-shell** | Vue 3 | Host (Module Federation) |
| **blog-frontend** | Vue 3 | Remote |
| **shopping-frontend** | React 18 | Remote |

## 2. 공통 계층: Design Tokens

### 2.1 Token as Single Source of Truth

Design Token은 **프레임워크 독립적**입니다.

```
CSS Variables (Design Tokens)
    ↓
┌─────────────────────────────────┐
│ Tailwind Preset                 │
│ (Framework Agnostic)            │
└─────────────────────────────────┘
    ↓                    ↓
Vue Component      React Component
```

### 2.2 Token 정의 (공통)

```css
/* Design Tokens (프레임워크 무관) */
:root {
  --semantic-brand-primary: #5e6ad2;
  --semantic-text-body: #ebeced;
  --semantic-bg-card: #0e0f10;
}
```

### 2.3 Tailwind Preset (공통)

```javascript
// @portal/design-tokens/tailwind.preset.js
export default {
  theme: {
    extend: {
      colors: {
        'brand': {
          'primary': 'var(--semantic-brand-primary)',
        },
        'text': {
          'body': 'var(--semantic-text-body)',
        },
        'bg': {
          'card': 'var(--semantic-bg-card)',
        },
      }
    }
  }
}
```

## 3. Vue vs React: 구현 비교

### 3.1 Button 컴포넌트

#### Vue 구현
```vue
<!-- Button.vue -->
<script setup lang="ts">
import type { ButtonProps } from './Button.types';

const props = withDefaults(defineProps<ButtonProps>(), {
  variant: 'primary',
  size: 'md',
  disabled: false,
});

const emit = defineEmits<{
  (e: 'click', event: MouseEvent): void
}>();
</script>

<template>
  <button
    :type="type"
    :disabled="disabled"
    :class="[
      'px-4 py-2 rounded-md',
      'bg-brand-primary text-white',
      'hover:bg-brand-primaryHover',
    ]"
    @click="emit('click', $event)"
  >
    <slot />
  </button>
</template>
```

#### React 구현
```tsx
// Button.tsx
import { forwardRef } from 'react';
import type { ButtonProps } from './Button.types';

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ variant = 'primary', size = 'md', disabled, children, onClick }, ref) => {
    return (
      <button
        ref={ref}
        type="button"
        disabled={disabled}
        className="
          px-4 py-2 rounded-md
          bg-brand-primary text-white
          hover:bg-brand-primaryHover
        "
        onClick={onClick}
      >
        {children}
      </button>
    );
  }
);
```

### 3.2 주요 차이점

| 항목 | Vue | React |
|------|-----|-------|
| **Props** | `defineProps<T>()` | Function params |
| **Events** | `emit('click', $event)` | `onClick` prop |
| **Children** | `<slot />` | `{children}` prop |
| **v-model** | `v-model="value"` | `value` + `onChange` |
| **Ref** | `ref<HTMLElement>()` | `useRef<HTMLElement>()` |
| **Class** | `:class="[...]"` | `className={cn(...)}` |
| **Conditional** | `v-if`, `v-show` | `{condition && <Component />}` |

### 3.3 Input 컴포넌트

#### Vue v-model
```vue
<!-- Input.vue -->
<script setup lang="ts">
const props = defineProps<{
  modelValue: string;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>();

function handleInput(event: Event) {
  emit('update:modelValue', (event.target as HTMLInputElement).value);
}
</script>

<template>
  <input
    :value="modelValue"
    @input="handleInput"
    class="bg-bg-card text-text-body border border-border-default"
  />
</template>

<!-- 사용 -->
<Input v-model="email" />
```

#### React Controlled Component
```tsx
// Input.tsx
interface InputProps {
  value: string;
  onValueChange: (value: string) => void;
}

export const Input: React.FC<InputProps> = ({ value, onValueChange }) => {
  return (
    <input
      value={value}
      onChange={(e) => onValueChange(e.target.value)}
      className="bg-bg-card text-text-body border border-border-default"
    />
  );
};

// 사용
const [email, setEmail] = useState('');
<Input value={email} onValueChange={setEmail} />
```

### 3.4 Modal 컴포넌트

#### Vue Teleport
```vue
<!-- Modal.vue -->
<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition-opacity duration-150"
      enter-from-class="opacity-0"
    >
      <div v-if="isOpen" class="fixed inset-0 z-50">
        <!-- Modal content -->
      </div>
    </Transition>
  </Teleport>
</template>
```

#### React Portal
```tsx
// Modal.tsx
import { createPortal } from 'react-dom';

export const Modal: React.FC = ({ open, children }) => {
  if (!open) return null;

  const modalContent = (
    <div className="fixed inset-0 z-50 animate-fade-in">
      {children}
    </div>
  );

  return createPortal(modalContent, document.body);
};
```

## 4. Design Token 기반 일관성

### 4.1 동일한 Token, 다른 구현

#### Vue
```vue
<template>
  <div class="bg-bg-card text-text-body border border-border-default">
    <!-- Semantic Token 사용 -->
  </div>
</template>
```

#### React
```tsx
<div className="bg-bg-card text-text-body border border-border-default">
  {/* 동일한 Semantic Token */}
</div>
```

**결과:** 프레임워크는 다르지만 **시각적으로 동일**

### 4.2 CSS Variables의 힘

```html
<!-- Vue Component -->
<div class="bg-brand-primary">Vue Button</div>

<!-- React Component -->
<div className="bg-brand-primary">React Button</div>
```

**컴파일 결과 (동일):**
```css
.bg-brand-primary {
  background-color: var(--semantic-brand-primary);
}
```

**런타임:**
```css
:root {
  --semantic-brand-primary: #5e6ad2;  /* Portal */
}

[data-service="blog"] {
  --semantic-brand-primary: #10b981;  /* Blog: Green */
}
```

## 5. Module Federation 환경

### 5.1 아키텍처

```
portal-shell (Vue 3, Host, :30000)
    ↓ exposes: apiClient, authStore
┌───────────────┬───────────────┐
│ blog-frontend │ shopping-frontend │
│ (Vue 3)       │ (React 18)    │
│ :30001        │ :30002        │
└───────────────┴───────────────┘
```

### 5.2 Design System 통합

#### portal-shell (Host)
```typescript
// portal-shell/module-federation.config.ts
export default {
  name: 'portalShell',
  remotes: {
    'blog-frontend': 'http://localhost:30001/remoteEntry.js',
    'shopping-frontend': 'http://localhost:30002/remoteEntry.js',
  },
  exposes: {
    './apiClient': './src/api/client.ts',
    './authStore': './src/stores/authStore.ts',
  },
};
```

#### shopping-frontend (React Remote)
```typescript
// shopping-frontend/module-federation.config.ts
export default {
  name: 'shoppingFrontend',
  filename: 'remoteEntry.js',
  exposes: {
    './bootstrap': './src/bootstrap.tsx',
  },
  shared: {
    react: { singleton: true },
    'react-dom': { singleton: true },
    '@portal/design-system-react': { singleton: true },  // Design System
  },
};
```

### 5.3 Design System 공유

```typescript
// Module Federation Shared Config
shared: {
  // Framework
  vue: { singleton: true },
  react: { singleton: true },

  // Design System
  '@portal/design-system-vue': { singleton: true },
  '@portal/design-system-react': { singleton: true },

  // Design Tokens (공통)
  '@portal/design-tokens': { singleton: true },
}
```

## 6. 실습 예제

### 예제 1: 동일한 Card 컴포넌트

#### Vue
```vue
<!-- Card.vue -->
<template>
  <div class="
    bg-bg-card
    border border-border-default
    rounded-lg p-4
    shadow-md
  ">
    <h3 class="text-text-heading font-semibold mb-2">
      <slot name="title" />
    </h3>
    <p class="text-text-body">
      <slot />
    </p>
  </div>
</template>

<!-- 사용 -->
<Card>
  <template #title>Card Title</template>
  Card content
</Card>
```

#### React
```tsx
// Card.tsx
interface CardProps {
  title?: ReactNode;
  children?: ReactNode;
}

export const Card: React.FC<CardProps> = ({ title, children }) => {
  return (
    <div className="
      bg-bg-card
      border border-border-default
      rounded-lg p-4
      shadow-md
    ">
      {title && (
        <h3 className="text-text-heading font-semibold mb-2">
          {title}
        </h3>
      )}
      <p className="text-text-body">
        {children}
      </p>
    </div>
  );
};

// 사용
<Card title="Card Title">
  Card content
</Card>
```

### 예제 2: Form 컴포넌트

#### Vue (Composition API)
```vue
<script setup lang="ts">
const form = reactive({
  email: '',
  password: '',
});

const handleSubmit = () => {
  console.log('Submit:', form);
};
</script>

<template>
  <form @submit.prevent="handleSubmit">
    <Input v-model="form.email" label="Email" type="email" />
    <Input v-model="form.password" label="Password" type="password" />
    <Button type="submit" variant="primary">Login</Button>
  </form>
</template>
```

#### React (Hooks)
```tsx
const LoginForm: React.FC = () => {
  const [form, setForm] = useState({
    email: '',
    password: '',
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    console.log('Submit:', form);
  };

  return (
    <form onSubmit={handleSubmit}>
      <Input
        label="Email"
        type="email"
        value={form.email}
        onValueChange={(v) => setForm({ ...form, email: v })}
      />
      <Input
        label="Password"
        type="password"
        value={form.password}
        onValueChange={(v) => setForm({ ...form, password: v })}
      />
      <Button type="submit" variant="primary">Login</Button>
    </form>
  );
};
```

## 7. 핵심 요약

### ✅ Key Takeaways

1. **Design Tokens = Single Source of Truth**
2. **CSS Variables = Framework Agnostic**
3. **Vue vs React = 구현 다름, 디자인 동일**
4. **Module Federation = Vue + React 공존 가능**
5. **Tailwind Preset = 공통 스타일 시스템**

### 🎯 Architecture Flow

```
Design Tokens (CSS Variables)
    ↓
Tailwind Preset
    ↓
┌───────────────┬───────────────┐
│ Vue Component │ React Component │
│ (Template)    │ (JSX)         │
└───────────────┴───────────────┘
    ↓               ↓
동일한 시각적 결과
```

### 📋 Component Parity Checklist

Vue와 React 컴포넌트가 동일한지 확인:

- [ ] Props 인터페이스 동일
- [ ] 동일한 Semantic Token 사용
- [ ] 동일한 Tailwind Class
- [ ] 동일한 Size Variants
- [ ] 동일한 State Handling
- [ ] 동일한 Event Handling
- [ ] 동일한 Accessibility 속성

### 🔄 Migration Path

#### Vue → React
```vue
<!-- Vue -->
<Button v-model="isOpen" @click="handleClick" :disabled="loading">
  Submit
</Button>
```

```tsx
// React
<Button
  value={isOpen}
  onValueChange={setIsOpen}
  onClick={handleClick}
  disabled={loading}
>
  Submit
</Button>
```

#### React → Vue
```tsx
// React
const [value, setValue] = useState('');
<Input value={value} onChange={(e) => setValue(e.target.value)} />
```

```vue
<!-- Vue -->
<script setup lang="ts">
const value = ref('');
</script>
<template>
  <Input v-model="value" />
</template>
```

## 8. Best Practices

### ✅ DO

```typescript
// 1. 공통 TypeScript 인터페이스 사용
// @portal/design-types/button.ts
export interface ButtonProps {
  variant?: 'primary' | 'secondary';
  size?: 'sm' | 'md' | 'lg';
  disabled?: boolean;
}

// 2. Semantic Token 사용
<div className="bg-bg-card text-text-body">

// 3. 동일한 Naming Convention
// Vue: Button.vue, Input.vue
// React: Button.tsx, Input.tsx
```

### ❌ DON'T

```typescript
// 1. 프레임워크별로 다른 Token 사용
// Vue: bg-gray-900
// React: bg-slate-900  // ❌

// 2. 하드코딩된 색상
<div style={{ backgroundColor: '#08090a' }}>  // ❌

// 3. 프레임워크별로 다른 Props 이름
// Vue: modelValue
// React: inputValue  // ❌ (동일한 이름 사용)
```

## 9. 관련 문서

- [Design Tokens](../tokens/design-tokens.md) - Framework Agnostic Tokens
- [Tailwind Integration](../tokens/tailwind-integration.md) - 공통 Preset
- [Button Component](../components/button-component.md) - Vue vs React 비교
- [Theming](./theming.md) - 공통 Theme System
