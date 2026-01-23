# React Hooks Deep Dive

## 학습 목표
- useState, useEffect, useCallback, useMemo 동작 원리 이해
- Hooks 사용 시 성능 최적화 패턴 학습
- Shopping Frontend 실제 코드 분석을 통한 실전 활용

---

## 1. Hooks 개요

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           REACT HOOKS                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   useState      ─────►  상태 관리                                            │
│   useEffect     ─────►  부수 효과 (Side Effect)                              │
│   useCallback   ─────►  함수 메모이제이션                                     │
│   useMemo       ─────►  값 메모이제이션                                       │
│   useRef        ─────►  DOM 참조 & 변경 가능한 값                             │
│   useContext    ─────►  Context 구독                                         │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.1 Hooks 규칙

| 규칙 | 설명 |
|------|------|
| **최상위 호출** | 조건문, 반복문, 중첩 함수 내부에서 호출 금지 |
| **함수 컴포넌트에서만** | 일반 JavaScript 함수에서 호출 금지 (Custom Hooks 제외) |
| **호출 순서 유지** | 매 렌더링마다 동일한 순서로 호출되어야 함 |

---

## 2. useState - 상태 관리

### 2.1 기본 사용법

```tsx
import { useState } from 'react'

function Counter() {
  // [현재 값, setter 함수] = useState(초기값)
  const [count, setCount] = useState(0)

  return (
    <div>
      <p>Count: {count}</p>
      <button onClick={() => setCount(count + 1)}>Increment</button>
    </div>
  )
}
```

### 2.2 함수형 업데이트

```tsx
// ❌ 이전 상태 기반 업데이트 (잘못된 방법)
setCount(count + 1)
setCount(count + 1) // 여전히 1만 증가

// ✅ 함수형 업데이트 (올바른 방법)
setCount(prev => prev + 1)
setCount(prev => prev + 1) // 2 증가
```

### 2.3 Portal Universe 코드 분석

**ProductListPage.tsx**
```tsx
const [products, setProducts] = useState<Product[]>([])
const [inventories, setInventories] = useState<Map<number, Inventory>>(new Map())
const [totalPages, setTotalPages] = useState(0)
const [loading, setLoading] = useState(true)
const [error, setError] = useState<string | null>(null)

// 여러 상태 동시 업데이트 패턴
const fetchProducts = async () => {
  setLoading(true)
  setError(null)

  try {
    const response = await productApi.getProducts()
    setProducts(response.data.content)
    setTotalPages(response.data.totalPages)
  } catch (err: any) {
    setError(err.message)
  } finally {
    setLoading(false) // 성공/실패 관계없이 실행
  }
}
```

### 2.4 초기값 지연 계산

```tsx
// ❌ 매 렌더링마다 실행 (비효율)
const [state, setState] = useState(expensiveCalculation())

// ✅ 초기 렌더링에만 실행
const [state, setState] = useState(() => expensiveCalculation())
```

---

## 3. useEffect - 부수 효과

### 3.1 기본 구조

```tsx
useEffect(() => {
  // 실행할 코드 (Effect)

  return () => {
    // 정리 함수 (Cleanup)
  }
}, [dependencies]) // 의존성 배열
```

### 3.2 의존성 배열

| 패턴 | 동작 |
|------|------|
| `[]` | 마운트 시에만 실행 |
| `[a, b]` | a 또는 b가 변경될 때 실행 |
| 없음 | 매 렌더링마다 실행 |

### 3.3 Portal Universe 코드 분석

**ProductListPage.tsx - 데이터 페칭**
```tsx
// fetchProducts가 변경될 때마다 실행
useEffect(() => {
  fetchProducts()
}, [fetchProducts])

// ⚠️ 주의: fetchProducts는 useCallback으로 메모이제이션되어야 함
const fetchProducts = useCallback(async () => {
  // ...
}, [currentPage, searchKeyword, category])
```

**CheckoutPage.tsx - 초기화 및 검증**
```tsx
// 마운트 시 장바구니 조회
useEffect(() => {
  fetchCart()
}, [fetchCart])

// 장바구니가 비었으면 리다이렉트
useEffect(() => {
  if (cart && cart.items.length === 0 && step === 'address') {
    navigate('/cart')
  }
}, [cart, step, navigate])
```

