---
id: api-components-input
title: 입력 컴포넌트 API
type: api
status: current
created: 2026-01-18
updated: 2026-02-06
author: documenter
tags: [design-system, api, input, components, vue3]
related:
  - api-design-system
  - guide-using-components
  - api-components-button
---

# 입력 컴포넌트 API

> Input, Textarea, Select, Checkbox, Radio, Switch, SearchBar, FormField

---

## 📋 개요

입력 컴포넌트는 사용자로부터 데이터를 수집하는 인터페이스를 제공합니다.

| 컴포넌트 | 용도 | v-model |
|---------|------|---------|
| Input | 단일 줄 텍스트 입력 | ✅ |
| Textarea | 여러 줄 텍스트 입력 | ✅ |
| Select | 드롭다운 선택 | ✅ |
| Checkbox | 체크박스 (다중 선택) | ✅ |
| Radio | 라디오 버튼 (단일 선택) | ✅ |
| Switch | 토글 스위치 | ✅ |
| SearchBar | 검색 입력창 | ✅ |
| FormField | 폼 필드 래퍼 (라벨, 오류 표시) | ❌ |

**Button 컴포넌트**는 [components-button.md](./components-button.md)를 참조하세요.

---

## 1️⃣ Input

단일 줄 텍스트 입력 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `modelValue` | `string \| number` | `''` | ❌ | 입력값 (v-model) |
| `type` | `'text' \| 'password' \| 'email' \| 'number' \| 'tel' \| 'url'` | `'text'` | ❌ | 입력 타입 |
| `placeholder` | `string` | `''` | ❌ | 플레이스홀더 텍스트 |
| `label` | `string` | `''` | ❌ | 입력 필드 라벨 |
| `disabled` | `boolean` | `false` | ❌ | 비활성화 상태 |
| `required` | `boolean` | `false` | ❌ | 필수 입력 표시 |
| `error` | `boolean` | `false` | ❌ | 오류 상태 표시 |
| `errorMessage` | `string` | `''` | ❌ | 오류 메시지 |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | ❌ | 입력 필드 크기 |
| `name` | `string` | - | ❌ | HTML name 속성 |
| `id` | `string` | - | ❌ | HTML id 속성 |

### Events

| Event | Payload | Description |
|-------|---------|-------------|
| `update:modelValue` | `string \| number` | 입력값 변경 시 |

### TypeScript Interface

```typescript
interface InputProps {
  modelValue?: string | number
  type?: 'text' | 'password' | 'email' | 'number' | 'tel' | 'url'
  placeholder?: string
  label?: string
  disabled?: boolean
  required?: boolean
  error?: boolean
  errorMessage?: string
  size?: 'sm' | 'md' | 'lg'
  name?: string
  id?: string
}
```

### 사용 예시

```vue
<script setup lang="ts">
import { ref, computed } from 'vue'
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

## 2️⃣ Textarea

여러 줄 텍스트 입력 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `modelValue` | `string \| number` | `''` | ❌ | 입력값 (v-model) |
| `placeholder` | `string` | `''` | ❌ | 플레이스홀더 텍스트 |
| `label` | `string` | `''` | ❌ | 입력 필드 라벨 |
| `rows` | `number` | `5` | ❌ | 표시할 줄 수 |
| `disabled` | `boolean` | `false` | ❌ | 비활성화 상태 |
| `required` | `boolean` | `false` | ❌ | 필수 입력 표시 |
| `error` | `boolean` | `false` | ❌ | 오류 상태 표시 |
| `errorMessage` | `string` | `''` | ❌ | 오류 메시지 |
| `name` | `string` | - | ❌ | HTML name 속성 |
| `id` | `string` | - | ❌ | HTML id 속성 |

### Events

| Event | Payload | Description |
|-------|---------|-------------|
| `update:modelValue` | `string` | 입력값 변경 시 |

### TypeScript Interface

```typescript
interface TextareaProps {
  modelValue?: string | number
  placeholder?: string
  label?: string
  rows?: number
  disabled?: boolean
  required?: boolean
  error?: boolean
  errorMessage?: string
  name?: string
  id?: string
}
```

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
    :rows="5"
  />
</template>
```

---

## 3️⃣ Select

