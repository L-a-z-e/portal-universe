---
id: api-components-data-display
title: 데이터 표시 컴포넌트 API
type: api
status: current
created: 2026-02-06
updated: 2026-02-06
author: Laze
tags: [design-system, api, data-display, components, vue3]
related:
  - api-design-system
  - guide-using-components
---

# 데이터 표시 컴포넌트 API

> Avatar

---

## 📋 개요

데이터 표시 컴포넌트는 사용자 정보와 상태를 시각적으로 표현합니다.

| 컴포넌트 | 용도 | 유형 |
|---------|------|------|
| Avatar | 사용자 프로필 이미지 | 이미지 |

---

## 1️⃣ Avatar

사용자 프로필 이미지 표시 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `src` | `string` | - | ❌ | 이미지 URL |
| `alt` | `string` | - | ❌ | 이미지 대체 텍스트 |
| `name` | `string` | - | ❌ | 사용자 이름 (fallback 이니셜 생성) |
| `size` | `'xs' \| 'sm' \| 'md' \| 'lg' \| 'xl' \| '2xl'` | `'md'` | ❌ | 아바타 크기 |
| `status` | `'online' \| 'offline' \| 'busy' \| 'away'` | - | ❌ | 상태 표시 점 |
| `shape` | `'circle' \| 'square'` | `'circle'` | ❌ | 아바타 모양 |

### Events

없음

### Slots

없음

### TypeScript Interface

```typescript
export interface AvatarProps {
  src?: string
  alt?: string
  name?: string
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl'
  status?: 'online' | 'offline' | 'busy' | 'away'
  shape?: 'circle' | 'square'
}
```

### 기능

- **이미지 로드 실패 시 자동 Fallback**: 이미지를 불러올 수 없는 경우 `name`에서 이니셜 추출
- **이니셜 생성 규칙**:
  - 이름이 2단어 이상: 첫 단어와 마지막 단어의 첫 글자 (예: "John Doe" → "JD")
  - 이름이 1단어: 처음 2글자 (예: "Jane" → "JA")
  - 이름이 없으면: "?"
- **상태 표시**: 온라인/오프라인/바쁨/자리비움 상태를 우측 하단에 점으로 표시
- **반응형 크기**: 6가지 크기 지원 (xs ~ 2xl)
- **모양 선택**: 원형(circle) 또는 둥근 사각형(square)

### 사용 예시

#### 기본 아바타

```vue
<script setup lang="ts">
import { Avatar } from '@portal/design-system'
</script>

<template>
  <!-- 이미지가 있는 아바타 -->
  <Avatar
    src="https://example.com/user.jpg"
    alt="John Doe"
    name="John Doe"
  />

  <!-- 이미지 없이 이니셜만 -->
  <Avatar name="Jane Smith" />

  <!-- 크기 변형 -->
  <Avatar name="User" size="xs" />
  <Avatar name="User" size="sm" />
  <Avatar name="User" size="md" />
  <Avatar name="User" size="lg" />
  <Avatar name="User" size="xl" />
  <Avatar name="User" size="2xl" />
</template>
```

#### 상태 표시가 있는 아바타

```vue
<script setup lang="ts">
import { Avatar } from '@portal/design-system'

interface User {
  id: string
  name: string
  avatar?: string
  status: 'online' | 'offline' | 'busy' | 'away'
}

const users: User[] = [
  { id: '1', name: 'John Doe', avatar: '/john.jpg', status: 'online' },
  { id: '2', name: 'Jane Smith', status: 'busy' },
  { id: '3', name: 'Bob Johnson', status: 'away' },
  { id: '4', name: 'Alice Lee', status: 'offline' },
]
</script>

<template>
  <div class="flex gap-4">
    <Avatar
      v-for="user in users"
      :key="user.id"
      :src="user.avatar"
      :name="user.name"
      :status="user.status"
      size="lg"
    />
  </div>
</template>
```

#### 이미지 로드 실패 Fallback

```vue
<script setup lang="ts">
import { Avatar } from '@portal/design-system'
</script>

<template>
  <!-- 잘못된 URL → 자동으로 이니셜 표시 -->
  <Avatar
    src="https://invalid-url.com/404.jpg"
    name="John Doe"
  />
  <!-- 결과: "JD" 표시 -->

  <!-- 한 단어 이름 -->
  <Avatar name="Jane" />
  <!-- 결과: "JA" 표시 -->

  <!-- 이름 없음 -->
  <Avatar />
  <!-- 결과: "?" 표시 -->
</template>
```

