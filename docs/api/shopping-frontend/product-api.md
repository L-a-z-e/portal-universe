---
id: api-shopping-product
title: Shopping Product API
type: api
status: current
version: v1
created: 2026-02-06
updated: 2026-02-08
author: Laze
tags: [api, shopping, frontend, product, admin]
related: [api-shopping-types, api-shopping-inventory]
---

# Shopping Product API

> 상품 조회, 검색, 관리 API (공개 + 관리자)

---

## 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `/api/v1/shopping` |
| **인증** | Bearer Token (관리자 기능만 ADMIN 권한 필요) |
| **엔드포인트** | `productApi`, `adminProductApi` |

---

## 공개 API (productApi)

### 상품 목록 조회

```typescript
getProducts(page = 0, size = 12, category?: string): Promise<ApiResponse<PagedResponse<Product>>>
```

**Endpoint**

```http
GET /api/v1/shopping/products?page=1&size=12&category=books
Authorization: Bearer {token}
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `page` | number | ❌ | 페이지 번호 (1부터 시작) | 1 |
| `size` | number | ❌ | 페이지 크기 | 12 |
| `category` | string | ❌ | 카테고리 필터 | - |

**Response**

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 1,
        "name": "스프링 부트 완벽 가이드",
        "description": "Spring Boot 실전 가이드",
        "price": 35000,
        "stockQuantity": 50,
        "imageUrl": "https://cdn.example.com/1.jpg",
        "category": "books",
        "createdAt": "2026-01-10T10:00:00Z"
      }
    ],
    "page": 1,
    "size": 12,
    "totalElements": 150,
    "totalPages": 13
  }
}
```

**사용 예시**

```typescript
import { productApi } from '@/api/endpoints'

const response = await productApi.getProducts(0, 20, 'books')
const products = response.data.items
```

---

### 상품 상세 조회

```typescript
getProduct(id: number): Promise<ApiResponse<Product>>
```

**Endpoint**

```http
GET /api/v1/shopping/products/{id}
Authorization: Bearer {token}
```

**Response**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "스프링 부트 완벽 가이드",
    "description": "Spring Boot 3.x 기반 마이크로서비스 구축 실전 가이드",
    "price": 35000,
    "stockQuantity": 50,
    "imageUrl": "https://cdn.example.com/1.jpg",
    "category": "books",
    "createdAt": "2026-01-10T10:00:00Z",
    "updatedAt": "2026-01-15T14:30:00Z"
  }
}
```

**Error (404)**

```json
{
  "success": false,
  "code": "PRODUCT_NOT_FOUND",
  "message": "상품을 찾을 수 없습니다."
}
```

---

### 상품 검색

```typescript
searchProducts(keyword: string, page = 0, size = 12): Promise<ApiResponse<PagedResponse<Product>>>
```

**Endpoint**

```http
GET /api/v1/shopping/search/products?keyword=spring&page=1&size=12
Authorization: Bearer {token}
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `keyword` | string | ✅ | 검색 키워드 |
| `page` | number | ❌ | 페이지 번호 (기본값: 0) |
| `size` | number | ❌ | 페이지 크기 (기본값: 12) |

**사용 예시**

```typescript
const response = await productApi.searchProducts('스프링', 0, 20)
const results = response.data.content
```

---

### 상품 생성 (관리자)

```typescript
createProduct(data: ProductCreateRequest): Promise<ApiResponse<Product>>
```

**Endpoint**

```http
POST /api/v1/shopping/products
Content-Type: application/json
Authorization: Bearer {admin_token}

{
  "name": "Vue 3 마스터하기",
  "description": "Vue 3 Composition API 완벽 가이드",
  "price": 32000,
  "imageUrl": "https://cdn.example.com/vue3.jpg",
  "category": "books"
}
```

**Request Body (ProductCreateRequest)**

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `name` | string | ✅ | 상품명 | 1~200자 |
| `description` | string | ✅ | 상세 설명 | 최대 2000자 |
| `price` | number | ✅ | 가격 | 양수 |
| `imageUrl` | string | ❌ | 이미지 URL | 유효한 URL |
| `category` | string | ❌ | 카테고리 | - |

**Response (201 Created)**

