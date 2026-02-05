---
id: design-pattern-002
title: Storybook - 컴포넌트 문서화
type: learning
created: 2026-01-22
updated: 2026-01-22
author: Portal Universe Team
tags:
  - design-system
  - storybook
  - documentation
  - testing
  - vue
related:
  - design-pattern-001
  - design-pattern-003
---

# Storybook - 컴포넌트 문서화

## 학습 목표

- Storybook의 역할과 중요성 이해
- Story 작성 방법 학습 (CSF 3.0)
- Args, ArgTypes, Controls 활용법 습득
- Portal Universe Storybook 설정 분석
- Theme Decorator & Global Types 구현 이해

## 1. Storybook이란?

### 1.1 개념

Storybook은 **UI 컴포넌트를 독립적으로 개발하고 문서화**하는 도구입니다.

**핵심 가치:**
- ✅ **Isolated Development**: 컴포넌트를 앱 외부에서 독립적으로 개발
- ✅ **Living Documentation**: 자동 생성되는 문서 (항상 최신)
- ✅ **Visual Testing**: 다양한 State를 시각적으로 테스트
- ✅ **Collaboration**: 디자이너-개발자 협업 도구

### 1.2 Storybook 용어

| 용어 | 설명 | 예시 |
|------|------|------|
| **Story** | 컴포넌트의 특정 상태 | `Primary`, `Secondary`, `Disabled` |
| **Args** | 컴포넌트 Props | `{ variant: 'primary', size: 'md' }` |
| **ArgTypes** | Args 타입 정의 | `{ variant: { control: 'select' } }` |
| **Controls** | 인터랙티브 UI | Select, Boolean, Text 등 |
| **Decorator** | Story Wrapper | Theme Provider, Router |

## 2. Portal Universe Storybook 설정

### 2.1 디렉토리 구조

```
frontend/design-system-vue/
├── .storybook/
│   ├── main.ts              # Storybook 설정
│   ├── preview.ts           # Preview 설정
│   ├── preview-head.html    # Head 태그
│   └── manager.ts           # Manager 설정
├── src/
│   └── components/
│       └── Button/
│           ├── Button.vue
│           ├── Button.stories.ts    # ← Story 파일
│           └── Button.types.ts
```

### 2.2 Preview 설정 분석

`frontend/design-system-vue/.storybook/preview.ts`:

```typescript
import type { Preview } from '@storybook/vue3';
import { themes } from 'storybook/theming';
import '../src/styles/index.css';

// Linear-inspired Dark Theme
const portalDarkTheme = {
  ...themes.dark,

  // Brand
  brandTitle: 'Portal Design System',
  brandUrl: '/',

  // UI Colors
  appBg: '#08090a',                 // Background
  appContentBg: '#0e0f10',          // Content area
  appPreviewBg: '#08090a',          // Preview area
  appBorderColor: '#26282b',        // Borders

  // Typography
  fontBase: '"Inter Variable", sans-serif',
  fontCode: '"JetBrains Mono", monospace',

  // Text colors
  textColor: '#f7f8f8',
  textMutedColor: '#8a8f98',

  // Toolbar
  barBg: '#0e0f10',
  barTextColor: '#8a8f98',
  barSelectedColor: '#5e6ad2',

  // Brand colors
  colorPrimary: '#5e6ad2',
};

const preview: Preview = {
  parameters: {
    actions: { argTypesRegex: '^on[A-Z].*' },
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/,
      },
    },
    docs: {
      theme: portalDarkTheme,
    },
  },

  // Global Types (Toolbar)
  globalTypes: {
    theme: {
      name: 'Theme',
      description: 'Global theme for components',
      defaultValue: 'dark',
      toolbar: {
        icon: 'circlehollow',
        items: [
          { value: 'dark', title: 'Dark (Default)' },
          { value: 'light', title: 'Light' },
        ],
        showName: true,
        dynamicTitle: true,
      },
    },
    service: {
      name: 'Service',
      description: 'Service theme variant',
      defaultValue: 'portal',
      toolbar: {
        icon: 'globe',
        items: [
          { value: 'portal', title: 'Portal (Indigo)' },
          { value: 'blog', title: 'Blog (Green)' },
          { value: 'shopping', title: 'Shopping (Orange)' },
        ],
        showName: true,
        dynamicTitle: true,
      },
    },
  },

  // Decorator (Theme Wrapper)
  decorators: [
    (story, context) => {
      const theme = context.globals.theme || 'dark';
      const service = context.globals.service || 'portal';

      return {
        setup() {
          // HTML attributes 설정
          if (typeof document !== 'undefined') {
            document.documentElement.setAttribute('data-theme', theme);
            document.documentElement.setAttribute('data-service', service);

            if (theme === 'dark') {
              document.documentElement.classList.add('dark');
            } else {
              document.documentElement.classList.remove('dark');
            }
          }
          return {};
        },
        template: `
          <div class="p-6 min-h-screen bg-bg-page text-text-body transition-all">
            <story />
          </div>
        `,
      };
    },
  ],
};

export default preview;
```

