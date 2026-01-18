# Design System

Portal Universe 플랫폼의 공유 Vue 3 컴포넌트 라이브러리 및 디자인 토큰 시스템입니다.

## 개요

Design System은 다음을 제공합니다:

- **재사용 가능한 Vue 3 컴포넌트**: 강타입(TypeScript) UI 컴포넌트 라이브러리
- **디자인 토큰 시스템**: 3-Layer 아키텍처로 관리되는 일관된 스타일
- **서비스별 테마**: Blog, Shopping 등 각 서비스의 브랜드 정체성 유지
- **다크 모드 지원**: Light/Dark 테마 자동 전환
- **Storybook 통합**: 모든 컴포넌트의 시각적 카탈로그

## 빠른 시작

### 설치

```bash
cd frontend/design-system
npm install
```

### 개발

```bash
# Storybook에서 컴포넌트 확인
npm run storybook

# 컴포넌트 개발 및 테스트
npm run test:watch
```

### 빌드

```bash
npm run build
```

## 주요 기능

### 1. 통일된 컴포넌트 API

모든 Vue 3 컴포넌트는 Composition API를 사용하며 강타입 Props를 제공합니다:

```typescript
import { Button, Card, Modal } from '@portal/design-system';
```

### 2. 디자인 토큰

Base → Semantic → Component 3단계로 계층화된 토큰 시스템으로 유지보수 최소화:

```css
/* Base: 절대값 */
--color-green-600: #12B886;

/* Semantic: 용도 기반 */
--color-brand-primary: var(--color-green-600);

/* Component: 실제 사용 */
.button-primary {
  background-color: var(--color-brand-primary);
}
```

### 3. 서비스별 테마

동일한 컴포넌트와 토큰 이름을 사용하면서 `data-service` 속성으로 테마 오버라이드:

```html
<!-- Blog 서비스 (Green) -->
<div data-service="blog" data-theme="light">
  <button class="bg-brand-primary">발행</button>
</div>

<!-- Shopping 서비스 (Orange) -->
<div data-service="shopping" data-theme="light">
  <button class="bg-brand-primary">구매</button>
</div>
```

### 4. 다크 모드

시스템 설정 또는 사용자 선택에 따른 자동 다크 모드:

```typescript
import { useTheme } from '@portal/design-system';

const { toggleTheme } = useTheme();
toggleTheme(); // Light ↔ Dark 전환
```

## 프로젝트 구조

```
design-system/
├── src/
│   ├── components/           # Vue 3 컴포넌트들
│   │   ├── Button/
│   │   ├── Card/
│   │   ├── Modal/
│   │   └── ...
│   ├── composables/
│   │   └── useTheme.ts       # 테마 관리
│   ├── types/
│   │   └── theme.ts          # 타입 정의
│   ├── styles/
│   │   └── index.css         # 글로벌 스타일 및 토큰
│   └── tokens/
│       ├── base/             # Base 토큰 (JSON)
│       ├── semantic/         # Semantic 토큰 (JSON)
│       └── themes/           # 서비스별 오버라이드
├── .storybook/               # Storybook 설정
├── docs/
│   ├── README.md             # 이 문서
│   ├── COMPONENTS.md         # 컴포넌트 카탈로그
│   ├── TOKENS.md             # 토큰 시스템 상세
│   ├── THEMING.md            # 테마 구성 및 커스터마이징
│   ├── USAGE.md              # 사용 가이드
│   └── architecture.md       # 아키텍처 상세
├── scripts/
│   └── build-tokens.js       # 토큰 빌드 스크립트
├── vitest.config.ts          # 테스트 설정
├── vite.config.ts            # Vite 설정
└── tailwind.config.js        # Tailwind CSS 설정
```

## 사용 예제

### 컴포넌트 사용

```vue
<template>
  <div class="p-6">
    <!-- 버튼 -->
    <Button
      variant="primary"
      size="md"
      @click="handleClick"
    >
      클릭하기
    </Button>

    <!-- 카드 -->
    <Card class="mt-4">
      <CardHeader>
        <CardTitle>제목</CardTitle>
      </CardHeader>
      <CardContent>
        <p>카드 본문 내용입니다.</p>
      </CardContent>
    </Card>

    <!-- 모달 -->
    <Modal v-model="isOpen" title="확인">
      <p>모달 내용입니다.</p>
      <template #footer>
        <Button @click="isOpen = false">닫기</Button>
      </template>
    </Modal>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Button, Card, CardHeader, CardTitle, CardContent, Modal } from '@portal/design-system'

const isOpen = ref(false)

const handleClick = () => {
  console.log('Button clicked')
}
</script>
```

### 테마 관리

```vue
<script setup lang="ts">
import { useTheme } from '@portal/design-system'

const { currentTheme, setTheme, toggleTheme, setService } = useTheme()

// 초기화 시 서비스 설정
setService('shopping')

// 다크 모드 토글
const handleThemeToggle = () => {
  toggleTheme()
}
</script>

<template>
  <button @click="handleThemeToggle">
    테마 전환 (현재: {{ currentTheme }})
  </button>
</template>
```

## 개발 커맨드

| 명령어 | 설명 |
|--------|------|
| `npm run dev` | Vite 개발 서버 실행 |
| `npm run storybook` | Storybook 실행 (localhost:6006) |
| `npm run test` | 테스트 실행 |
| `npm run test:watch` | 테스트 감시 모드 |
| `npm run build` | 프로덕션 빌드 |
| `npm run build-storybook` | Storybook 빌드 |

## 문서 네비게이션

- **[COMPONENTS.md](./COMPONENTS.md)** - 모든 컴포넌트 카탈로그 및 사용 예제
- **[TOKENS.md](./TOKENS.md)** - 디자인 토큰 시스템 상세 설명
- **[THEMING.md](./THEMING.md)** - 테마 구성, 커스터마이징, 서비스별 오버라이드
- **[USAGE.md](./USAGE.md)** - 통합 가이드 및 모범 사례
- **[architecture.md](./architecture.md)** - 아키텍처 설계 원칙

## 기술 스택

- **Vue 3**: Composition API 기반 프론트엔드 프레임워크
- **TypeScript**: 강타입 개발
- **Tailwind CSS**: 유틸리티 기반 스타일링
- **Vite**: 차세대 빌드 도구
- **Storybook**: UI 컴포넌트 개발 환경
- **Vitest**: 단위 테스트 프레임워크
- **PostCSS**: CSS 전처리

## 브라우저 호환성

- Chrome/Edge: Latest
- Firefox: Latest
- Safari: Latest
- iOS Safari: 12+
- Chrome Android: Latest

## 라이선스

MIT

## 지원 및 기여

이슈 및 PR은 Portal Universe 저장소에서 처리됩니다.