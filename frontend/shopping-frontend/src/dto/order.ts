import type { Address, AddressRequest } from './common'

export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'PAID'
  | 'SHIPPING'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'REFUNDED'

export interface OrderItem {
  id: number
  productId: number
  productName: string
  price: number
  quantity: number
  subtotal: number
}

export interface Order {
  id: number
  orderNumber: string
  userId: string
  status: OrderStatus
  totalAmount: number
  discountAmount?: number
  finalAmount?: number
  appliedUserCouponId?: number
  shippingAddress: Address
  items: OrderItem[]
  cancelReason?: string
  cancelledAt?: string
  createdAt: string
  updatedAt?: string
}

export interface CreateOrderRequest {
  shippingAddress: AddressRequest
  userCouponId?: number
}

export interface CancelOrderRequest {
  reason: string
}

export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING: '주문 대기',
  CONFIRMED: '주문 확정',
  PAID: '결제 완료',
  SHIPPING: '배송 중',
  DELIVERED: '배송 완료',
  CANCELLED: '주문 취소',
  REFUNDED: '환불 완료'
}
