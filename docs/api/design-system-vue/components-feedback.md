---
id: api-components-feedback
title: 피드백 컴포넌트 API
type: api
status: current
created: 2026-01-18
updated: 2026-01-18
author: documenter
tags: [design-system, api, feedback, components, vue3]
related:
  - api-design-system
  - guide-using-components
---

# 피드백 컴포넌트 API

> Modal, Toast, Badge, Tag, Alert, Spinner, Skeleton

---

## 📋 개요

피드백 컴포넌트는 사용자에게 시스템 상태, 알림, 로딩 상태 등을 전달합니다.

| 컴포넌트 | 용도 | 유형 |
|---------|------|------|
| Modal | 팝업 대화상자 | 오버레이 |
| Toast | 일시적 알림 메시지 | 알림 |
| Badge | 상태 표시 뱃지 | 표시 |
| Tag | 태그/라벨 | 표시 |
| Alert | 인라인 알림 메시지 | 알림 |
| Spinner | 로딩 스피너 | 로딩 |
| Skeleton | 스켈레톤 로더 | 로딩 |

---

## 1️⃣ Modal

팝업 대화상자 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `open` | `boolean` | `false` | ❌ | 모달 표시 상태 (v-model:open) |
| `title` | `string` | - | ❌ | 모달 제목 |
| `size` | `'sm' \| 'md' \| 'lg' \| 'xl' \| 'full'` | `'md'` | ❌ | 모달 크기 |
| `closable` | `boolean` | `true` | ❌ | X 버튼 표시 여부 |
| `closeOnOverlay` | `boolean` | `true` | ❌ | 오버레이 클릭 시 닫기 |
| `closeOnEsc` | `boolean` | `true` | ❌ | ESC 키로 닫기 |
| `persistent` | `boolean` | `false` | ❌ | 닫기 방지 (확인 필수) |

### Events

| Event | Payload | Description |
|-------|---------|-------------|
| `update:open` | `boolean` | 모달 상태 변경 시 |
| `close` | - | 모달 닫힘 시 |
| `opened` | - | 모달 열림 애니메이션 완료 |
| `closed` | - | 모달 닫힘 애니메이션 완료 |

### Slots

| Slot | Description |
|------|-------------|
| `default` | 모달 본문 콘텐츠 |
| `header` | 커스텀 헤더 (title 대체) |
| `footer` | 모달 하단 버튼 영역 |

### TypeScript Interface

```typescript
interface ModalProps {
  open?: boolean
  title?: string
  size?: 'sm' | 'md' | 'lg' | 'xl' | 'full'
  closable?: boolean
  closeOnOverlay?: boolean
  closeOnEsc?: boolean
  persistent?: boolean
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

  <Modal v-model:open="isOpen" title="확인" size="md">
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
  <Modal v-model:open="isOpen" title="삭제 확인" size="sm">
    <Alert type="warning">
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
| `add` | `ToastOptions` | 새 토스트 추가 |
| `remove` | `id: string` | 특정 토스트 제거 |
| `clear` | - | 모든 토스트 제거 |

### ToastOptions

```typescript
interface ToastOptions {
  type: 'success' | 'error' | 'warning' | 'info'
  message: string
  title?: string
  duration?: number  // ms, 기본값 3000
  closable?: boolean
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
    type: 'success',
    message: '저장되었습니다!',
    duration: 3000
  })
}

const showError = () => {
  add({
    type: 'error',
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
    type: 'warning',
    message: '저장되지 않은 변경사항이 있습니다.',
    closable: true
  })
}
</script>

<template>
  <Button @click="showSuccess">성공 토스트</Button>
  <Button @click="showError">오류 토스트</Button>
  <Button @click="showWarning">경고 토스트</Button>
</template>
```

### ToastProvider 설정

```vue
<!-- App.vue (루트 컴포넌트) -->
<script setup lang="ts">
import { ToastProvider } from '@portal/design-system'
</script>

<template>
  <ToastProvider position="top-right" :max="5">
    <router-view />
  </ToastProvider>
</template>
```

---

## 3️⃣ Badge

상태 표시 뱃지 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `variant` | `'default' \| 'primary' \| 'success' \| 'warning' \| 'error' \| 'info'` | `'default'` | ❌ | 뱃지 색상 변형 |
| `size` | `'xs' \| 'sm' \| 'md'` | `'md'` | ❌ | 뱃지 크기 |
| `rounded` | `boolean` | `false` | ❌ | 완전 둥근 모양 |
| `outline` | `boolean` | `false` | ❌ | 외곽선 스타일 |
| `dot` | `boolean` | `false` | ❌ | 점 표시 (내용 없음) |

### Slots

| Slot | Description |
|------|-------------|
| `default` | 뱃지 내용 |

### TypeScript Interface

```typescript
interface BadgeProps {
  variant?: 'default' | 'primary' | 'success' | 'warning' | 'error' | 'info'
  size?: 'xs' | 'sm' | 'md'
  rounded?: boolean
  outline?: boolean
  dot?: boolean
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
  <Badge variant="error">실패</Badge>
  <Badge variant="info">진행 중</Badge>

