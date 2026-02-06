# 상품 상세 페이지

## 학습 목표
- 상품 상세 정보 표시 및 장바구니 추가 기능 구현
- useParams를 통한 동적 라우팅 이해
- 재고 확인 및 수량 선택 UI 구현

---

## 1. 페이지 구조

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     PRODUCT DETAIL PAGE                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Image Gallery  ─────►  상품 이미지                                          │
│   Product Info   ─────►  이름, 가격, 설명                                     │
│   Stock Status   ─────►  재고 상태 표시                                       │
│   Quantity       ─────►  수량 선택 (+ / -)                                    │
│   Add to Cart    ─────►  장바구니 추가 버튼                                   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Portal Universe 코드 분석

### 2.1 ProductDetailPage.tsx

```tsx
// pages/ProductDetailPage.tsx
import React, { useState, useEffect } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { productApi, inventoryApi } from '@/api/endpoints'
import { useCartStore } from '@/stores/cartStore'
import type { Product, Inventory } from '@/types'

const ProductDetailPage: React.FC = () => {
  const { productId } = useParams<{ productId: string }>()
  const navigate = useNavigate()
  const { addItem } = useCartStore()

  // State
  const [product, setProduct] = useState<Product | null>(null)
  const [inventory, setInventory] = useState<Inventory | null>(null)
  const [quantity, setQuantity] = useState(1)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [addingToCart, setAddingToCart] = useState(false)

  // Fetch product and inventory
  useEffect(() => {
    const fetchData = async () => {
      if (!productId) return

      setLoading(true)
      setError(null)

      try {
        // 상품 정보 조회
        const productResponse = await productApi.getProduct(Number(productId))
        setProduct(productResponse.data)

        // 재고 정보 조회
        try {
          const invResponse = await inventoryApi.getInventories([Number(productId)])
          if (invResponse.data.length > 0) {
            setInventory(invResponse.data[0])
          }
        } catch (invError) {
          console.warn('Failed to fetch inventory:', invError)
        }
      } catch (err: any) {
        setError(err.response?.data?.error?.message || err.message || 'Failed to load product')
      } finally {
        setLoading(false)
      }
    }

    fetchData()
  }, [productId])

  // Format price
  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW'
    }).format(price)
  }

  // Handle quantity change
  const handleQuantityChange = (delta: number) => {
    const newQuantity = quantity + delta
    const maxQuantity = inventory?.quantity || 999

    if (newQuantity >= 1 && newQuantity <= maxQuantity) {
      setQuantity(newQuantity)
    }
  }

  // Add to cart
  const handleAddToCart = async () => {
    if (!product) return

    // 재고 확인
    if (inventory && inventory.quantity < quantity) {
      alert('재고가 부족합니다.')
      return
    }

    setAddingToCart(true)

    try {
      await addItem(
        product.id,
        product.name,
        product.price,
        quantity
      )

      alert('장바구니에 추가되었습니다.')
      setQuantity(1) // 수량 초기화
    } catch (error: any) {
      alert(error.message || '장바구니 추가에 실패했습니다.')
    } finally {
      setAddingToCart(false)
    }
  }

  // Buy now
  const handleBuyNow = async () => {
    await handleAddToCart()
    navigate('/cart')
  }

  // Render loading
  if (loading) {
    return <ProductDetailSkeleton />
  }

  // Render error
  if (error || !product) {
    return (
      <div className="max-w-4xl mx-auto py-8">
        <Alert variant="error" className="text-center">
          <p className="mb-4">{error || '상품을 찾을 수 없습니다.'}</p>
          <Button asChild variant="primary">
            <Link to="/products">상품 목록으로</Link>
          </Button>
        </Alert>
      </div>
    )
  }

  // Check stock
  const isInStock = !inventory || inventory.quantity > 0
  const stockQuantity = inventory?.quantity || 0

  return (
    <div className="max-w-6xl mx-auto">
      {/* Breadcrumb */}
      <nav className="mb-6 text-sm">
        <Link to="/products" className="text-brand-primary hover:underline">
          상품 목록
        </Link>
        <span className="mx-2 text-text-meta">/</span>
        <span className="text-text-body">{product.name}</span>
      </nav>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Image */}
        <div className="bg-bg-subtle rounded-lg aspect-square flex items-center justify-center overflow-hidden">
          {product.imageUrl ? (
            <img
              src={product.imageUrl}
              alt={product.name}
              className="w-full h-full object-cover"
            />
          ) : (
            <span className="text-text-meta text-lg">No Image</span>
          )}
        </div>

        {/* Product Info */}
        <div className="space-y-6">
          <div>
            <h1 className="text-3xl font-bold text-text-heading mb-4">
              {product.name}
            </h1>

            <p className="text-text-meta leading-relaxed">
              {product.description}
            </p>
          </div>

          {/* Price */}
          <div className="border-t border-b border-border-default py-6">
            <div className="flex items-center justify-between">
              <span className="text-lg text-text-meta">가격</span>
              <span className="text-3xl font-bold text-brand-primary">
                {formatPrice(product.price)}
              </span>
            </div>
          </div>

          {/* Stock Status */}
          <div className="flex items-center gap-3">
            <span className="text-text-meta">재고 상태:</span>
            {isInStock ? (
              <span className="px-3 py-1 bg-status-success-bg text-status-success text-sm font-medium rounded-full">
                재고 있음 ({stockQuantity}개)
              </span>
            ) : (
              <span className="px-3 py-1 bg-status-error-bg text-status-error text-sm font-medium rounded-full">
                품절
              </span>
            )}
          </div>

          {/* Quantity Selector */}
          {isInStock && (
            <div>
              <label className="block text-sm font-medium text-text-heading mb-2">
                수량
              </label>
              <div className="flex items-center gap-3">
                <button
                  onClick={() => handleQuantityChange(-1)}
                  disabled={quantity <= 1}
                  className="w-10 h-10 rounded-lg border border-border-default flex items-center justify-center hover:bg-bg-subtle transition-colors disabled:opacity-50"
                >
                  -
                </button>

                <input
                  type="number"
                  value={quantity}
                  onChange={(e) => setQuantity(Math.max(1, parseInt(e.target.value) || 1))}
                  className="w-20 h-10 text-center border border-border-default rounded-lg"
                  min="1"
                  max={stockQuantity}
                />

                <button
                  onClick={() => handleQuantityChange(1)}
                  disabled={quantity >= stockQuantity}
                  className="w-10 h-10 rounded-lg border border-border-default flex items-center justify-center hover:bg-bg-subtle transition-colors disabled:opacity-50"
                >
                  +
                </button>

                <span className="text-text-meta text-sm ml-2">
                  최대 {stockQuantity}개
                </span>
              </div>
            </div>
          )}

          {/* Actions */}
          <div className="flex gap-3">
            <Button
              onClick={handleAddToCart}
              disabled={!isInStock || addingToCart}
              variant="secondary"
              size="lg"
              className="flex-1"
            >
              {addingToCart ? '추가 중...' : '장바구니 담기'}
            </Button>

            <Button
              onClick={handleBuyNow}
              disabled={!isInStock || addingToCart}
              variant="primary"
              size="lg"
              className="flex-1"
            >
              바로 구매
            </Button>
          </div>

          {/* Warning */}
          {inventory && inventory.quantity < 10 && inventory.quantity > 0 && (
            <Alert variant="warning">
              재고가 {inventory.quantity}개 남았습니다. 서둘러 주문하세요!
            </Alert>
          )}
        </div>
      </div>
    </div>
  )
}

export default ProductDetailPage
```

