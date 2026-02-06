# Custom Hooks 패턴

## 학습 목표
- Custom Hooks 작성 규칙과 패턴 이해
- Shopping Frontend의 실제 Custom Hooks 분석
- 재사용 가능한 로직 추상화 기법 학습

---

## 1. Custom Hooks 개요

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          CUSTOM HOOKS                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   로직 재사용      ─────►  여러 컴포넌트에서 공통 로직 공유                   │
│   관심사 분리      ─────►  컴포넌트는 UI에만 집중                             │
│   테스트 용이      ─────►  독립적으로 테스트 가능                             │
│   코드 가독성      ─────►  명확한 인터페이스로 이해 쉬움                      │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.1 명명 규칙

```tsx
// ✅ 올바른 명명 (use로 시작)
function useProducts() { }
function useCoupons() { }
function useAuth() { }

// ❌ 잘못된 명명
function getProducts() { } // 일반 함수처럼 보임
function fetchCoupons() { } // Hook인지 불명확
```

### 1.2 Hook 규칙 준수

```tsx
function useCustomHook() {
  // ✅ 다른 Hooks 호출 가능
  const [state, setState] = useState(null)
  useEffect(() => {}, [])

  // ✅ 조건 없이 최상위에서 호출
  const callback = useCallback(() => {}, [])

  // ❌ 조건부 호출 금지
  if (condition) {
    useState(0) // 위반!
  }

  return { state, setState }
}
```

---

## 2. 데이터 페칭 Hook 패턴

### 2.1 기본 패턴

**Portal Universe 코드 분석 (useCoupons.ts)**
```tsx
export function useAvailableCoupons() {
  // 1. 상태 정의
  const [data, setData] = useState<Coupon[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)

  // 2. 데이터 페칭 함수 (useCallback으로 메모이제이션)
  const fetchCoupons = useCallback(async () => {
    try {
      setIsLoading(true)
      setError(null)
      const response = await couponApi.getAvailableCoupons()
      if (response.success) {
        setData(response.data)
      }
    } catch (e) {
      setError(e instanceof Error ? e : new Error('Failed to fetch coupons'))
    } finally {
      setIsLoading(false)
    }
  }, []) // 의존성 없음 - API 호출만 수행

  // 3. 초기 로드
  useEffect(() => {
    fetchCoupons()
  }, [fetchCoupons])

  // 4. 인터페이스 반환
  return { data, isLoading, error, refetch: fetchCoupons }
}
```

### 2.2 사용 예시

```tsx
function CouponListPage() {
  // 간단한 인터페이스로 데이터 페칭
  const { data: coupons, isLoading, error, refetch } = useAvailableCoupons()

  if (isLoading) return <Spinner />
  if (error) return <ErrorMessage error={error} onRetry={refetch} />

  return (
    <div>
      {coupons.map(coupon => (
        <CouponCard key={coupon.id} coupon={coupon} />
      ))}
    </div>
  )
}
```

---

## 3. Mutation Hook 패턴

### 3.1 기본 구조

**Portal Universe 코드 분석 (useCoupons.ts)**
```tsx
export function useIssueCoupon() {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<Error | null>(null)

  const issueCoupon = useCallback(async (couponId: number) => {
    try {
      setIsLoading(true)
      setError(null)
      const response = await couponApi.issueCoupon(couponId)
      return response.data // 성공 시 데이터 반환
    } catch (e) {
      const err = e instanceof Error ? e : new Error('Failed to issue coupon')
      setError(err)
      throw err // 호출자에게 에러 전파
    } finally {
      setIsLoading(false)
    }
  }, [])

  return { mutateAsync: issueCoupon, isPending: isLoading, error }
}
```

### 3.2 사용 예시

```tsx
function CouponCard({ coupon }: Props) {
  const { mutateAsync, isPending, error } = useIssueCoupon()
  const { refetch: refetchUserCoupons } = useUserCoupons()

  const handleIssue = async () => {
    try {
      await mutateAsync(coupon.id)
      alert('쿠폰이 발급되었습니다!')
      refetchUserCoupons() // 목록 새로고침
    } catch (error) {
      // 에러는 이미 상태에 저장됨
      console.error(error)
    }
  }

  return (
    <div>
      <button onClick={handleIssue} disabled={isPending}>
        {isPending ? '발급 중...' : '쿠폰 받기'}
      </button>
      {error && <p className="text-red-500">{error.message}</p>}
    </div>
  )
}
```

