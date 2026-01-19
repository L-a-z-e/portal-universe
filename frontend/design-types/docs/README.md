---
id: design-types-index
title: Design Types
type: guide
status: current
created: 2026-01-19
updated: 2026-01-19
author: documenter
tags: [design-types, typescript, type-definitions]
related:
  - design-tokens-index
  - design-system-vue-index
  - design-system-react-index
---

# @portal/design-types

Portal Universe Design System의 프레임워크 독립적인 TypeScript 타입 정의 패키지입니다. Vue와 React 디자인 시스템에서 공유되는 컴포넌트 Props 타입을 정의합니다.

## 개요

Design Types는 다음을 제공합니다:

- **Framework-agnostic**: Vue와 React 모두에서 사용 가능
- **Type Safety**: 모든 컴포넌트에 대한 타입 정의
- **Consistency**: 프레임워크 간 동일한 API 보장
- **IntelliSense**: IDE 자동완성 지원

## 설치

```bash
npm install @portal/design-types
```

## 빠른 시작

### 타입 임포트

```ts
import type { ButtonProps, InputProps, Size } from '@portal/design-types';
```

### 컴포넌트에서 사용

Vue:
```vue
<script setup lang="ts">
import type { ButtonProps } from '@portal/design-types';

const props = withDefaults(defineProps<ButtonProps>(), {
  variant: 'primary',
  size: 'md',
});
</script>
```

React:
```tsx
import type { ButtonProps } from '@portal/design-types';

export const Button: React.FC<ButtonProps & { children: React.ReactNode }> = ({
  variant = 'primary',
  size = 'md',
  children,
}) => {
  // ...
};
```

## 타입 카테고리

### Common Types
기본 타입 정의 (Size, Variant 등)

```ts
type Size = 'xs' | 'sm' | 'md' | 'lg' | 'xl';
type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'outline' | 'danger';
type StatusVariant = 'info' | 'success' | 'warning' | 'error';
```

### Component Types
컴포넌트별 Props 인터페이스

```ts
interface ButtonProps {
  variant?: ButtonVariant;
  size?: Exclude<Size, 'xl'>;
  disabled?: boolean;
  loading?: boolean;
  fullWidth?: boolean;
}
```

## 문서 구조

### API Reference
- [component-types.md](./api/component-types.md) - 컴포넌트 Props 타입
- [theme-types.md](./api/theme-types.md) - 테마 관련 타입

### Guides
- [typescript-usage.md](./guides/typescript-usage.md) - TypeScript 사용법

## 컴포넌트 타입 목록

### Form Components
- `ButtonProps`
- `InputProps`
- `TextareaProps`
- `CheckboxProps`
- `RadioProps`
- `SwitchProps`
- `SelectProps`
- `FormFieldProps`
- `SearchBarProps`

### Feedback Components
- `AlertProps`
- `ToastProps`
- `ModalProps`
- `SpinnerProps`
- `SkeletonProps`
- `ProgressProps`

### Layout Components
- `CardProps`
- `ContainerProps`
- `StackProps`
- `DividerProps`

### Navigation Components
- `BreadcrumbProps`
- `TabsProps`
- `LinkProps`
- `DropdownProps`
- `PaginationProps`

### Data Display Components
- `BadgeProps`
- `TagProps`
- `AvatarProps`
- `TooltipProps`
- `TableProps`

### Overlay Components
- `PopoverProps`

## 패키지 출력

| 파일 | 형식 | 용도 |
|------|------|------|
| `dist/index.d.ts` | TypeScript | 메인 타입 정의 |
| `dist/common.d.ts` | TypeScript | Common 타입 |
| `dist/components.d.ts` | TypeScript | Component Props 타입 |

## 관련 패키지

- [@portal/design-tokens](../design-tokens) - 디자인 토큰
- [@portal/design-system-vue](../design-system-vue) - Vue 컴포넌트
- [@portal/design-system-react](../design-system-react) - React 컴포넌트
