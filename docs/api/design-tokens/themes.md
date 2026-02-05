---
id: themes
title: Themes API
type: api
status: current
created: 2026-02-06
updated: 2026-02-06
author: documenter
tags: [api, themes, design-tokens, data-attributes]
related:
  - css-variables
  - use-theme
---

# Themes API

Portal Universe의 서비스별 테마 시스템 및 light/dark mode 전환 메커니즘입니다.

## 개요

| 항목 | 값 |
|------|-----|
| **테마 개수** | 4개 (Portal, Blog, Shopping, Prism) |
| **모드** | Light / Dark |
| **제어 방식** | HTML `data-*` 속성 |
| **우선순위** | `:root` < `[data-theme]` < `[data-service]` < `[data-service][data-theme]` |

## HTML 속성 API

### `data-service`

서비스 컨텍스트를 설정합니다.

```html
<html data-service="portal">   <!-- Portal 서비스 -->
<html data-service="blog">      <!-- Blog 서비스 -->
<html data-service="shopping">  <!-- Shopping 서비스 -->
<html data-service="prism">     <!-- Prism 서비스 -->
```

| 값 | 설명 | 기본 모드 |
|-----|------|----------|
| `portal` | 포털 서비스 (Linear 스타일) | Dark-first |
| `blog` | 블로그 서비스 (Velog 스타일) | Light-first |
| `shopping` | 쇼핑 서비스 (Shopify 스타일) | Light-first |
| `prism` | Prism AI 서비스 | Light-first |

### `data-theme`

라이트/다크 모드를 설정합니다.

```html
<html data-theme="light">  <!-- 라이트 모드 -->
<html data-theme="dark">   <!-- 다크 모드 -->
```

## 서비스별 테마 상세

### Portal 테마 (Dark-first)

Linear.app 스타일의 다크 UI 최적화 테마입니다.

#### 기본 색상 (Dark Mode)

| 토큰 | Light Mode | Dark Mode (기본) |
|------|------------|------------------|
| **Brand Primary** | `#4754c9` (indigo-500) | `#5e6ad2` (indigo-400) |
| **Brand Primary Hover** | `#3f4ab8` (indigo-600) | `#747fdb` (indigo-300) |
| **Text Heading** | `#08090a` (linear-950) | `#ffffff` |
| **Text Body** | `#3e3e44` (linear-600) | `#b4b4b4` (linear-300) |
| **Bg Page** | `#f7f8f8` (linear-50) | `#08090a` (linear-950) |
| **Bg Card** | `#ffffff` | `#0f1011` (linear-850) |
| **Border Default** | `#d0d6e0` (linear-200) | `#2a2a2a` (linear-700) |

#### CSS 선택자 패턴

```css
/* 기본 (Dark Mode) */
[data-service="portal"] {
  --semantic-brand-primary: #5e6ad2;
  --semantic-text-heading: #ffffff;
  --semantic-bg-page: #08090a;
}

/* Light Mode 오버라이드 */
[data-service="portal"][data-theme="light"] {
  --semantic-brand-primary: #4754c9;
  --semantic-text-heading: #08090a;
  --semantic-bg-page: #f7f8f8;
}
```

### Blog 테마 (Light-first)

Velog 스타일의 Teal/Mint 브랜딩입니다.

#### 기본 색상 (Light Mode)

| 토큰 | Light Mode (기본) | Dark Mode |
|------|-------------------|-----------|
| **Brand Primary** | `#12B886` (teal) | `#5e6ad2` (indigo-400) † |
| **Text Heading** | `#212529` (gray-900) | `#ffffff` |
| **Text Body** | `#212529` (gray-900) | `#b4b4b4` (linear-300) |
| **Bg Page** | `#F8F9FA` (gray-50) | `#08090a` (linear-950) |
| **Bg Card** | `#ffffff` | `#0f1011` (linear-850) |
| **Border Default** | `#E9ECEF` (gray-200) | `#2a2a2a` (linear-700) |

† **통일된 다크 모드**: Blog/Shopping/Prism은 다크 모드에서 동일한 Indigo accent 사용

#### CSS 선택자 패턴

```css
/* 기본 (Light Mode) */
[data-service="blog"] {
  --semantic-brand-primary: #12B886;
  --semantic-text-heading: #212529;
  --semantic-bg-page: #F8F9FA;
}

/* Dark Mode 오버라이드 */
[data-service="blog"][data-theme="dark"] {
  --semantic-brand-primary: #5e6ad2;
  --semantic-text-heading: #ffffff;
  --semantic-bg-page: #08090a;
}
```

### Shopping 테마 (Light-first)

Shopify 스타일의 Warm Orange 브랜딩입니다.

#### 기본 색상 (Light Mode)

