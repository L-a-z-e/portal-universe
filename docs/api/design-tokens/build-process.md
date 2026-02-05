---
id: build-process
title: Build Process
type: api
status: current
created: 2026-02-06
updated: 2026-02-06
author: documenter
tags: [api, build, design-tokens, process]
related:
  - css-variables
  - themes
---

# Build Process

Design Tokens의 빌드 프로세스 및 JSON → CSS 변환 과정입니다.

## 개요

| 항목 | 값 |
|------|-----|
| **빌드 스크립트** | `scripts/build-tokens.js` |
| **입력** | `src/tokens/**/*.json` |
| **출력** | `dist/tokens.css`, `dist/tokens.js`, `dist/tokens.json` |
| **빌드 명령** | `npm run build:tokens` |

## 디렉토리 구조

```
frontend/design-tokens/
├── src/
│   └── tokens/
│       ├── base/
│       │   ├── colors.json          # Base 색상 (12개 팔레트)
│       │   ├── typography.json      # 타이포그래피
│       │   ├── spacing.json         # 간격
│       │   ├── border.json          # 테두리
│       │   └── effects.json         # 효과 (shadow, animation, glass, opacity)
│       ├── semantic/
│       │   └── colors.json          # Semantic 색상 (역할 기반)
│       └── themes/
│           ├── portal.json          # Portal 테마 (dark-first)
│           ├── blog.json            # Blog 테마 (light-first)
│           ├── shopping.json        # Shopping 테마 (light-first)
│           └── prism.json           # Prism 테마 (light-first)
├── scripts/
│   └── build-tokens.js              # 빌드 스크립트
├── dist/
│   ├── tokens.css                   # 생성된 CSS
│   ├── tokens.js                    # ESM export
│   ├── tokens.cjs                   # CommonJS export
│   ├── tokens.d.ts                  # TypeScript 선언
│   └── tokens.json                  # JSON export
├── tailwind.preset.js               # Tailwind Preset
└── package.json
```

## 빌드 프로세스

### 1단계: Color Reference Map 구축

**목적**: `{color.indigo.400}` 형태의 토큰 참조 해석

```javascript
// build-tokens.js
const colorReferences = {};

function buildColorMap(obj, prefix = '') {
  for (const [key, value] of Object.entries(obj)) {
    if (key.startsWith('$')) continue;  // 메타데이터 스킵

    const fullKey = prefix ? `${prefix}.${key}` : key;

    if (typeof value === 'object' && value !== null && !('$value' in value)) {
      buildColorMap(value, fullKey);  // 재귀 탐색
    } else if (value && typeof value === 'object' && '$value' in value) {
      colorReferences[fullKey] = value.$value;  // 저장
    } else if (typeof value === 'string') {
      colorReferences[fullKey] = value;
    }
  }
}
```

**결과**:
```javascript
{
  'color.indigo.400': '#5e6ad2',
  'color.linear.950': '#08090a',
  'color.green.600': '#12B886',
  // ...
}
```

### 2단계: Base Tokens 처리

**입력 파일**: `src/tokens/base/*.json`

```json
// base/colors.json
{
  "color": {
    "indigo": {
      "400": {
        "$value": "#5e6ad2",
        "$description": "Linear primary accent"
      }
    }
  }
}
```

**변환 로직**:
```javascript
function flattenTokens(tokens, prefix, cssVars, parentKey = '') {
  for (const [key, value] of Object.entries(tokens)) {
    if (key.startsWith('$')) continue;  // 메타데이터 스킵

    const fullKey = parentKey ? `${parentKey}-${key}` : key;
    const cssVarName = prefix ? `${prefix}-${fullKey}` : `--${fullKey}`;

    if (typeof value === 'object' && value !== null && !('$value' in value)) {
      flattenTokens(value, prefix, cssVars, fullKey);  // 재귀
    } else if (value && typeof value === 'object' && '$value' in value) {
      cssVars.set(cssVarName, value.$value);  // CSS 변수 저장
    }
  }
}
```

**출력**:
```css
:root {
  --color-indigo-400: #5e6ad2;
  --typography-fontSize-base: 0.875rem;
  --spacing-md: 1rem;
}
```

### 3단계: Semantic Tokens 처리

