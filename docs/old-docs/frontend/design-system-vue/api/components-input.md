---
id: api-components-input
title: 입력 컴포넌트 API
type: api
status: current
created: 2026-01-18
updated: 2026-01-18
author: documenter
tags: [design-system, api, input, components, vue3]
related:
  - api-design-system
  - guide-using-components
---

# 입력 컴포넌트 API

> Button, Input, Textarea, Select, Checkbox, Radio, Switch, SearchBar

---

## 📋 개요

입력 컴포넌트는 사용자로부터 데이터를 수집하는 인터페이스를 제공합니다.

| 컴포넌트 | 용도 | v-model |
|---------|------|---------|
| Button | 클릭 가능한 버튼 | ❌ |
| Input | 단일 줄 텍스트 입력 | ✅ |
| Textarea | 여러 줄 텍스트 입력 | ✅ |
| Select | 드롭다운 선택 | ✅ |
| Checkbox | 체크박스 (다중 선택) | ✅ |
| Radio | 라디오 버튼 (단일 선택) | ✅ |
| Switch | 토글 스위치 | ✅ |
| SearchBar | 검색 입력창 | ✅ |

---

## 1️⃣ Button

클릭 가능한 버튼 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `variant` | `'primary' \| 'secondary' \| 'outline' \| 'ghost' \| 'danger'` | `'primary'` | ❌ | 버튼 스타일 변형 |
| `size` | `'xs' \| 'sm' \| 'md' \| 'lg' \| 'xl'` | `'md'` | ❌ | 버튼 크기 |
| `disabled` | `boolean` | `false` | ❌ | 비활성화 상태 |
| `loading` | `boolean` | `false` | ❌ | 로딩 상태 (스피너 표시) |
| `type` | `'button' \| 'submit' \| 'reset'` | `'button'` | ❌ | HTML button type |
| `fullWidth` | `boolean` | `false` | ❌ | 전체 너비 사용 |

### Events

| Event | Payload | Description |
|-------|---------|-------------|
| `click` | `MouseEvent` | 버튼 클릭 시 |

### Slots

| Slot | Description |
|------|-------------|
| `default` | 버튼 레이블 |
| `icon` | 버튼 아이콘 (좌측) |

### TypeScript Interface

```typescript
interface ButtonProps {
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger'
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl'
  disabled?: boolean
  loading?: boolean
  type?: 'button' | 'submit' | 'reset'
  fullWidth?: boolean
}
```

### 사용 예시

```vue
<script setup lang="ts">
import { Button } from '@portal/design-system'

const handleClick = () => {
  console.log('Button clicked!')
}
</script>

<template>
  <!-- 기본 사용 -->
  <Button variant="primary" @click="handleClick">
    저장하기
  </Button>

  <!-- 크기 변형 -->
  <Button size="sm">Small</Button>
  <Button size="md">Medium</Button>
  <Button size="lg">Large</Button>

  <!-- 변형 종류 -->
  <Button variant="primary">Primary</Button>
  <Button variant="secondary">Secondary</Button>
  <Button variant="outline">Outline</Button>
  <Button variant="ghost">Ghost</Button>
  <Button variant="danger">Danger</Button>

  <!-- 상태 -->
  <Button disabled>비활성</Button>
  <Button loading>로딩 중...</Button>

  <!-- 전체 너비 -->
  <Button fullWidth>전체 너비 버튼</Button>
</template>
```

---

## 2️⃣ Input

단일 줄 텍스트 입력 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `modelValue` | `string` | `''` | ❌ | 입력값 (v-model) |
| `type` | `'text' \| 'password' \| 'email' \| 'number' \| 'tel' \| 'url'` | `'text'` | ❌ | 입력 타입 |
| `placeholder` | `string` | `''` | ❌ | 플레이스홀더 텍스트 |
| `label` | `string` | - | ❌ | 입력 필드 라벨 |
| `disabled` | `boolean` | `false` | ❌ | 비활성화 상태 |
| `readonly` | `boolean` | `false` | ❌ | 읽기 전용 |
| `required` | `boolean` | `false` | ❌ | 필수 입력 표시 |
| `error` | `boolean` | `false` | ❌ | 오류 상태 표시 |
| `errorMessage` | `string` | - | ❌ | 오류 메시지 |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | ❌ | 입력 필드 크기 |

### Events

| Event | Payload | Description |
|-------|---------|-------------|
| `update:modelValue` | `string` | 입력값 변경 시 |
| `focus` | `FocusEvent` | 포커스 시 |
| `blur` | `FocusEvent` | 포커스 해제 시 |

