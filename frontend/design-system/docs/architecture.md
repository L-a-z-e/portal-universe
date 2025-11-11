# Design System Architecture

## 개요

Portal Universe의 디자인 시스템은 **확장 가능한 멀티 서비스 아키텍처**를 채택합니다.
여러 서비스(Blog, Shopping 등)가 공통 토큰을 공유하면서도, 각자의 브랜드 정체성을 유지할 수 있습니다.

## 핵심 원칙

### 1. 3-Layer Token Architecture

Layer 3: Component Tokens ← 컴포넌트별 세부 스타일
(button-primary-bg, card-shadow)

Layer 2: Semantic Tokens ← 역할 기반 토큰
(brand-primary, text-body)

Layer 1: Base/Foundation Tokens ← 원자 단위 값
(green-600, spacing-4)

**Layer 1 (Base)**: 절대값 정의
- `colors.json`: green-600: #12B886
- `typography.json`: font-size-lg: 1.125rem
- `spacing.json`: spacing-4: 1rem

**Layer 2 (Semantic)**: 용도 기반 매핑
- `brand-primary`: {green-600}
- `text-body`: {gray-900}
- Light/Dark 테마에서 동일한 semantic 이름 사용

**Layer 3 (Component)**: 컴포넌트별 조합
- 실제 컴포넌트에서 semantic 토큰 조합하여 사용

### 2. Service-Specific Theme Overrides

**문제**: 서비스마다 다른 브랜드 색상이 필요하다면?
- ❌ 나쁜 방법: `blog-primary`, `shopping-primary` 무한 증식
- ✅ 좋은 방법: **동일한 semantic 이름 + Service Context로 값만 오버라이드**

**구현 방식**:
```html
<!-- Blog 서비스 --> 
<div data-service="blog" data-theme="light">
<button class="bg-brand-primary">발행</button> 
<!-- brand-primary → #12B886 (Green) -->
</div> 
<!-- Shopping 서비스 --> 
<div data-service="shopping" data-theme="light">
<button class="bg-brand-primary">구매</button>
<!-- brand-primary → #FD7E14 (Orange) --> 
</div> 
```

### 3. Dark Mode Support
모든 서비스는 Light/Dark 모드를 지원:
```text
:root {
  --color-text-body: #212529;  /* Light */
}
[data-theme="dark"] {
  --color-text-body: #ECECEC;  /* Dark */
}
```
네, 요청하신 텍스트를 마크다운 형식으로 복원했습니다.

-----

# Token Naming Convention

## Format

```
{category}-{property}-{variant?}-{state?}
```

## Examples

* `brand-primary` → 브랜드 메인 색상
* `text-body` → 본문 텍스트
* `text-meta` → 메타 정보 텍스트
* `bg-card` → 카드 배경
* `border-default` → 기본 테두리
* `button-primary-hover` → 버튼 hover 상태

## Category 종류

* **brand**: 브랜드 정체성
* **text**: 텍스트
* **bg**: 배경
* **border**: 테두리
* **interactive**: 인터랙티브 요소
* **status**: 상태 표시 (success, error, warning)

-----

# File Organization

```bash
tokens/
├── base/           # 절대값 (불변)
├── semantic/       # 역할 기반 매핑
└── themes/         # 서비스별 오버라이드
styles/
├── index.css       # 공통 CSS 변수
└── themes/         # 서비스별 CSS 오버라이드
```

-----

# Usage Example

## Vue Component

```vue
<template>
  <article class="bg-bg-card border border-border-default rounded-lg p-6">
    <h2 class="text-text-heading font-bold">제목</h2>
    <p class="text-text-body leading-relaxed">본문</p>
    <button class="bg-brand-primary hover:bg-brand-primary-hover text-white">
      액션
    </button>
  </article>
</template>
```

## Theme Context 설정

```typescript
// blog-frontend/src/main.ts
import { setThemeContext } from '@portal/design-system';

setThemeContext('blog', 'light');
```
