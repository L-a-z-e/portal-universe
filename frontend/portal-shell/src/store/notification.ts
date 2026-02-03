// portal-shell/src/store/notification.ts

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Notification } from '../types/notification'
import { notificationService } from '../services/notificationService'

export const useNotificationStore = defineStore('notification', () => {
  // ==================== State ====================
  const notifications = ref<Notification[]>([])
  const unreadCount = ref(0)
  const isLoading = ref(false)
  const isDropdownOpen = ref(false)
  const hasMore = ref(true)
  const currentPage = ref(0)

  // ==================== Getters ====================
  const hasUnread = computed(() => unreadCount.value > 0)

  const unreadNotifications = computed(() =>
    notifications.value.filter((n) => n.status === 'UNREAD')
  )

  // ==================== Actions ====================

  /**
   * Fetch notifications (with pagination)
   */
  async function fetchNotifications(page = 0, reset = false) {
    if (isLoading.value) return
    isLoading.value = true

    try {
      const response = await notificationService.getNotifications(page, 20)

      if (reset) {
        notifications.value = response.content
      } else {
        notifications.value = [...notifications.value, ...response.content]
      }

      currentPage.value = response.number
      hasMore.value = !response.last
    } catch (error) {
      console.error('[NotificationStore] Failed to fetch notifications:', error)
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Fetch unread count
   */
  async function fetchUnreadCount() {
    try {
      unreadCount.value = await notificationService.getUnreadCount()
    } catch (error) {
      console.error('[NotificationStore] Failed to fetch unread count:', error)
    }
  }

  /**
   * Mark single notification as read
   */
  async function markAsRead(id: number) {
    try {
      await notificationService.markAsRead(id)

      const notification = notifications.value.find((n) => n.id === id)
      if (notification && notification.status === 'UNREAD') {
        notification.status = 'READ'
        notification.readAt = new Date().toISOString()
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
    } catch (error) {
      console.error('[NotificationStore] Failed to mark as read:', error)
    }
  }

  /**
   * Mark all notifications as read
   */
  async function markAllAsRead() {
    try {
      await notificationService.markAllAsRead()

      notifications.value.forEach((n) => {
        if (n.status === 'UNREAD') {
          n.status = 'READ'
          n.readAt = new Date().toISOString()
        }
      })
      unreadCount.value = 0
    } catch (error) {
      console.error('[NotificationStore] Failed to mark all as read:', error)
    }
  }

  /**
   * Add new notification (from WebSocket)
   */
  function addNotification(notification: Notification) {
    // Prepend to list
    notifications.value.unshift(notification)

    // Increment unread count if new notification is unread
    if (notification.status === 'UNREAD') {
      unreadCount.value++
    }
  }

  /**
   * Toggle dropdown visibility
   */
  function toggleDropdown() {
    isDropdownOpen.value = !isDropdownOpen.value

    // Fetch notifications when opening
    if (isDropdownOpen.value && notifications.value.length === 0) {
      fetchNotifications(0, true)
    }
  }

  /**
   * Close dropdown
   */
  function closeDropdown() {
    isDropdownOpen.value = false
  }

  /**
   * Load more notifications
   */
  async function loadMore() {
    if (hasMore.value && !isLoading.value) {
      await fetchNotifications(currentPage.value + 1)
    }
  }

  /**
   * Reset store state
   */
  function reset() {
    notifications.value = []
    unreadCount.value = 0
    isLoading.value = false
    isDropdownOpen.value = false
    hasMore.value = true
    currentPage.value = 0
  }

  return {
    // State
    notifications,
    unreadCount,
    isLoading,
    isDropdownOpen,
    hasMore,
    // Getters
    hasUnread,
    unreadNotifications,
    // Actions
    fetchNotifications,
    fetchUnreadCount,
    markAsRead,
    markAllAsRead,
    addNotification,
    toggleDropdown,
    closeDropdown,
    loadMore,
    reset
  }
})