#### 아바타 그룹 (사용자 목록)

```vue
<script setup lang="ts">
import { Avatar, Card } from '@portal/design-system'

const teamMembers = [
  { name: 'Sarah Connor', avatar: '/sarah.jpg', status: 'online' },
  { name: 'Kyle Reese', status: 'online' },
  { name: 'John Connor', avatar: '/john.jpg', status: 'away' },
  { name: 'Miles Dyson', status: 'offline' },
]
</script>

<template>
  <Card>
    <h3 class="text-lg font-semibold mb-4">팀 멤버</h3>

    <!-- 가로로 겹친 아바타 -->
    <div class="flex -space-x-2">
      <Avatar
        v-for="(member, i) in teamMembers"
        :key="i"
        :src="member.avatar"
        :name="member.name"
        :status="member.status"
        size="md"
        class="ring-2 ring-bg-card"
      />
      <div
        class="w-10 h-10 rounded-full bg-text-muted/20 flex items-center justify-center text-sm text-text-muted ring-2 ring-bg-card"
      >
        +5
      </div>
    </div>

    <!-- 세로 목록 -->
    <div class="mt-6 space-y-3">
      <div
        v-for="(member, i) in teamMembers"
        :key="i"
        class="flex items-center gap-3"
      >
        <Avatar
          :src="member.avatar"
          :name="member.name"
          :status="member.status"
          size="sm"
        />
        <div>
          <p class="text-sm font-medium">{{ member.name }}</p>
          <p class="text-xs text-text-muted capitalize">{{ member.status }}</p>
        </div>
      </div>
    </div>
  </Card>
</template>
```

#### 사각형 아바타 (앱 아이콘용)

```vue
<script setup lang="ts">
import { Avatar } from '@portal/design-system'

const apps = [
  { name: 'Slack', icon: '/slack.png' },
  { name: 'GitHub', icon: '/github.png' },
  { name: 'Notion', icon: '/notion.png' },
]
</script>

<template>
  <div class="flex gap-4">
    <Avatar
      v-for="app in apps"
      :key="app.name"
      :src="app.icon"
      :name="app.name"
      shape="square"
      size="lg"
    />
  </div>
</template>
```

### 접근성 (Accessibility)

- ✅ `alt` 속성이 제공되면 이미지에 적용
- ✅ `alt`가 없으면 `name`을 대체 텍스트로 사용
- ✅ `status` 속성이 있으면 `title` 속성으로 스크린 리더에 상태 전달
- ✅ `loading="lazy"`로 성능 최적화

**권장사항:**
- 사용자 이름이 있으면 항상 `name` prop 제공
- 상태 표시 사용 시 주변에 텍스트로도 상태 명시 (스크린 리더 고려)

```vue
<!-- ✅ 좋은 예 -->
<div class="flex items-center gap-2">
  <Avatar name="John Doe" status="online" />
  <span class="text-sm">
    John Doe
    <span class="text-text-muted">(온라인)</span>
  </span>
</div>

<!-- ❌ 나쁜 예 -->
<Avatar status="online" />
<!-- 이름 없음, 상태만 표시 -->
```

### 크기 가이드

| Size | Dimensions | 용도 |
|------|------------|------|
| `xs` | 24×24px | 인라인 텍스트, 댓글 |
| `sm` | 32×32px | 목록 항목 |
| `md` | 40×40px | 기본 사이즈 |
| `lg` | 48×48px | 카드 헤더 |
| `xl` | 64×64px | 프로필 페이지 |
| `2xl` | 80×80px | 대형 프로필, 히어로 섹션 |

### 상태 색상

| Status | Color | 의미 |
|--------|-------|------|
| `online` | 초록색 | 온라인 상태 |
| `offline` | 회색 | 오프라인 |
| `busy` | 빨간색 | 바쁨 (방해 금지) |
| `away` | 노란색 | 자리 비움 |

---

## 🔗 관련 문서

- [피드백 컴포넌트](./components-feedback.md) - Badge, Tag 등
- [입력 컴포넌트](./components-input.md) - Button, Input, Select 등
- [레이아웃 컴포넌트](./components-layout.md) - Card, Container, Stack 등

---

**최종 업데이트**: 2026-02-06
