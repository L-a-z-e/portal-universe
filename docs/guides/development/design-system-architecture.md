---
id: design-system-architecture
title: Design System Architecture - End-to-End Flow
type: guide
status: current
created: 2026-01-28
updated: 2026-01-28
author: Laze
tags: [guide, architecture, design-system, tokens, theming, components]
related:
  - getting-started
  - customization
  - token-system
---

# Design System Architecture - End-to-End Flow

**난이도**: ⭐⭐⭐ | **예상 시간**: 30분 | **카테고리**: Development

`PostCard.vue` 하나를 기준으로 토큰 정의부터 컴포넌트 렌더링까지 전체 흐름을 추적합니다.

---

## Part 1: 전체 흐름 한눈에 보기

```
[1] JSON 토큰 정의
    | build-tokens.js
[2] CSS 변수 500개+ 생성 (tokens.css)
    | design-system-vue 빌드 시 styles/index.css에 포함
[3] design-system.css 번들 생성 (CSS변수 + Tailwind + 테마 전부 포함)
    | blog-frontend/src/style.css에서 @import
[4] 앱에서 CSS 변수 + Tailwind 클래스 + 컴포넌트 사용
    | PostCard.vue에서 <Card>, <Tag>, <Avatar> import
[5] 브라우저에서 data-service="blog" 속성에 따라 CSS 변수값 결정 -> 렌더링
```

### 패키지 의존성 그래프

```
@portal/design-tokens          <-- 최하단: 모든 것이 의존
(JSON 토큰 -> CSS 변수 + Tailwind preset)
        |
   +---------+
   |         |
   v         v
@portal/   @portal/
design-    design-
types      types
(TS 타입 계약서)
   |         |
   v         v
@portal/design-   @portal/design-
system-vue         system-react
(Vue 33개 컴포넌트) (React 31개 컴포넌트)
   ^                   ^
   |                   |
   +---+---+      +----+
   |   |   |      |
portal blog shop
-shell -fe  -fe
```

---

## Part 2: 단계별 상세 추적 (PostCard.vue 기준)

### STEP 1 -- 토큰 JSON 정의

**파일**: `design-tokens/src/tokens/semantic/colors.json`

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
      "heading": { "$value": "#ffffff" },
      "body": { "$value": "#b4b4b4" },
      "meta": { "$value": "{color.linear.400}" }
    },
    "bg": {
      "page": { "$value": "{color.linear.950}" },
      "card": { "$value": "{color.linear.850}" }
    }
  }
}
```

`{color.indigo.400}` 같은 참조 문법은 `base/colors.json`에 정의된 실제 hex 값으로 resolve됩니다.

**파일**: `design-tokens/src/tokens/themes/blog.json` (블로그 테마 오버라이드)

```json
{
  "color": {
    "brand": {
      "primary": { "$value": "#12B886" },
      "primaryHover": { "$value": "#0CA678" }
    },
    "text": {
      "heading": { "$value": "{color.gray.900}" },
      "body": { "$value": "{color.gray.900}" }
    },
    "bg": {
      "page": { "$value": "{color.gray.50}" },
      "card": { "$value": "{color.neutral.white}" }
    }
  },
  "darkMode": {
    "color": {
      "brand": { "primary": { "$value": "#20C997" } },
      "bg": { "page": { "$value": "{color.linear.950}" } }
    }
  }
}
```

- Blog/Shopping은 **light-first**: 기본=라이트, `darkMode` 섹션이 다크 오버라이드
- Portal은 **dark-first**: 기본=다크, `lightMode` 섹션이 라이트 오버라이드

---

### STEP 2 -- build-tokens.js가 JSON을 CSS로 변환

**파일**: `design-tokens/scripts/build-tokens.js`

**동작 순서**:

```
Step 1: base/colors.json 읽어서 참조 맵 구축
        "color.indigo.400" -> "#5e6ad2"
        "color.linear.950" -> "#08090a"
        "color.gray.900"   -> "#212529"
        ...총 100개+ 매핑

