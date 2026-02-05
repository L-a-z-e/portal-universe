---
id: use-theme
title: useTheme Hook/Composable API
type: api
status: current
created: 2026-02-06
updated: 2026-02-06
author: documenter
tags: [api, vue, react, composable, hook, theme]
related:
  - themes
  - css-variables
---

# useTheme Hook/Composable API

Vue와 React에서 테마를 관리하는 Hook/Composable API입니다.

## 개요

| 항목 | 값 |
|------|-----|
| **Vue 패키지** | `@portal/design-system-vue` |
| **React 패키지** | `@portal/design-system-react` |
| **파일 (Vue)** | `src/composables/useTheme.ts` |
| **파일 (React)** | `src/hooks/useTheme.ts` |

## Vue useTheme Composable

### Import

```vue
<script setup>
import { useTheme } from '@portal/design-system-vue';
</script>
```

### 인터페이스

```typescript
export type ServiceType = 'portal' | 'blog' | 'shopping';
export type ThemeMode = 'light' | 'dark';

export function useTheme() {
  return {
    // 상태 (Ref)
    currentService: Ref<ServiceType>,
    currentTheme: Ref<ThemeMode>,

    // 메서드
    setService: (service: ServiceType) => void,
    setTheme: (mode: ThemeMode) => void,
    toggleTheme: () => void,
    initTheme: () => void,
  };
}
```

### 반환값

| 속성/메서드 | 타입 | 설명 |
|------------|------|------|
| `currentService` | `Ref<ServiceType>` | 현재 서비스 (읽기 전용) |
| `currentTheme` | `Ref<ThemeMode>` | 현재 테마 모드 (읽기 전용) |
| `setService(service)` | `Function` | 서비스 설정 및 DOM/localStorage 업데이트 |
| `setTheme(mode)` | `Function` | 테마 모드 설정 및 DOM/localStorage 업데이트 |
| `toggleTheme()` | `Function` | Light <-> Dark 토글 |
| `initTheme()` | `Function` | localStorage 또는 system preference에서 테마 초기화 |

### 동작 원리

#### 1. `setService(service)`

```typescript
setService('blog');
```

**처리 과정**:
1. `currentService.value = 'blog'`
2. `document.documentElement.setAttribute('data-service', 'blog')`
3. `localStorage.setItem('portal-service', 'blog')`

#### 2. `setTheme(mode)`

```typescript
setTheme('dark');
```

**처리 과정**:
1. `currentTheme.value = 'dark'`
2. `document.documentElement.setAttribute('data-theme', 'dark')`
3. `document.documentElement.classList.add('dark')` (Tailwind)
4. `localStorage.setItem('portal-theme', 'dark')`

#### 3. `toggleTheme()`

```typescript
toggleTheme();
```

**처리 과정**:
- `currentTheme.value === 'light'` → `setTheme('dark')`
- `currentTheme.value === 'dark'` → `setTheme('light')`

#### 4. `initTheme()`

자동으로 `onMounted`에서 호출됩니다.

**초기화 순서**:
1. localStorage에서 `portal-theme` 확인
2. 값이 있으면 → `setTheme(savedTheme)`
3. 없으면 → System preference 확인 (`prefers-color-scheme`)
4. System dark → `setTheme('dark')`, 아니면 `setTheme('light')`
5. localStorage에서 `portal-service` 확인 및 복원

### Vue 사용 예시

#### 기본 사용

```vue
<script setup lang="ts">
import { useTheme } from '@portal/design-system-vue';

const { currentService, currentTheme, setService, setTheme, toggleTheme } = useTheme();
</script>

<template>
  <div>
    <p>Current Service: {{ currentService }}</p>
    <p>Current Theme: {{ currentTheme }}</p>

    <button @click="setService('blog')">Blog</button>
    <button @click="setService('shopping')">Shopping</button>
    <button @click="toggleTheme()">Toggle Theme</button>
  </div>
</template>
```

#### 테마 스위처 컴포넌트

