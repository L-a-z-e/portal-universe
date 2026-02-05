---
id: design-pattern-001
title: Theming - Dark Mode & Service Themes
type: learning
created: 2026-01-22
updated: 2026-01-22
author: Portal Universe Team
tags:
  - design-system
  - theming
  - dark-mode
  - css-variables
  - service-theme
related:
  - design-token-001
  - design-token-002
---

# Theming - Dark Mode & Service Themes

## 학습 목표

- CSS Variables 기반 테마 시스템 이해
- Dark-First 아키텍처 구조 학습
- 서비스별 테마 전환 메커니즘 습득
- Vue `useTheme` Composable 분석
- localStorage 기반 테마 영속성 구현 이해

## 1. Portal Universe Theming 아키텍처

### 1.1 2-Axis Theming System

Portal Universe는 **2개 축**의 테마를 지원합니다:

```
┌─────────────────────────────────────────┐
│         Theme Mode (Axis 1)             │
│  • Light Mode                           │
│  • Dark Mode (Default)                  │
└─────────────────────────────────────────┘
                 +
┌─────────────────────────────────────────┐
│      Service Theme (Axis 2)             │
│  • Portal (Indigo) - Default            │
│  • Blog (Green)                         │
│  • Shopping (Orange)                    │
└─────────────────────────────────────────┘
```

### 1.2 HTML Attributes

```html
<html data-theme="dark" data-service="portal" class="dark">
  <!-- Dark Mode + Portal Theme -->
</html>

<html data-theme="light" data-service="blog" class="light">
  <!-- Light Mode + Blog Theme -->
</html>
```

## 2. Dark-First 아키텍처

### 2.1 개념

Portal Universe는 **다크모드를 기본**으로 하고, 라이트모드를 예외로 처리합니다.

**Why Dark-First?**
- ✅ Linear-inspired 디자인 철학
- ✅ 개발자 도구 친화적
- ✅ 눈의 피로 감소
- ✅ 트렌디한 UI

### 2.2 Tailwind 설정

`frontend/design-tokens/tailwind.preset.js`:

```javascript
export default {
  // Dark mode 기본 활성화
  darkMode: ['class', '[data-theme="dark"]'],

  plugins: [
    plugin(function({ addVariant }) {
      // 'light:' 변형 추가
      addVariant('light', [
        '[data-theme="light"] &',
        '.light &'
      ]);
    }),
  ]
}
```

### 2.3 CSS Classes 패턴

```tsx
// Dark mode (기본) + Light mode (오버라이드)
<div className="
  bg-[#08090a] text-white             {/* 다크모드 */}
  light:bg-white light:text-gray-900  {/* 라이트모드 */}
">
  Content
</div>
```

## 3. CSS Variables 구조

### 3.1 Base Variables (기본 팔레트)

```css
/* Linear Color Palette */
:root {
  --linear-50: #f7f8f8;
  --linear-100: #ebeced;
  --linear-200: #d0d6e0;
  --linear-300: #8a8f98;
  --linear-400: #6c717a;
  --linear-500: #5c6169;
  --linear-600: #3e3e44;
  --linear-700: #26282b;
  --linear-800: #1b1c1e;
  --linear-850: #141516;
  --linear-900: #0e0f10;
  --linear-950: #08090a;

  --indigo-400: #5e6ad2;
  --indigo-500: #4754c9;
  --indigo-600: #3f4ab8;
}
```

### 3.2 Semantic Variables (Dark Mode 기본)

```css
:root {
  /* Brand */
  --semantic-brand-primary: var(--indigo-400);
  --semantic-brand-primaryHover: var(--indigo-500);
  --semantic-brand-secondary: var(--linear-600);

  /* Text */
  --semantic-text-heading: var(--linear-50);
  --semantic-text-body: var(--linear-100);
  --semantic-text-meta: var(--linear-300);
  --semantic-text-muted: var(--linear-400);

  /* Background */
  --semantic-bg-page: var(--linear-950);
  --semantic-bg-card: var(--linear-900);
  --semantic-bg-elevated: var(--linear-800);

  /* Border */
  --semantic-border-default: #2a2a2a;
  --semantic-border-hover: #3a3a3a;
}
```

### 3.3 Light Mode Override

```css
[data-theme="light"] {
  /* Text */
  --semantic-text-heading: #1a1a1a;
  --semantic-text-body: #404040;
  --semantic-text-meta: #6b6b6b;

  /* Background */
  --semantic-bg-page: #ffffff;
  --semantic-bg-card: #f9fafb;
  --semantic-bg-elevated: #ffffff;

  /* Border */
  --semantic-border-default: #e5e7eb;
  --semantic-border-hover: #d1d5db;
}
```

## 4. Service Theme Override

### 4.1 Portal Theme (기본)

```css
:root {
  --semantic-brand-primary: #5e6ad2;  /* Indigo */
  --semantic-brand-primaryHover: #4754c9;
}
```

### 4.2 Blog Theme

```css
[data-service="blog"] {
  --semantic-brand-primary: #10b981;  /* Green */
  --semantic-brand-primaryHover: #059669;
}
```

