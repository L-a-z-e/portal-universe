# Design: Dashboard Page Refactoring

> Feature: `dashboardpage-refactoring`
> Created: 2026-02-03
> Plan Reference: `docs/pdca/01-plan/features/dashboardpage-refactoring.plan.md`

## 1. 개요

Dashboard 페이지의 mock 데이터를 실제 마이크로서비스 API 데이터로 교체하는 상세 설계.

## 2. 아키텍처

### 2.1 컴포넌트 구조

```
┌─────────────────────────────────────────────────────────┐
│                    DashboardPage.vue                     │
│  ┌─────────────────────────────────────────────────┐    │
│  │              useDashboard() composable           │    │
│  │  ┌─────────────┬─────────────┬──────────────┐   │    │
│  │  │ blogStats   │ orderStats  │ activities   │   │    │
│  │  │ (loading)   │ (loading)   │ (loading)    │   │    │
│  │  └─────────────┴─────────────┴──────────────┘   │    │
│  └─────────────────────────────────────────────────┘    │
│                           │                              │
│                           ▼                              │
│  ┌─────────────────────────────────────────────────┐    │
│  │              dashboardService.ts                 │    │
│  │  ┌─────────────┬─────────────┬──────────────┐   │    │
│  │  │ getBlogStats│ getOrderCnt │ getActivities│   │    │
│  │  └─────────────┴─────────────┴──────────────┘   │    │
│  └─────────────────────────────────────────────────┘    │
│                           │                              │
│                           ▼                              │
│  ┌─────────────────────────────────────────────────┐    │
│  │                  apiClient.ts                    │    │
│  │         (인증 헤더 자동 주입, 토큰 갱신)          │    │
│  └─────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                     API Gateway                          │
│  /api/blog/* → blog-service:8082                        │
│  /api/shopping/* → shopping-service:8083                │
│  /api/notification/* → notification-service:8084        │
└─────────────────────────────────────────────────────────┘
```

### 2.2 데이터 흐름

```
User Login
    │
    ▼
DashboardPage mounted
    │
    ▼
useDashboard().fetchAll()
    │
    ├──────────────────────────────────┐
    │                                  │
    ▼                                  ▼
Promise.allSettled([           각 섹션 독립적 로딩
  getBlogStats(),              (부분 실패 허용)
  getOrderCount(),
  getRecentActivities()
])
    │
    ▼
각 결과를 reactive state에 저장
    │
    ▼
UI 업데이트 (스켈레톤 → 실제 데이터)
```

## 3. 타입 정의

### 3.1 `types/dashboard.ts`

```typescript
// ============================================
// Blog Stats Types
// ============================================

/**
 * 블로그 작성자 통계 (Blog Service 응답)
 */
export interface AuthorStats {
  authorId: string
  authorName: string
  totalPosts: number
  publishedPosts: number
  totalViews: number
  totalLikes: number
  firstPostDate: string | null
  lastPostDate: string | null
}

// ============================================
// Order Stats Types
// ============================================

/**
 * 주문 통계 (집계된 데이터)
 */
export interface OrderStats {
  totalOrders: number
  recentOrderCount: number  // 최근 30일
}

// ============================================
// Activity Types
// ============================================

/**
 * 활동 타입 enum
 */
export type ActivityType =
  | 'POST_CREATED'      // 글 작성
  | 'COMMENT_CREATED'   // 댓글 작성
  | 'POST_LIKED'        // 좋아요 받음
  | 'ORDER_CREATED'     // 주문 생성
  | 'ORDER_COMPLETED'   // 주문 완료
  | 'PAYMENT_COMPLETED' // 결제 완료

/**
 * 최근 활동 아이템
 */
export interface ActivityItem {
  id: string
  type: ActivityType
  title: string
  description: string
  timestamp: string
  icon: string
  link?: string
}

// ============================================
// Dashboard State Types
// ============================================

/**
 * 개별 데이터 상태
 */
export interface DataState<T> {
  data: T | null
  loading: boolean
  error: string | null
}

/**
 * Dashboard 통계 카드 아이템
 */
export interface StatItem {
  label: string
  value: number
  icon: string
  change?: string  // "+3", "-2" 등 (선택적)
  loading: boolean
  error: string | null
}

/**
 * Dashboard 전체 상태
 */
export interface DashboardState {
  blogStats: DataState<AuthorStats>
  orderStats: DataState<OrderStats>
  activities: DataState<ActivityItem[]>
  lastFetchedAt: string | null
}
```

