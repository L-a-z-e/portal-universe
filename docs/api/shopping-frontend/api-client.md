---
id: api-shopping-client
title: Shopping API Client
type: api
status: current
version: v1
created: 2026-01-18
updated: 2026-01-18
author: Documenter Agent
tags: [api, shopping, frontend, client]
related: []
---

# Shopping API Client

> shopping-frontend에서 사용하는 API 클라이언트 명세

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `/api/v1/shopping` |
| **인증** | Bearer Token (JWT) |
| **버전** | v1 |
| **Content-Type** | `application/json` |

shopping-frontend는 API Gateway를 통해 shopping-service와 통신합니다. 모든 요청은 JWT 토큰을 포함해야 하며, axios interceptor를 통해 자동으로 헤더에 추가됩니다.

---

## 📑 API 그룹 목록

| 그룹 | 설명 | 엔드포인트 수 |
|------|------|---------------|
| [Product API](#-product-api) | 상품 관리 (CRUD, 검색) | 6 |
| [Inventory API](#-inventory-api) | 재고 관리 | 3 |
| [Cart API](#-cart-api) | 장바구니 관리 | 5 |
| [Order API](#-order-api) | 주문 관리 | 4 |
| [Payment API](#-payment-api) | 결제 처리 | 3 |
| [Delivery API](#-delivery-api) | 배송 조회/추적 | 3 |

---

## 📦 Product API

상품 정보 조회, 검색, 관리 기능을 제공합니다.

### 🔹 상품 목록 조회

```typescript
getProducts(params?: {
  page?: number
  size?: number
  category?: string
}): Promise<ApiResponse<PagedResponse<Product>>>
```

**Request**

```http
GET /api/v1/shopping/products?page=0&size=20&category=electronics
Authorization: Bearer {token}
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `page` | number | ❌ | 페이지 번호 (0부터 시작) | 0 |
| `size` | number | ❌ | 페이지 크기 | 20 |
| `category` | string | ❌ | 카테고리 필터 | - |

**Response (200 OK)**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "스프링 부트 완벽 가이드",
        "description": "Spring Boot 실전 가이드",
        "price": 35000,
        "category": "books",
        "stock": 50,
        "imageUrl": "https://cdn.example.com/products/1.jpg",
        "createdAt": "2026-01-10T10:00:00Z",
        "updatedAt": "2026-01-15T14:30:00Z"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "sort": { "sorted": false },
      "offset": 0,
      "paged": true,
      "unpaged": false
    },
    "totalElements": 150,
    "totalPages": 8,
    "last": false,
    "size": 20,
    "number": 0,
    "first": true,
    "numberOfElements": 20,
    "empty": false
  },
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

### 🔹 상품 상세 조회

```typescript
getProduct(id: number): Promise<ApiResponse<Product>>
```

**Request**

```http
GET /api/v1/shopping/products/{id}
Authorization: Bearer {token}
```

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | number | 상품 ID |

**Response (200 OK)**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "스프링 부트 완벽 가이드",
    "description": "Spring Boot 3.x 기반 마이크로서비스 구축 실전 가이드",
    "price": 35000,
    "category": "books",
    "stock": 50,
    "imageUrl": "https://cdn.example.com/products/1.jpg",
    "tags": ["spring", "java", "backend"],
    "rating": 4.5,
    "reviewCount": 120,
    "createdAt": "2026-01-10T10:00:00Z",
    "updatedAt": "2026-01-15T14:30:00Z"
  },
  "timestamp": "2026-01-18T10:30:00Z"
}
```

**Error Response (404 Not Found)**

```json
{
  "success": false,
  "code": "S001",
  "message": "상품을 찾을 수 없습니다.",
  "data": null,
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

### 🔹 상품 검색

```typescript
searchProducts(params: {
  keyword: string
  page?: number
  size?: number
}): Promise<ApiResponse<PagedResponse<Product>>>
```

**Request**

```http
GET /api/v1/shopping/products/search?keyword=spring&page=0&size=10
Authorization: Bearer {token}
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `keyword` | string | ✅ | 검색 키워드 | - |
| `page` | number | ❌ | 페이지 번호 | 0 |
| `size` | number | ❌ | 페이지 크기 | 10 |

**Response (200 OK)**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "스프링 부트 완벽 가이드",
        "price": 35000,
        "category": "books"
      }
    ],
    "totalElements": 15,
    "totalPages": 2
  },
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

### 🔹 상품 생성 (관리자)

```typescript
createProduct(data: CreateProductRequest): Promise<ApiResponse<Product>>
```

**Request**

```http
POST /api/v1/shopping/products
Content-Type: application/json
Authorization: Bearer {admin_token}

{
  "name": "Vue 3 마스터하기",
  "description": "Vue 3 Composition API 완벽 가이드",
  "price": 32000,
  "category": "books",
  "stock": 30,
  "imageUrl": "https://cdn.example.com/products/new.jpg"
}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `name` | string | ✅ | 상품명 | 1~200자 |
| `description` | string | ❌ | 상세 설명 | 최대 2000자 |
| `price` | number | ✅ | 가격 | 양수, 최대 10,000,000 |
| `category` | string | ✅ | 카테고리 | 사전 정의된 값 |
| `stock` | number | ✅ | 재고 수량 | 0 이상 |
| `imageUrl` | string | ❌ | 이미지 URL | 유효한 URL |

**Response (201 Created)**

```json
{
  "success": true,
  "data": {
    "id": 51,
    "name": "Vue 3 마스터하기",
    "price": 32000,
    "category": "books",
    "stock": 30,
    "createdAt": "2026-01-18T10:30:00Z"
  },
  "message": "상품이 성공적으로 생성되었습니다.",
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

### 🔹 상품 수정 (관리자)

```typescript
updateProduct(id: number, data: UpdateProductRequest): Promise<ApiResponse<Product>>
```

**Request**

```http
PUT /api/v1/shopping/products/{id}
Content-Type: application/json
Authorization: Bearer {admin_token}

{
  "name": "Vue 3 마스터하기 (개정판)",
  "price": 35000
}
```

**Response (200 OK)**

```json
{
  "success": true,
  "data": {
    "id": 51,
    "name": "Vue 3 마스터하기 (개정판)",
    "price": 35000,
    "updatedAt": "2026-01-18T11:00:00Z"
  },
  "timestamp": "2026-01-18T11:00:00Z"
}
```

---

### 🔹 상품 삭제 (관리자)

```typescript
deleteProduct(id: number): Promise<ApiResponse<void>>
```

**Request**

```http
DELETE /api/v1/shopping/products/{id}
Authorization: Bearer {admin_token}
```

**Response (204 No Content)**

```json
{
  "success": true,
  "message": "상품이 삭제되었습니다.",
  "timestamp": "2026-01-18T11:00:00Z"
}
```

---

## 📊 Inventory API

상품 재고 조회 및 관리 기능을 제공합니다.

### 🔹 재고 조회

```typescript
getInventory(productId: number): Promise<ApiResponse<Inventory>>
```

**Request**

```http
GET /api/v1/shopping/inventory/{productId}
Authorization: Bearer {token}
```

**Response (200 OK)**

```json
{
  "success": true,
  "data": {
    "productId": 1,
    "stock": 50,
    "reserved": 5,
    "available": 45,
    "lastUpdated": "2026-01-18T10:00:00Z"
  },
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

### 🔹 여러 상품 재고 조회

```typescript
getInventories(productIds: number[]): Promise<ApiResponse<Inventory[]>>
```

**Request**

```http
POST /api/v1/shopping/inventory/batch
Content-Type: application/json
Authorization: Bearer {token}

{
  "productIds": [1, 2, 3, 5]
}
```

**Response (200 OK)**

```json
{
  "success": true,
  "data": [
    {
      "productId": 1,
      "stock": 50,
      "available": 45
    },
    {
      "productId": 2,
      "stock": 30,
      "available": 28
    }
  ],
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

### 🔹 재고 추가 (관리자)

```typescript
addStock(productId: number, quantity: number): Promise<ApiResponse<Inventory>>
```

**Request**

```http
POST /api/v1/shopping/inventory/{productId}/add
Content-Type: application/json
Authorization: Bearer {admin_token}

{
  "quantity": 20
}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `quantity` | number | ✅ | 추가 수량 | 1 이상 |

**Response (200 OK)**

```json
{
  "success": true,
  "data": {
    "productId": 1,
    "stock": 70,
    "available": 65,
    "lastUpdated": "2026-01-18T11:00:00Z"
  },
  "message": "재고가 추가되었습니다.",
  "timestamp": "2026-01-18T11:00:00Z"
}
```

---

## 🛒 Cart API

사용자 장바구니 관리 기능을 제공합니다.

### 🔹 장바구니 조회

```typescript
getCart(): Promise<ApiResponse<Cart>>
```

**Request**

```http
GET /api/v1/shopping/cart
Authorization: Bearer {token}
```

**Response (200 OK)**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 123,
    "items": [
      {
        "id": 1,
        "productId": 1,
        "productName": "스프링 부트 완벽 가이드",
        "quantity": 2,
        "price": 35000,
        "subtotal": 70000
      },
      {
        "id": 2,
        "productId": 5,
        "productName": "Vue 3 마스터하기",
        "quantity": 1,
        "price": 32000,
        "subtotal": 32000
      }
    ],
    "totalItems": 2,
    "totalQuantity": 3,
    "totalAmount": 102000,
    "updatedAt": "2026-01-18T10:00:00Z"
  },
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

### 🔹 상품 추가

```typescript
addItem(data: AddCartItemRequest): Promise<ApiResponse<CartItem>>
```

**Request**

```http
POST /api/v1/shopping/cart/items
Content-Type: application/json
Authorization: Bearer {token}

{
  "productId": 10,
  "quantity": 2
}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `productId` | number | ✅ | 상품 ID | 유효한 상품 ID |
| `quantity` | number | ✅ | 수량 | 1 이상, 재고 이하 |

**Response (201 Created)**

```json
{
  "success": true,
  "data": {
    "id": 3,
    "productId": 10,
    "productName": "React 완벽 가이드",
    "quantity": 2,
    "price": 38000,
    "subtotal": 76000
  },
  "message": "장바구니에 추가되었습니다.",
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

### 🔹 수량 변경

```typescript
updateItem(itemId: number, quantity: number): Promise<ApiResponse<CartItem>>
```

**Request**

```http
PUT /api/v1/shopping/cart/items/{itemId}
Content-Type: application/json
Authorization: Bearer {token}

{
  "quantity": 5
}
```

**Response (200 OK)**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "quantity": 5,
    "subtotal": 175000
  },
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

### 🔹 상품 삭제

```typescript
removeItem(itemId: number): Promise<ApiResponse<void>>
```

**Request**

```http
DELETE /api/v1/shopping/cart/items/{itemId}
Authorization: Bearer {token}
```

**Response (204 No Content)**

```json
{
  "success": true,
  "message": "장바구니에서 삭제되었습니다.",
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

### 🔹 장바구니 비우기

```typescript
clearCart(): Promise<ApiResponse<void>>
```

**Request**

```http
DELETE /api/v1/shopping/cart
Authorization: Bearer {token}
```

**Response (204 No Content)**

```json
{
  "success": true,
  "message": "장바구니가 비워졌습니다.",
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

## 📦 Order API

주문 생성 및 조회 기능을 제공합니다.

### 🔹 주문 목록 조회

```typescript
getOrders(params?: {
  page?: number
  size?: number
}): Promise<ApiResponse<PagedResponse<Order>>>
```

**Request**

```http
GET /api/v1/shopping/orders?page=0&size=10
Authorization: Bearer {token}
```

**Response (200 OK)**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "orderNumber": "ORD-20260118-001",
        "status": "COMPLETED",
        "totalAmount": 102000,
        "itemCount": 3,
        "orderDate": "2026-01-18T09:00:00Z"
      }
    ],
    "totalElements": 25,
    "totalPages": 3
  },
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

### 🔹 주문 상세 조회

```typescript
getOrder(orderNumber: string): Promise<ApiResponse<Order>>
```

**Request**

```http
GET /api/v1/shopping/orders/{orderNumber}
Authorization: Bearer {token}
```

**Response (200 OK)**

```json
{
  "success": true,
  "data": {
    "orderNumber": "ORD-20260118-001",
    "userId": 123,
    "status": "COMPLETED",
    "items": [
      {
        "productId": 1,
        "productName": "스프링 부트 완벽 가이드",
        "quantity": 2,
        "price": 35000,
        "subtotal": 70000
      }
    ],
    "subtotal": 102000,
    "shippingFee": 3000,
    "totalAmount": 105000,
    "shippingAddress": {
      "recipient": "홍길동",
      "phone": "010-1234-5678",
      "address": "서울시 강남구 테헤란로 123",
      "zipCode": "06234"
    },
    "orderDate": "2026-01-18T09:00:00Z",
    "paidAt": "2026-01-18T09:05:00Z"
  },
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

### 🔹 주문 생성

```typescript
createOrder(data: CreateOrderRequest): Promise<ApiResponse<Order>>
```

**Request**

```http
POST /api/v1/shopping/orders
Content-Type: application/json
Authorization: Bearer {token}

{
  "items": [
    {
      "productId": 1,
      "quantity": 2
    }
  ],
  "shippingAddress": {
    "recipient": "홍길동",
    "phone": "010-1234-5678",
    "address": "서울시 강남구 테헤란로 123",
    "zipCode": "06234"
  }
}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `items` | OrderItem[] | ✅ | 주문 상품 목록 |
| `items[].productId` | number | ✅ | 상품 ID |
| `items[].quantity` | number | ✅ | 수량 |
| `shippingAddress` | Address | ✅ | 배송지 정보 |
| `shippingAddress.recipient` | string | ✅ | 수령인 |
| `shippingAddress.phone` | string | ✅ | 연락처 |
| `shippingAddress.address` | string | ✅ | 주소 |
| `shippingAddress.zipCode` | string | ✅ | 우편번호 |

**Response (201 Created)**

```json
{
  "success": true,
  "data": {
    "orderNumber": "ORD-20260118-123",
    "status": "PENDING",
    "totalAmount": 105000,
    "orderDate": "2026-01-18T10:30:00Z"
  },
  "message": "주문이 생성되었습니다.",
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

### 🔹 주문 취소

```typescript
cancelOrder(orderNumber: string): Promise<ApiResponse<void>>
```

**Request**

```http
POST /api/v1/shopping/orders/{orderNumber}/cancel
Authorization: Bearer {token}
```

**Response (200 OK)**

```json
{
  "success": true,
  "message": "주문이 취소되었습니다.",
  "timestamp": "2026-01-18T10:30:00Z"
}
```

**Error Response (400 Bad Request)**

```json
{
  "success": false,
  "code": "ORDER_CANNOT_CANCEL",
  "message": "배송 시작된 주문은 취소할 수 없습니다.",
  "data": null,
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

## 💳 Payment API

결제 처리 및 조회 기능을 제공합니다.

### 🔹 결제 조회

```typescript
getPayment(orderNumber: string): Promise<ApiResponse<Payment>>
```

**Request**

```http
GET /api/v1/shopping/payments/{orderNumber}
Authorization: Bearer {token}
```

**Response (200 OK)**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "orderNumber": "ORD-20260118-001",
    "amount": 105000,
    "method": "CARD",
    "status": "COMPLETED",
    "paidAt": "2026-01-18T09:05:00Z",
    "transactionId": "TXN-20260118-ABC123"
  },
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

### 🔹 결제 처리

```typescript
processPayment(data: PaymentRequest): Promise<ApiResponse<Payment>>
```

**Request**

```http
POST /api/v1/shopping/payments
Content-Type: application/json
Authorization: Bearer {token}

{
  "orderNumber": "ORD-20260118-123",
  "method": "CARD",
  "amount": 105000,
  "cardInfo": {
    "number": "1234-5678-9012-3456",
    "expiry": "12/28",
    "cvv": "123"
  }
}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `orderNumber` | string | ✅ | 주문 번호 | 유효한 주문 번호 |
| `method` | string | ✅ | 결제 수단 | CARD, BANK, KAKAO, NAVER |
| `amount` | number | ✅ | 결제 금액 | 주문 금액과 일치 |
| `cardInfo` | object | ❌ | 카드 정보 | method=CARD일 때 필수 |

**Response (200 OK)**

```json
{
  "success": true,
  "data": {
    "id": 15,
    "orderNumber": "ORD-20260118-123",
    "amount": 105000,
    "method": "CARD",
    "status": "COMPLETED",
    "paidAt": "2026-01-18T10:35:00Z",
    "transactionId": "TXN-20260118-XYZ789"
  },
  "message": "결제가 완료되었습니다.",
  "timestamp": "2026-01-18T10:35:00Z"
}
```

---

### 🔹 결제 취소

```typescript
cancelPayment(orderNumber: string): Promise<ApiResponse<void>>
```

**Request**

```http
POST /api/v1/shopping/payments/{orderNumber}/cancel
Authorization: Bearer {token}
```

**Response (200 OK)**

```json
{
  "success": true,
  "message": "결제가 취소되었습니다.",
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

## 🚚 Delivery API

배송 조회 및 추적 기능을 제공합니다.

### 🔹 주문별 배송 조회

```typescript
getDeliveryByOrder(orderNumber: string): Promise<ApiResponse<Delivery>>
```

**Request**

```http
GET /api/v1/shopping/deliveries/order/{orderNumber}
Authorization: Bearer {token}
```

**Response (200 OK)**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "orderNumber": "ORD-20260118-001",
    "trackingNumber": "TRK-20260118-ABC",
    "status": "IN_TRANSIT",
    "courier": "CJ대한통운",
    "estimatedDelivery": "2026-01-20T18:00:00Z",
    "shippedAt": "2026-01-18T14:00:00Z"
  },
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

### 🔹 배송 추적

```typescript
trackDelivery(trackingNumber: string): Promise<ApiResponse<DeliveryTracking>>
```

**Request**

```http
GET /api/v1/shopping/deliveries/{trackingNumber}
Authorization: Bearer {token}
```

**Response (200 OK)**

```json
{
  "success": true,
  "data": {
    "trackingNumber": "TRK-20260118-ABC",
    "status": "IN_TRANSIT",
    "currentLocation": "서울 송파구 물류센터",
    "history": [
      {
        "status": "SHIPPED",
        "location": "서울 강남구 집하장",
        "timestamp": "2026-01-18T14:00:00Z",
        "description": "상품이 발송되었습니다."
      },
      {
        "status": "IN_TRANSIT",
        "location": "서울 송파구 물류센터",
        "timestamp": "2026-01-18T16:00:00Z",
        "description": "배송 중입니다."
      }
    ],
    "estimatedDelivery": "2026-01-20T18:00:00Z"
  },
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

### 🔹 배송 상태 업데이트 (관리자/시스템)

```typescript
updateDeliveryStatus(trackingNumber: string, status: string): Promise<ApiResponse<Delivery>>
```

**Request**

```http
PUT /api/v1/shopping/deliveries/{trackingNumber}/status
Content-Type: application/json
Authorization: Bearer {system_token}

{
  "status": "DELIVERED",
  "location": "서울 강남구 테헤란로 123",
  "description": "배송이 완료되었습니다."
}
```

**Response (200 OK)**

```json
{
  "success": true,
  "data": {
    "trackingNumber": "TRK-20260118-ABC",
    "status": "DELIVERED",
    "deliveredAt": "2026-01-20T15:30:00Z"
  },
  "timestamp": "2026-01-20T15:30:00Z"
}
```

---

## 📐 공통 타입 정의

### ApiResponse<T>

```typescript
interface ApiResponse<T> {
  success: boolean
  data: T | null
  message?: string
  code?: string
  timestamp: string
}
```

### PagedResponse<T>

```typescript
interface PagedResponse<T> {
  content: T[]
  pageable: {
    pageNumber: number
    pageSize: number
    sort: {
      sorted: boolean
      unsorted: boolean
      empty: boolean
    }
    offset: number
    paged: boolean
    unpaged: boolean
  }
  totalElements: number
  totalPages: number
  last: boolean
  size: number
  number: number
  sort: {
    sorted: boolean
    unsorted: boolean
    empty: boolean
  }
  first: boolean
  numberOfElements: number
  empty: boolean
}
```

### Product

```typescript
interface Product {
  id: number
  name: string
  description?: string
  price: number
  category: string
  stock: number
  imageUrl?: string
  tags?: string[]
  rating?: number
  reviewCount?: number
  createdAt: string
  updatedAt: string
}
```

### Cart

```typescript
interface Cart {
  id: number
  userId: number
  items: CartItem[]
  totalItems: number
  totalQuantity: number
  totalAmount: number
  updatedAt: string
}

interface CartItem {
  id: number
  productId: number
  productName: string
  quantity: number
  price: number
  subtotal: number
}
```

### Order

```typescript
interface Order {
  orderNumber: string
  userId: number
  status: OrderStatus
  items: OrderItem[]
  subtotal: number
  shippingFee: number
  totalAmount: number
  shippingAddress: Address
  orderDate: string
  paidAt?: string
  canceledAt?: string
}

interface OrderItem {
  productId: number
  productName: string
  quantity: number
  price: number
  subtotal: number
}

interface Address {
  recipient: string
  phone: string
  address: string
  zipCode: string
}

type OrderStatus = 'PENDING' | 'PAID' | 'SHIPPED' | 'DELIVERED' | 'CANCELED'
```

### Payment

```typescript
interface Payment {
  id: number
  orderNumber: string
  amount: number
  method: PaymentMethod
  status: PaymentStatus
  paidAt?: string
  canceledAt?: string
  transactionId?: string
}

type PaymentMethod = 'CARD' | 'BANK' | 'KAKAO' | 'NAVER'
type PaymentStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'CANCELED'
```

### Delivery

```typescript
interface Delivery {
  id: number
  orderNumber: string
  trackingNumber: string
  status: DeliveryStatus
  courier: string
  estimatedDelivery?: string
  shippedAt?: string
  deliveredAt?: string
}

interface DeliveryTracking {
  trackingNumber: string
  status: DeliveryStatus
  currentLocation: string
  history: DeliveryEvent[]
  estimatedDelivery?: string
}

interface DeliveryEvent {
  status: DeliveryStatus
  location: string
  timestamp: string
  description: string
}

type DeliveryStatus = 'PREPARING' | 'SHIPPED' | 'IN_TRANSIT' | 'OUT_FOR_DELIVERY' | 'DELIVERED' | 'FAILED'
```

### Inventory

```typescript
interface Inventory {
  productId: number
  stock: number
  reserved: number
  available: number
  lastUpdated: string
}
```

---

## ⚠️ 에러 코드

### 공통 에러

| Code | HTTP Status | 설명 |
|------|-------------|------|
| `UNAUTHORIZED` | 401 | 인증되지 않은 요청 |
| `FORBIDDEN` | 403 | 권한 없음 |
| `VALIDATION_ERROR` | 400 | 요청 데이터 유효성 검증 실패 |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류 |

### Shopping 도메인 에러

| Code | HTTP Status | 설명 |
|------|-------------|------|
| `S001` | 404 | 상품을 찾을 수 없음 |
| `S002` | 400 | 재고 부족 |
| `S003` | 404 | 주문을 찾을 수 없음 |
| `S004` | 400 | 주문 취소 불가 (배송 시작됨) |
| `S005` | 404 | 장바구니 항목 없음 |
| `S006` | 400 | 결제 금액 불일치 |
| `S007` | 400 | 결제 처리 실패 |
| `S008` | 404 | 배송 정보 없음 |
| `S009` | 400 | 중복 상품 (관리자용) |
| `S010` | 400 | 유효하지 않은 카테고리 |

---

## 🔐 인증 및 권한

### JWT 토큰

모든 API 요청은 JWT Bearer 토큰을 포함해야 합니다:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 권한 레벨

| 레벨 | 설명 | 접근 가능 API |
|------|------|---------------|
| **USER** | 일반 사용자 | 조회, 장바구니, 주문 생성 |
| **ADMIN** | 관리자 | 상품/재고 관리, 배송 상태 변경 |
| **SYSTEM** | 시스템 | 모든 API |

### 권한이 필요한 API

- 상품 생성/수정/삭제: `ADMIN`
- 재고 추가: `ADMIN`
- 배송 상태 업데이트: `ADMIN` 또는 `SYSTEM`

---

## 🔧 사용 예시

### Axios Interceptor 설정

```typescript
// src/utils/api-client.ts
import axios from 'axios'
import { useAuthStore } from '@/stores/authStore'

const apiClient = axios.create({
  baseURL: '/api/v1/shopping',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request Interceptor - JWT 토큰 자동 첨부
apiClient.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response Interceptor - 에러 처리
apiClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      // 토큰 만료 시 로그인 페이지로 이동
      const authStore = useAuthStore()
      authStore.logout()
      window.location.href = '/login'
    }
    return Promise.reject(error.response?.data || error.message)
  }
)

export default apiClient
```

### Product API 사용 예시

```typescript
// src/api/productApi.ts
import apiClient from '@/utils/api-client'
import type { ApiResponse, PagedResponse, Product } from '@/types/api'

export const productApi = {
  // 상품 목록 조회
  async getProducts(params?: {
    page?: number
    size?: number
    category?: string
  }): Promise<ApiResponse<PagedResponse<Product>>> {
    return await apiClient.get('/products', { params })
  },

  // 상품 상세 조회
  async getProduct(id: number): Promise<ApiResponse<Product>> {
    return await apiClient.get(`/products/${id}`)
  },

  // 상품 검색
  async searchProducts(keyword: string, page = 0, size = 10): Promise<ApiResponse<PagedResponse<Product>>> {
    return await apiClient.get('/products/search', {
      params: { keyword, page, size },
    })
  },
}
```

### Vue 컴포넌트에서 사용

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { productApi } from '@/api/productApi'
import type { Product } from '@/types/api'

const products = ref<Product[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

async function loadProducts() {
  loading.value = true
  error.value = null

  try {
    const response = await productApi.getProducts({
      page: 0,
      size: 20,
      category: 'books'
    })

    if (response.success) {
      products.value = response.data.content
    }
  } catch (err: any) {
    error.value = err.message || '상품 목록을 불러오는 데 실패했습니다.'
    console.error('Failed to load products:', err)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadProducts()
})
</script>

<template>
  <div>
    <div v-if="loading">로딩 중...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else class="product-grid">
      <div v-for="product in products" :key="product.id" class="product-card">
        <h3>{{ product.name }}</h3>
        <p>{{ product.price.toLocaleString() }}원</p>
      </div>
    </div>
  </div>
</template>
```

### Cart API 사용 예시

```typescript
// src/composables/useCart.ts
import { ref } from 'vue'
import { cartApi } from '@/api/cartApi'
import type { Cart } from '@/types/api'

export function useCart() {
  const cart = ref<Cart | null>(null)
  const loading = ref(false)

  async function loadCart() {
    loading.value = true
    try {
      const response = await cartApi.getCart()
      if (response.success) {
        cart.value = response.data
      }
    } catch (error) {
      console.error('Failed to load cart:', error)
    } finally {
      loading.value = false
    }
  }

  async function addToCart(productId: number, quantity: number) {
    try {
      const response = await cartApi.addItem({ productId, quantity })
      if (response.success) {
        await loadCart() // 장바구니 다시 로드
        return true
      }
    } catch (error) {
      console.error('Failed to add to cart:', error)
      return false
    }
  }

  return {
    cart,
    loading,
    loadCart,
    addToCart,
  }
}
```

---

## 📊 Rate Limiting

### 제한 정책

| 사용자 타입 | 제한 | 기간 |
|-------------|------|------|
| 비로그인 | 100 요청 | 1시간 |
| 일반 사용자 | 1000 요청 | 1시간 |
| 관리자 | 10000 요청 | 1시간 |

### Rate Limit 헤더

```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1705560000
```

### 429 Too Many Requests

```json
{
  "success": false,
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "요청 제한을 초과했습니다. 잠시 후 다시 시도해 주세요.",
  "data": {
    "retryAfter": 3600
  },
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

## 🔄 Changelog

### v1.0.0 (2026-01-18)

- 최초 API 명세 작성
- Product, Inventory, Cart, Order, Payment, Delivery API 정의
- 타입 정의 및 에러 코드 문서화
- 인증/권한 규칙 명시

---

## 🔗 관련 문서

- [Backend Shopping Service API](../../api/) <!-- TODO: verify shopping service API location -->
- [Architecture Overview](../../architecture/)
- [Integration Guide](../../guides/)

---

**최종 업데이트**: 2026-01-18
