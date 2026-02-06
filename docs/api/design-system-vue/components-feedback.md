---
id: api-components-feedback
title: 피드백 컴포넌트 API
type: api
status: current
created: 2026-01-18
updated: 2026-02-06
author: Laze
tags: [design-system, api, feedback, components, vue3]
related:
  - api-design-system
  - guide-using-components
  - api-composables
---

# 피드백 컴포넌트 API

> Modal, Toast, ToastContainer, Alert, Badge, Tag, Spinner, Skeleton

---

## 📋 개요

피드백 컴포넌트는 사용자에게 시스템 상태, 알림, 로딩 상태 등을 전달합니다.

| 컴포넌트 | 용도 | 유형 |
|---------|------|------|
| Modal | 팝업 대화상자 | 오버레이 |
| Toast | 일시적 알림 메시지 | 알림 |
| ToastContainer | Toast 컨테이너 | 알림 |
| Alert | 인라인 알림 메시지 | 알림 |
| Badge | 상태 표시 뱃지 | 표시 |
| Tag | 태그/라벨 | 표시 |
| Spinner | 로딩 스피너 | 로딩 |
| Skeleton | 스켈레톤 로더 | 로딩 |

---

## 1️⃣ Modal

팝업 대화상자 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `modelValue` | `boolean` | `false` | ❌ | 모달 표시 상태 (v-model) |
| `title` | `string` | - | ❌ | 모달 제목 |
| `size` | `'sm' \| 'md' \| 'lg' \| 'xl'` | `'md'` | ❌ | 모달 크기 |
| `showClose` | `boolean` | `true` | ❌ | X 버튼 표시 여부 |
| `closeOnBackdrop` | `boolean` | `true` | ❌ | 백드롭 클릭 시 닫기 |
| `closeOnEscape` | `boolean` | `true` | ❌ | ESC 키로 닫기 |

### Events

| Event | Payload | Description |
|-------|---------|-------------|
| `update:modelValue` | `boolean` | 모달 상태 변경 시 |
| `close` | - | 모달 닫힘 시 |

### Slots

| Slot | Description |
|------|-------------|
| `default` | 모달 본문 콘텐츠 |
| `footer` | 모달 하단 버튼 영역 |

### TypeScript Interface

```typescript
interface ModalProps {
  modelValue?: boolean
  title?: string
  size?: 'sm' | 'md' | 'lg' | 'xl'
  showClose?: boolean
  closeOnBackdrop?: boolean
  closeOnEscape?: boolean
}
```

### 사용 예시

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { Modal, Button } from '@portal/design-system'

const isOpen = ref(false)

const handleConfirm = () => {
  console.log('확인 클릭')
  isOpen.value = false
}
</script>

<template>
  <Button @click="isOpen = true">모달 열기</Button>

  <Modal v-model="isOpen" title="확인" size="md">
    <p>정말로 이 작업을 수행하시겠습니까?</p>

    <template #footer>
      <Button variant="secondary" @click="isOpen = false">
        취소
      </Button>
      <Button variant="primary" @click="handleConfirm">
        확인
      </Button>
    </template>
  </Modal>
</template>
```

### 위험 모달 예시

```vue
<template>
  <Modal v-model="isOpen" title="삭제 확인" size="sm">
    <Alert variant="warning">
      이 작업은 되돌릴 수 없습니다.
    </Alert>
    <p class="mt-4">정말로 삭제하시겠습니까?</p>

    <template #footer>
      <Button variant="secondary" @click="isOpen = false">취소</Button>
      <Button variant="danger" @click="handleDelete">삭제</Button>
    </template>
  </Modal>
</template>
```

---

## 2️⃣ Toast

일시적 알림 메시지 (Composable 기반)

### useToast API

```typescript
import { useToast } from '@portal/design-system'

