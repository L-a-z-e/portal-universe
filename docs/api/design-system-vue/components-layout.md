---
id: api-components-layout
title: 레이아웃 컴포넌트 API
type: api
status: current
created: 2026-01-18
updated: 2026-02-06
author: documenter
tags: [design-system, api, layout, components, vue3]
related:
  - api-design-system
  - guide-using-components
---

# 레이아웃 컴포넌트 API

> Card, Container, Stack, Divider

---

## 📋 개요

레이아웃 컴포넌트는 UI 요소의 배치와 구조를 정의합니다.

| 컴포넌트 | 용도 | 유형 |
|---------|------|------|
| Card | 콘텐츠 카드 | 컨테이너 |
| Container | 페이지 래퍼 | 컨테이너 |
| Stack | 플렉스 레이아웃 | 레이아웃 |
| Divider | 구분선 | 유틸리티 |

---

## 1️⃣ Card

콘텐츠 카드 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `variant` | `CardVariant` | `'elevated'` | ❌ | 카드 스타일 변형 |
| `padding` | `PaddingSize` | `'md'` | ❌ | 내부 여백 |
| `hoverable` | `boolean` | `false` | ❌ | 호버 효과 |

### CardVariant Type

```typescript
type CardVariant =
  | 'elevated'
  | 'outlined'
  | 'flat'
  | 'glass'
  | 'interactive'
```

### PaddingSize Type

```typescript
type PaddingSize =
  | 'none'
  | 'sm'
  | 'md'
  | 'lg'
  | 'xl'
```

### Slots

| Slot | Description |
|------|-------------|
| `default` | 카드 콘텐츠 |

### TypeScript Interface

```typescript
interface CardProps {
  variant?: CardVariant
  padding?: PaddingSize
  hoverable?: boolean
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
    <h3 class="text-lg font-semibold mb-2">카드 제목</h3>
    <p class="text-body">카드 본문 내용이 들어갑니다.</p>
    <Button size="sm" class="mt-4">자세히 보기</Button>
  </Card>

  <!-- 패딩 없는 카드 (이미지용) -->
  <Card padding="none">
    <img src="/image.jpg" alt="Card Image" class="w-full h-48 object-cover rounded-t-xl" />
    <div class="p-4">
      <Badge variant="success" class="mb-2">NEW</Badge>
      <h3 class="font-semibold">이미지 카드</h3>
      <p class="text-sm text-meta">이미지가 포함된 카드입니다.</p>
    </div>
  </Card>

  <!-- 호버 효과가 있는 카드 -->
  <Card variant="elevated" hoverable>
    <h3>호버 가능한 카드</h3>
    <p>마우스를 올려보세요.</p>
  </Card>

  <!-- 인터랙티브 카드 (클릭 가능) -->
  <Card variant="interactive">
    <h3>클릭 가능한 카드</h3>
    <p>interactive variant는 자동으로 호버 효과를 포함합니다.</p>
  </Card>

  <!-- Glass 효과 카드 -->
  <Card variant="glass">
    <h3>Glass 카드</h3>
    <p>반투명 배경 효과</p>
  </Card>

  <!-- Outlined 카드 -->
  <Card variant="outlined">
    <h3>Outlined 카드</h3>
    <p>테두리만 있는 가벼운 카드</p>
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

페이지 래퍼 컴포넌트 (Polymorphic Component)

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `maxWidth` | `MaxWidth` | `'lg'` | ❌ | 최대 너비 |
| `centered` | `boolean` | `true` | ❌ | 가운데 정렬 |
| `padding` | `Exclude<PaddingSize, 'xl'>` | `'md'` | ❌ | 좌우 여백 |
| `as` | `ContainerElement` | `'div'` | ❌ | 렌더링할 HTML 요소 |

### MaxWidth Type

```typescript
type MaxWidth =
  | 'sm'
  | 'md'
  | 'lg'
  | 'xl'
  | '2xl'
  | 'full'
```

### ContainerElement Type

```typescript
type ContainerElement =
  | 'div'
  | 'section'
  | 'article'
  | 'main'
  | 'aside'
  | 'header'
  | 'footer'
```

### Slots

| Slot | Description |
|------|-------------|
| `default` | 컨테이너 내용 |

### TypeScript Interface

```typescript
interface ContainerProps {
  maxWidth?: MaxWidth
  centered?: boolean
  padding?: Exclude<PaddingSize, 'xl'>
  as?: ContainerElement
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
  <Container maxWidth="md" as="article">
    <article class="prose">
      <h1>블로그 제목</h1>
      <p>블로그 내용...</p>
    </article>
  </Container>

  <!-- 시맨틱 요소로 렌더링 -->
  <Container as="main" maxWidth="xl">
    <h1>메인 콘텐츠</h1>
  </Container>

