// portal-shell/src/types/dashboard.ts

// ============================================
// Blog Stats Types
// ============================================

/**
 * 블로그 작성자 통계 (Blog Service 응답)
 */
export interface AuthorStats {
  authorId: string
  authorUsername: string
  authorNickname: string
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
  recentOrderCount: number
}

// ============================================
// Activity Types
// ============================================

/**
 * 활동 타입
 */
export type ActivityType =
  | 'POST_CREATED'
  | 'COMMENT_CREATED'
  | 'POST_LIKED'
  | 'ORDER_CREATED'
  | 'ORDER_COMPLETED'
  | 'PAYMENT_COMPLETED'

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
  change?: string
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
