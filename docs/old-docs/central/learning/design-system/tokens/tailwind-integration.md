---
id: design-token-002
title: Tailwind CSS Design Token 통합
type: learning
created: 2026-01-22
updated: 2026-01-22
author: Portal Universe Team
tags:
  - design-system
  - tailwind
  - css-variables
  - preset
related:
  - design-token-001
---

# Tailwind CSS Design Token 통합

## 학습 목표

- Tailwind Preset을 통한 Token 통합 방법 이해
- CSS Custom Properties와 Tailwind 연결 구조 학습
- Dark-First 디자인을 위한 `light:` 변형 구현 이해
- Portal Universe의 Preset 확장 패턴 습득

## 1. Tailwind Preset이란?

### 1.1 개념

Tailwind Preset은 Tailwind CSS 설정을 **재사용 가능한 패키지**로 만든 것입니다.

**장점:**
- ✅ 설정 공유: 여러 프로젝트에서 동일한 설정 사용
- ✅ 일관성: Design Token을 Tailwind 유틸리티로 변환
- ✅ 유지보수: 한 곳에서 변경하면 모든 프로젝트에 반영
- ✅ 확장성: 개별 프로젝트에서 추가 커스터마이징 가능

### 1.2 Portal Universe Preset 구조

```
@portal/design-tokens (Preset 패키지)
    ↓
design-system-vue/tailwind.config.js
design-system-react/tailwind.config.js
portal-shell/tailwind.config.js
blog-frontend/tailwind.config.js
shopping-frontend/tailwind.config.js
```

## 2. Preset 구현 분석

### 2.1 기본 구조

`frontend/design-tokens/tailwind.preset.js`:

```javascript
import plugin from 'tailwindcss/plugin';

export default {
  // 1. Dark Mode 설정
  darkMode: ['class', '[data-theme="dark"]'],

  // 2. Theme 확장
  theme: {
    extend: {
      // Colors, Typography, Spacing 등...
    }
  },

  // 3. 플러그인
  plugins: [
    require('@tailwindcss/typography'),
    plugin(function({ addVariant }) {
      // Custom variants
    }),
  ]
}
```

### 2.2 개별 프로젝트 설정

#### design-system-vue
```javascript
// frontend/design-system-vue/tailwind.config.js
import preset from './tailwind.preset.js';

export default {
  presets: [preset],  // Preset 적용
  content: [
    './src/**/*.{vue,js,ts,jsx,tsx}',
  ],
}
```

#### design-system-react
```javascript
// frontend/design-system-react/tailwind.config.js
import preset from '@portal/design-tokens/tailwind';

export default {
  presets: [preset],  // NPM 패키지로 가져오기
  content: [
    './src/**/*.{js,ts,jsx,tsx}',
  ],
};
```

## 3. CSS Variables → Tailwind 매핑

### 3.1 Color Mapping

#### CSS Variables 정의
```css
/* CSS Variables */
:root {
  --semantic-brand-primary: #5e6ad2;
  --semantic-text-body: #ebeced;
  --semantic-bg-card: #0e0f10;
}
```

#### Tailwind Preset에서 참조
```javascript
// tailwind.preset.js
theme: {
  extend: {
    colors: {
      'brand': {
        'primary': 'var(--semantic-brand-primary)',  // CSS Variable 사용
      },
      'text': {
        'body': 'var(--semantic-text-body)',
      },
      'bg': {
        'card': 'var(--semantic-bg-card)',
      },
    }
  }
}
```

#### 컴포넌트에서 사용
```tsx
// ✅ Tailwind 유틸리티로 사용
<div className="bg-bg-card text-text-body">
  <button className="bg-brand-primary">Click</button>
</div>
```

**컴파일 결과:**
```css
.bg-bg-card {
  background-color: var(--semantic-bg-card);
}
.text-text-body {
  color: var(--semantic-text-body);
}
.bg-brand-primary {
  background-color: var(--semantic-brand-primary);
}
```

### 3.2 Typography Mapping

