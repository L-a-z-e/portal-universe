// portal-shell/src/services/notificationService.ts

import apiClient from '../api/apiClient'
import type { Notification, NotificationPage } from '../types/notification'

const BASE_PATH = '/notification/api/v1/notifications'

/**
 * Get paginated notifications for current user
 */
export async function getNotifications(
  page = 1,
  size = 20
): Promise<NotificationPage> {
  const response = await apiClient.get<{ data: NotificationPage }>(BASE_PATH, {
    params: { page, size }
  })
  return response.data.data
}

/**
 * Get unread notifications only
 */
export async function getUnreadNotifications(
  page = 1,
  size = 20
): Promise<NotificationPage> {
  const response = await apiClient.get<{ data: NotificationPage }>(
    `${BASE_PATH}/unread`,
    { params: { page, size } }
  )
  return response.data.data
}

/**
 * Get unread notification count
 */
export async function getUnreadCount(): Promise<number> {
  const response = await apiClient.get<{ data: number }>(
    `${BASE_PATH}/unread/count`
  )
  return response.data.data
}

/**
 * Mark a single notification as read
 */
export async function markAsRead(notificationId: number): Promise<Notification> {
  const response = await apiClient.put<{ data: Notification }>(
    `${BASE_PATH}/${notificationId}/read`
  )
  return response.data.data
}

/**
 * Mark all notifications as read
 */
export async function markAllAsRead(): Promise<number> {
  const response = await apiClient.put<{ data: number }>(
    `${BASE_PATH}/read-all`
  )
  return response.data.data
}

/**
 * Delete a notification
 */
export async function deleteNotification(notificationId: number): Promise<void> {
  await apiClient.delete(`${BASE_PATH}/${notificationId}`)
}

export const notificationService = {
  getNotifications,
  getUnreadNotifications,
  getUnreadCount,
  markAsRead,
  markAllAsRead,
  deleteNotification
}
