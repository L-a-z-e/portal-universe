# Zustand 상태 관리

## 학습 목표
- Zustand의 핵심 개념과 API 이해
- Redux 대비 Zustand의 장점 학습
- 실제 프로젝트 적용 패턴 학습

---

## 1. Zustand 소개

### 1.1 특징

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              ZUSTAND                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ✓ 최소한의 보일러플레이트                                                  │
│   ✓ 타입스크립트 지원 내장                                                   │
│   ✓ Provider 불필요                                                          │
│   ✓ 미들웨어 지원 (devtools, persist, immer)                                │
│   ✓ 번들 사이즈 < 1KB                                                       │
│   ✓ React 외부에서도 사용 가능                                               │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Redux vs Zustand

| 특성 | Redux | Zustand |
|------|-------|---------|
| 보일러플레이트 | 많음 (action, reducer, dispatch) | 최소 |
| Provider | 필수 | 불필요 |
| 미들웨어 | redux-thunk, saga 등 | 내장 |
| 번들 사이즈 | ~7KB | ~1KB |
| 학습 곡선 | 높음 | 낮음 |
| DevTools | 별도 설치 | 내장 옵션 |

---

## 2. 기본 사용법

### 2.1 Store 생성

```typescript
import { create } from 'zustand'

interface CounterState {
  count: number
  increment: () => void
  decrement: () => void
  reset: () => void
}

const useCounterStore = create<CounterState>((set) => ({
  // State
  count: 0,

  // Actions
  increment: () => set((state) => ({ count: state.count + 1 })),
  decrement: () => set((state) => ({ count: state.count - 1 })),
  reset: () => set({ count: 0 }),
}))

export default useCounterStore
```

### 2.2 컴포넌트에서 사용

```tsx
function Counter() {
  // 전체 상태 구독 (비권장 - 불필요한 리렌더링)
  const { count, increment, decrement } = useCounterStore()

  return (
    <div>
      <span>{count}</span>
      <button onClick={increment}>+</button>
      <button onClick={decrement}>-</button>
    </div>
  )
}
```

### 2.3 선택적 구독 (권장)

```tsx
function CountDisplay() {
  // 필요한 상태만 구독 (성능 최적화)
  const count = useCounterStore((state) => state.count)
  return <span>{count}</span>
}

function CountActions() {
  // 액션만 구독 (리렌더링 없음)
  const increment = useCounterStore((state) => state.increment)
  const decrement = useCounterStore((state) => state.decrement)

  return (
    <>
      <button onClick={increment}>+</button>
      <button onClick={decrement}>-</button>
    </>
  )
}
```

---

## 3. Cart Store 분석

### 3.1 구조

```typescript
import { create } from 'zustand'
import { devtools, persist } from 'zustand/middleware'

interface CartState {
  // State
  cart: Cart | null
  loading: boolean
  error: string | null
  itemCount: number
  totalAmount: number

  // Actions
  fetchCart: () => Promise<void>
  addItem: (productId: number, productName: string,
            price: number, quantity: number) => Promise<void>
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
```

### 3.2 미들웨어 적용

```typescript
export const useCartStore = create<CartState>()(
  devtools(                          // Redux DevTools 지원
    persist(                         // localStorage 저장
      (set, get) => ({
        ...initialState,

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
                  id: 0, userId: '', status: 'ACTIVE',
                  items: [], totalAmount: 0, itemCount: 0,
                  totalQuantity: 0, createdAt: new Date().toISOString()
                },
                itemCount: 0, totalAmount: 0, loading: false
              })
            } else {
              set({
                error: error.message || 'Failed to fetch cart',
                loading: false
              })
            }
          }
        },

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
          } catch (error: any) {
            set({
              error: error.response?.data?.error?.message ||
                     error.message || 'Failed to add item',
              loading: false
            })
            throw error  // 호출자에게 에러 전파
          }
        },

        // ... 기타 액션들
      }),
      {
        name: 'shopping-cart-storage',  // localStorage 키
        partialize: (state) => ({
          // 일부 상태만 저장 (민감 정보 제외)
          itemCount: state.itemCount,
          totalAmount: state.totalAmount
        })
      }
    ),
    { name: 'CartStore' }  // DevTools 표시 이름
  )
)
```

---

## 4. 미들웨어

### 4.1 devtools

```typescript
import { devtools } from 'zustand/middleware'

const useStore = create(
  devtools(
    (set) => ({
      // ...
    }),
    { name: 'MyStore' }  // Redux DevTools에 표시될 이름
  )
)
```

**DevTools 기능:**
- State 변경 이력 추적
- Time-travel debugging
- Action 이름 표시

### 4.2 persist

```typescript
import { persist } from 'zustand/middleware'

const useStore = create(
  persist(
    (set) => ({
      user: null,
      setUser: (user) => set({ user })
    }),
    {
      name: 'user-storage',      // localStorage 키
      storage: localStorage,      // 기본값
      partialize: (state) => ({   // 저장할 상태 선택
        user: state.user
      }),
      version: 1,                 // 버전 관리
      migrate: (state, version) => {  // 마이그레이션
        if (version === 0) {
          return { ...state, newField: 'default' }
        }
        return state
      }
    }
  )
)
```