### TypeScript Interface

```typescript
interface InputProps {
  modelValue?: string
  type?: 'text' | 'password' | 'email' | 'number' | 'tel' | 'url'
  placeholder?: string
  label?: string
  disabled?: boolean
  readonly?: boolean
  required?: boolean
  error?: boolean
  errorMessage?: string
  size?: 'sm' | 'md' | 'lg'
}
```

### 사용 예시

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { Input } from '@portal/design-system'

const email = ref('')
const isValidEmail = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value))
</script>

<template>
  <!-- 기본 사용 -->
  <Input v-model="email" type="email" placeholder="user@example.com" />

  <!-- 라벨과 필수 표시 -->
  <Input
    v-model="email"
    label="이메일"
    required
    placeholder="your@email.com"
  />

  <!-- 유효성 검사 -->
  <Input
    v-model="email"
    type="email"
    :error="!isValidEmail && email.length > 0"
    errorMessage="유효한 이메일을 입력하세요"
  />

  <!-- 비활성화 -->
  <Input v-model="email" disabled />
</template>
```

---

## 3️⃣ Textarea

여러 줄 텍스트 입력 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `modelValue` | `string` | `''` | ❌ | 입력값 (v-model) |
| `placeholder` | `string` | `''` | ❌ | 플레이스홀더 텍스트 |
| `label` | `string` | - | ❌ | 입력 필드 라벨 |
| `rows` | `number` | `3` | ❌ | 표시할 줄 수 |
| `disabled` | `boolean` | `false` | ❌ | 비활성화 상태 |
| `readonly` | `boolean` | `false` | ❌ | 읽기 전용 |
| `required` | `boolean` | `false` | ❌ | 필수 입력 표시 |
| `error` | `boolean` | `false` | ❌ | 오류 상태 표시 |
| `maxLength` | `number` | - | ❌ | 최대 글자 수 |
| `resize` | `'none' \| 'vertical' \| 'horizontal' \| 'both'` | `'vertical'` | ❌ | 리사이즈 방향 |

### Events

| Event | Payload | Description |
|-------|---------|-------------|
| `update:modelValue` | `string` | 입력값 변경 시 |
| `focus` | `FocusEvent` | 포커스 시 |
| `blur` | `FocusEvent` | 포커스 해제 시 |

### 사용 예시

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { Textarea } from '@portal/design-system'

const message = ref('')
</script>

<template>
  <Textarea
    v-model="message"
    label="메시지"
    placeholder="내용을 입력하세요..."
    rows="5"
    :maxLength="500"
  />
</template>
```

---

## 4️⃣ Select

드롭다운 선택 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `modelValue` | `string \| number` | - | ❌ | 선택된 값 (v-model) |
| `options` | `SelectOption[]` | `[]` | ✅ | 선택 옵션 목록 |
| `placeholder` | `string` | `'선택하세요'` | ❌ | 플레이스홀더 텍스트 |
| `label` | `string` | - | ❌ | 셀렉트 라벨 |
| `disabled` | `boolean` | `false` | ❌ | 비활성화 상태 |
| `required` | `boolean` | `false` | ❌ | 필수 선택 표시 |
| `error` | `boolean` | `false` | ❌ | 오류 상태 표시 |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | ❌ | 셀렉트 크기 |

### Types

```typescript
interface SelectOption {
  label: string
  value: string | number
  disabled?: boolean
}
```

### Events

| Event | Payload | Description |
|-------|---------|-------------|
| `update:modelValue` | `string \| number` | 선택 변경 시 |
| `change` | `string \| number` | 선택 변경 시 |

### 사용 예시

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { Select } from '@portal/design-system'

const selectedRole = ref('')
const roleOptions = [
  { label: '사용자', value: 'user' },
  { label: '관리자', value: 'admin' },
  { label: '슈퍼관리자', value: 'super_admin', disabled: true }
]
</script>

<template>
  <Select
    v-model="selectedRole"
    :options="roleOptions"
    label="역할 선택"
    placeholder="역할을 선택하세요"
    required
  />
