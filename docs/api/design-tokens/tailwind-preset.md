---
id: tailwind-preset
title: Tailwind Preset API
type: api
status: current
created: 2026-02-06
updated: 2026-02-06
author: Laze
tags: [api, tailwind, preset, utility-classes]
related:
  - css-variables
  - themes
---

# Tailwind Preset API

Design Tokens의 Tailwind CSS Preset 사용 가이드입니다.

## 개요

| 항목 | 값 |
|------|-----|
| **패키지** | `@portal/design-tokens` |
| **파일** | `tailwind.preset.js` |
| **Tailwind 버전** | v3+ |
| **Dark Mode** | `class` (`.dark` 클래스 또는 `[data-theme="dark"]`) |

## 설치 및 설정

### 1. Tailwind Config에 Preset 추가

```javascript
// tailwind.config.js
import designTokensPreset from '@portal/design-tokens/tailwind.preset';

export default {
  presets: [designTokensPreset],
  content: [
    './src/**/*.{vue,js,ts,jsx,tsx}',
  ],
  // 추가 설정...
};
```

### 2. CSS Import

```css
/* main.css */
@import '@portal/design-tokens/dist/tokens.css';

@tailwind base;
@tailwind components;
@tailwind utilities;
```

## Utility Classes

### Colors

#### Semantic Colors

Preset은 CSS 변수 기반 semantic colors를 제공합니다.

```html
<!-- Brand -->
<button class="bg-brand-primary text-text-inverse">
  Primary Button
</button>

<!-- Text -->
<h1 class="text-text-heading">Heading</h1>
<p class="text-text-body">Body text</p>
<small class="text-text-meta">Metadata</small>

<!-- Background -->
<div class="bg-bg-page">
  <div class="bg-bg-card">Card</div>
  <div class="bg-bg-elevated">Modal</div>
</div>

<!-- Border -->
<input class="border border-border-default focus:border-border-focus" />

<!-- Status -->
<div class="bg-status-successBg text-status-success">Success</div>
<div class="bg-status-errorBg text-status-error">Error</div>
<div class="bg-status-warningBg text-status-warning">Warning</div>
<div class="bg-status-infoBg text-status-info">Info</div>
```

#### Color Palette 전체 매핑

| Tailwind Class | CSS Variable |
|----------------|--------------|
| `bg-brand-primary` | `var(--semantic-brand-primary)` |
| `bg-brand-primaryHover` | `var(--semantic-brand-primaryHover)` |
| `text-text-heading` | `var(--semantic-text-heading)` |
| `text-text-body` | `var(--semantic-text-body)` |
| `text-text-meta` | `var(--semantic-text-meta)` |
| `bg-bg-page` | `var(--semantic-bg-page)` |
| `bg-bg-card` | `var(--semantic-bg-card)` |
| `border-border-default` | `var(--semantic-border-default)` |
| `border-border-focus` | `var(--semantic-border-focus)` |

#### Linear/Indigo 직접 접근

```html
<!-- Linear Gray Scale -->
<div class="bg-linear-950 text-linear-300">
  Dark UI with Linear colors
</div>

<!-- Indigo Accent -->
<button class="bg-indigo-400 hover:bg-indigo-500">
  Indigo Button
</button>
```

### Typography

#### Font Family

```html
<p class="font-sans">Inter Variable 폰트</p>
<code class="font-mono">JetBrains Mono 폰트</code>
<h1 class="font-serif">Noto Serif KR 폰트</h1>
```

#### Font Size

Linear 최적화 사이즈 스케일입니다.

| Class | Size | Pixel | 용도 |
|-------|------|-------|------|
| `text-micro` | 0.625rem | 10px | Badges, status |
| `text-xs` | 0.6875rem | 11px | Captions |
| `text-sm` | 0.8125rem | 13px | Helper text |
| `text-base` | 0.875rem | 14px | Body (기본) |
| `text-lg` | 1rem | 16px | Emphasized |
| `text-xl` | 1.125rem | 18px | h4 |
| `text-2xl` | 1.25rem | 20px | h3 |
| `text-3xl` | 1.5rem | 24px | h2 |
| `text-4xl` | 1.875rem | 30px | h1 |
| `text-5xl` ~ `text-9xl` | ... | 36px ~ 96px | Hero, Display |

