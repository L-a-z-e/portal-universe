---
id: storybook-react
title: Storybook Guide
type: guide
status: current
created: 2026-01-19
updated: 2026-01-19
author: documenter
tags: [guide, react, storybook]
related:
  - getting-started-react
  - system-overview-react
---

# Storybook Guide

Design System React의 Storybook 사용 방법을 안내합니다.

## Storybook 실행

```bash
# 개발 서버 실행
npm run storybook

# 빌드
npm run build-storybook
```

기본적으로 http://localhost:6006 에서 실행됩니다.

## 스토리 구조

### 기본 스토리

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
    disabled: { control: 'boolean' },
    loading: { control: 'boolean' },
    fullWidth: { control: 'boolean' },
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
```

### 여러 변형 보여주기

```tsx
export const AllVariants: Story = {
  render: () => (
    <div className="flex flex-wrap gap-4">
      <Button variant="primary">Primary</Button>
      <Button variant="secondary">Secondary</Button>
      <Button variant="ghost">Ghost</Button>
      <Button variant="outline">Outline</Button>
      <Button variant="danger">Danger</Button>
    </div>
  ),
};

export const AllSizes: Story = {
  render: () => (
    <div className="flex items-center gap-4">
      <Button size="xs">Extra Small</Button>
      <Button size="sm">Small</Button>
      <Button size="md">Medium</Button>
      <Button size="lg">Large</Button>
    </div>
  ),
};
```

## 테마 지원

### 테마 데코레이터

```tsx
// .storybook/preview.tsx
import type { Preview } from '@storybook/react';
import '@portal/design-tokens/dist/tokens.css';
import '../src/styles/index.css';

const preview: Preview = {
  parameters: {
    backgrounds: { disable: true },
  },
  globalTypes: {
    theme: {
      name: 'Theme',
      description: 'Global theme',
      defaultValue: 'dark',
      toolbar: {
        icon: 'circlehollow',
        items: ['light', 'dark'],
        showName: true,
      },
    },
    service: {
      name: 'Service',
      description: 'Service theme',
      defaultValue: 'portal',
      toolbar: {
        icon: 'paintbrush',
        items: ['portal', 'blog', 'shopping'],
        showName: true,
      },
    },
  },
  decorators: [
    (Story, context) => {
      const theme = context.globals.theme;
      const service = context.globals.service;

      return (
        <div data-theme={theme} data-service={service} className="p-4">
          <Story />
        </div>
      );
    },
  ],
};

export default preview;
```

## 폼 컴포넌트 스토리

### 폼 그룹

```tsx
// FormExample.stories.tsx
import { useState } from 'react';

export const LoginForm: Story = {
  render: () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [remember, setRemember] = useState(false);

    return (
      <Card padding="lg" className="w-96">
        <h2 className="text-xl font-semibold mb-4">Login</h2>
        <form className="space-y-4">
          <Input
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            type="email"
            label="Email"
            placeholder="email@example.com"
          />
          <Input
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            type="password"
            label="Password"
            placeholder="••••••••"
          />
          <Checkbox
            checked={remember}
            onChange={(e) => setRemember(e.target.checked)}
            label="Remember me"
          />
          <Button type="submit" fullWidth>
            Sign In
          </Button>
        </form>
      </Card>
    );
  },
};
```

## 인터랙션 테스트

```tsx
import { within, userEvent, expect } from '@storybook/test';

export const ClickTest: Story = {
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    const button = canvas.getByRole('button');

    await userEvent.click(button);

    await expect(button).toHaveFocus();
  },
};

export const FormInteraction: Story = {
  render: () => {
    const [value, setValue] = useState('');
    return (
      <Input
        value={value}
        onChange={(e) => setValue(e.target.value)}
        data-testid="test-input"
      />
    );
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    const input = canvas.getByTestId('test-input');

    await userEvent.type(input, 'Hello World');

    await expect(input).toHaveValue('Hello World');
  },
};
```

## 문서화

### MDX 문서

```mdx
{/* Button.mdx */}
import { Meta, Story, Canvas, Controls, Description } from '@storybook/blocks';
import * as ButtonStories from './Button.stories';

<Meta of={ButtonStories} />

# Button

<Description of={ButtonStories} />

## 기본 사용법

<Canvas of={ButtonStories.Primary} />

## Props

<Controls of={ButtonStories.Primary} />

## Variants

<Canvas of={ButtonStories.AllVariants} />
```

### JSDoc 주석

```tsx
/**
 * Primary UI component for user interaction
 *
 * @example
 * ```tsx
 * <Button variant="primary" onClick={handleClick}>
 *   Click me
 * </Button>
 * ```
 */
export const Button = forwardRef<HTMLButtonElement, ButtonComponentProps>(
  // ...
);
```

## 접근성 테스트

```tsx
export const AccessibilityTest: Story = {
  args: {
    variant: 'primary',
    children: 'Accessible Button',
  },
  parameters: {
    a11y: {
      config: {
        rules: [
          { id: 'color-contrast', enabled: true },
          { id: 'button-name', enabled: true },
        ],
      },
    },
  },
};
```

## 반응형 테스트

```tsx
export const Responsive: Story = {
  parameters: {
    viewport: {
      viewports: {
        mobile: { name: 'Mobile', styles: { width: '375px', height: '667px' } },
        tablet: { name: 'Tablet', styles: { width: '768px', height: '1024px' } },
        desktop: { name: 'Desktop', styles: { width: '1440px', height: '900px' } },
      },
    },
  },
  render: () => (
    <div className="flex flex-col md:flex-row gap-4">
      <Button fullWidth className="md:w-auto">
        Responsive Button
      </Button>
    </div>
  ),
};
```

## 모범 사례

### 1. 의미 있는 스토리 이름

```tsx
// Bad
export const Story1: Story = {};

// Good
export const Primary: Story = {};
export const Disabled: Story = {};
export const Loading: Story = {};
```

### 2. 실제 사용 사례

```tsx
export const FormSubmitButton: Story = {
  render: () => {
    const [isLoading, setIsLoading] = useState(false);

    const handleSubmit = async () => {
      setIsLoading(true);
      await new Promise((r) => setTimeout(r, 2000));
      setIsLoading(false);
    };

    return (
      <Button loading={isLoading} onClick={handleSubmit}>
        {isLoading ? 'Saving...' : 'Save Changes'}
      </Button>
    );
  },
};
```

### 3. 컴포넌트 조합

```tsx
export const ButtonGroup: Story = {
  render: () => (
    <div className="flex gap-2">
      <Button variant="ghost">Cancel</Button>
      <Button variant="primary">Save</Button>
    </div>
  ),
};
```

### 4. 에러 상태

```tsx
export const WithError: Story = {
  args: {
    error: true,
    errorMessage: 'This field is required',
  },
};
```