## 4. Service 레이어

### 4.1 `services/dashboardService.ts`

```typescript
// portal-shell/src/services/dashboardService.ts

import apiClient from '../api/apiClient'
import type { AuthorStats, OrderStats, ActivityItem } from '../types/dashboard'

// ============================================
// Blog Service API
// ============================================

const BLOG_BASE = '/blog/api/v1/posts'

/**
 * 블로그 작성자 통계 조회
 * @param authorId 작성자 ID (userId)
 */
export async function getBlogStats(authorId: string): Promise<AuthorStats> {
  const response = await apiClient.get<{ data: AuthorStats }>(
    `${BLOG_BASE}/stats/author/${authorId}`
  )
  return response.data.data
}

// ============================================
// Shopping Service API
// ============================================

const SHOPPING_BASE = '/shopping/api/v1'

interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

interface OrderResponse {
  orderNumber: string
  status: string
  totalAmount: number
  createdAt: string
}

/**
 * 주문 통계 조회
 * size=0으로 호출하여 totalElements만 가져옴
 */
export async function getOrderStats(): Promise<OrderStats> {
  const response = await apiClient.get<{ data: PageResponse<OrderResponse> }>(
    `${SHOPPING_BASE}/orders`,
    { params: { page: 0, size: 1 } }
  )

  const page = response.data.data
  return {
    totalOrders: page.totalElements,
    recentOrderCount: page.totalElements  // TODO: 서버에서 최근 30일 필터 지원 시 수정
  }
}

// ============================================
// Notification Service API (활동 피드로 활용)
// ============================================

const NOTIFICATION_BASE = '/notification/api/v1/notifications'

interface NotificationResponse {
  id: number
  type: string
  title: string
  content: string
  createdAt: string
  read: boolean
  metadata?: Record<string, unknown>
}

/**
 * 알림 타입을 ActivityType으로 매핑
 */
function mapNotificationType(type: string): ActivityItem['type'] {
  const typeMap: Record<string, ActivityItem['type']> = {
    'COMMENT': 'COMMENT_CREATED',
    'COMMENT_REPLY': 'COMMENT_CREATED',
    'LIKE': 'POST_LIKED',
    'ORDER_CREATED': 'ORDER_CREATED',
    'ORDER_CONFIRMED': 'ORDER_COMPLETED',
    'PAYMENT_COMPLETED': 'PAYMENT_COMPLETED'
  }
  return typeMap[type] || 'POST_CREATED'
}

/**
 * 활동 타입별 아이콘
 */
function getActivityIcon(type: ActivityItem['type']): string {
  const iconMap: Record<ActivityItem['type'], string> = {
    'POST_CREATED': '📝',
    'COMMENT_CREATED': '💬',
    'POST_LIKED': '❤️',
    'ORDER_CREATED': '🛒',
    'ORDER_COMPLETED': '📦',
    'PAYMENT_COMPLETED': '💳'
  }
  return iconMap[type] || '📌'
}

/**
 * 최근 활동 조회 (알림 기반)
 * @param limit 조회할 활동 수
 */
export async function getRecentActivities(limit = 5): Promise<ActivityItem[]> {
  const response = await apiClient.get<{ data: { content: NotificationResponse[] } }>(
    NOTIFICATION_BASE,
    { params: { page: 0, size: limit } }
  )

  const notifications = response.data.data.content

  return notifications.map((notification): ActivityItem => {
    const type = mapNotificationType(notification.type)
    return {
      id: String(notification.id),
      type,
      title: notification.title,
      description: notification.content,
      timestamp: notification.createdAt,
      icon: getActivityIcon(type)
    }
  })
}

// ============================================
// Aggregated API
// ============================================

export interface DashboardData {
  blogStats: AuthorStats | null
  orderStats: OrderStats | null
  activities: ActivityItem[]
  errors: {
    blogStats?: string
    orderStats?: string
    activities?: string
  }
}

/**
 * Dashboard 데이터 일괄 조회
 * 병렬 호출로 성능 최적화, 부분 실패 허용
 */
export async function fetchDashboardData(userId: string): Promise<DashboardData> {
  const [blogResult, orderResult, activitiesResult] = await Promise.allSettled([
    getBlogStats(userId),
    getOrderStats(),
    getRecentActivities(5)
  ])

  const result: DashboardData = {
    blogStats: null,
    orderStats: null,
    activities: [],
    errors: {}
  }

  if (blogResult.status === 'fulfilled') {
    result.blogStats = blogResult.value
  } else {
    result.errors.blogStats = blogResult.reason?.message || 'Failed to load blog stats'
  }

  if (orderResult.status === 'fulfilled') {
    result.orderStats = orderResult.value
  } else {
    result.errors.orderStats = orderResult.reason?.message || 'Failed to load order stats'
  }

  if (activitiesResult.status === 'fulfilled') {
    result.activities = activitiesResult.value
  } else {
    result.errors.activities = activitiesResult.reason?.message || 'Failed to load activities'
  }

  return result
}

export const dashboardService = {
  getBlogStats,
  getOrderStats,
  getRecentActivities,
  fetchDashboardData
}
```

