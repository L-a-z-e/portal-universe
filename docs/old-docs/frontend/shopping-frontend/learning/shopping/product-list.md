# 상품 목록 구현

## 학습 목표
- 상품 목록 페이지의 구조와 기능 이해
- 페이지네이션, 검색, 필터링 구현 학습
- Inventory 연동을 통한 재고 표시 학습

---

## 1. 페이지 구조

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       PRODUCT LIST PAGE                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Header         ─────►  제목, 검색 폼                                        │
│   Grid           ─────►  상품 카드 그리드 (Responsive)                       │
│   Pagination     ─────►  페이지 네비게이션                                   │
│   Loading        ─────►  Skeleton UI                                         │
│   Error          ─────►  에러 처리 및 재시도                                 │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Portal Universe 코드 분석

### 2.1 ProductListPage.tsx

```tsx
// pages/ProductListPage.tsx
import React, { useState, useEffect, useCallback } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { productApi, inventoryApi } from '@/api/endpoints'
import type { Product, Inventory } from '@/types'
import ProductCard from '@/components/ProductCard'

const ProductListPage: React.FC = () => {
  // ============================================
  // URL Query Params
  // ============================================
  const [searchParams, setSearchParams] = useSearchParams()
  const currentPage = parseInt(searchParams.get('page') || '0')
  const searchKeyword = searchParams.get('keyword') || ''
  const category = searchParams.get('category') || ''

  // ============================================
  // State
  // ============================================
  const [products, setProducts] = useState<Product[]>([])
  const [inventories, setInventories] = useState<Map<number, Inventory>>(new Map())
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchInput, setSearchInput] = useState(searchKeyword)

  // ============================================
  // Fetch Products
  // ============================================
  const fetchProducts = useCallback(async () => {
    setLoading(true)
    setError(null)

    try {
      let response
      if (searchKeyword) {
        // 검색 모드
        response = await productApi.searchProducts(searchKeyword, currentPage, 12)
      } else {
        // 일반 목록
        response = await productApi.getProducts(currentPage, 12, category || undefined)
      }

      const productsData = response.data.content
      setProducts(productsData)
      setTotalPages(response.data.totalPages)

      // ============================================
      // Fetch Inventories for all products
      // ============================================
      if (productsData.length > 0) {
        const productIds = productsData.map((p: Product) => p.id)
        try {
          const invResponse = await inventoryApi.getInventories(productIds)
          const invMap = new Map<number, Inventory>()
          invResponse.data.forEach((inv: Inventory) => {
            invMap.set(inv.productId, inv)
          })
          setInventories(invMap)
        } catch (invError) {
          console.warn('Failed to fetch inventories:', invError)
        }
      }
    } catch (err: any) {
      setError(err.response?.data?.error?.message || err.message || 'Failed to fetch products')
    } finally {
      setLoading(false)
    }
  }, [currentPage, searchKeyword, category])

  useEffect(() => {
    fetchProducts()
  }, [fetchProducts])

  // ============================================
  // Search Handler
  // ============================================
  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    const params = new URLSearchParams()
    if (searchInput) {
      params.set('keyword', searchInput)
    }
    params.set('page', '0')
    setSearchParams(params)
  }

  // ============================================
  // Pagination Handler
  // ============================================
  const handlePageChange = (page: number) => {
    const params = new URLSearchParams(searchParams)
    params.set('page', String(page))
    setSearchParams(params)
  }

  const clearSearch = () => {
    setSearchInput('')
    setSearchParams({})
  }

  // ============================================
  // Render
  // ============================================
  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-text-heading">Products</h1>

        {/* Search Form */}
        <form onSubmit={handleSearch} className="flex items-center gap-2">
          <Input
            type="text"
            value={searchInput}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => setSearchInput(e.target.value)}
            placeholder="Search products..."
          />
          <Button type="submit" variant="primary">Search</Button>
          {searchKeyword && (
            <Button type="button" onClick={clearSearch} variant="secondary">
              Clear
            </Button>
          )}
        </form>
      </div>

      {/* Loading */}
      {loading && <ProductListSkeleton />}

      {/* Error */}
      {error && !loading && (
        <Alert variant="error" className="text-center">
          <p className="mb-4">{error}</p>
          <Button onClick={fetchProducts} variant="primary">Retry</Button>
        </Alert>
      )}

      {/* Product Grid */}
      {!loading && !error && (
        <>
          {products.length === 0 ? (
            <EmptyState searchKeyword={searchKeyword} onClear={clearSearch} />
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
              {products.map((product) => (
                <ProductCard
                  key={product.id}
                  product={product}
                  inventory={inventories.get(product.id)}
                />
              ))}
            </div>
          )}

          {/* Pagination */}
          {totalPages > 1 && (
            <Pagination
              currentPage={currentPage}
              totalPages={totalPages}
              onPageChange={handlePageChange}
            />
          )}
        </>
      )}
    </div>
  )
}

export default ProductListPage
```

---

## 3. ProductCard 컴포넌트

### 3.1 구조