```javascript
// Preset
fontFamily: {
  'sans': ['Inter Variable', 'Inter', 'sans-serif'],
  'mono': ['JetBrains Mono', 'Fira Code', 'monospace'],
},
fontSize: {
  'micro': ['0.625rem', { lineHeight: '1' }],
  'xs': ['0.6875rem', { lineHeight: '1.2' }],
  'sm': ['0.8125rem', { lineHeight: '1.4' }],
  'base': ['0.875rem', { lineHeight: '1.5' }],
}
```

```tsx
// 사용
<p className="font-sans text-base">Body text</p>
<code className="font-mono text-sm">console.log()</code>
```

### 3.3 Spacing Mapping

```javascript
// Preset
spacing: {
  'xs': 'var(--spacing-xs)',    // 4px
  'sm': 'var(--spacing-sm)',    // 8px
  'md': 'var(--spacing-md)',    // 16px
  'lg': 'var(--spacing-lg)',    // 24px
}
```

```tsx
// 사용
<div className="p-md mb-lg">
  <h1 className="mb-sm">Title</h1>
</div>
```

## 4. Dark-First 아키텍처

### 4.1 `light:` 변형 구현

Portal Universe는 **다크모드가 기본**이며, 라이트모드는 `light:` 변형으로 오버라이드합니다.

#### Preset 플러그인
```javascript
plugins: [
  plugin(function({ addVariant }) {
    // 'light:' 변형 추가
    addVariant('light', [
      '[data-theme="light"] &',  // data-theme="light"일 때
      '.light &'                 // .light 클래스일 때
    ]);
  }),
]
```

#### 사용 예시
```tsx
// Dark mode (기본) + Light mode (오버라이드)
<button className="
  bg-white/90 text-[#08090a]              {/* 다크모드 기본 */}
  light:bg-brand-primary light:text-white {/* 라이트모드 오버라이드 */}
">
  Click me
</button>
```

#### HTML에서 테마 전환
```html
<!-- Dark Mode (기본) -->
<html data-theme="dark" class="dark">
  <button class="bg-white/90 text-[#08090a] light:bg-blue-600">
    <!-- bg-white/90 적용 -->
  </button>
</html>

<!-- Light Mode -->
<html data-theme="light" class="light">
  <button class="bg-white/90 text-[#08090a] light:bg-blue-600">
    <!-- light:bg-blue-600 적용 -->
  </button>
</html>
```

### 4.2 Button 컴포넌트 실제 사례

`frontend/design-system-react/src/components/Button/Button.tsx`:

```tsx
const variantClasses = {
  primary: [
    // Dark mode (기본)
    'bg-white/90 text-[#08090a]',
    'hover:bg-white',
    // Light mode (오버라이드)
    'light:bg-brand-primary light:text-white',
    'light:hover:bg-brand-primaryHover',
  ].join(' '),

  secondary: [
    'bg-transparent text-text-body',
    'hover:bg-white/5',
    'border border-[#2a2a2a]',
    'light:hover:bg-gray-100',
    'light:border-gray-200'
  ].join(' '),
};
```

## 5. 실습 예제

### 예제 1: Preset 확장하기

개별 프로젝트에서 Preset을 **확장**할 수 있습니다:

```javascript
// shopping-frontend/tailwind.config.js
import preset from '@portal/design-tokens/tailwind';

export default {
  presets: [preset],
  content: ['./src/**/*.{js,ts,jsx,tsx}'],

  theme: {
    extend: {
      // Shopping 서비스 전용 색상 추가
      colors: {
        'shopping': {
          'primary': '#F97316',     // 오렌지
          'secondary': '#EA580C',
        }
      }
    }
  }
}
```

```tsx
// 사용
<button className="bg-shopping-primary hover:bg-shopping-secondary">
  Add to Cart
</button>
```

### 예제 2: Animation Token 사용

