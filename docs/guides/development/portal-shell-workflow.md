---
id: portal-shell-development
title: Portal Shell - Development Workflow
type: guide
status: current
created: 2026-01-18
updated: 2026-01-18
author: Laze
tags: [portal-shell, development, workflow, debugging, testing, vue3]
related:
  - portal-shell-getting-started
  - module-federation-guide
---

# Portal Shell - Development Workflow

**난이도**: ⭐⭐ | **예상 시간**: 20분 | **카테고리**: Development

> Portal Shell 개발 프로세스 및 베스트 프랙티스

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **대상** | Portal Shell 개발자 |
| **포함 내용** | 개발, 디버깅, 테스트, 배포 |
| **브랜치 전략** | Git Flow |
| **코드 리뷰** | PR 기반 |

---

## 🔄 개발 흐름

```
Issue/Task 생성
   ↓
Feature Branch 생성
   ↓
로컬 개발 & 테스트
   ↓
Commit & Push
   ↓
Pull Request 생성
   ↓
코드 리뷰 & 승인
   ↓
Main/Dev Branch에 Merge
   ↓
배포
```

---

## 1️⃣ 개발 시작

### Step 1: Issue/Task 확인

GitHub Issues 또는 Jira에서 작업할 태스크를 확인합니다.

```
예시:
[PORTAL-123] Add user profile dropdown to header
```

### Step 2: 최신 코드 동기화

```bash
git checkout main
git pull origin main
```

### Step 3: Feature Branch 생성

**브랜치 명명 규칙:**

```
[type]/[ISSUE-ID]-[description]
```

| Type | 용도 | 예시 |
|------|------|------|
| `feature` | 새 기능 | `feature/PORTAL-123-user-profile` |
| `fix` | 버그 수정 | `fix/PORTAL-124-header-alignment` |
| `refactor` | 리팩토링 | `refactor/PORTAL-125-auth-service` |
| `docs` | 문서 작업 | `docs/PORTAL-126-api-guide` |
| `test` | 테스트 추가 | `test/PORTAL-127-auth-unit-tests` |
| `chore` | 빌드, 설정 | `chore/PORTAL-128-upgrade-vite` |

**예시:**

```bash
git checkout -b feature/PORTAL-123-user-profile
```

---

## 2️⃣ 로컬 개발

### Step 1: 의존성 설치 (최초 1회)

```bash
cd frontend
npm install
```

### Step 2: 개발 서버 실행

**전체 마이크로 프론트엔드 실행 (권장):**

```bash
npm run dev
```

이 명령어는 다음을 동시에 실행합니다:
- portal-shell (포트 30000)
- blog-frontend (포트 30001)
- shopping-frontend (포트 30002)
- design-system (포트 30003)

**portal-shell만 실행:**

```bash
npm run dev:portal
```

### Step 3: 브라우저 접속

```
http://localhost:30000
```

### Step 4: 코드 수정

**디렉토리 구조:**

```
src/
├── components/     # UI 컴포넌트
├── views/          # 페이지 컴포넌트
├── store/          # Pinia 스토어
├── api/            # API 클라이언트
├── router/         # 라우터
├── services/       # 비즈니스 로직
├── utils/          # 유틸리티
└── types/          # TypeScript 타입
```

### Step 5: Hot Module Replacement (HMR)

Vite는 파일 저장 시 자동으로 브라우저를 업데이트합니다.

```bash
# 콘솔 출력
✅ [vite] hmr update /src/components/Header.vue
```

---

## 3️⃣ 코드 작성 규칙

### Vue 3 Composition API

**권장 패턴:**