---

## 3. 수량 선택기 컴포넌트

### 3.1 재사용 가능한 QuantitySelector

```tsx
// components/QuantitySelector.tsx
interface Props {
  value: number
  max: number
  onChange: (value: number) => void
  disabled?: boolean
}

export function QuantitySelector({ value, max, onChange, disabled }: Props) {
  const handleChange = (delta: number) => {
    const newValue = value + delta
    if (newValue >= 1 && newValue <= max) {
      onChange(newValue)
    }
  }

  return (
    <div className="flex items-center gap-2">
      <button
        onClick={() => handleChange(-1)}
        disabled={disabled || value <= 1}
        className="w-8 h-8 rounded border hover:bg-bg-subtle disabled:opacity-50"
      >
        -
      </button>

      <input
        type="number"
        value={value}
        onChange={(e) => {
          const val = parseInt(e.target.value) || 1
          onChange(Math.max(1, Math.min(max, val)))
        }}
        disabled={disabled}
        className="w-16 h-8 text-center border rounded"
      />

      <button
        onClick={() => handleChange(1)}
        disabled={disabled || value >= max}
        className="w-8 h-8 rounded border hover:bg-bg-subtle disabled:opacity-50"
      >
        +
      </button>
    </div>
  )
}
```

---

## 4. 재고 상태 표시

### 4.1 StockBadge 컴포넌트

```tsx
interface Props {
  inventory: Inventory | null
}

export function StockBadge({ inventory }: Props) {
  if (!inventory) {
    return (
      <span className="px-3 py-1 bg-bg-subtle text-text-meta text-sm rounded-full">
        재고 정보 없음
      </span>
    )
  }

  if (inventory.quantity === 0) {
    return (
      <span className="px-3 py-1 bg-status-error-bg text-status-error text-sm font-medium rounded-full">
        품절
      </span>
    )
  }

  if (inventory.quantity < 10) {
    return (
      <span className="px-3 py-1 bg-status-warning-bg text-status-warning text-sm font-medium rounded-full">
        재고 부족 ({inventory.quantity}개)
      </span>
    )
  }

  return (
    <span className="px-3 py-1 bg-status-success-bg text-status-success text-sm font-medium rounded-full">
      재고 있음 ({inventory.quantity}개)
    </span>
  )
}
```

---

## 5. 장바구니 추가 로직

### 5.1 Zustand Store 연동

```tsx
// stores/cartStore.ts
export const useCartStore = create<CartState>((set, get) => ({
  cart: null,

  addItem: async (productId, productName, price, quantity) => {
    set({ loading: true, error: null })

    try {
      const response = await cartApi.addItem({ productId, quantity })

      set({
        cart: response.data,
        itemCount: response.data.itemCount,
        totalAmount: response.data.totalAmount,
        loading: false
      })

      console.log(`Added ${quantity}x ${productName} to cart`)
    } catch (error: any) {
      set({
        error: error.response?.data?.error?.message || '장바구니 추가 실패',
        loading: false
      })
      throw error
    }
  }
}))
```

---

## 6. 핵심 정리

| 기능 | 구현 |
|------|------|
| **URL 파라미터** | `useParams<{ productId }>()` |
| **상품 조회** | `productApi.getProduct(id)` |
| **재고 조회** | `inventoryApi.getInventories([id])` |
| **수량 선택** | useState + 증감 버튼 |
| **장바구니 추가** | `cartStore.addItem()` |
| **바로 구매** | 추가 후 `/cart`로 이동 |

---

## 다음 학습

- [Cart Implementation](./cart-implementation.md)
- [Product List](./product-list.md)
- [Checkout Flow](./checkout-flow.md)