const { add, remove, clear } = useToast()
```

### Methods

| Method | Parameters | Description |
|--------|------------|-------------|
| `add` | `Omit<ToastItem, 'id'>` | 새 토스트 추가 |
| `remove` | `id: string` | 특정 토스트 제거 |
| `clear` | - | 모든 토스트 제거 |

### ToastItem Interface

```typescript
interface ToastItem {
  id: string
  variant?: 'info' | 'success' | 'warning' | 'error'
  title?: string
  message: string
  duration?: number  // ms, 기본값 5000
  dismissible?: boolean
  action?: {
    label: string
    onClick: () => void
  }
}
```

### 사용 예시

```vue
<script setup lang="ts">
import { useToast } from '@portal/design-system'

const { add } = useToast()

const showSuccess = () => {
  add({
    variant: 'success',
    message: '저장되었습니다!',
    duration: 3000
  })
}

const showError = () => {
  add({
    variant: 'error',
    title: '오류 발생',
    message: '저장 중 오류가 발생했습니다.',
    duration: 5000,
    action: {
      label: '재시도',
      onClick: () => retryOperation()
    }
  })
}

const showWarning = () => {
  add({
    variant: 'warning',
    message: '저장되지 않은 변경사항이 있습니다.',
    dismissible: true
  })
}
</script>

<template>
  <Button @click="showSuccess">성공 토스트</Button>
  <Button @click="showError">오류 토스트</Button>
  <Button @click="showWarning">경고 토스트</Button>
</template>
```

> **참고**: ToastProvider 설정은 [Composables API 문서](./composables.md#usetoast)를 참조하세요.

---

## 3️⃣ ToastContainer

Toast를 화면에 표시하는 컨테이너 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `position` | `ToastPosition` | `'top-right'` | ❌ | 토스트 표시 위치 |
| `maxToasts` | `number` | `5` | ❌ | 최대 토스트 개수 |

### ToastPosition Type

```typescript
type ToastPosition =
  | 'top-right'
  | 'top-left'
  | 'top-center'
  | 'bottom-right'
  | 'bottom-left'
  | 'bottom-center'
```

### TypeScript Interface

```typescript
interface ToastContainerProps {
  position?: ToastPosition
  maxToasts?: number
}
```

### 사용 예시

```vue
<!-- App.vue (루트 컴포넌트) -->
<script setup lang="ts">
import { ToastContainer } from '@portal/design-system'
</script>

<template>
  <ToastContainer position="top-right" :maxToasts="5" />
  <router-view />
</template>
```

---

## 4️⃣ Alert

인라인 알림 메시지 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `variant` | `'info' \| 'success' \| 'warning' \| 'error'` | `'info'` | ❌ | 알림 타입 |
| `title` | `string` | - | ❌ | 알림 제목 |
| `dismissible` | `boolean` | `false` | ❌ | 닫기 버튼 표시 |
| `showIcon` | `boolean` | `true` | ❌ | 아이콘 표시 |
| `bordered` | `boolean` | `false` | ❌ | 테두리 표시 |

### Events

| Event | Payload | Description |
|-------|---------|-------------|
| `dismiss` | - | 닫기 버튼 클릭 시 |

### Slots

| Slot | Description |
|------|-------------|
| `default` | 알림 메시지 내용 |
| `icon` | 커스텀 아이콘 |
| `action` | 액션 버튼 영역 |

### TypeScript Interface

```typescript
interface AlertProps {
  variant?: 'info' | 'success' | 'warning' | 'error'
  title?: string
  dismissible?: boolean
  showIcon?: boolean
  bordered?: boolean
}
```

### 사용 예시

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { Alert, Button } from '@portal/design-system'

const showAlert = ref(true)
</script>

<template>
  <!-- 기본 알림 -->
  <Alert variant="info">
    중요한 정보를 확인하세요.
  </Alert>

  <!-- 제목이 있는 알림 -->
  <Alert variant="warning" title="주의">
    이 작업은 되돌릴 수 없습니다.
  </Alert>

  <!-- 닫을 수 있는 알림 -->
  <Alert
    v-if="showAlert"
    variant="success"
    dismissible
    @dismiss="showAlert = false"
  >
    성공적으로 저장되었습니다!
  </Alert>

  <!-- 액션이 있는 알림 -->
  <Alert variant="error" title="오류 발생">
    네트워크 오류가 발생했습니다.
    <template #action>
      <Button size="sm" variant="outline">재시도</Button>
    </template>
  </Alert>
</template>
```