```vue
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useAuthStore } from '@/store/auth';

// Props 정의
interface Props {
  title: string;
  count?: number;
}
const props = withDefaults(defineProps<Props>(), {
  count: 0
});

// Emits 정의
const emit = defineEmits<{
  update: [value: string];
  close: [];
}>();

// Reactive 상태
const isOpen = ref(false);
const authStore = useAuthStore();

// Computed
const userName = computed(() => authStore.user?.name || 'Guest');

// Methods
function handleClick() {
  emit('update', 'new value');
}

// Lifecycle
onMounted(() => {
  console.log('Component mounted');
});
</script>

<template>
  <div class="component">
    <h2>{{ props.title }}</h2>
    <p>{{ userName }}</p>
    <button @click="handleClick">Click</button>
  </div>
</template>

<style scoped>
.component {
  /* Scoped styles */
}
</style>
```

### TypeScript 타입 정의

**공통 타입은 `src/types/`에 정의:**

```typescript
// src/types/user.ts
export interface User {
  id: string;
  email: string;
  name: string;
  roles: string[];
}

export interface UserProfile extends User {
  avatar?: string;
  bio?: string;
}
```

### API 호출

**API 클라이언트 사용:**

```typescript
// src/api/userApi.ts
import apiClient from './apiClient';
import type { User } from '@/types/user';

export const userApi = {
  async getProfile(): Promise<User> {
    const response = await apiClient.get('/api/v1/users/me');
    return response.data;
  },

  async updateProfile(data: Partial<User>): Promise<User> {
    const response = await apiClient.put('/api/v1/users/me', data);
    return response.data;
  }
};
```

**컴포넌트에서 사용:**

```typescript
import { userApi } from '@/api/userApi';

async function loadProfile() {
  try {
    const user = await userApi.getProfile();
    console.log('User:', user);
  } catch (error) {
    console.error('Failed to load profile:', error);
  }
}
```

### Pinia Store

**Store 정의:**

```typescript
// src/store/user.ts
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { User } from '@/types/user';

export const useUserStore = defineStore('user', () => {
  // State
  const user = ref<User | null>(null);
  const loading = ref(false);

  // Getters
  const isLoggedIn = computed(() => !!user.value);
  const userName = computed(() => user.value?.name || 'Guest');

  // Actions
  async function loadUser() {
    loading.value = true;
    try {
      const response = await fetch('/api/v1/users/me');
      user.value = await response.json();
    } finally {
      loading.value = false;
    }
  }

  function clearUser() {
    user.value = null;
  }

  return {
    user,
    loading,
    isLoggedIn,
    userName,
    loadUser,
    clearUser
  };
});
```

**컴포넌트에서 사용:**

```vue
<script setup lang="ts">
import { useUserStore } from '@/store/user';

const userStore = useUserStore();

onMounted(() => {
  userStore.loadUser();
});
</script>

<template>
  <div v-if="userStore.loading">Loading...</div>
  <div v-else>{{ userStore.userName }}</div>
</template>
```

---

## 4️⃣ 디버깅

### Vue Devtools

**설치:**

