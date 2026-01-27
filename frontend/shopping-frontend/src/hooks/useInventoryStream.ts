/**
 * Inventory Stream Hook
 * EventSource 기반 SSE 연결로 실시간 재고 업데이트 수신
 */
import { useState, useEffect, useRef, useCallback } from 'react'
import { inventoryStreamApi } from '@/api/endpoints'
import type { InventoryUpdate } from '@/types'

interface UseInventoryStreamOptions {
  productIds: number[]
  enabled?: boolean
}

export function useInventoryStream({ productIds, enabled = true }: UseInventoryStreamOptions) {
  const [updates, setUpdates] = useState<Map<number, InventoryUpdate>>(new Map())
  const [isConnected, setIsConnected] = useState(false)
  const [error, setError] = useState<Error | null>(null)
  const eventSourceRef = useRef<EventSource | null>(null)
  const reconnectTimeoutRef = useRef<ReturnType<typeof setTimeout>>()

  const connect = useCallback(() => {
    if (!enabled || productIds.length === 0) return

    // Close existing connection
    if (eventSourceRef.current) {
      eventSourceRef.current.close()
    }

    const url = inventoryStreamApi.getStreamUrl(productIds)
    const eventSource = new EventSource(url)
    eventSourceRef.current = eventSource

    eventSource.onopen = () => {
      setIsConnected(true)
      setError(null)
    }

    eventSource.onmessage = (event) => {
      try {
        const update: InventoryUpdate = JSON.parse(event.data)
        setUpdates(prev => {
          const next = new Map(prev)
          next.set(update.productId, update)
          return next
        })
      } catch {
        // ignore parse errors
      }
    }

    eventSource.onerror = () => {
      setIsConnected(false)
      eventSource.close()
      // Auto-reconnect after 5 seconds
      reconnectTimeoutRef.current = setTimeout(() => {
        connect()
      }, 5000)
    }
  }, [productIds, enabled])

  useEffect(() => {
    connect()

    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close()
      }
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current)
      }
    }
  }, [connect])

  const getUpdate = useCallback((productId: number) => {
    return updates.get(productId) ?? null
  }, [updates])

  return { updates, isConnected, error, getUpdate }
}
