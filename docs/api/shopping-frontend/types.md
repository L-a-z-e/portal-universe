---
id: api-shopping-types
title: Shopping Frontend Type Definitions
type: api
status: current
version: v1
created: 2026-02-06
updated: 2026-02-17
author: Laze
tags: [api, shopping, frontend, typescript, types]
related: [api-shopping-client]
---

# Shopping Frontend 타입 정의

> shopping-frontend에서 사용하는 TypeScript 타입 정의 (실제 코드 기반)

---

## 개요

타입 정의 파일 구조 (DDD 기반):
- `src/dto/*.ts` - 도메인별 DTO (product, cart, order, payment 등 12개)
- `src/types/index.ts` - barrel re-export (dto/* + ui + common)
- `src/types/common.ts` - `@portal/design-types` re-export
- `src/types/ui.ts` - UI 공통 타입 (ToastMessage, ModalState, TableColumn 등)

---

## 공통 타입

### ApiResponse

```typescript
export interface ApiResponse<T> {
  success: boolean
  data: T
  message?: string
  code?: string
  timestamp: string
}

export interface ApiErrorResponse {
  success: false
  code: string
  message: string
  data: null | { errors?: FieldError[] }
  timestamp: string
}

export interface FieldError {
  field: string
  message: string
  rejectedValue?: any
}
```

### PagedResponse

```typescript
export interface PageInfo {
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface PagedResponse<T> {
  content: T[]
  pageable: PageInfo
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
  empty: boolean
}
```

### Address

```typescript
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
```

---

## Product 타입

### Product

```typescript
export interface Product {
  id: number
  name: string
  description: string
  price: number
  discountPrice?: number     // 할인가 (null이면 할인 없음)
  stockQuantity?: number     // optional, 재고 정보는 Inventory에서 관리
  imageUrl?: string
  images?: string[]          // 다중 이미지 URL 리스트
  category?: string
  featured?: boolean         // 추천 상품 여부 (BESTSELLER 배지용)
  averageRating?: number     // 리뷰 평균 평점 (상세 조회 시만, 목록에서는 null)
  reviewCount?: number       // 리뷰 수 (상세 조회 시만, 목록에서는 null)
  createdAt: string
  updatedAt?: string
}
```

### Product Request

```typescript
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
```

### Admin Product Types

```typescript
export type ProductStatus = 'ACTIVE' | 'INACTIVE' | 'OUT_OF_STOCK'

export interface AdminProduct extends Product {
  stock: number
  status?: ProductStatus
}

export interface ProductFilters {
  page: number
  size: number
  keyword?: string
  category?: string
  status?: ProductStatus
  sortBy?: 'name' | 'price' | 'createdAt'
  sortOrder?: 'asc' | 'desc'
}

export interface ProductFormData {
  name: string
  description: string
  price: number
  stock: number
  imageUrl?: string
  category?: string
}
```

---

## Inventory 타입

### Inventory

```typescript
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
```

### Stock Movement

```typescript
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
```

### Inventory Stream (SSE)

```typescript
export interface InventoryUpdate {
  productId: number
  available: number
  reserved: number
  timestamp: string
}
```

---

## Cart 타입

### Cart

```typescript
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
```

### Cart Request

```typescript
export interface AddCartItemRequest {
  productId: number
  quantity: number
}

export interface UpdateCartItemRequest {
  quantity: number
}
```

---

## Order 타입

### Order

```typescript
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
```

### Order Request

```typescript
export interface CreateOrderRequest {
  shippingAddress: AddressRequest
  userCouponId?: number
}

export interface CancelOrderRequest {
  reason: string
}
```

### Order Status Labels

```typescript
export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING: '주문 대기',
  CONFIRMED: '주문 확정',
  PAID: '결제 완료',
  SHIPPING: '배송 중',
  DELIVERED: '배송 완료',
  CANCELLED: '주문 취소',
  REFUNDED: '환불 완료'
}
```

---

## Payment 타입

### Payment

```typescript
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
```

### Payment Request

```typescript
export interface ProcessPaymentRequest {
  orderNumber: string
  paymentMethod: PaymentMethod
  cardNumber?: string
  cardExpiry?: string
  cardCvv?: string
}
```

### Payment Labels

```typescript
export const PAYMENT_STATUS_LABELS: Record<PaymentStatus, string> = {
  PENDING: '결제 대기',
  PROCESSING: '결제 처리 중',
  COMPLETED: '결제 완료',
  FAILED: '결제 실패',
  CANCELLED: '결제 취소',
  REFUNDED: '환불 완료'
}

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CARD: '카드',
  BANK_TRANSFER: '계좌이체',
  VIRTUAL_ACCOUNT: '가상계좌',
  MOBILE: '휴대폰',
  POINTS: '포인트'
}
```

---

## Delivery 타입

### Delivery

```typescript
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
```

### Delivery Request

```typescript
export interface UpdateDeliveryStatusRequest {
  status: DeliveryStatus
  location?: string
  description?: string
}
```

### Delivery Status Labels

```typescript
export const DELIVERY_STATUS_LABELS: Record<DeliveryStatus, string> = {
  PENDING: '배송 준비 대기',
  PREPARING: '상품 준비 중',
  SHIPPED: '출고 완료',
  IN_TRANSIT: '배송 중',
  OUT_FOR_DELIVERY: '배달 출발',
  DELIVERED: '배송 완료',
  FAILED: '배송 실패'
}
```

---

## Coupon 타입

### Coupon

```typescript
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
```

### Coupon Request

```typescript
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
```

### Coupon Labels

```typescript
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
```

---

## TimeDeal 타입

### TimeDeal

```typescript
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

export interface TimeDealPurchase {
  id: number
  timeDealProductId: number
  productName: string
  quantity: number
  purchasePrice: number
  totalPrice: number
  purchasedAt: string
}
```

### TimeDeal Request

```typescript
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
```

### TimeDeal Status Labels

```typescript
export const TIMEDEAL_STATUS_LABELS: Record<TimeDealStatus, string> = {
  SCHEDULED: '예정',
  ACTIVE: '진행 중',
  ENDED: '종료',
  SOLD_OUT: '품절',
  CANCELLED: '취소'
}
```

---

## Queue 타입

### Queue

```typescript
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
```

### Queue Status Labels

```typescript
export const QUEUE_STATUS_LABELS: Record<QueueStatus, string> = {
  WAITING: '대기 중',
  ENTERED: '입장 완료',
  EXPIRED: '만료됨',
  LEFT: '이탈'
}
```

---

## Search 타입

### Search

```typescript
export interface SearchSuggestion {
  keyword: string
}
```

---

## Review 타입 (Blog Integration)

### Blog Review

```typescript
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
```

---

## UI Helper 타입 (Admin)

### Admin UI Types

```typescript
export interface ToastMessage {
  id: string
  type: 'success' | 'error' | 'warning' | 'info'
  title: string
  message?: string
  duration?: number
}

export interface ModalState {
  isOpen: boolean
  type: 'delete' | 'confirm' | null
  data?: any
}

export interface SelectOption {
  value: string
  label: string
}
```

### Table Types

```typescript
export interface TableColumn<T> {
  key: keyof T | string
  header: string
  width?: string | number
  sortable?: boolean
  render?: (value: any, row: T) => React.ReactNode
}

export interface TableAction<T> {
  label: string
  icon?: React.ReactNode
  onClick: (row: T) => void
  variant?: 'primary' | 'danger' | 'default'
  visible?: (row: T) => boolean
}
```

### Component Props Types

```typescript
export interface ConfirmModalProps {
  isOpen: boolean
  title: string
  message: string | React.ReactNode
  confirmText?: string
  cancelText?: string
  variant?: 'danger' | 'warning' | 'default'
  onConfirm: () => void | Promise<void>
  onCancel: () => void
  loading?: boolean
}

export interface PaginationProps {
  currentPage: number
  totalPages: number
  onPageChange: (page: number) => void
  disabled?: boolean
}

export interface ProductFormProps {
  mode: 'create' | 'edit'
  initialData?: Partial<ProductFormData>
  onSubmit: (data: ProductFormData) => void | Promise<void>
  onCancel: () => void
  isSubmitting?: boolean
}
```

---

## Auth 타입

### User

```typescript
export interface User {
  id: string
  email: string
  name: string
  roles: string[]
  avatar?: string
}

export type UserRole = 'ROLE_USER' | 'ROLE_ADMIN'
```

---

## 사용 예시

### 타입 가져오기

```typescript
import type {
  Product,
  Cart,
  Order,
  Coupon,
  TimeDeal,
  ApiResponse,
  PagedResponse
  ProductFilters,
  ProductFormData,
  AdminProduct
} from '@/types'
```

### 타입 활용

```typescript
// API 응답 타입
const response: ApiResponse<Product> = await productApi.getProduct(1)

// 페이지네이션 응답
const pagedResponse: PagedResponse<Product> = response.data

// 상태 관리
const [products, setProducts] = useState<Product[]>([])
const [order, setOrder] = useState<Order | null>(null)

// 함수 파라미터 타입
const createProduct = async (data: ProductCreateRequest): Promise<Product> => {
  const response = await adminProductApi.createProduct(data)
  return response.data
}
```

---

## 관련 문서

- [Client API](./client-api.md)
- [Product API](./product-api.md)
- [Cart API](./cart-api.md)
- [Order API](./order-api.md)
- [Coupon API](./coupon-api.md)
- [TimeDeal API](./timedeal-api.md)
- [Queue API](./queue-api.md)

---

**최종 업데이트**: 2026-02-17
