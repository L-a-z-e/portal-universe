/**
 * Admin Coupon Hooks
 * 관리자 쿠폰 관련 React Hooks
 */
import { useState, useEffect, useCallback } from 'react'
import { adminCouponApi } from '@/api/endpoints'
import type { Coupon, CouponCreateRequest, PagedResponse } from '@/types'

interface UseAdminCouponsOptions {
  page?: number
  size?: number
}

/**
 * 쿠폰 목록 조회 (Admin)
 */
export function useAdminCoupons(options: UseAdminCouponsOptions = {}) {
  const { page = 0, size = 10 } = options
  const [data, setData] = useState<PagedResponse<Coupon> | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)

  const fetchCoupons = useCallback(async () => {
    try {
      setIsLoading(true)
      setError(null)
      const response = await adminCouponApi.getCoupons(page, size)
      if (response.success) {
        setData(response.data)
      }
    } catch (e) {
      setError(e instanceof Error ? e : new Error('Failed to fetch coupons'))
    } finally {
      setIsLoading(false)
    }
  }, [page, size])

  useEffect(() => {
    fetchCoupons()
  }, [fetchCoupons])

  return { data, isLoading, error, refetch: fetchCoupons }
}

/**
 * 쿠폰 상세 조회 (Admin)
 */
export function useAdminCoupon(id: number | null) {
  const [data, setData] = useState<Coupon | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)

  const fetchCoupon = useCallback(async () => {
    if (!id) {
      setIsLoading(false)
      return
    }
    try {
      setIsLoading(true)
      setError(null)
      const response = await adminCouponApi.getCoupon(id)
      if (response.success) {
        setData(response.data)
      }
    } catch (e) {
      setError(e instanceof Error ? e : new Error('Failed to fetch coupon'))
    } finally {
      setIsLoading(false)
    }
  }, [id])

  useEffect(() => {
    fetchCoupon()
  }, [fetchCoupon])

  return { data, isLoading, error, refetch: fetchCoupon }
}

/**
 * 쿠폰 생성 (Admin)
 */
export function useCreateCoupon() {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  const createCoupon = useCallback(async (data: CouponCreateRequest) => {
    try {
      setIsLoading(true)
      setError(null)
      const response = await adminCouponApi.createCoupon(data)
      if (!response.success) {
        throw new Error(response.message || 'Failed to create coupon')
      }
      return response.data
    } catch (e) {
      const err = e instanceof Error ? e : new Error('Failed to create coupon')
      setError(err)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  return { mutateAsync: createCoupon, isPending: isLoading, error }
}

/**
 * 쿠폰 비활성화 (Admin)
 */
export function useDeactivateCoupon() {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  const deactivateCoupon = useCallback(async (id: number) => {
    try {
      setIsLoading(true)
      setError(null)
      const response = await adminCouponApi.deactivateCoupon(id)
      if (!response.success) {
        throw new Error(response.message || 'Failed to deactivate coupon')
      }
    } catch (e) {
      const err = e instanceof Error ? e : new Error('Failed to deactivate coupon')
      setError(err)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  return { mutateAsync: deactivateCoupon, isPending: isLoading, error }
}