## 5. Composable 레이어

### 5.1 `composables/useDashboard.ts`

```typescript
// portal-shell/src/composables/useDashboard.ts

import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '../store/auth'
import { dashboardService } from '../services/dashboardService'
import type {
  AuthorStats,
  OrderStats,
  ActivityItem,
  StatItem
} from '../types/dashboard'

export function useDashboard() {
  const authStore = useAuthStore()

  // ============================================
  // State
  // ============================================

  const blogStats = ref<AuthorStats | null>(null)
  const orderStats = ref<OrderStats | null>(null)
  const activities = ref<ActivityItem[]>([])

  const loading = ref({
    blogStats: true,
    orderStats: true,
    activities: true
  })

  const errors = ref({
    blogStats: null as string | null,
    orderStats: null as string | null,
    activities: null as string | null
  })

  const lastFetchedAt = ref<Date | null>(null)

  // ============================================
  // Computed
  // ============================================

  /**
   * Stats 카드 데이터
   */
  const stats = computed<StatItem[]>(() => [
    {
      label: '작성한 글',
      value: blogStats.value?.totalPosts ?? 0,
      icon: '📄',
      loading: loading.value.blogStats,
      error: errors.value.blogStats
    },
    {
      label: '주문 건수',
      value: orderStats.value?.totalOrders ?? 0,
      icon: '📦',
      loading: loading.value.orderStats,
      error: errors.value.orderStats
    },
    {
      label: '받은 좋아요',
      value: blogStats.value?.totalLikes ?? 0,
      icon: '❤️',
      loading: loading.value.blogStats,
      error: errors.value.blogStats
    }
  ])

  /**
   * 전체 로딩 상태
   */
  const isLoading = computed(() =>
    loading.value.blogStats ||
    loading.value.orderStats ||
    loading.value.activities
  )

  /**
   * 에러 존재 여부
   */
  const hasErrors = computed(() =>
    !!errors.value.blogStats ||
    !!errors.value.orderStats ||
    !!errors.value.activities
  )

  // ============================================
  // Methods
  // ============================================

  /**
   * 모든 데이터 가져오기
   */
  async function fetchAll() {
    if (!authStore.isAuthenticated || !authStore.user?.id) {
      console.warn('[useDashboard] Not authenticated')
      return
    }

    const userId = authStore.user.id

    // Reset states
    loading.value = { blogStats: true, orderStats: true, activities: true }
    errors.value = { blogStats: null, orderStats: null, activities: null }

    try {
      const data = await dashboardService.fetchDashboardData(userId)

      blogStats.value = data.blogStats
      orderStats.value = data.orderStats
      activities.value = data.activities

      if (data.errors.blogStats) errors.value.blogStats = data.errors.blogStats
      if (data.errors.orderStats) errors.value.orderStats = data.errors.orderStats
      if (data.errors.activities) errors.value.activities = data.errors.activities

      lastFetchedAt.value = new Date()
    } finally {
      loading.value = { blogStats: false, orderStats: false, activities: false }
    }
  }

  /**
   * 블로그 통계만 새로고침
   */
  async function refreshBlogStats() {
    if (!authStore.user?.id) return

    loading.value.blogStats = true
    errors.value.blogStats = null

    try {
      blogStats.value = await dashboardService.getBlogStats(authStore.user.id)
    } catch (e) {
      errors.value.blogStats = (e as Error).message
    } finally {
      loading.value.blogStats = false
    }
  }

  /**
   * 주문 통계만 새로고침
   */
  async function refreshOrderStats() {
    loading.value.orderStats = true
    errors.value.orderStats = null

    try {
      orderStats.value = await dashboardService.getOrderStats()
    } catch (e) {
      errors.value.orderStats = (e as Error).message
    } finally {
      loading.value.orderStats = false
    }
  }

  /**
   * 활동 목록만 새로고침
   */
  async function refreshActivities() {
    loading.value.activities = true
    errors.value.activities = null

    try {
      activities.value = await dashboardService.getRecentActivities(5)
    } catch (e) {
      errors.value.activities = (e as Error).message
    } finally {
      loading.value.activities = false
    }
  }

  // ============================================
  // Lifecycle
  // ============================================

  onMounted(() => {
    fetchAll()
  })

  // ============================================
  // Return
  // ============================================

  return {
    // State
    blogStats,
    orderStats,
    activities,
    loading,
    errors,
    lastFetchedAt,

    // Computed
    stats,
    isLoading,
    hasErrors,

    // Methods
    fetchAll,
    refreshBlogStats,
    refreshOrderStats,
    refreshActivities
  }
}
```

