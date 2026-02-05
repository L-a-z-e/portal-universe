---
id: design-system-vue-usage
title: Design System 사용 가이드
type: guide
status: current
created: 2026-01-19
updated: 2026-01-30
author: Portal Universe Team
tags: [design-system-vue, usage, integration, vue]
---

# Usage Guide

Design System을 프로젝트에 통합하고 효과적으로 사용하는 방법을 설명합니다.

## 목차

- [설치 및 설정](#설치-및-설정)
- [기본 사용법](#기본-사용법)
- [컴포넌트 통합](#컴포넌트-통합)
- [토큰 활용](#토큰-활용)
- [테마 관리](#테마-관리)
- [모범 사례](#모범-사례)
- [성능 최적화](#성능-최적화)
- [문제 해결](#문제-해결)

## 설치 및 설정

### 1. 패키지 설치

```bash
npm install @portal/design-system
```

### 2. CSS 임포트

`main.ts` 또는 `main.js`에서:

```typescript
import { createApp } from 'vue'
import App from './App.vue'
import '@portal/design-system/style.css'

const app = createApp(App)
app.mount('#app')
```

### 3. Tailwind CSS 설정 (선택사항)

프로젝트에서 Tailwind를 사용하는 경우:

```javascript
// tailwind.config.js
import designSystemPreset from '@portal/design-system/tailwind.preset.js'

export default {
  presets: [designSystemPreset],
  content: [
    './index.html',
    './src/**/*.{vue,js,ts,jsx,tsx}'
  ]
}
```

### 4. 글로벌 레이아웃 설정

`App.vue`에서 테마 컨텍스트 설정:

```vue
<template>
  <div :data-service="currentService" :data-theme="currentTheme">
    <RouterView />
  </div>
</template>

<script setup lang="ts">
import { useTheme } from '@portal/design-system'
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const { currentService, currentTheme, initTheme, setService } = useTheme()

onMounted(() => {
  // 테마 초기화 (저장된 설정 또는 시스템 설정)
  initTheme()

  // 라우트에 따라 서비스 자동 설정
  router.afterEach((to) => {
    if (to.matched.some(record => record.path.includes('blog'))) {
      setService('blog')
    } else if (to.matched.some(record => record.path.includes('shopping'))) {
      setService('shopping')
    }
  })
})
</script>
```

## 기본 사용법

### 컴포넌트 Import

```typescript
// 개별 import (권장)
import { Button, Card, Modal } from '@portal/design-system'

// 또는 전체 import
import * as DesignSystem from '@portal/design-system'
```

### 간단한 버튼 예제

```vue
<template>
  <Button @click="handleClick" variant="primary" size="md">
    클릭하기
  </Button>
</template>

<script setup lang="ts">
import { Button } from '@portal/design-system'

const handleClick = () => {
  console.log('Button clicked')
}
</script>
```

## 컴포넌트 통합

### 폼 구축

완전한 폼 예제:

```vue
<template>
  <Card class="w-full max-w-md">
    <CardHeader>
      <CardTitle>사용자 등록</CardTitle>
      <CardDescription>새 계정을 만들어주세요</CardDescription>
    </CardHeader>

    <CardContent>
      <form @submit.prevent="handleSubmit" class="space-y-4">
        <!-- 이름 입력 -->
        <FormField>
          <label class="block text-sm font-medium mb-2">이름</label>
          <Input
            v-model="form.name"
            placeholder="이름을 입력하세요"
            :error="errors.name !== undefined"
            :helper-text="errors.name"
          />
        </FormField>

        <!-- 이메일 입력 -->
        <FormField>
          <label class="block text-sm font-medium mb-2">이메일</label>
          <Input
            v-model="form.email"
            type="email"
            placeholder="이메일@example.com"
            :error="errors.email !== undefined"
            :helper-text="errors.email"
          />
        </FormField>

        <!-- 비밀번호 입력 -->
        <FormField>
          <label class="block text-sm font-medium mb-2">비밀번호</label>
          <Input
            v-model="form.password"
            type="password"
            placeholder="비밀번호를 입력하세요"
            :error="errors.password !== undefined"
            :helper-text="errors.password"
          />
        </FormField>

        <!-- 약관 동의 -->
        <Checkbox
          v-model="form.agree"
          label="이용약관에 동의합니다"
        />

        <!-- 제출 버튼 -->
        <Button
          type="submit"
          class="w-full"
          :loading="isSubmitting"
        >
          가입하기
        </Button>
      </form>
    </CardContent>
  </Card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
  Input,
  Button,
  Checkbox,
  FormField
} from '@portal/design-system'

const form = ref({
  name: '',
  email: '',
  password: '',
  agree: false
})

const errors = ref<Record<string, string>>({})
const isSubmitting = ref(false)

const validateForm = () => {
  errors.value = {}

  if (!form.value.name) {
    errors.value.name = '이름은 필수입니다'
  }

  if (!form.value.email) {
    errors.value.email = '이메일은 필수입니다'
  } else if (!form.value.email.includes('@')) {
    errors.value.email = '유효한 이메일을 입력하세요'
  }

  if (!form.value.password) {
    errors.value.password = '비밀번호는 필수입니다'
  } else if (form.value.password.length < 8) {
    errors.value.password = '비밀번호는 최소 8자 이상이어야 합니다'
  }

  if (!form.value.agree) {
    errors.value.agree = '약관에 동의해야 합니다'
  }

  return Object.keys(errors.value).length === 0
}

const handleSubmit = async () => {
  if (!validateForm()) return

  isSubmitting.value = true

  try {
    // API 호출
    await new Promise(resolve => setTimeout(resolve, 1000))
    console.log('Form submitted:', form.value)
  } finally {
    isSubmitting.value = false
  }
}
</script>
```

### 모달 구현

```vue
<template>
  <div>
    <Button @click="isOpen = true">
      모달 열기
    </Button>

    <Modal
      v-model="isOpen"
      title="작업 확인"
      size="md"
      @update:modelValue="handleModalClose"
    >
      <div class="space-y-4">
        <Alert type="warning">
          이 작업은 되돌릴 수 없습니다. 정말 진행하시겠습니까?
        </Alert>

        <div class="bg-gray-100 p-4 rounded">
          <p class="text-sm">{{ itemToDelete?.name }}</p>
        </div>
      </div>

      <template #footer>
        <Button
          variant="secondary"
          @click="isOpen = false"
        >
          취소
        </Button>
        <Button
          variant="danger"
          @click="handleConfirm"
          :loading="isDeleting"
        >
          삭제
        </Button>
      </template>
    </Modal>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Button, Modal, Alert } from '@portal/design-system'

const isOpen = ref(false)
const isDeleting = ref(false)
const itemToDelete = ref({ name: '삭제할 항목' })

const handleModalClose = (value: boolean) => {
  isOpen.value = value
}

const handleConfirm = async () => {
  isDeleting.value = true

  try {
    // API 호출
    await new Promise(resolve => setTimeout(resolve, 1000))
    console.log('Item deleted')
    isOpen.value = false
  } finally {
    isDeleting.value = false
  }
}
</script>
```

### 데이터 테이블 레이아웃

```vue
<template>
  <div class="space-y-4">
    <!-- 헤더 -->
    <Stack direction="row" justify="between" align="center">
      <h2 class="text-2xl font-bold">상품 목록</h2>
      <Button @click="handleAddNew">
        새 상품 추가
      </Button>
    </Stack>

    <!-- 테이블 -->
    <Card>
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead class="border-b border-border-default">
            <tr>
              <th class="text-left px-4 py-3">상품명</th>
              <th class="text-right px-4 py-3">가격</th>
              <th class="text-right px-4 py-3">재고</th>
              <th class="text-center px-4 py-3">작업</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="product in products" :key="product.id" class="border-b">
              <td class="px-4 py-3">{{ product.name }}</td>
              <td class="text-right px-4 py-3">{{ formatPrice(product.price) }}</td>
              <td class="text-right px-4 py-3">
                <Badge :variant="product.stock > 10 ? 'success' : 'warning'">
                  {{ product.stock }}개
                </Badge>
              </td>
              <td class="text-center px-4 py-3">
                <Button size="sm" variant="ghost" @click="handleEdit(product)">
                  편집
                </Button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Card, Button, Badge, Stack } from '@portal/design-system'

interface Product {
  id: number
  name: string
  price: number
  stock: number
}

const products = ref<Product[]>([
  { id: 1, name: '상품 1', price: 10000, stock: 5 },
  { id: 2, name: '상품 2', price: 20000, stock: 15 }
])

const formatPrice = (price: number) => {
  return new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: 'KRW'
  }).format(price)
}

const handleAddNew = () => {
  console.log('Add new product')
}

const handleEdit = (product: Product) => {
  console.log('Edit product:', product)
}
</script>
```

## 토큰 활용

### CSS 클래스를 통한 토큰 사용

```vue
<template>
  <div class="bg-bg-card border border-border-default rounded-lg p-6">
    <h2 class="text-2xl font-bold text-text-heading">제목</h2>
    <p class="text-text-body mt-2">본문 텍스트</p>
    <Button class="mt-4 bg-brand-primary hover:bg-brand-primary-hover">
      액션
    </Button>
  </div>
</template>

<script setup lang="ts">
import { Button } from '@portal/design-system'
</script>
```

### CSS 변수를 통한 토큰 사용

```vue
<template>
  <div :style="containerStyle">
    <h2 :style="titleStyle">커스텀 스타일</h2>
    <p :style="textStyle">본문 텍스트</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const containerStyle = computed(() => ({
  backgroundColor: 'var(--color-bg-card)',
  borderColor: 'var(--color-border-default)',
  borderWidth: '1px',
  borderRadius: 'var(--borderRadius-lg)',
  padding: 'var(--spacing-6)'
}))

const titleStyle = computed(() => ({
  fontSize: 'var(--font-size-2xl)',
  fontWeight: 'var(--font-weight-bold)',
  color: 'var(--color-text-heading)'
}))

const textStyle = computed(() => ({
  fontSize: 'var(--font-size-base)',
  color: 'var(--color-text-body)',
  lineHeight: 'var(--line-height-normal)'
}))
</script>
```

## 테마 관리

### 서비스별 테마 전환

```vue
<template>
  <div class="flex gap-4">
    <Button
      v-for="service in services"
      :key="service"
      :variant="currentService === service ? 'primary' : 'secondary'"
      @click="setService(service)"
    >
      {{ service }}
    </Button>
  </div>
</template>

<script setup lang="ts">
import { Button } from '@portal/design-system'
import { useTheme } from '@portal/design-system'

const { currentService, setService } = useTheme()

const services = ['portal', 'blog', 'shopping']
</script>
```

### 다크 모드 토글

```vue
<template>
  <Button
    :variant="currentTheme === 'dark' ? 'primary' : 'secondary'"
    @click="toggleTheme"
  >
    {{ currentTheme === 'dark' ? '☀️ Light' : '🌙 Dark' }}
  </Button>
</template>

<script setup lang="ts">
import { Button } from '@portal/design-system'
import { useTheme } from '@portal/design-system'

const { currentTheme, toggleTheme } = useTheme()
</script>
```

## 모범 사례

### 1. 컴포넌트 조합 (Composition)

```vue
<!-- 좋은 예: 재사용 가능한 컴포넌트 조합 -->
<template>
  <Card>
    <CardHeader>
      <CardTitle>{{ title }}</CardTitle>
    </CardHeader>
    <CardContent>
      <slot />
    </CardContent>
  </Card>
</template>

<script setup lang="ts">
import { Card, CardHeader, CardTitle, CardContent } from '@portal/design-system'

defineProps<{
  title: string
}>()
</script>
```

### 2. 타입 안전성

```vue
<!-- TypeScript를 활용한 타입 안전한 Props -->
<script setup lang="ts">
import type { ButtonProps } from '@portal/design-system'
import { Button } from '@portal/design-system'

const props = withDefaults(defineProps<{
  variant?: ButtonProps['variant']
  size?: ButtonProps['size']
  disabled?: boolean
}>(), {
  variant: 'primary',
  size: 'md'
})
</script>
```

### 3. 반응형 디자인

```vue
<template>
  <!-- 반응형 레이아웃 -->
  <Container max-width="lg" padding="md">
    <Stack
      :direction="isMobile ? 'column' : 'row'"
      gap="lg"
      align="start"
    >
      <div class="flex-1">콘텐츠</div>
      <aside class="w-full md:w-64">사이드바</aside>
    </Stack>
  </Container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useWindowSize } from '@vueuse/core'
import { Container, Stack } from '@portal/design-system'

const { width } = useWindowSize()
const isMobile = computed(() => width.value < 768)
</script>
```

## 성능 최적화

### 1. 지연 로딩 (Lazy Loading)

```typescript
import { defineAsyncComponent } from 'vue'

const HeavyModal = defineAsyncComponent(() =>
  import('./HeavyModal.vue')
)
```

### 2. 메모이제이션

```vue
<template>
  <div v-for="item in memoizedItems" :key="item.id">
    {{ item.name }}
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const items = ref([...])

const memoizedItems = computed(() => {
  return items.value.sort((a, b) => a.name.localeCompare(b.name))
})
</script>
```

### 3. 가상 스크롤 (Virtual Scrolling)

큰 리스트의 경우 가상 스크롤을 사용합니다:

```vue
<template>
  <VirtualScroller :items="largeList" :item-size="50">
    <template #default="{ item }">
      <Card>{{ item.name }}</Card>
    </template>
  </VirtualScroller>
</template>
```

## 문제 해결

### 스타일이 적용되지 않음

1. CSS 임포트 확인
2. Tailwind 설정 확인
3. 브라우저 캐시 삭제

```bash
# 캐시 삭제 및 빌드
rm -rf node_modules/.vite
npm run dev
```

### 타입 오류

1. TypeScript 버전 확인 (`~5.9.3`)
2. `tsconfig.json` 확인

```json
{
  "compilerOptions": {
    "strict": true,
    "moduleResolution": "bundler"
  }
}
```

### 테마가 적용되지 않음

1. `data-service` 속성 확인
2. `data-theme` 속성 확인
3. 글로벌 스타일 로드 확인

```vue
<!-- 올바른 구조 -->
<div :data-service="currentService" :data-theme="currentTheme">
  <!-- 콘텐츠 -->
</div>
```

## 통합 체크리스트

- [ ] Design System 패키지 설치
- [ ] CSS 임포트 추가
- [ ] Tailwind 설정 (필요한 경우)
- [ ] App.vue에 테마 컨텍스트 설정
- [ ] 컴포넌트 임포트 확인
- [ ] 라우트별 서비스 설정
- [ ] 다크 모드 UI 구현
- [ ] 모바일 반응형 테스트
- [ ] 브라우저 호환성 테스트

## 다음 단계

- [COMPONENTS.md](./COMPONENTS.md) - 모든 컴포넌트 상세
- [TOKENS.md](./TOKENS.md) - 토큰 시스템 이해
- [THEMING.md](./THEMING.md) - 고급 테마 커스터마이징
- [Storybook](http://localhost:6006) - 인터랙티브 예제