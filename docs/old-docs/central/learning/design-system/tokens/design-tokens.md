---
id: design-token-001
title: Design Tokens - 색상, 타이포그래피, 간격
type: learning
created: 2026-01-22
updated: 2026-01-22
author: Laze
tags:
  - design-system
  - design-tokens
  - css-variables
  - theming
related:
  - design-token-002
---

# Design Tokens - 색상, 타이포그래피, 간격

## 학습 목표

- Design Token의 개념과 3-tier 구조 이해
- Portal Universe의 Semantic Token 시스템 이해
- CSS Custom Properties 기반 Token 구현 방법 학습
- Linear-inspired Dark-First 디자인 철학 이해

## 1. Design Tokens란?

### 1.1 개념

Design Token은 디자인 시스템의 시각적 속성(색상, 타이포그래피, 간격 등)을 **명명된 변수로 저장**한 것입니다.

**장점:**
- ✅ 일관성: 전체 시스템에서 동일한 값 사용
- ✅ 유지보수성: 한 곳에서 변경하면 전체 반영
- ✅ 확장성: 다크모드, 테마 변경 용이
- ✅ 플랫폼 무관: CSS, JavaScript, iOS, Android 모두 지원 가능

### 1.2 3-Tier Token 구조

Portal Universe는 3계층 Token 시스템을 사용합니다:

| Layer | 이름 | 예시 | 설명 |
|-------|------|------|------|
| 1 | **Base Tokens** | `green-600`, `spacing-4` | 원시 값 (Primitive) |
| 2 | **Semantic Tokens** | `brand-primary`, `text-body` | 역할 기반 추상화 |
| 3 | **Component Tokens** | Component class에서 적용 | 실제 사용 |

```
Base Token (green-600: #10b981)
    ↓
Semantic Token (brand-primary: var(--green-600))
    ↓
Component (bg-brand-primary)
```

## 2. Portal Universe Token 시스템

### 2.1 색상 Tokens

#### Brand Colors
```css
/* CSS Variables */
--semantic-brand-primary: #5e6ad2;
--semantic-brand-primaryHover: #4754c9;
--semantic-brand-secondary: #3e3e44;
```

```tsx
// Tailwind 사용
<button className="bg-brand-primary hover:bg-brand-primaryHover">
  Click me
</button>
```

#### Text Colors
```css
--semantic-text-heading: #f7f8f8;     /* 제목 */
--semantic-text-body: #ebeced;        /* 본문 */
--semantic-text-meta: #8a8f98;        /* 메타정보 */
--semantic-text-muted: #6c717a;       /* 약한 텍스트 */
--semantic-text-inverse: #08090a;     /* 역전 (흰 배경에 검은 글씨) */
--semantic-text-link: #5e6ad2;        /* 링크 */
--semantic-text-linkHover: #4754c9;   /* 링크 호버 */
```

#### Background Colors
```css
--semantic-bg-page: #08090a;          /* 페이지 배경 */
--semantic-bg-card: #0e0f10;          /* 카드 배경 */
--semantic-bg-elevated: #1b1c1e;      /* 높이 있는 요소 */
--semantic-bg-muted: #26282b;         /* 약한 배경 */
--semantic-bg-hover: rgba(255, 255, 255, 0.05);  /* 호버 효과 */
```

#### Border Colors
```css
--semantic-border-default: #2a2a2a;
--semantic-border-hover: #3a3a3a;
--semantic-border-focus: #5e6ad2;
--semantic-border-muted: rgba(255, 255, 255, 0.1);
```

#### Status Colors
```css
--semantic-status-success: #10b981;
--semantic-status-successBg: rgba(16, 185, 129, 0.1);
--semantic-status-error: #E03131;
--semantic-status-errorBg: rgba(224, 49, 49, 0.1);
--semantic-status-warning: #F59E0B;
--semantic-status-warningBg: rgba(245, 158, 11, 0.1);
--semantic-status-info: #3B82F6;
--semantic-status-infoBg: rgba(59, 130, 246, 0.1);
```

### 2.2 타이포그래피 Tokens

#### Font Family
```javascript
fontFamily: {
  'sans': [
    'Inter Variable',
    'Inter',
    '-apple-system',
    'BlinkMacSystemFont',
    'Pretendard Variable',
    'sans-serif'
  ],
  'mono': [
    'JetBrains Mono',
    'Fira Code',
    'SF Mono',
    'monospace'
  ],
}
```

#### Font Sizes (Linear-inspired compact sizing)
```javascript
fontSize: {
  'micro': ['0.625rem', { lineHeight: '1' }],      // 10px
  'xs':    ['0.6875rem', { lineHeight: '1.2' }],   // 11px
  'sm':    ['0.8125rem', { lineHeight: '1.4' }],   // 13px
  'base':  ['0.875rem', { lineHeight: '1.5' }],    // 14px (기본)
  'lg':    ['1rem', { lineHeight: '1.5' }],        // 16px
  'xl':    ['1.125rem', { lineHeight: '1.4' }],    // 18px
  '2xl':   ['1.25rem', { lineHeight: '1.3' }],     // 20px
  '3xl':   ['1.5rem', { lineHeight: '1.3' }],      // 24px
  '4xl':   ['1.875rem', { lineHeight: '1.2' }],    // 30px
}
```

#### Font Weights
```javascript
fontWeight: {
  'light': '300',
  'normal': '400',
  'medium': '510',       // Inter Variable 최적화
  'semibold': '590',     // Inter Variable 최적화
  'bold': '680',         // Inter Variable 최적화
  'extrabold': '800',
}
```

### 2.3 Spacing Tokens

