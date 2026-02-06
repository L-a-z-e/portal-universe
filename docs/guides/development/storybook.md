---
id: storybook-vue
title: Storybook Guide
type: guide
status: current
created: 2026-01-19
updated: 2026-01-19
author: Laze
tags: [guide, vue, storybook]
related:
  - getting-started-vue
  - system-overview-vue
---

# Storybook Guide

**난이도**: ⭐⭐ | **예상 시간**: 15분 | **카테고리**: Development

Design System Vue의 Storybook 사용 방법을 안내합니다.

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

```ts
// Button.stories.ts
import type { Meta, StoryObj } from '@storybook/vue3';
import Button from './Button.vue';

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
  },
  render: (args) => ({
    components: { Button },
    setup() { return { args }; },
    template: '<Button v-bind="args">Primary Button</Button>',
  }),
};
```

### 여러 변형 보여주기

```ts
export const AllVariants: Story = {
  render: () => ({
    components: { Button },
    template: `
      <div class="flex flex-wrap gap-4">
        <Button variant="primary">Primary</Button>
        <Button variant="secondary">Secondary</Button>
        <Button variant="ghost">Ghost</Button>
        <Button variant="outline">Outline</Button>
        <Button variant="danger">Danger</Button>
      </div>
    `,
  }),
};

export const AllSizes: Story = {
  render: () => ({
    components: { Button },
    template: `
      <div class="flex items-center gap-4">
        <Button size="xs">Extra Small</Button>
        <Button size="sm">Small</Button>
        <Button size="md">Medium</Button>
        <Button size="lg">Large</Button>
      </div>
    `,
  }),
};
```

### 인터랙티브 스토리

```ts
export const Interactive: Story = {
  args: {
    variant: 'primary',
    size: 'md',
    disabled: false,
    loading: false,
    fullWidth: false,
  },
  render: (args) => ({
    components: { Button },
    setup() { return { args }; },
    template: '<Button v-bind="args">Interactive Button</Button>',
  }),
};
```

## 테마 지원

### 테마 데코레이터

```ts
// .storybook/preview.ts
import type { Preview } from '@storybook/vue3';
import '@portal/design-tokens/dist/tokens.css';
import '../src/styles/index.css';

const preview: Preview = {
  parameters: {
    backgrounds: {
      disable: true,
    },
  },
  globalTypes: {
    theme: {
      name: 'Theme',
      description: 'Global theme for components',
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
    (story, context) => ({
      setup() {
        const theme = context.globals.theme;
        const service = context.globals.service;

        return { theme, service };
      },
      template: `
        <div :data-theme="theme" :data-service="service" class="p-4">
          <story />
        </div>
      `,
    }),
  ],
};

export default preview;
```

### 테마별 스토리

```ts
export const LightMode: Story = {
  decorators: [
    (story) => ({
      template: `
        <div data-theme="light" data-service="blog" class="p-4">
          <story />
        </div>
      `,
    }),
  ],
  render: () => ({
    components: { Button },
    template: '<Button>Light Mode Button</Button>',
  }),
};
```

## 폼 컴포넌트 스토리

### 폼 그룹

```ts
// FormExample.stories.ts
export const LoginForm: Story = {
  render: () => ({
    components: { Card, Input, Checkbox, Button },
    setup() {
      const email = ref('');
      const password = ref('');
      const remember = ref(false);
      return { email, password, remember };
    },
    template: `
      <Card padding="lg" class="w-96">
        <h2 class="text-xl font-semibold mb-4">Login</h2>
        <form class="space-y-4">
          <Input
            v-model="email"
            type="email"
            label="Email"
            placeholder="email@example.com"
          />
          <Input
            v-model="password"
            type="password"
            label="Password"
            placeholder="••••••••"
          />
          <Checkbox v-model="remember" label="Remember me" />
          <Button type="submit" fullWidth>Sign In</Button>
        </form>
      </Card>
    `,
  }),
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

## 사용 가이드

버튼은 사용자 액션을 트리거하는 데 사용됩니다.

### Primary 버튼
- 페이지당 하나의 주요 액션에 사용
- 폼 제출, CTA 등

### Secondary 버튼
- 보조 액션에 사용
- 취소, 이전 등

### Danger 버튼
- 위험하거나 되돌릴 수 없는 액션에 사용
- 삭제, 영구 제거 등
```

### 자동 문서화

```ts
const meta: Meta<typeof Button> = {
  title: 'Components/Button',
  component: Button,
  tags: ['autodocs'], // 자동 문서 생성
  parameters: {
    docs: {
      description: {
        component: '사용자 인터랙션을 위한 기본 버튼 컴포넌트입니다.',
      },
    },
  },
};
```

## 테스트

### 인터랙션 테스트

```ts
import { within, userEvent, expect } from '@storybook/test';

export const ClickTest: Story = {
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    const button = canvas.getByRole('button');

    await userEvent.click(button);

    // 클릭 후 상태 확인
    await expect(button).toHaveFocus();
  },
};
```

### 접근성 테스트

```ts
export const AccessibilityTest: Story = {
  args: {
    variant: 'primary',
  },
  parameters: {
    a11y: {
      config: {
        rules: [
          { id: 'color-contrast', enabled: true },
        ],
      },
    },
  },
};
```

## 모범 사례

### 1. 의미 있는 스토리 이름

```ts
// Bad
export const Story1: Story = {};
export const Story2: Story = {};

// Good
export const Primary: Story = {};
export const Disabled: Story = {};
export const Loading: Story = {};
```

### 2. 실제 사용 사례 반영

```ts
// 실제 사용 시나리오
export const FormSubmitButton: Story = {
  render: () => ({
    setup() {
      const isLoading = ref(false);
      const handleSubmit = async () => {
        isLoading.value = true;
        await new Promise(r => setTimeout(r, 2000));
        isLoading.value = false;
      };
      return { isLoading, handleSubmit };
    },
    template: `
      <Button :loading="isLoading" @click="handleSubmit">
        {{ isLoading ? 'Saving...' : 'Save Changes' }}
      </Button>
    `,
  }),
};
```

### 3. 컴포넌트 조합

```ts
export const ButtonGroup: Story = {
  render: () => ({
    components: { Button },
    template: `
      <div class="flex gap-2">
        <Button variant="ghost">Cancel</Button>
        <Button variant="primary">Save</Button>
      </div>
    `,
  }),
};
```

### 4. 반응형 테스트

```ts
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
  render: () => ({
    template: `
      <div class="flex flex-col md:flex-row gap-4">
        <Button fullWidth class="md:w-auto">Responsive Button</Button>
      </div>
    `,
  }),
};
```