Step 2: base/*.json 5개 파일 읽어서 CSS 변수 생성
        --color-blue-500: #339AF0
        --spacing-md: 1rem
        --typography-fontSize-base: 0.875rem

Step 3: semantic/colors.json 읽고 참조 resolve
        "$value": "{color.indigo.400}"
          -> colorReferences["color.indigo.400"]
          -> "#5e6ad2"
        결과: --semantic-brand-primary: #5e6ad2

Step 4: themes/*.json 읽고 서비스별 오버라이드 생성
        Portal: dark-first (기본=다크, lightMode 섹션이 라이트 오버라이드)
        Blog:   light-first (기본=라이트, darkMode 섹션이 다크 오버라이드)
        Shopping: light-first

Step 5: 파일 출력
        dist/tokens.css  -- :root { 500개+ 변수 } + 테마 오버라이드 셀렉터들
        dist/tokens.json -- 원본 구조 그대로
        dist/tokens.js   -- ESM export
```

**변환 결과** (tokens.css의 일부):

```css
:root {
    --semantic-brand-primary: #5e6ad2;   /* semantic에서 온 기본값 */
    --semantic-bg-page: #08090a;
    --semantic-text-heading: #f7f8f8;
}

[data-service="blog"] {
    --semantic-brand-primary: #12B886;   /* blog.json에서 오버라이드 */
    --semantic-bg-page: #F8F9FA;
    --semantic-text-heading: #212529;
}

[data-service="blog"][data-theme="dark"] {
    --semantic-brand-primary: #20C997;   /* blog.json의 darkMode에서 */
    --semantic-bg-page: #08090a;
}
```

---

### STEP 3 -- design-system-vue가 CSS를 번들로 묶음

**파일**: `design-system-vue/src/styles/index.css`

이 파일이 **design-system.css 빌드의 원본**입니다:

```css
@import '@fontsource-variable/inter';        /* 1. Inter 폰트 */

@tailwind base;                               /* 2. Tailwind 기본 리셋 */
@tailwind components;
@tailwind utilities;

/* 3. 500개+ CSS 변수 (tokens.css 내용이 인라인됨) */
:root {
    --border-radius-default: 0.25rem;
    --color-blue-500: #339AF0;
    --semantic-brand-primary: #5e6ad2;
    /* ... */
}

[data-service="blog"] { /* ... */ }          /* 4. 서비스 테마 */
[data-service="shopping"] { /* ... */ }
[data-theme="dark"] { /* ... */ }            /* 5. 다크모드 */
[data-service="blog"][data-theme="dark"] { /* ... */ }

@layer base {
    body {
        @apply bg-bg-page text-text-body;    /* 6. 기본 body 스타일 */
        @apply font-sans antialiased;
    }
}
```

`npm run build:vue`로 Vite 빌드 시:

```
입력: src/styles/index.css + src/components/*.vue
출력: dist/design-system.css   <-- 위 CSS 전부 번들링
      dist/index.js            <-- 컴포넌트 JS
```

`design-system.css` 하나에 **토큰 CSS 변수 + Tailwind + 테마 + 기본 스타일**이 전부 들어갑니다.

---

### STEP 4 -- blog-frontend가 가져다 쓰는 과정

**1단계: CSS Import** (`blog-frontend/src/style.css`)

```css
@import '@portal/design-system-vue/style.css';  /* dist/design-system.css를 가져옴 */
@tailwind base;
@tailwind components;
@tailwind utilities;
```

**2단계: Tailwind 설정** (`blog-frontend/tailwind.config.js`)

```javascript
import designSystemPreset from '@portal/design-tokens/tailwind';
export default {
  presets: [designSystemPreset],   // CSS 변수를 Tailwind 유틸리티로 매핑
  content: ['./src/**/*.{vue,js,ts}'],
}
```

이 preset이 하는 일:

| CSS 변수 | Tailwind 클래스 |
|----------|----------------|
| `--semantic-bg-page` | `bg-bg-page` |
| `--semantic-text-heading` | `text-text-heading` |
| `--semantic-brand-primary` | `bg-brand-primary`, `text-brand-primary` |
| `--semantic-border-default` | `border-border-default` |

**3단계: 앱 엔트리** (`blog-frontend/src/main.ts`)

```typescript
import './style.css';       // style.css 로드 -> design-system.css 로드 -> CSS 변수 활성화
import { createApp } from 'vue';
import App from './App.vue';
// ... 앱 마운트
```

**4단계: 컴포넌트 사용** (`blog-frontend/src/components/PostCard.vue`)

```vue
<script setup>
import { Card, Tag, Avatar } from '@portal/design-system-vue';
//                              ^
//                              dist/index.js에서 컴포넌트 가져옴
</script>