---

## 4. Portal 통합 Hook

### 4.1 usePortalTheme

**Portal Universe 코드 분석 (usePortalStore.ts)**
```tsx
import { useState, useEffect } from 'react'

interface PortalTheme {
  isDark: boolean
  isConnected: boolean
}

export function usePortalTheme(): PortalTheme {
  const [theme, setTheme] = useState<PortalTheme>({
    isDark: false,
    isConnected: false
  })

  useEffect(() => {
    // Portal Shell과의 연동 확인
    const isEmbedded = (window as any).__POWERED_BY_PORTAL_SHELL__ === true

    if (!isEmbedded) {
      return // Standalone 모드에서는 연결 안 됨
    }

    try {
      // Module Federation으로 Portal Shell의 themeStore import
      import('portal/stores').then((module) => {
        const themeStore = module.useThemeStore.getState()

        // 초기 테마 설정
        setTheme({
          isDark: themeStore.isDark,
          isConnected: true
        })

        // 테마 변경 구독
        const unsubscribe = module.useThemeStore.subscribe((state) => {
          setTheme({
            isDark: state.isDark,
            isConnected: true
          })
        })

        return () => unsubscribe()
      })
    } catch (error) {
      console.warn('[Shopping] Failed to connect Portal themeStore:', error)
    }
  }, [])

  return theme
}
```

### 4.2 App.tsx에서 사용

```tsx
function App({ theme = 'light' }: AppProps) {
  const isEmbedded = window.__POWERED_BY_PORTAL_SHELL__ === true
  const portalTheme = usePortalTheme()

  // Embedded 모드에서 연결되면 Portal 테마 사용
  const isDark = isEmbedded && portalTheme.isConnected
    ? portalTheme.isDark
    : theme === 'dark'

  useEffect(() => {
    document.documentElement.classList.toggle('dark', isDark)
  }, [isDark])

  return <div>...</div>
}
```

---

## 5. 유틸리티 Hook 패턴

### 5.1 useDebounce

```tsx
import { useState, useEffect } from 'react'

function useDebounce<T>(value: T, delay: number = 500): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value)

  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedValue(value)
    }, delay)

    return () => {
      clearTimeout(handler)
    }
  }, [value, delay])

  return debouncedValue
}

// 사용 예시
function SearchBar() {
  const [searchInput, setSearchInput] = useState('')
  const debouncedSearch = useDebounce(searchInput, 300)

  useEffect(() => {
    if (debouncedSearch) {
      searchProducts(debouncedSearch)
    }
  }, [debouncedSearch])

  return (
    <input
      value={searchInput}
      onChange={(e) => setSearchInput(e.target.value)}
      placeholder="Search..."
    />
  )
}
```

### 5.2 useLocalStorage

```tsx
import { useState, useEffect } from 'react'

function useLocalStorage<T>(key: string, initialValue: T) {
  const [value, setValue] = useState<T>(() => {
    try {
      const item = window.localStorage.getItem(key)
      return item ? JSON.parse(item) : initialValue
    } catch (error) {
      console.warn(`Error loading ${key} from localStorage`, error)
      return initialValue
    }
  })

  useEffect(() => {
    try {
      window.localStorage.setItem(key, JSON.stringify(value))
    } catch (error) {
      console.warn(`Error saving ${key} to localStorage`, error)
    }
  }, [key, value])

  return [value, setValue] as const
}

// 사용 예시
function ThemeToggle() {
  const [theme, setTheme] = useLocalStorage<'light' | 'dark'>('theme', 'light')

  return (
    <button onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')}>
      Current: {theme}
    </button>
  )
}
```

### 5.3 useMediaQuery

```tsx
import { useState, useEffect } from 'react'

function useMediaQuery(query: string): boolean {
  const [matches, setMatches] = useState(() => {
    if (typeof window !== 'undefined') {
      return window.matchMedia(query).matches
    }
    return false
  })

  useEffect(() => {
    const mediaQuery = window.matchMedia(query)
    const handler = (event: MediaQueryListEvent) => {
      setMatches(event.matches)
    }

    mediaQuery.addEventListener('change', handler)
    return () => mediaQuery.removeEventListener('change', handler)
  }, [query])

  return matches
}

// 사용 예시
function ResponsiveComponent() {
  const isMobile = useMediaQuery('(max-width: 768px)')
  const isDesktop = useMediaQuery('(min-width: 1024px)')

  return (
    <div>
      {isMobile && <MobileMenu />}
      {isDesktop && <DesktopSidebar />}
    </div>
  )
}
```

