---
id: guide-using-components
title: 컴포넌트 사용 가이드
type: guide
status: current
created: 2026-01-18
updated: 2026-01-18
author: Laze
tags: [design-system, components, vue3, usage]
related:
  - guide-getting-started
  - guide-theming
---

# 컴포넌트 사용 가이드

> @portal/design-system Vue 컴포넌트 상세 사용법

---

## 📋 개요

Design System은 재사용 가능한 Vue 3 컴포넌트를 제공합니다.

| 컴포넌트 | 용도 | v-model 지원 |
|---------|------|-------------|
| Button | 클릭 가능한 버튼 | ❌ |
| Card | 콘텐츠 카드 | ❌ |
| Badge | 상태 표시 뱃지 | ❌ |
| Input | 텍스트 입력 필드 | ✅ |
| Modal | 팝업 다이얼로그 | ✅ (open) |
| Tag | 태그/라벨 | ❌ |
| Avatar | 사용자 아바타 | ❌ |
| SearchBar | 검색 입력창 | ✅ |

---

## 1️⃣ Button 컴포넌트

### 기본 사용법

```vue
<script setup lang="ts">
import { Button } from '@portal/design-system'

const handleClick = () => {
  console.log('Button clicked!')
}
</script>

<template>
  <Button variant="primary" size="md" @click="handleClick">
    클릭하기
  </Button>
</template>
```

### Props

| Prop | Type | Default | 설명 |
|------|------|---------|------|
| variant | `'primary' \| 'secondary' \| 'danger'` | `'primary'` | 버튼 스타일 |
| size | `'sm' \| 'md' \| 'lg'` | `'md'` | 버튼 크기 |
| disabled | `boolean` | `false` | 비활성화 상태 |
| loading | `boolean` | `false` | 로딩 상태 |

### 변형 예제

```vue
<template>
  <!-- Primary 버튼 -->
  <Button variant="primary">저장</Button>

  <!-- Secondary 버튼 -->
  <Button variant="secondary">취소</Button>

  <!-- Danger 버튼 -->
  <Button variant="danger">삭제</Button>

  <!-- 비활성화 -->
  <Button disabled>비활성</Button>

  <!-- 크기 변형 -->
  <Button size="sm">작게</Button>
  <Button size="md">중간</Button>
  <Button size="lg">크게</Button>
</template>
```

---

## 2️⃣ Input 컴포넌트

### 기본 사용법 (v-model)

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { Input } from '@portal/design-system'

const email = ref('')
</script>

<template>
  <Input
    v-model="email"
    type="email"
    placeholder="user@example.com"
  />
  <p>입력값: {{ email }}</p>
</template>
```

### Props

| Prop | Type | Default | 설명 |
|------|------|---------|------|
| modelValue | `string` | `''` | 입력값 (v-model) |
| type | `'text' \| 'email' \| 'password' \| 'number'` | `'text'` | 입력 타입 |
| placeholder | `string` | `''` | 플레이스홀더 |
| disabled | `boolean` | `false` | 비활성화 상태 |
| error | `boolean` | `false` | 에러 상태 |

### 유효성 검사 예제

```vue
<script setup lang="ts">
import { ref, computed } from 'vue'
import { Input } from '@portal/design-system'

const email = ref('')
const isValidEmail = computed(() => {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)
})
</script>

<template>
  <div>
    <Input
      v-model="email"
      type="email"
      :error="!isValidEmail && email.length > 0"
    />
    <p v-if="!isValidEmail && email.length > 0" class="text-red-500">
      유효한 이메일을 입력하세요
    </p>
  </div>
</template>
```

---

## 3️⃣ Modal 컴포넌트

### 기본 사용법

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

  <Modal v-model:open="isOpen" title="확인">
    <p>정말로 삭제하시겠습니까?</p>

    <template #footer>
      <Button variant="secondary" @click="isOpen = false">
        취소
      </Button>
      <Button variant="danger" @click="handleConfirm">
        삭제
      </Button>
    </template>
  </Modal>
</template>
```

### Props

| Prop | Type | Default | 설명 |
|------|------|---------|------|
| open | `boolean` | `false` | 모달 표시 상태 (v-model:open) |
| title | `string` | `''` | 모달 제목 |
| size | `'sm' \| 'md' \| 'lg'` | `'md'` | 모달 크기 |
| closable | `boolean` | `true` | X 버튼 표시 여부 |

### Slots

| Slot | 설명 |
|------|------|
| default | 모달 본문 콘텐츠 |
| footer | 모달 하단 버튼 영역 |

---

## 4️⃣ Card 컴포넌트

### 기본 사용법

