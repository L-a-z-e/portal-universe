---
id: component-types
title: Component Types Reference
type: api
status: current
created: 2026-01-19
updated: 2026-01-19
author: documenter
tags: [api, typescript, component-types]
related:
  - theme-types
  - typescript-usage
---

# Component Types Reference

모든 컴포넌트의 Props 타입 정의 레퍼런스입니다.

## Form Components

### ButtonProps

```ts
interface ButtonProps {
  /** 버튼 스타일 변형 */
  variant?: 'primary' | 'secondary' | 'ghost' | 'outline' | 'danger';
  /** 버튼 크기 */
  size?: 'xs' | 'sm' | 'md' | 'lg';
  /** 비활성화 상태 */
  disabled?: boolean;
  /** 로딩 상태 */
  loading?: boolean;
  /** 전체 너비 사용 */
  fullWidth?: boolean;
  /** 버튼 타입 */
  type?: 'button' | 'submit' | 'reset';
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `variant` | `ButtonVariant` | `'primary'` | 버튼 스타일 |
| `size` | `'xs' \| 'sm' \| 'md' \| 'lg'` | `'md'` | 크기 |
| `disabled` | `boolean` | `false` | 비활성화 |
| `loading` | `boolean` | `false` | 로딩 상태 |
| `fullWidth` | `boolean` | `false` | 전체 너비 |
| `type` | `'button' \| 'submit' \| 'reset'` | `'button'` | HTML 타입 |

### InputProps

```ts
interface InputProps {
  type?: 'text' | 'email' | 'password' | 'number' | 'tel' | 'url';
  value?: string | number;
  placeholder?: string;
  disabled?: boolean;
  error?: boolean;
  errorMessage?: string;
  label?: string;
  required?: boolean;
  size?: 'sm' | 'md' | 'lg';
  name?: string;
  id?: string;
}
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `type` | `InputType` | `'text'` | 입력 타입 |
| `value` | `string \| number` | - | 입력 값 |
| `placeholder` | `string` | - | 플레이스홀더 |
| `disabled` | `boolean` | `false` | 비활성화 |
| `error` | `boolean` | `false` | 에러 상태 |
| `errorMessage` | `string` | - | 에러 메시지 |
| `size` | `FormSize` | `'md'` | 크기 |

### TextareaProps

`InputProps`를 확장하며 `type` 속성을 제외합니다.

```ts
interface TextareaProps extends Omit<InputProps, 'type'> {
  rows?: number;
  resize?: 'none' | 'vertical' | 'horizontal' | 'both';
}
```

### CheckboxProps

```ts
interface CheckboxProps {
  checked?: boolean;
  indeterminate?: boolean;
  disabled?: boolean;
  label?: string;
  error?: boolean;
  errorMessage?: string;
  size?: 'sm' | 'md' | 'lg';
  value?: string | number;
  name?: string;
  id?: string;
}
```

### RadioProps

```ts
interface RadioOption {
  label: string;
  value: string | number;
  disabled?: boolean;
}

interface RadioProps {
  value?: string | number;
  options: RadioOption[];
  name: string;
  direction?: 'horizontal' | 'vertical';
  disabled?: boolean;
  error?: boolean;
  errorMessage?: string;
  size?: 'sm' | 'md' | 'lg';
}
```

### SwitchProps

```ts
interface SwitchProps {
  checked?: boolean;
  disabled?: boolean;
  label?: string;
  labelPosition?: 'left' | 'right';
  size?: 'sm' | 'md' | 'lg';
  activeColor?: 'primary' | 'success' | 'warning' | 'error';
  name?: string;
  id?: string;
}
```

### SelectProps

```ts
interface SelectOption {
  label: string;
  value: string | number;
  disabled?: boolean;
}

interface SelectProps {
  value?: string | number | null;
  options: SelectOption[];
  placeholder?: string;
  disabled?: boolean;
  error?: boolean;
  errorMessage?: string;
  label?: string;
  required?: boolean;
  clearable?: boolean;
  searchable?: boolean;
  size?: 'sm' | 'md' | 'lg';
  name?: string;
  id?: string;
}
```

### FormFieldProps

폼 필드 래퍼 컴포넌트용 타입입니다.

```ts
interface FormFieldProps {
  label?: string;
  required?: boolean;
  error?: boolean;
  errorMessage?: string;
  helperText?: string;
  id?: string;
  disabled?: boolean;
  size?: 'sm' | 'md' | 'lg';
}
```

### SearchBarProps

```ts
interface SearchBarProps {
  value: string;
  placeholder?: string;
  loading?: boolean;
  disabled?: boolean;
  autofocus?: boolean;
}
```

## Feedback Components

### AlertProps

```ts
interface AlertProps {
  variant?: 'info' | 'success' | 'warning' | 'error';
  title?: string;
  dismissible?: boolean;
  showIcon?: boolean;
  bordered?: boolean;
}
```

### ToastProps

```ts
interface ToastItem {
  id: string;
  variant?: 'info' | 'success' | 'warning' | 'error';
  title?: string;
  message: string;
  duration?: number;
  dismissible?: boolean;
  action?: {
    label: string;
    onClick: () => void;
  };
}

interface ToastContainerProps {
  position?: ToastPosition;
  maxToasts?: number;
}

type ToastPosition =
  | 'top-right' | 'top-left' | 'top-center'
  | 'bottom-right' | 'bottom-left' | 'bottom-center';
```

### ModalProps