## 6. View 컴포넌트 수정

### 6.1 `views/DashboardPage.vue` 수정 사항

```vue
<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { useDashboard } from '../composables/useDashboard'
import { Button, Badge } from '@portal/design-system-vue'
import { getRemoteConfigs } from '../config/remoteRegistry'
import { formatRelativeTime } from '../utils/dateUtils'  // 새로 추가 필요

const router = useRouter()
const authStore = useAuthStore()

// Dashboard composable 사용
const {
  stats,
  activities,
  loading,
  errors,
  isLoading,
  fetchAll
} = useDashboard()

// 기존 services computed 유지
const services = computed(() => {
  const configs = getRemoteConfigs()
  return configs.map(config => ({
    id: config.key,
    name: config.name,
    icon: config.icon || '📦',
    description: config.description || '',
    path: config.basePath,
    isActive: true
  }))
})

// Quick actions 유지
const quickActions = [
  { id: 'new-post', label: '새 글 작성', icon: '✏️', path: '/blog/write', shortcut: 'N' },
  { id: 'browse-products', label: '상품 둘러보기', icon: '🛍️', path: '/shopping', shortcut: 'S' },
  { id: 'my-orders', label: '주문 내역', icon: '📦', path: '/shopping/orders', shortcut: 'O' },
]

// Greeting 유지
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '좋은 아침이에요'
  if (hour < 18) return '좋은 오후에요'
  return '좋은 저녁이에요'
})

function navigateTo(path: string) {
  router.push(path)
}
</script>

<template>
  <div class="bg-bg-page text-text-body">
    <!-- Header 유지 -->

    <main class="max-w-7xl mx-auto px-4 py-8">
      <!-- Stats Overview: 실제 데이터 사용 -->
      <section class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
        <div
          v-for="stat in stats"
          :key="stat.label"
          class="bg-bg-card border border-border-default rounded-xl p-5
                 hover:border-brand-primary/30 transition-colors"
        >
          <!-- 로딩 상태 -->
          <template v-if="stat.loading">
            <div class="animate-pulse">
              <div class="h-8 w-8 bg-bg-elevated rounded mb-2"></div>
              <div class="h-8 w-16 bg-bg-elevated rounded mb-1"></div>
              <div class="h-4 w-20 bg-bg-elevated rounded"></div>
            </div>
          </template>

          <!-- 에러 상태 -->
          <template v-else-if="stat.error">
            <div class="flex items-center justify-between mb-2">
              <span class="text-2xl">{{ stat.icon }}</span>
              <Badge variant="danger" size="sm">에러</Badge>
            </div>
            <p class="text-lg text-text-meta">--</p>
            <p class="text-sm text-text-meta">{{ stat.label }}</p>
          </template>

          <!-- 정상 상태 -->
          <template v-else>
            <div class="flex items-center justify-between mb-2">
              <span class="text-2xl">{{ stat.icon }}</span>
              <Badge v-if="stat.change" variant="success" size="sm">
                {{ stat.change }}
              </Badge>
            </div>
            <p class="text-2xl font-bold text-text-heading">{{ stat.value }}</p>
            <p class="text-sm text-text-meta">{{ stat.label }}</p>
          </template>
        </div>
      </section>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Quick Actions 유지 -->

        <!-- Recent Activity: 실제 데이터 사용 -->
        <section class="lg:col-span-2">
          <div class="bg-bg-card border border-border-default rounded-xl p-5">
            <div class="flex items-center justify-between mb-4">
              <h2 class="text-lg font-semibold text-text-heading flex items-center gap-2">
                <span>📊</span>
                최근 활동
              </h2>
              <Button variant="ghost" size="sm" @click="fetchAll">새로고침</Button>
            </div>

            <!-- 로딩 상태 -->
            <div v-if="loading.activities" class="space-y-3">
              <div v-for="i in 3" :key="i" class="animate-pulse flex items-center gap-4 p-3">
                <div class="w-10 h-10 bg-bg-elevated rounded-full"></div>
                <div class="flex-1">
                  <div class="h-4 w-3/4 bg-bg-elevated rounded mb-2"></div>
                  <div class="h-3 w-1/2 bg-bg-elevated rounded"></div>
                </div>
              </div>
            </div>

            <!-- 에러 상태 -->
            <div v-else-if="errors.activities" class="text-center py-8 text-text-meta">
              <p>활동을 불러올 수 없습니다</p>
              <Button variant="ghost" size="sm" class="mt-2" @click="fetchAll">
                다시 시도
              </Button>
            </div>

            <!-- 빈 상태 -->
            <div v-else-if="activities.length === 0" class="text-center py-8 text-text-meta">
              <p>아직 활동이 없습니다</p>
              <p class="text-sm mt-1">글을 작성하거나 상품을 주문해보세요!</p>
            </div>

            <!-- 정상 상태 -->
            <div v-else class="space-y-3">
              <div
                v-for="activity in activities"
                :key="activity.id"
                class="flex items-center gap-4 p-3 rounded-lg
                       hover:bg-bg-elevated transition-colors cursor-pointer"
                @click="activity.link && navigateTo(activity.link)"
              >
                <div class="w-10 h-10 rounded-full bg-brand-primary/10
                            flex items-center justify-center text-lg">
                  {{ activity.icon }}
                </div>
                <div class="flex-1 min-w-0">
                  <p class="text-text-heading font-medium truncate">
                    {{ activity.title }}
                  </p>
                  <p class="text-sm text-text-meta">{{ activity.description }}</p>
                </div>
                <span class="text-xs text-text-meta whitespace-nowrap">
                  {{ formatRelativeTime(activity.timestamp) }}
                </span>
              </div>
            </div>
          </div>
        </section>
      </div>

      <!-- Services Grid, Keyboard Hint 유지 -->
    </main>
  </div>
</template>
```

