# 장바구니 구현

## 학습 목표
- Zustand를 활용한 장바구니 상태 관리
- 장바구니 아이템 추가/수정/삭제 로직 이해
- 실시간 금액 계산 및 UI 업데이트

---

## 1. 장바구니 구조

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          CART STORE                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   cart           ─────►  장바구니 객체 (items, totalAmount, itemCount)       │
│   fetchCart      ─────►  장바구니 조회                                        │
│   addItem        ─────►  상품 추가                                            │
│   updateQuantity ─────►  수량 변경                                            │
│   removeItem     ─────►  상품 삭제                                            │
│   clearCart      ─────►  장바구니 비우기                                      │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Cart Store 구현

### 2.1 cartStore.ts

**Portal Universe 코드**
```tsx
// stores/cartStore.ts
import { create } from 'zustand'
import { devtools, persist } from 'zustand/middleware'
import { cartApi } from '@/api/endpoints'
import type { Cart } from '@/types'

interface CartState {
  // State
  cart: Cart | null
  loading: boolean
  error: string | null
  itemCount: number
  totalAmount: number

  // Actions
  fetchCart: () => Promise<void>
  addItem: (productId: number, productName: string, price: number, quantity: number) => Promise<void>
  updateItemQuantity: (itemId: number, quantity: number) => Promise<void>
  removeItem: (itemId: number) => Promise<void>
  clearCart: () => Promise<void>
  reset: () => void
}

const initialState = {
  cart: null,
  loading: false,
  error: null,
  itemCount: 0,
  totalAmount: 0
}

export const useCartStore = create<CartState>()(
  devtools(
    persist(
      (set, get) => ({
        ...initialState,

        // ============================================
        // 장바구니 조회
        // ============================================
        fetchCart: async () => {
          set({ loading: true, error: null })

          try {
            const response = await cartApi.getCart()
            const cart = response.data

            set({
              cart,
              itemCount: cart.itemCount,
              totalAmount: cart.totalAmount,
              loading: false
            })
          } catch (error: any) {
            if (error.response?.status === 404) {
              // 빈 장바구니 초기화
              set({
                cart: {
                  id: 0,
                  userId: '',
                  status: 'ACTIVE',
                  items: [],
                  totalAmount: 0,
                  itemCount: 0,
                  totalQuantity: 0,
                  createdAt: new Date().toISOString()
                },
                itemCount: 0,
                totalAmount: 0,
                loading: false
              })
            } else {
              set({
                error: error.message || 'Failed to fetch cart',
                loading: false
              })
            }
          }
        },

        // ============================================
        // 상품 추가
        // ============================================
        addItem: async (productId, productName, price, quantity) => {
          set({ loading: true, error: null })

          try {
            const response = await cartApi.addItem({ productId, quantity })
            const cart = response.data

            set({
              cart,
              itemCount: cart.itemCount,
              totalAmount: cart.totalAmount,
              loading: false
            })

            console.log(`✅ Added ${quantity}x ${productName} to cart`)
          } catch (error: any) {
            set({
              error: error.response?.data?.error?.message || error.message || 'Failed to add item',
              loading: false
            })
            throw error
          }
        },

        // ============================================
        // 수량 변경
        // ============================================
        updateItemQuantity: async (itemId, quantity) => {
          const originalCart = get().cart

          try {
            // 낙관적 업데이트 (Optimistic Update)
            set(state => ({
              cart: state.cart ? {
                ...state.cart,
                items: state.cart.items.map(item =>
                  item.id === itemId ? { ...item, quantity } : item
                )
              } : null
            }))

            // 서버 요청
            const response = await cartApi.updateItemQuantity(itemId, quantity)
            const cart = response.data

            // 서버 응답으로 동기화
            set({
              cart,
              itemCount: cart.itemCount,
              totalAmount: cart.totalAmount
            })
          } catch (error: any) {
            // 실패 시 롤백
            set({ cart: originalCart })

            set({
              error: error.response?.data?.error?.message || error.message || 'Failed to update quantity'
            })
            throw error
          }
        },

        // ============================================
        // 상품 삭제
        // ============================================
        removeItem: async (itemId) => {
          set({ loading: true, error: null })

          try {
            const response = await cartApi.removeItem(itemId)
            const cart = response.data

            set({
              cart,
              itemCount: cart.itemCount,
              totalAmount: cart.totalAmount,
              loading: false
            })
          } catch (error: any) {
            set({
              error: error.response?.data?.error?.message || error.message || 'Failed to remove item',
              loading: false
            })
            throw error
          }
        },

        // ============================================
        // 장바구니 비우기
        // ============================================
        clearCart: async () => {
          set({ loading: true, error: null })

          try {
            await cartApi.clearCart()

            set({
              ...initialState
            })
          } catch (error: any) {
            set({
              error: error.response?.data?.error?.message || error.message || 'Failed to clear cart',
              loading: false
            })
            throw error
          }
        },

        // ============================================
        // 상태 초기화
        // ============================================
        reset: () => {
          set(initialState)
        }
      }),
      {
        name: 'shopping-cart-storage',
        // localStorage에 저장할 필드 선택
        partialize: (state) => ({
          itemCount: state.itemCount,
          totalAmount: state.totalAmount
        })
      }
    ),
    { name: 'CartStore' }
  )
)
```