**App.tsx - 테마 동기화**
```tsx
useEffect(() => {
  // 테마 클래스 적용
  if (isDark) {
    document.documentElement.classList.add('dark')
  } else {
    document.documentElement.classList.remove('dark')
  }
  updateDataTheme(isDark)
}, [isDark, isEmbedded, portalTheme.isConnected])
```

### 3.4 Cleanup 함수 활용

```tsx
useEffect(() => {
  // 이벤트 리스너 등록
  const handleResize = () => console.log(window.innerWidth)
  window.addEventListener('resize', handleResize)

  // 컴포넌트 언마운트 시 제거 (메모리 누수 방지)
  return () => {
    window.removeEventListener('resize', handleResize)
  }
}, [])
```

---

## 4. useCallback - 함수 메모이제이션

### 4.1 목적

```tsx
// ❌ 매 렌더링마다 새 함수 생성
const handleClick = () => {
  console.log(value)
}

// ✅ 의존성 변경 시에만 새 함수 생성
const handleClick = useCallback(() => {
  console.log(value)
}, [value])
```

### 4.2 언제 사용할까?

| 상황 | 사용 여부 |
|------|----------|
| 자식 컴포넌트에 props로 전달 | ✅ |
| useEffect 의존성 배열에 포함 | ✅ |
| 간단한 이벤트 핸들러 | ❌ (오버헤드만 증가) |

### 4.3 Portal Universe 코드 분석

**ProductListPage.tsx**
```tsx
// 페이지/검색어/카테고리 변경 시에만 새 함수 생성
const fetchProducts = useCallback(async () => {
  setLoading(true)
  setError(null)

  try {
    let response
    if (searchKeyword) {
      response = await productApi.searchProducts(searchKeyword, currentPage, 12)
    } else {
      response = await productApi.getProducts(currentPage, 12, category || undefined)
    }

    setProducts(response.data.content)
    setTotalPages(response.data.totalPages)
  } catch (err: any) {
    setError(err.message)
  } finally {
    setLoading(false)
  }
}, [currentPage, searchKeyword, category])

// useEffect에서 안전하게 사용
useEffect(() => {
  fetchProducts()
}, [fetchProducts])
```

**useCoupons.ts**
```tsx
export function useAvailableCoupons() {
  const [data, setData] = useState<Coupon[]>([])

  // refetch 함수를 메모이제이션
  const fetchCoupons = useCallback(async () => {
    try {
      const response = await couponApi.getAvailableCoupons()
      if (response.success) {
        setData(response.data)
      }
    } catch (e) {
      // error handling
    }
  }, []) // 의존성 없음 - 항상 같은 함수

  useEffect(() => {
    fetchCoupons()
  }, [fetchCoupons])

  return { data, refetch: fetchCoupons }
}
```

---

## 5. useMemo - 값 메모이제이션

### 5.1 기본 사용법

```tsx
// ❌ 매 렌더링마다 재계산
const filteredList = items.filter(item => item.active)

// ✅ items가 변경될 때만 재계산
const filteredList = useMemo(
  () => items.filter(item => item.active),
  [items]
)
```

### 5.2 언제 사용할까?

| 상황 | 사용 여부 |
|------|----------|
| 비용이 큰 계산 (필터링, 정렬) | ✅ |
| 참조 동일성이 필요한 객체/배열 | ✅ |
| 간단한 계산 | ❌ |

### 5.3 실습 예제 - 상품 필터링

```tsx
function ProductList({ products, filters }: Props) {
  // 필터링 & 정렬 (비용이 큰 연산)
  const filteredProducts = useMemo(() => {
    return products
      .filter(p => p.category === filters.category)
      .filter(p => p.price >= filters.minPrice && p.price <= filters.maxPrice)
      .sort((a, b) => {
        if (filters.sortBy === 'price') return a.price - b.price
        return a.name.localeCompare(b.name)
      })
  }, [products, filters])

  return (
    <div>
      {filteredProducts.map(p => <ProductCard key={p.id} product={p} />)}
    </div>
  )
}
```

