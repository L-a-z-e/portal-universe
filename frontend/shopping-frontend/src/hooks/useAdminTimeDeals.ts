/**
 * Admin TimeDeal Hooks
 * 관리자 타임딜 관련 React Hooks
 */
import { useState, useEffect, useCallback } from 'react'
import { adminTimeDealApi } from '@/api/endpoints'
import type { TimeDeal, TimeDealCreateRequest, PageResponse } from '@/types'

interface UseAdminTimeDealsOptions {
  page?: number
  size?: number
}

/**
 * 타임딜 목록 조회 (Admin)
 */
export function useAdminTimeDeals(options: UseAdminTimeDealsOptions = {}) {
  const { page = 1, size = 10 } = options
  const [data, setData] = useState<PageResponse<TimeDeal> | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)

  const fetchTimeDeals = useCallback(async () => {
    try {
      setIsLoading(true)
      setError(null)
      const response = await adminTimeDealApi.getTimeDeals(page, size)
      if (response.success) {
        setData(response.data)
      }
    } catch (e) {
      setError(e instanceof Error ? e : new Error('Failed to fetch time deals'))
    } finally {
      setIsLoading(false)
    }
  }, [page, size])

  useEffect(() => {
    fetchTimeDeals()
  }, [fetchTimeDeals])

  return { data, isLoading, error, refetch: fetchTimeDeals }
}

/**
 * 타임딜 상세 조회 (Admin)
 */
export function useAdminTimeDeal(id: number | null) {
  const [data, setData] = useState<TimeDeal | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)

  const fetchTimeDeal = useCallback(async () => {
    if (!id) {
      setIsLoading(false)
      return
    }
    try {
      setIsLoading(true)
      setError(null)
      const response = await adminTimeDealApi.getTimeDeal(id)
      if (response.success) {
        setData(response.data)
      }
    } catch (e) {
      setError(e instanceof Error ? e : new Error('Failed to fetch time deal'))
    } finally {
      setIsLoading(false)
    }
  }, [id])

  useEffect(() => {
    fetchTimeDeal()
  }, [fetchTimeDeal])

  return { data, isLoading, error, refetch: fetchTimeDeal }
}

/**
 * 타임딜 생성 (Admin)
 */
export function useCreateTimeDeal() {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  const createTimeDeal = useCallback(async (data: TimeDealCreateRequest) => {
    try {
      setIsLoading(true)
      setError(null)
      const response = await adminTimeDealApi.createTimeDeal(data)
      return response.data
    } catch (e) {
      const err = e instanceof Error ? e : new Error('Failed to create time deal')
      setError(err)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  return { mutateAsync: createTimeDeal, isPending: isLoading, error }
}

/**
 * 타임딜 취소 (Admin)
 */
export function useCancelTimeDeal() {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  const cancelTimeDeal = useCallback(async (id: number) => {
    try {
      setIsLoading(true)
      setError(null)
      await adminTimeDealApi.cancelTimeDeal(id)
    } catch (e) {
      const err = e instanceof Error ? e : new Error('Failed to cancel time deal')
      setError(err)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  return { mutateAsync: cancelTimeDeal, isPending: isLoading, error }
}
