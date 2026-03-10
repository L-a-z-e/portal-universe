// portal-shell/src/services/dashboardService.ts

import apiClient from '../api/apiClient'
import type { AuthorStats, OrderStats, ActivityItem, ActivityType } from '../types/dashboard'
import { parseDate } from '../utils/dateUtils'

const BLOG_BASE = '/api/v1/blog/posts'

export async function getBlogStats(authorId: string): Promise<AuthorStats> {
  const response = await apiClient.get<{ data: AuthorStats }>(
    `${BLOG_BASE}/stats/author/${authorId}`
  )
  return response.data.data
}

const SHOPPING_BASE = '/api/v1/shopping'

interface PageResponse<T> {
  items: T[]
  totalElements: number
  totalPages: number
  size: number
  page: number
}

interface OrderResponse {
  orderNumber: string
  status: string
  totalAmount: number
  createdAt: string
}

// size=1로 호출하여 totalElements만 가져옴
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

const NOTIFICATION_BASE = '/notification/api/v1/notifications'

interface NotificationResponse {
  id: number
  type: string
  title: string
  message: string
  link: string | null
  status: string
  createdAt: string | number[]
}

function mapNotificationType(type: string): ActivityType {
  const typeMap: Record<string, ActivityType> = {
    'BLOG_COMMENT': 'COMMENT_CREATED',
    'BLOG_REPLY': 'COMMENT_CREATED',
    'BLOG_LIKE': 'POST_LIKED',
    'BLOG_FOLLOW': 'POST_CREATED',
    'BLOG_NEW_POST': 'POST_CREATED',
    'ORDER_CREATED': 'ORDER_CREATED',
    'ORDER_CONFIRMED': 'ORDER_COMPLETED',
    'PAYMENT_COMPLETED': 'PAYMENT_COMPLETED'
  }
  return typeMap[type] || 'POST_CREATED'
}

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

export async function getRecentActivities(limit = 5): Promise<ActivityItem[]> {
  const response = await apiClient.get<{ data: { items: NotificationResponse[] } }>(
    NOTIFICATION_BASE,
    { params: { page: 0, size: limit } }
  )

  const notifications = response.data.data.items

  return notifications.map((notification): ActivityItem => {
    const type = mapNotificationType(notification.type)
    const parsed = parseDate(notification.createdAt)
    return {
      id: String(notification.id),
      type,
      title: notification.title,
      description: notification.message,
      timestamp: parsed?.toISOString() ?? '',
      icon: getActivityIcon(type),
      link: notification.link ?? undefined
    }
  })
}

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

// 병렬 호출로 성능 최적화, 부분 실패 허용
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
