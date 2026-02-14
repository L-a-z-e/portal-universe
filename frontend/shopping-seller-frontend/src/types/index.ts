// Re-export types from shopping-frontend
// 판매자 앱은 동일한 타입을 사용합니다

export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'PAID' | 'SHIPPING' | 'DELIVERED' | 'CANCELLED' | 'REFUNDED'
export type PaymentStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'CANCELLED' | 'REFUNDED'
export type PaymentMethod = 'CARD' | 'TRANSFER' | 'VIRTUAL_ACCOUNT' | 'MOBILE'
export type DeliveryStatus = 'PENDING' | 'PREPARING' | 'SHIPPED' | 'IN_TRANSIT' | 'OUT_FOR_DELIVERY' | 'DELIVERED' | 'FAILED'
export type CouponStatus = 'ACTIVE' | 'INACTIVE' | 'EXPIRED' | 'SOLD_OUT'
export type DiscountType = 'FIXED' | 'PERCENTAGE'
export type TimeDealStatus = 'SCHEDULED' | 'ACTIVE' | 'ENDED' | 'CANCELLED' | 'SOLD_OUT'
export type MovementType = 'INITIAL' | 'RESERVE' | 'DEDUCT' | 'RELEASE' | 'INBOUND' | 'RETURN' | 'ADJUSTMENT'

export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING: 'Pending',
  CONFIRMED: 'Confirmed',
  PAID: 'Paid',
  SHIPPING: 'Shipping',
  DELIVERED: 'Delivered',
  CANCELLED: 'Cancelled',
  REFUNDED: 'Refunded'
}

export const PAYMENT_STATUS_LABELS: Record<PaymentStatus, string> = {
  PENDING: 'Pending',
  COMPLETED: 'Completed',
  FAILED: 'Failed',
  CANCELLED: 'Cancelled',
  REFUNDED: 'Refunded'
}

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CARD: 'Card',
  TRANSFER: 'Transfer',
  VIRTUAL_ACCOUNT: 'Virtual Account',
  MOBILE: 'Mobile'
}

export const DELIVERY_STATUS_LABELS: Record<DeliveryStatus, string> = {
  PENDING: 'Pending',
  PREPARING: 'Preparing',
  SHIPPED: 'Shipped',
  IN_TRANSIT: 'In Transit',
  OUT_FOR_DELIVERY: 'Out for Delivery',
  DELIVERED: 'Delivered',
  FAILED: 'Failed'
}

export const COUPON_STATUS_LABELS: Record<CouponStatus, string> = {
  ACTIVE: 'Active',
  INACTIVE: 'Inactive',
  EXPIRED: 'Expired',
  SOLD_OUT: 'Sold Out'
}

export const DISCOUNT_TYPE_LABELS: Record<DiscountType, string> = {
  FIXED: 'Fixed Amount',
  PERCENTAGE: 'Percentage'
}

export const TIMEDEAL_STATUS_LABELS: Record<TimeDealStatus, string> = {
  SCHEDULED: 'Scheduled',
  ACTIVE: 'Active',
  ENDED: 'Ended',
  CANCELLED: 'Cancelled',
  SOLD_OUT: 'Sold Out'
}

export interface Product {
  id: number
  name: string
  description: string
  price: number
  imageUrl?: string
  category?: string
  stockQuantity?: number
  createdAt: string
  updatedAt: string
}

export interface ProductFormData {
  name: string
  description: string
  price: number
  stock: number
  imageUrl?: string
  category?: string
}

export interface ProductFilters {
  page: number
  size: number
  keyword?: string
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

export interface Order {
  id: number
  orderNumber: string
  userId: number
  status: OrderStatus
  totalAmount: number
  discountAmount?: number
  finalAmount?: number
  items: OrderItem[]
  shippingAddress?: Address
  cancelReason?: string
  cancelledAt?: string
  createdAt: string
  updatedAt: string
}

export interface OrderItem {
  id: number
  productId: number
  productName: string
  quantity: number
  price: number
  subtotal: number
}

export interface Address {
  receiverName: string
  receiverPhone: string
  address1: string
  address2?: string
  zipCode: string
}

export interface Payment {
  id: number
  orderNumber: string
  transactionId: string
  method: PaymentMethod
  status: PaymentStatus
  amount: number
  paidAt?: string
  createdAt: string
}

export interface Delivery {
  id: number
  orderNumber: string
  trackingNumber: string
  carrier: string
  status: DeliveryStatus
  estimatedDeliveryDate?: string
  histories: DeliveryHistory[]
  createdAt: string
}

export interface DeliveryHistory {
  id: number
  status: DeliveryStatus
  location?: string
  description?: string
  createdAt: string
}

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
  status: CouponStatus
  startsAt: string
  expiresAt: string
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

export interface TimeDeal {
  id: number
  name: string
  description?: string
  status: TimeDealStatus
  startsAt: string
  endsAt: string
  products?: TimeDealProduct[]
}

export interface TimeDealProduct {
  id: number
  productId: number
  productName: string
  originalPrice: number
  dealPrice: number
  discountRate: number
  dealQuantity: number
  soldQuantity: number
}

export interface PaginatedResponse<T> {
  items: T[]
  totalItems: number
  totalPages: number
  currentPage: number
}
