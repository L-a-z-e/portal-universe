# Design Tokens 상세 가이드

## 개요

Design Tokens는 디자인 결정을 변수화한 값입니다. Portal Universe Design System은 **3-계층 토큰 구조**를 사용하여 확장 가능하고 유지보수하기 쉬운 디자인 시스템을 구축합니다.

## Base 토큰 (원시 값)

### 컬러 토큰

**위치**: `src/tokens/base/colors.json`

#### Brand Colors

기본 브랜드 색상 (서비스별 테마에서 오버라이드됨)

```json
"color": {
  "brand": {
    "primary": "#20C997",        // 기본: Mantine 그린
    "primaryHover": "#12B886",   // 호버 상태
    "secondary": "#38D9A9"       // 보조 색상
  }
}
```

#### Neutral Colors

회색 톤의 중립적 색상들 (모든 서비스에서 공통 사용)

```json
"neutral": {
  "white": "#ffffff",    // 순백
  "50": "#f9fafb",
  "100": "#f3f4f6",
  "200": "#e5e7eb",
  "300": "#d1d5db",
  "400": "#9ca3af",
  "500": "#6b7280",
  "600": "#4b5563",
  "700": "#374151",
  "800": "#1f2937",
  "900": "#111827",
  "black": "#000000"     // 순검
}
```

#### Service-Specific Colors

각 서비스의 특정 색상 팔레트

```json
"green": {
  "$description": "Blog 서비스 시그니처 컬러",
  "50": "#E6FCF5",
  "100": "#C3FAE8",
  "200": "#96F2D7",
  "300": "#63E6BE",
  "400": "#38D9A9",
  "500": "#20C997",     // Blog primary
  "600": "#12B886",
  "700": "#0CA678",
  "800": "#099268",
  "900": "#087F5B"
},
"orange": {
  "$description": "Shopping 서비스용 따뜻한 색상",
  "50": "#FFF4E6",
  "100": "#FFE8CC",
  "200": "#FFD8A8",
  "300": "#FFC078",
  "400": "#FFA94D",
  "500": "#FF922B",     // Shopping primary
  "600": "#FD7E14",
  "700": "#F76707",
  "800": "#E8590C",
  "900": "#D9480F"
}
```

#### Status Colors

상태를 나타내는 색상들

```json
"red": {
  "$description": "오류 및 파괴적 액션",
  "50": "#FFE3E3",
  "500": "#FA5252",
  "600": "#F03E3E",
  "900": "#C92A2A"
},
"blue": {
  "$description": "정보 및 링크",
  "50": "#D0EBFF",
  "500": "#339AF0",
  "600": "#228BE6",
  "700": "#1976D2",
  "900": "#1864AB"
},
"yellow": {
  "$description": "경고 상태",
  "50": "#FFF9DB",
  "500": "#FCC419",
  "600": "#FAB005"
}
```

### Typography 토큰

**위치**: `src/tokens/base/typography.json`

```json
"font": {
  "family": {
    "sans": "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
    "mono": "'Fira Code', 'Courier New', monospace"
  },
  "size": {
    "xs": "0.75rem",   // 12px
    "sm": "0.875rem",  // 14px
    "base": "1rem",    // 16px
    "lg": "1.125rem",  // 18px
    "xl": "1.25rem",   // 20px
    "2xl": "1.5rem",   // 24px
    "3xl": "1.875rem", // 30px
    "4xl": "2.25rem",  // 36px
    "5xl": "3rem"      // 48px
  },
  "weight": {
    "light": "300",
    "normal": "400",
    "medium": "500",
    "semibold": "600",
    "bold": "700"
  },
  "lineHeight": {
    "tight": "1.25",      // 제목용
    "snug": "1.375",
    "normal": "1.5",      // 기본
    "relaxed": "1.625",
    "loose": "2"          // 긴 텍스트
  }
}
```

### Spacing 토큰

**위치**: `src/tokens/base/spacing.json`

```json
"spacing": {
  "xs": "0.5rem",   // 8px
  "sm": "1rem",     // 16px
  "md": "1.5rem",   // 24px
  "lg": "2rem",     // 32px
  "xl": "3rem",     // 48px
  "2xl": "4rem"     // 64px
}
```