**입력 파일**: `src/tokens/semantic/colors.json`

```json
{
  "color": {
    "brand": {
      "primary": {
        "$value": "{color.indigo.400}",
        "$type": "color",
        "$description": "Primary brand color - Linear indigo"
      }
    }
  }
}
```

**참조 해석 로직**:
```javascript
function resolveColorReference(value, colorReferences) {
  if (typeof value !== 'string') return value;

  const refMatch = value.match(/^\{([^}]+)\}$/);  // {color.xxx} 패턴 검사
  if (!refMatch) return value;

  const refPath = refMatch[1];  // 'color.indigo.400'
  const resolved = colorReferences[refPath];  // '#5e6ad2'

  return resolved || value;  // 참조 실패 시 원본 반환
}
```

**출력**:
```css
:root {
  --semantic-brand-primary: #5e6ad2;  /* {color.indigo.400} 해석됨 */
}
```

### 4단계: Theme Tokens 처리

**입력 파일**: `src/tokens/themes/portal.json`

```json
{
  "$description": "Portal service theme - Linear-inspired dark-first design",
  "color": {
    "brand": {
      "primary": {
        "$value": "{color.indigo.400}",
        "$description": "Portal brand primary - Linear indigo",
        "$type": "color"
      }
    }
  },
  "lightMode": {
    "$description": "Portal light mode - inverted Linear palette",
    "color": {
      "brand": {
        "primary": {
          "$value": "{color.indigo.500}",
          "$type": "color"
        }
      }
    }
  }
}
```

**Dark-first vs Light-first 처리**:
```javascript
themeFiles.forEach(themeName => {
  const themeLightVars = new Map();
  const themeDarkVars = new Map();

  if (themeName === 'portal') {
    // Dark-first: 기본값은 dark, lightMode 섹션이 light override
    processThemeColors(themeTokens.color, themeDarkVars, colorReferences);
    if (themeTokens.lightMode) {
      processThemeColors(themeTokens.lightMode.color, themeLightVars, colorReferences);
    }
    themes.set(themeName, themeDarkVars);
    themeLightModes.set(themeName, themeLightVars);
  } else {
    // Light-first: 기본값은 light, darkMode 섹션이 dark override
    processThemeColors(themeTokens.color, themeLightVars, colorReferences);
    if (themeTokens.darkMode) {
      processThemeColors(themeTokens.darkMode.color, themeDarkVars, colorReferences);
    }
    themes.set(themeName, themeLightVars);
    themeDarkModes.set(themeName, themeDarkVars);
  }
});
```

**출력**:
```css
/* Portal 기본 (Dark Mode) */
[data-service="portal"] {
  --semantic-brand-primary: #5e6ad2;  /* indigo-400 */
}

/* Portal Light Mode */
[data-service="portal"][data-theme="light"] {
  --semantic-brand-primary: #4754c9;  /* indigo-500 */
}
```

### 5단계: 출력 파일 생성

#### `dist/tokens.css`

```css
/* ============================================
   @portal/design-tokens - Auto-generated CSS Variables
   Linear-inspired theme - DO NOT EDIT MANUALLY
   ============================================ */

:root {
  /* Base 변수들 (정렬됨) */
  --border-radius-default: 0.25rem;
  --color-indigo-400: #5e6ad2;
  /* ... */
}

[data-theme="dark"] {
  /* 전역 다크 모드 오버라이드 */
}

[data-service="portal"] {
  /* Portal 기본값 */
}

[data-service="portal"][data-theme="light"] {
  /* Portal Light 오버라이드 */
}

/* ... Blog, Shopping, Prism ... */
```

#### `dist/tokens.js` (ESM)

```javascript
export const tokens = {
  base: { /* ... */ },
  semantic: { /* ... */ },
  themes: { /* ... */ }
};

export const cssVariables = {
  '--color-indigo-400': '#5e6ad2',
  // ...
};

export default tokens;
```

#### `dist/tokens.cjs` (CommonJS)

```javascript
const tokens = { /* ... */ };
const cssVariables = { /* ... */ };

module.exports = { tokens, cssVariables, default: tokens };
```

#### `dist/tokens.d.ts` (TypeScript)