```json
{
  "success": true,
  "data": {
    "id": 51,
    "name": "Vue 3 마스터하기",
    "description": "Vue 3 Composition API 완벽 가이드",
    "price": 32000,
    "stockQuantity": 0,
    "imageUrl": "https://cdn.example.com/vue3.jpg",
    "category": "books",
    "createdAt": "2026-02-06T10:30:00Z"
  }
}
```

---

### 상품 수정 (관리자)

```typescript
updateProduct(id: number, data: ProductUpdateRequest): Promise<ApiResponse<Product>>
```

**Endpoint**

```http
PUT /api/v1/shopping/products/{id}
Content-Type: application/json
Authorization: Bearer {admin_token}

{
  "name": "Vue 3 마스터하기 (개정판)",
  "price": 35000
}
```

**Request Body (ProductUpdateRequest)**

모든 필드가 optional입니다.

| 필드 | 타입 | 설명 |
|------|------|------|
| `name` | string | 상품명 |
| `description` | string | 상세 설명 |
| `price` | number | 가격 |
| `imageUrl` | string | 이미지 URL |
| `category` | string | 카테고리 |

---

### 상품 삭제 (관리자)

```typescript
deleteProduct(id: number): Promise<ApiResponse<void>>
```

**Endpoint**

```http
DELETE /api/v1/shopping/products/{id}
Authorization: Bearer {admin_token}
```

**Response (200 OK)**

```json
{
  "success": true,
  "message": "상품이 삭제되었습니다."
}
```

---

## 관리자 API (adminProductApi)

### 관리자 상품 목록 조회

```typescript
getProducts(params: {
  page?: number
  size?: number
  keyword?: string
  category?: string
  status?: string
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}): Promise<ApiResponse<PagedResponse<Product>>>
```

**Endpoint**

```http
GET /api/v1/shopping/products?page=1&size=20&keyword=spring&sortBy=createdAt&sortOrder=desc
Authorization: Bearer {admin_token}
```

**Query Parameters**

| 파라미터 | 타입 | 설명 | 기본값 |
|----------|------|------|--------|
| `page` | number | 페이지 번호 | 1 |
| `size` | number | 페이지 크기 | 20 |
| `keyword` | string | 검색 키워드 | - |
| `category` | string | 카테고리 필터 | - |
| `status` | string | 상태 필터 | - |
| `sortBy` | string | 정렬 기준 | - |
| `sortOrder` | 'asc' \| 'desc' | 정렬 순서 | - |

**사용 예시**

```typescript
import { adminProductApi } from '@/api/endpoints'

const response = await adminProductApi.getProducts({
  page: 0,
  size: 20,
  keyword: 'spring',
  sortBy: 'createdAt',
  sortOrder: 'desc'
})
```

---

### 관리자 상품 생성

```typescript
createProduct(data: ProductCreateRequest): Promise<ApiResponse<Product>>
```

**Endpoint**

```http
POST /api/v1/shopping/admin/products
Content-Type: application/json
Authorization: Bearer {admin_token}
```

**참고**: 공개 API의 `createProduct`와 동일한 형식이지만, 관리자 전용 엔드포인트를 사용합니다.

---

### 관리자 상품 수정

```typescript
updateProduct(id: number, data: ProductUpdateRequest): Promise<ApiResponse<Product>>
```

**Endpoint**

```http
PUT /api/v1/shopping/admin/products/{id}
Content-Type: application/json
Authorization: Bearer {admin_token}
```

---

### 관리자 상품 삭제

```typescript
deleteProduct(id: number): Promise<ApiResponse<void>>
```

**Endpoint**

```http
DELETE /api/v1/shopping/admin/products/{id}
Authorization: Bearer {admin_token}
```

---

### 관리자 재고 수정

```typescript
updateStock(id: number, stock: number): Promise<ApiResponse<Product>>
```

**Endpoint**

```http
PATCH /api/v1/shopping/admin/products/{id}/stock
Content-Type: application/json
Authorization: Bearer {admin_token}

{
  "stock": 100
}
```

**Response**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "스프링 부트 완벽 가이드",
    "stockQuantity": 100,
    "updatedAt": "2026-02-06T11:00:00Z"
  }
}
```

---

## React Hooks

### useAdminProducts

상품 목록 조회 Hook

```typescript
import { useAdminProducts } from '@/hooks/useAdminProducts'