---

## 5️⃣ Badge

상태 표시 뱃지 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `variant` | `BadgeVariant` | `'default'` | ❌ | 뱃지 색상 변형 |
| `size` | `'xs' \| 'sm' \| 'md' \| 'lg'` | `'md'` | ❌ | 뱃지 크기 |

### BadgeVariant Type

```typescript
type BadgeVariant =
  | 'default'
  | 'primary'
  | 'success'
  | 'warning'
  | 'danger'
  | 'info'
  | 'outline'
```

### Slots

| Slot | Description |
|------|-------------|
| `default` | 뱃지 내용 |

### TypeScript Interface

```typescript
interface BadgeProps {
  variant?: BadgeVariant
  size?: 'xs' | 'sm' | 'md' | 'lg'
}
```

### 사용 예시

```vue
<script setup lang="ts">
import { Badge } from '@portal/design-system'
</script>

<template>
  <!-- 상태 뱃지 -->
  <Badge variant="success">완료</Badge>
  <Badge variant="warning">대기 중</Badge>
  <Badge variant="danger">실패</Badge>
  <Badge variant="info">진행 중</Badge>

  <!-- 크기 변형 -->
  <Badge size="xs">XS</Badge>
  <Badge size="sm">SM</Badge>
  <Badge size="md">MD</Badge>
  <Badge size="lg">LG</Badge>

  <!-- 외곽선 스타일 -->
  <Badge variant="outline">Outline</Badge>
</template>
```

---

## 6️⃣ Tag

태그/라벨 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `variant` | `TagVariant` | `'default'` | ❌ | 태그 스타일 변형 |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | ❌ | 태그 크기 |
| `removable` | `boolean` | `false` | ❌ | 제거 버튼 표시 |
| `clickable` | `boolean` | `false` | ❌ | 클릭 가능 여부 |

### TagVariant Type

```typescript
type TagVariant =
  | 'default'
  | 'primary'
  | 'success'
  | 'error'
  | 'warning'
  | 'info'
```

### Events

| Event | Payload | Description |
|-------|---------|-------------|
| `click` | - | 태그 클릭 시 (clickable일 때) |
| `remove` | - | 제거 버튼 클릭 시 |

### Slots

| Slot | Description |
|------|-------------|
| `default` | 태그 내용 |

### TypeScript Interface

```typescript
interface TagProps {
  variant?: TagVariant
  size?: 'sm' | 'md' | 'lg'
  removable?: boolean
  clickable?: boolean
}
```

### 사용 예시

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { Tag } from '@portal/design-system'

const tags = ref(['Vue', 'TypeScript', 'Design System'])

const removeTag = (tag: string) => {
  tags.value = tags.value.filter(t => t !== tag)
}
</script>

<template>
  <div class="flex gap-2">
    <Tag
      v-for="tag in tags"
      :key="tag"
      removable
      @remove="removeTag(tag)"
    >
      #{{ tag }}
    </Tag>
  </div>

  <!-- 클릭 가능한 태그 -->
  <Tag variant="primary" clickable @click="handleTagClick">
    클릭 가능
  </Tag>

  <!-- 다양한 변형 -->
  <Tag variant="success">성공</Tag>
  <Tag variant="warning">경고</Tag>
  <Tag variant="error">오류</Tag>
