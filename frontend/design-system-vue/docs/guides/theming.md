---
id: theming-vue
title: Theming Guide
type: guide
status: current
created: 2026-01-19
updated: 2026-01-19
author: documenter
tags: [guide, vue, theming]
related:
  - getting-started-vue
  - composables-vue
---

# Theming Guide

Design System Vue에서 테마를 사용하고 커스터마이징하는 방법을 안내합니다.

## 테마 시스템 개요

Design System은 두 가지 테마 축을 제공합니다:

1. **서비스 테마** (`data-service`): 서비스별 브랜드 색상
2. **다크/라이트 모드** (`data-theme`): 밝기 모드

```html
<body data-service="portal" data-theme="dark">
  <!-- Portal 서비스, 다크 모드 -->
</body>
```

## 기본 테마 설정

### 정적 설정

```vue
<!-- App.vue -->
<script setup lang="ts">
import { onMounted } from 'vue';

onMounted(() => {
  // 서비스 테마 설정
  document.body.dataset.service = 'blog';

  // 다크/라이트 모드 설정
  document.body.dataset.theme = 'light';
});
</script>

<template>
  <router-view />
</template>
```

### useTheme Composable 사용

```vue
<script setup lang="ts">
import { useTheme } from '@portal/design-system-vue';
import { onMounted } from 'vue';

const { theme, service, setTheme, setService } = useTheme();

onMounted(() => {
  // 저장된 설정 복원
  const savedTheme = localStorage.getItem('theme') as 'light' | 'dark';
  const savedService = localStorage.getItem('service');

  if (savedTheme) setTheme(savedTheme);
  if (savedService) setService(savedService);
});
</script>
```

## 서비스 테마

### 사용 가능한 서비스