### 2.3 핵심 기능

#### 1. Custom Theme
```typescript
const portalDarkTheme = {
  ...themes.dark,
  brandTitle: 'Portal Design System',
  appBg: '#08090a',
  colorPrimary: '#5e6ad2',
};
```

#### 2. Global Types (Toolbar Controls)
```typescript
globalTypes: {
  theme: {
    defaultValue: 'dark',
    toolbar: {
      icon: 'circlehollow',
      items: [
        { value: 'dark', title: 'Dark (Default)' },
        { value: 'light', title: 'Light' },
      ],
    },
  },
}
```

#### 3. Decorator (Theme Provider)
```typescript
decorators: [
  (story, context) => {
    const theme = context.globals.theme;

    // HTML attribute 설정
    document.documentElement.setAttribute('data-theme', theme);

    // Wrapper
    return {
      template: `
        <div class="bg-bg-page text-text-body">
          <story />
        </div>
      `,
    };
  },
]
```

## 3. Story 작성 (CSF 3.0)

### 3.1 기본 구조

`Button.stories.ts`:

```typescript
import type { Meta, StoryObj } from '@storybook/vue3';
import { Button } from './index';

// Meta 정의
const meta: Meta<typeof Button> = {
  title: 'Components/Button',         // Sidebar 위치
  component: Button,
  tags: ['autodocs'],                 // 자동 문서 생성
  argTypes: {
    variant: {
      control: 'select',
      options: ['primary', 'secondary', 'ghost', 'outline', 'danger'],
    },
    size: {
      control: 'select',
      options: ['xs', 'sm', 'md', 'lg'],
    },
    disabled: {
      control: 'boolean',
    },
    loading: {
      control: 'boolean',
    },
  },
};

export default meta;
type Story = StoryObj<typeof Button>;

// Stories
export const Primary: Story = {
  args: {
    variant: 'primary',
    default: 'Primary Button',
  },
  render: (args) => ({
    components: { Button },
    setup() {
      return { args };
    },
    template: '<Button v-bind="args">{{ args.default }}</Button>',
  }),
};

export const Secondary: Story = {
  args: {
    variant: 'secondary',
    default: 'Secondary Button',
  },
  render: (args) => ({
    components: { Button },
    setup() {
      return { args };
    },
    template: '<Button v-bind="args">{{ args.default }}</Button>',
  }),
};

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

export const Sizes: Story = {
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

export const WithIcon: Story = {
  render: () => ({
    components: { Button },
    template: `
      <div class="flex gap-4">
        <Button variant="primary">
          <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          Add Item
        </Button>
      </div>
    `,
  }),
};

export const Loading: Story = {
  args: {
    loading: true,
    default: 'Loading...',
  },
  render: (args) => ({
    components: { Button },
    setup() {
      return { args };
    },
    template: '<Button v-bind="args">{{ args.default }}</Button>',
  }),
};
```