### 4.3 Shopping Theme

```css
[data-service="shopping"] {
  --semantic-brand-primary: #F97316;  /* Orange */
  --semantic-brand-primaryHover: #EA580C;
}
```

### 4.4 컴포넌트에서 자동 적용

```tsx
// 컴포넌트 코드 변경 없이 테마에 따라 색상 자동 변경
<button className="bg-brand-primary hover:bg-brand-primaryHover">
  Click me
</button>
```

```html
<!-- Portal Theme -->
<html data-service="portal">
  <button class="bg-brand-primary">  <!-- Indigo -->
</html>

<!-- Blog Theme -->
<html data-service="blog">
  <button class="bg-brand-primary">  <!-- Green -->
</html>

<!-- Shopping Theme -->
<html data-service="shopping">
  <button class="bg-brand-primary">  <!-- Orange -->
</html>
```

## 5. Vue useTheme Composable 분석

### 5.1 전체 코드

`frontend/design-system-vue/src/composables/useTheme.ts`:

```typescript
import { ref, onMounted } from 'vue';

export type ServiceType = 'portal' | 'blog' | 'shopping';
export type ThemeMode = 'light' | 'dark';

const currentService = ref<ServiceType>('portal');
const currentTheme = ref<ThemeMode>('light');

export function useTheme() {
  /**
   * Set service context
   */
  const setService = (service: ServiceType) => {
    currentService.value = service;

    if (typeof document !== 'undefined') {
      document.documentElement.setAttribute('data-service', service);
    }

    if (typeof localStorage !== 'undefined') {
      localStorage.setItem('portal-service', service);
    }
  };

  /**
   * Set theme mode (light/dark)
   */
  const setTheme = (mode: ThemeMode) => {
    currentTheme.value = mode;

    if (typeof document !== 'undefined') {
      document.documentElement.setAttribute('data-theme', mode);

      // Tailwind darkMode: 'class' support
      if (mode === 'dark') {
        document.documentElement.classList.add('dark');
      } else {
        document.documentElement.classList.remove('dark');
      }
    }

    if (typeof localStorage !== 'undefined') {
      localStorage.setItem('portal-theme', mode);
    }
  };

  /**
   * Toggle between light and dark mode
   */
  const toggleTheme = () => {
    const newTheme = currentTheme.value === 'light' ? 'dark' : 'light';
    setTheme(newTheme);
  };

  /**
   * Initialize theme from localStorage or system preference
   */
  const initTheme = () => {
    if (typeof window === 'undefined') return;

    const savedTheme = localStorage.getItem('portal-theme') as ThemeMode;
    const savedService = localStorage.getItem('portal-service') as ServiceType;

    if (savedTheme) {
      setTheme(savedTheme);
    } else {
      // System preference
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      setTheme(prefersDark ? 'dark' : 'light');
    }

    if (savedService) {
      setService(savedService);
    }
  };

  /**
   * Watch for system theme changes
   */
  onMounted(() => {
    initTheme();

    if (typeof window !== 'undefined') {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
      const handleChange = (e: MediaQueryListEvent) => {
        // Only follow system if user hasn't manually set theme
        if (!localStorage.getItem('portal-theme')) {
          setTheme(e.matches ? 'dark' : 'light');
        }
      };

      mediaQuery.addEventListener('change', handleChange);

      return () => {
        mediaQuery.removeEventListener('change', handleChange);
      };
    }
  });

  return {
    currentService,
    currentTheme,
    setService,
    setTheme,
    toggleTheme,
    initTheme,
  };
}
```

### 5.2 핵심 기능

#### 1. setTheme
```typescript
const setTheme = (mode: ThemeMode) => {
  // 1. State 업데이트
  currentTheme.value = mode;

  // 2. HTML attribute 설정
  document.documentElement.setAttribute('data-theme', mode);

  // 3. Tailwind class 토글
  if (mode === 'dark') {
    document.documentElement.classList.add('dark');
  } else {
    document.documentElement.classList.remove('dark');
  }

  // 4. localStorage 저장
  localStorage.setItem('portal-theme', mode);
};
```

#### 2. System Preference
```typescript
const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
setTheme(prefersDark ? 'dark' : 'light');
```

#### 3. System Preference Watcher
```typescript
const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
mediaQuery.addEventListener('change', (e) => {
  if (!localStorage.getItem('portal-theme')) {
    setTheme(e.matches ? 'dark' : 'light');
  }
});
```

## 6. 실습 예제

### 예제 1: Theme Toggle Button

```vue
<!-- Vue -->
<script setup lang="ts">
import { useTheme } from '@portal/design-system-vue';

const { currentTheme, toggleTheme } = useTheme();
</script>

<template>
  <button @click="toggleTheme">
    <span v-if="currentTheme === 'dark'">🌙 Dark</span>
    <span v-else>☀️ Light</span>
  </button>
</template>
```