| Service | Primary Color | 특징 |
|---------|---------------|------|
| `portal` | Indigo (#5e6ad2) | Linear 스타일 다크 테마 (기본) |
| `blog` | Green (#12B886) | 그린 계열 라이트 테마 |
| `shopping` | Orange (#FD7E14) | 오렌지 계열 라이트 테마 |

### 서비스 전환

```vue
<script setup lang="ts">
import { useTheme } from '@portal/design-system-vue';

const { service, setService } = useTheme();
</script>

<template>
  <div class="flex gap-2">
    <DsButton
      :variant="service === 'portal' ? 'primary' : 'ghost'"
      @click="setService('portal')"
    >
      Portal
    </DsButton>
    <DsButton
      :variant="service === 'blog' ? 'primary' : 'ghost'"
      @click="setService('blog')"
    >
      Blog
    </DsButton>
    <DsButton
      :variant="service === 'shopping' ? 'primary' : 'ghost'"
      @click="setService('shopping')"
    >
      Shopping
    </DsButton>
  </div>
</template>
```

## 다크/라이트 모드

### 토글 버튼

```vue
<script setup lang="ts">
import { useTheme } from '@portal/design-system-vue';

const { theme, toggleTheme } = useTheme();
</script>

<template>
  <DsButton variant="ghost" @click="toggleTheme">
    <IconSun v-if="theme === 'dark'" class="w-5 h-5" />
    <IconMoon v-else class="w-5 h-5" />
    <span class="ml-2">{{ theme === 'dark' ? '라이트 모드' : '다크 모드' }}</span>
  </DsButton>
</template>
```

### 시스템 설정 따르기

```ts
import { useTheme } from '@portal/design-system-vue';

const { setTheme } = useTheme();

// 초기 설정
const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
setTheme(prefersDark ? 'dark' : 'light');

// 시스템 변경 감지
window.matchMedia('(prefers-color-scheme: dark)')
  .addEventListener('change', (e) => {
    setTheme(e.matches ? 'dark' : 'light');
  });
```

### 설정 저장

```vue
<script setup lang="ts">
import { useTheme } from '@portal/design-system-vue';
import { watch } from 'vue';

const { theme, service } = useTheme();

// 변경 시 저장
watch(theme, (newTheme) => {
  localStorage.setItem('theme', newTheme);
});

watch(service, (newService) => {
  localStorage.setItem('service', newService);
});
</script>
```

## 커스텀 테마

### CSS 변수 오버라이드

```css
/* 전역 오버라이드 */
:root {
  --semantic-brand-primary: #8b5cf6;
  --semantic-brand-primaryHover: #7c3aed;
}

/* 특정 서비스 오버라이드 */
[data-service="blog"] {
  --semantic-brand-primary: #06b6d4;
}

/* 특정 모드 오버라이드 */
[data-theme="dark"] {
  --semantic-bg-page: #0a0a0a;
}
```

### 커스텀 서비스 추가

```css
/* 새로운 서비스 테마 */
[data-service="custom"] {
  --semantic-brand-primary: #8b5cf6;
  --semantic-brand-primaryHover: #7c3aed;
  --semantic-brand-secondary: #a78bfa;

  --semantic-bg-page: #faf5ff;
  --semantic-bg-card: #ffffff;
  --semantic-text-body: #1e1b4b;
}

[data-service="custom"][data-theme="dark"] {
  --semantic-bg-page: #0f0d1a;
  --semantic-bg-card: #1a1625;
  --semantic-text-body: #e9d5ff;
}
```

### 컴포넌트 레벨 테마

```vue
<template>
  <div class="custom-section">
    <DsCard>
      Custom themed content
    </DsCard>
  </div>
</template>

<style scoped>
.custom-section {
  --semantic-brand-primary: #ec4899;
  --semantic-bg-card: #fdf2f8;
}
</style>
```

## 테마 컴포넌트

### ThemeProvider

```vue
<!-- ThemeProvider.vue -->
<script setup lang="ts">
import { provide, ref, watch } from 'vue';

interface Props {
  initialTheme?: 'light' | 'dark';
  initialService?: string;
}

const props = withDefaults(defineProps<Props>(), {
  initialTheme: 'dark',
  initialService: 'portal',
});

const theme = ref(props.initialTheme);
const service = ref(props.initialService);

watch([theme, service], ([t, s]) => {
  document.body.dataset.theme = t;
  document.body.dataset.service = s;
}, { immediate: true });

provide('theme', { theme, service });
</script>

<template>
  <slot />
</template>
```

### 사용

```vue
<!-- App.vue -->
<template>
  <ThemeProvider initialTheme="dark" initialService="blog">
    <router-view />
  </ThemeProvider>
</template>
```

## 테마 전환 애니메이션

### CSS 트랜지션

```css
body {
  transition: background-color 0.3s ease, color 0.3s ease;
}

/* 또는 특정 속성만 */
.ds-card {
  transition: background-color 0.3s ease,
              border-color 0.3s ease,
              box-shadow 0.3s ease;
}
```

### View Transitions API (실험적)

```ts
async function toggleThemeWithTransition() {
  if (!document.startViewTransition) {
    toggleTheme();
    return;
  }

  await document.startViewTransition(() => {
    toggleTheme();
  }).ready;
}
```

## 모범 사례

### 1. Semantic 토큰 사용

```css
/* Bad: 하드코딩된 색상 */
.card {
  background: #1f2937;
  color: #f3f4f6;
}

/* Good: Semantic 토큰 */
.card {
  background: var(--semantic-bg-card);
  color: var(--semantic-text-body);
}
```

### 2. 테마 지속성

```ts
// 앱 시작 시 복원
const savedTheme = localStorage.getItem('theme');
if (savedTheme) {
  document.body.dataset.theme = savedTheme;
}
```

### 3. 접근성 고려

```ts
// 색상 대비 확인
// 모든 텍스트는 WCAG 2.1 AA 기준 충족
// - 일반 텍스트: 4.5:1 대비
// - 큰 텍스트: 3:1 대비
```

### 4. 초기 깜빡임 방지

```html
<!-- index.html -->
<script>
  // 렌더링 전 테마 적용
  const theme = localStorage.getItem('theme') || 'dark';
  const service = localStorage.getItem('service') || 'portal';
  document.body.dataset.theme = theme;
  document.body.dataset.service = service;
</script>
```