```vue
<script setup lang="ts">
import { useTheme } from '@portal/design-system-vue';

const { currentTheme, toggleTheme } = useTheme();
</script>

<template>
  <button
    @click="toggleTheme()"
    class="p-sm rounded-md bg-bg-card border border-border-default
           hover:border-border-hover transition-normal"
  >
    <span v-if="currentTheme === 'light'">🌙 Dark</span>
    <span v-else>☀️ Light</span>
  </button>
</template>
```

#### 서비스 선택 메뉴

```vue
<script setup lang="ts">
import { useTheme } from '@portal/design-system-vue';
import type { ServiceType } from '@portal/design-system-vue';

const { currentService, setService } = useTheme();

const services: { value: ServiceType; label: string }[] = [
  { value: 'portal', label: 'Portal' },
  { value: 'blog', label: 'Blog' },
  { value: 'shopping', label: 'Shopping' },
];
</script>

<template>
  <select :value="currentService" @change="(e) => setService(e.target.value)">
    <option v-for="service in services" :key="service.value" :value="service.value">
      {{ service.label }}
    </option>
  </select>
</template>
```

### System Preference 감지

`onMounted`에서 자동으로 시스템 테마 변경을 감지합니다.

```typescript
// useTheme 내부 로직
onMounted(() => {
  initTheme();

  const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
  const handleChange = (e: MediaQueryListEvent) => {
    // localStorage에 저장된 값이 없을 때만 시스템 설정 따름
    if (!localStorage.getItem('portal-theme')) {
      setTheme(e.matches ? 'dark' : 'light');
    }
  };

  mediaQuery.addEventListener('change', handleChange);
  return () => {
    mediaQuery.removeEventListener('change', handleChange);
  };
});
```

**동작**:
- 사용자가 명시적으로 테마를 설정하지 않은 경우 (localStorage 없음)
- 시스템 설정이 변경되면 자동으로 테마 업데이트

---

## React useTheme Hook

### Import

```tsx
import { useTheme } from '@portal/design-system-react';
```

### 인터페이스

```typescript
export type ServiceType = 'portal' | 'blog' | 'shopping' | 'prism';
export type ThemeMode = 'light' | 'dark' | 'system';

export interface UseThemeOptions {
  defaultService?: ServiceType;
  defaultMode?: ThemeMode;
}

export interface UseThemeReturn {
  service: ServiceType;
  mode: ThemeMode;
  resolvedMode: 'light' | 'dark';
  setService: (service: ServiceType) => void;
  setMode: (mode: ThemeMode) => void;
  toggleMode: () => void;
}

export function useTheme(options?: UseThemeOptions): UseThemeReturn;
```

### 반환값

| 속성/메서드 | 타입 | 설명 |
|------------|------|------|
| `service` | `ServiceType` | 현재 서비스 |
| `mode` | `ThemeMode` | 현재 테마 모드 (`'light'` \| `'dark'` \| `'system'`) |
| `resolvedMode` | `'light'` \| `'dark'` | 실제 적용되는 모드 (system 해석됨) |
| `setService(service)` | `Function` | 서비스 변경 |
| `setMode(mode)` | `Function` | 테마 모드 변경 |
| `toggleMode()` | `Function` | Light <-> Dark 토글 |

### Vue와의 차이점

| 기능 | Vue | React |
|------|-----|-------|
| **System 모드** | ❌ 없음 | ✅ `mode: 'system'` 지원 |
| **Resolved Mode** | - | ✅ `resolvedMode` 제공 |
| **System Preference 감지** | ✅ 자동 (localStorage 없을 때) | ✅ 자동 (`mode === 'system'`일 때) |
| **LocalStorage** | ✅ 자동 저장 | ❌ 수동 구현 필요 |

### React 사용 예시

#### 기본 사용

```tsx
import { useTheme } from '@portal/design-system-react';

export function App() {
  const { service, mode, resolvedMode, setService, setMode, toggleMode } = useTheme();

  return (
    <div>
      <p>Service: {service}</p>
      <p>Mode: {mode}</p>
      <p>Resolved Mode: {resolvedMode}</p>

      <button onClick={() => setService('blog')}>Blog</button>
      <button onClick={() => setMode('dark')}>Dark</button>
      <button onClick={toggleMode}>Toggle</button>
    </div>
  );
}
```

