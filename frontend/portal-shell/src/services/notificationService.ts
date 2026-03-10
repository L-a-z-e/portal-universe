// portal-shell/src/services/notificationService.ts

import apiClient from '../api/apiClient'
import type { Notification, NotificationPage } from '../types/notification'

const BASE_PATH = '/notification/api/v1/notifications'

export async function getNotifications(
  page = 1,
  size = 20
): Promise<NotificationPage> {
  const response = await apiClient.get<{ data: NotificationPage }>(BASE_PATH, {
    params: { page: page - 1, size }
  })
  return response.data.data
}

export async function getUnreadNotifications(
  page = 1,
  size = 20
): Promise<NotificationPage> {
  const response = await apiClient.get<{ data: NotificationPage }>(
    `${BASE_PATH}/unread`,
    { params: { page: page - 1, size } }
  )
  return response.data.data
}

export async function getUnreadCount(): Promise<number> {
  const response = await apiClient.get<{ data: number }>(
    `${BASE_PATH}/unread/count`
  )
  return response.data.data
}

export async function markAsRead(notificationId: number): Promise<Notification> {
  const response = await apiClient.put<{ data: Notification }>(
    `${BASE_PATH}/${notificationId}/read`
  )
  return response.data.data
}

export async function markAllAsRead(): Promise<number> {
  const response = await apiClient.put<{ data: number }>(
    `${BASE_PATH}/read-all`
  )
  return response.data.data
}

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
