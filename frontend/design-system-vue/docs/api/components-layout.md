---
id: api-components-layout
title: 레이아웃 컴포넌트 API
type: api
status: current
created: 2026-01-18
updated: 2026-01-18
author: documenter
tags: [design-system, api, layout, components, vue3]
related:
  - api-design-system
  - guide-using-components
---

# 레이아웃 컴포넌트 API

> Card, Container, Stack, Divider, FormField, Breadcrumb

---

## 📋 개요

레이아웃 컴포넌트는 UI 요소의 배치와 구조를 정의합니다.

| 컴포넌트 | 용도 | 유형 |
|---------|------|------|
| Card | 콘텐츠 카드 | 컨테이너 |
| Container | 페이지 래퍼 | 컨테이너 |
| Stack | 플렉스 레이아웃 | 레이아웃 |
| Divider | 구분선 | 유틸리티 |
| FormField | 폼 필드 래퍼 | 폼 |
| Breadcrumb | 경로 탐색 | 내비게이션 |

---

## 1️⃣ Card

콘텐츠 카드 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `variant` | `'elevated' \| 'outlined' \| 'flat' \| 'glass' \| 'interactive'` | `'elevated'` | ❌ | 카드 스타일 변형 |
| `padding` | `'none' \| 'sm' \| 'md' \| 'lg'` | `'md'` | ❌ | 내부 여백 |
| `hoverable` | `boolean` | `false` | ❌ | 호버 효과 |
| `clickable` | `boolean` | `false` | ❌ | 클릭 가능 (커서 포인터) |
| `bordered` | `boolean` | `true` | ❌ | 테두리 표시 |

### Events

| Event | Payload | Description |
|-------|---------|-------------|
| `click` | `MouseEvent` | 카드 클릭 시 (clickable일 때) |

### Slots

| Slot | Description |
|------|-------------|
| `default` | 카드 본문 콘텐츠 |
| `header` | 카드 헤더 영역 |
| `footer` | 카드 푸터 영역 |
| `media` | 미디어 영역 (이미지, 비디오) |

### TypeScript Interface

```typescript
interface CardProps {
  variant?: 'elevated' | 'outlined' | 'flat' | 'glass' | 'interactive'
  padding?: 'none' | 'sm' | 'md' | 'lg'
  hoverable?: boolean
  clickable?: boolean
  bordered?: boolean
}
```

### 사용 예시

```vue
<script setup lang="ts">
import { Card, Button, Badge } from '@portal/design-system'
</script>

<template>
  <!-- 기본 카드 -->
  <Card>
    <template #header>
      <h3 class="text-lg font-semibold">카드 제목</h3>
    </template>

    <p class="text-body">카드 본문 내용이 들어갑니다.</p>

    <template #footer>
      <Button size="sm">자세히 보기</Button>
    </template>
  </Card>

  <!-- 미디어 카드 -->
  <Card padding="none">
    <template #media>
      <img src="/image.jpg" alt="Card Image" class="w-full h-48 object-cover" />
    </template>

    <div class="p-4">
      <Badge variant="success" class="mb-2">NEW</Badge>
      <h3 class="font-semibold">이미지 카드</h3>
      <p class="text-sm text-meta">이미지가 포함된 카드입니다.</p>
    </div>
  </Card>

  <!-- 인터랙티브 카드 -->
  <Card variant="interactive" hoverable clickable @click="handleClick">
    <h3>클릭 가능한 카드</h3>
    <p>호버 시 효과가 적용됩니다.</p>
  </Card>

  <!-- Glass 효과 카드 -->
  <Card variant="glass">
    <h3>Glass 카드</h3>
    <p>반투명 배경 효과</p>
  </Card>
</template>
```

### 카드 변형 비교

| Variant | 배경 | 테두리 | 그림자 | 용도 |
|---------|------|--------|--------|------|
| `elevated` | 불투명 | 있음 | 있음 | 기본 카드 |
| `outlined` | 투명 | 있음 | 없음 | 가벼운 카드 |
| `flat` | 있음 | 없음 | 없음 | 섹션 구분 |
| `glass` | 반투명 | 있음 | 없음 | 모던 UI |
| `interactive` | 불투명 | 있음 | 호버 시 | 클릭 가능 카드 |

---

## 2️⃣ Container

페이지 래퍼 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `maxWidth` | `'sm' \| 'md' \| 'lg' \| 'xl' \| '2xl' \| 'full'` | `'xl'` | ❌ | 최대 너비 |
| `centered` | `boolean` | `true` | ❌ | 가운데 정렬 |
| `padding` | `boolean` | `true` | ❌ | 좌우 여백 적용 |

### Slots

| Slot | Description |
|------|-------------|
| `default` | 컨테이너 내용 |

### TypeScript Interface

```typescript
interface ContainerProps {
  maxWidth?: 'sm' | 'md' | 'lg' | 'xl' | '2xl' | 'full'
  centered?: boolean
  padding?: boolean
}
```

### 최대 너비 값

