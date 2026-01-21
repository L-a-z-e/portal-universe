# Blog Frontend

Portal Universe의 블로그 프론트엔드 모듈입니다.

## 기술 스택

- **Framework**: Vue 3 (Composition API + `<script setup>`)
- **Language**: TypeScript
- **Build Tool**: Vite 7
- **Styling**: Tailwind CSS
- **State Management**: Pinia
- **Editor**: Toast UI Editor
- **Module Federation**: @originjs/vite-plugin-federation

## 개발 환경 설정

### 1. 의존성 설치

```bash
npm install
```

### 2. 개발 서버 실행

```bash
npm run dev
```

개발 서버가 `http://localhost:30001`에서 실행됩니다.

### 3. 빌드

```bash
# 개발 환경
npm run build:dev

# Docker 환경
npm run build:docker

# Kubernetes 환경
npm run build:k8s
```

### 4. 프리뷰

```bash
npm run preview
```

## 테스트

### E2E 테스트 (Playwright)

Phase 1-A 기능에 대한 E2E 테스트가 포함되어 있습니다.

#### 빠른 시작

```bash
# 브라우저 설치 (최초 1회)
npx playwright install

# 테스트 실행
npm run test:e2e

# UI 모드 (권장)
npm run test:e2e:ui
```

#### 테스트 커버리지

- ✅ 시리즈 기능 (목록, 상세, 네비게이션)
- ✅ 좋아요 기능
- ✅ 태그 페이지 (목록, 상세, 검색, 정렬)
- ✅ 트렌딩/최신 탭
- ✅ 댓글/답글 기능

상세 가이드: [e2e/QUICKSTART.md](./e2e/QUICKSTART.md)

## 프로젝트 구조

```
src/
├── api/              # API 클라이언트
├── components/       # Vue 컴포넌트
├── views/            # 페이지 컴포넌트
├── stores/           # Pinia 스토어
├── router/           # Vue Router 설정
├── types/            # TypeScript 타입
├── dto/              # DTO 타입
└── config/           # 설정 파일

e2e/
├── tests/            # E2E 테스트
├── fixtures/         # 테스트 데이터
├── README.md         # 테스트 문서
└── QUICKSTART.md     # 빠른 시작 가이드
```

## 주요 기능 (Phase 1-A)

### 1. 시리즈 기능
- 시리즈 목록 및 상세 페이지
- 시리즈 내 포스트 순서 관리
- 포스트 상세에서 시리즈 네비게이션

### 2. 좋아요 기능
- 포스트 좋아요/좋아요 취소
- 실시간 카운트 업데이트
- 로그인 상태 확인

### 3. 태그 시스템
- 태그 목록 (정렬, 검색)
- 태그별 포스트 필터링
- 태그 상세 페이지

### 4. 트렌딩/최신
- 메인 페이지 탭 전환
- 기간별 필터 (트렌딩)
- 무한 스크롤

### 5. 댓글/답글
- 댓글 작성, 수정, 삭제
- 답글 (대댓글) 기능
- 답글 접기/펼치기

## 환경 변수

환경별 설정 파일:
- `.env.dev`: 로컬 개발
- `.env.docker`: Docker 환경
- `.env.k8s`: Kubernetes 환경

```env
VITE_API_BASE_URL=http://localhost:30000/api/v1
VITE_BLOG_SERVICE_URL=http://localhost:8080/api/v1
```

## Module Federation

이 앱은 `portal-shell`에서 Remote 모듈로 로드됩니다.

### Exposed Modules
- `./bootstrap`: 앱 진입점

### Remote Dependencies
- `apiClient`: Host에서 제공
- `authStore`: Host에서 제공

## 스크립트 명령어

| 명령어 | 설명 |
|--------|------|
| `npm run dev` | 개발 서버 실행 (watch + preview) |
| `npm run build` | 빌드 (dev 모드) |
| `npm run build:dev` | 개발 환경 빌드 |
| `npm run build:docker` | Docker 환경 빌드 |
| `npm run build:k8s` | Kubernetes 환경 빌드 |
| `npm run preview` | 프리뷰 서버 실행 |
| `npm run clean` | 빌드 산출물 삭제 |
| `npm run test:e2e` | E2E 테스트 실행 |
| `npm run test:e2e:ui` | E2E 테스트 UI 모드 |
| `npm run test:e2e:debug` | E2E 테스트 디버그 모드 |

## 개발 가이드

### 컴포넌트 작성

```vue
<script setup lang="ts">
import { ref, computed } from 'vue'

interface Props {
  title: string
  count?: number
}

const props = withDefaults(defineProps<Props>(), {
  count: 0
})

const emit = defineEmits<{
  (e: 'update', value: number): void
}>()

const localCount = ref(props.count)

const displayText = computed(() => {
  return `${props.title}: ${localCount.value}`
})

const increment = () => {
  localCount.value++
  emit('update', localCount.value)
}
</script>

<template>
  <div class="p-4 border rounded">
    <h3>{{ displayText }}</h3>
    <button @click="increment" class="btn-primary">
      Increment
    </button>
  </div>
</template>
```

### Pinia Store 사용

```typescript
import { usePostStore } from '@/stores/postStore'

const postStore = usePostStore()

// Actions
await postStore.fetchPosts()
await postStore.likePost(postId)

// Getters
const trendingPosts = postStore.trendingPosts
const isLoading = postStore.loading
```

### API 호출

```typescript
import { blogApi } from '@/api'

// GET
const post = await blogApi.posts.getPost(postId)

// POST
const newPost = await blogApi.posts.createPost(postData)

// PUT
const updatedPost = await blogApi.posts.updatePost(postId, postData)

// DELETE
await blogApi.posts.deletePost(postId)
```

## 참고 문서

- [Vue 3 Documentation](https://vuejs.org/)
- [Vite Documentation](https://vitejs.dev/)
- [Pinia Documentation](https://pinia.vuejs.org/)
- [Tailwind CSS Documentation](https://tailwindcss.com/)
- [Playwright Documentation](https://playwright.dev/)

## 관련 프로젝트

- `portal-shell`: Host 앱
- `design-system-vue`: Vue Design System
- `services/blog-service`: 백엔드 API

## 라이선스

Private
