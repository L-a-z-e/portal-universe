// frontend/portal-shell/src/composables/useWebSocket.ts
// WebSocket connection management for real-time notifications

import { ref, onUnmounted, watch } from 'vue'
import { Client } from '@stomp/stompjs'
import type { IMessage, StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from '../store/auth'
import { useNotificationStore } from '../store/notification'
import type { Notification } from '../types/notification'
import apiClient from '../api/apiClient'

// Singleton instance for WebSocket client
let clientInstance: Client | null = null
let subscriptionInstance: StompSubscription | null = null

export function useWebSocket() {
  const authStore = useAuthStore()
  const notificationStore = useNotificationStore()

  // ==================== State ====================
  const isConnected = ref(false)
  const reconnectAttempts = ref(0)

  // ==================== WebSocket URL ====================
  function getWebSocketUrl(): string {
    // Use API Gateway URL from environment variable
    // Docker/K8s: VITE_API_BASE_URL is empty → use current origin (same-origin proxy)
    const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || window.location.origin
    // API Gateway routes to notification-service WebSocket
    return `${apiBaseUrl}/notification/ws/notifications`
  }

  // ==================== WS Ticket ====================
  async function fetchWsTicket(): Promise<string> {
    const response = await apiClient.post('/api/v1/notifications/ws-ticket')
    return response.data.data.ticket
  }

  // ==================== Connect ====================
  async function connect() {
    // Skip if already connected
    if (clientInstance?.active) {
      isConnected.value = true
      return
    }

    // Skip if not authenticated
    if (!authStore.isAuthenticated || !authStore.user?.profile.sub) {
      return
    }

    const userId = authStore.user.profile.sub

    clientInstance = new Client({
      // Use SockJS as WebSocket factory
      webSocketFactory: () => new SockJS(getWebSocketUrl()) as WebSocket,

      // Fetch a fresh one-time ticket before each (re)connect
      beforeConnect: async () => {
        try {
          const ticket = await fetchWsTicket()
          clientInstance!.connectHeaders = { 'X-WS-Ticket': ticket }
        } catch (error) {
          console.error('[WebSocket] Failed to fetch WS ticket:', error)
          throw error
        }
      },

      // Debug logging disabled
      debug: () => {},

      // On connect success
      onConnect: () => {
        isConnected.value = true
        reconnectAttempts.value = 0

        // Subscribe to user's notification queue
        subscribeToNotifications(userId)
      },

      // On disconnect
      onDisconnect: () => {
        isConnected.value = false
        subscriptionInstance = null
      },

      // On STOMP error
      onStompError: (frame) => {
        console.error('[WebSocket] STOMP error:', frame.headers['message'])
      },

      // On WebSocket error
      onWebSocketError: (event) => {
        console.error('[WebSocket] WebSocket error:', event)
      },

      // Reconnection settings
      reconnectDelay: 5000,  // 5 seconds
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    })

    clientInstance.activate()
  }

  // ==================== Subscribe ====================
  function subscribeToNotifications(userId: string) {
    if (!clientInstance) return

    const destination = `/user/${userId}/queue/notifications`

    subscriptionInstance = clientInstance.subscribe(
      destination,
      (message: IMessage) => {
        try {
          const notification: Notification = JSON.parse(message.body)

          // Check for duplicate before adding
          const existingNotifications = notificationStore.notifications
          const exists = existingNotifications.some(n => n.id === notification.id)

          if (!exists) {
            notificationStore.addNotification(notification)
          }
        } catch (error) {
          console.error('[WebSocket] Failed to parse message:', error)
        }
      }
    )
  }

  // ==================== Disconnect ====================
  function disconnect() {
    if (subscriptionInstance) {
      subscriptionInstance.unsubscribe()
      subscriptionInstance = null
    }

    if (clientInstance?.active) {
      clientInstance.deactivate()
    }

    clientInstance = null
    isConnected.value = false
  }

  // ==================== Watch Auth State ====================
  const stopAuthWatch = watch(
    () => authStore.isAuthenticated,
    (newIsAuthenticated, oldIsAuthenticated) => {
      if (newIsAuthenticated && !oldIsAuthenticated) {
        // Logged in - connect
        void connect()
      } else if (!newIsAuthenticated && oldIsAuthenticated) {
        // Logged out - disconnect
        disconnect()
      }
    }
  )

  // ==================== Lifecycle ====================
  // Auto-connect if already authenticated
  if (authStore.isAuthenticated) {
    void connect()
  }

  onUnmounted(() => {
    stopAuthWatch()
    // Note: Don't disconnect on unmount if other components may still need it
    // The singleton pattern ensures connection persists across components
  })

  return {
    isConnected,
    connect,
    disconnect,
  }
}
