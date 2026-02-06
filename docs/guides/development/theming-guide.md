---
id: guide-theming
title: 테마 적용 가이드
type: guide
status: current
created: 2026-01-18
updated: 2026-01-18
author: Laze
tags: [design-system, theming, dark-mode, css-variables]
related:
  - guide-using-components
  - guide-getting-started
---

# 테마 적용 가이드

**난이도**: ⭐⭐ | **예상 시간**: 20분 | **카테고리**: Development

> Light/Dark 모드 및 서비스별 테마 커스터마이징 가이드

---

## 📋 개요

Design System의 테마 시스템은 다음 기능을 제공합니다:

- **명암 모드**: Light (밝음) / Dark (어두움)
- **서비스별 테마**: Blog (초록) / Shopping (주황) / Portal (민트)
- **동적 전환**: 런타임에 테마 변경 가능
- **시스템 설정 연동**: OS 다크 모드 자동 감지

---

## 🌗 명암 모드 (Light/Dark)

### useTheme Composable API

```vue
<script setup lang="ts">
import { useTheme } from '@portal/design-system'
import type { ThemeMode } from '@portal/design-system'

const {
  currentTheme,      // ref<ThemeMode> ('light' | 'dark')
  currentService,    // ref<ServiceType> ('blog' | 'shopping' | 'portal')
  setTheme,          // (mode: ThemeMode) => void
  toggleTheme,       // () => void - Light ↔ Dark 전환
  setService,        // (service: ServiceType) => void
  initTheme          // () => void - 초기화 (localStorage + 시스템 설정)
} = useTheme()
</script>
```

### Light/Dark 모드 전환 구현

#### 예제 1: 토글 버튼

```vue
<script setup lang="ts">
import { useTheme } from '@portal/design-system'
import { Button } from '@portal/design-system'

const { currentTheme, toggleTheme } = useTheme()
</script>

<template>
  <Button @click="toggleTheme" variant="secondary">
    {{ currentTheme === 'light' ? '🌙 Dark' : '☀️ Light' }}
  </Button>
</template>
```

#### 예제 2: 명시적 설정

```vue
<script setup lang="ts">
import { useTheme } from '@portal/design-system'

const { setTheme } = useTheme()

const switchToLight = () => setTheme('light')
const switchToDark = () => setTheme('dark')
</script>

<template>
  <div class="flex gap-2">
    <button @click="switchToLight">Light</button>
    <button @click="switchToDark">Dark</button>
  </div>
</template>
```

### 초기화 (루트 컴포넌트)

```vue
<!-- src/App.vue -->
<script setup lang="ts">
import { onMounted } from 'vue'
import { useTheme } from '@portal/design-system'

const { initTheme } = useTheme()

onMounted(() => {
  // 1. localStorage에서 저장된 테마 복원
  // 2. 없으면 시스템 설정(prefers-color-scheme) 반영
  // 3. 시스템 설정 변경 감지 리스너 등록
  initTheme()
})
</script>

<template>
  <router-view />
</template>
```

---

## 🎨 서비스별 테마

### Blog 서비스 (초록 강조)

**브랜드 색**: `#20C997` (Mantine Green)

**특징**:
- 긴 글 읽기 최적화 (큰 폰트, 넓은 줄 간격)
- 코드 블록 하이라이팅
- Blockquote 강조

**설정 예제**:
```vue
<script setup>
import { useTheme } from '@portal/design-system'

const { setService } = useTheme()
setService('blog')
</script>
```

### Shopping 서비스 (주황 강조)

**브랜드 색**: `#FF922B` (Mantine Orange)

**특징**:
- 역동적인 그림자 효과
- 가격 정보 강조
- 액션 버튼 강조

**설정 예제**:
```vue
<script setup>
import { useTheme } from '@portal/design-system'

const { setService } = useTheme()
setService('shopping')
</script>
```

---

## 🔄 Module Federation 환경에서 테마 전환

### Portal Shell에서 라우트 기반 서비스 전환

```vue
<!-- portal-shell/src/App.vue -->
<script setup lang="ts">
import { watch } from 'vue'
import { useRoute } from 'vue-router'
import { useTheme } from '@portal/design-system'

const route = useRoute()
const { setService } = useTheme()

// 라우트 변경 시 서비스 테마 자동 전환
watch(
  () => route.path,
  (newPath) => {
    if (newPath.includes('/blog')) {
      setService('blog')
    } else if (newPath.includes('/shopping')) {
      setService('shopping')
    } else {
      setService('portal')
    }
  },
  { immediate: true }
)
</script>

<template>
  <router-view />
</template>
```

### Remote Module에서 Standalone 모드 처리

```vue
<!-- blog-frontend/src/App.vue -->
<script setup lang="ts">
import { onMounted } from 'vue'
import { useTheme } from '@portal/design-system'

onMounted(() => {
  // Standalone 모드에서만 수동 설정
  if (import.meta.env.MODE === 'standalone') {
    const { setService, initTheme } = useTheme()
    initTheme()
    setService('blog')
  }
  // Portal Shell에서는 이미 설정됨
})
</script>
```

---

## 🛠️ Tailwind CSS 통합

### tailwind.config.js 설정

```javascript
export default {
  darkMode: ['class', '[data-theme="dark"]'],
  theme: {
    extend: {
      colors: {
        'brand-primary': 'var(--semantic-brand-primary)',
        'brand-primaryHover': 'var(--semantic-brand-primaryHover)',
        'text-heading': 'var(--semantic-text-heading)',
        'text-body': 'var(--semantic-text-body)',
        'bg-page': 'var(--semantic-bg-page)',
        'bg-card': 'var(--semantic-bg-card)',
      }
    }
  }
}
```

### Tailwind 클래스 사용

```vue
<template>
  <!-- Light: 흰 배경, Dark: 어두운 배경 -->
  <div class="bg-page text-body">
    <div class="bg-card border border-default p-4">
      <h1 class="text-heading">제목</h1>
      <p class="text-body">본문</p>
      <span class="text-meta">메타 정보</span>
    </div>
  </div>
</template>
```

---

## 💡 베스트 프랙티스

### 1. Semantic 변수 사용

```vue
<!-- ✗ 나쁜 예: Base 토큰 직접 사용 -->
<div style="color: var(--base-color-gray-900)">

<!-- ✓ 좋은 예: Semantic 토큰 사용 -->
<div class="text-heading">
```

### 2. 컴포넌트 Props 우선

```vue
<!-- ✗ 나쁜 예: 스타일 직접 오버라이드 -->
<Button class="bg-red-500">삭제</Button>

<!-- ✓ 좋은 예: Props 사용 -->
<Button variant="danger">삭제</Button>
```

### 3. 테마 초기화는 루트에서만

```vue
<!-- ✗ 나쁜 예: 여러 컴포넌트에서 initTheme() -->
<script setup>
// child-component.vue에서 initTheme() 호출 (X)
</script>

<!-- ✓ 좋은 예: App.vue에서만 초기화 -->
<script setup>
// App.vue에서 onMounted(() => initTheme())
</script>
```

---

## 🔗 관련 문서

- [컴포넌트 사용 가이드](./using-components.md) - 테마가 적용된 컴포넌트 사용법
- [API Reference](../../api/README.md) - CSS 변수 전체 목록
- [Architecture](../../architecture/design-system/theming.md) - 테마 시스템 아키텍처

---

**최종 업데이트**: 2026-01-18