드롭다운 선택 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `modelValue` | `string \| number \| null` | `null` | ❌ | 선택된 값 (v-model) |
| `options` | `SelectOption[]` | `[]` | ✅ | 선택 옵션 목록 |
| `placeholder` | `string` | `'선택하세요'` | ❌ | 플레이스홀더 텍스트 |
| `label` | `string` | - | ❌ | 셀렉트 라벨 |
| `disabled` | `boolean` | `false` | ❌ | 비활성화 상태 |
| `required` | `boolean` | `false` | ❌ | 필수 선택 표시 |
| `error` | `boolean` | `false` | ❌ | 오류 상태 표시 |
| `errorMessage` | `string` | - | ❌ | 오류 메시지 |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | ❌ | 셀렉트 크기 |
| `clearable` | `boolean` | `false` | ❌ | 선택 해제 버튼 표시 |
| `searchable` | `boolean` | `false` | ❌ | 검색 기능 활성화 |
| `name` | `string` | - | ❌ | HTML name 속성 |
| `id` | `string` | - | ❌ | HTML id 속성 |

### Types

```typescript
interface SelectOption {
  label: string
  value: string | number
  disabled?: boolean
}

interface SelectProps {
  modelValue?: string | number | null
  options: SelectOption[]
  placeholder?: string
  label?: string
  disabled?: boolean
  required?: boolean
  error?: boolean
  errorMessage?: string
  size?: 'sm' | 'md' | 'lg'
  clearable?: boolean
  searchable?: boolean
  name?: string
  id?: string
}
```

### Events

| Event | Payload | Description |
|-------|---------|-------------|
| `update:modelValue` | `string \| number \| null` | 선택 변경 시 |
| `change` | `string \| number \| null` | 선택 변경 시 |
| `open` | - | 드롭다운 열림 |
| `close` | - | 드롭다운 닫힘 |
| `search` | `string` | 검색어 입력 시 (searchable=true) |

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

## 4️⃣ Checkbox

체크박스 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `modelValue` | `boolean` | `false` | ❌ | 체크 상태 (v-model) |
| `label` | `string` | - | ❌ | 체크박스 라벨 |
| `disabled` | `boolean` | `false` | ❌ | 비활성화 상태 |
| `error` | `boolean` | `false` | ❌ | 오류 상태 표시 |
| `errorMessage` | `string` | - | ❌ | 오류 메시지 |
| `indeterminate` | `boolean` | `false` | ❌ | 불확정 상태 |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | ❌ | 체크박스 크기 |
| `value` | `string \| number` | - | ❌ | HTML value 속성 |
| `name` | `string` | - | ❌ | HTML name 속성 |
| `id` | `string` | - | ❌ | HTML id 속성 |

### Events

| Event | Payload | Description |
|-------|---------|-------------|
| `update:modelValue` | `boolean` | 체크 상태 변경 시 |
| `change` | `boolean` | 체크 상태 변경 시 |

### TypeScript Interface

```typescript
interface CheckboxProps {
  modelValue?: boolean
  label?: string
  disabled?: boolean
  error?: boolean
  errorMessage?: string
  indeterminate?: boolean
  size?: 'sm' | 'md' | 'lg'
  value?: string | number
  name?: string
  id?: string
}
```

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

## 5️⃣ Radio

라디오 버튼 그룹 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `modelValue` | `string \| number` | - | ❌ | 선택된 값 (v-model) |
| `options` | `RadioOption[]` | `[]` | ✅ | 라디오 옵션 목록 |
| `name` | `string` | - | ✅ | 라디오 그룹 이름 |
| `disabled` | `boolean` | `false` | ❌ | 비활성화 상태 |
| `direction` | `'horizontal' \| 'vertical'` | `'vertical'` | ❌ | 배치 방향 |
| `error` | `boolean` | `false` | ❌ | 오류 상태 표시 |
| `errorMessage` | `string` | - | ❌ | 오류 메시지 |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | ❌ | 라디오 버튼 크기 |

### Types

