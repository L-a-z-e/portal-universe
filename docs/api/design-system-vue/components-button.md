---
id: components-button-vue
title: Button Components
type: api
status: current
created: 2026-01-19
updated: 2026-02-06
author: documenter
tags: [api, vue, button]
related:
  - components-input-vue
  - components-feedback-vue
---

# Button Components

버튼 관련 컴포넌트의 API 레퍼런스입니다.

## DsButton

기본 버튼 컴포넌트입니다.

### Import

```ts
import { DsButton } from '@portal/design-system-vue';
import type { ButtonProps } from '@portal/design-system-vue';
```

### TypeScript Interface

```typescript
interface ButtonProps {
  variant?: 'primary' | 'secondary' | 'ghost' | 'outline' | 'danger';
  size?: 'xs' | 'sm' | 'md' | 'lg';
  disabled?: boolean;
  loading?: boolean;
  fullWidth?: boolean;
  type?: 'button' | 'submit' | 'reset';
}
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `variant` | `'primary' \| 'secondary' \| 'ghost' \| 'outline' \| 'danger'` | `'primary'` | 버튼 스타일 |
| `size` | `'xs' \| 'sm' \| 'md' \| 'lg'` | `'md'` | 버튼 크기 |
| `disabled` | `boolean` | `false` | 비활성화 |
| `loading` | `boolean` | `false` | 로딩 상태 |
| `fullWidth` | `boolean` | `false` | 전체 너비 |
| `type` | `'button' \| 'submit' \| 'reset'` | `'button'` | HTML 타입 |

### Events

| Event | Payload | Description |
|-------|---------|-------------|
| `click` | `MouseEvent` | 클릭 이벤트 |

### Slots

| Slot | Description |
|------|-------------|
| `default` | 버튼 내용 |

### 기본 사용법

```vue
<template>
  <DsButton @click="handleClick">
    Click me
  </DsButton>
</template>

<script setup lang="ts">
import { DsButton } from '@portal/design-system-vue';

function handleClick(event: MouseEvent) {
  console.log('Button clicked!', event);
}
</script>
```

### Variants

```vue
<template>
  <div class="flex gap-4">
    <DsButton variant="primary">Primary</DsButton>
    <DsButton variant="secondary">Secondary</DsButton>
    <DsButton variant="ghost">Ghost</DsButton>
    <DsButton variant="outline">Outline</DsButton>
    <DsButton variant="danger">Danger</DsButton>
  </div>
</template>
```

### Sizes

```vue
<template>
  <div class="flex items-center gap-4">
    <DsButton size="xs">Extra Small</DsButton>
    <DsButton size="sm">Small</DsButton>
    <DsButton size="md">Medium</DsButton>
    <DsButton size="lg">Large</DsButton>
  </div>
</template>
```

### Loading State

```vue
<template>
  <DsButton :loading="isLoading" @click="handleSubmit">
    {{ isLoading ? 'Saving...' : 'Save' }}
  </DsButton>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { DsButton } from '@portal/design-system-vue';

const isLoading = ref(false);

async function handleSubmit() {
  isLoading.value = true;
  try {
    await saveData();
  } finally {
    isLoading.value = false;
  }
}
</script>
```

### Full Width

```vue
<template>
  <DsButton fullWidth>
    Full Width Button
  </DsButton>
</template>
```

### With Icon

```vue
<template>
  <DsButton variant="primary">
    <IconPlus class="w-4 h-4" />
    Add Item
  </DsButton>
</template>
```

### Form Submit

```vue
<template>
  <form @submit.prevent="handleSubmit">
    <DsInput v-model="email" label="Email" />
    <DsButton type="submit" :loading="isSubmitting">
      Submit
    </DsButton>
  </form>
</template>
```

### Button Group

```vue
<template>
  <div class="flex">
    <DsButton variant="outline" class="rounded-r-none border-r-0">
      Left
    </DsButton>
    <DsButton variant="outline" class="rounded-none">
      Center
    </DsButton>
    <DsButton variant="outline" class="rounded-l-none border-l-0">
      Right
    </DsButton>
  </div>
</template>
```

### Disabled State

```vue
<template>
  <div class="flex gap-4">
    <DsButton disabled>Disabled Primary</DsButton>
    <DsButton variant="secondary" disabled>Disabled Secondary</DsButton>
    <DsButton variant="danger" disabled>Disabled Danger</DsButton>
  </div>
</template>
```

### CSS Classes

| Class | Description |
|-------|-------------|
| `.ds-button` | 기본 클래스 |
| `.ds-button--primary` | Primary variant |
| `.ds-button--secondary` | Secondary variant |
| `.ds-button--ghost` | Ghost variant |
| `.ds-button--outline` | Outline variant |
| `.ds-button--danger` | Danger variant |
| `.ds-button--xs` | Extra small size |
| `.ds-button--sm` | Small size |
| `.ds-button--md` | Medium size |
| `.ds-button--lg` | Large size |
| `.ds-button--loading` | Loading state |
| `.ds-button--full-width` | Full width |

### Customization

CSS 변수를 오버라이드하여 커스터마이징할 수 있습니다:

```css
.my-custom-button {
  --ds-button-bg: #8b5cf6;
  --ds-button-bg-hover: #7c3aed;
  --ds-button-text: #ffffff;
}
```

### Accessibility

- `disabled` 상태에서 `aria-disabled="true"` 설정
- `loading` 상태에서 `aria-busy="true"` 설정
- 키보드 네비게이션 지원 (Tab, Enter, Space)