**사용 사례**:
- 마진, 패딩
- 간격
- 너비, 높이

### Border 토큰

**위치**: `src/tokens/base/border.json`

```json
"border": {
  "radius": {
    "none": "0",
    "sm": "0.25rem",    // 4px
    "default": "0.375rem", // 6px
    "md": "0.5rem",     // 8px
    "lg": "0.75rem",    // 12px
    "xl": "1rem",       // 16px
    "2xl": "1.5rem",    // 24px
    "full": "9999px"    // 원형
  },
  "width": {
    "hairline": "0.5px",
    "thin": "1px",
    "medium": "2px",
    "thick": "4px"
  },
  "shadow": {
    "sm": "0 1px 2px rgba(0, 0, 0, 0.05)",
    "md": "0 4px 6px rgba(0, 0, 0, 0.1)",
    "lg": "0 10px 15px rgba(0, 0, 0, 0.1)",
    "xl": "0 20px 25px rgba(0, 0, 0, 0.1)"
  }
}
```

## Semantic 토큰 (의미 기반)

**위치**: `src/tokens/semantic/colors.json`

Base 토큰을 참조하여 UI 역할 기반 색상을 정의합니다.

### Brand

```json
"brand": {
  "primary": "{color.brand.primary}",
  "primaryHover": "{color.brand.primaryHover}",
  "secondary": "{color.brand.secondary}"
}
```

**용도**: 브랜드 강조, CTA 버튼, 주요 액센트

### Text Colors

```json
"text": {
  "heading": "{color.gray.900}",        // h1-h6
  "body": "{color.gray.900}",          // p, 본문
  "meta": "{color.gray.600}",          // 날짜, 태그, 카테고리
  "muted": "{color.gray.500}",         // 비활성, 비고
  "inverse": "{color.neutral.white}",  // 다크 배경 위
  "link": "{color.blue.600}",          // a 요소
  "linkHover": "{color.blue.700}"      // a:hover
}
```

**용도**: 텍스트 색상 일관성

### Background Colors

```json
"bg": {
  "page": "{color.gray.50}",       // 전체 페이지 배경
  "card": "{color.neutral.white}", // 카드, 섹션
  "elevated": "{color.neutral.white}", // 모달, 드롭다운
  "muted": "{color.gray.100}",     // 테이블 행, 약한 배경
  "hover": "{color.gray.50}"       // 인터랙티브 요소 호버
}
```

**용도**: 배경색 일관성, 레이어 구분

### Border Colors

```json
"border": {
  "default": "{color.gray.200}",
  "hover": "{color.gray.300}",
  "focus": "{color.blue.600}",     // 포커스 상태 (brand 대신 blue)
  "muted": "{color.gray.100}"      // 약한 테두리
}
```

**용도**: 경계선, 분할선, 포커스 표시

### Status Colors

```json
"status": {
  "success": "{color.green.600}",   // ✓ 성공
  "successBg": "{color.green.50}",
  "error": "{color.red.600}",       // ✕ 오류
  "errorBg": "{color.red.50}",
  "warning": "{color.yellow.600}",  // ⚠ 경고
  "warningBg": "{color.yellow.50}",
  "info": "{color.blue.600}",       // ℹ 정보
  "infoBg": "{color.blue.50}"
}
```

**용도**: 상태 표시, 알림 메시지

## Theme 토큰 (서비스 특정)

**위치**: `src/tokens/themes/`

서비스별 브랜드 컬러를 오버라이드합니다.

### Blog Theme

```json
// src/tokens/themes/blog.json
{
  "color": {
    "brand": {
      "primary": "#20C997",        // Mantine Green
      "primaryHover": "#12B886"
    }
  }
}
```

### Shopping Theme

```json
// src/tokens/themes/shopping.json
{
  "color": {
    "brand": {
      "primary": "#FF922B",        // Mantine Orange
      "primaryHover": "#FD7E14"
    }
  }
}
```

## CSS 변수 생성

빌드 시 `scripts/build-tokens.js`가 JSON 토큰을 CSS 변수로 변환합니다.

### 생성된 CSS 변수