```ts
interface ModalProps {
  open: boolean;
  title?: string;
  size?: 'sm' | 'md' | 'lg';
  showClose?: boolean;
  closeOnBackdrop?: boolean;
  closeOnEscape?: boolean;
}
```

### SpinnerProps

```ts
interface SpinnerProps {
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl';
  color?: 'primary' | 'current' | 'white';
  label?: string;
}
```

### SkeletonProps

```ts
interface SkeletonProps {
  variant?: 'text' | 'circular' | 'rectangular' | 'rounded';
  width?: string;
  height?: string;
  animation?: 'pulse' | 'wave' | 'none';
  lines?: number;
}
```

### ProgressProps

```ts
interface ProgressProps {
  value: number;
  max?: number;
  size?: 'sm' | 'md' | 'lg';
  showLabel?: boolean;
  variant?: 'default' | 'info' | 'success' | 'warning' | 'error';
}
```

## Layout Components

### CardProps

```ts
interface CardProps {
  variant?: 'elevated' | 'outlined' | 'flat' | 'glass' | 'interactive';
  hoverable?: boolean;
  padding?: 'none' | 'sm' | 'md' | 'lg' | 'xl';
}
```

### ContainerProps

```ts
interface ContainerProps {
  maxWidth?: 'sm' | 'md' | 'lg' | 'xl' | '2xl' | 'full';
  centered?: boolean;
  padding?: 'none' | 'sm' | 'md' | 'lg';
  as?: 'div' | 'section' | 'article' | 'main' | 'aside';
}
```

### StackProps

```ts
interface StackProps {
  direction?: 'horizontal' | 'vertical';
  gap?: 'none' | 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl';
  align?: 'start' | 'center' | 'end' | 'stretch' | 'baseline';
  justify?: 'start' | 'center' | 'end' | 'between' | 'around' | 'evenly';
  wrap?: boolean;
  as?: 'div' | 'section' | 'article' | 'ul' | 'ol' | 'nav';
}
```

### DividerProps

```ts
interface DividerProps {
  orientation?: 'horizontal' | 'vertical';
  variant?: 'solid' | 'dashed' | 'dotted';
  color?: 'default' | 'muted' | 'strong';
  label?: string;
  spacing?: 'none' | 'sm' | 'md' | 'lg';
}
```

## Navigation Components

### BreadcrumbProps

```ts
interface BreadcrumbItem {
  label: string;
  href?: string;
  icon?: string;
}

interface BreadcrumbProps {
  items: BreadcrumbItem[];
  separator?: string;
  maxItems?: number;
  size?: 'sm' | 'md' | 'lg';
}
```

### TabsProps

```ts
interface TabItem {
  label: string;
  value: string;
  disabled?: boolean;
  icon?: string;
}

interface TabsProps {
  value: string;
  items: TabItem[];
  variant?: 'default' | 'pills' | 'underline';
  size?: 'sm' | 'md' | 'lg';
  fullWidth?: boolean;
}
```

### LinkProps

```ts
interface LinkProps {
  href?: string;
  target?: '_self' | '_blank' | '_parent' | '_top';
  variant?: 'default' | 'primary' | 'muted' | 'underline';
  external?: boolean;
  disabled?: boolean;
  size?: 'sm' | 'md' | 'lg';
}
```

### DropdownProps

```ts
interface DropdownItem {
  label: string;
  value?: string | number;
  icon?: string;
  disabled?: boolean;
  divider?: boolean;
}

interface DropdownProps {
  items: DropdownItem[];
  trigger?: 'click' | 'hover';
  placement?: DropdownPlacement;
  disabled?: boolean;
  closeOnSelect?: boolean;
  width?: 'auto' | 'trigger' | string;
}

type DropdownPlacement =
  | 'bottom' | 'bottom-start' | 'bottom-end'
  | 'top' | 'top-start' | 'top-end';
```

### PaginationProps

```ts
interface PaginationProps {
  page: number;
  totalPages: number;
  siblingCount?: number;
  showFirstLast?: boolean;
  size?: 'sm' | 'md' | 'lg';
}
```

## Data Display Components

### BadgeProps

```ts
interface BadgeProps {
  variant?: 'default' | 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'outline';
  size?: 'xs' | 'sm' | 'md' | 'lg';
}
```

### TagProps

```ts
interface TagProps {
  variant?: 'default' | 'primary' | 'success' | 'error' | 'warning' | 'info';
  size?: 'sm' | 'md' | 'lg';
  removable?: boolean;
  clickable?: boolean;
}
```

### AvatarProps

```ts
interface AvatarProps {
  src?: string;
  alt?: string;
  name?: string;
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl';
  status?: 'online' | 'offline' | 'busy' | 'away';
  shape?: 'circle' | 'square';
}
```

### TooltipProps

```ts
interface TooltipProps {
  content: string;
  placement?: DropdownPlacement;
  delay?: number;
  disabled?: boolean;
}
```

### TableProps

```ts
interface TableColumn<T = unknown> {
  key: string;
  label: string;
  sortable?: boolean;
  width?: string;
  align?: 'left' | 'center' | 'right';
  render?: (value: unknown, row: T) => unknown;
}

interface TableProps<T = unknown> {
  columns: TableColumn<T>[];
  data: T[];
  loading?: boolean;
  emptyText?: string;
  striped?: boolean;
  hoverable?: boolean;
}
```

## Overlay Components

### PopoverProps

```ts
interface PopoverProps {
  open: boolean;
  placement?: DropdownPlacement;
  trigger?: 'click' | 'hover';
  closeOnClickOutside?: boolean;
}
```
