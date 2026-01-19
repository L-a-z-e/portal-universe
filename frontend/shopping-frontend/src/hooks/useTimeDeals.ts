/**
 * TimeDeal Hooks
 * 타임딜 관련 React Hooks
 */
import { useState, useEffect, useCallback } from 'react'
import { timeDealApi } from '@/api/endpoints'
import type { TimeDeal } from '@/types'

/**
 * 진행 중인 타임딜 목록 조회
 */
export function useActiveTimeDeals() {
  const [data, setData] = useState<TimeDeal[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)

  const fetchTimeDeals = useCallback(async () => {
    try {
      setIsLoading(true)
      setError(null)
      const response = await timeDealApi.getActiveTimeDeals()
      if (response.success) {
        setData(response.data)
      }
    } catch (e) {
      setError(e instanceof Error ? e : new Error('Failed to fetch time deals'))
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchTimeDeals()
  }, [fetchTimeDeals])

  return { data, isLoading, error, refetch: fetchTimeDeals }
}

/**
 * 타임딜 상세 조회
 */
export function useTimeDeal(id: number | null) {
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
      const response = await timeDealApi.getTimeDeal(id)
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
 * 타임딜 구매
 */
export function usePurchaseTimeDeal() {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  const purchaseTimeDeal = useCallback(async (id: number, quantity: number) => {
    try {
      setIsLoading(true)
      setError(null)
      const response = await timeDealApi.purchaseTimeDeal(id, quantity)
      if (!response.success) {
        throw new Error(response.message || 'Failed to purchase time deal')
      }
      return response.data
    } catch (e) {
      const err = e instanceof Error ? e : new Error('Failed to purchase time deal')
      setError(err)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  return { mutateAsync: purchaseTimeDeal, isPending: isLoading, error }
}

/**
 * 타임딜 남은 시간 계산
 */
export function useTimeDealCountdown(endsAt: string | null) {
  const [timeLeft, setTimeLeft] = useState<{
    hours: number
    minutes: number
    seconds: number
    isExpired: boolean
  }>({
    hours: 0,
    minutes: 0,
    seconds: 0,
    isExpired: true
  })

  useEffect(() => {
    if (!endsAt) return

    const calculateTimeLeft = () => {
      const now = new Date().getTime()
      const end = new Date(endsAt).getTime()
      const difference = end - now

      if (difference <= 0) {
        return { hours: 0, minutes: 0, seconds: 0, isExpired: true }
      }

      return {
        hours: Math.floor(difference / (1000 * 60 * 60)),
        minutes: Math.floor((difference % (1000 * 60 * 60)) / (1000 * 60)),
        seconds: Math.floor((difference % (1000 * 60)) / 1000),
        isExpired: false
      }
    }

    // Initial calculation
    setTimeLeft(calculateTimeLeft())

    // Update every second
    const timer = setInterval(() => {
      setTimeLeft(calculateTimeLeft())
    }, 1000)

    return () => clearInterval(timer)
  }, [endsAt])

  return timeLeft
}

/**
 * 타임딜 할인율 계산
 */
export function calculateDiscountRate(originalPrice: number, dealPrice: number): number {
  if (originalPrice <= 0) return 0
  return Math.round(((originalPrice - dealPrice) / originalPrice) * 100)
}

/**
 * 재고 비율 계산 (진행률 바 표시용)
 */
export function calculateStockPercentage(sold: number, total: number): number {
  if (total <= 0) return 100
  return Math.round((sold / total) * 100)
}