```css
--spacing-xs: 0.25rem;   /* 4px */
--spacing-sm: 0.5rem;    /* 8px */
--spacing-md: 1rem;      /* 16px */
--spacing-lg: 1.5rem;    /* 24px */
--spacing-xl: 2rem;      /* 32px */
--spacing-2xl: 3rem;     /* 48px */
```

```tsx
// Tailwind 사용
<div className="p-md mb-lg">
  <h1 className="mb-sm">Title</h1>
  <p className="mt-xs">Content</p>
</div>
```

### 2.4 Border Radius Tokens

```javascript
borderRadius: {
  'none': '0',
  'sm': '0.25rem',      // 4px
  'DEFAULT': '0.375rem', // 6px
  'md': '0.5rem',       // 8px
  'lg': '0.75rem',      // 12px
  'xl': '1rem',         // 16px
  '2xl': '1.5rem',      // 24px
  'full': '9999px',
}
```

### 2.5 Shadow Tokens

```javascript
boxShadow: {
  'sm': '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
  'DEFAULT': '0 1px 3px 0 rgba(0, 0, 0, 0.1)',
  'md': '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
  'lg': '0 10px 15px -3px rgba(0, 0, 0, 0.1)',
  'xl': '0 20px 25px -5px rgba(0, 0, 0, 0.1)',
  '2xl': '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
  'glow': '0 0 20px rgba(94, 106, 210, 0.3)',      // 브랜드 글로우
  'glow-lg': '0 0 40px rgba(94, 106, 210, 0.4)',
}
```

## 3. Portal Universe 구현 분석

### 3.1 파일 구조

```
frontend/design-tokens/
├── src/
│   └── tokens/           # CSS Variables 정의
├── tailwind.preset.js    # Tailwind 설정
└── package.json
```

### 3.2 Tailwind Preset 구현

`frontend/design-tokens/tailwind.preset.js`:

```javascript
export default {
  darkMode: ['class', '[data-theme="dark"]'],  // 다크모드 우선

  theme: {
    extend: {
      colors: {
        'brand': {
          'primary': 'var(--semantic-brand-primary)',
          'primaryHover': 'var(--semantic-brand-primaryHover)',
        },
        'text': {
          'heading': 'var(--semantic-text-heading)',
          'body': 'var(--semantic-text-body)',
          'meta': 'var(--semantic-text-meta)',
        },
        // ... 더 많은 토큰
      }
    }
  },

  plugins: [
    // 'light:' 변형 추가
    plugin(function({ addVariant }) {
      addVariant('light', ['[data-theme="light"] &', '.light &']);
    }),
  ]
}
```

### 3.3 실제 사용 예시

#### Vue Component
```vue
<template>
  <div class="bg-bg-card border border-border-default rounded-lg p-md">
    <h2 class="text-text-heading text-lg font-semibold mb-sm">
      Card Title
    </h2>
    <p class="text-text-body text-base">
      Card content with semantic tokens
    </p>
  </div>
</template>
```

#### React Component
```tsx
export const Card: React.FC = () => {
  return (
    <div className="bg-bg-card border border-border-default rounded-lg p-md">
      <h2 className="text-text-heading text-lg font-semibold mb-sm">
        Card Title
      </h2>
      <p className="text-text-body text-base">
        Card content with semantic tokens
      </p>
    </div>
  );
};
```

## 4. 실습 예제

### 예제 1: Token을 사용한 Button

```tsx
// ❌ Bad - 하드코딩된 값
<button className="bg-blue-600 text-white px-4 py-2 rounded">
  Click me
</button>

// ✅ Good - Semantic Token 사용
<button className="bg-brand-primary text-text-inverse px-4 py-2 rounded-md
                   hover:bg-brand-primaryHover transition-colors">
  Click me
</button>
```

### 예제 2: Status Badge

```tsx
// Success Badge
<span className="px-2 py-1 rounded-md text-xs font-medium
                 bg-status-successBg text-status-success
                 border border-status-success/20">
  Success
</span>

// Error Badge
<span className="px-2 py-1 rounded-md text-xs font-medium
                 bg-status-errorBg text-status-error
                 border border-status-error/20">
  Error
</span>
```

### 예제 3: Dark/Light Mode 대응

```tsx
// 자동으로 다크/라이트 모드에 대응
<div className="bg-bg-page text-text-body">
  <header className="bg-bg-card border-b border-border-default">
    <h1 className="text-text-heading">Portal Universe</h1>
  </header>
</div>
```

## 5. 핵심 요약

### ✅ Key Takeaways

1. **3-Tier 구조**: Base → Semantic → Component
2. **Semantic Token 사용**: `bg-brand-primary` (O), `bg-blue-600` (X)
3. **CSS Variables 기반**: 런타임 테마 변경 가능
4. **Dark-First**: 다크모드가 기본, `light:` 변형으로 라이트모드 오버라이드
5. **Linear-inspired**: 컴팩트한 폰트 크기, 정교한 그레이스케일

### 🎯 Best Practices

```tsx
// ✅ DO
<div className="bg-bg-card text-text-body">
<button className="bg-brand-primary hover:bg-brand-primaryHover">
<p className="text-text-meta">

// ❌ DON'T
<div className="bg-gray-900 text-gray-300">
<button className="bg-blue-600 hover:bg-blue-700">
<p className="text-gray-500">
```

## 6. 관련 문서

- [Tailwind Integration](./tailwind-integration.md) - Tailwind CSS 토큰 매핑
- [Theming Pattern](../patterns/theming.md) - 다크모드 & 서비스별 테마
- [Button Component](../components/button-component.md) - Token 적용 사례
