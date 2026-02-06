---
id: api-components-navigation
title: 내비게이션 컴포넌트 API
type: api
status: current
created: 2026-02-06
updated: 2026-02-06
author: Laze
tags: [design-system, api, navigation, components, vue3]
related:
  - api-design-system
  - guide-using-components
---

# 내비게이션 컴포넌트 API

> Link, Dropdown, Tabs, Breadcrumb

---

## 📋 개요

내비게이션 컴포넌트는 사용자의 이동과 메뉴 선택을 위한 인터페이스를 제공합니다.

| 컴포넌트 | 용도 | 유형 |
|---------|------|------|
| Link | 하이퍼링크 | 기본 |
| Dropdown | 드롭다운 메뉴 | 메뉴 |
| Tabs | 탭 내비게이션 | 내비게이션 |
| Breadcrumb | 경로 탐색 | 내비게이션 |

---

## 1️⃣ Link

하이퍼링크 컴포넌트 (Vue Router 지원)

### Props

| Prop | 타입 | 기본값 | 필수 | 설명 |
|------|------|--------|------|------|
| `href` | `string` | - | ❌ | 외부 링크 URL |
| `to` | `RouteLocationRaw` | - | ❌ | Vue Router 경로 |
| `target` | `'_self' \| '_blank' \| '_parent' \| '_top'` | `'_self'` | ❌ | 링크 타겟 |
| `variant` | `'default' \| 'primary' \| 'muted' \| 'underline'` | `'default'` | ❌ | 링크 스타일 변형 |
| `external` | `boolean` | `false` | ❌ | 외부 링크 아이콘 표시 |
| `disabled` | `boolean` | `false` | ❌ | 비활성화 상태 |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | ❌ | 링크 크기 |

### Events

없음 (네이티브 `click` 이벤트 사용)

### Slots

| Slot | 설명 |
|------|------|
| `default` | 링크 텍스트 또는 콘텐츠 |

### TypeScript Interface

```typescript
import type { RouteLocationRaw } from 'vue-router'

export interface LinkProps {
  href?: string
  to?: RouteLocationRaw
  target?: '_self' | '_blank' | '_parent' | '_top'
  variant?: 'default' | 'primary' | 'muted' | 'underline'
  external?: boolean
  disabled?: boolean
  size?: 'sm' | 'md' | 'lg'
}
```

### 사용 예시

```vue
<script setup lang="ts">
import { Link } from '@portal/design-system'
</script>

<template>
  <!-- 기본 링크 -->
  <Link href="/about">회사 소개</Link>

  <!-- Vue Router 링크 -->
  <Link :to="{ name: 'profile', params: { id: 123 } }">
    내 프로필
  </Link>

  <!-- 외부 링크 -->
  <Link href="https://portal-universe.com" target="_blank" external>
    포털 유니버스 웹사이트
  </Link>

  <!-- 변형 스타일 -->
  <div class="space-x-4">
    <Link href="/help" variant="default">기본 스타일</Link>
    <Link href="/signup" variant="primary">주요 스타일</Link>
    <Link href="/terms" variant="muted">부드러운 스타일</Link>
    <Link href="/docs" variant="underline">밑줄 스타일</Link>
  </div>

  <!-- 비활성화된 링크 -->
  <Link href="/coming-soon" disabled>
    준비 중
  </Link>

  <!-- 크기 변형 -->
  <div class="space-x-4">
    <Link href="/small" size="sm">작은 링크</Link>
    <Link href="/medium" size="md">보통 링크</Link>
    <Link href="/large" size="lg">큰 링크</Link>
  </div>
</template>
```

### 링크 변형 비교

| Variant | 색상 | 용도 |
|---------|------|------|
| `default` | 링크 색상 (파란색) | 일반 하이퍼링크 |
| `primary` | 브랜드 색상 | 강조 링크 |
| `muted` | 회색 계열 | 보조 링크 |
| `underline` | 링크 색상 + 밑줄 | 본문 내 링크 |

### 접근성

- `target="_blank"`일 때 자동으로 `rel="noopener noreferrer"` 추가
- 외부 링크일 때 아이콘으로 시각적 표시
- 비활성화 시 `aria-disabled="true"` 및 `tabindex="-1"` 적용
- 키보드 포커스 링 제공

---

## 2️⃣ Dropdown

드롭다운 메뉴 컴포넌트

### Props

