# Design Tokens

Portal Universe 디자인 시스템의 토큰 체계를 상세히 설명합니다.

## 개요

Design Tokens는 **3-Layer Architecture**로 구성되어 있습니다:

1. **Base Tokens**: 절대값 (색상, 간격, 타이포그래피 등)
2. **Semantic Tokens**: 역할 기반 의미 (브랜드 색상, 텍스트 역할 등)
3. **Component Tokens**: 컴포넌트별 세부 스타일

이 계층 구조는 **유지보수성**과 **확장성**을 극대화합니다.

## Layer 1: Base Tokens

절대값 정의로 모든 디자인의 기초를 이룹니다. 불변입니다.

### 색상 (Colors)

```json
{
  "colors": {
    "neutral": {
      "50": "#FAFAFA",
      "100": "#F5F5F5",
      "200": "#EEEEEE",
      "300": "#E0E0E0",
      "400": "#BDBDBD",
      "500": "#9E9E9E",
      "600": "#757575",
      "700": "#616161",
      "800": "#424242",
      "900": "#212121"
    },
    "green": {
      "400": "#66BB6A",
      "500": "#4CAF50",
      "600": "#12B886",
      "700": "#039D6E"
    },
    "orange": {
      "400": "#FFA726",
      "500": "#FF9800",
      "600": "#FD7E14",
      "700": "#F76707"
    },
    "red": {
      "400": "#EF5350",
      "500": "#F44336",
      "600": "#E53935",
      "700": "#C62828"
    },
    "blue": {
      "400": "#42A5F5",
      "500": "#2196F3",
      "600": "#1976D2",
      "700": "#1565C0"
    }
  }
}
```

### 간격 (Spacing)

```json
{
  "spacing": {
    "0": "0",
    "1": "0.25rem",
    "2": "0.5rem",
    "3": "0.75rem",
    "4": "1rem",
    "6": "1.5rem",
    "8": "2rem",
    "12": "3rem",
    "16": "4rem",
    "20": "5rem",
    "24": "6rem",
    "32": "8rem"
  }
}
```

### 타이포그래피 (Typography)

#### 폰트 크기

```json
{
  "fontSize": {
    "xs": "0.75rem",
    "sm": "0.875rem",
    "base": "1rem",
    "lg": "1.125rem",
    "xl": "1.25rem",
    "2xl": "1.5rem",
    "3xl": "1.875rem",
    "4xl": "2.25rem"
  }
}
```

#### 라인 높이

```json
{
  "lineHeight": {
    "tight": "1.25",
    "normal": "1.5",
    "relaxed": "1.625",
    "loose": "2"
  }
}
```

#### 폰트 가중치

```json
{
  "fontWeight": {
    "light": 300,
    "normal": 400,
    "medium": 500,
    "semibold": 600,
    "bold": 700
  }
}
```

### 그림자 (Shadows)

```json
{
  "shadow": {
    "sm": "0 1px 2px 0 rgba(0, 0, 0, 0.05)",
    "base": "0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06)",
    "md": "0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)",
    "lg": "0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)",
    "xl": "0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)"
  }
}
```

### 반지름 (Border Radius)

```json
{
  "borderRadius": {
    "sm": "0.25rem",
    "base": "0.375rem",
    "md": "0.5rem",
    "lg": "0.75rem",
    "xl": "1rem",
    "full": "9999px"
  }
}
```

## Layer 2: Semantic Tokens

역할 기반 의미를 가진 토큰으로, Base Tokens를 참조합니다.

### 브랜드 (Brand)

```json
{
  "brand": {
    "primary": "var(--color-green-600)",
    "primaryHover": "var(--color-green-700)",
    "primaryLight": "var(--color-green-400)",
    "secondary": "var(--color-orange-600)",
    "secondaryHover": "var(--color-orange-700)",
    "accent": "var(--color-blue-600)"
  }
}
```

### 텍스트 (Text)

