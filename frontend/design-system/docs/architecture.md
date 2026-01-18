# Design System 아키텍처

## 개요

Portal Universe Design System은 **3-계층 토큰 시스템**을 기반으로 하여 Base → Semantic → Component 단계의 추상화 레벨을 통해 일관되고 확장 가능한 디자인을 제공합니다.

## 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────┐
│           Vue 3 Components (.vue)                       │
│  (Button, Input, Modal, Card 등 21개 컴포넌트)          │
└────────────────┬────────────────────────────────────────┘
                 │ import styles
                 ▼
┌─────────────────────────────────────────────────────────┐
│      Component CSS Classes (Tailwind utilities)         │
│  bg-brand-primary, text-heading, px-md 등               │
└────────────────┬────────────────────────────────────────┘
                 │ var(--semantic-*)
                 ▼
┌─────────────────────────────────────────────────────────┐
│   Semantic Layer (의미 기반 색상 및 스타일 토큰)        │
│  --semantic-brand-primary, --semantic-text-heading      │
│  --semantic-bg-page, --semantic-border-default          │
└────────────────┬────────────────────────────────────────┘
                 │ {color.brand.primary} 등 참조
                 ▼
┌─────────────────────────────────────────────────────────┐
│    Base Layer (Primitive Design Tokens - JSON)          │
│  color.brand.primary: #20C997 (기본)                   │
│  color.green: Green palette (Blog 서비스용)            │
│  color.orange: Orange palette (Shopping 서비스용)      │
│  typography, spacing, border-radius 토큰               │
└────────────────┬────────────────────────────────────────┘
                 │ data-service="blog|shopping"
                 ▼
┌─────────────────────────────────────────────────────────┐
│        Service-specific Theme Overrides                 │
│  blog.json: brand-primary=#20C997 (green)              │
│  shopping.json: brand-primary=#FF922B (orange)         │
└─────────────────────────────────────────────────────────┘
```

## 3-계층 토큰 시스템

### 1. Base Layer (기본 토큰)

**위치**: `src/tokens/base/`

원시적인 디자인 값을 정의합니다.

```json
{
  "color": {
    "brand": {
      "primary": "#20C997",
      "primaryHover": "#12B886",
      "secondary": "#38D9A9"
    },
    "neutral": { "white": "#ffffff", "50": "#f9fafb", ... },
    "green": { "50": "#E6FCF5", "600": "#12B886", ... },
    "orange": { "50": "#FFF4E6", "600": "#FD7E14", ... }
  },
  "typography": { "fontFamily": "...", "fontSize": "..." },
  "spacing": { "xs": "0.5rem", "sm": "1rem", ... },
  "border": { "radius": "...", "width": "..." }
}
```

**파일 목록**:
- `colors.json`: 컬러 팔레트
- `typography.json`: 폰트, 크기, 두께
- `spacing.json`: 마진, 패딩
- `border.json`: 반경, 너비

### 2. Semantic Layer (의미 기반 토큰)

**위치**: `src/tokens/semantic/colors.json`

Base 토큰을 참조하여 의미를 부여합니다.

```json
{
  "color": {
    "brand": {
      "primary": "{color.brand.primary}",      // Base 참조
      "primaryHover": "{color.brand.primaryHover}"
    },
    "text": {
      "heading": "{color.gray.900}",          // 제목용 색상
      "body": "{color.gray.900}",             // 본문 텍스트
      "meta": "{color.gray.600}",             // 보조 텍스트
      "muted": "{color.gray.500}",            // 비활성 텍스트
      "link": "{color.blue.600}",             // 링크
      "inverse": "{color.neutral.white}"      // 다크 배경
    },
    "bg": {
      "page": "{color.gray.50}",              // 페이지 배경
      "card": "{color.neutral.white}",        // 카드 배경
      "elevated": "{color.neutral.white}",    // 모달, 드롭다운
      "muted": "{color.gray.100}",            // 약한 배경
      "hover": "{color.gray.50}"              // 호버 상태
    },
    "border": {
      "default": "{color.gray.200}",
      "hover": "{color.gray.300}",
      "focus": "{color.blue.600}"
    },
    "status": {
      "success": "{color.green.600}",         // 성공
      "error": "{color.red.600}",             // 오류
      "warning": "{color.yellow.600}",        // 경고
      "info": "{color.blue.600}"              // 정보
    }
  }
}
```

**역할**:
- UI 역할 기반 색상 정의 (텍스트, 배경, 테두리, 상태)
- Base 토큰과 Component 사이의 추상화 계층
- Light/Dark 모드 전환 시 일괄 변경 가능

### 3. Component Layer (컴포넌트 토큰)

**위치**: `src/components/Button/Button.vue` 등

Tailwind CSS 유틸리티 클래스와 CSS 변수를 조합합니다.

```vue
<!-- Button.vue -->
<script setup>
const variantClasses = {
  primary: 'bg-brand-primary text-white hover:bg-brand-primaryHover',
  secondary: 'bg-gray-100 text-gray-900 hover:bg-gray-200'
}
const sizeClasses = {
  sm: 'px-sm py-2 text-sm',
  md: 'px-md py-3 text-base'
}
</script>

<template>
  <button :class="[variantClasses[variant], sizeClasses[size]]"
    class="rounded-md transition-colors focus:ring-2">
    <slot />
  </button>
