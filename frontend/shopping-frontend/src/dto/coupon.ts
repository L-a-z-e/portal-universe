export type DiscountType = 'FIXED' | 'PERCENTAGE'
export type CouponStatus = 'ACTIVE' | 'INACTIVE' | 'EXPIRED' | 'EXHAUSTED'
export type UserCouponStatus = 'AVAILABLE' | 'USED' | 'EXPIRED'

export interface Coupon {
  id: number
  code: string
  name: string
  description?: string
  discountType: DiscountType
  discountValue: number
  minimumOrderAmount?: number
  maximumDiscountAmount?: number
  totalQuantity: number
  issuedQuantity: number
  remainingQuantity: number
  status: CouponStatus
  startsAt: string
  expiresAt: string
  createdAt: string
}

export interface UserCoupon {
  id: number
  couponId: number
  couponCode: string
  couponName: string
  discountType: DiscountType
  discountValue: number
  minimumOrderAmount?: number
  maximumDiscountAmount?: number
  status: UserCouponStatus
  issuedAt: string
  expiresAt: string
  usedAt?: string
  usedOrderId?: number
}

export interface CouponCreateRequest {
  code: string
  name: string
  description?: string
  discountType: DiscountType
  discountValue: number
  minimumOrderAmount?: number
  maximumDiscountAmount?: number
  totalQuantity: number
  startsAt: string
  expiresAt: string
}

export const DISCOUNT_TYPE_LABELS: Record<DiscountType, string> = {
  FIXED: '정액 할인',
  PERCENTAGE: '정률 할인'
}

export const COUPON_STATUS_LABELS: Record<CouponStatus, string> = {
  ACTIVE: '활성',
  INACTIVE: '비활성',
  EXPIRED: '만료',
  EXHAUSTED: '소진'
}

export const USER_COUPON_STATUS_LABELS: Record<UserCouponStatus, string> = {
  AVAILABLE: '사용 가능',
  USED: '사용 완료',
  EXPIRED: '기한 만료'
}