</template>
```

---

## 5️⃣ Checkbox

체크박스 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `modelValue` | `boolean` | `false` | ❌ | 체크 상태 (v-model) |
| `label` | `string` | - | ❌ | 체크박스 라벨 |
| `disabled` | `boolean` | `false` | ❌ | 비활성화 상태 |
| `error` | `boolean` | `false` | ❌ | 오류 상태 표시 |
| `indeterminate` | `boolean` | `false` | ❌ | 불확정 상태 |

### Events

| Event | Payload | Description |
|-------|---------|-------------|
| `update:modelValue` | `boolean` | 체크 상태 변경 시 |
| `change` | `boolean` | 체크 상태 변경 시 |

### 사용 예시

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { Checkbox } from '@portal/design-system'

const agreeTerms = ref(false)
</script>

<template>
  <Checkbox
    v-model="agreeTerms"
    label="서비스 이용약관에 동의합니다"
    :error="!agreeTerms"
  />
</template>
```

---

## 6️⃣ Radio

라디오 버튼 그룹 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `modelValue` | `string \| number` | - | ❌ | 선택된 값 (v-model) |
| `options` | `RadioOption[]` | `[]` | ✅ | 라디오 옵션 목록 |
| `name` | `string` | - | ✅ | 라디오 그룹 이름 |
| `disabled` | `boolean` | `false` | ❌ | 비활성화 상태 |
| `direction` | `'horizontal' \| 'vertical'` | `'vertical'` | ❌ | 배치 방향 |

### Types

```typescript
interface RadioOption {
  label: string
  value: string | number
  disabled?: boolean
}
```

### Events

| Event | Payload | Description |
|-------|---------|-------------|
| `update:modelValue` | `string \| number` | 선택 변경 시 |

### 사용 예시

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { Radio } from '@portal/design-system'

const selectedPlan = ref('basic')
const planOptions = [
  { label: 'Basic (무료)', value: 'basic' },
  { label: 'Pro ($10/월)', value: 'pro' },
  { label: 'Enterprise (문의)', value: 'enterprise' }
]
</script>

<template>
  <Radio
    v-model="selectedPlan"
    :options="planOptions"
    name="pricing-plan"
    direction="vertical"
  />
</template>
```

---

## 7️⃣ Switch

토글 스위치 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `modelValue` | `boolean` | `false` | ❌ | 토글 상태 (v-model) |
| `label` | `string` | - | ❌ | 스위치 라벨 |
| `disabled` | `boolean` | `false` | ❌ | 비활성화 상태 |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | ❌ | 스위치 크기 |

### Events

| Event | Payload | Description |
|-------|---------|-------------|
| `update:modelValue` | `boolean` | 토글 상태 변경 시 |
| `change` | `boolean` | 토글 상태 변경 시 |

### 사용 예시

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { Switch } from '@portal/design-system'
import { useTheme } from '@portal/design-system'

const isDarkMode = ref(false)
const { toggleTheme } = useTheme()

const handleToggle = (value: boolean) => {
  toggleTheme()
}
</script>

<template>
  <Switch
    v-model="isDarkMode"
    label="다크 모드"
    @change="handleToggle"
  />
</template>
```

---

## 8️⃣ SearchBar

검색 입력 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `modelValue` | `string` | `''` | ❌ | 검색어 (v-model) |
| `placeholder` | `string` | `'검색...'` | ❌ | 플레이스홀더 텍스트 |
| `disabled` | `boolean` | `false` | ❌ | 비활성화 상태 |
| `loading` | `boolean` | `false` | ❌ | 로딩 상태 |
| `showClearButton` | `boolean` | `true` | ❌ | 초기화 버튼 표시 |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | ❌ | 검색바 크기 |

### Events

| Event | Payload | Description |
|-------|---------|-------------|
| `update:modelValue` | `string` | 검색어 변경 시 |
| `search` | `string` | Enter 키 또는 검색 버튼 클릭 시 |
| `clear` | - | 초기화 버튼 클릭 시 |

### 사용 예시

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { SearchBar } from '@portal/design-system'

const searchQuery = ref('')
const isSearching = ref(false)

const handleSearch = async (query: string) => {
  isSearching.value = true
  try {
    // API 호출
    await searchAPI(query)
  } finally {
    isSearching.value = false
  }
}

const handleClear = () => {
  searchQuery.value = ''
}
</script>

<template>
  <SearchBar
    v-model="searchQuery"
    placeholder="검색어를 입력하세요"
    :loading="isSearching"
    @search="handleSearch"
    @clear="handleClear"
  />
</template>
```

---

## 🔗 관련 문서

- [피드백 컴포넌트](./components-feedback.md) - Modal, Toast, Badge 등
- [레이아웃 컴포넌트](./components-layout.md) - Card, Container, Stack 등
- [컴포넌트 사용 가이드](../guides/using-components.md)

---

**최종 업데이트**: 2026-01-18
