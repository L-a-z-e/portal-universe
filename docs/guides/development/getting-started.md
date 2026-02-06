---
id: getting-started
title: Getting Started with Design Tokens
type: guide
status: current
created: 2026-01-19
updated: 2026-01-19
author: Laze
tags: [guide, getting-started, design-tokens]
related:
  - token-system
  - css-variables
  - customization
---

# Getting Started with Design Tokens

Design Tokens 패키지를 프로젝트에 설정하고 사용하는 방법을 안내합니다.

## 설치

```bash
npm install @portal/design-tokens
# or
pnpm add @portal/design-tokens
# or
yarn add @portal/design-tokens
```

## 기본 설정

### 1. CSS 임포트

프로젝트의 메인 CSS 파일에 토큰을 임포트합니다.

```css
/* main.css 또는 style.css */
@import '@portal/design-tokens/dist/tokens.css';
```

Vue 프로젝트의 경우:

```ts
// main.ts
import '@portal/design-tokens/dist/tokens.css'
```

React 프로젝트의 경우:

```tsx
// main.tsx 또는 App.tsx
import '@portal/design-tokens/dist/tokens.css'
```

### 2. Tailwind 통합 (선택)

Tailwind CSS를 사용하는 경우 프리셋을 추가합니다.

```js
// tailwind.config.js
module.exports = {
  presets: [require('@portal/design-tokens/tailwind.preset')],
  content: ['./src/**/*.{vue,ts,tsx,js,jsx}'],
};
```

이제 Tailwind 유틸리티 클래스로 토큰을 사용할 수 있습니다:

```html
<div class="bg-brand-primary text-text-heading">
  Hello World
</div>
```

## CSS 변수 사용

### 직접 사용

```css
.my-button {
  background-color: var(--semantic-brand-primary);
  color: var(--semantic-text-inverse);
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--border-radius-md);
  font-weight: var(--typography-fontWeight-medium);
  transition: all var(--effects-animation-duration-normal)
              var(--effects-animation-easing-easeOut);
}

.my-button:hover {
  background-color: var(--semantic-brand-primaryHover);
}
```

### Vue Scoped Styles

```vue
<template>
  <button class="btn">Click me</button>
</template>

<style scoped>
.btn {
  background-color: var(--semantic-brand-primary);
  color: var(--semantic-text-inverse);
  padding: var(--spacing-sm) var(--spacing-md);
}
</style>
```

### React CSS Modules

```tsx
// Button.module.css
.btn {
  background-color: var(--semantic-brand-primary);
  color: var(--semantic-text-inverse);
  padding: var(--spacing-sm) var(--spacing-md);
}

// Button.tsx
import styles from './Button.module.css';

export function Button({ children }) {
  return <button className={styles.btn}>{children}</button>;
}
```

## JavaScript에서 토큰 사용

### ESM Import

```ts
import tokens from '@portal/design-tokens';

// 토큰 값 접근
console.log(tokens.color.indigo['400']); // '#5e6ad2'
console.log(tokens.spacing.md); // '1rem'
```

### TypeScript 지원

TypeScript 프로젝트에서는 자동 완성이 지원됩니다.

```ts
import tokens from '@portal/design-tokens';

// 타입 추론됨
const primaryColor: string = tokens.color.brand.primary;
```

## 테마 설정

### 서비스 테마 적용

HTML에 `data-service` 속성을 추가하여 서비스별 테마를 적용합니다.

```html
<!-- Portal 테마 (기본) -->
<body data-service="portal">

<!-- Blog 테마 -->
<body data-service="blog">

<!-- Shopping 테마 -->
<body data-service="shopping">
```

### 다크/라이트 모드

`data-theme` 속성으로 다크/라이트 모드를 전환합니다.

```html
<!-- 라이트 모드 -->
<body data-theme="light">

<!-- 다크 모드 -->
<body data-theme="dark">
```

### JavaScript로 테마 전환

```ts
// 테마 토글
function toggleTheme() {
  const current = document.body.dataset.theme;
  document.body.dataset.theme = current === 'dark' ? 'light' : 'dark';
}

// 서비스 테마 변경
function setService(service: 'portal' | 'blog' | 'shopping') {
  document.body.dataset.service = service;
}
```

### Vue Composable

```ts
// useTheme.ts
import { ref, watchEffect } from 'vue';

export function useTheme() {
  const theme = ref<'light' | 'dark'>('dark');
  const service = ref<'portal' | 'blog' | 'shopping'>('portal');

  watchEffect(() => {
    document.body.dataset.theme = theme.value;
    document.body.dataset.service = service.value;
  });

  return { theme, service };
}
```

### React Hook

```tsx
// useTheme.ts
import { useState, useEffect } from 'react';

export function useTheme() {
  const [theme, setTheme] = useState<'light' | 'dark'>('dark');
  const [service, setService] = useState<'portal' | 'blog' | 'shopping'>('portal');

  useEffect(() => {
    document.body.dataset.theme = theme;
    document.body.dataset.service = service;
  }, [theme, service]);

  return { theme, setTheme, service, setService };
}
```

## 권장 패턴

### 1. Semantic 토큰 사용

Base 토큰 대신 Semantic 토큰을 사용하세요.

```css
/* Bad */
.card {
  background-color: var(--color-linear-850);
  color: var(--color-linear-300);
}

/* Good */
.card {
  background-color: var(--semantic-bg-card);
  color: var(--semantic-text-body);
}
```

### 2. 일관된 Spacing

레이아웃에 토큰 Spacing을 사용하세요.

```css
/* Bad */
.container {
  padding: 16px 24px;
  gap: 8px;
}

/* Good */
.container {
  padding: var(--spacing-md) var(--spacing-lg);
  gap: var(--spacing-sm);
}
```

### 3. 테마 인식 스타일

테마 전환 시 자동 적용되도록 Semantic 토큰을 사용하세요.

```css
/* 테마 전환 시 자동 적용 */
.card {
  background-color: var(--semantic-bg-card);
  border: 1px solid var(--semantic-border-default);
}
```

## 다음 단계

- [Token System Architecture](../architecture/token-system.md) - 토큰 시스템 이해
- [CSS Variables Reference](../api/css-variables.md) - 전체 변수 목록
- [Theming Guide](./theming-guide.md) - 커스터마이징 방법