```typescript
export interface Tokens {
  base: {
    colors?: Record<string, unknown>;
    typography?: Record<string, unknown>;
    spacing?: Record<string, unknown>;
    border?: Record<string, unknown>;
    effects?: Record<string, unknown>;
  };
  semantic: Record<string, unknown>;
  themes: {
    portal?: Record<string, unknown>;
    blog?: Record<string, unknown>;
    shopping?: Record<string, unknown>;
  };
}

export declare const tokens: Tokens;
export declare const cssVariables: Record<string, string>;
export default tokens;
```

#### `dist/tokens.json`

```json
{
  "base": { /* ... */ },
  "semantic": { /* ... */ },
  "themes": { /* ... */ }
}
```

## 빌드 명령어

```bash
# 디자인 토큰 빌드
cd frontend/design-tokens
npm run build:tokens

# 또는 루트에서
cd frontend
npm run build:design
```

**package.json**:
```json
{
  "scripts": {
    "build:tokens": "node scripts/build-tokens.js"
  }
}
```

## 빌드 로그 예시

```
📖 Step 1: Building color reference map...
  ✅ Color reference map built (108 colors)

📖 Step 2: Reading base tokens...
  ✅ colors.json loaded
  ✅ typography.json loaded
  ✅ spacing.json loaded
  ✅ border.json loaded
  ✅ effects.json loaded

📖 Step 3: Reading semantic tokens...
  ✅ semantic/colors.json loaded

📖 Step 4: Reading theme tokens (with darkMode/lightMode support)...
  ✅ themes/portal.json loaded (dark-first)
  ✅ themes/blog.json loaded (light-first)
  ✅ themes/shopping.json loaded (light-first)
  ✅ themes/prism.json loaded (light-first)

🎨 Step 5: Generating output files...
  ✅ tokens.css generated
  ✅ tokens.json generated
  ✅ tokens.js generated
  ✅ tokens.cjs generated
  ✅ tokens.d.ts generated

📊 Summary:
   Total base variables: 242
   Service themes generated: 4
   Dark mode overrides: 3
   Light mode overrides: 1

✨ Design tokens built successfully!
```

## Token Reference 패턴

### 지원되는 형식

| 패턴 | 예시 | 설명 |
|------|------|------|
| `{color.xxx.yyy}` | `{color.indigo.400}` | 색상 참조 |
| 직접 값 | `#5e6ad2` | 하드코딩된 값 |
| 중첩 참조 | `{color.neutral.white}` | 중첩된 객체 참조 |

### 참조 해석 규칙

1. `{` 로 시작하고 `}` 로 끝나는 문자열 검사
2. 중괄호 제거 후 경로 추출 (`color.indigo.400`)
3. `colorReferences` 맵에서 조회
4. 값이 있으면 → 치환, 없으면 → 원본 유지

```javascript
// 성공 예시
"{color.indigo.400}" → "#5e6ad2"

// 실패 예시 (맵에 없음)
"{color.unknown.999}" → "{color.unknown.999}" (원본 유지)
```

## JSON 토큰 형식

### DTCG (Design Tokens Community Group) 형식 부분 준수

```json
{
  "tokenName": {
    "$value": "actualValue",
    "$type": "color | dimension | number | ...",
    "$description": "설명"
  }
}
```

**사용 중인 `$type`**:
- `color`: 색상
- `dimension`: 크기 (rem, px 등)
- `number`: 숫자
- `duration`: 시간 (ms)
- `cubicBezier`: easing function
- `boxShadow`: shadow
- `fontFamily`: 폰트 패밀리

## 변경 사항 반영

### 토큰 수정 시

1. `src/tokens/**/*.json` 파일 수정
2. `npm run build:tokens` 실행
3. `dist/` 파일들이 재생성됨
4. Git commit 및 push

### 주의사항

- `dist/` 파일은 자동 생성되므로 **직접 수정 금지**
- 토큰 참조 오류는 빌드 시 콘솔 경고 출력
- 순환 참조는 지원하지 않음

## 관련 문서

- [CSS Variables Reference](./css-variables.md) - 생성된 CSS 변수 목록
- [Themes API](./themes.md) - 테마 시스템
- [Tailwind Preset API](./tailwind-preset.md) - Tailwind 프리셋
