/**
 * Admin Stock Movement Hooks
 * 관리자 재고 이동 이력 조회
 */
import { useState, useEffect, useCallback } from 'react'
import { stockMovementApi } from '@/api/endpoints'
import type { StockMovement, PagedResponse } from '@/types'

interface UseAdminStockMovementsOptions {
  productId: number | null
  page?: number
  size?: number
}

export function useAdminStockMovements(options: UseAdminStockMovementsOptions) {
  const { productId, page = 0, size = 20 } = options
  const [data, setData] = useState<PagedResponse<StockMovement> | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  const fetch = useCallback(async () => {
    if (!productId) {
      setIsLoading(false)
      return
    }
    try {
      setIsLoading(true)
      setError(null)
      const response = await stockMovementApi.getMovements(productId, page, size)
      if (response.success) {
        setData(response.data)
      }
    } catch (e) {
      setError(e instanceof Error ? e : new Error('Failed to fetch stock movements'))
    } finally {
      setIsLoading(false)
    }
  }, [productId, page, size])

  useEffect(() => {
    fetch()
  }, [fetch])

  return { data, isLoading, error, refetch: fetch }
}
