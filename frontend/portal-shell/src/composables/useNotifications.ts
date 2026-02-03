// portal-shell/src/composables/useNotifications.ts

import { onMounted, onUnmounted, ref } from 'vue'
import { useAuthStore } from '../store/auth'
import { useNotificationStore } from '../store/notification'

const POLL_INTERVAL_MS = 30000 // 30 seconds

/**
 * Composable for managing notification subscriptions.
 *
 * Currently uses REST polling as a fallback.
 * TODO: Add WebSocket (STOMP over SockJS) support for real-time notifications.
 *
 * To add WebSocket support:
 * 1. Install: pnpm add @stomp/stompjs sockjs-client
 * 2. Import: import { Client, IMessage } from '@stomp/stompjs'
 * 3. Connect to /ws/notifications endpoint
 * 4. Subscribe to /user/{userId}/queue/notifications
 */
export function useNotifications() {
  const authStore = useAuthStore()
  const notificationStore = useNotificationStore()

  const isConnected = ref(false)
  let pollInterval: ReturnType<typeof setInterval> | null = null

  /**
   * Start polling for notifications
   */
  function startPolling() {
    if (!authStore.isAuthenticated) {
      console.log('[useNotifications] Not authenticated, skipping poll')
      return
    }

    // Initial fetch
    notificationStore.fetchUnreadCount()

    // Set up polling interval
    pollInterval = setInterval(() => {
      if (authStore.isAuthenticated) {
        notificationStore.fetchUnreadCount()
      }
    }, POLL_INTERVAL_MS)

    isConnected.value = true
    console.log('[useNotifications] Polling started')
  }

  /**
   * Stop polling
   */
  function stopPolling() {
    if (pollInterval) {
      clearInterval(pollInterval)
      pollInterval = null
    }
    isConnected.value = false
    console.log('[useNotifications] Polling stopped')
  }

  /**
   * Initialize notifications when mounted
   */
  function connect() {
    startPolling()
  }

  /**
   * Cleanup when unmounted
   */
  function disconnect() {
    stopPolling()
    notificationStore.reset()
  }

  // Lifecycle hooks
  onMounted(() => {
    if (authStore.isAuthenticated) {
      connect()
    }
  })

  onUnmounted(() => {
    disconnect()
  })

  return {
    isConnected,
    connect,
    disconnect
  }
}