---

## 3. CartPage 구현

### 3.1 CartPage.tsx

```tsx
// pages/CartPage.tsx
import React, { useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useCartStore } from '@/stores/cartStore'
import CartItem from '@/components/CartItem'

const CartPage: React.FC = () => {
  const navigate = useNavigate()
  const { cart, loading, fetchCart, clearCart } = useCartStore()

  useEffect(() => {
    fetchCart()
  }, [fetchCart])

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW'
    }).format(price)
  }

  const handleClearCart = async () => {
    if (!confirm('장바구니를 비우시겠습니까?')) return

    try {
      await clearCart()
      alert('장바구니가 비워졌습니다.')
    } catch (error) {
      alert('장바구니 비우기에 실패했습니다.')
    }
  }

  const handleCheckout = () => {
    if (!cart || cart.items.length === 0) {
      alert('장바구니가 비어있습니다.')
      return
    }
    navigate('/checkout')
  }

  if (loading) {
    return <div>Loading...</div>
  }

  if (!cart || cart.items.length === 0) {
    return (
      <div className="max-w-4xl mx-auto py-12 text-center">
        <div className="mb-6">
          <svg className="w-24 h-24 mx-auto text-text-meta" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z" />
          </svg>
        </div>
        <h2 className="text-2xl font-bold text-text-heading mb-2">
          장바구니가 비어있습니다
        </h2>
        <p className="text-text-meta mb-6">
          상품을 담아보세요!
        </p>
        <Button asChild variant="primary">
          <Link to="/products">쇼핑 계속하기</Link>
        </Button>
      </div>
    )
  }

  return (
    <div className="max-w-6xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-text-heading">
          장바구니 ({cart.itemCount}개)
        </h1>

        <button
          onClick={handleClearCart}
          className="text-status-error hover:underline text-sm"
        >
          장바구니 비우기
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Cart Items */}
        <div className="lg:col-span-2 space-y-4">
          {cart.items.map((item) => (
            <CartItem key={item.id} item={item} />
          ))}
        </div>

        {/* Summary */}
        <div className="lg:col-span-1">
          <div className="bg-bg-card border border-border-default rounded-lg p-6 sticky top-4">
            <h2 className="text-lg font-bold text-text-heading mb-4">
              주문 요약
            </h2>

            <div className="space-y-3 mb-6">
              <div className="flex justify-between text-text-body">
                <span>상품 금액</span>
                <span>{formatPrice(cart.totalAmount)}</span>
              </div>
              <div className="flex justify-between text-text-body">
                <span>배송비</span>
                <span className="text-status-success">무료</span>
              </div>
              <div className="border-t border-border-default pt-3 flex justify-between text-lg font-bold">
                <span className="text-text-heading">총 결제 금액</span>
                <span className="text-brand-primary">{formatPrice(cart.totalAmount)}</span>
              </div>
            </div>

            <Button
              onClick={handleCheckout}
              variant="primary"
              size="lg"
              className="w-full"
            >
              주문하기
            </Button>

            <Button
              asChild
              variant="secondary"
              size="lg"
              className="w-full mt-2"
            >
              <Link to="/products">쇼핑 계속하기</Link>
            </Button>
          </div>
        </div>
      </div>
    </div>
  )
}

export default CartPage
```