---

## 6. 복잡한 로직 Hook

### 6.1 useTimeDeals - 타임딜 관리

**Portal Universe 코드 분석 (useTimeDeals.ts)**
```tsx
export function useTimeDeals(params?: {
  status?: TimeDealStatus
  page?: number
  size?: number
}) {
  const [data, setData] = useState<PageResponse<TimeDeal> | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)

  const fetchTimeDeals = useCallback(async () => {
    try {
      setIsLoading(true)
      setError(null)

      const response = await timeDealApi.getTimeDeals({
        status: params?.status,
        page: params?.page ?? 0,
        size: params?.size ?? 10
      })

      if (response.success) {
        setData(response.data)
      }
    } catch (e) {
      setError(e instanceof Error ? e : new Error('Failed to fetch time deals'))
    } finally {
      setIsLoading(false)
    }
  }, [params?.status, params?.page, params?.size])

  useEffect(() => {
    fetchTimeDeals()
  }, [fetchTimeDeals])

  return { data, isLoading, error, refetch: fetchTimeDeals }
}
```

### 6.2 useQueue - 대기열 관리

```tsx
export function useQueue(eventType: string, eventId: number) {
  const [status, setStatus] = useState<QueueStatus | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const checkStatus = useCallback(async () => {
    try {
      const response = await queueApi.getStatus(eventType, eventId)
      setStatus(response.data)
    } catch (error) {
      console.error('Failed to check queue status', error)
    }
  }, [eventType, eventId])

  useEffect(() => {
    setIsLoading(true)
    checkStatus().finally(() => setIsLoading(false))

    // 5초마다 폴링
    const interval = setInterval(checkStatus, 5000)

    return () => clearInterval(interval)
  }, [checkStatus])

  return { status, isLoading, refetch: checkStatus }
}
```

---

## 7. Custom Hook 설계 원칙

### 7.1 단일 책임 원칙

```tsx
// ❌ 너무 많은 책임
function useEverything() {
  const products = useProducts()
  const cart = useCart()
  const user = useUser()
  const theme = useTheme()
  // ...
  return { products, cart, user, theme }
}

// ✅ 단일 책임
function useProducts() { /* 상품 관련 로직만 */ }
function useCart() { /* 장바구니 로직만 */ }
```

### 7.2 명확한 인터페이스

```tsx
// ✅ 일관된 인터페이스 (React Query 스타일)
function useProducts() {
  return {
    data: products,
    isLoading,
    error,
    refetch
  }
}

function useCoupons() {
  return {
    data: coupons, // 동일한 이름
    isLoading,     // 동일한 이름
    error,         // 동일한 이름
    refetch        // 동일한 이름
  }
}
```

### 7.3 의존성 최소화

```tsx
// ❌ 과도한 의존성
function useFetchData(url, method, headers, body, retry, timeout) {
  // ...
}

// ✅ 옵션 객체 사용
function useFetchData(url: string, options?: FetchOptions) {
  const { method = 'GET', headers, body, retry, timeout } = options || {}
  // ...
}
```

---

## 8. 핵심 정리

| 패턴 | 용도 | 예시 |
|------|------|------|
| **Data Fetching** | API 호출 | `useProducts`, `useCoupons` |
| **Mutation** | 데이터 변경 | `useIssueCoupon` |
| **Portal Integration** | Host 통신 | `usePortalTheme`, `usePortalStore` |
| **Utility** | 재사용 로직 | `useDebounce`, `useMediaQuery` |
| **Complex Logic** | 복잡한 비즈니스 로직 | `useQueue`, `useTimeDeals` |

---

## 9. 테스트

```tsx
import { renderHook, waitFor } from '@testing-library/react'
import { useAvailableCoupons } from './useCoupons'

describe('useAvailableCoupons', () => {
  it('fetches coupons on mount', async () => {
    const { result } = renderHook(() => useAvailableCoupons())

    expect(result.current.isLoading).toBe(true)

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false)
    })

    expect(result.current.data).toBeDefined()
  })
})
```

---

## 다음 학습

- [API Integration](./api-integration.md)
- [Render Optimization](./render-optimization.md)
- [Form Handling](./form-handling.md)
