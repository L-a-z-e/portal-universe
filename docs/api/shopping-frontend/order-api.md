---
id: api-shopping-order
title: Shopping Order + Payment + Delivery API
type: api
status: current
version: v1
created: 2026-02-06
updated: 2026-02-08
author: Laze
tags: [api, shopping, frontend, order, payment, delivery, admin]
related: [api-shopping-types, api-shopping-cart]
---

# Shopping Order + Payment + Delivery API

> 주문, 결제, 배송 관리 API (공개 + 관리자)

---

## 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `/api/v1/shopping` |
| **인증** | Bearer Token (필수) |
| **엔드포인트** | `orderApi`, `paymentApi`, `deliveryApi`, `adminOrderApi`, `adminPaymentApi` |

---

## Order API

### 주문 목록 조회

```typescript
getOrders(page = 0, size = 10): Promise<ApiResponse<PagedResponse<Order>>>
```

**Endpoint**

```http
GET /api/v1/shopping/orders?page=1&size=10
Authorization: Bearer {token}
```

**Response**

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 1,
        "orderNumber": "ORD-20260206-001",
        "userId": "user123",
        "status": "PAID",
        "totalAmount": 70000,
        "discountAmount": 5000,
        "finalAmount": 65000,
        "appliedUserCouponId": 10,
        "items": [
          {
            "id": 1,
            "productId": 1,
            "productName": "스프링 부트 완벽 가이드",
            "price": 35000,
            "quantity": 2,
            "subtotal": 70000
          }
        ],
        "shippingAddress": {
          "receiverName": "홍길동",
          "receiverPhone": "010-1234-5678",
          "zipCode": "06234",
          "address1": "서울시 강남구 테헤란로 123",
          "address2": "ABC빌딩 5층"
        },
        "createdAt": "2026-02-06T09:00:00Z",
        "updatedAt": "2026-02-06T09:05:00Z"
      }
    ],
    "page": 1,
    "size": 10,
    "totalElements": 25,
    "totalPages": 3
  }
}
```

---

### 주문 상세 조회

```typescript
getOrder(orderNumber: string): Promise<ApiResponse<Order>>
```

**Endpoint**

```http
GET /api/v1/shopping/orders/{orderNumber}
Authorization: Bearer {token}
```

**Response**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "orderNumber": "ORD-20260206-001",
    "userId": "user123",
    "status": "PAID",
    "totalAmount": 70000,
    "discountAmount": 5000,
    "finalAmount": 65000,
    "appliedUserCouponId": 10,
    "items": [/*...*/],
    "shippingAddress": {/*...*/},
    "createdAt": "2026-02-06T09:00:00Z"
  }
}
```

---

### 주문 생성

```typescript
createOrder(data: CreateOrderRequest): Promise<ApiResponse<Order>>
```

**Endpoint**

```http
POST /api/v1/shopping/orders
Content-Type: application/json
Authorization: Bearer {token}

{
  "shippingAddress": {
    "receiverName": "홍길동",
    "receiverPhone": "010-1234-5678",
    "zipCode": "06234",
    "address1": "서울시 강남구 테헤란로 123",
    "address2": "ABC빌딩 5층"
  },
  "userCouponId": 10
}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `shippingAddress` | AddressRequest | ✅ | 배송지 정보 |
| `userCouponId` | number | ❌ | 적용할 쿠폰 ID |

**Response (201 Created)**

```json
{
  "success": true,
  "data": {
    "id": 2,
    "orderNumber": "ORD-20260206-123",
    "status": "PENDING",
    "totalAmount": 70000,
    "finalAmount": 65000,
    "createdAt": "2026-02-06T10:30:00Z"
  }
}
```

---

### 주문 취소

```typescript
cancelOrder(orderNumber: string, data: CancelOrderRequest): Promise<ApiResponse<Order>>
```

**Endpoint**

```http
POST /api/v1/shopping/orders/{orderNumber}/cancel
Content-Type: application/json
Authorization: Bearer {token}

{
  "reason": "고객 변심"
}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `reason` | string | ✅ | 취소 사유 |

**Response**

```json
{
  "success": true,
  "data": {
    "orderNumber": "ORD-20260206-123",
    "status": "CANCELLED",
    "cancelReason": "고객 변심",
    "cancelledAt": "2026-02-06T11:00:00Z"
  }
}
```

**Error (배송 시작됨)**

```json
{
  "success": false,
  "code": "ORDER_CANNOT_CANCEL",
  "message": "배송 시작된 주문은 취소할 수 없습니다."
}
```

---

## Payment API

### 결제 정보 조회

```typescript
getPayment(orderNumber: string): Promise<ApiResponse<Payment>>
```

**Endpoint**

```http
GET /api/v1/shopping/payments/{orderNumber}
Authorization: Bearer {token}
```

