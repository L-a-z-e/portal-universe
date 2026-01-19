---
id: typescript-usage
title: TypeScript Usage Guide
type: guide
status: current
created: 2026-01-19
updated: 2026-01-19
author: documenter
tags: [guide, typescript, usage]
related:
  - component-types
  - theme-types
---

# TypeScript Usage Guide

Design Types 패키지를 TypeScript 프로젝트에서 효과적으로 사용하는 방법을 안내합니다.

## 설치

```bash
npm install @portal/design-types
```

Design Types는 순수 TypeScript 타입 패키지이므로 런타임 의존성이 없습니다.

## 기본 사용법

### 타입 임포트

```ts
// 개별 타입 임포트
import type { ButtonProps, Size, StatusVariant } from '@portal/design-types';

// Common 타입만 임포트
import type { Size, ButtonVariant } from '@portal/design-types/common';

// Component 타입만 임포트
import type { ButtonProps, InputProps } from '@portal/design-types/components';
```

### 타입 재사용

```ts
import type { Size, StatusVariant } from '@portal/design-types';

// 커스텀 컴포넌트에서 재사용
interface MyComponentProps {
  size: Size;
  status: StatusVariant;
  children: React.ReactNode;
}
```

## Vue에서 사용

### defineProps with Types

```vue
<script setup lang="ts">
import type { ButtonProps } from '@portal/design-types';

// 기본값과 함께 사용
const props = withDefaults(defineProps<ButtonProps>(), {
  variant: 'primary',
  size: 'md',
  disabled: false,
  loading: false,
});
</script>
```

### Props 확장

```vue
<script setup lang="ts">
import type { ButtonProps } from '@portal/design-types';

// 추가 props 정의
interface MyButtonProps extends ButtonProps {
  icon?: string;
  iconPosition?: 'left' | 'right';
}

const props = withDefaults(defineProps<MyButtonProps>(), {
  variant: 'primary',
  iconPosition: 'left',
});
</script>
```

### Emit 타입 정의

```vue
<script setup lang="ts">
import type { SelectOption } from '@portal/design-types';

const emit = defineEmits<{
  'update:modelValue': [value: string | number];
  'change': [option: SelectOption];
}>();
</script>
```

## React에서 사용

### Functional Component

```tsx
import type { ButtonProps } from '@portal/design-types';

// 기본 사용
export const Button: React.FC<ButtonProps & { children: React.ReactNode }> = ({
  variant = 'primary',
  size = 'md',
  disabled = false,
  loading = false,
  children,
  ...rest
}) => {
  return (
    <button disabled={disabled || loading} {...rest}>
      {loading ? <Spinner size="sm" /> : children}
    </button>
  );
};
```

### forwardRef 사용

```tsx
import { forwardRef } from 'react';
import type { InputProps } from '@portal/design-types';

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ type = 'text', size = 'md', error, errorMessage, ...rest }, ref) => {
    return (
      <div>
        <input ref={ref} type={type} {...rest} />
        {error && errorMessage && <span>{errorMessage}</span>}
      </div>
    );
  }
);

Input.displayName = 'Input';
```

### Props 확장

```tsx
import type { CardProps } from '@portal/design-types';

interface ClickableCardProps extends CardProps {
  onClick?: () => void;
  href?: string;
}

export const ClickableCard: React.FC<ClickableCardProps & { children: React.ReactNode }> = ({
  onClick,
  href,
  children,
  ...cardProps
}) => {
  if (href) {
    return <a href={href}><Card {...cardProps}>{children}</Card></a>;
  }
  return <Card {...cardProps} onClick={onClick}>{children}</Card>;
};
```

## Generic 컴포넌트

### Table 컴포넌트