| Prop | 타입 | 기본값 | 필수 | 설명 |
|------|------|--------|------|------|
| `items` | `DropdownItem[]` | `[]` | ✅ | 메뉴 항목 목록 |
| `trigger` | `'click' \| 'hover'` | `'click'` | ❌ | 메뉴 활성화 트리거 |
| `placement` | `'bottom' \| 'bottom-start' \| 'bottom-end' \| 'top' \| 'top-start' \| 'top-end'` | `'bottom-start'` | ❌ | 메뉴 위치 |
| `disabled` | `boolean` | `false` | ❌ | 비활성화 상태 |
| `closeOnSelect` | `boolean` | `true` | ❌ | 항목 선택 시 메뉴 닫기 |
| `width` | `'auto' \| 'trigger' \| string` | `'auto'` | ❌ | 메뉴 너비 |

### Types

```typescript
export interface DropdownItem {
  label: string
  value?: string | number
  icon?: string
  disabled?: boolean
  divider?: boolean  // 구분선으로 사용
}
```

### Events

| Event | Payload | 설명 |
|-------|---------|------|
| `select` | `DropdownItem` | 항목 선택 시 |
| `open` | - | 메뉴 열릴 때 |
| `close` | - | 메뉴 닫힐 때 |

### Slots

| Slot | Props | 설명 |
|------|-------|------|
| `trigger` | - | 커스텀 트리거 버튼 |
| `item` | `{ item: DropdownItem }` | 커스텀 메뉴 항목 렌더링 |

### TypeScript Interface

```typescript
export interface DropdownProps {
  items: DropdownItem[]
  trigger?: 'click' | 'hover'
  placement?: 'bottom' | 'bottom-start' | 'bottom-end' | 'top' | 'top-start' | 'top-end'
  disabled?: boolean
  closeOnSelect?: boolean
  width?: 'auto' | 'trigger' | string
}

export interface DropdownEmits {
  (e: 'select', item: DropdownItem): void
  (e: 'open'): void
  (e: 'close'): void
}
```

### 사용 예시

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { Dropdown, Button, Badge } from '@portal/design-system'
import type { DropdownItem } from '@portal/design-system'

const basicItems: DropdownItem[] = [
  { label: '내 프로필', value: 'profile' },
  { label: '설정', value: 'settings' },
  { divider: true },
  { label: '로그아웃', value: 'logout' },
]

const actionItems: DropdownItem[] = [
  { label: '편집', value: 'edit', icon: 'pencil' },
  { label: '복사', value: 'copy', icon: 'copy' },
  { label: '공유', value: 'share', icon: 'share' },
  { divider: true },
  { label: '삭제', value: 'delete', icon: 'trash', disabled: false },
]

const handleSelect = (item: DropdownItem) => {
  console.log('Selected:', item.value)
}
</script>

<template>
  <!-- 기본 드롭다운 -->
  <Dropdown :items="basicItems" @select="handleSelect">
    <template #trigger>
      <Button variant="outline">
        메뉴
      </Button>
    </template>
  </Dropdown>

  <!-- 커스텀 트리거 -->
  <Dropdown :items="basicItems" @select="handleSelect">
    <template #trigger>
      <button class="flex items-center gap-2 px-4 py-2 rounded-lg hover:bg-gray-100">
        <img src="/avatar.jpg" alt="User" class="w-8 h-8 rounded-full" />
        <span>홍길동</span>
      </button>
    </template>
  </Dropdown>

  <!-- 호버 트리거 -->
  <Dropdown :items="actionItems" trigger="hover" placement="bottom-end">
    <template #trigger>
      <Button variant="ghost" size="sm">
        더보기
      </Button>
    </template>
  </Dropdown>

  <!-- 트리거 너비에 맞춤 -->
  <Dropdown :items="basicItems" width="trigger" @select="handleSelect">
    <template #trigger>
      <Button variant="outline" class="w-64">
        긴 버튼 텍스트입니다
      </Button>
    </template>
  </Dropdown>

  <!-- 커스텀 항목 렌더링 -->
  <Dropdown :items="actionItems" @select="handleSelect">
    <template #trigger>
      <Button variant="outline">작업</Button>
    </template>
    <template #item="{ item }">
      <div class="flex items-center justify-between w-full">
        <span>{{ item.label }}</span>
        <Badge v-if="item.value === 'edit'" variant="info" size="sm">NEW</Badge>
      </div>
    </template>
  </Dropdown>

  <!-- 상단 배치 -->
  <Dropdown :items="basicItems" placement="top-start" @select="handleSelect">
    <template #trigger>
      <Button variant="outline">위로 열기</Button>
    </template>
  </Dropdown>

  <!-- 선택 후 닫히지 않음 -->
  <Dropdown :items="actionItems" :closeOnSelect="false" @select="handleSelect">
    <template #trigger>
      <Button variant="outline">다중 선택</Button>
    </template>
  </Dropdown>
