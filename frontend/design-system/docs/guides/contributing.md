---
id: guide-contributing
title: 기여 가이드
type: guide
status: current
created: 2026-01-18
updated: 2026-01-18
author: documenter
tags: [design-system, contributing, component-development]
related:
  - guide-getting-started
  - arch-system-overview
---

# 기여 가이드

> Design System에 새로운 컴포넌트를 추가하는 방법

---

## 📋 개요

Design System에 기여하기 위한 가이드입니다. 새로운 컴포넌트 추가, 기존 컴포넌트 수정, 문서화 등의 방법을 설명합니다.

---

## 🏗️ 새 컴포넌트 추가

### Step 1: 디렉토리 구조 생성

```bash
# src/components/ 하위에 새 컴포넌트 디렉토리 생성
mkdir -p src/components/NewComponent
```

디렉토리 구조:
```
src/components/NewComponent/
├── NewComponent.vue        # 컴포넌트 구현
├── NewComponent.types.ts   # Props 인터페이스
├── NewComponent.stories.ts # Storybook 스토리
├── __tests__/
│   └── NewComponent.test.ts # 단위 테스트
└── index.ts                # Export
```

### Step 2: 타입 정의

```typescript
// src/components/NewComponent/NewComponent.types.ts
export interface NewComponentProps {
  /**
   * 컴포넌트 변형
   * @default 'default'
   */
  variant?: 'default' | 'primary' | 'secondary'

  /**
   * 컴포넌트 크기
   * @default 'md'
   */
  size?: 'sm' | 'md' | 'lg'

  /**
   * 비활성화 상태
   * @default false
   */
  disabled?: boolean
}

export interface NewComponentEmits {
  (e: 'click', event: MouseEvent): void
  (e: 'update:modelValue', value: string): void
}
```

### Step 3: 컴포넌트 구현

```vue
<!-- src/components/NewComponent/NewComponent.vue -->
<script setup lang="ts">
import type { NewComponentProps, NewComponentEmits } from './NewComponent.types'

const props = withDefaults(defineProps<NewComponentProps>(), {
  variant: 'default',
  size: 'md',
  disabled: false
})

const emit = defineEmits<NewComponentEmits>()

// Variant 클래스 매핑
const variantClasses = {
  default: 'bg-gray-100 text-gray-900',
  primary: 'bg-brand-primary text-white',
  secondary: 'bg-gray-200 text-gray-800'
}

// Size 클래스 매핑
const sizeClasses = {
  sm: 'px-2 py-1 text-sm',
  md: 'px-4 py-2 text-base',
  lg: 'px-6 py-3 text-lg'
}

const handleClick = (event: MouseEvent) => {
  if (!props.disabled) {
    emit('click', event)
  }
}
</script>

<template>
  <div
    :class="[
      'rounded-md transition-colors',
      variantClasses[variant],
      sizeClasses[size],
      disabled && 'opacity-50 cursor-not-allowed'
    ]"
    @click="handleClick"
  >
    <slot />
  </div>
</template>
```

### Step 4: Export 설정

```typescript
// src/components/NewComponent/index.ts
export { default as NewComponent } from './NewComponent.vue'
export type { NewComponentProps, NewComponentEmits } from './NewComponent.types'
```

```typescript
// src/components/index.ts에 추가
export * from './NewComponent'
```

### Step 5: Storybook 스토리 작성

```typescript
// src/components/NewComponent/NewComponent.stories.ts
import type { Meta, StoryObj } from '@storybook/vue3'
import NewComponent from './NewComponent.vue'

const meta: Meta<typeof NewComponent> = {
  title: 'Components/NewComponent',
  component: NewComponent,
  tags: ['autodocs'],
  argTypes: {
    variant: {
      control: { type: 'select' },
      options: ['default', 'primary', 'secondary']
    },
    size: {
      control: { type: 'select' },
      options: ['sm', 'md', 'lg']
    },
    disabled: {
      control: { type: 'boolean' }
    }
  }
}

export default meta
type Story = StoryObj<typeof NewComponent>

export const Default: Story = {
  args: {
    variant: 'default',
    size: 'md'
  },
  render: (args) => ({
    components: { NewComponent },
    setup() {
      return { args }
    },
    template: '<NewComponent v-bind="args">New Component</NewComponent>'
  })
}

export const Primary: Story = {
  args: {
    variant: 'primary',
    size: 'md'
  },
  render: (args) => ({
    components: { NewComponent },
    setup() {
      return { args }
    },
    template: '<NewComponent v-bind="args">Primary</NewComponent>'
  })
}

export const AllSizes: Story = {
  render: () => ({
    components: { NewComponent },
    template: `
      <div class="flex gap-4 items-center">
        <NewComponent size="sm">Small</NewComponent>
        <NewComponent size="md">Medium</NewComponent>
        <NewComponent size="lg">Large</NewComponent>
      </div>
    `
  })
}
```

### Step 6: 단위 테스트 작성

```typescript
// src/components/NewComponent/__tests__/NewComponent.test.ts
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import NewComponent from '../NewComponent.vue'

describe('NewComponent', () => {
  it('renders correctly', () => {
    const wrapper = mount(NewComponent, {
      slots: {
        default: 'Test Content'
      }
    })

    expect(wrapper.text()).toContain('Test Content')
  })

  it('applies variant class', () => {
    const wrapper = mount(NewComponent, {
      props: {
        variant: 'primary'
      }
    })

    expect(wrapper.classes()).toContain('bg-brand-primary')
  })

  it('applies size class', () => {
    const wrapper = mount(NewComponent, {
      props: {
        size: 'lg'
      }
    })

    expect(wrapper.classes()).toContain('px-6')
  })

  it('emits click event', async () => {
    const wrapper = mount(NewComponent)

    await wrapper.trigger('click')

    expect(wrapper.emitted()).toHaveProperty('click')
  })

  it('does not emit click when disabled', async () => {
    const wrapper = mount(NewComponent, {
      props: {
        disabled: true
      }
    })

    await wrapper.trigger('click')

    expect(wrapper.emitted()).not.toHaveProperty('click')
  })
})
```

---

## ✅ 체크리스트

### 새 컴포넌트 추가 시

- [ ] 타입 정의 파일 생성 (`*.types.ts`)
- [ ] 컴포넌트 구현 (`*.vue`)
- [ ] Export 설정 (`index.ts`)
- [ ] 컴포넌트 인덱스에 추가 (`src/components/index.ts`)
- [ ] Storybook 스토리 작성 (`*.stories.ts`)
- [ ] 단위 테스트 작성 (`__tests__/*.test.ts`)
- [ ] API 문서 업데이트

### 코드 스타일

- [ ] TypeScript strict 모드 준수
- [ ] `<script setup>` 문법 사용
- [ ] Tailwind 유틸리티 클래스 사용
- [ ] Semantic 토큰 사용 (하드코딩 색상 금지)

---

## 🔄 Pull Request 가이드

### 커밋 메시지 형식

```
feat(design-system): add NewComponent

- Add NewComponent.vue with variants and sizes
- Add Storybook stories
- Add unit tests
```

### PR 체크리스트

- [ ] 테스트 통과 (`npm test`)
- [ ] 린트 통과 (`npm run lint`)
- [ ] Storybook 빌드 성공 (`npm run build-storybook`)
- [ ] 문서 업데이트

---

## 🔗 관련 문서

- [Architecture](../architecture/README.md) - 시스템 아키텍처
- [Token System](../architecture/token-system.md) - 토큰 시스템
- [API Reference](../api/README.md) - API 문서

---

**최종 업데이트**: 2026-01-18
