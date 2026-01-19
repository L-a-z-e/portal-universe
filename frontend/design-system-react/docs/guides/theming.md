---
id: theming-react
title: Theming Guide
type: guide
status: current
created: 2026-01-19
updated: 2026-01-19
author: documenter
tags: [guide, react, theming]
related:
  - getting-started-react
  - hooks-react
---

# Theming Guide

Design System React에서 테마를 사용하고 커스터마이징하는 방법을 안내합니다.

## 테마 시스템 개요

Design System은 두 가지 테마 축을 제공합니다:

1. **서비스 테마** (`data-service`): 서비스별 브랜드 색상
2. **다크/라이트 모드** (`data-theme`): 밝기 모드

```html
<html data-service="portal" data-theme="dark">
  <!-- Portal 서비스, 다크 모드 -->
</html>
```

## useTheme Hook 사용

### 기본 사용법

```tsx
import { useTheme } from '@portal/design-system-react';

function App() {
  const {
    service,
    mode,
    resolvedMode,
    setService,
    setMode,
    toggleMode,
  } = useTheme();

  return (
    <div>
      <p>Service: {service}</p>
      <p>Mode: {mode}</p>
      <p>Resolved: {resolvedMode}</p>
    </div>
  );
}
```

### Theme Switcher 컴포넌트

```tsx
import { useTheme } from '@portal/design-system-react';
import { SunIcon, MoonIcon } from '@heroicons/react/24/outline';

function ThemeToggle() {
  const { resolvedMode, toggleMode } = useTheme();

  return (
    <Button variant="ghost" onClick={toggleMode}>
      {resolvedMode === 'dark' ? (
        <SunIcon className="w-5 h-5" />
      ) : (
        <MoonIcon className="w-5 h-5" />
      )}
      <span className="ml-2">
        {resolvedMode === 'dark' ? '라이트 모드' : '다크 모드'}
      </span>
    </Button>
  );
}
```

### Service Selector

```tsx
function ServiceSelector() {
  const { service, setService } = useTheme();

  return (
    <div className="flex gap-2">
      {(['portal', 'blog', 'shopping'] as const).map((s) => (
        <Button
          key={s}
          variant={service === s ? 'primary' : 'ghost'}
          onClick={() => setService(s)}
        >
          {s}
        </Button>
      ))}
    </div>
  );
}
```

## 시스템 테마 감지

```tsx
function App() {
  const { mode, resolvedMode } = useTheme({
    defaultMode: 'system', // 시스템 설정 따르기
  });

  // mode = 'system', resolvedMode = 'light' 또는 'dark'
  return <div>Theme: {resolvedMode}</div>;
}
```

## 테마 지속성

### localStorage 사용

```tsx
import { useTheme } from '@portal/design-system-react';
import { useEffect } from 'react';

function ThemeManager() {
  const { mode, service, setMode, setService } = useTheme();

  // 저장된 설정 복원
  useEffect(() => {
    const savedMode = localStorage.getItem('theme-mode');
    const savedService = localStorage.getItem('theme-service');

    if (savedMode) setMode(savedMode as any);
    if (savedService) setService(savedService as any);
  }, []);

  // 변경 시 저장
  useEffect(() => {
    localStorage.setItem('theme-mode', mode);
    localStorage.setItem('theme-service', service);
  }, [mode, service]);

  return null; // 또는 children
}
```

### 초기 깜빡임 방지

```html
<!-- index.html -->
<script>
  // 렌더링 전 테마 적용
  const mode = localStorage.getItem('theme-mode') || 'dark';
  const service = localStorage.getItem('theme-service') || 'portal';

  // system 모드 처리
  const resolved = mode === 'system'
    ? (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')
    : mode;

  document.documentElement.setAttribute('data-theme', resolved);
  document.documentElement.setAttribute('data-service', service);
</script>
```

## ThemeProvider 패턴

### 커스텀 Provider

```tsx
// contexts/ThemeContext.tsx
import { createContext, useContext, useState, useEffect, ReactNode } from 'react';

interface ThemeContextType {
  mode: 'light' | 'dark' | 'system';
  service: 'portal' | 'blog' | 'shopping';
  resolvedMode: 'light' | 'dark';
  setMode: (mode: 'light' | 'dark' | 'system') => void;
  setService: (service: 'portal' | 'blog' | 'shopping') => void;
}

const ThemeContext = createContext<ThemeContextType | null>(null);

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [mode, setMode] = useState<'light' | 'dark' | 'system'>('dark');
  const [service, setService] = useState<'portal' | 'blog' | 'shopping'>('portal');
  const [systemMode, setSystemMode] = useState<'light' | 'dark'>('dark');

  const resolvedMode = mode === 'system' ? systemMode : mode;

  useEffect(() => {
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    setSystemMode(mq.matches ? 'dark' : 'light');

    const handler = (e: MediaQueryListEvent) => {
      setSystemMode(e.matches ? 'dark' : 'light');
    };
    mq.addEventListener('change', handler);
    return () => mq.removeEventListener('change', handler);
  }, []);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', resolvedMode);
    document.documentElement.setAttribute('data-service', service);
  }, [resolvedMode, service]);

  return (
    <ThemeContext.Provider
      value={{ mode, service, resolvedMode, setMode, setService }}
    >
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within ThemeProvider');
  }
  return context;
}
```

### 사용

```tsx
// App.tsx
function App() {
  return (
    <ThemeProvider>
      <YourApp />
    </ThemeProvider>
  );
}
```

## CSS 커스터마이징

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

### 컴포넌트 레벨 커스터마이징

```tsx
// CSS-in-JS 스타일
const CustomCard = ({ children }) => (
  <div
    style={{
      '--semantic-bg-card': '#fef3c7',
      '--semantic-border-default': '#fbbf24',
      background: 'var(--semantic-bg-card)',
      border: '1px solid var(--semantic-border-default)',
    } as React.CSSProperties}
  >
    {children}
  </div>
);
```

### Tailwind 클래스

```tsx
<Button
  className="bg-purple-600 hover:bg-purple-700"
>
  Custom Styled
</Button>
```

## 테마 전환 애니메이션

### CSS Transitions

```css
* {
  transition: background-color 0.3s ease, color 0.3s ease, border-color 0.3s ease;
}
```

### View Transitions API

```tsx
async function toggleThemeWithTransition() {
  if (!document.startViewTransition) {
    toggleMode();
    return;
  }

  await document.startViewTransition(() => {
    toggleMode();
  }).ready;
}
```

## 모범 사례

### 1. Semantic 토큰 사용

```tsx
// Bad
<div style={{ backgroundColor: '#1f2937', color: '#f3f4f6' }}>

// Good
<div className="bg-bg-card text-text-body">
```

### 2. 반응형 테마

```tsx
// 화면 크기에 따른 다른 레이아웃
<div className="bg-bg-card md:bg-bg-elevated">
```

### 3. 접근성

```tsx
// 색맹 사용자를 위한 아이콘 병행 사용
<Badge variant="success">
  <CheckIcon className="w-4 h-4 mr-1" />
  완료
</Badge>
```

### 4. 테마 테스트

```tsx
// Storybook에서 모든 테마 조합 테스트
export const AllThemes = () => (
  <div className="grid grid-cols-2 gap-4">
    {['portal', 'blog', 'shopping'].map((service) =>
      ['light', 'dark'].map((theme) => (
        <div
          key={`${service}-${theme}`}
          data-service={service}
          data-theme={theme}
          className="p-4"
        >
          <Button>Test Button</Button>
        </div>
      ))
    )}
  </div>
);
```