```tsx
import type { TableProps, TableColumn } from '@portal/design-types';

function Table<T>({ columns, data, loading, emptyText }: TableProps<T>) {
  if (loading) return <Skeleton />;
  if (data.length === 0) return <div>{emptyText}</div>;

  return (
    <table>
      <thead>
        <tr>
          {columns.map((col) => (
            <th key={col.key}>{col.label}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {data.map((row, idx) => (
          <tr key={idx}>
            {columns.map((col) => (
              <td key={col.key}>
                {col.render
                  ? col.render((row as Record<string, unknown>)[col.key], row)
                  : String((row as Record<string, unknown>)[col.key])}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}

// 사용 예
interface User {
  id: number;
  name: string;
  email: string;
}

const columns: TableColumn<User>[] = [
  { key: 'id', label: 'ID' },
  { key: 'name', label: 'Name' },
  { key: 'email', label: 'Email' },
];

<Table<User> columns={columns} data={users} />
```

## 타입 가드

### Variant 체크

```ts
import type { StatusVariant } from '@portal/design-types';

function isStatusVariant(value: string): value is StatusVariant {
  return ['info', 'success', 'warning', 'error'].includes(value);
}

// 사용
const variant = 'success';
if (isStatusVariant(variant)) {
  // variant is StatusVariant
}
```

### Option 타입 체크

```ts
import type { SelectOption } from '@portal/design-types';

function isSelectOption(value: unknown): value is SelectOption {
  return (
    typeof value === 'object' &&
    value !== null &&
    'label' in value &&
    'value' in value
  );
}
```

## 유틸리티 타입 활용

### Partial Props

```ts
import type { ButtonProps } from '@portal/design-types';

// 모든 props를 선택적으로
type OptionalButtonProps = Partial<ButtonProps>;

// 특정 props만 필수로
type RequiredButtonProps = Required<Pick<ButtonProps, 'variant' | 'size'>>;
```

### Pick / Omit

```ts
import type { InputProps } from '@portal/design-types';

// 일부 props만 선택
type SimpleInputProps = Pick<InputProps, 'value' | 'placeholder' | 'disabled'>;

// 일부 props 제외
type InputWithoutError = Omit<InputProps, 'error' | 'errorMessage'>;
```

### Extract / Exclude

```ts
import type { Size } from '@portal/design-types';

// 특정 값만 추출
type SmallSizes = Extract<Size, 'xs' | 'sm'>; // 'xs' | 'sm'

// 특정 값 제외
type LargeSizes = Exclude<Size, 'xs' | 'sm'>; // 'md' | 'lg' | 'xl'
```

## 모범 사례

### 1. 타입 재사용

동일한 타입을 여러 곳에서 정의하지 말고 design-types에서 임포트하세요.

```ts
// Bad: 중복 정의
type Size = 'xs' | 'sm' | 'md' | 'lg' | 'xl';

// Good: 재사용
import type { Size } from '@portal/design-types';
```

### 2. 확장 시 명확한 이름

Props를 확장할 때 명확한 이름을 사용하세요.

```ts
// Bad
interface Props extends ButtonProps {
  x: boolean;
}

// Good
interface IconButtonProps extends ButtonProps {
  icon: string;
  iconPosition: 'left' | 'right';
}
```

### 3. 문서화

복잡한 타입에는 JSDoc 주석을 추가하세요.

```ts
/**
 * 사용자 정의 버튼 Props
 * @extends ButtonProps
 * @property {string} icon - 버튼에 표시할 아이콘 이름
 * @property {'left' | 'right'} iconPosition - 아이콘 위치
 */
interface IconButtonProps extends ButtonProps {
  icon: string;
  iconPosition: 'left' | 'right';
}
```

## 트러블슈팅

### 타입 충돌

```ts
// Vue의 Ref와 충돌 방지
import type { Ref as VueRef } from 'vue';
import type { SelectOption } from '@portal/design-types';

const selectedOption: VueRef<SelectOption | null> = ref(null);
```

### 제네릭 추론 문제

```ts
// 명시적 타입 지정
const columns: TableColumn<User>[] = [...];

// 또는 함수 호출 시 타입 지정
<Table<User> columns={columns} data={users} />
```

### strictNullChecks

```ts
// strictNullChecks가 활성화된 경우
const value = props.value ?? ''; // undefined 처리
```