```javascript
// Preset에 정의됨
animation: {
  'fade-in': 'fade-in 160ms cubic-bezier(0.25, 0.1, 0.25, 1)',
  'scale-in': 'scale-in 160ms cubic-bezier(0.25, 0.1, 0.25, 1)',
}
```

```tsx
// 사용
<div className="animate-fade-in">
  Fade in animation
</div>

<div className="animate-scale-in">
  Scale in animation
</div>
```

### 예제 3: Responsive Spacing

```tsx
// Preset의 spacing token 활용
<div className="
  p-sm          {/* 모바일: 8px */}
  md:p-md       {/* 태블릿: 16px */}
  lg:p-lg       {/* 데스크탑: 24px */}
">
  Responsive padding
</div>
```

## 6. 고급 패턴

### 6.1 Service-Specific Theming

Portal Universe는 서비스별 테마를 지원합니다:

```html
<html data-service="blog">
  <!-- Blog 서비스 테마 적용 -->
</html>

<html data-service="shopping">
  <!-- Shopping 서비스 테마 적용 -->
</html>
```

CSS Variables로 서비스별 브랜드 색상을 오버라이드:

```css
/* Base */
:root {
  --semantic-brand-primary: #5e6ad2;  /* Portal (기본) */
}

/* Blog Theme */
[data-service="blog"] {
  --semantic-brand-primary: #10b981;  /* Green */
}

/* Shopping Theme */
[data-service="shopping"] {
  --semantic-brand-primary: #F97316;  /* Orange */
}
```

```tsx
// 컴포넌트는 변경 없이 테마에 따라 색상 자동 변경
<button className="bg-brand-primary">
  {/* Portal: 보라색, Blog: 초록색, Shopping: 오렌지 */}
</button>
```

### 6.2 Custom Variant 추가

```javascript
// tailwind.config.js
plugins: [
  plugin(function({ addVariant }) {
    // 'service-blog:' 변형 추가
    addVariant('service-blog', '[data-service="blog"] &');
    addVariant('service-shopping', '[data-service="shopping"] &');
  }),
]
```

```tsx
// 사용
<div className="
  bg-brand-primary
  service-blog:bg-green-500
  service-shopping:bg-orange-500
">
  Service-specific styling
</div>
```

## 7. 핵심 요약

### ✅ Key Takeaways

1. **Preset = 재사용 가능한 Tailwind 설정**
2. **CSS Variables → Tailwind 유틸리티 매핑**
3. **Dark-First + `light:` 변형**
4. **개별 프로젝트에서 Preset 확장 가능**
5. **Service-Specific 테마 지원**

### 🎯 Architecture Flow

```
CSS Variables 정의
    ↓
Tailwind Preset에서 참조
    ↓
개별 프로젝트에서 presets 배열로 적용
    ↓
컴포넌트에서 Tailwind 유틸리티 사용
    ↓
런타임에 CSS Variable 값 동적 변경 (테마 전환)
```

### 📋 Checklist

```tsx
// ✅ DO
// 1. Preset에서 제공하는 Semantic Token 사용
<div className="bg-bg-card text-text-body">

// 2. light: 변형으로 라이트모드 스타일 추가
<button className="bg-white light:bg-brand-primary">

// 3. CSS Variable을 직접 사용하지 말고 Tailwind 유틸리티 사용
<div className="text-text-heading">  // ✅

// ❌ DON'T
// 1. 하드코딩된 Tailwind 색상 사용
<div className="bg-gray-900 text-gray-300">  // ❌

// 2. 인라인 스타일로 CSS Variable 사용
<div style={{ color: 'var(--semantic-text-body)' }}>  // ❌

// 3. Preset을 무시하고 개별 설정에 중복 정의
theme: {
  colors: {
    'brand-primary': '#5e6ad2'  // ❌ Preset에 이미 있음
  }
}
```

## 8. 관련 문서

- [Design Tokens](./design-tokens.md) - Token 개념과 구조
- [Theming Pattern](../patterns/theming.md) - 테마 시스템 구현
- [Dual Framework](../patterns/dual-framework.md) - Vue/React에서 동일 Token 사용