**Response**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "orderNumber": "ORD-20260206-001",
    "amount": 65000,
    "method": "CARD",
    "status": "COMPLETED",
    "transactionId": "TXN-20260206-ABC123",
    "paidAt": "2026-02-06T09:05:00Z",
    "createdAt": "2026-02-06T09:00:00Z"
  }
}
```

---

### 결제 처리

```typescript
processPayment(data: ProcessPaymentRequest): Promise<ApiResponse<Payment>>
```

**Endpoint**

```http
POST /api/v1/shopping/payments
Content-Type: application/json
Authorization: Bearer {token}

{
  "orderNumber": "ORD-20260206-123",
  "paymentMethod": "CARD",
  "cardNumber": "1234-5678-9012-3456",
  "cardExpiry": "12/28",
  "cardCvv": "123"
}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `orderNumber` | string | ✅ | 주문 번호 |
| `paymentMethod` | PaymentMethod | ✅ | 결제 수단 (CARD, BANK_TRANSFER, VIRTUAL_ACCOUNT, MOBILE, POINTS) |
| `cardNumber` | string | ❌ | 카드 번호 (method=CARD일 때 필수) |
| `cardExpiry` | string | ❌ | 카드 유효기간 |
| `cardCvv` | string | ❌ | CVV |

**Response**

```json
{
  "success": true,
  "data": {
    "id": 15,
    "orderNumber": "ORD-20260206-123",
    "amount": 65000,
    "method": "CARD",
    "status": "COMPLETED",
    "transactionId": "TXN-20260206-XYZ789",
    "paidAt": "2026-02-06T10:35:00Z"
  }
}
```

---

### 결제 취소

```typescript
cancelPayment(orderNumber: string): Promise<ApiResponse<Payment>>
```

**Endpoint**

```http
POST /api/v1/shopping/payments/{orderNumber}/cancel
Authorization: Bearer {token}
```

**Response**

```json
{
  "success": true,
  "data": {
    "orderNumber": "ORD-20260206-123",
    "status": "CANCELLED",
    "updatedAt": "2026-02-06T11:00:00Z"
  }
}
```

---

## Delivery API

### 주문별 배송 조회

```typescript
getDeliveryByOrder(orderNumber: string): Promise<ApiResponse<Delivery>>
```

**Endpoint**

```http
GET /api/v1/shopping/deliveries/order/{orderNumber}
Authorization: Bearer {token}
```

**Response**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "orderNumber": "ORD-20260206-001",
    "trackingNumber": "TRK-20260206-ABC",
    "carrier": "CJ대한통운",
    "status": "IN_TRANSIT",
    "shippingAddress": {
      "receiverName": "홍길동",
      "receiverPhone": "010-1234-5678",
      "zipCode": "06234",
      "address1": "서울시 강남구 테헤란로 123",
      "address2": "ABC빌딩 5층"
    },
    "histories": [
      {
        "id": 1,
        "status": "SHIPPED",
        "location": "서울 강남구 집하장",
        "description": "상품이 발송되었습니다.",
        "createdAt": "2026-02-06T14:00:00Z"
      },
      {
        "id": 2,
        "status": "IN_TRANSIT",
        "location": "서울 송파구 물류센터",
        "description": "배송 중입니다.",
        "createdAt": "2026-02-06T16:00:00Z"
      }
    ],
    "estimatedDeliveryDate": "2026-02-08T18:00:00Z",
    "createdAt": "2026-02-06T14:00:00Z"
  }
}
```

---

### 배송 추적 (운송장번호)

```typescript
trackDelivery(trackingNumber: string): Promise<ApiResponse<Delivery>>
```

**Endpoint**

```http
GET /api/v1/shopping/deliveries/{trackingNumber}
Authorization: Bearer {token}
```

**Response**

동일한 형식의 Delivery 객체를 반환합니다.

---

### 배송 상태 업데이트 (관리자/배송사)

```typescript
updateDeliveryStatus(trackingNumber: string, data: UpdateDeliveryStatusRequest): Promise<ApiResponse<Delivery>>
```

**Endpoint**

```http
PUT /api/v1/shopping/deliveries/{trackingNumber}/status
Content-Type: application/json
Authorization: Bearer {admin_token}

{
  "status": "DELIVERED",
  "location": "서울 강남구 테헤란로 123",
  "description": "배송이 완료되었습니다."
}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `status` | DeliveryStatus | ✅ | 배송 상태 |
| `location` | string | ❌ | 현재 위치 |
| `description` | string | ❌ | 상태 설명 |

**Response**

```json
{
  "success": true,
  "data": {
    "trackingNumber": "TRK-20260206-ABC",
    "status": "DELIVERED",
    "actualDeliveryDate": "2026-02-08T15:30:00Z"
  }
}
```

---

## Admin Order API

### 관리자 주문 목록 조회

```typescript
getOrders(params: {
  page?: number
  size?: number
  status?: string
  keyword?: string
}): Promise<ApiResponse<PagedResponse<Order>>>
```

**Endpoint**

```http
GET /api/v1/shopping/admin/orders?page=1&size=20&status=PAID
Authorization: Bearer {admin_token}
```

