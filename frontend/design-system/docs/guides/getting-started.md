---
id: guide-getting-started
title: 빠른 시작 가이드
type: guide
status: current
created: 2026-01-18
updated: 2026-01-18
author: documenter
tags: [design-system, setup, vue3, getting-started]
related:
  - guide-using-components
  - guide-theming
---

# 빠른 시작 가이드

> @portal/design-system 설치 및 설정 가이드

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **대상** | Vue 3 프론트엔드 개발자 |
| **전제 조건** | Node.js 18+, npm 9+ |

---

## ✅ 사전 요구사항

| 소프트웨어 | 버전 | 확인 명령어 |
|-----------|------|------------|
| Node.js | 18+ | `node --version` |
| npm | 9+ | `npm --version` |
| Vue | 3.5+ | - |

---

## 🔧 설치 및 설정

### Step 1: 라이브러리 설치

Portal Universe 프로젝트는 **npm workspaces**를 사용합니다.

```bash
# frontend 디렉토리로 이동
cd frontend

# 전체 워크스페이스 의존성 설치 (design-system 포함)
npm install
```

### Step 2: Vue 앱에서 Import

#### 컴포넌트 Import

```vue
<!-- src/App.vue 또는 컴포넌트 파일 -->
<script setup lang="ts">
import { Button, Input, Card } from '@portal/design-system'
import '@portal/design-system/style.css'
</script>

<template>
  <div>
    <Button variant="primary">클릭</Button>
    <Input v-model="text" placeholder="입력하세요" />
  </div>
</template>
```

#### Tailwind CSS 설정 (선택)

Design System의 Tailwind Preset을 프로젝트에 통합하려면:

```javascript
// tailwind.config.js
import { presetConfig } from '@portal/design-system'

export default {
  presets: [presetConfig],
  content: [
    './src/**/*.{vue,js,ts,jsx,tsx}',
    './node_modules/@portal/design-system/**/*.{vue,js,ts}'
  ]
}
```

### Step 3: 테마 초기화

```vue
<!-- src/App.vue (루트 컴포넌트) -->
<script setup lang="ts">
import { useTheme } from '@portal/design-system'
import { onMounted } from 'vue'

const { initTheme } = useTheme()

onMounted(() => {
  // localStorage 및 시스템 설정 기반 테마 초기화
  initTheme()
})
</script>

<template>
  <div id="app">
    <router-view />
  </div>
</template>
```

---

## ✅ 설치 확인

### 개발 서버 실행

```bash
# portal-shell 실행
npm run dev:portal

# blog-frontend 실행
npm run dev:blog
```

### Storybook 실행

```bash
# design-system Storybook 실행
cd frontend/design-system
npm run storybook
```

브라우저에서 http://localhost:6006 접속

---

## 🎨 첫 컴포넌트 사용

### 예제 1: Button 컴포넌트

```vue
<script setup lang="ts">
import { Button } from '@portal/design-system'

const handleClick = () => {
  alert('버튼 클릭!')
}
</script>

<template>
  <div class="p-4">
    <Button variant="primary" size="md" @click="handleClick">
      클릭하기
    </Button>
  </div>
</template>
```

### 예제 2: Input + Button 조합

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { Button, Input } from '@portal/design-system'

const email = ref('')

const handleSubmit = () => {
  console.log('Email:', email.value)
}
</script>

<template>
  <div class="flex gap-2 p-4">
    <Input
      v-model="email"
      type="email"
      placeholder="user@example.com"
    />
    <Button variant="primary" @click="handleSubmit">
      제출
    </Button>
  </div>
</template>
```

---

## ⚠️ 자주 발생하는 문제

### 문제 1: 스타일이 적용되지 않음

**원인**: `style.css` import 누락

**해결 방법**:
```vue
<script setup>
import '@portal/design-system/style.css'
</script>
```

### 문제 2: Tailwind 클래스가 작동하지 않음

**원인**: Tailwind Preset 미설정 또는 content 경로 누락

**해결 방법**:
```javascript
// tailwind.config.js
export default {
  content: [
    './src/**/*.{vue,js,ts}',
    './node_modules/@portal/design-system/**/*.{vue,js,ts}'
  ]
}
```

### 문제 3: Module Federation 환경에서 중복 인스턴스

**원인**: `shared` 설정 누락

**해결 방법** (vite.config.ts):
```typescript
import federation from '@originjs/vite-plugin-federation'

export default {
  plugins: [
    federation({
      shared: {
        vue: { singleton: true, requiredVersion: '^3.5' },
        '@portal/design-system': { singleton: true }
      }
    })
  ]
}
```

---

## ➡️ 다음 단계

1. **컴포넌트 사용**: [using-components.md](./using-components.md)에서 컴포넌트 상세 사용법
2. **테마 커스터마이징**: [theming-guide.md](./theming-guide.md)에서 Light/Dark 모드, 서비스별 테마 설정

---

## 🔗 관련 문서

- [컴포넌트 사용 가이드](./using-components.md)
- [테마 적용 가이드](./theming-guide.md)
- [API 명세서](../api/README.md)

---

**최종 업데이트**: 2026-01-18