```html
<h1 class="text-4xl font-semibold">Page Title</h1>
<p class="text-base">Default body text is 14px</p>
<small class="text-sm text-text-meta">Helper text</small>
```

#### Font Weight

Linear의 커스텀 가중치를 포함합니다.

| Class | Weight | 용도 |
|-------|--------|------|
| `font-light` | 300 | Reduced emphasis |
| `font-normal` | 400 | Default body |
| `font-medium` | 510 | Linear medium |
| `font-semibold` | 590 | Linear semibold |
| `font-bold` | 680 | Linear bold |
| `font-extrabold` | 800 | Maximum emphasis |

```html
<h2 class="font-semibold">Linear Semibold (590)</h2>
<p class="font-medium">Subtle emphasis</p>
```

#### Line Height

```html
<h1 class="leading-tight">Headline (1.2)</h1>
<p class="leading-normal">Body text (1.5)</p>
<article class="leading-loose">Long-form content (1.75)</article>
```

### Spacing

```html
<!-- Padding -->
<div class="p-xs">4px</div>
<div class="p-sm">8px</div>
<div class="p-md">16px</div>
<div class="p-lg">24px</div>
<div class="p-xl">32px</div>
<div class="p-2xl">48px</div>

<!-- Margin -->
<div class="m-sm mb-lg">
  8px all sides, 24px bottom
</div>

<!-- Gap -->
<div class="flex gap-md">
  16px gap between flex items
</div>
```

### Border Radius

```html
<div class="rounded-sm">2px</div>
<div class="rounded">4px (기본)</div>
<div class="rounded-md">6px</div>
<div class="rounded-lg">8px</div>
<div class="rounded-full">Pills, circles</div>
```

### Effects

#### Shadow

```html
<div class="shadow-sm">Subtle shadow</div>
<div class="shadow">Default shadow</div>
<div class="shadow-lg">Large shadow</div>
<div class="shadow-glow">Brand color glow</div>
```

#### Backdrop Blur (Glassmorphism)

```html
<div class="backdrop-blur-glass bg-bg-card/70 border border-border-subtle">
  Glassmorphism card
</div>

<div class="backdrop-blur-sm">4px blur</div>
<div class="backdrop-blur-md">16px blur</div>
<div class="backdrop-blur-xl">40px blur</div>
```

### Animation

#### Transition Duration

Linear 최적화된 지속 시간입니다.

```html
<button class="transition-fast">100ms</button>
<button class="transition-normal">160ms (Linear 기본)</button>
<button class="transition-slow">250ms</button>
```

#### Timing Function

```html
<div class="ease-linear-ease">Linear app easing</div>
<div class="ease-out-quart">Snappy decelerate</div>
<div class="ease-spring">Bouncy effect</div>
```

#### Keyframe Animations

Preset에 포함된 애니메이션입니다.

```html
<div class="animate-fade-in">Fade in</div>
<div class="animate-slide-up">Slide up</div>
<div class="animate-scale-in">Scale in</div>
```

## Custom Variant: `light:`

Dark-first 디자인을 위한 커스텀 variant입니다.

```html
<!-- Dark 모드 기본, Light 모드 오버라이드 -->
<div class="bg-linear-950 light:bg-linear-50">
  <!-- Dark: #08090a, Light: #f7f8f8 -->
</div>

<p class="text-linear-300 light:text-linear-700">
  <!-- Dark: #b4b4b4, Light: #2a2a2a -->
</p>
```

**적용 조건**: `.light` 클래스 또는 `[data-theme="light"]` 속성

```html
<!-- 방법 1: 클래스 -->
<html class="light">

<!-- 방법 2: 속성 (권장) -->
<html data-theme="light">
```

