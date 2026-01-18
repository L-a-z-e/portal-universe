# Blog Frontend 아키텍처

## Dual Mode 지원

```
┌────────────────────────────────────────────────────────┐
│                    Blog Frontend                        │
│  ┌─────────────────────┐  ┌────────────────────────┐  │
│  │    main.ts          │  │    bootstrap.ts         │  │
│  │  (Standalone Mode)  │  │  (Embedded Mode)        │  │
│  │                     │  │                         │  │
│  │  - Browser History  │  │  - Memory History       │  │
│  │  - Direct Access    │  │  - Portal Shell 통합    │  │
│  │  - localhost:30001  │  │  - /blog/* 경로        │  │
│  └─────────┬───────────┘  └────────────┬───────────┘  │
│            │                           │               │
│            └───────────┬───────────────┘               │
│                        ▼                               │
│  ┌─────────────────────────────────────────────────┐  │
│  │                    App.vue                       │  │
│  │  ┌────────────────────────────────────────────┐ │  │
│  │  │              <router-view>                  │ │  │
│  │  │  PostListPage | PostDetailPage | WritePage │ │  │
│  │  └────────────────────────────────────────────┘ │  │
│  └─────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

## mountBlogApp API

```typescript
// bootstrap.ts
export function mountBlogApp(
  el: HTMLElement,
  options: MountOptions = {}
): BlogAppInstance {

  const { initialPath, onNavigate } = options

  // 1. Vue 앱 생성
  const app = createApp(App)
  app.use(createPinia())

  // 2. Memory History 라우터 생성
  const router = createBlogRouter('/')
  app.use(router)

  // 3. 초기 경로 설정
  router.push(initialPath || '/')

  // 4. 경로 변경 시 Parent 알림
  router.afterEach((to, from) => {
    if (to.path !== from.path) {
      onNavigate?.(to.path)
    }
  })

  // 5. 마운트
  app.mount(el)

  // 6. 인스턴스 반환
  return {
    router,
    onParentNavigate: (path) => router.push(path),
    unmount: () => {
      app.unmount()
      cleanupCSS()
    }
  }
}
```

## 라우터 설정

```typescript
// router/index.ts
export function createBlogRouter(base: string) {
  return createRouter({
    history: createMemoryHistory(base),
    routes: [
      {
        path: '/',
        component: () => import('@/views/PostListPage.vue')
      },
      {
        path: '/:id',
        component: () => import('@/views/PostDetailPage.vue')
      },
      {
        path: '/write',
        component: () => import('@/views/PostWritePage.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: '/:id/edit',
        component: () => import('@/views/PostEditPage.vue'),
        meta: { requiresAuth: true }
      }
    ]
  })
}
```

## API 통신

### API 클라이언트

```typescript
// api/index.ts
import axios from 'axios'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL + '/api/v1/blog'
})

// Portal Shell의 인증 토큰 사용
apiClient.interceptors.request.use((config) => {
  // Portal Shell에서 주입된 토큰 사용
  const token = window.__PORTAL_AUTH_TOKEN__
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export default apiClient
```

### Posts API

```typescript
// api/posts.ts
export const postsApi = {
  getList: (page: number, size: number) =>
    apiClient.get('/posts', { params: { page, size } }),

  getById: (id: string) =>
    apiClient.get(`/posts/${id}`),

  getWithView: (id: string) =>
    apiClient.get(`/posts/${id}/view`),

  create: (data: PostCreateRequest) =>
    apiClient.post('/posts', data),

  update: (id: string, data: PostUpdateRequest) =>
    apiClient.put(`/posts/${id}`, data),

  delete: (id: string) =>
    apiClient.delete(`/posts/${id}`),

  search: (keyword: string, page: number, size: number) =>
    apiClient.get('/posts/search', { params: { keyword, page, size } })
}
```

## 페이지 컴포넌트

### PostListPage

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { postsApi } from '@/api/posts'
import PostCard from '@/components/PostCard.vue'

const posts = ref<PostSummary[]>([])
const page = ref(0)
const totalPages = ref(0)

onMounted(async () => {
  const response = await postsApi.getList(page.value, 10)
  posts.value = response.data.data.content
  totalPages.value = response.data.data.totalPages
})
</script>

<template>
  <div class="post-list">
    <PostCard
      v-for="post in posts"
      :key="post.id"
      :post="post"
    />
    <Pagination
      :current="page"
      :total="totalPages"
      @change="handlePageChange"
    />
  </div>
</template>
```

### PostDetailPage

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { postsApi } from '@/api/posts'

const route = useRoute()
const post = ref<PostResponse | null>(null)

onMounted(async () => {
  const id = route.params.id as string
  const response = await postsApi.getWithView(id)
  post.value = response.data.data
})
</script>
```

## CSS 정리 (Unmount)

```typescript
unmount: () => {
  app.unmount()
  el.innerHTML = ''

  // Blog CSS 제거
  document.querySelectorAll('style').forEach((style) => {
    if (style.textContent?.includes('[data-service="blog"]')) {
      style.remove()
    }
  })

  // data-service 초기화
  document.documentElement.removeAttribute('data-service')
}
```

## 테마 적용

```css
/* data-service="blog" 일 때 적용 */
[data-service="blog"] {
  --semantic-brand-primary: #20C997;
  --semantic-brand-primaryHover: #12B886;

  /* 블로그 최적화 */
  --font-size-body: 1.125rem;
  --line-height-body: 1.75;
}
```
