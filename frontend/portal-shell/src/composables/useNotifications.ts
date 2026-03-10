// portal-shell/src/composables/useNotifications.ts

import { onMounted, onUnmounted, ref } from 'vue'
import { useAuthStore } from '../store/auth'
import { useNotificationStore } from '../store/notification'

const POLL_INTERVAL_MS = 30000 // 30 seconds

// TODO: WebSocket (STOMP over SockJS) 지원 추가 예정, 현재는 REST polling fallback
export function useNotifications() {
  const authStore = useAuthStore()
  const notificationStore = useNotificationStore()

  const isConnected = ref(false)
  let pollInterval: ReturnType<typeof setInterval> | null = null

  function startPolling() {
    if (!authStore.isAuthenticated) {
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
  }

  function stopPolling() {
    if (pollInterval) {
      clearInterval(pollInterval)
      pollInterval = null
    }
    isConnected.value = false
  }

  function connect() {
    startPolling()
  }

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
