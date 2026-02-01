/**
 * Shopping Frontend Type Definitions
 * Backend DTO와 매핑되는 타입들
 */

// ============================================
// Common Types
// ============================================

export interface PageInfo {
  page: number
  size: number
  totalElements: number
  totalPages: number
}

// API types from canonical source
export type { ApiResponse, ApiErrorResponse, ErrorDetails, FieldError } from '@portal/design-types'

export interface PagedResponse<T> {
  content: T[]
  pageable: PageInfo
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
  empty: boolean
}

// ============================================
// Address
// ============================================

export interface Address {
  receiverName: string
  receiverPhone: string
  zipCode: string
  address1: string
  address2?: string
}

export interface AddressRequest {
  receiverName: string
  receiverPhone: string
  zipCode: string
  address1: string
  address2?: string
}

// ============================================
// Product
// ============================================

export interface Product {
  id: number
  name: string
  description: string
  price: number
  stockQuantity?: number
  imageUrl?: string
  category?: string
  createdAt: string
  updatedAt?: string
}

export interface ProductCreateRequest {
  name: string
  description: string
  price: number
  imageUrl?: string
  category?: string
}

export interface ProductUpdateRequest {
  name?: string
  description?: string
  price?: number
  imageUrl?: string
  category?: string
}

// ============================================
// Inventory
// ============================================

export interface Inventory {
  id: number
  productId: number
  availableQuantity: number
  reservedQuantity: number
  totalQuantity: number
  createdAt: string
  updatedAt?: string
}

export interface InventoryUpdateRequest {
  quantity: number
  reason?: string
}

// ============================================
// Cart
// ============================================

export type CartStatus = 'ACTIVE' | 'CHECKED_OUT'

export interface CartItem {
  id: number
  productId: number
  productName: string
  price: number
  quantity: number
  addedAt: string
}

export interface Cart {
  id: number
  userId: string
  status: CartStatus
  items: CartItem[]
  totalAmount: number
  itemCount: number
  totalQuantity: number
  createdAt: string
  updatedAt?: string
}

export interface AddCartItemRequest {
  productId: number
  quantity: number
}

export interface UpdateCartItemRequest {
  quantity: number
}

// ============================================
// Order
// ============================================

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

// ============================================
// Payment
// ============================================

export type PaymentStatus =
  | 'PENDING'
  | 'PROCESSING'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED'
  | 'REFUNDED'

export type PaymentMethod =
  | 'CARD'
  | 'BANK_TRANSFER'
  | 'VIRTUAL_ACCOUNT'
  | 'MOBILE'
  | 'POINTS'

export interface Payment {
  id: number
  orderNumber: string
  amount: number
  method: PaymentMethod
  status: PaymentStatus
  transactionId?: string
  pgResponse?: string
  paidAt?: string
  failedAt?: string
  failureReason?: string
  createdAt: string
  updatedAt?: string
}

export interface ProcessPaymentRequest {
  orderNumber: string
  paymentMethod: PaymentMethod
  cardNumber?: string
  cardExpiry?: string
  cardCvv?: string
}

// ============================================
// Delivery
// ============================================

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

// ============================================
// Stock Movement (for admin/debugging)
// ============================================

export type MovementType =
  | 'INITIAL'
  | 'RESERVE'
  | 'DEDUCT'
  | 'RELEASE'
  | 'INBOUND'
  | 'RETURN'
  | 'ADJUSTMENT'

export interface StockMovement {
  id: number
  inventoryId: number
  productId: number
  movementType: MovementType
  quantity: number
  previousAvailable: number
  afterAvailable: number
  previousReserved: number
  afterReserved: number
  referenceType?: string
  referenceId?: string
  reason?: string
  performedBy?: string
  createdAt: string
}

// ============================================
// UI Helper Types
// ============================================

export interface SelectOption {
  value: string
  label: string
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

export const PAYMENT_STATUS_LABELS: Record<PaymentStatus, string> = {
  PENDING: '결제 대기',
  PROCESSING: '결제 처리 중',
  COMPLETED: '결제 완료',
  FAILED: '결제 실패',
  CANCELLED: '결제 취소',
  REFUNDED: '환불 완료'
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

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CARD: '카드',
  BANK_TRANSFER: '계좌이체',
  VIRTUAL_ACCOUNT: '가상계좌',
  MOBILE: '휴대폰',
  POINTS: '포인트'
}

// ============================================
// Coupon
// ============================================

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

// ============================================
// TimeDeal
// ============================================

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

export const TIMEDEAL_STATUS_LABELS: Record<TimeDealStatus, string> = {
  SCHEDULED: '예정',
  ACTIVE: '진행 중',
  ENDED: '종료',
  SOLD_OUT: '품절',
  CANCELLED: '취소'
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

// ========================================
// Queue Types
// ========================================

export type QueueStatus = 'WAITING' | 'ENTERED' | 'EXPIRED' | 'LEFT'

export interface QueueStatusResponse {
  entryToken: string
  status: QueueStatus
  position: number
  estimatedWaitSeconds: number
  totalWaiting: number
  message: string
}

export interface QueueActivateRequest {
  maxCapacity: number
  entryBatchSize: number
  entryIntervalSeconds: number
}

export const QUEUE_STATUS_LABELS: Record<QueueStatus, string> = {
  WAITING: '대기 중',
  ENTERED: '입장 완료',
  EXPIRED: '만료됨',
  LEFT: '이탈'
}

// ============================================
// Search Types
// ============================================

export interface SearchSuggestion {
  keyword: string
}

// ============================================
// Inventory Stream (SSE)
// ============================================

export interface InventoryUpdate {
  productId: number
  available: number
  reserved: number
  timestamp: string
}

// ============================================
// Blog Review (Product Integration)
// ============================================

export interface BlogReview {
  id: string
  title: string
  content: string
  authorId: string
  productId: string
}

export interface ProductWithReviews {
  id: number
  name: string
  description: string
  price: number
  stock: number
  reviews: BlogReview[]
}