</template>
```

---

## 7️⃣ Spinner

로딩 스피너 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `size` | `'xs' \| 'sm' \| 'md' \| 'lg' \| 'xl'` | `'md'` | ❌ | 스피너 크기 |
| `color` | `'primary' \| 'current' \| 'white'` | `'primary'` | ❌ | 스피너 색상 |
| `label` | `string` | `'Loading'` | ❌ | 스크린 리더용 라벨 |

### TypeScript Interface

```typescript
interface SpinnerProps {
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl'
  color?: 'primary' | 'current' | 'white'
  label?: string
}
```

### 사용 예시

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { Spinner, Button } from '@portal/design-system'

const isLoading = ref(false)

const handleClick = async () => {
  isLoading.value = true
  await someAsyncOperation()
  isLoading.value = false
}
</script>

<template>
  <!-- 기본 스피너 -->
  <Spinner />

  <!-- 크기 변형 -->
  <Spinner size="xs" />
  <Spinner size="sm" />
  <Spinner size="md" />
  <Spinner size="lg" />
  <Spinner size="xl" />

  <!-- 색상 변형 -->
  <Spinner color="primary" />
  <Spinner color="current" />
  <Spinner color="white" />

  <!-- 버튼 내 스피너 -->
  <Button :disabled="isLoading" @click="handleClick">
    <Spinner v-if="isLoading" size="sm" color="current" class="mr-2" />
    {{ isLoading ? '로딩 중...' : '저장' }}
  </Button>

  <!-- 전체 화면 로딩 -->
  <div v-if="isLoading" class="fixed inset-0 flex items-center justify-center bg-black/50">
    <Spinner size="xl" color="white" label="로딩 중..." />
  </div>
</template>
```

---

## 8️⃣ Skeleton

스켈레톤 로더 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `variant` | `'text' \| 'circular' \| 'rectangular' \| 'rounded'` | `'text'` | ❌ | 스켈레톤 모양 |
| `width` | `string` | - | ❌ | 스켈레톤 너비 |
| `height` | `string` | - | ❌ | 스켈레톤 높이 |
| `animation` | `'pulse' \| 'wave' \| 'none'` | `'pulse'` | ❌ | 애니메이션 타입 |
| `lines` | `number` | `1` | ❌ | 텍스트 라인 수 (variant='text'일 때) |

### TypeScript Interface

```typescript
interface SkeletonProps {
  variant?: 'text' | 'circular' | 'rectangular' | 'rounded'
  width?: string
  height?: string
  animation?: 'pulse' | 'wave' | 'none'
  lines?: number
}
```

### 사용 예시

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { Skeleton, Card } from '@portal/design-system'

const isLoading = ref(true)
const data = ref(null)
</script>

<template>
  <Card>
    <!-- 로딩 상태 -->
    <template v-if="isLoading">
      <!-- 아바타 스켈레톤 -->
      <div class="flex items-center gap-4 mb-4">
        <Skeleton variant="circular" width="48px" height="48px" />
        <div class="flex-1">
          <Skeleton width="120px" height="16px" />
          <Skeleton width="80px" height="12px" class="mt-2" />
        </div>
      </div>

      <!-- 텍스트 스켈레톤 (여러 줄) -->
      <Skeleton variant="text" :lines="3" height="16px" class="mb-2" />

      <!-- 이미지 스켈레톤 -->
      <Skeleton variant="rectangular" width="100%" height="200px" />
    </template>

    <!-- 실제 콘텐츠 -->
    <template v-else>
      <!-- 데이터 표시 -->
    </template>
  </Card>
</template>
```

### 카드 로딩 스켈레톤 패턴

```vue
<template>
  <div class="grid grid-cols-3 gap-4">
    <Card v-for="i in 3" :key="i">
      <Skeleton variant="rectangular" height="150px" class="mb-4" />
      <Skeleton width="70%" height="20px" class="mb-2" />
      <Skeleton variant="text" :lines="2" height="14px" />
    </Card>
  </div>
</template>
```

---

## 🔗 관련 문서

- [입력 컴포넌트](./components-input.md) - Button, Input, Select 등
- [레이아웃 컴포넌트](./components-layout.md) - Card, Container, Stack 등
- [Composables API](./composables.md) - useToast 상세

---

**최종 업데이트**: 2026-02-06