```css
/* Base 토큰 (--base- 접두사) */
:root {
  --base-color-brand-primary: #20C997;
  --base-color-brand-primaryHover: #12B886;
  --base-color-gray-900: #212529;
  --base-font-family-sans: -apple-system, BlinkMacSystemFont, ...
  --base-spacing-xs: 0.5rem;
  --base-spacing-sm: 1rem;
  --base-border-radius-md: 0.5rem;
}

/* Semantic 토큰 (--semantic- 접두사) */
:root {
  --semantic-brand-primary: var(--base-color-brand-primary);
  --semantic-text-heading: var(--base-color-gray-900);
  --semantic-bg-page: var(--base-color-gray-50);
  --semantic-border-default: var(--base-color-gray-200);
  --semantic-status-success: var(--base-color-green-600);
}
```

### 테마 오버라이드

```css
/* Blog 테마 */
[data-service="blog"] {
  --semantic-brand-primary: #20C997;
}

/* Shopping 테마 */
[data-service="shopping"] {
  --semantic-brand-primary: #FF922B;
}

/* Dark 모드 */
[data-theme="dark"],
.dark {
  --semantic-text-heading: var(--base-color-gray-100);
  --semantic-bg-page: var(--base-color-gray-900);
  --semantic-bg-card: var(--base-color-gray-800);
}
```

## Tailwind CSS 통합

### tailwind.preset.js

Tailwind가 CSS 변수를 인식하도록 매핑합니다.

```javascript
colors: {
  'brand': {
    'primary': 'var(--semantic-brand-primary)',
    'primaryHover': 'var(--semantic-brand-primaryHover)',
  },
  'text': {
    'heading': 'var(--semantic-text-heading)',
    'body': 'var(--semantic-text-body)',
  },
  'bg': {
    'page': 'var(--semantic-bg-page)',
    'card': 'var(--semantic-bg-card)',
  }
}
```

### 컴포넌트에서 사용

```vue
<!-- Button.vue -->
<button class="bg-brand-primary text-white hover:bg-brand-primaryHover">
  Click me
</button>

<!-- 생성된 CSS -->
<button style="background-color: var(--semantic-brand-primary);">
```

## 토큰 참조 형식

### JSON 참조

```json
{
  "color": {
    "text": {
      "heading": "{color.gray.900}"    // color 그룹의 gray.900 참조
    }
  }
}
```

### CSS 변수

```css
color: var(--semantic-text-heading);  /* Semantic 변수 사용 */
color: var(--base-color-gray-900);    /* Base 변수 직접 사용 */
```

### Tailwind

```vue
<div class="text-heading bg-brand-primary">
  <!-- Tailwind 유틸리티 클래스 -->
</div>
```

## 토큰 업데이트 워크플로우

### 시나리오 1: Base 토큰 변경

```bash
# 1. src/tokens/base/colors.json 수정
"green.600": "#10b981"  # 기존: #12B886

# 2. 빌드
npm run build

# 3. 자동으로 반영
# Semantic 토큰 → {color.green.600} 참조
# CSS 변수 → --semantic-* 변수
# 컴포넌트 → Tailwind 클래스
```

### 시나리오 2: 서비스 테마 추가

```bash
# 1. src/tokens/themes/newservice.json 생성
{
  "color": {
    "brand": {
      "primary": "#NEW_COLOR"
    }
  }
}

# 2. src/styles/themes/newservice.css 생성
[data-service="newservice"] {
  --semantic-brand-primary: #NEW_COLOR;
}

# 3. src/index.ts에 import 추가
import './styles/themes/newservice.css'

# 4. 타입 수정
export type ServiceType = 'portal' | 'blog' | 'shopping' | 'newservice'
```

## 모범 사례

### ✓ 해야 할 것

- Semantic 토큰 참조: `--semantic-text-heading`, `text-heading` 클래스
- 의미 기반 토큰 이름: `text-muted`, `bg-elevated`
- 서비스별 오버라이드: `data-service` 속성

### ✗ 하지 말아야 할 것

- Base 색상 직접 사용: `--base-color-gray-900` (대신 Semantic 사용)
- 하드코딩 색상값: `#212529` (대신 토큰 변수 사용)
- 서비스별 조건부 스타일: CSS 선택자로 처리

---

**다음**: [THEMING.md](./THEMING.md)에서 테마 시스템 구현을 확인하세요.