---

## 4. CartItem 컴포넌트

### 4.1 개별 아이템 UI

```tsx
// components/CartItem.tsx
import { memo, useCallback } from 'react'
import { useCartStore } from '@/stores/cartStore'
import type { CartItem as CartItemType } from '@/types'

interface Props {
  item: CartItemType
}

export const CartItem = memo(function CartItem({ item }: Props) {
  const { updateItemQuantity, removeItem } = useCartStore()

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW'
    }).format(price)
  }

  const handleQuantityChange = useCallback(async (delta: number) => {
    const newQuantity = item.quantity + delta
    if (newQuantity < 1) return

    try {
      await updateItemQuantity(item.id, newQuantity)
    } catch (error) {
      alert('수량 변경에 실패했습니다.')
    }
  }, [item.id, item.quantity, updateItemQuantity])

  const handleRemove = useCallback(async () => {
    if (!confirm('이 상품을 삭제하시겠습니까?')) return

    try {
      await removeItem(item.id)
    } catch (error) {
      alert('상품 삭제에 실패했습니다.')
    }
  }, [item.id, removeItem])

  return (
    <div className="bg-bg-card border border-border-default rounded-lg p-4">
      <div className="flex gap-4">
        {/* Image */}
        <div className="w-24 h-24 bg-bg-subtle rounded flex-shrink-0">
          {item.product.imageUrl ? (
            <img
              src={item.product.imageUrl}
              alt={item.product.name}
              className="w-full h-full object-cover rounded"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-text-meta text-xs">
              No Image
            </div>
          )}
        </div>

        {/* Info */}
        <div className="flex-1">
          <h3 className="text-lg font-semibold text-text-heading mb-1">
            {item.product.name}
          </h3>
          <p className="text-text-meta text-sm mb-3">
            {formatPrice(item.price)}
          </p>

          {/* Quantity Controls */}
          <div className="flex items-center gap-3">
            <button
              onClick={() => handleQuantityChange(-1)}
              disabled={item.quantity <= 1}
              className="w-8 h-8 rounded border border-border-default flex items-center justify-center hover:bg-bg-subtle disabled:opacity-50"
            >
              -
            </button>

            <span className="w-12 text-center font-medium">
              {item.quantity}
            </span>

            <button
              onClick={() => handleQuantityChange(1)}
              className="w-8 h-8 rounded border border-border-default flex items-center justify-center hover:bg-bg-subtle"
            >
              +
            </button>

            <button
              onClick={handleRemove}
              className="ml-auto text-status-error hover:underline text-sm"
            >
              삭제
            </button>
          </div>
        </div>

        {/* Total Price */}
        <div className="text-right">
          <p className="text-lg font-bold text-brand-primary">
            {formatPrice(item.price * item.quantity)}
          </p>
        </div>
      </div>
    </div>
  )
})
```

---

## 5. 낙관적 업데이트 (Optimistic Update)

### 5.1 개념

```tsx
// 1. 즉시 UI 업데이트 (사용자에게 빠른 피드백)
set(state => ({
  cart: { ...state.cart, items: updatedItems }
}))

// 2. 서버에 요청
const response = await cartApi.updateItemQuantity(itemId, quantity)

// 3. 서버 응답으로 동기화
set({ cart: response.data })

// 4. 실패 시 롤백
catch (error) {
  set({ cart: originalCart })
}
```

---

## 6. 핵심 정리

| 기능 | 구현 |
|------|------|
| **상태 관리** | Zustand + devtools + persist |
| **조회** | `fetchCart()` |
| **추가** | `addItem(productId, quantity)` |
| **수량 변경** | `updateItemQuantity(itemId, quantity)` + 낙관적 업데이트 |
| **삭제** | `removeItem(itemId)` |
| **비우기** | `clearCart()` |
| **최적화** | React.memo, useCallback |

---

## 다음 학습

- [Checkout Flow](./checkout-flow.md)
- [Product Detail](./product-detail.md)
- [Zustand 상태 관리](../react/zustand-state.md)