</template>
```

### Placement 옵션

| Placement | 위치 | 용도 |
|-----------|------|------|
| `bottom` | 하단 중앙 | 중앙 정렬 메뉴 |
| `bottom-start` | 하단 왼쪽 | 일반 드롭다운 |
| `bottom-end` | 하단 오른쪽 | 사용자 메뉴 |
| `top` | 상단 중앙 | 하단 공간 부족 시 |
| `top-start` | 상단 왼쪽 | 하단 공간 부족 시 |
| `top-end` | 상단 오른쪽 | 하단 공간 부족 시 |

### 접근성

- 키보드 내비게이션 지원:
  - `Enter` / `Space`: 메뉴 열기/항목 선택
  - `ArrowDown`: 다음 항목으로 이동
  - `ArrowUp`: 이전 항목으로 이동
  - `Escape`: 메뉴 닫기
  - `Tab`: 메뉴 닫고 포커스 이동
- `aria-haspopup="menu"`, `aria-expanded`, `role="menu"` 적용
- 비활성화된 항목은 선택 불가
- Click-outside로 메뉴 자동 닫기

---

## 3️⃣ Tabs

탭 내비게이션 컴포넌트

### Props

| Prop | 타입 | 기본값 | 필수 | 설명 |
|------|------|--------|------|------|
| `modelValue` | `string` | - | ✅ | 선택된 탭 값 (v-model) |
| `items` | `TabItem[]` | `[]` | ✅ | 탭 항목 목록 |
| `variant` | `'default' \| 'pills' \| 'underline'` | `'default'` | ❌ | 탭 스타일 변형 |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | ❌ | 탭 크기 |
| `fullWidth` | `boolean` | `false` | ❌ | 전체 너비 탭 |

### Types

```typescript
export interface TabItem {
  label: string
  value: string
  disabled?: boolean
  icon?: string
}
```

### Events

| Event | Payload | 설명 |
|-------|---------|------|
| `update:modelValue` | `string` | 선택된 탭 값 변경 (v-model) |
| `change` | `string` | 탭 변경 시 |

### Slots

| Slot | Props | 설명 |
|------|-------|------|
| `tab` | `{ tab: TabItem, active: boolean }` | 커스텀 탭 렌더링 |

### TypeScript Interface

```typescript
export interface TabsProps {
  modelValue: string
  items: TabItem[]
  variant?: 'default' | 'pills' | 'underline'
  size?: 'sm' | 'md' | 'lg'
  fullWidth?: boolean
}

export interface TabsEmits {
  (e: 'update:modelValue', value: string): void
  (e: 'change', value: string): void
}
```

### 사용 예시

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { Tabs, Card, Badge } from '@portal/design-system'
import type { TabItem } from '@portal/design-system'

const currentTab = ref('overview')

const tabs: TabItem[] = [
  { label: '개요', value: 'overview' },
  { label: '상세 정보', value: 'details' },
  { label: '리뷰', value: 'reviews' },
  { label: '설정', value: 'settings', disabled: true },
]

const handleTabChange = (value: string) => {
  console.log('Tab changed to:', value)
}
</script>

<template>
  <!-- 기본 탭 -->
  <div>
    <Tabs v-model="currentTab" :items="tabs" @change="handleTabChange" />

    <div class="mt-4">
      <div v-if="currentTab === 'overview'">개요 콘텐츠</div>
      <div v-if="currentTab === 'details'">상세 정보 콘텐츠</div>
      <div v-if="currentTab === 'reviews'">리뷰 콘텐츠</div>
    </div>
  </div>

  <!-- Pills 변형 -->
  <Tabs
    v-model="currentTab"
    :items="tabs"
    variant="pills"
  />

  <!-- Underline 변형 -->
  <Tabs
    v-model="currentTab"
    :items="tabs"
    variant="underline"
  />

  <!-- 작은 크기 -->
  <Tabs
    v-model="currentTab"
    :items="tabs"
    size="sm"
  />

  <!-- 전체 너비 -->
  <Tabs
    v-model="currentTab"
    :items="tabs"
    fullWidth
  />

  <!-- 카드와 함께 사용 -->
  <Card>
    <template #header>
      <Tabs v-model="currentTab" :items="tabs" />
    </template>

    <div>
      <div v-if="currentTab === 'overview'">
        <h3 class="text-lg font-semibold mb-2">프로젝트 개요</h3>
        <p>프로젝트에 대한 설명...</p>
      </div>
      <div v-if="currentTab === 'details'">
        <h3 class="text-lg font-semibold mb-2">상세 정보</h3>
        <p>상세한 정보...</p>
      </div>
    </div>
  </Card>

  <!-- 커스텀 탭 렌더링 (배지 포함) -->
  <Tabs v-model="currentTab" :items="tabs">
    <template #tab="{ tab, active }">
      <div class="flex items-center gap-2">
        <span>{{ tab.label }}</span>
        <Badge v-if="tab.value === 'reviews'" variant="info" size="sm">
          3
        </Badge>
      </div>
    </template>
  </Tabs>

  <!-- 아이콘이 있는 탭 -->
  <Tabs
    v-model="currentTab"
    :items="[
      { label: '홈', value: 'home', icon: 'home' },
      { label: '검색', value: 'search', icon: 'search' },
      { label: '알림', value: 'notifications', icon: 'bell' },
      { label: '프로필', value: 'profile', icon: 'user' },
    ]"
  />
</template>
```