| 토큰 | Light Mode (기본) | Dark Mode |
|------|-------------------|-----------|
| **Brand Primary** | `#FF6B35` (orange) | `#5e6ad2` (indigo-400) † |
| **Text Heading** | `#212529` (gray-900) | `#ffffff` |
| **Bg Page** | `#ffffff` | `#08090a` (linear-950) |
| **Bg Muted** | `#FFF4E6` (orange-50) | `#0e0f10` (linear-900) |
| **Border Hover** | `#FFC078` (orange-300) | `#3e3e44` (linear-600) |

#### CSS 선택자 패턴

```css
/* 기본 (Light Mode) */
[data-service="shopping"] {
  --semantic-brand-primary: #FF6B35;
  --semantic-bg-muted: #FFF4E6;
}

/* Dark Mode 오버라이드 */
[data-service="shopping"][data-theme="dark"] {
  --semantic-brand-primary: #5e6ad2;
  --semantic-bg-muted: #0e0f10;
}
```

### Prism 테마 (Light-first)

AI/Tech 느낌의 Blue-Indigo 브랜딩입니다.

#### 기본 색상 (Light Mode)

| 토큰 | Light Mode (기본) | Dark Mode |
|------|-------------------|-----------|
| **Brand Primary** | `#4F46E5` (indigo) | `#5e6ad2` (indigo-400) † |
| **Text Heading** | `#212529` (gray-900) | `#ffffff` |
| **Bg Muted** | `#EEF2FF` (indigo-50) | `#0e0f10` (linear-900) |
| **Border Hover** | `#818CF8` (indigo-400) | `#3e3e44` (linear-600) |

## CSS 선택자 우선순위

```css
/* 1. 기본값 (:root) */
:root {
  --semantic-brand-primary: #5e6ad2;
}

/* 2. 전역 Dark Mode 오버라이드 (우선순위 낮음) */
[data-theme="dark"] {
  --semantic-brand-primary: #20C997;
}

/* 3. 서비스별 오버라이드 (Light Mode 기본값) */
[data-service="blog"] {
  --semantic-brand-primary: #12B886;
}

/* 4. 서비스별 Dark Mode 오버라이드 (최고 우선순위) */
[data-service="blog"][data-theme="dark"] {
  --semantic-brand-primary: #5e6ad2;
}
```

**우선순위 순서**: `[data-service][data-theme]` > `[data-service]` > `[data-theme]` > `:root`

## 테마 전환 예시

### HTML 구조

```html
<!DOCTYPE html>
<html lang="ko" data-service="portal" data-theme="dark">
<head>
  <link rel="stylesheet" href="@portal/design-tokens/dist/tokens.css">
</head>
<body>
  <h1>Portal Universe</h1>
  <!-- --semantic-brand-primary는 Portal Dark Mode 값인 #5e6ad2 -->
</body>
</html>
```

### JavaScript로 테마 전환

```javascript
// 서비스 전환
document.documentElement.setAttribute('data-service', 'blog');

// 모드 전환
document.documentElement.setAttribute('data-theme', 'light');

// 동시 전환
document.documentElement.setAttribute('data-service', 'shopping');
document.documentElement.setAttribute('data-theme', 'dark');
```

### Vue Composable로 전환

```vue
<script setup>
import { useTheme } from '@portal/design-system-vue';

const { setService, setTheme, toggleTheme } = useTheme();

setService('blog');      // Blog 서비스로 전환
setTheme('light');       // Light 모드로 전환
toggleTheme();           // Light <-> Dark 토글
</script>
```

### React Hook으로 전환

```tsx
import { useTheme } from '@portal/design-system-react';

export function ThemeSwitcher() {
  const { setService, setMode, toggleMode } = useTheme();

  return (
    <div>
      <button onClick={() => setService('shopping')}>Shopping</button>
      <button onClick={() => setMode('dark')}>Dark</button>
      <button onClick={toggleMode}>Toggle</button>
    </div>
  );
}
```

## 전체 변수 오버라이드 목록

### Portal 테마

<details>
<summary>Portal Light Mode 오버라이드 (15개 변수)</summary>