### 4.3 immer (불변성 헬퍼)

```typescript
import { immer } from 'zustand/middleware/immer'

const useStore = create(
  immer((set) => ({
    items: [],

    addItem: (item) => set((state) => {
      // immer 덕분에 직접 수정 가능
      state.items.push(item)
    }),

    updateItem: (id, updates) => set((state) => {
      const item = state.items.find(i => i.id === id)
      if (item) {
        Object.assign(item, updates)
      }
    })
  }))
)
```

---

## 5. 고급 패턴

### 5.1 Computed Values (Getter)

```typescript
interface CartState {
  items: CartItem[]
  // Computed getter
  get totalPrice(): number
}

// Zustand에서는 get() 사용
const useCartStore = create<CartState>((set, get) => ({
  items: [],

  // 직접 계산
  getTotalPrice: () => {
    return get().items.reduce((sum, item) =>
      sum + item.price * item.quantity, 0
    )
  },

  // 또는 별도 selector 정의
}))

// 컴포넌트에서 selector 사용
const totalPrice = useCartStore(
  (state) => state.items.reduce((sum, item) =>
    sum + item.price * item.quantity, 0
  )
)
```

### 5.2 Slices Pattern (대규모 Store)

```typescript
// cartSlice.ts
export const createCartSlice = (set: any, get: any) => ({
  cart: null,
  loading: false,
  addToCart: (item: CartItem) => set((state: any) => ({
    cart: { ...state.cart, items: [...state.cart.items, item] }
  }))
})

// userSlice.ts
export const createUserSlice = (set: any) => ({
  user: null,
  setUser: (user: User) => set({ user })
})

// store.ts
import { create } from 'zustand'

const useStore = create((...args) => ({
  ...createCartSlice(...args),
  ...createUserSlice(...args),
}))

export const useCartStore = () => useStore((state) => ({
  cart: state.cart,
  loading: state.loading,
  addToCart: state.addToCart
}))
```

### 5.3 React 외부에서 사용

```typescript
// API 클라이언트에서 직접 사용
import { useCartStore } from './stores/cartStore'

// getState()로 현재 상태 조회
const currentCart = useCartStore.getState().cart

// setState()로 상태 변경
useCartStore.setState({ loading: true })

// subscribe()로 변경 감지
const unsubscribe = useCartStore.subscribe((state) => {
  console.log('Cart changed:', state.cart)
})
```

---

## 6. 성능 최적화

### 6.1 Shallow 비교

```typescript
import { shallow } from 'zustand/shallow'

// 여러 값 선택 시 shallow 사용
const { cart, loading } = useCartStore(
  (state) => ({ cart: state.cart, loading: state.loading }),
  shallow  // 객체 얕은 비교
)
```

### 6.2 메모이제이션

```typescript
import { useMemo } from 'react'

function CartSummary() {
  const items = useCartStore((state) => state.cart?.items ?? [])

  // 파생 데이터 메모이제이션
  const summary = useMemo(() => ({
    totalItems: items.length,
    totalPrice: items.reduce((sum, item) =>
      sum + item.price * item.quantity, 0
    )
  }), [items])

  return <div>{summary.totalItems} items - ${summary.totalPrice}</div>
}
```

---

## 7. 테스트

### 7.1 Store 모킹

```typescript
import { useCartStore } from './cartStore'

describe('CartStore', () => {
  beforeEach(() => {
    // Store 초기화
    useCartStore.setState({
      cart: null,
      loading: false,
      error: null,
      itemCount: 0,
      totalAmount: 0
    })
  })

  it('adds item to cart', async () => {
    const { addItem } = useCartStore.getState()

    await addItem(1, 'Product', 100, 2)

    const { cart } = useCartStore.getState()
    expect(cart?.items).toHaveLength(1)
  })
})
```

### 7.2 컴포넌트 테스트

```typescript
import { render, screen } from '@testing-library/react'
import { useCartStore } from './cartStore'

beforeEach(() => {
  useCartStore.setState({
    cart: {
      items: [{ id: 1, name: 'Test', price: 100, quantity: 2 }],
      totalAmount: 200,
      itemCount: 1
    }
  })
})

test('displays cart items', () => {
  render(<CartList />)
  expect(screen.getByText('Test')).toBeInTheDocument()
})
```

---

## 8. 핵심 정리

| 개념 | 설명 |
|------|------|
| `create` | Store 생성 함수 |
| `set` | 상태 업데이트 함수 |
| `get` | 현재 상태 조회 함수 |
| `devtools` | Redux DevTools 통합 |
| `persist` | localStorage 자동 저장 |
| `shallow` | 성능 최적화를 위한 얕은 비교 |
| `getState()` | React 외부에서 상태 조회 |
| `subscribe()` | 상태 변경 구독 |

---

## 다음 학습

- [Custom Hooks 패턴](./custom-hooks.md)
- [React Query 통합](./api-integration.md)
- [상태 동기화](../mfe/portal-integration.md)