### 3.2 React Story 예시

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
    <div className="flex flex-wrap gap-4">
      <Button variant="primary">Primary</Button>
      <Button variant="secondary">Secondary</Button>
      <Button variant="ghost">Ghost</Button>
      <Button variant="outline">Outline</Button>
      <Button variant="danger">Danger</Button>
    </div>
  ),
};
```

## 4. ArgTypes & Controls

### 4.1 Control Types

```typescript
argTypes: {
  // Select
  variant: {
    control: 'select',
    options: ['primary', 'secondary'],
  },

  // Radio
  size: {
    control: 'radio',
    options: ['sm', 'md', 'lg'],
  },

  // Boolean
  disabled: {
    control: 'boolean',
  },

  // Text
  label: {
    control: 'text',
  },

  // Number
  count: {
    control: { type: 'number', min: 0, max: 100, step: 1 },
  },

  // Color
  color: {
    control: 'color',
  },

  // Date
  date: {
    control: 'date',
  },

  // Object
  user: {
    control: 'object',
  },
}
```

### 4.2 Description & Table

```typescript
argTypes: {
  variant: {
    control: 'select',
    options: ['primary', 'secondary'],
    description: 'Button variant style',
    table: {
      type: { summary: 'string' },
      defaultValue: { summary: 'primary' },
    },
  },
}
```

## 5. 실습 예제

### 예제 1: Input Story

```typescript
// Input.stories.ts
import type { Meta, StoryObj } from '@storybook/vue3';
import { Input } from './index';

const meta: Meta<typeof Input> = {
  title: 'Components/Input',
  component: Input,
  tags: ['autodocs'],
  argTypes: {
    type: {
      control: 'select',
      options: ['text', 'email', 'password', 'number'],
    },
    size: {
      control: 'radio',
      options: ['sm', 'md', 'lg'],
    },
    error: {
      control: 'boolean',
    },
    disabled: {
      control: 'boolean',
    },
  },
};

export default meta;
type Story = StoryObj<typeof Input>;

export const Default: Story = {
  args: {
    label: 'Email',
    placeholder: 'Enter your email',
    type: 'email',
  },
};

export const WithError: Story = {
  args: {
    label: 'Email',
    type: 'email',
    error: true,
    errorMessage: 'Please enter a valid email',
  },
};

export const Sizes: Story = {
  render: () => ({
    components: { Input },
    template: `
      <div class="space-y-4">
        <Input size="sm" label="Small" placeholder="Small input" />
        <Input size="md" label="Medium" placeholder="Medium input" />
        <Input size="lg" label="Large" placeholder="Large input" />
      </div>
    `,
  }),
};
```

### 예제 2: Modal Story

```typescript
// Modal.stories.ts
import type { Meta, StoryObj } from '@storybook/vue3';
import { ref } from 'vue';
import { Modal } from './index';
import { Button } from '../Button';

const meta: Meta<typeof Modal> = {
  title: 'Components/Modal',
  component: Modal,
  tags: ['autodocs'],
};

export default meta;
type Story = StoryObj<typeof Modal>;

export const Default: Story = {
  render: () => ({
    components: { Modal, Button },
    setup() {
      const isOpen = ref(false);
      return { isOpen };
    },
    template: `
      <div>
        <Button @click="isOpen = true">Open Modal</Button>
        <Modal v-model="isOpen" title="Modal Title" size="md">
          <p>This is modal content.</p>
        </Modal>
      </div>
    `,
  }),
};