| Size | Max Width | 용도 |
|------|-----------|------|
| `sm` | 640px | 폼, 로그인 페이지 |
| `md` | 768px | 블로그 글 |
| `lg` | 1024px | 대시보드 |
| `xl` | 1280px | 일반 페이지 |
| `2xl` | 1536px | 와이드 레이아웃 |
| `full` | 100% | 전체 너비 |

### 사용 예시

```vue
<script setup lang="ts">
import { Container } from '@portal/design-system'
</script>

<template>
  <!-- 기본 컨테이너 -->
  <Container>
    <div class="py-8">
      <h1>페이지 제목</h1>
      <p>페이지 내용</p>
    </div>
  </Container>

  <!-- 좁은 컨테이너 (폼) -->
  <Container maxWidth="sm">
    <form class="py-8">
      <h2>로그인</h2>
      <!-- 폼 필드 -->
    </form>
  </Container>

  <!-- 블로그 글 레이아웃 -->
  <Container maxWidth="md">
    <article class="prose">
      <h1>블로그 제목</h1>
      <p>블로그 내용...</p>
    </article>
  </Container>
</template>
```

---

## 3️⃣ Stack

플렉스 레이아웃 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `direction` | `'horizontal' \| 'vertical'` | `'vertical'` | ❌ | 배치 방향 |
| `gap` | `'none' \| 'xs' \| 'sm' \| 'md' \| 'lg' \| 'xl'` | `'md'` | ❌ | 요소 간 간격 |
| `align` | `'start' \| 'center' \| 'end' \| 'stretch' \| 'baseline'` | `'stretch'` | ❌ | 교차축 정렬 |
| `justify` | `'start' \| 'center' \| 'end' \| 'between' \| 'around' \| 'evenly'` | `'start'` | ❌ | 주축 정렬 |
| `wrap` | `boolean` | `false` | ❌ | 줄 바꿈 허용 |

### Slots

| Slot | Description |
|------|-------------|
| `default` | 스택 내 요소들 |

### TypeScript Interface

```typescript
interface StackProps {
  direction?: 'horizontal' | 'vertical'
  gap?: 'none' | 'xs' | 'sm' | 'md' | 'lg' | 'xl'
  align?: 'start' | 'center' | 'end' | 'stretch' | 'baseline'
  justify?: 'start' | 'center' | 'end' | 'between' | 'around' | 'evenly'
  wrap?: boolean
}
```

### Gap 값

| Gap | Size |
|-----|------|
| `none` | 0 |
| `xs` | 4px |
| `sm` | 8px |
| `md` | 16px |
| `lg` | 24px |
| `xl` | 32px |

### 사용 예시

```vue
<script setup lang="ts">
import { Stack, Card, Button } from '@portal/design-system'
</script>

<template>
  <!-- 세로 스택 -->
  <Stack direction="vertical" gap="md">
    <Card>카드 1</Card>
    <Card>카드 2</Card>
    <Card>카드 3</Card>
  </Stack>

  <!-- 가로 스택 -->
  <Stack direction="horizontal" gap="lg" align="center">
    <Button>버튼 1</Button>
    <Button>버튼 2</Button>
    <Button>버튼 3</Button>
  </Stack>

  <!-- 양 끝 정렬 -->
  <Stack direction="horizontal" justify="between" align="center">
    <h2>제목</h2>
    <Button>액션</Button>
  </Stack>

  <!-- 줄 바꿈 그리드 -->
  <Stack direction="horizontal" gap="md" wrap>
    <Card v-for="i in 6" :key="i" class="w-[calc(33%-1rem)]">
      카드 {{ i }}
    </Card>
  </Stack>
</template>
```

---

## 4️⃣ Divider

구분선 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `direction` | `'horizontal' \| 'vertical'` | `'horizontal'` | ❌ | 구분선 방향 |
| `variant` | `'solid' \| 'dashed' \| 'dotted'` | `'solid'` | ❌ | 선 스타일 |
| `label` | `string` | - | ❌ | 구분선 내 텍스트 |
| `labelPosition` | `'left' \| 'center' \| 'right'` | `'center'` | ❌ | 라벨 위치 |
| `spacing` | `'none' \| 'sm' \| 'md' \| 'lg'` | `'md'` | ❌ | 상하 여백 |

### Slots

| Slot | Description |
|------|-------------|
| `default` | 구분선 내 커스텀 콘텐츠 |

### 사용 예시

```vue
<script setup lang="ts">
import { Divider } from '@portal/design-system'
</script>

<template>
  <!-- 기본 구분선 -->
  <div>위 콘텐츠</div>
  <Divider />
  <div>아래 콘텐츠</div>

  <!-- 라벨이 있는 구분선 -->
  <Divider label="또는" />

  <!-- 점선 구분선 -->
  <Divider variant="dashed" />

  <!-- 세로 구분선 -->
  <div class="flex items-center gap-4">
    <span>항목 1</span>
    <Divider direction="vertical" class="h-4" />
    <span>항목 2</span>
    <Divider direction="vertical" class="h-4" />
    <span>항목 3</span>
  </div>

  <!-- 커스텀 콘텐츠 -->
  <Divider>
    <Badge variant="info">NEW</Badge>
  </Divider>
</template>
```