```css
[data-service="portal"][data-theme="light"] {
  --semantic-brand-primary: #4754c9;
  --semantic-brand-primaryHover: #3f4ab8;
  --semantic-brand-secondary: #5e6ad2;
  --semantic-text-heading: #08090a;
  --semantic-text-body: #3e3e44;
  --semantic-text-meta: #6b6b6b;
  --semantic-text-muted: #b4b4b4;
  --semantic-text-inverse: #f7f8f8;
  --semantic-text-link: #4754c9;
  --semantic-text-linkHover: #3f4ab8;
  --semantic-bg-page: #f7f8f8;
  --semantic-bg-card: #ffffff;
  --semantic-bg-elevated: #ffffff;
  --semantic-bg-muted: #ebeced;
  --semantic-bg-hover: #ebeced;
  --semantic-border-default: #d0d6e0;
  --semantic-border-hover: #b4b4b4;
  --semantic-border-focus: #4754c9;
  --semantic-border-muted: #ebeced;
  --semantic-status-success: #12B886;
  --semantic-status-successBg: #E6FCF5;
  --semantic-status-error: #F03E3E;
  --semantic-status-errorBg: #FFE3E3;
  --semantic-status-warning: #FAB005;
  --semantic-status-warningBg: #FFF9DB;
  --semantic-status-info: #228BE6;
  --semantic-status-infoBg: #D0EBFF;
}
```

</details>

### Blog/Shopping/Prism 테마

<details>
<summary>Blog Light Mode (기본) 오버라이드 (7개 변수)</summary>

```css
[data-service="blog"] {
  --semantic-brand-primary: #12B886;
  --semantic-brand-primaryHover: #0CA678;
  --semantic-brand-secondary: #38D9A9;
  --semantic-bg-muted: #E6FCF5;  /* Green tint */
  --semantic-border-focus: #12B886;
  --semantic-status-success: #12B886;
  --semantic-status-successBg: #E6FCF5;
}
```

</details>

<details>
<summary>Shopping Light Mode (기본) 오버라이드 (7개 변수)</summary>

```css
[data-service="shopping"] {
  --semantic-brand-primary: #FF6B35;
  --semantic-brand-primaryHover: #E55A2B;
  --semantic-brand-secondary: #FFA94D;
  --semantic-bg-muted: #FFF4E6;  /* Orange tint */
  --semantic-border-hover: #FFC078;
  --semantic-border-focus: #FF6B35;
}
```

</details>

<details>
<summary>Unified Dark Mode (모든 서비스 공통)</summary>

```css
[data-service="blog"][data-theme="dark"],
[data-service="shopping"][data-theme="dark"],
[data-service="prism"][data-theme="dark"] {
  --semantic-brand-primary: #5e6ad2;
  --semantic-brand-primaryHover: #747fdb;
  --semantic-brand-secondary: #9ba3e5;
  --semantic-text-heading: #ffffff;
  --semantic-text-body: #b4b4b4;
  --semantic-text-meta: #6b6b6b;
  --semantic-text-muted: #6b6b6b;
  --semantic-text-inverse: #08090a;
  --semantic-text-link: #5e6ad2;
  --semantic-text-linkHover: #747fdb;
  --semantic-bg-page: #08090a;
  --semantic-bg-card: #0f1011;
  --semantic-bg-elevated: #18191b;
  --semantic-bg-muted: #0e0f10;
  --semantic-bg-hover: #2a2a2a;
  --semantic-border-default: #2a2a2a;
  --semantic-border-hover: #3e3e44;
  --semantic-border-focus: #5e6ad2;
  --semantic-border-muted: #18191b;
}
```

</details>

## 테마 감지 및 초기화

### System Preference 감지

```javascript
// 시스템 다크 모드 감지
const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;

// MediaQuery 변경 감지
const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
mediaQuery.addEventListener('change', (e) => {
  if (e.matches) {
    document.documentElement.setAttribute('data-theme', 'dark');
  } else {
    document.documentElement.setAttribute('data-theme', 'light');
  }
});
```

### LocalStorage 저장

```javascript
// 테마 저장
localStorage.setItem('portal-service', 'blog');
localStorage.setItem('portal-theme', 'light');

// 테마 복원
const savedService = localStorage.getItem('portal-service');
const savedTheme = localStorage.getItem('portal-theme');

if (savedService) {
  document.documentElement.setAttribute('data-service', savedService);
}
if (savedTheme) {
  document.documentElement.setAttribute('data-theme', savedTheme);
}
```

## Tailwind CSS와의 통합

Tailwind `darkMode: 'class'` 설정과 함께 사용 시:

```javascript
// data-theme 변경 시 .dark 클래스도 함께 토글
function setTheme(mode) {
  document.documentElement.setAttribute('data-theme', mode);

  if (mode === 'dark') {
    document.documentElement.classList.add('dark');
  } else {
    document.documentElement.classList.remove('dark');
  }
}
```

자세한 내용은 [Tailwind Preset API](./tailwind-preset.md)를 참조하세요.

## 관련 문서

- [CSS Variables Reference](./css-variables.md) - 전체 CSS 변수 목록
- [Tailwind Preset API](./tailwind-preset.md) - Tailwind 프리셋 사용법
- [useTheme Hook](./use-theme.md) - Vue/React 테마 관리
- [Build Process](./build-process.md) - 테마 빌드 과정