```tsx
// components/ProductCard.tsx
import { memo } from 'react'
import { Link } from 'react-router-dom'
import type { Product, Inventory } from '@/types'

interface Props {
  product: Product
  inventory?: Inventory
}

// ✅ memo로 감싸서 product/inventory 변경 시에만 리렌더링
export const ProductCard = memo(function ProductCard({ product, inventory }: Props) {
  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW'
    }).format(price)
  }

  const getStockStatus = () => {
    if (!inventory) return null

    if (inventory.quantity === 0) {
      return (
        <span className="px-2 py-1 bg-status-error-bg text-status-error text-xs rounded">
          품절
        </span>
      )
    }

    if (inventory.quantity < 10) {
      return (
        <span className="px-2 py-1 bg-status-warning-bg text-status-warning text-xs rounded">
          재고 부족
        </span>
      )
    }

    return (
      <span className="px-2 py-1 bg-status-success-bg text-status-success text-xs rounded">
        재고 있음
      </span>
    )
  }

  return (
    <Link to={`/products/${product.id}`}>
      <div className="bg-bg-card border border-border-default rounded-lg overflow-hidden hover:shadow-lg transition-shadow">
        {/* Image */}
        <div className="aspect-square bg-bg-subtle">
          {product.imageUrl ? (
            <img
              src={product.imageUrl}
              alt={product.name}
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-text-meta">
              No Image
            </div>
          )}
        </div>

        {/* Content */}
        <div className="p-4">
          <h3 className="text-lg font-semibold text-text-heading mb-2 line-clamp-2">
            {product.name}
          </h3>

          <p className="text-text-meta text-sm mb-3 line-clamp-2">
            {product.description}
          </p>

          <div className="flex items-center justify-between">
            <span className="text-xl font-bold text-brand-primary">
              {formatPrice(product.price)}
            </span>

            {getStockStatus()}
          </div>
        </div>
      </div>
    </Link>
  )
})
```

---

## 4. Pagination 컴포넌트

### 4.1 구현

```tsx
// components/common/Pagination.tsx
interface Props {
  currentPage: number
  totalPages: number
  onPageChange: (page: number) => void
}

export function Pagination({ currentPage, totalPages, onPageChange }: Props) {
  const getPageNumbers = () => {
    const pages: number[] = []
    const maxVisible = 5

    if (totalPages <= maxVisible) {
      // 전체 페이지가 5개 이하면 모두 표시
      for (let i = 0; i < totalPages; i++) {
        pages.push(i)
      }
    } else {
      // 현재 페이지 기준으로 5개 표시
      let start = Math.max(0, currentPage - 2)
      let end = Math.min(totalPages - 1, start + maxVisible - 1)

      if (end - start < maxVisible - 1) {
        start = Math.max(0, end - maxVisible + 1)
      }

      for (let i = start; i <= end; i++) {
        pages.push(i)
      }
    }

    return pages
  }

  return (
    <div className="flex items-center justify-center gap-2 mt-8">
      <Button
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage === 0}
        variant="secondary"
      >
        Previous
      </Button>

      <div className="flex items-center gap-1">
        {getPageNumbers().map((page) => (
          <Button
            key={page}
            onClick={() => onPageChange(page)}
            variant={page === currentPage ? 'primary' : 'secondary'}
            size="sm"
            className="w-10 h-10"
          >
            {page + 1}
          </Button>
        ))}
      </div>

      <Button
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage === totalPages - 1}
        variant="secondary"
      >
        Next
      </Button>
    </div>
  )
}
```

---

## 5. Skeleton UI

### 5.1 로딩 상태

```tsx
function ProductListSkeleton() {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
      {Array.from({ length: 8 }).map((_, i) => (
        <div key={i} className="bg-bg-card rounded-lg overflow-hidden animate-pulse">
          <div className="aspect-square bg-bg-subtle" />
          <div className="p-4 space-y-3">
            <div className="h-6 bg-bg-subtle rounded w-3/4" />
            <div className="h-4 bg-bg-subtle rounded w-full" />
            <div className="h-4 bg-bg-subtle rounded w-2/3" />
            <div className="flex justify-between items-center">
              <div className="h-8 bg-bg-subtle rounded w-1/3" />
              <div className="h-6 bg-bg-subtle rounded w-1/4" />
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}
```

---

## 6. 검색 및 필터링

### 6.1 URL Query String 활용

```tsx
// URL: /products?keyword=laptop&page=2&category=electronics

const [searchParams, setSearchParams] = useSearchParams()
const keyword = searchParams.get('keyword') || ''
const page = parseInt(searchParams.get('page') || '0')
const category = searchParams.get('category') || ''

// 검색 실행
const handleSearch = (e: React.FormEvent) => {
  e.preventDefault()
  const params = new URLSearchParams()
  params.set('keyword', searchInput)
  params.set('page', '0')
  setSearchParams(params)
}
```

### 6.2 카테고리 필터

```tsx
function CategoryFilter({ currentCategory, onCategoryChange }: Props) {
  const categories = ['전체', '전자제품', '의류', '식품', '도서']

  return (
    <div className="flex gap-2">
      {categories.map((category) => (
        <button
          key={category}
          onClick={() => onCategoryChange(category === '전체' ? '' : category)}
          className={`
            px-4 py-2 rounded-lg font-medium transition-colors
            ${currentCategory === category
              ? 'bg-brand-primary text-white'
              : 'bg-bg-subtle text-text-body hover:bg-bg-subtle/80'
            }
          `}
        >
          {category}
        </button>
      ))}
    </div>
  )
}
```

---

## 7. 핵심 정리

| 기능 | 구현 방식 |
|------|----------|
| **상품 조회** | `productApi.getProducts(page, size, category)` |
| **검색** | `productApi.searchProducts(keyword, page, size)` |
| **재고 조회** | `inventoryApi.getInventories(productIds)` |
| **페이지네이션** | URL Query String (`?page=2`) |
| **로딩 상태** | Skeleton UI |
| **에러 처리** | try-catch + 재시도 버튼 |
| **최적화** | React.memo, useCallback |

---

## 다음 학습

- [Product Detail](./product-detail.md)
- [Cart Implementation](./cart-implementation.md)
- [API Integration](../react/api-integration.md)
