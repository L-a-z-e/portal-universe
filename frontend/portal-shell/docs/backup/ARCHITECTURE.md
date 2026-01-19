# Portal Shell 아키텍처

## 시스템 구조

```
┌─────────────────────────────────────────────────────────┐
│                    Portal Shell (Host)                   │
│                      localhost:30000                     │
│  ┌───────────────────────────────────────────────────┐  │
│  │                      App.vue                       │  │
│  │  ┌──────────────────────────────────────────────┐ │  │
│  │  │ Header (Logo, Nav, Auth, Theme)              │ │  │
│  │  ├──────────────────────────────────────────────┤ │  │
│  │  │ <router-view>                                │ │  │
│  │  │  ┌────────────────────────────────────────┐  │ │  │
│  │  │  │ HomePage | RemoteWrapper | SignupPage │  │ │  │
│  │  │  └────────────────────────────────────────┘  │ │  │
│  │  ├──────────────────────────────────────────────┤ │  │
│  │  │ Footer                                       │ │  │
│  │  └──────────────────────────────────────────────┘ │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
           │                              │
           ▼                              ▼
    ┌──────────────┐              ┌──────────────┐
    │ Blog Remote  │              │Shopping Remote│
    │ :30001       │              │ :30002       │
    └──────────────┘              └──────────────┘
```

## Remote 모듈 로딩

### RemoteWrapper 컴포넌트

```vue
<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { loadRemote } from '@/services/remoteLoader'

const props = defineProps<{
  remoteName: 'blog' | 'shopping'
}>()

const container = ref<HTMLElement>()
let appInstance: { unmount: () => void } | null = null

onMounted(async () => {
  // 1. Remote 모듈 동적 로드
  const { mountApp } = await loadRemote(props.remoteName)

  // 2. data-service 속성 설정 (테마 적용)
  document.documentElement.setAttribute('data-service', props.remoteName)

  // 3. Remote 앱 마운트
  appInstance = mountApp(container.value!, {
    initialPath: route.fullPath.replace(`/${props.remoteName}`, '') || '/',
    onNavigate: (path) => {
      router.push(`/${props.remoteName}${path}`)
    }
  })
})

onUnmounted(() => {
  appInstance?.unmount()
})
</script>
```

### Remote 레지스트리

```typescript
// config/remoteRegistry.ts
export const remoteRegistry = {
  blog: {
    url: import.meta.env.VITE_BLOG_REMOTE_URL,
    entry: '/assets/remoteEntry.js',
    mount: 'mountBlogApp'
  },
  shopping: {
    url: import.meta.env.VITE_SHOPPING_REMOTE_URL,
    entry: '/assets/remoteEntry.js',
    mount: 'mountShoppingApp'
  }
}
```

## 인증 흐름

### OAuth2 PKCE Flow

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│ Portal Shell │       │ Auth Service │       │   Browser    │
└──────┬───────┘       └──────┬───────┘       └──────┬───────┘
       │                      │                      │
       │  1. Login Click      │                      │
       │ ─────────────────────────────────────────────▶
       │                      │                      │
       │  2. Generate PKCE    │                      │
       │     (code_verifier,  │                      │
       │      code_challenge) │                      │
       │                      │                      │
       │  3. Redirect to Auth │                      │
       │ ────────────────────▶│                      │
       │                      │  4. Login Form       │
       │                      │ ─────────────────────▶
       │                      │                      │
       │                      │  5. Credentials      │
       │                      │ ◀─────────────────────
       │                      │                      │
       │  6. Redirect with    │                      │
       │     authorization_code                      │
       │ ◀────────────────────│                      │
       │                      │                      │
       │  7. Exchange code    │                      │
       │     + code_verifier  │                      │
       │ ────────────────────▶│                      │
       │                      │                      │
       │  8. Access Token     │                      │
       │     + Refresh Token  │                      │
       │ ◀────────────────────│                      │
       │                      │                      │
```

### Auth Store

```typescript
// store/auth.ts
export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(null)
  const refreshToken = ref<string | null>(null)
  const user = ref<User | null>(null)

  const isAuthenticated = computed(() => !!accessToken.value)
  const isAdmin = computed(() => user.value?.roles?.includes('ROLE_ADMIN'))
  const displayName = computed(() => user.value?.name || user.value?.email)

  async function refreshAccessToken() {
    const response = await authApi.refresh(refreshToken.value!)
    accessToken.value = response.accessToken
  }

  function logout() {
    accessToken.value = null
    refreshToken.value = null
    user.value = null
    localStorage.removeItem('auth')
  }

  return { accessToken, user, isAuthenticated, isAdmin, logout }
})
```

## 테마 시스템

### Theme Store

```typescript
// store/theme.ts
export const useThemeStore = defineStore('theme', () => {
  const isDark = ref(false)

  function initialize() {
    // 1. 로컬 스토리지 확인
    const saved = localStorage.getItem('theme')
    if (saved) {
      isDark.value = saved === 'dark'
      return
    }

    // 2. 시스템 설정 확인
    isDark.value = window.matchMedia('(prefers-color-scheme: dark)').matches
  }

  function toggle() {
    isDark.value = !isDark.value
    localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
  }

  return { isDark, initialize, toggle }
})
```

### 서비스별 테마 적용

```vue
<!-- App.vue -->
<script setup>
watch(() => route.path, () => {
  if (!route.meta.remoteName) {
    // Host 페이지: Portal 테마
    document.documentElement.setAttribute('data-service', 'portal')
  }
  // Remote 페이지: RemoteWrapper에서 설정
})
</script>
```

## 라우터 설정

```typescript
// router/index.ts
const routes = [
  {
    path: '/',
    component: HomePage
  },
  {
    path: '/signup',
    component: SignupPage
  },
  {
    path: '/callback',
    component: CallbackPage
  },
  {
    path: '/blog/:pathMatch(.*)*',
    component: RemoteWrapper,
    meta: { remoteName: 'blog' }
  },
  {
    path: '/shopping/:pathMatch(.*)*',
    component: RemoteWrapper,
    meta: { remoteName: 'shopping' }
  },
  {
    path: '/:pathMatch(.*)*',
    component: NotFound
  }
]
```

## API 클라이언트

### Axios 인스턴스

```typescript
// api/apiClient.ts
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL
})

// 요청 인터셉터: JWT 토큰 추가
apiClient.interceptors.request.use((config) => {
  const authStore = useAuthStore()
  if (authStore.accessToken) {
    config.headers.Authorization = `Bearer ${authStore.accessToken}`
  }
  return config
})

// 응답 인터셉터: 401 → 토큰 갱신
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      await authStore.refreshAccessToken()
      return apiClient.request(error.config)
    }
    return Promise.reject(error)
  }
)
```

## KeepAlive 전략

```vue
<!-- App.vue -->
<router-view v-slot="{ Component, route }">
  <KeepAlive :max="3">
    <component
      :is="Component"
      :key="route.meta.remoteName || route.name"
    />
  </KeepAlive>
</router-view>
```

- Remote 모듈은 `remoteName`으로 캐싱
- Host 페이지는 `route.name`으로 캐싱
- 최대 3개 컴포넌트 캐싱 (메모리 관리)
