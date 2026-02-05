---
id: design-tokens-index
title: Design Tokens
type: guide
status: current
created: 2026-01-19
updated: 2026-01-19
author: documenter
tags: [design-tokens, css-variables, theming]
related:
  - design-types-index
  - design-system-vue-index
  - design-system-react-index
---

# @portal/design-tokens

Portal Universe Design System의 핵심 토큰 시스템입니다. 모든 UI 컴포넌트에서 공유하는 색상, 타이포그래피, 간격, 효과 등의 디자인 토큰을 정의합니다.

## 개요

Design Tokens는 3계층 토큰 시스템을 기반으로 합니다:

```
┌─────────────────────────────────────────────────────────────┐
│  Layer 3: Component Tokens                                  │
│  (Tailwind classes, Component styles)                       │
├─────────────────────────────────────────────────────────────┤
│  Layer 2: Semantic Tokens                                   │
│  (--semantic-brand-primary, --semantic-text-body)           │
├─────────────────────────────────────────────────────────────┤
│  Layer 1: Base Tokens                                       │
│  (--color-indigo-400, --spacing-md)                         │
└─────────────────────────────────────────────────────────────┘
```

## 설치

```bash
npm install @portal/design-tokens
```

## 빠른 시작

### CSS 변수 사용

```css
/* tokens.css 임포트 */
@import '@portal/design-tokens/dist/tokens.css';

/* CSS 변수 사용 */
.my-component {
  background-color: var(--semantic-bg-card);
  color: var(--semantic-text-body);
  padding: var(--spacing-md);
}
```

### Tailwind 통합

```js
// tailwind.config.js
module.exports = {
  presets: [require('@portal/design-tokens/tailwind.preset')],
};
```

### JavaScript/TypeScript

```ts
import tokens from '@portal/design-tokens';

console.log(tokens.color.indigo['400']); // '#5e6ad2'
```

## 서비스 테마

Portal Universe는 서비스별 테마를 지원합니다:

| 서비스 | data-service | 특징 |
|--------|--------------|------|
| Portal | `portal` | Linear 스타일 다크 테마 (기본) |
| Blog | `blog` | 그린 계열 라이트 테마 |
| Shopping | `shopping` | 오렌지 계열 라이트 테마 |

```html
<!-- 서비스 테마 적용 -->
<body data-service="blog">
  <!-- Blog 테마 스타일 적용 -->
</body>
```

## 문서 구조

### Architecture
- [token-system.md](./architecture/token-system.md) - 토큰 계층 구조 및 설계

### API Reference
- [css-variables.md](./api/css-variables.md) - CSS 변수 레퍼런스

### Guides
- [getting-started.md](./guides/getting-started.md) - 시작 가이드
- [customization.md](./guides/customization.md) - 커스터마이징 가이드

## 패키지 출력

| 파일 | 형식 | 용도 |
|------|------|------|
| `dist/tokens.css` | CSS | CSS 변수 정의 |
| `dist/tokens.json` | JSON | 토큰 데이터 |
| `dist/tokens.js` | ESM | JavaScript 모듈 |
| `dist/tokens.cjs` | CommonJS | Node.js 모듈 |
| `dist/tokens.d.ts` | TypeScript | 타입 정의 |
| `tailwind.preset.js` | JS | Tailwind 프리셋 |

## 관련 패키지

- [@portal/design-types](../design-types) - 컴포넌트 타입 정의
- [@portal/design-system-vue](../design-system-vue) - Vue 컴포넌트
- [@portal/design-system-react](../design-system-react) - React 컴포넌트
