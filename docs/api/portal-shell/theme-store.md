---
id: api-portal-shell-theme-store
title: Portal Shell Theme Store
type: api
status: current
version: v2
created: 2026-01-18
updated: 2026-02-06
author: Documenter Agent
tags: [api, portal-shell, pinia, theme, dark-mode, system-theme, module-federation]
related:
  - api-portal-shell-auth-store
  - api-portal-shell-store-adapter
---

# Portal Shell Theme Store

> Module Federation을 통해 Remote 모듈에 제공되는 테마 상태 관리 Pinia Store

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **Module Federation Path** | `portal/stores` |
| **Export 이름** | `useThemeStore` |
| **Store 라이브러리** | Pinia |
| **Store ID** | `theme` |
| **주요 기능** | Light/Dark/System 모드 전환, 테마 설정 저장, 시스템 테마 자동 감지 |
| **기본 테마** | Dark (Linear-inspired) |

---

## 🎯 주요 기능

### 1. 다크 모드 전환
- Light/Dark/System 모드 지원
- `document.documentElement`에 `dark` 또는 `light` 클래스 추가

### 2. 시스템 테마 자동 감지
- `mode: 'system'` 설정 시 OS 테마 자동 반영
- `prefers-color-scheme` 미디어 쿼리 리스너 등록
- 시스템 테마 변경 시 자동 업데이트

### 3. 테마 설정 영속화
- localStorage에 테마 설정 저장 (`theme` 키)
- 페이지 새로고침 시에도 테마 유지

### 4. 초기화
- 앱 시작 시 저장된 테마 설정 복원
- 기본값: Dark 모드 (Linear 스타일)

---

## 📦 타입 정의

### ThemeMode

```typescript
type ThemeMode = 'dark' | 'light' | 'system';
```

테마 모드 타입.

---

### ThemeStore

```typescript
interface ThemeStore {
  // State
  isDark: boolean;
  mode: ThemeMode;

  // Actions
  toggle(): void;
  setMode(mode: ThemeMode): void;
  applyTheme(): void;
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

**주의:** `mode: 'system'`일 때는 OS 설정에 따라 자동으로 변경됨.

---

### mode

```typescript
mode: ThemeMode
```

현재 테마 모드.

- `'dark'`: 강제 다크 모드
- `'light'`: 강제 라이트 모드
- `'system'`: OS 테마 자동 감지

---

## 🔹 Actions

### toggle

```typescript
toggle(): void
```

Light/Dark 모드를 전환합니다.

**동작:**
1. `isDark` 값 반전
2. `mode`를 `'dark'` 또는 `'light'`로 설정
3. `applyTheme()` 호출

**예시:**

```typescript
import { useThemeStore } from 'portal/stores';

const themeStore = useThemeStore();

// 테마 전환
themeStore.toggle();

console.log(themeStore.isDark ? 'Dark 모드' : 'Light 모드');
```

---

### setMode

```typescript
setMode(mode: ThemeMode): void
```

테마 모드를 명시적으로 설정합니다.

**Parameters:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `mode` | `'dark'` \| `'light'` \| `'system'` | ✅ | 설정할 테마 모드 |

**동작:**
1. `mode`를 파라미터로 설정
2. `'system'`이면 OS 테마에 따라 `isDark` 설정
3. 그 외에는 해당 모드로 `isDark` 설정
4. `applyTheme()` 호출

**예시:**

```typescript
import { useThemeStore } from 'portal/stores';

const themeStore = useThemeStore();

// 다크 모드 강제
themeStore.setMode('dark');

// 라이트 모드 강제
themeStore.setMode('light');

// OS 테마 자동 감지
themeStore.setMode('system');
```

---

### applyTheme

```typescript
applyTheme(): void
```

현재 테마를 DOM에 적용하고 localStorage에 저장합니다.

**동작:**
1. `isDark`에 따라 `document.documentElement`에 `dark` 또는 `light` 클래스 추가
2. localStorage에 `mode` 저장

**주의:** 일반적으로 직접 호출할 필요 없음. `toggle()`이나 `setMode()`가 자동으로 호출함.

**예시:**

```typescript
import { useThemeStore } from 'portal/stores';

const themeStore = useThemeStore();

// 직접 호출 (일반적으로 불필요)
themeStore.applyTheme();
```

---

### initialize

```typescript
initialize(): void
```

저장된 테마 설정을 복원하고 시스템 테마 리스너를 등록합니다.

**동작:**
1. localStorage에서 `theme` 값 읽기
2. `'system'`이면 OS 테마에 따라 설정
3. `'light'`이면 라이트 모드로 설정
4. 그 외에는 다크 모드로 설정 (기본값)
5. `applyTheme()` 호출
6. `prefers-color-scheme` 미디어 쿼리 리스너 등록

**예시:**

```typescript
import { useThemeStore } from 'portal/stores';

const themeStore = useThemeStore();

// 앱 시작 시 호출
themeStore.initialize();
```

**주의:** `mode: 'system'`일 때 OS 테마 변경 시 자동으로 업데이트됨.

---

## 🔹 Remote 모듈에서 사용하기

### 1. Vue 3 컴포넌트에서 사용

```vue
<script setup lang="ts">
import { useThemeStore } from 'portal/stores';
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
import { useThemeStore } from 'portal/stores';
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
import { useThemeStore } from 'portal/stores';
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
import { useThemeStore } from 'portal/stores';
const themeStore = useThemeStore();
themeStore.initialize();

app.mount('#app');
```

---

### 테마에 따라 동적 스타일 적용

```vue
<script setup lang="ts">
import { useThemeStore } from 'portal/stores';
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
import { useThemeStore } from 'portal/stores';

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
import { useThemeStore } from 'portal/stores';

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
import { useThemeStore } from 'portal/stores';
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
import { useThemeStore } from 'portal/stores';
```

**이유**: Shell의 themeStore를 사용해야 테마 상태가 전역적으로 동기화됨

---

## 🔗 관련 문서

- [Auth Store API](./auth-store.md) - 인증 상태 관리
- [API Client](./api-client.md) - HTTP 요청 클라이언트
- [Store Adapter](./store-adapter.md) - React 통합용 Adapter

---

**최종 업데이트**: 2026-02-06

---

## 📝 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|-----------|
| v1 | 2026-01-18 | 최초 작성 |
| v2 | 2026-02-06 | ThemeMode 타입 추가, mode state 추가, setMode/applyTheme actions 추가, 시스템 테마 자동 감지 기능 추가 |