## 7. 유틸리티 함수

### 7.1 `utils/dateUtils.ts` (신규 또는 기존 확장)

```typescript
/**
 * 상대적 시간 표시 (예: "2시간 전", "3일 전")
 */
export function formatRelativeTime(dateString: string): string {
  const date = new Date(dateString)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()

  const seconds = Math.floor(diffMs / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (days > 7) {
    return date.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })
  }
  if (days > 0) return `${days}일 전`
  if (hours > 0) return `${hours}시간 전`
  if (minutes > 0) return `${minutes}분 전`
  return '방금 전'
}
```

## 8. 구현 순서

### Phase 1: 기반 구축
1. [ ] `types/dashboard.ts` 생성
2. [ ] `services/dashboardService.ts` 생성
3. [ ] `composables/useDashboard.ts` 생성
4. [ ] `utils/dateUtils.ts` 업데이트 (formatRelativeTime)

### Phase 2: DashboardPage 수정
5. [ ] `DashboardPage.vue`에서 mock 데이터 제거
6. [ ] `useDashboard` composable 연동
7. [ ] Stats 섹션 실제 데이터 표시

### Phase 3: UX 개선
8. [ ] 스켈레톤 로딩 UI 적용
9. [ ] 에러 상태 UI 적용
10. [ ] Empty 상태 UI 적용