### 탭 변형 비교

| Variant | 스타일 | 용도 |
|---------|--------|------|
| `default` | 하단 경계선 + 활성 탭 하이라이트 | 일반 탭 내비게이션 |
| `pills` | 둥근 배경 + 활성 탭 강조 | 모던한 UI |
| `underline` | 활성 탭 하단 밑줄 | 최소한의 디자인 |

### 접근성

- 키보드 내비게이션 지원:
  - `ArrowLeft`: 이전 탭으로 이동
  - `ArrowRight`: 다음 탭으로 이동
  - `Home`: 첫 번째 탭으로 이동
  - `End`: 마지막 탭으로 이동
- `role="tablist"`, `role="tab"` 적용
- `aria-selected`, `aria-disabled` 상태 표시
- 활성 탭만 `tabindex="0"`, 나머지는 `tabindex="-1"`
- 비활성화된 탭은 건너뛰기

### 탭 콘텐츠 패턴

```vue
<script setup lang="ts">
import { ref, computed } from 'vue'
import { Tabs } from '@portal/design-system'

const currentTab = ref('tab1')

const tabs = [
  { label: '탭 1', value: 'tab1' },
  { label: '탭 2', value: 'tab2' },
  { label: '탭 3', value: 'tab3' },
]

// 조건부 렌더링 (권장)
</script>

<template>
  <div>
    <Tabs v-model="currentTab" :items="tabs" />

    <div class="mt-4">
      <div v-if="currentTab === 'tab1'">탭 1 콘텐츠</div>
      <div v-else-if="currentTab === 'tab2'">탭 2 콘텐츠</div>
      <div v-else-if="currentTab === 'tab3'">탭 3 콘텐츠</div>
    </div>
  </div>

  <!-- 또는 컴포넌트 동적 로딩 -->
  <div>
    <Tabs v-model="currentTab" :items="tabs" />

    <KeepAlive>
      <component :is="tabComponents[currentTab]" />
    </KeepAlive>
  </div>
</template>
```

---

## 4️⃣ Breadcrumb

계층적 경로 탐색을 위한 브레드크럼 컴포넌트 (Vue Router 지원)

### Props

| Prop | 타입 | 기본값 | 필수 | 설명 |
|------|------|--------|------|------|
| `items` | `BreadcrumbItem[]` | `[]` | ✅ | 브레드크럼 항목 목록 |
| `separator` | `string` | `'/'` | ❌ | 항목 구분자 |
| `maxItems` | `number` | - | ❌ | 최대 표시 항목 수 (초과 시 ellipsis) |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | ❌ | 브레드크럼 크기 |

### Types

```typescript
import type { RouteLocationRaw } from 'vue-router'

export interface BreadcrumbItem {
  label: string
  href?: string          // 일반 링크 URL
  to?: RouteLocationRaw  // Vue Router 경로
  icon?: string          // 아이콘 (선택적)
}
```

### Events

없음 (Vue Router의 네비게이션 이벤트 사용)

### Slots

| Slot | Props | 설명 |
|------|-------|------|
| `separator` | - | 커스텀 구분자 렌더링 |
| `item` | `{ item: BreadcrumbItem, index: number, isLast: boolean }` | 커스텀 항목 렌더링 |

### TypeScript Interface