<template>
  <Card hoverable padding="none" class="velog-card group cursor-pointer">
    <!-- design-system의 Card 컴포넌트 사용 -->

    <h2 class="post-title group-hover:text-brand-primary">
      <!--                             ^
           Tailwind 클래스 -> var(--semantic-brand-primary)
           -> blog 테마면 #12B886 (그린)                  -->
    </h2>

    <Tag variant="default" size="sm">
      <!-- design-system의 Tag 컴포넌트 사용 -->
      {{ tag }}
    </Tag>

    <Avatar :name="post.authorName" size="xs" />
    <!-- design-system의 Avatar 컴포넌트 사용 -->
  </Card>
</template>

<style scoped>
.post-title {
  color: var(--semantic-text-heading);   /* CSS 변수 직접 사용도 가능 */
}
.post-summary {
  color: var(--semantic-text-body);
}
.thumbnail-wrapper {
  background-color: var(--semantic-bg-muted);
}
.meta-section {
  border-top: 1px solid var(--semantic-border-muted);
}
</style>
```

---

### STEP 5 -- 컴포넌트 내부에서 실제로 어떻게 스타일링하는가

#### Card.vue 내부

**파일**: `design-system-vue/src/components/Card/Card.vue`

```javascript
const variantClasses = {
  elevated: [
    'bg-[#0f1011]',                                              // 다크모드 기본 배경
    'border border-[#2a2a2a]',
    'shadow-[0_1px_2px_rgba(0,0,0,0.3)]',
    'light:bg-white light:border-gray-200 light:shadow-sm'       // 라이트모드 오버라이드
  ].join(' '),

  interactive: [
    'bg-[#0f1011]',
    'hover:border-[#3a3a3a] hover:bg-[#18191b]',
    'hover:-translate-y-0.5',                                    // 호버 시 살짝 떠오르는 효과
    'cursor-pointer',
    'light:bg-white light:border-gray-200',
  ].join(' ')
};

const paddingClasses = {
  none: '',       // PostCard에서 padding="none"으로 사용
  md: 'p-4',
  lg: 'p-6',
};
```

`light:` prefix는 `tailwind.preset.js`에서 정의한 커스텀 Tailwind variant로, `[data-theme="light"]` 일 때만 적용됩니다.

#### Tag.vue 내부

**파일**: `design-system-vue/src/components/Tag/Tag.vue`

```javascript
const variantClasses = {
  default: 'bg-bg-muted text-text-meta hover:bg-bg-hover border border-border-muted',
  //       ^                                                ^
  //       bg-bg-muted -> var(--semantic-bg-muted)          border-border-muted -> var(--semantic-border-muted)

  primary: 'bg-brand-primary/10 text-brand-primary border border-brand-primary/20',
  //       ^
  //       bg-brand-primary/10 -> var(--semantic-brand-primary) + opacity 10%
};

const sizeClasses = {
  sm: 'text-xs px-2 py-1 rounded-md',     // PostCard에서 size="sm" 사용
  md: 'text-sm px-3 py-1.5 rounded-lg',
};
```

#### Button.vue 내부

**파일**: `design-system-vue/src/components/Button/Button.vue`

```javascript
const variantClasses = {
  primary: [
    // 다크모드 (기본): 밝은 버튼 + 어두운 텍스트
    'bg-white/90 text-[#08090a]',
    'hover:bg-white',
    // 라이트모드: 브랜드 컬러 버튼 + 흰 텍스트
    'light:bg-brand-primary light:text-white',
    'light:hover:bg-brand-primaryHover',
  ].join(' '),

  secondary: [
    'bg-transparent text-text-body',
    'border border-[#2a2a2a]',
    'hover:bg-white/5 hover:text-text-heading',
    'light:hover:bg-gray-100 light:border-gray-200',
  ].join(' '),

  danger: [
    'bg-[#E03131] text-white',              // danger는 다크/라이트 동일
    'hover:bg-[#C92A2A]',
  ].join(' ')
};