---

## 5️⃣ FormField

폼 필드 래퍼 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `label` | `string` | - | ❌ | 필드 라벨 |
| `required` | `boolean` | `false` | ❌ | 필수 표시 (*) |
| `error` | `string` | - | ❌ | 오류 메시지 |
| `hint` | `string` | - | ❌ | 힌트 텍스트 |
| `disabled` | `boolean` | `false` | ❌ | 비활성화 스타일 |

### Slots

| Slot | Description |
|------|-------------|
| `default` | 입력 컴포넌트 |
| `label` | 커스텀 라벨 |
| `hint` | 커스텀 힌트 |

### 사용 예시

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { FormField, Input, Select, Textarea } from '@portal/design-system'

const email = ref('')
const emailError = ref('')

const validateEmail = () => {
  if (!email.value) {
    emailError.value = '이메일을 입력하세요'
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)) {
    emailError.value = '올바른 이메일 형식이 아닙니다'
  } else {
    emailError.value = ''
  }
}
</script>

<template>
  <form class="space-y-4">
    <!-- 기본 필드 -->
    <FormField label="이름" required>
      <Input v-model="name" placeholder="홍길동" />
    </FormField>

    <!-- 오류가 있는 필드 -->
    <FormField
      label="이메일"
      required
      :error="emailError"
      hint="업무용 이메일을 입력하세요"
    >
      <Input
        v-model="email"
        type="email"
        placeholder="user@company.com"
        :error="!!emailError"
        @blur="validateEmail"
      />
    </FormField>

    <!-- Select 필드 -->
    <FormField label="부서" required>
      <Select
        v-model="department"
        :options="departmentOptions"
        placeholder="부서를 선택하세요"
      />
    </FormField>

    <!-- Textarea 필드 -->
    <FormField label="자기소개" hint="500자 이내로 작성해주세요">
      <Textarea
        v-model="bio"
        placeholder="간단한 자기소개를 작성하세요"
        rows="4"
      />
    </FormField>
  </form>
</template>
```

---

## 6️⃣ Breadcrumb

경로 탐색 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `items` | `BreadcrumbItem[]` | `[]` | ✅ | 경로 항목 목록 |
| `separator` | `string` | `'/'` | ❌ | 구분자 문자 |
| `maxItems` | `number` | - | ❌ | 최대 표시 항목 수 |
| `collapseFrom` | `'start' \| 'end'` | `'start'` | ❌ | 축소 시작 위치 |

### Types

```typescript
interface BreadcrumbItem {
  label: string
  href?: string
  icon?: string
  disabled?: boolean
}
```

### Events

| Event | Payload | Description |
|-------|---------|-------------|
| `itemClick` | `BreadcrumbItem` | 항목 클릭 시 |

### Slots

| Slot | Description |
|------|-------------|
| `separator` | 커스텀 구분자 |
| `item` | 커스텀 항목 렌더링 |

### 사용 예시

```vue
<script setup lang="ts">
import { Breadcrumb } from '@portal/design-system'

const breadcrumbItems = [
  { label: '홈', href: '/' },
  { label: '블로그', href: '/blog' },
  { label: '카테고리', href: '/blog/category' },
  { label: '글 제목' }  // 마지막 항목은 href 없음 (현재 페이지)
]
</script>

<template>
  <!-- 기본 사용 -->
  <Breadcrumb :items="breadcrumbItems" />

  <!-- 커스텀 구분자 -->
  <Breadcrumb :items="breadcrumbItems" separator=">" />

  <!-- 아이콘 포함 -->
  <Breadcrumb :items="[
    { label: '홈', href: '/', icon: 'home' },
    { label: '설정', href: '/settings', icon: 'cog' },
    { label: '프로필' }
  ]" />

  <!-- 긴 경로 축소 -->
  <Breadcrumb
    :items="longPathItems"
    :maxItems="4"
    collapseFrom="start"
  />
</template>
```

### 라우터 통합

```vue
<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Breadcrumb } from '@portal/design-system'

const route = useRoute()

const breadcrumbItems = computed(() => {
  const paths = route.path.split('/').filter(Boolean)
  return [
    { label: '홈', href: '/' },
    ...paths.map((path, index) => ({
      label: path.charAt(0).toUpperCase() + path.slice(1),
      href: '/' + paths.slice(0, index + 1).join('/'),
    }))
  ]
})
</script>

<template>
  <Breadcrumb :items="breadcrumbItems" />
</template>
```

---

## 🔗 관련 문서

- [입력 컴포넌트](./components-input.md) - Button, Input, Select 등
- [피드백 컴포넌트](./components-feedback.md) - Modal, Toast, Badge 등
- [컴포넌트 사용 가이드](../guides/using-components.md)

---

**최종 업데이트**: 2026-01-18
