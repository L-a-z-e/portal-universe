---
id: design-component-001
title: Button Component - Variants & States
type: learning
created: 2026-01-22
updated: 2026-01-22
author: Laze
tags:
  - design-system
  - button
  - variants
  - react
  - vue
related:
  - design-component-002
  - design-token-001
---

# Button Component - Variants & States

## 학습 목표

- Button Variant 패턴 이해 (Primary, Secondary, Ghost, Danger)
- Size Variant 구현 방법 학습
- Loading/Disabled State 처리 이해
- Linear-inspired Dark-First Button 디자인 습득
- Vue와 React에서 동일한 디자인을 다른 방식으로 구현하는 방법 학습

## 1. Button Variants 개념

### 1.1 Variant란?

Variant는 컴포넌트의 **시각적 스타일 변형**입니다. Button은 용도에 따라 다양한 Variant를 가집니다.

| Variant | 용도 | 예시 |
|---------|------|------|
| **Primary** | 주요 액션 | 제출, 저장, 구매 |
| **Secondary** | 보조 액션 | 취소, 뒤로가기 |
| **Ghost** | 최소 강조 | 드롭다운 토글, 부가 기능 |
| **Outline** | 경계 강조 | 필터, 선택 옵션 |
| **Danger** | 위험한 액션 | 삭제, 제거 |

### 1.2 Size Variants

| Size | Height | Padding | Font Size | 용도 |
|------|--------|---------|-----------|------|
| **xs** | 24px (h-6) | px-2 | text-xs | Compact UI |
| **sm** | 32px (h-8) | px-3 | text-sm | 좁은 공간 |
| **md** | 36px (h-9) | px-4 | text-sm | 기본 크기 |
| **lg** | 44px (h-11) | px-5 | text-base | 중요 액션 |

## 2. Portal Universe Button 구조

### 2.1 TypeScript 인터페이스

```typescript
// @portal/design-types/button.ts
export interface ButtonProps {
  variant?: 'primary' | 'secondary' | 'ghost' | 'outline' | 'danger';
  size?: 'xs' | 'sm' | 'md' | 'lg';
  disabled?: boolean;
  loading?: boolean;
  fullWidth?: boolean;
  type?: 'button' | 'submit' | 'reset';
}
```

### 2.2 Variant Classes 구조

```typescript
// Record<Variant, ClassNames>
const variantClasses: Record<ButtonVariant, string> = {
  primary: '...',
  secondary: '...',
  ghost: '...',
  outline: '...',
  danger: '...',
};

const sizeClasses: Record<ButtonSize, string> = {
  xs: '...',
  sm: '...',
  md: '...',
  lg: '...',
};
```

## 3. React 구현 분석

### 3.1 전체 코드 구조

`frontend/design-system-react/src/components/Button/Button.tsx`:

```tsx
import { forwardRef } from 'react';
import { cn } from '../../utils/cn';
import { Spinner } from '../Spinner';

export interface ButtonComponentProps extends ButtonProps {
  children?: ReactNode;
  className?: string;
  onClick?: () => void;
}

export const Button = forwardRef<HTMLButtonElement, ButtonComponentProps>(
  (
    {
      variant = 'primary',
      size = 'md',
      disabled,
      loading,
      fullWidth,
      type = 'button',
      className,
      children,
      ...props
    },
    ref
  ) => {
    const isDisabled = disabled || loading;

    return (
      <button
        ref={ref}
        type={type}
        disabled={isDisabled}
        className={cn(
          // Base styles
          'inline-flex items-center justify-center font-medium rounded-md',
          'transition-all duration-150 ease-out',
          'focus:outline-none focus-visible:ring-2',
          // Variant
          variantClasses[variant],
          // Size
          sizeClasses[size],
          // Full width
          fullWidth && 'w-full',
          // Disabled state
          isDisabled && 'opacity-50 cursor-not-allowed pointer-events-none',
          className
        )}
        {...props}
      >
        {loading && <Spinner size="sm" />}
        {children}
      </button>
    );
  }
);
```

### 3.2 Primary Variant (Dark-First)

```tsx
const variantClasses = {
  primary: [
    // Dark mode (기본)
    'bg-white/90 text-[#08090a]',          // 밝은 버튼
    'hover:bg-white',
    'active:bg-white/80 active:scale-[0.98]',  // 미세한 스케일 효과
    // Light mode (오버라이드)
    'light:bg-brand-primary light:text-white',
    'light:hover:bg-brand-primaryHover',
    'light:active:bg-brand-primary',
    'border border-transparent',
    'shadow-sm'
  ].join(' '),
};
```