const sizeClasses = {
  sm: 'h-8 px-3 text-sm gap-1.5',
  md: 'h-9 px-4 text-sm gap-2',
  lg: 'h-11 px-5 text-base gap-2'
};
```

---

## Part 3: 테마 전환 상세

### useTheme composable

**파일**: `design-system-vue/src/composables/useTheme.ts`

```typescript
export function useTheme() {
  const currentService = ref<'portal' | 'blog' | 'shopping'>('portal');
  const currentTheme = ref<'light' | 'dark'>('light');

  // 서비스 변경 -> HTML 속성 변경 -> CSS 변수 자동 전환
  const setService = (service) => {
    document.documentElement.setAttribute('data-service', service);
    // <html data-service="blog">
    // -> [data-service="blog"] { --semantic-brand-primary: #12B886 } 활성화
    localStorage.setItem('portal-service', service);  // 새로고침 시에도 유지
  };

  // 테마 변경 -> 다크/라이트 전환
  const setTheme = (mode) => {
    document.documentElement.setAttribute('data-theme', mode);
    // <html data-theme="dark">
    // -> [data-service="blog"][data-theme="dark"] { ... } 활성화

    // Tailwind의 darkMode: 'class'도 지원
    if (mode === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
    localStorage.setItem('portal-theme', mode);
  };

  // 초기화: localStorage 또는 시스템 설정에서 복원
  const initTheme = () => {
    const savedTheme = localStorage.getItem('portal-theme');
    if (savedTheme) {
      setTheme(savedTheme);
    } else {
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      setTheme(prefersDark ? 'dark' : 'light');
    }
  };

  onMounted(() => initTheme());  // 컴포넌트 마운트 시 자동 초기화
}
```

### CSS 우선순위

브라우저에서 `<html data-service="blog" data-theme="dark">` 상태일 때:

```
1. :root { --semantic-brand-primary: #5e6ad2 }                     <-- 기본값 (Portal 다크)
2. [data-theme="dark"] { --semantic-brand-primary: #20C997 }       <-- 글로벌 다크 오버라이드
3. [data-service="blog"] { --semantic-brand-primary: #12B886 }     <-- Blog 라이트 기본
4. [data-service="blog"][data-theme="dark"] {                      <-- 최종 승자
     --semantic-brand-primary: #20C997                               (가장 구체적인 셀렉터)
     --semantic-bg-page: #08090a
   }
```

CSS specificity 규칙에 의해 **가장 구체적인 셀렉터가 이깁니다**:

| 조합 | brand-primary 값 |
|------|-----------------|
| Blog + Light | `#12B886` (밝은 그린) |
| Blog + Dark | `#20C997` (다크모드용 그린) |
| Portal 기본 | `#5e6ad2` (인디고) |
| Shopping + Light | `#FD7E14` (오렌지) |

---

## Part 4: 빌드 과정 상세

### 빌드 명령

**파일**: `frontend/package.json`

```bash
npm run build
# = npm run build:design && npm run build:apps
# = (build:tokens && build:types && build:vue && build:react) && (build:shell && build:blog && build:shopping)
```

### 각 단계에서 일어나는 일

| 단계 | 명령 | 동작 | 출력 |
|------|------|------|------|
| 1 | `build:tokens` | `node scripts/build-tokens.js` 실행. JSON 읽기, 참조 resolve, CSS 변수 생성 | `dist/tokens.css`, `tokens.json`, `tokens.js`, `tokens.d.ts` |
| 2 | `build:types` | `tsc`로 TypeScript 컴파일 | `dist/index.d.ts` (타입 선언만) |
| 3 | `build:vue` | Vite build (lib 모드). styles/index.css + components/*.vue 번들링 | `dist/design-system.css`, `dist/index.js` |
| 4 | `build:react` | Vite build (lib 모드). components/*.tsx 번들링 | `dist/index.js`, `dist/index.d.ts` |
| 5 | `build:apps` | 각 앱별 Vite build | 각 앱의 `dist/` 산출물 |

### workspace 심볼릭 링크

```bash
# npm install 실행 시 자동으로:
frontend/node_modules/@portal/design-tokens     -> ../../design-tokens     (심볼릭 링크)
frontend/node_modules/@portal/design-system-vue  -> ../../design-system-vue  (심볼릭 링크)
frontend/node_modules/@portal/design-system-react -> ../../design-system-react

# 그래서 코드에서 이렇게 쓸 수 있음:
import { Button } from '@portal/design-system-vue'
# 실제로는 ../design-system-vue/dist/index.js를 가리킴
```

---

## Part 5: 사용 방법 요약

### 방법 1: CSS 변수 직접 사용 (scoped style)

```vue
<style scoped>
.my-section {
  background: var(--semantic-bg-card);
  color: var(--semantic-text-body);
  border: 1px solid var(--semantic-border-default);
}
.my-title {
  color: var(--semantic-text-heading);
}
.my-link:hover {
  color: var(--semantic-brand-primary);
}
</style>
```

### 방법 2: Tailwind 클래스 사용 (template)

```vue
<template>
  <div class="bg-bg-card text-text-body border border-border-default rounded-lg p-4">
    <h2 class="text-text-heading text-xl font-semibold">제목</h2>
    <p class="text-text-meta text-sm">부가 정보</p>
    <button class="bg-brand-primary text-white px-4 py-2 rounded hover:bg-brand-primaryHover">
      액션
    </button>
  </div>
</template>
```

### 방법 3: Design System 컴포넌트 사용

```vue
<script setup>
import { Button, Card, Tag, Input, Modal, Alert, Spinner } from '@portal/design-system-vue';
</script>

<template>
  <Card variant="elevated" padding="md" hoverable>
    <Input placeholder="검색어 입력..." size="md" />
    <Tag variant="primary" size="sm">카테고리</Tag>
    <Button variant="primary" size="md" :loading="isLoading">
      저장하기
    </Button>
  </Card>

  <Modal v-model="showModal" title="확인">
    <Alert variant="warning">정말 삭제하시겠습니까?</Alert>
  </Modal>

  <Spinner v-if="loading" size="lg" />
</template>
```

---

## Part 6: 사용 가능한 컴포넌트

### Vue 컴포넌트 (`@portal/design-system-vue`) -- 33개

| 카테고리 | 컴포넌트 | 주요 Props |
|---------|---------|-----------|
| **Form** | `Button` | variant(`primary`/`secondary`/`ghost`/`outline`/`danger`), size, disabled, loading, fullWidth |
| | `Input` | size, placeholder, disabled |
| | `Textarea` | rows, placeholder |
| | `Checkbox` | checked, disabled |
| | `Radio` | checked, disabled |
| | `Switch` | checked, disabled |
| | `Select` | options, placeholder |
| | `FormField` | label, error, required |
| | `SearchBar` | placeholder, size |
| **Layout** | `Card` | variant(`elevated`/`outlined`/`flat`/`glass`/`interactive`), padding(`none`/`sm`/`md`/`lg`/`xl`), hoverable |
| | `Container` | maxWidth |
| | `Stack` | direction, gap, align |
| | `Divider` | orientation |
| **Feedback** | `Alert` | variant(`info`/`success`/`warning`/`error`) |
| | `Spinner` | size |
| | `Skeleton` | width, height |
| | `Toast` | position, duration |
| | `Modal` | title, v-model |
| **Navigation** | `Link` | href, external |
| | `Dropdown` | items |
| | `Tabs` | items, activeTab |
| | `Breadcrumb` | items |
| **Data** | `Badge` | variant, size |
| | `Tag` | variant(`default`/`primary`/`success`/`error`/`warning`/`info`), size, removable, clickable |
| | `Avatar` | name, src, size |

### React 컴포넌트 (`@portal/design-system-react`) -- 31개

Vue와 동일한 컴포넌트에 추가로:

| 추가 컴포넌트 | 주요 Props |
|-------------|-----------|
| `Progress` | value, max |
| `Table` | columns, data |
| `Tooltip` | content, placement |
| `Popover` | trigger, content |
| `Pagination` | total, currentPage, pageSize |

---

## Part 7: 실제 사용 현황

### blog-frontend -- 28개 파일에서 사용

| 파일 | 사용 컴포넌트 |
|------|-------------|
| `PostCard.vue` | `Card`, `Tag`, `Avatar` |
| `PostWritePage.vue` | `Button`, `Input`, `Card`, `Textarea` |
| `PostEditPage.vue` | `Button`, `Card`, `Input` |
| `PostListPage.vue` | `Button`, `Card`, `SearchBar` |
| `MyPage.vue` | `Button`, `Spinner`, `Alert`, `Card` |
| `CommentList.vue` | `Card` |
| `CommentItem.vue` | `Avatar`, `Button` |
| `CommentForm.vue` | `Textarea`, `Button` |
| `LikeButton.vue` | `Button` |
| `LikersModal.vue` | `Avatar`, `Button`, `Modal`, `Spinner` |
| `FollowButton.vue` | `Button` |
| `FollowerModal.vue` | `Avatar`, `Button`, `Modal`, `Spinner` |
| `ProfileEditForm.vue` | `Button`, `Input`, `Textarea`, `Alert` |

### portal-shell -- 7개 파일

| 파일 | 사용 컴포넌트 |
|------|-------------|
| `HomePage.vue` | `Button`, `Badge` |
| `LoginModal.vue` | `Modal`, `Input`, `Button` |
| `SignupPage.vue` | `Button`, `Card`, `Input` |
| `RemoteWrapper.vue` | `Spinner`, `Button`, `Card` |

### shopping-frontend -- 24개 파일

| 파일 | 사용 컴포넌트 |
|------|-------------|
| `ProductDetailPage.tsx` | `Button`, `Spinner`, `Alert`, `Badge` |
| `CartPage.tsx` | `Button`, `Spinner`, `Alert` |
| `CheckoutPage.tsx` | `Button`, `Alert`, `Input` |
| `AdminTimeDealFormPage.tsx` | `Button`, `Card`, `Input`, `Select`, `Spinner` |
| `OrderListPage.tsx` | `Button`, `Spinner`, `Alert`, `Badge` |

---

## Part 8: 핵심 파일 맵

| 목적 | 파일 경로 |
|------|----------|
| 색상 원시값 | `design-tokens/src/tokens/base/colors.json` |
| 의미 매핑 | `design-tokens/src/tokens/semantic/colors.json` |
| 블로그 테마 | `design-tokens/src/tokens/themes/blog.json` |
| 쇼핑 테마 | `design-tokens/src/tokens/themes/shopping.json` |
| JSON->CSS 변환 | `design-tokens/scripts/build-tokens.js` |
| Tailwind preset | `design-tokens/tailwind.preset.js` |
| Vue CSS 번들 원본 | `design-system-vue/src/styles/index.css` |
| Vue 컴포넌트 export | `design-system-vue/src/components/index.ts` |
| Card 컴포넌트 | `design-system-vue/src/components/Card/Card.vue` |
| Button 컴포넌트 | `design-system-vue/src/components/Button/Button.vue` |
| Tag 컴포넌트 | `design-system-vue/src/components/Tag/Tag.vue` |
| 테마 전환 로직 | `design-system-vue/src/composables/useTheme.ts` |
| 앱 CSS 진입점 | `blog-frontend/src/style.css` |
| 앱 JS 진입점 | `blog-frontend/src/main.ts` |
| PostCard 실제 사용 | `blog-frontend/src/components/PostCard.vue` |
| workspace 설정 | `frontend/package.json` |

---

## 다음 단계

- [Getting Started](./getting-started.md) -- 토큰 패키지 설치 및 기본 설정
- [Theming Guide](./theming-guide.md) -- 테마 커스터마이징 방법