```vue
<script setup lang="ts">
import { Card, Button } from '@portal/design-system'
</script>

<template>
  <Card>
    <template #header>
      <h3 class="text-lg font-semibold">카드 제목</h3>
    </template>

    <p>카드 본문 내용입니다.</p>

    <template #footer>
      <Button size="sm">자세히 보기</Button>
    </template>
  </Card>
</template>
```

### Slots

| Slot | 설명 |
|------|------|
| header | 카드 헤더 영역 |
| default | 카드 본문 영역 |
| footer | 카드 푸터 영역 |

---

## 5️⃣ Badge 컴포넌트

### 기본 사용법

```vue
<script setup lang="ts">
import { Badge } from '@portal/design-system'
</script>

<template>
  <Badge variant="success">완료</Badge>
  <Badge variant="warning">대기중</Badge>
  <Badge variant="danger">실패</Badge>
  <Badge variant="info">진행중</Badge>
</template>
```

### Props

| Prop | Type | Default | 설명 |
|------|------|---------|------|
| variant | `'success' \| 'warning' \| 'danger' \| 'info'` | `'info'` | 뱃지 색상 |
| size | `'sm' \| 'md'` | `'md'` | 뱃지 크기 |

---

## 6️⃣ Tag 컴포넌트

### 기본 사용법

```vue
<script setup lang="ts">
import { Tag } from '@portal/design-system'

const handleClose = (tagName: string) => {
  console.log(`${tagName} 태그 제거`)
}
</script>

<template>
  <Tag closable @close="handleClose('Vue')">Vue</Tag>
  <Tag closable @close="handleClose('React')">React</Tag>
  <Tag>TypeScript</Tag>
</template>
```

### Props

| Prop | Type | Default | 설명 |
|------|------|---------|------|
| closable | `boolean` | `false` | X 버튼 표시 여부 |
| color | `string` | - | 사용자 정의 배경색 |

---

## 7️⃣ Avatar 컴포넌트

### 기본 사용법

```vue
<script setup lang="ts">
import { Avatar } from '@portal/design-system'
</script>

<template>
  <!-- 이미지 아바타 -->
  <Avatar src="https://example.com/avatar.jpg" alt="User" />

  <!-- 이니셜 아바타 -->
  <Avatar name="John Doe" />

  <!-- 크기 변형 -->
  <Avatar name="JS" size="sm" />
  <Avatar name="JS" size="md" />
  <Avatar name="JS" size="lg" />
</template>
```

### Props

| Prop | Type | Default | 설명 |
|------|------|---------|------|
| src | `string` | - | 이미지 URL |
| alt | `string` | `'avatar'` | 이미지 대체 텍스트 |
| name | `string` | - | 이니셜 표시 (src 없을 때) |
| size | `'sm' \| 'md' \| 'lg'` | `'md'` | 아바타 크기 |

---

## 8️⃣ SearchBar 컴포넌트

### 기본 사용법

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { SearchBar } from '@portal/design-system'

const searchQuery = ref('')

const handleSearch = () => {
  console.log('검색:', searchQuery.value)
}
</script>

<template>
  <SearchBar
    v-model="searchQuery"
    placeholder="검색어를 입력하세요"
    @search="handleSearch"
  />
</template>
```

### Props

| Prop | Type | Default | 설명 |
|------|------|---------|------|
| modelValue | `string` | `''` | 검색어 (v-model) |
| placeholder | `string` | `'검색...'` | 플레이스홀더 텍스트 |
| disabled | `boolean` | `false` | 비활성화 상태 |

### Events

| Event | Payload | 설명 |
|-------|---------|------|
| update:modelValue | `string` | 검색어 변경 시 |
| search | - | Enter 키 또는 검색 버튼 클릭 시 |

---

## 💡 베스트 프랙티스

### 1. v-model 사용 권장

```vue
<!-- ✗ 나쁜 예 -->
<Input :modelValue="email" @update:modelValue="email = $event" />

<!-- ✓ 좋은 예 -->
<Input v-model="email" />
```

### 2. Semantic 클래스 사용

```vue
<!-- ✗ 원시 Tailwind 클래스 오버라이드 -->
<Button class="bg-red-500 text-white">위험</Button>

<!-- ✓ 컴포넌트 Props 사용 -->
<Button variant="danger">위험</Button>
```

### 3. 레이아웃만 Tailwind 사용

```vue
<!-- ✓ 레이아웃 관련은 Tailwind OK -->
<Button variant="primary" class="w-full">전체 너비</Button>
```

---

## 🔗 관련 문서

- [API Reference](../api/README.md) - Props, Events, Slots 상세 명세
- [테마 적용 가이드](./theming-guide.md) - 컴포넌트 스타일 커스터마이징
- [Storybook](http://localhost:6006) - 컴포넌트 인터랙티브 문서

---

**최종 업데이트**: 2026-01-18
