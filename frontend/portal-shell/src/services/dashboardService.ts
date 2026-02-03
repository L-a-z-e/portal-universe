// portal-shell/src/services/dashboardService.ts

import apiClient from '../api/apiClient'
import type { AuthorStats, OrderStats, ActivityItem, ActivityType } from '../types/dashboard'

// ============================================
// Blog Service API
// ============================================

const BLOG_BASE = '/api/v1/blog/posts'

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

const SHOPPING_BASE = '/api/v1/shopping'

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
 * size=1로 호출하여 totalElements 가져옴
 */
export async function getOrderStats(): Promise<OrderStats> {
  const response = await apiClient.get<{ data: PageResponse<OrderResponse> }>(
    `${SHOPPING_BASE}/orders`,
    { params: { page: 0, size: 1 } }
  )

  const page = response.data.data
  return {
    totalOrders: page.totalElements,
    recentOrderCount: page.totalElements
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
function mapNotificationType(type: string): ActivityType {
  const typeMap: Record<string, ActivityType> = {
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
function getActivityIcon(type: ActivityType): string {
  const iconMap: Record<ActivityType, string> = {
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
