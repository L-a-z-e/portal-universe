---
id: system-overview-react
title: System Overview
type: architecture
status: current
created: 2026-01-19
updated: 2026-01-19
author: documenter
tags: [architecture, react, system-overview]
related:
  - getting-started-react
  - theming-react
---

# System Overview

Design System React의 아키텍처와 설계 원칙을 설명합니다.

## 아키텍처

```
@portal/design-system-react
├── src/
│   ├── components/          # React 컴포넌트
│   │   ├── Button/
│   │   │   ├── Button.tsx
│   │   │   ├── Button.test.tsx
│   │   │   ├── Button.stories.tsx
│   │   │   └── index.ts
│   │   └── ...
│   ├── hooks/              # React Hooks
│   │   ├── useTheme.ts
│   │   └── useToast.ts
│   ├── utils/              # 유틸리티
│   │   └── cn.ts
│   ├── styles/             # 글로벌 스타일
│   └── index.ts            # 메인 엔트리
└── dist/                   # 빌드 출력
```

## 컴포넌트 구조

각 컴포넌트는 다음 파일로 구성됩니다:

```
Button/
├── Button.tsx          # 메인 컴포넌트
├── Button.test.tsx     # 테스트
├── Button.stories.tsx  # Storybook 스토리
└── index.ts            # Export
```

### 컴포넌트 파일 예시

```tsx
// Button.tsx
import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from 'react';
import type { ButtonProps } from '@portal/design-types';
import { cn } from '../../utils/cn';
import { Spinner } from '../Spinner';

export interface ButtonComponentProps
  extends ButtonProps,
    Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'type'> {
  children?: ReactNode;
}

const variantClasses = {
  primary: 'bg-brand-primary text-text-inverse hover:bg-brand-primaryHover',
  secondary: 'bg-bg-muted text-text-body hover:bg-bg-hover border border-border-default',
  ghost: 'bg-transparent text-text-body hover:bg-bg-hover',
  outline: 'bg-transparent text-text-body border border-border-default hover:bg-bg-hover',
  danger: 'bg-status-error text-white hover:bg-red-600',
};

const sizeClasses = {
  xs: 'h-6 px-2 text-xs gap-1',
  sm: 'h-8 px-3 text-sm gap-1.5',
  md: 'h-9 px-4 text-base gap-2',
  lg: 'h-11 px-6 text-lg gap-2',
};

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
          'inline-flex items-center justify-center font-medium rounded-md',
          'transition-all duration-normal',
          'focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-primary',
          variantClasses[variant],
          sizeClasses[size],
          fullWidth && 'w-full',
          isDisabled && 'opacity-50 cursor-not-allowed',
          className
        )}
        {...props}
      >
        {loading && <Spinner size="sm" color="current" />}
        {children}
      </button>
    );
  }
);

Button.displayName = 'Button';
```

## 스타일링 전략

### cn 유틸리티

`clsx`와 `tailwind-merge`를 결합한 유틸리티입니다:

```ts
// utils/cn.ts
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
```

### Design Tokens 기반

모든 스타일은 Design Tokens CSS 변수를 사용합니다:

```tsx
// Tailwind 클래스에 CSS 변수 활용
<button className="bg-brand-primary text-text-inverse">
  Button
</button>
```

## Props 시스템

### TypeScript 타입

`@portal/design-types`에서 타입을 가져와 확장합니다:

```tsx
import type { ButtonProps } from '@portal/design-types';

// HTML 속성과 결합
export interface ButtonComponentProps
  extends ButtonProps,
    Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'type'> {
  children?: ReactNode;
}
```

### forwardRef 패턴

모든 인터랙티브 컴포넌트는 ref 전달을 지원합니다:

```tsx
export const Input = forwardRef<HTMLInputElement, InputComponentProps>(
  (props, ref) => {
    return <input ref={ref} {...props} />;
  }
);

Input.displayName = 'Input';
```

## Hooks

### useTheme