**디자인 철학 (Linear-inspired):**
- 다크모드: 밝은 배경 (`bg-white/90`) + 어두운 텍스트
- 라이트모드: 브랜드 색상 배경 + 흰색 텍스트
- `active:scale-[0.98]`: 클릭 시 미세한 축소 효과

### 3.3 Secondary Variant

```tsx
secondary: [
  'bg-transparent text-text-body',
  'hover:bg-white/5 hover:text-text-heading',
  'active:bg-white/10 active:scale-[0.98]',
  'border border-[#2a2a2a]',
  'light:hover:bg-gray-100',
  'light:border-gray-200'
].join(' '),
```

**특징:**
- 투명 배경 + 테두리
- 호버 시 미세한 배경 색상

### 3.4 Ghost Variant

```tsx
ghost: [
  'bg-transparent text-text-body',
  'hover:bg-white/5 hover:text-text-heading',
  'active:bg-white/10 active:scale-[0.98]',
  'border border-transparent',       // 테두리 없음
  'light:hover:bg-gray-100'
].join(' '),
```

### 3.5 Danger Variant

```tsx
danger: [
  'bg-[#E03131] text-white',           // 고정 색상 (테마 무관)
  'hover:bg-[#C92A2A]',
  'active:bg-[#A51D1D] active:scale-[0.98]',
  'border border-transparent',
  'shadow-sm'
].join(' ')
```

**특징:**
- 다크/라이트 모드 무관하게 동일한 빨간색
- 위험한 액션임을 명확히 표시

### 3.6 Size Classes

```tsx
const sizeClasses: Record<ButtonSize, string> = {
  xs: 'h-6 px-2 text-xs gap-1',
  sm: 'h-8 px-3 text-sm gap-1.5',
  md: 'h-9 px-4 text-sm gap-2',
  lg: 'h-11 px-5 text-base gap-2',
};
```

**`gap-*`**: Icon + Text 사이 간격 자동 조정

## 4. Vue 구현 분석

### 4.1 전체 코드 구조

`frontend/design-system-vue/src/components/Button/Button.vue`:

```vue
<script setup lang="ts">
import type { ButtonProps } from './Button.types';

const props = withDefaults(defineProps<ButtonProps>(), {
  variant: 'primary',
  size: 'md',
  type: 'button',
  disabled: false,
  loading: false,
  fullWidth: false,
});

const emit = defineEmits<{
  (e: 'click', event: MouseEvent): void
}>();

const variantClasses = {
  primary: [
    'bg-white/90 text-[#08090a]',
    'hover:bg-white',
    'active:bg-white/80 active:scale-[0.98]',
    'light:bg-brand-primary light:text-white',
    'light:hover:bg-brand-primaryHover',
  ].join(' '),
  // ... 나머지 variants
};

const sizeClasses = {
  xs: 'h-6 px-2 text-xs gap-1',
  sm: 'h-8 px-3 text-sm gap-1.5',
  md: 'h-9 px-4 text-sm gap-2',
  lg: 'h-11 px-5 text-base gap-2',
};

const isDisabled = computed(() => props.disabled || props.loading);
</script>

<template>
  <button
    :type="type"
    :disabled="isDisabled"
    :class="[
      // Base
      'inline-flex items-center justify-center font-medium rounded-md',
      'transition-all duration-150 ease-out',
      'focus:outline-none focus-visible:ring-2',
      // Variant
      variantClasses[variant],
      // Size
      sizeClasses[size],
      // Full width
      fullWidth && 'w-full',
      // Disabled
      isDisabled && 'opacity-50 cursor-not-allowed pointer-events-none',
    ]"
    @click="emit('click', $event)"
  >
    <Spinner v-if="loading" size="sm" />
    <slot />
  </button>
</template>
```

### 4.2 Vue vs React 차이점

| 항목 | React | Vue |
|------|-------|-----|
| Props | Interface | `defineProps<T>()` |
| Events | `onClick` prop | `@click` + `emit` |
| Children | `children` prop | `<slot />` |
| Class 조합 | `cn()` 유틸리티 | `:class` 배열 |
| Disabled | `disabled` prop | `computed(() => ...)` |

## 5. 실습 예제

### 예제 1: 기본 사용

```tsx
// React
<Button variant="primary" size="md">
  Submit
</Button>

<Button variant="secondary" size="sm">
  Cancel
</Button>
```