---

## 6. useRef - DOM 참조 & 변경 가능한 값

### 6.1 DOM 요소 참조

```tsx
import { useRef, useEffect } from 'react'

function InputFocus() {
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    // 마운트 시 포커스
    inputRef.current?.focus()
  }, [])

  return <input ref={inputRef} />
}
```

### 6.2 변경 가능한 값 저장 (리렌더링 트리거 없음)

```tsx
function Timer() {
  const intervalRef = useRef<number | null>(null)

  const start = () => {
    intervalRef.current = setInterval(() => {
      console.log('Tick')
    }, 1000)
  }

  const stop = () => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current)
    }
  }

  return (
    <>
      <button onClick={start}>Start</button>
      <button onClick={stop}>Stop</button>
    </>
  )
}
```

### 6.3 Portal Universe 코드 분석

**router/index.tsx - 이전 경로 추적**
```tsx
const NavigationSync: React.FC = () => {
  const location = useLocation()
  const prevPathRef = useRef(location.pathname)

  useEffect(() => {
    if (prevPathRef.current !== location.pathname) {
      console.log(`Path changed: ${prevPathRef.current} → ${location.pathname}`)
      prevPathRef.current = location.pathname // 값 갱신 (리렌더링 없음)
      navigationCallback?.(location.pathname)
    }
  }, [location.pathname])

  return null
}
```

**router/index.tsx - Router 인스턴스 보존**
```tsx
export const ShoppingRouter: React.FC<ShoppingRouterProps> = ({
  isEmbedded,
  initialPath,
  onNavigate
}) => {
  // 라우터 인스턴스를 ref에 저장 (재생성 방지)
  const routerRef = useRef<RouterInstance | null>(null)

  if (!routerRef.current) {
    routerRef.current = createRouter({ isEmbedded, initialPath })
    routerInstance = routerRef.current
  }

  return <RouterProvider router={routerRef.current} />
}
```

---

## 7. Hooks 조합 패턴

### 7.1 데이터 페칭 패턴

```tsx
function useProduct(productId: number) {
  const [data, setData] = useState<Product | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)

  const fetchData = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const response = await productApi.getProduct(productId)
      setData(response.data)
    } catch (e) {
      setError(e as Error)
    } finally {
      setLoading(false)
    }
  }, [productId])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  return { data, loading, error, refetch: fetchData }
}
```

### 7.2 폼 관리 패턴

```tsx
function useForm<T>(initialValues: T) {
  const [values, setValues] = useState(initialValues)

  const handleChange = useCallback((field: keyof T, value: any) => {
    setValues(prev => ({ ...prev, [field]: value }))
  }, [])

  const reset = useCallback(() => {
    setValues(initialValues)
  }, [initialValues])

  return { values, handleChange, reset }
}
```

---

## 8. 성능 최적화 체크리스트

- [ ] useCallback: 자식에 전달하거나 의존성으로 사용되는 함수만 적용
- [ ] useMemo: 비용이 큰 계산만 메모이제이션
- [ ] useState 초기값: 복잡한 계산은 함수로 지연 실행
- [ ] useEffect 의존성: 필요한 값만 포함 (린트 경고 무시 금지)
- [ ] 과도한 최적화 지양: 측정 후 필요한 경우에만 적용

---

## 9. 핵심 정리

| Hook | 용도 | 주의사항 |
|------|------|----------|
| `useState` | 상태 관리 | 함수형 업데이트 활용 |
| `useEffect` | 부수 효과 | Cleanup 함수 작성 |
| `useCallback` | 함수 메모이제이션 | 자식 props, 의존성에만 사용 |
| `useMemo` | 값 메모이제이션 | 비용 큰 계산에만 사용 |
| `useRef` | DOM/값 참조 | 리렌더링 트리거 안 됨 |

---

## 다음 학습

- [React Router v6](./react-router-v6.md)
- [Custom Hooks 패턴](./custom-hooks.md)
- [Render 최적화](./render-optimization.md)