```ts
// useTheme.ts
import { useState, useEffect, useCallback } from 'react';

export function useTheme(options = {}) {
  const { defaultService = 'portal', defaultMode = 'system' } = options;

  const [service, setService] = useState(defaultService);
  const [mode, setMode] = useState(defaultMode);
  const [systemMode, setSystemMode] = useState('dark');

  // 시스템 테마 감지
  useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    setSystemMode(mediaQuery.matches ? 'dark' : 'light');

    const handler = (e) => setSystemMode(e.matches ? 'dark' : 'light');
    mediaQuery.addEventListener('change', handler);
    return () => mediaQuery.removeEventListener('change', handler);
  }, []);

  const resolvedMode = mode === 'system' ? systemMode : mode;

  // DOM 속성 업데이트
  useEffect(() => {
    document.documentElement.setAttribute('data-service', service);
    document.documentElement.setAttribute('data-theme', resolvedMode);
  }, [service, resolvedMode]);

  const toggleMode = useCallback(() => {
    setMode((current) => {
      if (current === 'light') return 'dark';
      if (current === 'dark') return 'light';
      return systemMode === 'dark' ? 'light' : 'dark';
    });
  }, [systemMode]);

  return { service, mode, resolvedMode, setService, setMode, toggleMode };
}
```

### useToast

```ts
// useToast.ts
import { useContext, useCallback } from 'react';
import { ToastContext } from '../components/Toast';

export function useToast() {
  const context = useContext(ToastContext);

  if (!context) {
    throw new Error('useToast must be used within ToastProvider');
  }

  const { addToast, removeToast } = context;

  const show = useCallback((options) => addToast(options), [addToast]);

  const success = useCallback(
    (message, options) => show({ message, variant: 'success', ...options }),
    [show]
  );

  const error = useCallback(
    (message, options) => show({ message, variant: 'error', ...options }),
    [show]
  );

  return { show, success, error, warning, info, remove: removeToast };
}
```

## 테스팅

### Vitest + Testing Library

```tsx
// Button.test.tsx
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Button } from './Button';

describe('Button', () => {
  it('renders with children', () => {
    render(<Button>Click me</Button>);
    expect(screen.getByRole('button')).toHaveTextContent('Click me');
  });

  it('calls onClick when clicked', () => {
    const handleClick = vi.fn();
    render(<Button onClick={handleClick}>Click</Button>);

    fireEvent.click(screen.getByRole('button'));
    expect(handleClick).toHaveBeenCalled();
  });

  it('does not call onClick when disabled', () => {
    const handleClick = vi.fn();
    render(<Button disabled onClick={handleClick}>Click</Button>);

    fireEvent.click(screen.getByRole('button'));
    expect(handleClick).not.toHaveBeenCalled();
  });

  it('shows spinner when loading', () => {
    render(<Button loading>Loading</Button>);
    expect(screen.getByRole('button')).toBeDisabled();
  });
});
```

## Storybook

### 스토리 구조

```tsx
// Button.stories.tsx
import type { Meta, StoryObj } from '@storybook/react';
import { Button } from './Button';

const meta: Meta<typeof Button> = {
  title: 'Components/Button',
  component: Button,
  tags: ['autodocs'],
  argTypes: {
    variant: {
      control: 'select',
      options: ['primary', 'secondary', 'ghost', 'outline', 'danger'],
    },
    size: {
      control: 'select',
      options: ['xs', 'sm', 'md', 'lg'],
    },
  },
};

export default meta;
type Story = StoryObj<typeof Button>;

export const Primary: Story = {
  args: {
    variant: 'primary',
    children: 'Primary Button',
  },
};

export const AllVariants: Story = {
  render: () => (
    <div className="flex gap-4">
      <Button variant="primary">Primary</Button>
      <Button variant="secondary">Secondary</Button>
      <Button variant="ghost">Ghost</Button>
      <Button variant="outline">Outline</Button>
      <Button variant="danger">Danger</Button>
    </div>
  ),
};
```

## 빌드

### Vite 빌드 설정

```ts
// vite.config.ts
export default defineConfig({
  plugins: [react()],
  build: {
    lib: {
      entry: resolve(__dirname, 'src/index.ts'),
      name: 'DesignSystemReact',
      formats: ['es', 'cjs'],
    },
    rollupOptions: {
      external: ['react', 'react-dom', '@portal/design-tokens', '@portal/design-types'],
      output: {
        globals: {
          react: 'React',
          'react-dom': 'ReactDOM',
        },
      },
    },
  },
});
```

### 출력 파일

```
dist/
├── design-system-react.es.js    # ESM 번들
├── design-system-react.cjs.js   # CommonJS 번들
├── style.css                    # 스타일
└── types/                       # TypeScript 타입
```