```json
{
  "text": {
    "heading": "var(--color-neutral-900)",
    "body": "var(--color-neutral-800)",
    "meta": "var(--color-neutral-600)",
    "muted": "var(--color-neutral-500)",
    "inverse": "var(--color-neutral-50)",
    "link": "var(--color-blue-600)",
    "linkHover": "var(--color-blue-700)",
    "error": "var(--color-red-600)",
    "success": "var(--color-green-600)",
    "warning": "var(--color-orange-600)"
  }
}
```

### 배경 (Background)

```json
{
  "bg": {
    "page": "var(--color-neutral-50)",
    "card": "var(--color-neutral-0)",
    "elevated": "var(--color-neutral-0)",
    "muted": "var(--color-neutral-100)",
    "hover": "var(--color-neutral-200)",
    "overlay": "rgba(0, 0, 0, 0.5)"
  }
}
```

### 테두리 (Border)

```json
{
  "border": {
    "default": "var(--color-neutral-200)",
    "hover": "var(--color-neutral-300)",
    "focus": "var(--color-brand-primary)",
    "muted": "var(--color-neutral-100)",
    "error": "var(--color-red-600)"
  }
}
```

### 상태 (Status)

```json
{
  "status": {
    "success": "var(--color-green-600)",
    "successBg": "var(--color-green-50)",
    "error": "var(--color-red-600)",
    "errorBg": "var(--color-red-50)",
    "warning": "var(--color-orange-600)",
    "warningBg": "var(--color-orange-50)",
    "info": "var(--color-blue-600)",
    "infoBg": "var(--color-blue-50)"
  }
}
```

## Layer 3: Component Tokens

컴포넌트별 세부 스타일을 정의합니다.

### Button Component

```json
{
  "button": {
    "primary": {
      "bg": "var(--color-brand-primary)",
      "text": "white",
      "border": "transparent",
      "hover": {
        "bg": "var(--color-brand-primaryHover)"
      },
      "focus": {
        "shadow": "var(--shadow-md)"
      },
      "disabled": {
        "bg": "var(--color-neutral-300)",
        "text": "var(--color-neutral-600)"
      }
    },
    "secondary": {
      "bg": "var(--color-neutral-100)",
      "text": "var(--color-text-body)",
      "border": "var(--color-border-default)",
      "hover": {
        "bg": "var(--color-neutral-200)"
      }
    }
  }
}
```

### Card Component

```json
{
  "card": {
    "bg": "var(--color-bg-card)",
    "border": "var(--color-border-default)",
    "shadow": "var(--shadow-md)",
    "borderRadius": "var(--borderRadius-lg)",
    "padding": "var(--spacing-6)"
  }
}
```

### Input Component

```json
{
  "input": {
    "bg": "var(--color-neutral-0)",
    "text": "var(--color-text-body)",
    "border": "var(--color-border-default)",
    "borderRadius": "var(--borderRadius-md)",
    "padding": "var(--spacing-3) var(--spacing-4)",
    "focus": {
      "border": "var(--color-border-focus)",
      "shadow": "0 0 0 3px var(--color-brand-primary)"
    },
    "error": {
      "border": "var(--color-border-error)"
    }
  }
}
```

## CSS 변수 구현

토큰은 CSS 변수로 구현되어 런타임에 변경 가능합니다:

```css
:root {
  /* Base Colors */
  --color-neutral-50: #FAFAFA;
  --color-neutral-900: #212121;
  --color-green-600: #12B886;
  --color-orange-600: #FD7E14;
  
  /* Semantic */
  --color-brand-primary: var(--color-green-600);
  --color-text-body: var(--color-neutral-800);
  --color-bg-card: #FFFFFF;
  
  /* Spacing */
  --spacing-4: 1rem;
  --spacing-6: 1.5rem;
  
  /* Typography */
  --font-size-base: 1rem;
  --line-height-normal: 1.5;
  --font-weight-semibold: 600;
}

/* Dark Mode */
[data-theme="dark"] {
  --color-text-body: var(--color-neutral-100);
  --color-bg-card: #1A1A1A;
  --color-bg-page: #0F0F0F;
}

/* Service Override: Shopping (Orange) */
[data-service="shopping"] {
  --color-brand-primary: var(--color-orange-600);
  --color-brand-primaryHover: var(--color-orange-700);
}
```