```tsx
// React
const [theme, setTheme] = useState<'light' | 'dark'>('dark');

const toggleTheme = () => {
  const newTheme = theme === 'light' ? 'dark' : 'light';
  setTheme(newTheme);

  document.documentElement.setAttribute('data-theme', newTheme);
  if (newTheme === 'dark') {
    document.documentElement.classList.add('dark');
  } else {
    document.documentElement.classList.remove('dark');
  }

  localStorage.setItem('portal-theme', newTheme);
};

<button onClick={toggleTheme}>
  {theme === 'dark' ? '🌙 Dark' : '☀️ Light'}
</button>
```

### 예제 2: Service Theme Selector

```vue
<!-- Vue -->
<script setup lang="ts">
import { useTheme } from '@portal/design-system-vue';

const { currentService, setService } = useTheme();
</script>

<template>
  <select :value="currentService" @change="setService($event.target.value)">
    <option value="portal">Portal (Indigo)</option>
    <option value="blog">Blog (Green)</option>
    <option value="shopping">Shopping (Orange)</option>
  </select>
</template>
```

### 예제 3: Theme-Aware Component

```tsx
// React
<div className="
  bg-bg-card text-text-body
  border border-border-default
  rounded-lg p-4
">
  {/* 자동으로 다크/라이트 모드 대응 */}
  <h2 className="text-text-heading font-semibold mb-2">
    Card Title
  </h2>
  <p className="text-text-meta">
    This card adapts to the current theme automatically.
  </p>
</div>
```

### 예제 4: System Preference 감지

```typescript
// React Hook
import { useEffect, useState } from 'react';

export function useSystemTheme() {
  const [systemTheme, setSystemTheme] = useState<'light' | 'dark'>('dark');

  useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');

    setSystemTheme(mediaQuery.matches ? 'dark' : 'light');

    const handler = (e: MediaQueryListEvent) => {
      setSystemTheme(e.matches ? 'dark' : 'light');
    };

    mediaQuery.addEventListener('change', handler);
    return () => mediaQuery.removeEventListener('change', handler);
  }, []);

  return systemTheme;
}
```

### 예제 5: Theme Transition

```css
/* 부드러운 테마 전환 */
html {
  transition: background-color 0.3s ease, color 0.3s ease;
}

* {
  transition: background-color 0.3s ease, border-color 0.3s ease;
}
```

## 7. 고급 패턴

### 7.1 SSR 고려

```typescript
// Server-Side Rendering에서 안전한 코드
const setTheme = (mode: ThemeMode) => {
  if (typeof document === 'undefined') return;

  document.documentElement.setAttribute('data-theme', mode);
};
```

### 7.2 Flash of Unstyled Content (FOUC) 방지

```html
<!-- index.html -->
<script>
  // DOM 로드 전에 실행
  (function() {
    const savedTheme = localStorage.getItem('portal-theme');
    if (savedTheme) {
      document.documentElement.setAttribute('data-theme', savedTheme);
      if (savedTheme === 'dark') {
        document.documentElement.classList.add('dark');
      }
    }
  })();
</script>
```

### 7.3 Custom Service Theme

```css
/* 새로운 서비스 추가 */
[data-service="admin"] {
  --semantic-brand-primary: #8B5CF6;  /* Purple */
  --semantic-brand-primaryHover: #7C3AED;
}
```

```typescript
// Type 확장
export type ServiceType = 'portal' | 'blog' | 'shopping' | 'admin';
```

## 8. 핵심 요약

### ✅ Key Takeaways

1. **2-Axis Theming**: Theme Mode (Light/Dark) + Service Theme
2. **CSS Variables**: 런타임 테마 전환 가능
3. **Dark-First**: 다크모드 기본, `light:` 변형으로 라이트모드
4. **localStorage**: 사용자 선택 영속성
5. **System Preference**: OS 다크모드 자동 감지

### 🎯 Architecture Flow

```
User Action (toggleTheme)
    ↓
State Update (currentTheme.value = 'dark')
    ↓
HTML Attribute (data-theme="dark")
    ↓
Tailwind Class (.dark)
    ↓
CSS Variables Override ([data-theme="dark"] { ... })
    ↓
Component Re-render (자동 스타일 변경)
    ↓
localStorage Save (영속성)
```

### 📋 Best Practices

```tsx
// ✅ DO
// 1. Semantic Token 사용
<div className="bg-bg-card text-text-body">

// 2. light: 변형으로 라이트모드 지원
<button className="bg-white light:bg-brand-primary">

// 3. useTheme Composable 사용
const { currentTheme, toggleTheme } = useTheme();

// ❌ DON'T
// 1. 하드코딩된 색상
<div className="bg-gray-900 text-white">

// 2. 인라인 스타일
<div style={{ backgroundColor: '#08090a' }}>

// 3. 테마 감지 없이 조건부 렌더링
{isDark ? <DarkComponent /> : <LightComponent />}
```

## 9. 관련 문서

- [Design Tokens](../tokens/design-tokens.md) - CSS Variables 정의
- [Tailwind Integration](../tokens/tailwind-integration.md) - Preset 설정
- [Button Component](../components/button-component.md) - 테마 적용 사례