export const WithForm: Story = {
  render: () => ({
    components: { Modal, Button, Input },
    setup() {
      const isOpen = ref(false);
      return { isOpen };
    },
    template: `
      <div>
        <Button @click="isOpen = true">Open Form Modal</Button>
        <Modal v-model="isOpen" title="Add User" size="md">
          <div class="space-y-4">
            <Input label="Name" placeholder="Enter name" />
            <Input label="Email" type="email" placeholder="Enter email" />
          </div>
          <template #footer>
            <div class="flex gap-2 justify-end">
              <Button variant="secondary" @click="isOpen = false">Cancel</Button>
              <Button variant="primary">Submit</Button>
            </div>
          </template>
        </Modal>
      </div>
    `,
  }),
};
```

### 예제 3: Interactive Story

```typescript
// Counter.stories.ts
export const Interactive: Story = {
  render: () => ({
    setup() {
      const count = ref(0);
      const increment = () => count.value++;
      const decrement = () => count.value--;

      return { count, increment, decrement };
    },
    template: `
      <div class="space-y-4">
        <div class="text-2xl font-bold">Count: {{ count }}</div>
        <div class="flex gap-2">
          <Button @click="decrement">-</Button>
          <Button @click="increment">+</Button>
        </div>
      </div>
    `,
  }),
};
```

## 6. 고급 패턴

### 6.1 Play Function (User Interaction)

```typescript
import { userEvent, within } from '@storybook/testing-library';
import { expect } from '@storybook/jest';

export const TestInteraction: Story = {
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);

    // Button 찾기
    const button = canvas.getByRole('button', { name: /submit/i });

    // 클릭
    await userEvent.click(button);

    // 검증
    await expect(canvas.getByText('Submitted')).toBeInTheDocument();
  },
};
```

### 6.2 Parameters

```typescript
export const Primary: Story = {
  parameters: {
    // Backgrounds
    backgrounds: {
      default: 'dark',
      values: [
        { name: 'dark', value: '#08090a' },
        { name: 'light', value: '#ffffff' },
      ],
    },

    // Layout
    layout: 'centered',  // 'centered' | 'fullscreen' | 'padded'

    // Docs
    docs: {
      description: {
        story: 'This is the primary button variant.',
      },
    },
  },
};
```

### 6.3 Custom Decorator

```typescript
// .storybook/preview.ts
export const decorators = [
  (story) => ({
    components: { story },
    template: `
      <div class="p-8 bg-bg-page">
        <story />
      </div>
    `,
  }),
];
```

## 7. 핵심 요약

### ✅ Key Takeaways

1. **Story = Component State**: 각 상태를 Story로 표현
2. **Args = Props**: 인터랙티브하게 Props 조작
3. **ArgTypes = Control**: Select, Boolean, Text 등
4. **Decorator = Wrapper**: Theme, Router 등 Context 제공
5. **autodocs**: 자동 문서 생성

### 🎯 Best Practices

```typescript
// ✅ DO
// 1. 모든 주요 State를 Story로 작성
export const Primary: Story = { ... };
export const Disabled: Story = { ... };
export const Loading: Story = { ... };

// 2. Variants 한눈에 보기
export const AllVariants: Story = {
  render: () => (
    <div className="flex gap-4">
      <Button variant="primary">Primary</Button>
      <Button variant="secondary">Secondary</Button>
    </div>
  ),
};

// 3. ArgTypes로 Control 제공
argTypes: {
  variant: { control: 'select', options: [...] },
}

// ❌ DON'T
// 1. 단일 Story만 작성
export const Default: Story = { ... };  // Only one

// 2. Control 없이 하드코딩
// 3. Decorator 없이 직접 Theme 처리
```

### 📋 Story Checklist

- [ ] Default Story
- [ ] All Variants Story
- [ ] Size Variants Story
- [ ] Disabled State
- [ ] Loading State (해당 시)
- [ ] Error State (해당 시)
- [ ] With Icon (해당 시)
- [ ] Interactive Example

## 8. 관련 문서

- [Button Component](../components/button-component.md) - Button Story 예시
- [Theming](./theming.md) - Storybook Theme 설정
- [Dual Framework](./dual-framework.md) - Vue/React Story 차이