## 토큰 명명 규칙

### 포맷

```
{category}-{property}-{variant?}-{state?}
```

### 예시

| 토큰 | 설명 |
|------|------|
| `color-neutral-50` | 중립색 Base Token |
| `color-brand-primary` | 브랜드 주요색 Semantic Token |
| `text-body` | 본문 텍스트 색상 Semantic Token |
| `button-primary-bg` | Primary 버튼 배경 Component Token |
| `input-focus-shadow` | Input Focus 상태 그림자 Component Token |

## 토큰 사용 방법

### CSS 변수 직접 사용

```css
.button-primary {
  background-color: var(--color-brand-primary);
  padding: var(--spacing-4);
  border-radius: var(--borderRadius-md);
  color: white;
}

.button-primary:hover {
  background-color: var(--color-brand-primaryHover);
}
```

### Tailwind CSS를 통한 사용

Design System은 Tailwind CSS를 지원하며, 토큰이 자동으로 Tailwind 클래스로 변환됩니다:

```vue
<template>
  <button class="bg-brand-primary hover:bg-brand-primary-hover text-white px-4 py-2 rounded-md">
    클릭하기
  </button>
</template>
```

### Vue 컴포넌트에서 동적 사용

```vue
<script setup lang="ts">
import { ref, computed } from 'vue'

const isDarkMode = ref(false)

const buttonStyle = computed(() => ({
  backgroundColor: 'var(--color-brand-primary)',
  padding: 'var(--spacing-4)',
  borderRadius: 'var(--borderRadius-md)'
}))
</script>

<template>
  <button :style="buttonStyle">
    클릭하기
  </button>
</template>
```

## 다크 모드 토큰

모든 Semantic 토큰은 Light/Dark 모드를 지원합니다:

```css
/* Light Mode (기본) */
:root {
  --color-text-body: #333333;
  --color-bg-page: #FFFFFF;
}

/* Dark Mode */
[data-theme="dark"] {
  --color-text-body: #ECECEC;
  --color-bg-page: #1A1A1A;
}
```

## 서비스별 토큰 오버라이드

Blog와 Shopping 서비스는 각자의 브랜드 토큰을 유지합니다:

```css
/* Blog Service (Green) */
[data-service="blog"] {
  --color-brand-primary: #12B886;
  --color-brand-secondary: #4CAF50;
}

/* Shopping Service (Orange) */
[data-service="shopping"] {
  --color-brand-primary: #FD7E14;
  --color-brand-secondary: #FF9800;
}
```

## 토큰 빌드 프로세스

토큰 JSON 파일은 빌드 시간에 CSS 변수로 변환됩니다:

```bash
npm run build:tokens
```

프로세스:

1. `tokens/base/*.json` 읽음
2. `tokens/semantic/*.json` 참조 병합
3. `tokens/themes/*.json` 오버라이드 적용
4. CSS 변수 생성
5. `dist/design-system.css` 출력

## 토큰 확장

새로운 토큰을 추가하려면:

1. 해당 Base 또는 Semantic 토큰 파일에 추가
2. 타입 정의 업데이트 (`src/types/theme.ts`)
3. Storybook 예제 추가
4. 빌드 실행: `npm run build`

예: 새로운 색상 추가

```json
// tokens/base/colors.json
{
  "purple": {
    "600": "#7C3AED"
  }
}

// tokens/semantic/brand.json
{
  "tertiary": "var(--color-purple-600)"
}
```

## 토큰 참고 자료

- **Figma Variables API**: 디자이너와 동기화
- **Token Studio**: Figma에서 토큰 관리
- **Design Tokens Format**: W3C 표준 포맷

## 다음 단계

- [THEMING.md](./THEMING.md) - 테마 커스터마이징
- [USAGE.md](./USAGE.md) - 토큰을 활용한 컴포넌트 개발