  <!-- 크기 변형 -->
  <Badge size="xs">XS</Badge>
  <Badge size="sm">SM</Badge>
  <Badge size="md">MD</Badge>

  <!-- 외곽선 스타일 -->
  <Badge variant="primary" outline>Outline</Badge>

  <!-- 둥근 모양 -->
  <Badge variant="success" rounded>99+</Badge>

  <!-- 점 표시 -->
  <span class="relative">
    알림
    <Badge variant="error" dot class="absolute -top-1 -right-1" />
  </span>
</template>
```

---

## 4️⃣ Tag

태그/라벨 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `color` | `string` | - | ❌ | 커스텀 배경색 |
| `closable` | `boolean` | `false` | ❌ | X 버튼 표시 |
| `size` | `'sm' \| 'md'` | `'md'` | ❌ | 태그 크기 |
| `variant` | `'filled' \| 'outline'` | `'filled'` | ❌ | 태그 스타일 |

### Events

| Event | Payload | Description |
|-------|---------|-------------|
| `close` | - | 닫기 버튼 클릭 시 |
| `click` | `MouseEvent` | 태그 클릭 시 |

### Slots

| Slot | Description |
|------|-------------|
| `default` | 태그 내용 |

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
      closable
      @close="removeTag(tag)"
    >
      #{{ tag }}
    </Tag>
  </div>

  <!-- 커스텀 색상 -->
  <Tag color="#20C997">커스텀 색상</Tag>

  <!-- 외곽선 스타일 -->
  <Tag variant="outline">Outline Tag</Tag>
</template>
```

---

## 5️⃣ Alert

인라인 알림 메시지 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `type` | `'info' \| 'success' \| 'warning' \| 'error'` | `'info'` | ❌ | 알림 타입 |
| `title` | `string` | - | ❌ | 알림 제목 |
| `closable` | `boolean` | `false` | ❌ | 닫기 버튼 표시 |
| `showIcon` | `boolean` | `true` | ❌ | 아이콘 표시 |
| `bordered` | `boolean` | `true` | ❌ | 테두리 표시 |

### Events

| Event | Payload | Description |
|-------|---------|-------------|
| `close` | - | 닫기 버튼 클릭 시 |

### Slots

| Slot | Description |
|------|-------------|
| `default` | 알림 메시지 내용 |
| `action` | 액션 버튼 영역 |

### 사용 예시

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { Alert, Button } from '@portal/design-system'

const showAlert = ref(true)
</script>

<template>
  <!-- 기본 알림 -->
  <Alert type="info">
    중요한 정보를 확인하세요.
  </Alert>

  <!-- 제목이 있는 알림 -->
  <Alert type="warning" title="주의">
    이 작업은 되돌릴 수 없습니다.
  </Alert>

  <!-- 닫을 수 있는 알림 -->
  <Alert
    v-if="showAlert"
    type="success"
    closable
    @close="showAlert = false"
  >
    성공적으로 저장되었습니다!
  </Alert>

  <!-- 액션이 있는 알림 -->
  <Alert type="error" title="오류 발생">
    네트워크 오류가 발생했습니다.
    <template #action>
      <Button size="sm" variant="outline">재시도</Button>
    </template>
  </Alert>
</template>
```

---

## 6️⃣ Spinner

로딩 스피너 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `size` | `'xs' \| 'sm' \| 'md' \| 'lg' \| 'xl'` | `'md'` | ❌ | 스피너 크기 |
| `color` | `string` | `'brand-primary'` | ❌ | 스피너 색상 |
| `label` | `string` | - | ❌ | 스크린 리더용 라벨 |

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

  <!-- 버튼 내 스피너 -->
  <Button :disabled="isLoading" @click="handleClick">
    <Spinner v-if="isLoading" size="sm" class="mr-2" />
    {{ isLoading ? '로딩 중...' : '저장' }}
  </Button>

  <!-- 전체 화면 로딩 -->
  <div v-if="isLoading" class="fixed inset-0 flex items-center justify-center bg-black/50">
    <Spinner size="xl" color="white" label="로딩 중..." />
  </div>
</template>
```

---

## 7️⃣ Skeleton

스켈레톤 로더 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `width` | `string` | `'100%'` | ❌ | 스켈레톤 너비 |
| `height` | `string` | `'20px'` | ❌ | 스켈레톤 높이 |
| `count` | `number` | `1` | ❌ | 반복 횟수 |
| `variant` | `'text' \| 'circular' \| 'rectangular'` | `'text'` | ❌ | 스켈레톤 모양 |
| `animation` | `'pulse' \| 'wave' \| 'none'` | `'pulse'` | ❌ | 애니메이션 타입 |

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

      <!-- 텍스트 스켈레톤 -->
      <Skeleton count="3" height="16px" class="mb-2" />

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
      <Skeleton count="2" height="14px" />
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

**최종 업데이트**: 2026-01-18