</template>
```

**Tailwind Preset 매핑** (`tailwind.preset.js`):

```javascript
colors: {
  'brand': {
    'primary': 'var(--semantic-brand-primary)',
    'primaryHover': 'var(--semantic-brand-primaryHover)'
  },
  'text': {
    'heading': 'var(--semantic-text-heading)',
    'body': 'var(--semantic-text-body)'
  },
  'bg': {
    'page': 'var(--semantic-bg-page)',
    'card': 'var(--semantic-bg-card)'
  }
}
```

## 토큰 흐름

### 런타임 토큰 생성

1. **빌드 시점** (`npm run build`):
   ```bash
   $ npm run build:tokens
   # scripts/build-tokens.js 실행
   # src/tokens/base/*.json + semantic/*.json → CSS 변수 생성
   # dist/design-system.css에 포함
   ```

2. **CSS 변수 생성** (`dist/design-system.css`):
   ```css
   :root {
     --base-color-brand-primary: #20C997;
     --semantic-brand-primary: var(--base-color-brand-primary);
     --semantic-text-heading: #212529;
     --semantic-bg-page: #f8f9fa;
   }
   ```

3. **서비스별 테마 오버라이드**:
   ```css
   /* src/styles/themes/blog.css */
   [data-service="blog"] {
     --semantic-brand-primary: #20C997;  /* Green */
   }

   /* src/styles/themes/shopping.css */
   [data-service="shopping"] {
     --semantic-brand-primary: #FF922B;  /* Orange */
   }
   ```

## 테마 시스템

### 서비스 컨텍스트 (data-service)

HTML 요소의 `data-service` 속성으로 테마를 전환합니다.

```html
<!-- Blog 서비스 -->
<html data-service="blog">
  <!-- 모든 자식 요소가 Blog 테마 적용 -->
</html>

<!-- Shopping 서비스 -->
<html data-service="shopping">
  <!-- 모든 자식 요소가 Shopping 테마 적용 -->
</html>
```

### 명암 모드 (Light/Dark)

HTML 요소의 `data-theme` 속성 또는 `class="dark"`로 명암 모드 전환합니다.

```html
<!-- Light 모드 (기본) -->
<html data-theme="light">

<!-- Dark 모드 -->
<html data-theme="dark" class="dark">
```

### useTheme 컴포저블

```typescript
// src/composables/useTheme.ts
const { setService, setTheme, toggleTheme, currentService, currentTheme } = useTheme()

// 서비스 전환
setService('blog')        // Blog 테마 적용
setService('shopping')    // Shopping 테마 적용

// 명암 모드 전환
setTheme('light')         // Light 모드
setTheme('dark')          // Dark 모드
toggleTheme()             // 토글

// 초기화 (localStorage, 시스템 설정 반영)
initTheme()
```

## 빌드 프로세스

### 1. 토큰 빌드

```bash
# scripts/build-tokens.js
# JSON 토큰 → CSS 변수로 변환
Base tokens (base/*.json)
        ↓
Semantic tokens (semantic/*.json)
        ↓
Theme tokens (themes/*.json)
        ↓
CSS Variables (--base-*, --semantic-*)
        ↓
dist/design-system.css
```

### 2. Vite 빌드

```bash
npm run build

# 출력물:
# dist/index.js          - ES 모듈
# dist/index.cjs         - CommonJS
# dist/index.d.ts        - TypeScript 선언
# dist/design-system.css - 컴파일된 스타일
```

### 3. 패키지 배포

```json
{
  "exports": {
    ".": {
      "types": "./dist/index.d.ts",
      "import": "./dist/index.js",
      "require": "./dist/index.cjs"
    },
    "./style.css": "./dist/design-system.css",
    "./tailwind.preset.js": "./tailwind.preset.js"
  }
}
```

## 파일 구조 상세

```
src/
├── components/              # 21개 Vue 3 컴포넌트
│   ├── Button/
│   │   ├── Button.vue       # 컴포넌트 구현
│   │   ├── Button.types.ts  # Props 인터페이스
│   │   ├── Button.stories.ts# Storybook 스토리
│   │   ├── __tests__/       # 단위 테스트
│   │   └── index.ts         # 익스포트
│   ├── Input/
│   ├── Modal/
│   └── ...
│
├── composables/             # Vue 3 Composables
│   ├── useTheme.ts          # 테마 관리
│   ├── useToast.ts          # 토스트 알림
│   └── __tests__/
│
├── styles/                  # 글로벌 스타일
│   ├── index.css            # Base 스타일
│   └── themes/
│       ├── blog.css         # Blog 테마 오버라이드
│       └── shopping.css     # Shopping 테마 오버라이드
│
├── tokens/                  # Design Tokens (JSON)
│   ├── base/
│   │   ├── colors.json      # 컬러 팔레트
│   │   ├── typography.json  # 폰트, 사이즈
│   │   ├── spacing.json     # 마진, 패딩
│   │   └── border.json      # 반경, 너비
│   ├── semantic/
│   │   └── colors.json      # 의미 기반 색상
│   └── themes/
│       ├── blog.json        # Blog 서비스 토큰
│       └── shopping.json    # Shopping 서비스 토큰
│
├── types/
│   └── theme.ts             # 타입 정의
│
└── index.ts                 # 라이브러리 진입점
```

## 확장성 고려사항

### 새로운 컴포넌트 추가

1. `src/components/NewComponent/` 디렉토리 생성
2. `NewComponent.vue`, `NewComponent.types.ts` 작성
3. `NewComponent.stories.ts` 작성 (Storybook)
4. `src/components/index.ts`에 export 추가

### 새로운 서비스 테마 추가

1. `src/tokens/themes/newservice.json` 생성
2. `src/styles/themes/newservice.css` 생성
3. `src/index.ts`에 import 추가
4. `useTheme()` 타입 수정

### Base 토큰 업데이트

1. `src/tokens/base/` 파일 수정
2. `npm run build:tokens` 실행
3. 모든 semantic 및 component 토큰이 자동 반영됨

---

**다음**: [COMPONENTS.md](./COMPONENTS.md)에서 컴포넌트 목록을 확인하세요.
