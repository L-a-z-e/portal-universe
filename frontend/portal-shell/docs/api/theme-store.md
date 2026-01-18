---
id: api-portal-shell-theme-store
title: Portal Shell Theme Store
type: api
status: current
version: v1
created: 2026-01-18
updated: 2026-01-18
author: Documenter Agent
tags: [api, portal-shell, pinia, theme, dark-mode, module-federation]
related:
  - api-portal-shell-auth-store
---

# Portal Shell Theme Store

> Module Federation을 통해 Remote 모듈에 제공되는 테마 상태 관리 Pinia Store

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **Module Federation Path** | `portal-shell/themeStore` |
| **Store 라이브러리** | Pinia |
| **Store ID** | `theme` |
| **주요 기능** | Light/Dark 모드 전환, 테마 설정 저장 |

---

## 🎯 주요 기능

### 1. 다크 모드 전환
- Light/Dark 모드 토글
- `document.documentElement`에 `dark` 클래스 추가/제거

### 2. 테마 설정 영속화
- localStorage에 테마 설정 저장
- 페이지 새로고침 시에도 테마 유지

### 3. 초기화
- 앱 시작 시 저장된 테마 설정 복원

---

## 📦 타입 정의

```typescript
interface ThemeStore {
  // State
  isDark: boolean;

  // Actions
  toggle(): void;
  initialize(): void;
}
```

---

## 🔹 State

### isDark

```typescript
isDark: boolean
```

현재 다크 모드 여부.

- `true`: Dark 모드
- `false`: Light 모드

---

## 🔹 Actions

### toggle

```typescript
toggle(): void
```

Light/Dark 모드를 전환합니다.

**동작:**
1. `isDark` 값 반전
2. `document.documentElement`에 `dark` 클래스 추가/제거
3. localStorage에 테마 설정 저장 (`theme` 키)

**예시:**

```typescript
import { useThemeStore } from 'portal-shell/themeStore';

const themeStore = useThemeStore();

// 테마 전환
themeStore.toggle();

console.log(themeStore.isDark ? 'Dark 모드' : 'Light 모드');
```

---

### initialize

```typescript
initialize(): void
```

저장된 테마 설정을 복원합니다.

**동작:**
1. localStorage에서 `theme` 값 읽기
2. `'dark'`이면 다크 모드로 설정
3. 그 외에는 라이트 모드로 설정

**예시:**

```typescript
import { useThemeStore } from 'portal-shell/themeStore';

const themeStore = useThemeStore();

// 앱 시작 시 호출
themeStore.initialize();
```

---

## 🔹 Remote 모듈에서 사용하기

### 1. Vue 3 컴포넌트에서 사용

```vue
<script setup lang="ts">
import { useThemeStore } from 'portal-shell/themeStore';
import { onMounted } from 'vue';

const themeStore = useThemeStore();

// 컴포넌트 마운트 시 테마 초기화
onMounted(() => {
  themeStore.initialize();
});

const toggleTheme = () => {
  themeStore.toggle();
};
</script>

<template>
  <div>
    <button @click="toggleTheme">
      {{ themeStore.isDark ? '🌙 다크 모드' : '☀️ 라이트 모드' }}
    </button>
  </div>
</template>
```

---

### 2. 테마 전환 버튼 컴포넌트

```vue
<script setup lang="ts">
import { useThemeStore } from 'portal-shell/themeStore';
import { computed } from 'vue';

const themeStore = useThemeStore();

const icon = computed(() => themeStore.isDark ? '🌙' : '☀️');
const label = computed(() => themeStore.isDark ? 'Dark' : 'Light');
</script>

<template>
  <button
    @click="themeStore.toggle()"
    class="theme-toggle"
    :aria-label="`Switch to ${themeStore.isDark ? 'light' : 'dark'} mode`"
  >
    <span class="icon">{{ icon }}</span>
    <span class="label">{{ label }}</span>
  </button>
</template>

<style scoped>
.theme-toggle {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border: 1px solid var(--border-color);
  border-radius: 0.375rem;
  background: var(--bg-secondary);
  cursor: pointer;
  transition: all 0.2s;
}

.theme-toggle:hover {
  background: var(--bg-hover);
}
</style>
```

---

### 3. Composable로 추상화

```typescript
// blog-frontend/src/composables/useTheme.ts
import { useThemeStore } from 'portal-shell/themeStore';
import { computed } from 'vue';

export const useTheme = () => {
  const themeStore = useThemeStore();

  const isDark = computed(() => themeStore.isDark);
  const mode = computed(() => themeStore.isDark ? 'dark' : 'light');

  const toggle = () => {
    themeStore.toggle();
  };

  const setDark = () => {
    if (!themeStore.isDark) {
      themeStore.toggle();
    }
  };

  const setLight = () => {
    if (themeStore.isDark) {
      themeStore.toggle();
    }
  };

  return {
    isDark,
    mode,
    toggle,
    setDark,
    setLight,
  };
};
```

