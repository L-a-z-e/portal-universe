/**
 * Queue Hooks
 * 대기열 관련 React Hooks
 */
import { useState, useEffect, useCallback, useRef } from 'react'
import { queueApi } from '@/api/endpoints'
import type { QueueStatusResponse, QueueStatus } from '@/types'

interface UseQueueOptions {
  eventType: string
  eventId: number
  autoEnter?: boolean
}

/**
 * 대기열 상태 관리 Hook (SSE 연결 포함)
 */
export function useQueue(options: UseQueueOptions) {
  const { eventType, eventId, autoEnter = false } = options

  const [status, setStatus] = useState<QueueStatusResponse | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)
  const [isConnected, setIsConnected] = useState(false)

  const eventSourceRef = useRef<EventSource | null>(null)
  const entryTokenRef = useRef<string | null>(null)
  const reconnectTimeoutRef = useRef<ReturnType<typeof setTimeout>>()

  // 대기열 진입
  const enterQueue = useCallback(async () => {
    try {
      setIsLoading(true)
      setError(null)
      const response = await queueApi.enterQueue(eventType, eventId)
      if (response.success) {
        setStatus(response.data)
        entryTokenRef.current = response.data.entryToken
        return response.data
      }
    } catch (e) {
      const err = e instanceof Error ? e : new Error('Failed to enter queue')
      setError(err)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [eventType, eventId])

  // 대기열 이탈
  const leaveQueue = useCallback(async () => {
    try {
      setIsLoading(true)
      setError(null)

      if (entryTokenRef.current) {
        await queueApi.leaveQueueByToken(entryTokenRef.current)
      } else {
        await queueApi.leaveQueue(eventType, eventId)
      }

      // SSE 연결 해제
      if (eventSourceRef.current) {
        eventSourceRef.current.close()
        eventSourceRef.current = null
        setIsConnected(false)
      }

      setStatus(null)
      entryTokenRef.current = null
    } catch (e) {
      const err = e instanceof Error ? e : new Error('Failed to leave queue')
      setError(err)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [eventType, eventId])

  // 대기열 상태 조회
  const refreshStatus = useCallback(async () => {
    if (!entryTokenRef.current) return

    try {
      const response = await queueApi.getQueueStatusByToken(entryTokenRef.current)
      if (response.success) {
        setStatus(response.data)
      }
    } catch (e) {
      console.error('Failed to refresh queue status:', e)
    }
  }, [])

  // SSE 연결
  const connectSSE = useCallback(() => {
    if (!entryTokenRef.current || eventSourceRef.current) return

    const url = queueApi.getSubscribeUrl(eventType, eventId, entryTokenRef.current)
    const eventSource = new EventSource(url)
    eventSourceRef.current = eventSource

    eventSource.onopen = () => {
      setIsConnected(true)
      console.log('SSE connected for queue')
    }

    eventSource.addEventListener('queue-status', (event) => {
      try {
        const envelope = JSON.parse(event.data) as { type: string; data: QueueStatusResponse; timestamp: string }
        const data = envelope.data
        setStatus(data)

        // 입장 완료 또는 만료 시 연결 종료
        if (data.status !== 'WAITING') {
          eventSource.close()
          eventSourceRef.current = null
          setIsConnected(false)
        }
      } catch (e) {
        console.error('Failed to parse SSE data:', e)
      }
    })

    eventSource.onerror = (e) => {
      console.error('SSE error:', e)
      eventSource.close()
      eventSourceRef.current = null
      setIsConnected(false)

      // 재연결 시도 (5초 후)
      reconnectTimeoutRef.current = setTimeout(() => {
        if (entryTokenRef.current && status?.status === 'WAITING') {
          connectSSE()
        }
      }, 5000)
    }
  }, [eventType, eventId, status?.status])

  // 자동 진입 및 SSE 연결
  useEffect(() => {
    if (autoEnter) {
      enterQueue().then((response) => {
        if (response && response.status === 'WAITING') {
          connectSSE()
        }
      })
    }

    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close()
        eventSourceRef.current = null
      }
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current)
      }
    }
  }, [autoEnter, enterQueue, connectSSE])

  // status가 WAITING이면 SSE 연결
  useEffect(() => {
    if (status?.status === 'WAITING' && entryTokenRef.current && !eventSourceRef.current) {
      connectSSE()
    }
  }, [status?.status, connectSSE])

  return {
    status,
    isLoading,
    error,
    isConnected,
    enterQueue,
    leaveQueue,
    refreshStatus,
    entryToken: entryTokenRef.current
  }
}

/**
 * 대기열 폴링 Hook (SSE 미지원 환경용)
 */
export function useQueuePolling(entryToken: string | null, interval = 3000) {
  const [status, setStatus] = useState<QueueStatusResponse | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  useEffect(() => {
    if (!entryToken) return

    const fetchStatus = async () => {
      try {
        setIsLoading(true)
        const response = await queueApi.getQueueStatusByToken(entryToken)
        if (response.success) {
          setStatus(response.data)
        }
      } catch (e) {
        setError(e instanceof Error ? e : new Error('Failed to fetch queue status'))
      } finally {
        setIsLoading(false)
      }
    }

    // 초기 조회
    fetchStatus()

    // 대기 중일 때만 폴링
    const pollInterval = setInterval(() => {
      if (status?.status === 'WAITING') {
        fetchStatus()
      }
    }, interval)

    return () => clearInterval(pollInterval)
  }, [entryToken, interval, status?.status])

  return { status, isLoading, error }
}

/**
 * 예상 대기 시간 포맷 헬퍼
 */
export function formatWaitTime(seconds: number): string {
  if (seconds < 60) {
    return `약 ${seconds}초`
  }
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  if (minutes < 60) {
    return remainingSeconds > 0 ? `약 ${minutes}분 ${remainingSeconds}초` : `약 ${minutes}분`
  }
  const hours = Math.floor(minutes / 60)
  const remainingMinutes = minutes % 60
  return `약 ${hours}시간 ${remainingMinutes}분`
}
