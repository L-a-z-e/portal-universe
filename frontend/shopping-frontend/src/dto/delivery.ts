import type { Address } from './common'

export type DeliveryStatus =
  | 'PENDING'
  | 'PREPARING'
  | 'SHIPPED'
  | 'IN_TRANSIT'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'FAILED'

export interface DeliveryHistory {
  id: number
  status: DeliveryStatus
  location?: string
  description?: string
  createdAt: string
}

export interface Delivery {
  id: number
  orderNumber: string
  trackingNumber: string
  carrier: string
  status: DeliveryStatus
  shippingAddress: Address
  histories: DeliveryHistory[]
  estimatedDeliveryDate?: string
  actualDeliveryDate?: string
  createdAt: string
  updatedAt?: string
}

export interface UpdateDeliveryStatusRequest {
  status: DeliveryStatus
  location?: string
  description?: string
}

export const DELIVERY_STATUS_LABELS: Record<DeliveryStatus, string> = {
  PENDING: '배송 준비 대기',
  PREPARING: '상품 준비 중',
  SHIPPED: '출고 완료',
  IN_TRANSIT: '배송 중',
  OUT_FOR_DELIVERY: '배달 출발',
  DELIVERED: '배송 완료',
  FAILED: '배송 실패'
}
