---
id: token-system
title: Token System Architecture
type: architecture
status: current
created: 2026-01-19
updated: 2026-01-19
author: documenter
tags: [architecture, design-tokens, css-variables]
related:
  - css-variables
  - getting-started
---

# Token System Architecture

## 개요

Design Tokens는 디자인 결정사항을 플랫폼에 구애받지 않는 방식으로 저장합니다. 이를 통해 일관된 디자인 언어를 유지하면서 다양한 플랫폼(웹, iOS, Android)에서 동일한 스타일을 적용할 수 있습니다.

## 3계층 토큰 시스템

### Layer 1: Base Tokens (Primitive)

기본 값을 정의하는 가장 낮은 레벨의 토큰입니다. 직접 사용하지 않고 Semantic Tokens에서 참조합니다.

```json
{
  "color": {
    "indigo": {
      "400": "#5e6ad2",
      "500": "#4754c9"
    },
    "green": {
      "500": "#20C997",
      "600": "#12B886"
    }
  }
}
```

**파일 위치:**
- `src/tokens/base/colors.json`
- `src/tokens/base/typography.json`
- `src/tokens/base/spacing.json`
- `src/tokens/base/border.json`
- `src/tokens/base/effects.json`

### Layer 2: Semantic Tokens

역할 기반의 토큰으로, Base Tokens를 참조하여 의미 있는 이름을 부여합니다.

```json
{
  "color": {
    "brand": {
      "primary": {
        "$value": "{color.indigo.400}",
        "$description": "Primary brand color"
      }
    },
    "text": {
      "body": {
        "$value": "{color.linear.300}",
        "$description": "Body text color"
      }
    }
  }
}
```

**파일 위치:**
- `src/tokens/semantic/colors.json`

### Layer 3: Component Tokens

Tailwind 클래스나 컴포넌트 스타일에서 사용되는 실제 적용 토큰입니다.

```css
/* Tailwind 클래스로 노출 */
.bg-brand-primary { background-color: var(--semantic-brand-primary); }
.text-body { color: var(--semantic-text-body); }
```

## 토큰 카테고리

### Colors

| Category | Description | Example |
|----------|-------------|---------|
| `brand` | 브랜드 색상 | `--semantic-brand-primary` |
| `text` | 텍스트 색상 | `--semantic-text-body` |
| `bg` | 배경 색상 | `--semantic-bg-card` |
| `border` | 테두리 색상 | `--semantic-border-default` |
| `status` | 상태 색상 | `--semantic-status-success` |
| `accent` | 강조 색상 | `--semantic-accent-indigo` |

### Typography

| Property | CSS Variable | Example |
|----------|--------------|---------|
| Font Family | `--typography-fontFamily-*` | `--typography-fontFamily-sans` |
| Font Size | `--typography-fontSize-*` | `--typography-fontSize-base` |
| Font Weight | `--typography-fontWeight-*` | `--typography-fontWeight-medium` |
| Line Height | `--typography-lineHeight-*` | `--typography-lineHeight-normal` |
| Letter Spacing | `--typography-letterSpacing-*` | `--typography-letterSpacing-tight` |

### Spacing

| Size | Value | CSS Variable |
|------|-------|--------------|
| xs | 0.25rem | `--spacing-xs` |
| sm | 0.5rem | `--spacing-sm` |
| md | 1rem | `--spacing-md` |
| lg | 1.5rem | `--spacing-lg` |
| xl | 2rem | `--spacing-xl` |
| 2xl | 3rem | `--spacing-2xl` |

### Effects

| Type | CSS Variable | Description |
|------|--------------|-------------|
| Shadow | `--effects-shadow-*` | 그림자 효과 |
| Glass | `--effects-glass-*` | 글래스모피즘 효과 |
| Animation | `--effects-animation-*` | 애니메이션 타이밍 |
| Opacity | `--effects-opacity-*` | 투명도 값 |

## 테마 시스템

### 서비스별 테마

각 서비스는 고유한 테마를 가지며, `data-service` 속성으로 전환합니다.

```css
/* Portal (기본) - Linear 스타일 다크 테마 */
[data-service="portal"] {
  --semantic-brand-primary: #5e6ad2;
  --semantic-bg-page: #08090a;
}

/* Blog - 그린 계열 라이트 테마 */
[data-service="blog"] {
  --semantic-brand-primary: #12B886;
  --semantic-bg-page: #F8F9FA;
}

/* Shopping - 오렌지 계열 라이트 테마 */
[data-service="shopping"] {
  --semantic-brand-primary: #FD7E14;
  --semantic-bg-page: #ffffff;
}
```

### 다크/라이트 모드

`data-theme` 속성으로 다크/라이트 모드를 전환합니다.

```css
/* Light mode */
[data-theme="light"] {
  --semantic-bg-page: #f7f8f8;
  --semantic-text-body: #3e3e44;
}

/* Dark mode */
[data-theme="dark"] {
  --semantic-bg-page: #0F1419;
  --semantic-text-body: #C9D1D9;
}
```

### 조합 예시

```html
<!-- Portal 서비스, 라이트 모드 -->
<body data-service="portal" data-theme="light">

<!-- Blog 서비스, 다크 모드 -->
<body data-service="blog" data-theme="dark">
```

## 빌드 파이프라인

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  JSON Tokens    │ -> │  Build Script   │ -> │  Output Files   │
│  (src/tokens/)  │    │  (build-tokens) │    │  (dist/)        │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                              ▼
                    ┌─────────────────────────────────────────┐
                    │  - tokens.css (CSS Variables)           │
                    │  - tokens.json (Raw Data)               │
                    │  - tokens.js (ESM Module)               │
                    │  - tokens.cjs (CommonJS)                │
                    │  - tokens.d.ts (TypeScript Types)       │
                    └─────────────────────────────────────────┘
```

## 디자인 원칙

### 1. Single Source of Truth
모든 디자인 값은 JSON 토큰 파일에서 시작합니다.

### 2. Semantic Naming
색상은 `red-500` 대신 `error`, `success` 등 의미 있는 이름을 사용합니다.

### 3. Consistent Scaling
폰트 크기, 간격 등은 일관된 스케일을 따릅니다.

### 4. Theme Flexibility
테마 전환 시에도 컴포넌트 코드 변경 없이 스타일이 적용됩니다.