```typescript
interface RadioOption {
  label: string
  value: string | number
  disabled?: boolean
}

interface RadioProps {
  modelValue?: string | number
  options: RadioOption[]
  name: string
  disabled?: boolean
  direction?: 'horizontal' | 'vertical'
  error?: boolean
  errorMessage?: string
  size?: 'sm' | 'md' | 'lg'
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

## 6️⃣ Switch

토글 스위치 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `modelValue` | `boolean` | `false` | ❌ | 토글 상태 (v-model) |
| `label` | `string` | - | ❌ | 스위치 라벨 |
| `disabled` | `boolean` | `false` | ❌ | 비활성화 상태 |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | ❌ | 스위치 크기 |
| `labelPosition` | `'left' \| 'right'` | `'right'` | ❌ | 라벨 위치 |
| `activeColor` | `string` | - | ❌ | 활성 상태 색상 |
| `name` | `string` | - | ❌ | HTML name 속성 |
| `id` | `string` | - | ❌ | HTML id 속성 |

### Events

| Event | Payload | Description |
|-------|---------|-------------|
| `update:modelValue` | `boolean` | 토글 상태 변경 시 |
| `change` | `boolean` | 토글 상태 변경 시 |

### TypeScript Interface

```typescript
interface SwitchProps {
  modelValue?: boolean
  label?: string
  disabled?: boolean
  size?: 'sm' | 'md' | 'lg'
  labelPosition?: 'left' | 'right'
  activeColor?: string
  name?: string
  id?: string
}
```

### 사용 예시

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { Switch } from '@portal/design-system'

const isDarkMode = ref(false)

const handleToggle = (value: boolean) => {
  console.log('Dark mode:', value)
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

## 7️⃣ SearchBar

검색 입력 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `modelValue` | `string` | - | ✅ | 검색어 (v-model) |
| `placeholder` | `string` | `'검색...'` | ❌ | 플레이스홀더 텍스트 |
| `disabled` | `boolean` | `false` | ❌ | 비활성화 상태 |
| `loading` | `boolean` | `false` | ❌ | 로딩 상태 |
| `autofocus` | `boolean` | `false` | ❌ | 자동 포커스 |

### Events

| Event | Payload | Description |
|-------|---------|-------------|
| `update:modelValue` | `string` | 검색어 변경 시 |
| `search` | `string` | Enter 키 또는 검색 버튼 클릭 시 |
| `clear` | - | 초기화 버튼 클릭 시 |

### TypeScript Interface

```typescript
interface SearchBarProps {
  modelValue: string
  placeholder?: string
  loading?: boolean
  disabled?: boolean
  autofocus?: boolean
}
```

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

## 8️⃣ FormField

폼 필드 래퍼 컴포넌트 - 라벨, 오류 메시지, 도움말 텍스트를 자동으로 처리합니다.

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `label` | `string` | - | ❌ | 필드 라벨 |
| `required` | `boolean` | `false` | ❌ | 필수 입력 표시 |
| `error` | `boolean` | `false` | ❌ | 오류 상태 표시 |
| `errorMessage` | `string` | - | ❌ | 오류 메시지 |
| `helperText` | `string` | - | ❌ | 도움말 텍스트 |
| `id` | `string` | - | ❌ | 필드 ID (자동 생성됨) |
| `disabled` | `boolean` | `false` | ❌ | 비활성화 상태 |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | ❌ | 필드 크기 |

### Slots

| Slot | Description |
|------|-------------|
| `default` | 폼 필드 내용 (Input, Select 등) |
| `label` | 커스텀 라벨 |
| `helper` | 커스텀 도움말 텍스트 |
| `error` | 커스텀 오류 메시지 |

### TypeScript Interface

```typescript
interface FormFieldProps {
  label?: string
  required?: boolean
  error?: boolean
  errorMessage?: string
  helperText?: string
  id?: string
  disabled?: boolean
  size?: 'sm' | 'md' | 'lg'
}
```

### 사용 예시

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { FormField, Input } from '@portal/design-system'

const email = ref('')
const hasError = ref(false)
</script>

<template>
  <!-- 기본 사용 -->
  <FormField
    label="이메일"
    required
    helperText="회사 이메일을 입력하세요"
  >
    <Input v-model="email" type="email" />
  </FormField>

  <!-- 오류 표시 -->
  <FormField
    label="비밀번호"
    required
    :error="hasError"
    errorMessage="비밀번호는 최소 8자 이상이어야 합니다"
  >
    <Input type="password" />
  </FormField>

  <!-- 커스텀 슬롯 -->
  <FormField>
    <template #label>
      <span class="font-bold">사용자 이름 *</span>
    </template>
    <Input />
    <template #helper>
      <span class="text-xs">2-20자 사이로 입력하세요</span>
    </template>
  </FormField>
</template>
```

---

## 🔗 관련 문서

- [버튼 컴포넌트](./components-button.md) - Button
- [피드백 컴포넌트](./components-feedback.md) - Modal, Toast, Badge 등
- [레이아웃 컴포넌트](./components-layout.md) - Card, Container, Stack 등
- [컴포넌트 사용 가이드](../guides/using-components.md)

---

**최종 업데이트**: 2026-02-06