```typescript
export interface BreadcrumbProps {
  items: BreadcrumbItem[]
  separator?: string
  maxItems?: number
  size?: 'sm' | 'md' | 'lg'
}
```

### 사용 예시

```vue
<script setup lang="ts">
import { Breadcrumb } from '@portal/design-system'
import type { BreadcrumbItem } from '@portal/design-system'

const items: BreadcrumbItem[] = [
  { label: 'Home', href: '/' },
  { label: 'Products', href: '/products' },
  { label: 'Electronics', href: '/products/electronics' },
  { label: 'Smartphones' }, // 마지막 항목은 현재 페이지
]

const routerItems: BreadcrumbItem[] = [
  { label: 'Dashboard', to: { name: 'dashboard' } },
  { label: 'Users', to: { name: 'users' } },
  { label: 'Profile', to: { name: 'user-profile', params: { id: 123 } } },
  { label: 'Settings' },
]

const longPath: BreadcrumbItem[] = [
  { label: 'Home', href: '/' },
  { label: 'Products', href: '/products' },
  { label: 'Electronics', href: '/products/electronics' },
  { label: 'Computers', href: '/products/electronics/computers' },
  { label: 'Laptops', href: '/products/electronics/computers/laptops' },
  { label: 'Gaming Laptops' },
]
</script>

<template>
  <!-- 기본 브레드크럼 -->
  <Breadcrumb :items="items" />

  <!-- Vue Router 사용 -->
  <Breadcrumb :items="routerItems" />

  <!-- 커스텀 구분자 -->
  <Breadcrumb :items="items" separator=">" />

  <!-- 아이콘 구분자 -->
  <Breadcrumb :items="items">
    <template #separator>
      <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
      </svg>
    </template>
  </Breadcrumb>

  <!-- 긴 경로 축약 (maxItems) -->
  <Breadcrumb :items="longPath" :max-items="3" />
  <!-- 결과: Home / ... / Laptops / Gaming Laptops -->

  <!-- 크기 변형 -->
  <div class="space-y-4">
    <Breadcrumb :items="items" size="sm" />
    <Breadcrumb :items="items" size="md" />
    <Breadcrumb :items="items" size="lg" />
  </div>

  <!-- 커스텀 항목 렌더링 -->
  <Breadcrumb :items="items">
    <template #item="{ item, isLast }">
      <span :class="{ 'font-bold': isLast }">
        {{ item.label }}
      </span>
    </template>
  </Breadcrumb>
</template>
```

### maxItems 동작 방식

`maxItems`를 설정하면 긴 경로를 축약하여 표시합니다:

| maxItems | 입력 (6개) | 출력 |
|----------|-----------|------|
| (없음) | Home / A / B / C / D / Current | 모두 표시 |
| `3` | Home / A / B / C / D / Current | Home / ... / D / Current |
| `4` | Home / A / B / C / D / Current | Home / ... / C / D / Current |

- **첫 번째 항목**은 항상 표시
- **마지막 (maxItems - 1)개 항목**을 표시
- 생략된 부분은 클릭 가능한 `...` 버튼으로 표시 (클릭 시 전체 경로 펼침)

### 링크 동작

| 조건 | 렌더링 | 클릭 동작 |
|------|--------|----------|
| `to` prop 있음 | `<router-link>` | Vue Router 내비게이션 |
| `href` prop 있음 | `<a>` | 일반 링크 이동 |
| 마지막 항목 | `<span>` | 클릭 불가 (현재 페이지) |

### 접근성

- `<nav aria-label="Breadcrumb">` 사용
- 마지막 항목에 `aria-current="page"` 적용
- 구분자에 `aria-hidden="true"` 적용 (스크린 리더에서 숨김)
- ellipsis 버튼에 `aria-label="Show more breadcrumbs"` 적용
- 키보드 포커스 링 제공 (`focus:ring-2`)

### 반응형 처리

```vue
<template>
  <!-- 모바일에서 축약 -->
  <Breadcrumb
    :items="items"
    :max-items="2"
    class="md:hidden"
  />

  <!-- 데스크탑에서 전체 표시 -->
  <Breadcrumb
    :items="items"
    class="hidden md:block"
  />
</template>
```

---

## 🔗 관련 문서

- [레이아웃 컴포넌트](./components-layout.md) - Card, Container 등
- [입력 컴포넌트](./components-input.md) - Button, Input, Select 등
- [컴포넌트 사용 가이드](../guides/using-components.md)

---

**최종 업데이트**: 2026-02-06
