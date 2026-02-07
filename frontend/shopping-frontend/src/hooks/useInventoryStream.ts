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

const MAX_RECONNECT_ATTEMPTS = 5
const BASE_RECONNECT_DELAY = 3000

export function useInventoryStream({ productIds, enabled = true }: UseInventoryStreamOptions) {
  const [updates, setUpdates] = useState<Map<number, InventoryUpdate>>(new Map())
  const [isConnected, setIsConnected] = useState(false)
  const [error, setError] = useState<Error | null>(null)
  const eventSourceRef = useRef<EventSource | null>(null)
  const reconnectTimeoutRef = useRef<ReturnType<typeof setTimeout>>()
  const reconnectCountRef = useRef(0)

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
      reconnectCountRef.current = 0
    }

    eventSource.onmessage = (event) => {
      try {
        const envelope = JSON.parse(event.data) as { type: string; data: InventoryUpdate; timestamp: string }
        if (envelope.type === 'heartbeat') return
        const update = envelope.data
        setUpdates(prev => {
          const existing = prev.get(update.productId)
          if (existing && existing.available === update.available && existing.reserved === update.reserved) {
            return prev
          }
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

      if (reconnectCountRef.current >= MAX_RECONNECT_ATTEMPTS) {
        setError(new Error('SSE connection failed after max retries'))
        return
      }

      const delay = BASE_RECONNECT_DELAY * Math.pow(2, reconnectCountRef.current)
      reconnectCountRef.current++
      reconnectTimeoutRef.current = setTimeout(() => {
        connect()
      }, delay)
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
