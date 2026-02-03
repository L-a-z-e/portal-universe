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
    if (!authStore.isAuthenticated || !authStore.user?.profile?.sub) {
      console.warn('[useDashboard] Not authenticated')
      loading.value = { blogStats: false, orderStats: false, activities: false }
      return
    }

    const userId = authStore.user.profile.sub

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
    if (!authStore.user?.profile?.sub) return

    loading.value.blogStats = true
    errors.value.blogStats = null

    try {
      blogStats.value = await dashboardService.getBlogStats(authStore.user.profile.sub)
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