#### System 모드 사용

```tsx
export function ThemeSwitcher() {
  const { mode, setMode } = useTheme();

  return (
    <div className="flex gap-sm">
      <button
        onClick={() => setMode('light')}
        className={mode === 'light' ? 'active' : ''}
      >
        ☀️ Light
      </button>
      <button
        onClick={() => setMode('dark')}
        className={mode === 'dark' ? 'active' : ''}
      >
        🌙 Dark
      </button>
      <button
        onClick={() => setMode('system')}
        className={mode === 'system' ? 'active' : ''}
      >
        💻 System
      </button>
    </div>
  );
}
```

#### 초기값 설정

```tsx
export function App() {
  const { service, mode } = useTheme({
    defaultService: 'blog',
    defaultMode: 'system',
  });

  return <div>Service: {service}, Mode: {mode}</div>;
}
```

### System Preference 자동 감지

React Hook은 `useEffect`로 시스템 설정을 감지합니다.

```typescript
// useTheme 내부 로직
const [systemMode, setSystemMode] = useState<'light' | 'dark'>('dark');

useEffect(() => {
  const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
  setSystemMode(mediaQuery.matches ? 'dark' : 'light');

  const handler = (e: MediaQueryListEvent) => {
    setSystemMode(e.matches ? 'dark' : 'light');
  };

  mediaQuery.addEventListener('change', handler);
  return () => mediaQuery.removeEventListener('change', handler);
}, []);

// resolvedMode 계산
const resolvedMode = mode === 'system' ? systemMode : mode;
```

### DOM 속성 자동 업데이트

```typescript
// useTheme 내부 로직
useEffect(() => {
  document.documentElement.setAttribute('data-service', service);
  document.documentElement.setAttribute('data-theme', resolvedMode);
}, [service, resolvedMode]);
```

**특징**:
- `service`나 `resolvedMode`가 변경되면 자동으로 DOM 속성 업데이트
- LocalStorage는 자동으로 저장되지 않음 (수동 구현 필요)

---

## LocalStorage 키

| 키 | 값 | 설명 |
|----|-----|------|
| `portal-service` | `'portal'` \| `'blog'` \| `'shopping'` \| `'prism'` | 저장된 서비스 |
| `portal-theme` | `'light'` \| `'dark'` (Vue) <br> `'light'` \| `'dark'` \| `'system'` (React) | 저장된 테마 모드 |

### LocalStorage 수동 구현 (React)

```tsx
import { useTheme } from '@portal/design-system-react';
import { useEffect } from 'react';

export function App() {
  const { service, mode, setService, setMode } = useTheme();

  // LocalStorage 복원
  useEffect(() => {
    const savedService = localStorage.getItem('portal-service');
    const savedMode = localStorage.getItem('portal-theme');

    if (savedService) setService(savedService as ServiceType);
    if (savedMode) setMode(savedMode as ThemeMode);
  }, []);

  // LocalStorage 저장
  useEffect(() => {
    localStorage.setItem('portal-service', service);
    localStorage.setItem('portal-theme', mode);
  }, [service, mode]);

  return <div>...</div>;
}
```

---

## 타입 정의

### Vue Types

```typescript
// @portal/design-system-vue/src/types/theme.ts
export type ServiceType = 'portal' | 'blog' | 'shopping';
export type ThemeMode = 'light' | 'dark';

export interface ThemeConfig {
  service: ServiceType;
  mode: ThemeMode;
}
```

### React Types

```typescript
// @portal/design-types (공유 패키지)
export type ServiceType = 'portal' | 'blog' | 'shopping' | 'prism';
export type ThemeMode = 'light' | 'dark' | 'system';

export interface ThemeConfig {
  service: ServiceType;
  mode: ThemeMode;
}
```

---

## 관련 문서

- [Themes API](./themes.md) - 테마 시스템 상세
- [CSS Variables Reference](./css-variables.md) - CSS 변수 목록
- [Tailwind Preset API](./tailwind-preset.md) - Tailwind 사용법
