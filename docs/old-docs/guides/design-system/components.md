---
id: design-system-vue-components
title: Design System Vue 컴포넌트 카탈로그
type: guide
status: current
created: 2026-01-19
updated: 2026-01-30
author: Laze
tags: [design-system-vue, components, vue, catalog]
---

# Component Catalog

Design System에서 제공하는 모든 Vue 3 컴포넌트의 카탈로그 및 사용 가이드입니다.

## 목차

- [기본 컴포넌트](#기본-컴포넌트)
- [입력 컴포넌트](#입력-컴포넌트)
- [레이아웃 컴포넌트](#레이아웃-컴포넌트)
- [피드백 컴포넌트](#피드백-컴포넌트)
- [네비게이션 컴포넌트](#네비게이션-컴포넌트)
- [Storybook 보기](#storybook-보기)

## 기본 컴포넌트

### Button (버튼)

클릭 가능한 버튼 요소입니다. 다양한 크기, 변형, 상태를 지원합니다.

**Props:**
- `variant`: 'primary' | 'secondary' | 'danger' | 'ghost' (기본: 'primary')
- `size`: 'sm' | 'md' | 'lg' (기본: 'md')
- `disabled`: boolean (기본: false)
- `loading`: boolean (기본: false)
- `type`: 'button' | 'submit' | 'reset' (기본: 'button')

**사용 예제:**

```vue
<template>
  <!-- Primary 버튼 -->
  <Button @click="handleClick">
    클릭하기
  </Button>

  <!-- Secondary 버튼 -->
  <Button variant="secondary" size="lg">
    보조 버튼
  </Button>

  <!-- 로딩 상태 -->
  <Button :loading="isLoading">
    처리 중...
  </Button>

  <!-- Disabled 상태 -->
  <Button disabled>
    비활성화됨
  </Button>

  <!-- Danger 버튼 -->
  <Button variant="danger" @click="handleDelete">
    삭제하기
  </Button>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Button } from '@portal/design-system'

const isLoading = ref(false)

const handleClick = () => {
  console.log('Button clicked')
}

const handleDelete = () => {
  if (confirm('정말 삭제하시겠습니까?')) {
    // Delete logic
  }
}
</script>
```

### Card (카드)

정보를 담는 기본 컨테이너 컴포넌트입니다.

**Slots:**
- `default`: 카드 본문
- `header`: 카드 헤더
- `footer`: 카드 푸터

**사용 예제:**

```vue
<template>
  <Card>
    <CardHeader>
      <CardTitle>카드 제목</CardTitle>
      <CardDescription>부제목 또는 설명</CardDescription>
    </CardHeader>
    <CardContent>
      <p>카드 본문 내용입니다.</p>
    </CardContent>
    <CardFooter>
      <Button>확인</Button>
    </CardFooter>
  </Card>
</template>

<script setup lang="ts">
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter, Button } from '@portal/design-system'
</script>
```

### Badge (배지)

라벨, 상태, 카운트 등을 표시하는 작은 요소입니다.

**Props:**
- `variant`: 'default' | 'primary' | 'secondary' | 'success' | 'destructive' | 'outline' (기본: 'default')

**사용 예제:**

```vue
<template>
  <div class="flex gap-2">
    <Badge>기본</Badge>
    <Badge variant="primary">Primary</Badge>
    <Badge variant="success">성공</Badge>
    <Badge variant="destructive">오류</Badge>
  </div>
</template>

<script setup lang="ts">
import { Badge } from '@portal/design-system'
</script>
```

## 입력 컴포넌트

### SearchBar (검색 입력)

검색 기능을 제공하는 입력 필드입니다. 검색 아이콘, 클리어 버튼, 로딩 스피너, 반응형 지원을 포함합니다.

**Props:**
- `modelValue`: string (기본: '')
- `placeholder`: string (기본: '검색...')
- `loading`: boolean (기본: false)
- `disabled`: boolean (기본: false)
- `autofocus`: boolean (기본: false)

**이벤트:**
- `update:modelValue`: 입력값 변경 시 발생
- `search`: 검색 실행 시 발생 (Enter 키 또는 검색 버튼 클릭)
- `clear`: 클리어 버튼 클릭 시 발생

**사용 예제:**

```vue
<template>
  <div class="space-y-4">
    <!-- 기본 검색 -->
    <SearchBar
      v-model="searchKeyword"
      placeholder="상품을 검색하세요"
      @search="handleSearch"
    />

    <!-- 로딩 상태 -->
    <SearchBar
      v-model="searchKeyword"
      :loading="isSearching"
      @search="handleSearch"
    />

    <!-- 비활성화 -->
    <SearchBar
      v-model="searchKeyword"
      disabled
      placeholder="검색 비활성화"
    />

    <!-- 자동 포커스 -->
    <SearchBar
      v-model="searchKeyword"
      autofocus
      @search="handleSearch"
      @clear="handleClear"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { SearchBar } from '@portal/design-system'

const searchKeyword = ref('')
const isSearching = ref(false)

const handleSearch = (keyword: string) => {
  console.log('Searching for:', keyword)
  isSearching.value = true
  // API 호출
  setTimeout(() => {
    isSearching.value = false
  }, 1000)
}

const handleClear = () => {
  console.log('Search cleared')
  searchKeyword.value = ''
}
</script>
```

### Input (텍스트 입력)

텍스트 입력 필드입니다.

**Props:**
- `modelValue`: string
- `type`: 'text' | 'email' | 'password' | 'number' | 'search' (기본: 'text')
- `placeholder`: string
- `disabled`: boolean
- `readonly`: boolean
- `error`: boolean
- `helperText`: string

**이벤트:**
- `update:modelValue`: 입력값 변경 시 발생

**사용 예제:**

```vue
<template>
  <div class="space-y-4">
    <!-- 기본 입력 -->
    <Input
      v-model="email"
      type="email"
      placeholder="이메일을 입력하세요"
    />

    <!-- 에러 상태 -->
    <Input
      v-model="username"
      :error="usernameError"
      :helper-text="usernameError ? '사용자명은 3자 이상이어야 합니다' : ''"
    />

    <!-- 비활성화 -->
    <Input
      type="text"
      placeholder="비활성화된 입력"
      disabled
    />

    <!-- 읽기 전용 -->
    <Input
      :model-value="userId"
      readonly
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Input } from '@portal/design-system'

const email = ref('')
const username = ref('')
const userId = ref('USER-12345')

const usernameError = computed(() => username.value.length > 0 && username.value.length < 3)
</script>
```

### Textarea (여러 줄 입력)

여러 줄 텍스트 입력 필드입니다.

**Props:**
- `modelValue`: string
- `placeholder`: string
- `rows`: number (기본: 4)
- `disabled`: boolean
- `readonly`: boolean
- `error`: boolean

**사용 예제:**

```vue
<template>
  <Textarea
    v-model="description"
    placeholder="설명을 입력하세요 (최대 500자)"
    :rows="5"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Textarea } from '@portal/design-system'

const description = ref('')
</script>
```

### Select (선택 드롭다운)

드롭다운 선택 요소입니다.

**Props:**
- `modelValue`: string | number
- `options`: Array<{ label: string; value: string | number }>
- `placeholder`: string
- `disabled`: boolean
- `multiple`: boolean

**사용 예제:**

```vue
<template>
  <Select
    v-model="category"
    :options="categories"
    placeholder="카테고리 선택"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Select } from '@portal/design-system'

const category = ref('')
const categories = [
  { label: '기술', value: 'tech' },
  { label: '라이프스타일', value: 'lifestyle' },
  { label: '뉴스', value: 'news' }
]
</script>
```

### Checkbox (체크박스)

선택 가능한 체크박스입니다.

**Props:**
- `modelValue`: boolean
- `label`: string
- `disabled`: boolean
- `indeterminate`: boolean

**사용 예제:**

```vue
<template>
  <div class="space-y-3">
    <Checkbox
      v-model="agreeTerms"
      label="약관에 동의합니다"
    />
    <Checkbox
      v-model="agreeNewsletter"
      label="뉴스레터 수신에 동의합니다"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Checkbox } from '@portal/design-system'

const agreeTerms = ref(false)
const agreeNewsletter = ref(false)
</script>
```

### Radio (라디오 버튼)

단일 선택 라디오 버튼입니다.

**Props:**
- `modelValue`: string | number
- `value`: string | number
- `label`: string
- `disabled`: boolean

**사용 예제:**

```vue
<template>
  <div class="space-y-3">
    <Radio
      v-model="paymentMethod"
      value="credit_card"
      label="신용카드"
    />
    <Radio
      v-model="paymentMethod"
      value="bank_transfer"
      label="계좌 이체"
    />
    <Radio
      v-model="paymentMethod"
      value="digital_wallet"
      label="디지털 지갑"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Radio } from '@portal/design-system'

const paymentMethod = ref('credit_card')
</script>
```

### Switch (토글 스위치)

토글 가능한 스위치 컴포넌트입니다.

**Props:**
- `modelValue`: boolean
- `label`: string
- `disabled`: boolean

**사용 예제:**

```vue
<template>
  <div class="space-y-3">
    <Switch
      v-model="darkMode"
      label="다크 모드"
    />
    <Switch
      v-model="notifications"
      label="알림 활성화"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Switch, useTheme } from '@portal/design-system'

const { toggleTheme } = useTheme()
const darkMode = ref(false)
const notifications = ref(true)

const toggleDarkMode = (value: boolean) => {
  darkMode.value = value
  toggleTheme()
}
</script>
```

## 레이아웃 컴포넌트

### Container (컨테이너)

반응형 콘텐츠 컨테이너입니다.

**Props:**
- `maxWidth`: 'sm' | 'md' | 'lg' | 'xl' | 'full' (기본: 'lg')
- `padding`: 'sm' | 'md' | 'lg' (기본: 'md')

**사용 예제:**

```vue
<template>
  <Container max-width="lg" padding="md">
    <h1>페이지 제목</h1>
    <p>콘텐츠가 최대 너비로 제한됩니다.</p>
  </Container>
</template>

<script setup lang="ts">
import { Container } from '@portal/design-system'
</script>
```

### Stack (스택 레이아웃)

유연한 Flexbox 레이아웃 컴포넌트입니다.

**Props:**
- `direction`: 'row' | 'column' (기본: 'column')
- `gap`: number | 'sm' | 'md' | 'lg' (기본: 'md')
- `align`: 'start' | 'center' | 'end' | 'stretch' (기본: 'stretch')
- `justify`: 'start' | 'center' | 'end' | 'between' | 'around' (기본: 'start')

**사용 예제:**

```vue
<template>
  <Stack direction="row" gap="md" justify="between" align="center">
    <h2>제목</h2>
    <Button>액션</Button>
  </Stack>

  <Stack direction="column" gap="lg">
    <div>첫 번째 아이템</div>
    <div>두 번째 아이템</div>
    <div>세 번째 아이템</div>
  </Stack>
</template>

<script setup lang="ts">
import { Stack, Button } from '@portal/design-system'
</script>
```

## 피드백 컴포넌트

### Alert (알림)

메시지, 경고, 오류 등을 표시하는 알림 박스입니다.

**Props:**
- `type`: 'info' | 'success' | 'warning' | 'error' (기본: 'info')
- `title`: string
- `closable`: boolean (기본: false)

**Slots:**
- `default`: 알림 메시지

**사용 예제:**

```vue
<template>
  <div class="space-y-4">
    <Alert type="info" title="정보">
      이것은 정보 알림입니다.
    </Alert>

    <Alert type="success" title="성공!" closable>
      작업이 성공적으로 완료되었습니다.
    </Alert>

    <Alert type="warning" title="주의" closable>
      주의해야 할 내용이 있습니다.
    </Alert>

    <Alert type="error" title="오류 발생" closable>
      오류가 발생했습니다. 다시 시도해주세요.
    </Alert>
  </div>
</template>

<script setup lang="ts">
import { Alert } from '@portal/design-system'
</script>
```

### Modal (모달)

모달 다이얼로그 컴포넌트입니다.

**Props:**
- `modelValue`: boolean (v-model)
- `title`: string
- `size`: 'sm' | 'md' | 'lg' (기본: 'md')
- `closable`: boolean (기본: true)

**Slots:**
- `default`: 모달 본문
- `footer`: 모달 하단 (버튼 등)

**이벤트:**
- `update:modelValue`: 모달 상태 변경

**사용 예제:**

```vue
<template>
  <div>
    <Button @click="isModalOpen = true">모달 열기</Button>

    <Modal
      v-model="isModalOpen"
      title="확인 다이얼로그"
      size="md"
    >
      <p>정말 진행하시겠습니까?</p>

      <template #footer>
        <Button variant="secondary" @click="isModalOpen = false">
          취소
        </Button>
        <Button @click="handleConfirm">
          확인
        </Button>
      </template>
    </Modal>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Modal, Button } from '@portal/design-system'

const isModalOpen = ref(false)

const handleConfirm = () => {
  console.log('Confirmed!')
  isModalOpen.value = false
}
</script>
```

### Spinner (로딩 표시기)

로딩 상태를 표시하는 회전 스피너입니다.

**Props:**
- `size`: 'sm' | 'md' | 'lg' (기본: 'md')
- `color`: 'primary' | 'secondary' | 'white' (기본: 'primary')

**사용 예제:**

```vue
<template>
  <div v-if="isLoading" class="flex justify-center items-center h-40">
    <Spinner size="lg" />
  </div>
  <div v-else>
    <p>콘텐츠가 로드되었습니다.</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Spinner } from '@portal/design-system'

const isLoading = ref(true)

onMounted(() => {
  setTimeout(() => {
    isLoading.value = false
  }, 2000)
})
</script>
```

### Skeleton (스켈레톤)

콘텐츠 로딩 중 표시할 스켈레톤 로더입니다.

**Props:**
- `width`: string | number (기본: '100%')
- `height`: string | number
- `variant`: 'text' | 'rect' | 'circle' (기본: 'text')
- `count`: number (기본: 1)

**사용 예제:**

```vue
<template>
  <div class="space-y-4">
    <Skeleton height="20" count="3" />
    <Skeleton variant="rect" height="200" />
    <Skeleton variant="circle" width="64" height="64" />
  </div>
</template>

<script setup lang="ts">
import { Skeleton } from '@portal/design-system'
</script>
```

## 네비게이션 컴포넌트

### Breadcrumb (경로 표시)

현재 위치의 계층 구조를 표시합니다.

**Props:**
- `items`: Array<{ label: string; to?: string; onClick?: () => void }>

**사용 예제:**

```vue
<template>
  <Breadcrumb :items="breadcrumbs" />
</template>

<script setup lang="ts">
import { Breadcrumb } from '@portal/design-system'

const breadcrumbs = [
  { label: '홈', to: '/' },
  { label: '카테고리', to: '/category' },
  { label: '상품', to: '/category/product/123' }
]
</script>
```

### Tabs (탭)

여러 콘텐츠 섹션을 탭으로 나누어 표시합니다.

**Props:**
- `modelValue`: string (활성 탭)
- `tabs`: Array<{ label: string; value: string }>

**이벤트:**
- `update:modelValue`: 활성 탭 변경

**Slots:**
- 각 탭 콘텐츠는 slot name으로 지정

**사용 예제:**

```vue
<template>
  <Tabs v-model="activeTab" :tabs="tabs">
    <template #profile>
      <p>프로필 정보</p>
    </template>
    <template #settings>
      <p>설정</p>
    </template>
    <template #notifications>
      <p>알림</p>
    </template>
  </Tabs>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Tabs } from '@portal/design-system'

const activeTab = ref('profile')
const tabs = [
  { label: '프로필', value: 'profile' },
  { label: '설정', value: 'settings' },
  { label: '알림', value: 'notifications' }
]
</script>
```

### Dropdown (드롭다운 메뉴)

클릭 시 메뉴를 표시하는 드롭다운 컴포넌트입니다.

**Props:**
- `items`: Array<{ label: string; action?: () => void; divider?: boolean }>
- `placement`: 'top' | 'bottom' | 'left' | 'right' (기본: 'bottom')

**Slots:**
- `trigger`: 드롭다운을 열 요소

**사용 예제:**

```vue
<template>
  <Dropdown :items="menuItems">
    <template #trigger="{ toggle }">
      <Button @click="toggle">메뉴</Button>
    </template>
  </Dropdown>
</template>

<script setup lang="ts">
import { Dropdown, Button } from '@portal/design-system'

const menuItems = [
  { label: '편집', action: () => console.log('Edit') },
  { label: '복사', action: () => console.log('Copy') },
  { divider: true },
  { label: '삭제', action: () => console.log('Delete') }
]
</script>
```

## Storybook 보기

모든 컴포넌트를 Storybook에서 시각적으로 확인할 수 있습니다:

```bash
npm run storybook
```

Storybook은 `http://localhost:6006`에서 실행됩니다.

각 컴포넌트의:
- 다양한 상태와 props 조합
- 상호작용 가능한 컨트롤
- 코드 예제 및 주석
- 접근성(Accessibility) 검사

를 확인할 수 있습니다.

## 컴포넌트 import 패턴

### 개별 import

```typescript
import { Button, Card, Modal } from '@portal/design-system'
```

### 타입 import

```typescript
import type { ButtonProps, CardProps } from '@portal/design-system'
```

## 다음 단계

- [TOKENS.md](./TOKENS.md) - 디자인 토큰 시스템 이해하기
- [THEMING.md](./THEMING.md) - 테마 커스터마이징
- [USAGE.md](./USAGE.md) - 통합 가이드