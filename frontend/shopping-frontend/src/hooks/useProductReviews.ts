/**
 * Product Reviews Hook
 * Blog 서비스 연동 상품 리뷰 조회
 */
import { useState, useEffect, useCallback } from 'react'
import { productReviewApi } from '@/api'
import type { ProductWithReviews } from '@/types'

export function useProductReviews(productId: number | null) {
  const [data, setData] = useState<ProductWithReviews | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)

  const fetch = useCallback(async () => {
    if (!productId) {
      setIsLoading(false)
      return
    }
    try {
      setIsLoading(true)
      setError(null)
      const response = await productReviewApi.getProductWithReviews(productId)
      if (response.success) {
        setData(response.data)
      }
    } catch (e) {
      setError(e instanceof Error ? e : new Error('Failed to fetch reviews'))
    } finally {
      setIsLoading(false)
    }
  }, [productId])

  useEffect(() => {
    fetch()
  }, [fetch])

  return { data, isLoading, error, refetch: fetch }
}
