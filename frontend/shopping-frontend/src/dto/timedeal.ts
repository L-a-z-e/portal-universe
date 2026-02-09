export type TimeDealStatus = 'SCHEDULED' | 'ACTIVE' | 'ENDED' | 'SOLD_OUT' | 'CANCELLED'

export interface TimeDealProduct {
  id: number
  productId: number
  productName: string
  originalPrice: number
  dealPrice: number
  discountRate: number
  dealQuantity: number
  soldQuantity: number
  remainingQuantity: number
  maxPerUser: number
  available: boolean
}

export interface TimeDeal {
  id: number
  name: string
  description: string
  status: TimeDealStatus
  startsAt: string
  endsAt: string
  products: TimeDealProduct[]
  createdAt: string
}

export interface TimeDealProductCreateRequest {
  productId: number
  dealPrice: number
  dealQuantity: number
  maxPerUser: number
}

export interface TimeDealCreateRequest {
  name: string
  description?: string
  startsAt: string
  endsAt: string
  products: TimeDealProductCreateRequest[]
}

export interface TimeDealPurchase {
  id: number
  timeDealProductId: number
  productName: string
  quantity: number
  purchasePrice: number
  totalPrice: number
  purchasedAt: string
}

export const TIMEDEAL_STATUS_LABELS: Record<TimeDealStatus, string> = {
  SCHEDULED: '예정',
  ACTIVE: '진행 중',
  ENDED: '종료',
  SOLD_OUT: '품절',
  CANCELLED: '취소'
}
