// portal-shell/src/types/notification.ts

export interface Notification {
  id: number
  userId: number
  type: NotificationType
  title: string
  message: string
  link: string | null
  status: NotificationStatus
  referenceId: string | null
  referenceType: string | null
  createdAt: string | number[]
  readAt: string | number[] | null
}

export type NotificationStatus = 'UNREAD' | 'READ'

export type NotificationType =
  // Shopping
  | 'ORDER_CREATED'
  | 'ORDER_CONFIRMED'
  | 'ORDER_CANCELLED'
  | 'DELIVERY_STARTED'
  | 'DELIVERY_IN_TRANSIT'
  | 'DELIVERY_COMPLETED'
  | 'PAYMENT_COMPLETED'
  | 'PAYMENT_FAILED'
  | 'REFUND_COMPLETED'
  | 'COUPON_ISSUED'
  | 'COUPON_EXPIRING'
  | 'TIMEDEAL_STARTING'
  | 'TIMEDEAL_STARTED'
  // Blog
  | 'BLOG_LIKE'
  | 'BLOG_COMMENT'
  | 'BLOG_REPLY'
  | 'BLOG_FOLLOW'
  | 'BLOG_NEW_POST'
  // Prism
  | 'PRISM_TASK_COMPLETED'
  | 'PRISM_TASK_FAILED'
  // System
  | 'SYSTEM'

export interface NotificationPage {
  items: Notification[]
  totalElements: number
  totalPages: number
  size: number
  page: number
}
