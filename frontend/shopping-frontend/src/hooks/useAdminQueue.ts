/**
 * Admin Queue Hooks
 * 관리자 대기열 관리 관련 React Hooks
 */
import { useState, useCallback } from 'react'
import { adminQueueApi } from '@/api/endpoints'
import type { QueueActivateRequest } from '@/types'

export function useActivateQueue() {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  const activate = useCallback(async (eventType: string, eventId: number, request: QueueActivateRequest) => {
    try {
      setIsLoading(true)
      setError(null)
      await adminQueueApi.activateQueue(eventType, eventId, request)
    } catch (e) {
      const err = e instanceof Error ? e : new Error('Failed to activate queue')
      setError(err)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  return { mutateAsync: activate, isPending: isLoading, error }
}

export function useDeactivateQueue() {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  const deactivate = useCallback(async (eventType: string, eventId: number) => {
    try {
      setIsLoading(true)
      setError(null)
      await adminQueueApi.deactivateQueue(eventType, eventId)
    } catch (e) {
      const err = e instanceof Error ? e : new Error('Failed to deactivate queue')
      setError(err)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  return { mutateAsync: deactivate, isPending: isLoading, error }
}

export function useProcessQueue() {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  const process = useCallback(async (eventType: string, eventId: number) => {
    try {
      setIsLoading(true)
      setError(null)
      await adminQueueApi.processQueue(eventType, eventId)
    } catch (e) {
      const err = e instanceof Error ? e : new Error('Failed to process queue')
      setError(err)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  return { mutateAsync: process, isPending: isLoading, error }
}