**사용 예시:**

```vue
<script setup lang="ts">
import { useTheme } from '@/composables/useTheme';

const { isDark, toggle, setDark, setLight } = useTheme();
</script>

<template>
  <div>
    <button @click="toggle">토글</button>
    <button @click="setDark">다크 모드</button>
    <button @click="setLight">라이트 모드</button>
  </div>
</template>
```

---

## 🔹 사용 예시

### 앱 초기화 시 테마 복원

```typescript
// blog-frontend/src/main.ts
import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);

// 앱 시작 시 테마 초기화
import { useThemeStore } from 'portal-shell/themeStore';
const themeStore = useThemeStore();
themeStore.initialize();

app.mount('#app');
```

---

### 테마에 따라 동적 스타일 적용

```vue
<script setup lang="ts">
import { useThemeStore } from 'portal-shell/themeStore';
import { computed } from 'vue';

const themeStore = useThemeStore();

const backgroundColor = computed(() =>
  themeStore.isDark ? '#1a1a1a' : '#ffffff'
);

const textColor = computed(() =>
  themeStore.isDark ? '#ffffff' : '#000000'
);
</script>

<template>
  <div
    class="container"
    :style="{
      backgroundColor,
      color: textColor
    }"
  >
    <h1>컨텐츠</h1>
  </div>
</template>
```

---

### 네비게이션 바에 테마 전환 버튼 추가

```vue
<script setup lang="ts">
import { useThemeStore } from 'portal-shell/themeStore';

const themeStore = useThemeStore();
</script>

<template>
  <nav class="navbar">
    <div class="nav-left">
      <router-link to="/">홈</router-link>
      <router-link to="/posts">게시물</router-link>
    </div>

    <div class="nav-right">
      <button @click="themeStore.toggle()" class="theme-btn">
        {{ themeStore.isDark ? '🌙' : '☀️' }}
      </button>
    </div>
  </nav>
</template>

<style scoped>
.navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 2rem;
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border-color);
}

.theme-btn {
  font-size: 1.5rem;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: transform 0.2s;
}

.theme-btn:hover {
  transform: scale(1.2);
}
</style>
```

---

## 🎨 TailwindCSS와 함께 사용

### TailwindCSS Dark Mode 설정

```javascript
// tailwind.config.js
module.exports = {
  darkMode: 'class', // class 기반 다크 모드
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {},
  },
  plugins: [],
};
```

### 컴포넌트에서 사용

```vue
<script setup lang="ts">
import { useThemeStore } from 'portal-shell/themeStore';

const themeStore = useThemeStore();
</script>

<template>
  <div class="bg-white dark:bg-gray-900 text-black dark:text-white">
    <h1 class="text-2xl font-bold">제목</h1>
    <p class="text-gray-600 dark:text-gray-400">설명</p>

    <button
      @click="themeStore.toggle()"
      class="px-4 py-2 bg-blue-500 hover:bg-blue-600 dark:bg-blue-700 dark:hover:bg-blue-800 text-white rounded"
    >
      테마 전환
    </button>
  </div>
</template>
```

---

## ⚠️ 주의사항

### 1. initialize() 호출 시점

```typescript
// ❌ 나쁜 예: 컴포넌트마다 initialize() 호출
// Component A
onMounted(() => themeStore.initialize());

// Component B
onMounted(() => themeStore.initialize());

// ✅ 좋은 예: 앱 초기화 시 한 번만 호출
// main.ts
import { useThemeStore } from 'portal-shell/themeStore';
const themeStore = useThemeStore();
themeStore.initialize();
```

**이유**: 중복 호출은 불필요하며, 앱 시작 시 한 번만 호출해야 함

---

### 2. SSR 환경에서 주의

```typescript
// ❌ 나쁜 예: 서버 사이드에서 접근 시도
const themeStore = useThemeStore();
themeStore.initialize(); // document, localStorage 접근 불가

// ✅ 좋은 예: 클라이언트에서만 호출
import { onMounted } from 'vue';

onMounted(() => {
  const themeStore = useThemeStore();
  themeStore.initialize();
});
```

**이유**: `document.documentElement`와 `localStorage`는 브라우저에서만 접근 가능

---

### 3. Remote 모듈에서 독자적인 Theme Store 생성 금지

```typescript
// ❌ 나쁜 예: Remote에서 독립된 theme store 생성
import { defineStore } from 'pinia';

export const useMyThemeStore = defineStore('myTheme', {
  // ...
});

// ✅ 좋은 예: Shell의 themeStore 사용
import { useThemeStore } from 'portal-shell/themeStore';
```

**이유**: Shell의 themeStore를 사용해야 테마 상태가 전역적으로 동기화됨

---

## 🔗 관련 문서

- [Auth Store API](./auth-store.md) - 인증 상태 관리
- [API Client](./api-client.md) - HTTP 요청 클라이언트

---

**최종 업데이트**: 2026-01-18