**Query Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `page` | number | 페이지 번호 |
| `size` | number | 페이지 크기 |
| `status` | string | 주문 상태 필터 |
| `keyword` | string | 검색 키워드 (주문번호, 사용자명) |

---

### 관리자 주문 상세 조회

```typescript
getOrder(orderNumber: string): Promise<ApiResponse<Order>>
```

**Endpoint**

```http
GET /api/v1/shopping/admin/orders/{orderNumber}
Authorization: Bearer {admin_token}
```

---

### 관리자 주문 상태 변경

```typescript
updateOrderStatus(orderNumber: string, status: string): Promise<ApiResponse<Order>>
```

**Endpoint**

```http
PUT /api/v1/shopping/admin/orders/{orderNumber}/status
Content-Type: application/json
Authorization: Bearer {admin_token}

{
  "status": "SHIPPING"
}
```

**Response**

```json
{
  "success": true,
  "data": {
    "orderNumber": "ORD-20260206-001",
    "status": "SHIPPING",
    "updatedAt": "2026-02-06T12:00:00Z"
  }
}
```

---

## Admin Payment API

### 환불 처리

```typescript
refundPayment(paymentNumber: string): Promise<ApiResponse<Payment>>
```

**Endpoint**

```http
POST /api/v1/shopping/payments/{paymentNumber}/refund
Authorization: Bearer {admin_token}
```

**Response**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "orderNumber": "ORD-20260206-001",
    "status": "REFUNDED",
    "amount": 65000,
    "updatedAt": "2026-02-06T13:00:00Z"
  }
}
```

---

## React Hooks

### useAdminOrders

```typescript
import { useAdminOrders } from '@/hooks/useAdminOrders'

export function OrderManagementPage() {
  const [filters, setFilters] = useState({
    page: 0,
    size: 20,
    status: '',
    keyword: ''
  })

  const { data, isLoading, error } = useAdminOrders(filters)

  return (
    <div>
      {data?.content.map((order) => (
        <OrderCard key={order.id} order={order} />
      ))}
    </div>
  )
}
```

### useUpdateOrderStatus

```typescript
import { useUpdateOrderStatus } from '@/hooks/useAdminOrders'

export function OrderStatusButton({ orderNumber }: { orderNumber: string }) {
  const { mutateAsync, isPending } = useUpdateOrderStatus()

  const handleUpdateStatus = async (status: string) => {
    try {
      await mutateAsync(orderNumber, status)
      alert('상태가 변경되었습니다')
    } catch (error) {
      alert('상태 변경 실패')
    }
  }

  return (
    <button onClick={() => handleUpdateStatus('SHIPPING')} disabled={isPending}>
      배송 시작
    </button>
  )
}
```

### useRefundPayment

```typescript
import { useRefundPayment } from '@/hooks/useAdminPayments'

export function RefundButton({ paymentNumber }: { paymentNumber: string }) {
  const { mutateAsync, isPending } = useRefundPayment()

  const handleRefund = async () => {
    if (!confirm('정말 환불하시겠습니까?')) return

    try {
      await mutateAsync(paymentNumber)
      alert('환불이 완료되었습니다')
    } catch (error) {
      alert('환불 실패')
    }
  }

  return (
    <button onClick={handleRefund} disabled={isPending}>
      {isPending ? '환불 중...' : '환불'}
    </button>
  )
}
```

### useDeliveryByOrder

```typescript
import { useDeliveryByOrder } from '@/hooks/useAdminDelivery'

export function DeliveryTrackingCard({ orderNumber }: { orderNumber: string }) {
  const { data, isLoading, error } = useDeliveryByOrder(orderNumber)

  if (isLoading) return <div>로딩 중...</div>
  if (error || !data) return <div>배송 정보 없음</div>

  return (
    <div>
      <h3>배송 추적: {data.trackingNumber}</h3>
      <p>상태: {DELIVERY_STATUS_LABELS[data.status]}</p>
      <div>
        {data.histories.map((history) => (
          <div key={history.id}>
            <p>{history.description} - {history.location}</p>
            <small>{history.createdAt}</small>
          </div>
        ))}
      </div>
    </div>
  )
}
```

---

## 에러 코드

| Code | HTTP Status | 설명 |
|------|-------------|------|
| `ORDER_NOT_FOUND` | 404 | 주문을 찾을 수 없음 |
| `ORDER_CANNOT_CANCEL` | 400 | 주문 취소 불가 (배송 시작됨) |
| `PAYMENT_NOT_FOUND` | 404 | 결제 정보 없음 |
| `PAYMENT_FAILED` | 400 | 결제 처리 실패 |
| `PAYMENT_AMOUNT_MISMATCH` | 400 | 결제 금액 불일치 |
| `DELIVERY_NOT_FOUND` | 404 | 배송 정보 없음 |

---

## 관련 문서

- [Client API](./client-api.md)
- [Cart API](./cart-api.md)
- [Coupon API](./coupon-api.md)
- [공통 타입 정의](./types.md)

---

**최종 업데이트**: 2026-02-06