- Chrome: [Vue.js devtools](https://chrome.google.com/webstore/detail/vuejs-devtools/nhdogjmejiglipccpnnnanhbledajbpd)
- Firefox: [Vue.js devtools](https://addons.mozilla.org/en-US/firefox/addon/vue-js-devtools/)

**사용법:**

1. 브라우저 개발자 도구 열기 (F12)
2. "Vue" 탭 선택
3. 다음 기능 사용:
   - 컴포넌트 트리 탐색
   - Props/Data 확인
   - Pinia Store 상태 확인
   - 이벤트 추적
   - 라우터 네비게이션 확인

### 콘솔 로그

**구조화된 로그 사용:**

```typescript
// src/utils/logger.ts
export const logger = {
  info(message: string, data?: any) {
    console.log(`ℹ️ [INFO] ${message}`, data);
  },
  warn(message: string, data?: any) {
    console.warn(`⚠️ [WARN] ${message}`, data);
  },
  error(message: string, error?: any) {
    console.error(`❌ [ERROR] ${message}`, error);
  },
  debug(message: string, data?: any) {
    if (import.meta.env.DEV) {
      console.debug(`🐛 [DEBUG] ${message}`, data);
    }
  }
};
```

**사용 예시:**

```typescript
import { logger } from '@/utils/logger';

try {
  const user = await userApi.getProfile();
  logger.info('User profile loaded', user);
} catch (error) {
  logger.error('Failed to load user profile', error);
}
```

### Module Federation 디버깅

**Remote 로드 상태 확인:**

```javascript
// 브라우저 콘솔에서
window.__FEDERATION__
```

**특정 Remote 확인:**

```javascript
// blog Remote 확인
console.log(__FEDERATION__.instances.blog)
```

**remoteEntry.js 로드 확인:**

Network 탭에서 다음 요청 확인:

```
http://localhost:30001/assets/remoteEntry.js  [Status: 200]
```

### Vite 디버그 모드

```bash
DEBUG=vite:* npm run dev
```

### TypeScript 타입 체크

```bash
vue-tsc --noEmit
```

---

## 5️⃣ 테스트

### 단위 테스트 (Vitest)

**테스트 파일 생성:**

```typescript
// src/components/__tests__/Header.spec.ts
import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import Header from '../Header.vue';

describe('Header', () => {
  it('renders properly', () => {
    const wrapper = mount(Header, {
      props: { title: 'Test Title' }
    });
    expect(wrapper.text()).toContain('Test Title');
  });

  it('emits logout event', async () => {
    const wrapper = mount(Header);
    await wrapper.find('.logout-btn').trigger('click');
    expect(wrapper.emitted('logout')).toBeTruthy();
  });
});
```

**테스트 실행:**

```bash
# 전체 테스트
npm run test

# Watch 모드
npm run test:watch

# 커버리지
npm run test:coverage
```

### E2E 테스트 (Playwright)

**테스트 파일 생성:**

```typescript
// e2e/portal-shell.spec.ts
import { test, expect } from '@playwright/test';

test('homepage loads correctly', async ({ page }) => {
  await page.goto('http://localhost:30000');
  await expect(page).toHaveTitle(/Portal Universe/);
  await expect(page.locator('h1')).toContainText('Welcome');
});

test('navigation to blog works', async ({ page }) => {
  await page.goto('http://localhost:30000');
  await page.click('text=Blog');
  await expect(page).toHaveURL('http://localhost:30000/blog');
});
```

**테스트 실행:**

```bash
# E2E 테스트
npm run test:e2e

# UI 모드
npm run test:e2e:ui
```

---

## 6️⃣ Commit & Push

### Commit 메시지 규칙

**포맷:**

```
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

**Types:**

| Type | 설명 | 예시 |
|------|------|------|
| `feat` | 새 기능 | `feat(auth): add logout button` |
| `fix` | 버그 수정 | `fix(header): resolve mobile menu issue` |
| `docs` | 문서 변경 | `docs(readme): update setup guide` |
| `style` | 코드 스타일 | `style(header): format with prettier` |
| `refactor` | 리팩토링 | `refactor(api): extract auth logic` |
| `test` | 테스트 추가 | `test(header): add unit tests` |
| `chore` | 빌드/설정 | `chore(deps): upgrade vue to 3.5.21` |

**예시:**

```bash
git add src/components/Header.vue
git commit -m "feat(header): add user profile dropdown

- Add dropdown menu component
- Display user name and avatar
- Add logout button
- Integrate with authStore"
```

### Push

```bash
git push origin feature/PORTAL-123-user-profile
```

---

## 7️⃣ Pull Request

### PR 생성

1. GitHub에서 "New Pull Request" 클릭
2. Base: `main` (또는 `dev`), Compare: `feature/PORTAL-123-user-profile`
3. PR 템플릿 작성:

```markdown
## 📋 Summary
User profile dropdown 추가

## 🎯 Changes
- [ ] Add dropdown menu component
- [ ] Display user name and avatar
- [ ] Add logout button
- [ ] Integrate with authStore

## 🧪 Test Plan
- [ ] Unit tests pass
- [ ] E2E tests pass
- [ ] Manual test in Chrome/Firefox
- [ ] Mobile responsive test

## 📸 Screenshots
[스크린샷 첨부]

## 🔗 Related Issues
Closes #123
```

### 코드 리뷰

1. 리뷰어 지정 (팀 리드 또는 시니어 개발자)
2. CI/CD 체크 통과 대기
3. 리뷰 피드백 반영:

```bash
# 추가 커밋
git add .
git commit -m "fix(header): apply code review feedback"
git push origin feature/PORTAL-123-user-profile
```

### Merge

1. 승인 후 "Squash and merge" 또는 "Merge pull request"
2. Feature branch 삭제

```bash
git checkout main
git pull origin main
git branch -d feature/PORTAL-123-user-profile
```

---

## 8️⃣ 빌드 & 배포

### 로컬 빌드

**개발 빌드:**

```bash
npm run build:dev
```

**Docker 빌드:**

```bash
npm run build:docker
```

**Kubernetes 빌드:**

```bash
npm run build:k8s
```

### 빌드 결과 확인

```bash
ls -lh dist/
```

**출력 예시:**

```
dist/
├── assets/
│   ├── shellEntry.js       # Module Federation entry
│   ├── index.css
│   └── index.js
└── index.html
```

### 빌드 테스트

```bash
npm run preview
```

브라우저에서 확인:

```
http://localhost:30000
```

### Docker 이미지 빌드

```bash
docker build -t portal-shell:latest .
```

### Docker 컨테이너 실행

```bash
docker run -p 30000:80 portal-shell:latest
```

### Kubernetes 배포

```bash
kubectl apply -f k8s/portal-shell.yaml
```

---

## 9️⃣ 성능 최적화

### Bundle 분석

```bash
npm run build:dev -- --mode analyze
```

### Code Splitting

**동적 import 사용:**

```typescript
// router/index.ts
const routes = [
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/ProfilePage.vue')  // ✅ Lazy load
  }
];
```

### Image 최적화

```vue
<template>
  <!-- WebP 사용 -->
  <img src="/images/avatar.webp" alt="Avatar" loading="lazy" />
</template>
```

### Lighthouse 점수 확인

1. Chrome DevTools → Lighthouse 탭
2. "Generate report" 클릭
3. Performance, Accessibility, Best Practices, SEO 점수 확인

---

## 🔟 보안

### 환경 변수 보호

**절대 커밋하지 말 것:**

- `.env.local`
- API 키
- 시크릿

**.gitignore 확인:**

```
.env.local
.env.*.local
```

### XSS 방지

Vue는 기본적으로 XSS 방지를 제공하지만, `v-html` 사용 시 주의:

```vue
<!-- ❌ 위험 -->
<div v-html="userInput"></div>

<!-- ✅ 안전 -->
<div>{{ userInput }}</div>
```

### CSRF 방지

API 클라이언트에 CSRF 토큰 포함:

```typescript
// src/api/apiClient.ts
apiClient.interceptors.request.use(config => {
  const csrfToken = document.querySelector('meta[name="csrf-token"]')?.getAttribute('content');
  if (csrfToken) {
    config.headers['X-CSRF-TOKEN'] = csrfToken;
  }
  return config;
});
```

---

## ➡️ 다음 단계

1. **Remote 모듈 추가**: [Module Federation 통합 가이드](./module-federation-guide.md)
2. **Architecture 문서**: [../architecture/](../architecture/)
3. **API 명세**: [../api/](../api/)
4. **Troubleshooting 가이드**: [../troubleshooting/](../troubleshooting/)

---

## 🔗 관련 문서

- [Getting Started](./getting-started.md)
- [Vue 3 Best Practices](https://vuejs.org/guide/best-practices/)
- [Vite Documentation](https://vite.dev/)

---

**최종 업데이트**: 2026-01-18
