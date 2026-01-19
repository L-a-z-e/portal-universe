---
id: customization
title: Customization Guide
type: guide
status: current
created: 2026-01-19
updated: 2026-01-19
author: documenter
tags: [guide, customization, theming]
related:
  - token-system
  - getting-started
---

# Customization Guide

Design Tokens를 커스터마이징하여 프로젝트에 맞는 테마를 만드는 방법을 안내합니다.

## 방법 1: CSS 오버라이드

가장 간단한 방법으로, CSS 변수를 직접 오버라이드합니다.

### 전역 오버라이드

```css
/* 전역 브랜드 색상 변경 */
:root {
  --semantic-brand-primary: #8b5cf6; /* purple */
  --semantic-brand-primaryHover: #7c3aed;
  --semantic-brand-secondary: #a78bfa;
}
```

### 특정 영역 오버라이드

```css
/* 특정 섹션에만 적용 */
.promo-section {
  --semantic-brand-primary: #f59e0b;
  --semantic-bg-card: #fffbeb;
}
```

### 미디어 쿼리 기반 오버라이드

```css
/* 시스템 다크 모드 감지 */
@media (prefers-color-scheme: dark) {
  :root {
    --semantic-bg-page: #0f0f0f;
    --semantic-text-body: #e5e5e5;
  }
}
```

## 방법 2: 커스텀 테마 생성

새로운 서비스 테마를 추가할 수 있습니다.

### 1. 테마 JSON 파일 생성

```json
// src/tokens/themes/my-service.json
{
  "color": {
    "brand": {
      "primary": {
        "$value": "{color.purple.500}",
        "$description": "My service primary color"
      }
    }
  }
}
```

### 2. 빌드 스크립트 수정

```js
// scripts/build-tokens.js
const themes = ['portal', 'blog', 'shopping', 'my-service'];
```

### 3. CSS에서 사용

```css
[data-service="my-service"] {
  --semantic-brand-primary: var(--custom-purple);
  /* ... */
}
```

## 방법 3: Tailwind 확장

Tailwind 설정을 확장하여 추가 토큰을 정의합니다.

```js
// tailwind.config.js
module.exports = {
  presets: [require('@portal/design-tokens/tailwind.preset')],
  theme: {
    extend: {
      colors: {
        // 커스텀 색상 추가
        'brand-accent': 'var(--custom-accent)',
      },
      spacing: {
        // 커스텀 spacing 추가
        '18': '4.5rem',
      },
    },
  },
};
```

## 새 색상 팔레트 추가

### 1. Base 토큰 추가

```json
// src/tokens/base/colors.json
{
  "color": {
    "purple": {
      "$description": "Purple palette for accent",
      "50": "#faf5ff",
      "100": "#f3e8ff",
      "200": "#e9d5ff",
      "300": "#d8b4fe",
      "400": "#c084fc",
      "500": "#a855f7",
      "600": "#9333ea",
      "700": "#7e22ce",
      "800": "#6b21a8",
      "900": "#581c87"
    }
  }
}
```

### 2. Semantic 토큰에서 참조

```json
// src/tokens/semantic/colors.json
{
  "color": {
    "accent": {
      "purple": {
        "$value": "{color.purple.500}",
        "$type": "color"
      }
    }
  }
}
```

### 3. 빌드 실행

```bash
npm run build
```

## 타이포그래피 커스터마이징

### 폰트 패밀리 변경

```json
// src/tokens/base/typography.json
{
  "typography": {
    "fontFamily": {
      "sans": "var(--custom-font-family), sans-serif"
    }
  }
}
```

### 폰트 크기 스케일 조정

```json
{
  "typography": {
    "fontSize": {
      "base": "1rem", // 기본값 변경
      "lg": "1.125rem"
    }
  }
}
```

## 다크 모드 커스터마이징

### 다크 모드 색상 오버라이드

```css
[data-theme="dark"] {
  --semantic-bg-page: #0a0a0a;
  --semantic-bg-card: #171717;
  --semantic-text-body: #d4d4d4;
  --semantic-border-default: #262626;
}
```

### 서비스별 다크 모드

```css
[data-service="blog"][data-theme="dark"] {
  --semantic-brand-primary: #34d399;
  --semantic-bg-page: #022c22;
}
```

## 컴포넌트 레벨 커스터마이징

컴포넌트 내에서 토큰을 오버라이드할 수 있습니다.

### Vue

```vue
<template>
  <div class="custom-card">
    <slot />
  </div>
</template>

<style scoped>
.custom-card {
  --semantic-bg-card: #fef3c7;
  --semantic-border-default: #fbbf24;

  background: var(--semantic-bg-card);
  border: 1px solid var(--semantic-border-default);
}
</style>
```

### React

```tsx
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

## 런타임 테마 전환

JavaScript로 런타임에 토큰 값을 변경할 수 있습니다.

```ts
// 특정 토큰 값 변경
document.documentElement.style.setProperty(
  '--semantic-brand-primary',
  '#8b5cf6'
);

// 여러 토큰 한번에 변경
const customTheme = {
  '--semantic-brand-primary': '#8b5cf6',
  '--semantic-bg-page': '#faf5ff',
  '--semantic-text-body': '#1e1b4b',
};

Object.entries(customTheme).forEach(([key, value]) => {
  document.documentElement.style.setProperty(key, value);
});
```

## 모범 사례

### 1. 계층 유지

Base → Semantic 순서를 유지하세요.

```css
/* Good: Semantic 토큰 오버라이드 */
:root {
  --semantic-brand-primary: #8b5cf6;
}

/* Avoid: Base 토큰 직접 사용 */
.button {
  background: var(--color-purple-500);
}
```

### 2. 접근성 고려

색상 대비를 충분히 유지하세요.

```css
/* 배경과 텍스트의 대비 확인 */
:root {
  --semantic-bg-card: #1f1f1f;
  --semantic-text-body: #e5e5e5; /* 충분한 대비 */
}
```

### 3. 문서화

커스텀 토큰을 문서화하세요.

```js
/**
 * @token --custom-promo-bg
 * @description Promotional banner background
 * @value #fef3c7
 */
```

## 트러블슈팅

### 토큰이 적용되지 않음

1. CSS 임포트 순서 확인
2. specificity 확인
3. 브라우저 개발자 도구에서 CSS 변수 값 확인

### 테마 전환이 작동하지 않음

1. `data-service`, `data-theme` 속성 확인
2. CSS 선택자 우선순위 확인
3. JavaScript 이벤트 리스너 확인