```vue
<!-- Vue -->
<Button variant="primary" size="md">
  Submit
</Button>

<Button variant="secondary" size="sm">
  Cancel
</Button>
```

### 예제 2: Loading State

```tsx
// React
const [loading, setLoading] = useState(false);

const handleSubmit = async () => {
  setLoading(true);
  await api.submit();
  setLoading(false);
};

<Button variant="primary" loading={loading} onClick={handleSubmit}>
  {loading ? 'Saving...' : 'Save'}
</Button>
```

```vue
<!-- Vue -->
<script setup lang="ts">
const loading = ref(false);

const handleSubmit = async () => {
  loading.value = true;
  await api.submit();
  loading.value = false;
};
</script>

<template>
  <Button variant="primary" :loading="loading" @click="handleSubmit">
    {{ loading ? 'Saving...' : 'Save' }}
  </Button>
</template>
```

### 예제 3: Icon Button

```tsx
// React
import { PlusIcon } from '@heroicons/react/24/outline';

<Button variant="primary" size="md">
  <PlusIcon className="w-4 h-4" />
  Add Item
</Button>

<Button variant="ghost" size="sm">
  <PlusIcon className="w-4 h-4" />
</Button>
```

```vue
<!-- Vue -->
<template>
  <Button variant="primary" size="md">
    <PlusIcon class="w-4 h-4" />
    Add Item
  </Button>

  <Button variant="ghost" size="sm">
    <PlusIcon class="w-4 h-4" />
  </Button>
</template>
```

### 예제 4: Button Group

```tsx
// React
<div className="inline-flex rounded-md shadow-sm">
  <Button variant="secondary" size="sm" className="rounded-r-none">
    Left
  </Button>
  <Button variant="secondary" size="sm" className="rounded-none border-l-0">
    Middle
  </Button>
  <Button variant="secondary" size="sm" className="rounded-l-none border-l-0">
    Right
  </Button>
</div>
```

### 예제 5: Full Width Button

```tsx
// React
<Button variant="primary" fullWidth>
  Continue
</Button>
```

```vue
<!-- Vue -->
<Button variant="primary" full-width>
  Continue
</Button>
```

## 6. 고급 패턴

### 6.1 Conditional Variant

```tsx
// React
const getVariant = (isPrimary: boolean): ButtonVariant => {
  return isPrimary ? 'primary' : 'secondary';
};

<Button variant={getVariant(isImportant)}>
  Click me
</Button>
```

### 6.2 Custom Loading Spinner

```tsx
// React
<Button variant="primary" loading={loading}>
  {loading && <Spinner size="sm" color="current" />}
  {loading ? 'Processing...' : 'Submit'}
</Button>
```

### 6.3 Disabled Tooltip

```tsx
// React
<Tooltip content="Please fill all fields">
  <Button variant="primary" disabled={!isValid}>
    Submit
  </Button>
</Tooltip>
```

## 7. 핵심 요약

### ✅ Key Takeaways

1. **Variant**: 용도별 시각적 스타일 변형
2. **Size**: xs, sm, md, lg 4단계
3. **Dark-First**: 다크모드 기본, `light:` 변형으로 라이트모드 처리
4. **Loading State**: Spinner 표시 + disabled 처리
5. **`active:scale-[0.98]`**: Linear-inspired 미세 축소 효과

### 🎯 Best Practices

```tsx
// ✅ DO
<Button variant="primary" size="md" loading={loading}>
  Submit
</Button>

<Button variant="danger" onClick={handleDelete}>
  Delete
</Button>

// ❌ DON'T
<button className="bg-blue-600 px-4 py-2">  // 커스텀 스타일 사용
  Submit
</button>

<Button variant="primary" disabled={loading}>  // loading prop 사용
  Submit
</Button>
```

### 📋 Variant 선택 가이드

```typescript
// Primary: 주요 액션 (페이지당 1개)
<Button variant="primary">Save</Button>

// Secondary: 보조 액션
<Button variant="secondary">Cancel</Button>

// Ghost: 최소 강조
<Button variant="ghost">Learn More</Button>

// Outline: 선택 옵션
<Button variant="outline">Filter</Button>

// Danger: 위험한 액션
<Button variant="danger">Delete Account</Button>
```

## 8. 관련 문서

- [Design Tokens](../tokens/design-tokens.md) - Button에 사용된 Token
- [Input Component](./input-component.md) - Form 컴포넌트 패턴
- [Theming](../patterns/theming.md) - Dark/Light Mode 전환