  <!-- 패딩 없는 컨테이너 -->
  <Container padding="none" maxWidth="2xl">
    <div class="custom-padding">
      <!-- 커스텀 레이아웃 -->
    </div>
  </Container>
</template>
```

---

## 3️⃣ Stack

플렉스 레이아웃 컴포넌트 (Polymorphic Component)

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `direction` | `'horizontal' \| 'vertical'` | `'vertical'` | ❌ | 배치 방향 |
| `gap` | `GapSize` | `'md'` | ❌ | 요소 간 간격 |
| `align` | `Align` | `'stretch'` | ❌ | 교차축 정렬 |
| `justify` | `Justify` | `'start'` | ❌ | 주축 정렬 |
| `wrap` | `boolean` | `false` | ❌ | 줄 바꿈 허용 |
| `as` | `StackElement` | `'div'` | ❌ | 렌더링할 HTML 요소 |

### GapSize Type

```typescript
type GapSize =
  | 'none'
  | 'xs'
  | 'sm'
  | 'md'
  | 'lg'
  | 'xl'
  | '2xl'
```

### Align Type

```typescript
type Align =
  | 'start'
  | 'center'
  | 'end'
  | 'stretch'
  | 'baseline'
```

### Justify Type

```typescript
type Justify =
  | 'start'
  | 'center'
  | 'end'
  | 'between'
  | 'around'
  | 'evenly'
```

### StackElement Type

```typescript
type StackElement =
  | 'div'
  | 'section'
  | 'ul'
  | 'ol'
  | 'nav'
```

### Slots

| Slot | Description |
|------|-------------|
| `default` | 스택 내 요소들 |

### TypeScript Interface

```typescript
interface StackProps {
  direction?: 'horizontal' | 'vertical'
  gap?: GapSize
  align?: Align
  justify?: Justify
  wrap?: boolean
  as?: StackElement
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
| `2xl` | 48px |

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
    <Card v-for="i in 6" :key="i" class="w-[calc(33.33%-1rem)]">
      카드 {{ i }}
    </Card>
  </Stack>

  <!-- 내비게이션으로 렌더링 -->
  <Stack as="nav" direction="horizontal" gap="sm">
    <a href="/home">Home</a>
    <a href="/about">About</a>
    <a href="/contact">Contact</a>
  </Stack>

  <!-- 리스트로 렌더링 -->
  <Stack as="ul" direction="vertical" gap="xs">
    <li>아이템 1</li>
    <li>아이템 2</li>
    <li>아이템 3</li>
  </Stack>
</template>
```

---

## 4️⃣ Divider

구분선 컴포넌트

### Props

| Prop | Type | Default | Required | Description |
|------|------|---------|----------|-------------|
| `orientation` | `'horizontal' \| 'vertical'` | `'horizontal'` | ❌ | 구분선 방향 |
| `variant` | `DividerVariant` | `'solid'` | ❌ | 선 스타일 |
| `color` | `DividerColor` | `'default'` | ❌ | 선 색상 |
| `label` | `string` | - | ❌ | 구분선 내 텍스트 |
| `spacing` | `Exclude<PaddingSize, 'xl'>` | `'md'` | ❌ | 상하/좌우 여백 |

### DividerVariant Type

```typescript
type DividerVariant =
  | 'solid'
  | 'dashed'
  | 'dotted'
```

### DividerColor Type

```typescript
type DividerColor =
  | 'default'
  | 'muted'
  | 'strong'
```

### Slots

| Slot | Description |
|------|-------------|
| `default` | 구분선 내 커스텀 콘텐츠 (label 대체) |

### TypeScript Interface

```typescript
interface DividerProps {
  orientation?: 'horizontal' | 'vertical'
  variant?: DividerVariant
  color?: DividerColor
  label?: string
  spacing?: Exclude<PaddingSize, 'xl'>
}
```

### 사용 예시

```vue
<script setup lang="ts">
import { Divider, Badge } from '@portal/design-system'
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

  <!-- 색상 변형 -->
  <Divider color="muted" />
  <Divider color="strong" />

  <!-- 여백 조정 -->
  <Divider spacing="none" />
  <Divider spacing="sm" />
  <Divider spacing="lg" />

  <!-- 세로 구분선 -->
  <div class="flex items-center gap-4">
    <span>항목 1</span>
    <Divider orientation="vertical" spacing="none" class="h-4" />
    <span>항목 2</span>
    <Divider orientation="vertical" spacing="none" class="h-4" />
    <span>항목 3</span>
  </div>

  <!-- 커스텀 콘텐츠 (슬롯 사용) -->
  <Divider>
    <Badge variant="info">NEW</Badge>
  </Divider>
</template>
```

---

## 🔗 관련 문서

- [입력 컴포넌트](./components-input.md) - Button, Input, Select, FormField 등
- [피드백 컴포넌트](./components-feedback.md) - Modal, Toast, Badge 등
- [내비게이션 컴포넌트](./components-navigation.md) - Breadcrumb, Tabs 등
- [컴포넌트 사용 가이드](../guides/using-components.md)

---

**최종 업데이트**: 2026-02-06