### Phase 4: 검증
11. [ ] Playwright로 실제 동작 확인
12. [ ] 에러 시나리오 테스트

## 9. API 엔드포인트 요약

| 용도 | Method | Endpoint | 비고 |
|------|--------|----------|------|
| 블로그 통계 | GET | `/blog/api/v1/posts/stats/author/{userId}` | userId = authStore.user.id |
| 주문 통계 | GET | `/shopping/api/v1/orders?page=0&size=1` | totalElements 사용 |
| 최근 활동 | GET | `/notification/api/v1/notifications?page=0&size=5` | 알림을 활동으로 변환 |

## 10. 에러 처리 전략

| 에러 유형 | 처리 방법 |
|----------|----------|
| 네트워크 오류 | 해당 섹션에 "불러올 수 없음" + 재시도 버튼 |
| 401 Unauthorized | apiClient에서 토큰 갱신 자동 처리 |
| 404 Not Found | 해당 섹션에 "데이터 없음" 표시 |
| 500 Server Error | 해당 섹션에 에러 표시 + 전체 새로고침 버튼 |

## 11. 성능 고려사항

1. **병렬 API 호출**: `Promise.allSettled`로 3개 API 동시 호출
2. **부분 실패 허용**: 한 API 실패해도 다른 섹션 정상 표시
3. **스켈레톤 UI**: 로딩 중에도 레이아웃 유지로 CLS 방지
4. **캐싱**: 필요시 localStorage 또는 Pinia persist 고려

## 12. 테스트 시나리오

| 시나리오 | 예상 결과 |
|----------|----------|
| 정상 로드 | 3개 stats + 5개 활동 표시 |
| 블로그 API 실패 | 작성한 글/좋아요 섹션 에러, 주문/활동 정상 |
| 미인증 상태 | 데이터 로드 안 함 (빈 상태) |
| 활동 0개 | Empty 상태 메시지 표시 |
