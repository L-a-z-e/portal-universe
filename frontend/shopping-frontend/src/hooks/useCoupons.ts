/**
 * Coupon Hooks
 * 쿠폰 관련 React Hooks
 */
import { useState, useEffect, useCallback } from 'react'
import { couponApi } from '@/api/endpoints'
import type { Coupon, UserCoupon } from '@/types'
export type { UserCoupon }

/**
 * 발급 가능한 쿠폰 목록 조회
 */
export function useAvailableCoupons() {
  const [data, setData] = useState<Coupon[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)

  const fetchCoupons = useCallback(async () => {
    try {
      setIsLoading(true)
      setError(null)
      const response = await couponApi.getAvailableCoupons()
      if (response.success) {
        setData(response.data)
      }
    } catch (e) {
      setError(e instanceof Error ? e : new Error('Failed to fetch coupons'))
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchCoupons()
  }, [fetchCoupons])

  return { data, isLoading, error, refetch: fetchCoupons }
}

/**
 * 사용자 쿠폰 목록 조회
 */
export function useUserCoupons() {
  const [data, setData] = useState<UserCoupon[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)

  const fetchCoupons = useCallback(async () => {
    try {
      setIsLoading(true)
      setError(null)
      const response = await couponApi.getUserCoupons()
      if (response.success) {
        setData(response.data)
      }
    } catch (e) {
      setError(e instanceof Error ? e : new Error('Failed to fetch user coupons'))
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchCoupons()
  }, [fetchCoupons])

  return { data, isLoading, error, refetch: fetchCoupons }
}

/**
 * 사용 가능한 사용자 쿠폰 목록 조회
 */
export function useAvailableUserCoupons() {
  const [data, setData] = useState<UserCoupon[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)

  const fetchCoupons = useCallback(async () => {
    try {
      setIsLoading(true)
      setError(null)
      const response = await couponApi.getAvailableUserCoupons()
      if (response.success) {
        setData(response.data)
      }
    } catch (e) {
      setError(e instanceof Error ? e : new Error('Failed to fetch available user coupons'))
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchCoupons()
  }, [fetchCoupons])

  return { data, isLoading, error, refetch: fetchCoupons }
}

/**
 * 쿠폰 발급
 */
export function useIssueCoupon() {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  const issueCoupon = useCallback(async (couponId: number) => {
    try {
      setIsLoading(true)
      setError(null)
      const response = await couponApi.issueCoupon(couponId)
      return response.data
    } catch (e) {
      const err = e instanceof Error ? e : new Error('Failed to issue coupon')
      setError(err)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  return { mutateAsync: issueCoupon, isPending: isLoading, error }
}

/**
 * 할인 금액 계산 (클라이언트 측) - Coupon 기반
 */
export function calculateDiscount(coupon: Coupon, orderAmount: number): number {
  // 최소 주문 금액 검증
  if (coupon.minimumOrderAmount && orderAmount < coupon.minimumOrderAmount) {
    return 0
  }

  let discount: number
  if (coupon.discountType === 'FIXED') {
    discount = coupon.discountValue
  } else {
    // PERCENTAGE
    discount = Math.round(orderAmount * coupon.discountValue / 100)
  }

  // 최대 할인 금액 제한
  if (coupon.maximumDiscountAmount && discount > coupon.maximumDiscountAmount) {
    discount = coupon.maximumDiscountAmount
  }

  // 할인 금액이 주문 금액을 초과하지 않도록
  if (discount > orderAmount) {
    discount = orderAmount
  }

  return discount
}

/**
 * 할인 금액 계산 - UserCoupon (flat 구조) 기반
 */
export function calculateDiscountFromUserCoupon(uc: UserCoupon, orderAmount: number): number {
  if (uc.minimumOrderAmount && orderAmount < uc.minimumOrderAmount) {
    return 0
  }

  let discount: number
  if (uc.discountType === 'FIXED') {
    discount = uc.discountValue
  } else {
    discount = Math.round(orderAmount * uc.discountValue / 100)
  }

  if (uc.maximumDiscountAmount && discount > uc.maximumDiscountAmount) {
    discount = uc.maximumDiscountAmount
  }

  if (discount > orderAmount) {
    discount = orderAmount
  }

  return discount
}

/**
 * 쿠폰 적용 가능 여부 확인 - Coupon 기반
 */
export function canApplyCoupon(coupon: Coupon, orderAmount: number): boolean {
  if (coupon.minimumOrderAmount && orderAmount < coupon.minimumOrderAmount) {
    return false
  }
  return true
}

/**
 * 쿠폰 적용 가능 여부 확인 - UserCoupon (flat 구조) 기반
 */
export function canApplyUserCoupon(uc: UserCoupon, orderAmount: number): boolean {
  if (uc.minimumOrderAmount && orderAmount < uc.minimumOrderAmount) {
    return false
  }
  return true
}