export function ProductManagementPage() {
  const [filters, setFilters] = useState<ProductFilters>({
    page: 0,
    size: 20,
    keyword: '',
    category: '',
    sortBy: 'createdAt',
    sortOrder: 'desc'
  })

  const { data, isLoading, error, refetch } = useAdminProducts(filters)

  if (isLoading) return <div>로딩 중...</div>
  if (error) return <div>에러: {error.message}</div>

  return (
    <div>
      {data?.data.content.map((product) => (
        <ProductCard key={product.id} product={product} />
      ))}
    </div>
  )
}
```

### useAdminProduct

상품 상세 조회 Hook

```typescript
import { useAdminProduct } from '@/hooks/useAdminProducts'

export function ProductDetailPage({ id }: { id: number }) {
  const { data, isLoading, error, refetch } = useAdminProduct(id)

  if (isLoading) return <div>로딩 중...</div>
  if (error) return <div>에러: {error.message}</div>
  if (!data?.data) return <div>상품을 찾을 수 없습니다</div>

  const product = data.data

  return (
    <div>
      <h1>{product.name}</h1>
      <p>{product.description}</p>
      <p>가격: {product.price}원</p>
    </div>
  )
}
```

### useCreateProduct

상품 생성 Hook (Mutation)

```typescript
import { useCreateProduct } from '@/hooks/useAdminProducts'

export function ProductCreateForm() {
  const { mutateAsync, isPending, error } = useCreateProduct()

  const handleSubmit = async (formData: ProductFormData) => {
    try {
      const product = await mutateAsync(formData)
      console.log('생성된 상품:', product)
      alert('상품이 생성되었습니다')
    } catch (error) {
      console.error('생성 실패:', error)
    }
  }

  return (
    <form onSubmit={(e) => { e.preventDefault(); handleSubmit(formData) }}>
      {/* form fields */}
      <button type="submit" disabled={isPending}>
        {isPending ? '생성 중...' : '상품 생성'}
      </button>
    </form>
  )
}
```

### useUpdateProduct

상품 수정 Hook (Mutation)

```typescript
import { useUpdateProduct } from '@/hooks/useAdminProducts'

export function ProductEditForm({ id, initialData }: Props) {
  const { mutateAsync, isPending } = useUpdateProduct()

  const handleSubmit = async (formData: ProductFormData) => {
    try {
      const product = await mutateAsync({ id, data: formData })
      alert('상품이 수정되었습니다')
    } catch (error) {
      alert('수정 실패')
    }
  }

  return (
    <form onSubmit={(e) => { e.preventDefault(); handleSubmit(formData) }}>
      {/* form fields */}
      <button type="submit" disabled={isPending}>수정</button>
    </form>
  )
}
```

### useDeleteProduct

상품 삭제 Hook (Mutation)

```typescript
import { useDeleteProduct } from '@/hooks/useAdminProducts'

export function ProductDeleteButton({ id }: { id: number }) {
  const { mutateAsync, isPending } = useDeleteProduct()

  const handleDelete = async () => {
    if (!confirm('정말 삭제하시겠습니까?')) return

    try {
      await mutateAsync(id)
      alert('상품이 삭제되었습니다')
    } catch (error) {
      alert('삭제 실패')
    }
  }

  return (
    <button onClick={handleDelete} disabled={isPending}>
      {isPending ? '삭제 중...' : '삭제'}
    </button>
  )
}
```

---

## 에러 코드

| Code | HTTP Status | 설명 |
|------|-------------|------|
| `PRODUCT_NOT_FOUND` | 404 | 상품을 찾을 수 없음 |
| `DUPLICATE_PRODUCT_NAME` | 400 | 중복된 상품명 (관리자용) |
| `INVALID_CATEGORY` | 400 | 유효하지 않은 카테고리 |
| `VALIDATION_ERROR` | 400 | 요청 데이터 유효성 검증 실패 |

---

## 관련 문서

- [Client API](./client-api.md)
- [Inventory API](./inventory-api.md)
- [공통 타입 정의](./types.md)

---

---

## 변경 이력

| 날짜 | 변경 내용 |
|------|----------|
| 2026-02-08 | 페이지네이션 기본값 수정: page 0 → 1 (ADR-031 정합) |
| 2026-02-06 | 최초 작성 |

---

**최종 업데이트**: 2026-02-08