## Dark Mode 설정

### Preset의 Dark Mode Config

```javascript
// tailwind.preset.js
export default {
  darkMode: ['class', '[data-theme="dark"]'],
  // ...
};
```

- `.dark` 클래스 또는 `[data-theme="dark"]` 속성으로 다크 모드 활성화
- 두 조건 중 하나만 만족해도 적용

### Dark Mode 사용 예시

```html
<html data-theme="dark">
  <div class="bg-bg-page dark:bg-linear-950">
    <!-- Dark 모드에서 bg-linear-950 적용 -->
  </div>

  <button class="bg-brand-primary hover:bg-brand-primaryHover dark:shadow-lg">
    <!-- Dark 모드에서만 shadow-lg -->
  </button>
</html>
```

## 실전 예제

### Card 컴포넌트

```html
<div class="p-lg bg-bg-card rounded-lg shadow-md border border-border-default
            hover:shadow-lg hover:border-border-hover
            transition-normal ease-linear-ease">
  <h2 class="text-2xl font-semibold text-text-heading mb-sm">
    Card Title
  </h2>
  <p class="text-base text-text-body leading-relaxed">
    Card content with optimized readability
  </p>
</div>
```

### Button Variants

```html
<!-- Primary Button -->
<button class="px-md py-sm bg-brand-primary text-text-inverse
               rounded-md font-medium
               hover:bg-brand-primaryHover
               transition-normal ease-out-quart">
  Primary
</button>

<!-- Outline Button -->
<button class="px-md py-sm bg-transparent text-brand-primary
               border-2 border-brand-primary rounded-md font-medium
               hover:bg-brand-primary hover:text-text-inverse
               transition-normal">
  Outline
</button>

<!-- Ghost Button -->
<button class="px-md py-sm bg-transparent text-text-body
               hover:bg-bg-hover rounded-md
               transition-fast">
  Ghost
</button>
```

### Glassmorphism Card

```html
<div class="p-lg rounded-lg
            backdrop-blur-glass bg-bg-card/70
            border border-border-subtle
            shadow-glow">
  <h3 class="text-xl font-semibold text-text-heading">
    Glassmorphism
  </h3>
</div>
```

### Status Alert

```html
<div class="p-md bg-status-successBg border border-status-success rounded-md">
  <p class="text-sm text-status-success">Operation succeeded!</p>
</div>

<div class="p-md bg-status-errorBg border border-status-error rounded-md">
  <p class="text-sm text-status-error">An error occurred.</p>
</div>
```

### Responsive Grid with Service Theme

```html
<div data-service="blog" data-theme="light">
  <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-lg">
    <div class="p-lg bg-bg-card rounded-lg border border-border-default">
      <!-- Blog light mode: card is white, border is gray-200 -->
    </div>
  </div>
</div>
```

## Typography Plugin 통합

Preset은 `@tailwindcss/typography` 플러그인을 포함합니다.

```html
<article class="prose prose-lg">
  <h1>Article Title</h1>
  <p>Article content with automatic typography styles...</p>
</article>
```

## 커스터마이징

### Preset 확장

```javascript
// tailwind.config.js
import designTokensPreset from '@portal/design-tokens/tailwind.preset';

export default {
  presets: [designTokensPreset],
  theme: {
    extend: {
      // 추가 색상
      colors: {
        custom: '#abc123',
      },
      // 추가 spacing
      spacing: {
        '3xl': '4rem',
      },
    },
  },
};
```

### Preset 오버라이드

```javascript
export default {
  presets: [designTokensPreset],
  theme: {
    // fontSize를 완전히 오버라이드 (권장하지 않음)
    fontSize: {
      base: '16px',
    },
  },
};
```

## 관련 문서

- [CSS Variables Reference](./css-variables.md) - 전체 CSS 변수 목록
- [Themes API](./themes.md) - 테마 시스템
- [useTheme Hook](./use-theme.md) - 테마 관리 훅
