# Error Boundary

## 학습 목표
- Error Boundary 개념과 동작 원리 이해
- Class 기반 Error Boundary 구현
- Shopping Frontend 적용 방안 학습

---

## 1. Error Boundary 개요

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ERROR BOUNDARY                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   렌더링 중 에러 포착  ─────►  하위 컴포넌트 트리의 에러 캐치                 │
│   Fallback UI 표시    ─────►  사용자에게 에러 메시지 표시                    │
│   에러 복구           ─────►  재시도, 리셋 기능 제공                          │
│   에러 로깅           ─────►  에러 리포팅 서비스에 전송                       │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.1 Error Boundary가 포착하는 에러

✅ **포착 가능**
- 렌더링 중 발생하는 에러
- 생명주기 메서드의 에러
- 하위 트리 전체의 생성자 에러

❌ **포착 불가능**
- 이벤트 핸들러 (try-catch 사용)
- 비동기 코드 (setTimeout, Promise)
- 서버 사이드 렌더링
- Error Boundary 자체의 에러

---

## 2. Class 기반 Error Boundary 구현

### 2.1 기본 구조

```tsx
import React, { Component, ErrorInfo, ReactNode } from 'react'

interface Props {
  children: ReactNode
  fallback?: ReactNode
}

interface State {
  hasError: boolean
  error: Error | null
}

class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = {
      hasError: false,
      error: null
    }
  }

  // 1. 에러 포착 (Render Phase)
  static getDerivedStateFromError(error: Error): State {
    // 다음 렌더링에서 Fallback UI 표시
    return {
      hasError: true,
      error
    }
  }

  // 2. 에러 로깅 (Commit Phase)
  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    // 에러 리포팅 서비스에 전송
    console.error('Error caught by boundary:', error, errorInfo)
    // 예: Sentry.captureException(error, { extra: errorInfo })
  }

  // 3. 에러 상태 리셋
  resetError = () => {
    this.setState({
      hasError: false,
      error: null
    })
  }

  render() {
    if (this.state.hasError) {
      // Fallback UI 렌더링
      return this.props.fallback || (
        <div className="error-boundary">
          <h2>Something went wrong</h2>
          <p>{this.state.error?.message}</p>
          <button onClick={this.resetError}>Try again</button>
        </div>
      )
    }

    return this.props.children
  }
}

export default ErrorBoundary
```

### 2.2 사용 예시

```tsx
function App() {
  return (
    <ErrorBoundary>
      <ProductList />
    </ErrorBoundary>
  )
}

function ProductList() {
  const [products, setProducts] = useState([])

  if (products.length === 0) {
    throw new Error('No products available') // ErrorBoundary가 포착
  }

  return (
    <div>
      {products.map(p => <ProductCard key={p.id} product={p} />)}
    </div>
  )
}
```

---

## 3. 향상된 Error Boundary

### 3.1 에러 타입별 처리

```tsx
interface Props {
  children: ReactNode
  onError?: (error: Error, errorInfo: ErrorInfo) => void
  fallbackRender?: (props: {
    error: Error
    resetError: () => void
  }) => ReactNode
}

class AdvancedErrorBoundary extends Component<Props, State> {
  // ... 생략 ...

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Error boundary caught:', error, errorInfo)

    // Custom error handler 호출
    this.props.onError?.(error, errorInfo)

    // 에러 타입별 처리
    if (error.name === 'ChunkLoadError') {
      // Code splitting 실패 시 페이지 새로고침
      window.location.reload()
    }
  }

  render() {
    if (this.state.hasError) {
      // Custom fallback render
      if (this.props.fallbackRender) {
        return this.props.fallbackRender({
          error: this.state.error!,
          resetError: this.resetError
        })
      }

      return <DefaultErrorFallback error={this.state.error!} resetError={this.resetError} />
    }

    return this.props.children
  }
}
```

### 3.2 Fallback 컴포넌트

```tsx
interface FallbackProps {
  error: Error
  resetError: () => void
}

function DefaultErrorFallback({ error, resetError }: FallbackProps) {
  return (
    <div className="min-h-[400px] flex items-center justify-center">
      <div className="max-w-md p-6 bg-bg-card border border-border-default rounded-lg">
        <div className="flex items-center gap-3 mb-4">
          <div className="w-12 h-12 rounded-full bg-status-error-bg flex items-center justify-center">
            <svg className="w-6 h-6 text-status-error" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </div>
          <h2 className="text-lg font-bold text-text-heading">
            Something went wrong
          </h2>
        </div>

        <p className="text-text-meta mb-4">
          {error.message || 'An unexpected error occurred'}
        </p>

        <div className="flex gap-3">
          <button
            onClick={resetError}
            className="px-4 py-2 bg-brand-primary text-white rounded-lg hover:bg-brand-primary/90 transition-colors"
          >
            Try again
          </button>
          <button
            onClick={() => window.location.href = '/'}
            className="px-4 py-2 bg-bg-subtle text-text-body rounded-lg hover:bg-bg-subtle/80 transition-colors"
          >
            Go to Home
          </button>
        </div>
      </div>
    </div>
  )
}
```

---

## 4. Portal Universe 적용

### 4.1 App 레벨 Error Boundary

```tsx
// App.tsx
import ErrorBoundary from '@/components/ErrorBoundary'

function App(props: AppProps) {
  return (
    <ErrorBoundary
      fallbackRender={({ error, resetError }) => (
        <div className="min-h-screen flex items-center justify-center">
          <div className="text-center">
            <h1 className="text-2xl font-bold mb-4">
              Shopping App Error
            </h1>
            <p className="text-text-meta mb-4">{error.message}</p>
            <button onClick={resetError} className="btn-primary">
              Reload App
            </button>
          </div>
        </div>
      )}
    >
      <div className="min-h-screen bg-bg-page">
        <ShoppingRouter
          isEmbedded={isEmbedded}
          initialPath={initialPath}
          onNavigate={onNavigate}
        />
      </div>
    </ErrorBoundary>
  )
}
```

### 4.2 Route 레벨 Error Boundary

```tsx
// router/index.tsx
const routes = [
  {
    path: '/',
    element: <Layout />,
    children: [
      {
        path: 'products',
        element: (
          <ErrorBoundary>
            <Suspense fallback={<PageLoader />}>
              <ProductListPage />
            </Suspense>
          </ErrorBoundary>
        )
      },
      {
        path: 'products/:productId',
        element: (
          <ErrorBoundary
            fallbackRender={({ error, resetError }) => (
              <div className="p-6">
                <h2>Failed to load product</h2>
                <p>{error.message}</p>
                <button onClick={resetError}>Retry</button>
                <Link to="/products">Back to Products</Link>
              </div>
            )}
          >
            <Suspense fallback={<PageLoader />}>
              <ProductDetailPage />
            </Suspense>
          </ErrorBoundary>
        )
      }
    ]
  }
]
```

---

## 5. 이벤트 핸들러 에러 처리

### 5.1 try-catch 사용

```tsx
function ProductCard({ product }: Props) {
  const { mutateAsync, isPending } = useIssueCoupon()

  const handleAddToCart = async () => {
    try {
      await cartApi.addItem({
        productId: product.id,
        quantity: 1
      })
      alert('Added to cart!')
    } catch (error) {
      // 이벤트 핸들러 에러는 Error Boundary가 포착 안 함
      console.error('Failed to add to cart:', error)
      alert('Failed to add to cart. Please try again.')
    }
  }

  return (
    <div className="card">
      <h3>{product.name}</h3>
      <button onClick={handleAddToCart} disabled={isPending}>
        Add to Cart
      </button>
    </div>
  )
}
```

### 5.2 Custom Hook으로 에러 처리

```tsx
function useAsyncError() {
  const [, setError] = useState()

  return (error: Error) => {
    setError(() => {
      throw error // Error Boundary가 포착
    })
  }
}

// 사용
function ProductCard({ product }: Props) {
  const throwError = useAsyncError()

  const handleAddToCart = async () => {
    try {
      await cartApi.addItem({ productId: product.id, quantity: 1 })
    } catch (error) {
      throwError(error as Error) // Error Boundary로 전파
    }
  }

  return <button onClick={handleAddToCart}>Add to Cart</button>
}
```

---

## 6. 비동기 에러 처리

### 6.1 Promise 에러

```tsx
// ❌ Error Boundary가 포착 안 함
function ProductList() {
  useEffect(() => {
    fetch('/api/products')
      .then(res => res.json())
      .then(data => setProducts(data))
    // 에러 핸들링 없음!
  }, [])
}

// ✅ try-catch + throwError
function ProductList() {
  const throwError = useAsyncError()

  useEffect(() => {
    const fetchProducts = async () => {
      try {
        const res = await fetch('/api/products')
        const data = await res.json()
        setProducts(data)
      } catch (error) {
        throwError(error as Error)
      }
    }
    fetchProducts()
  }, [throwError])
}
```

### 6.2 Custom Hook으로 추상화

```tsx
function useProducts() {
  const [data, setData] = useState<Product[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)

  useEffect(() => {
    const fetchData = async () => {
      try {
        setIsLoading(true)
        const response = await productApi.getProducts()
        setData(response.data.content)
      } catch (e) {
        setError(e as Error) // 에러를 상태로 저장
      } finally {
        setIsLoading(false)
      }
    }
    fetchData()
  }, [])

  return { data, isLoading, error }
}

// 컴포넌트에서 에러 처리
function ProductList() {
  const { data, isLoading, error } = useProducts()

  if (error) throw error // Error Boundary로 전파

  if (isLoading) return <Spinner />

  return (
    <div>
      {data.map(p => <ProductCard key={p.id} product={p} />)}
    </div>
  )
}
```

---

## 7. react-error-boundary 라이브러리

### 7.1 설치

```bash
npm install react-error-boundary
```

### 7.2 사용

```tsx
import { ErrorBoundary } from 'react-error-boundary'

function ErrorFallback({ error, resetErrorBoundary }: any) {
  return (
    <div role="alert">
      <p>Something went wrong:</p>
      <pre>{error.message}</pre>
      <button onClick={resetErrorBoundary}>Try again</button>
    </div>
  )
}

function App() {
  return (
    <ErrorBoundary
      FallbackComponent={ErrorFallback}
      onReset={() => {
        // 상태 리셋 로직
      }}
      onError={(error, errorInfo) => {
        // 에러 로깅
        console.error('Logged error:', error, errorInfo)
      }}
    >
      <ProductList />
    </ErrorBoundary>
  )
}
```

### 7.3 useErrorHandler Hook

```tsx
import { useErrorHandler } from 'react-error-boundary'

function ProductCard({ product }: Props) {
  const handleError = useErrorHandler()

  const handleClick = async () => {
    try {
      await addToCart(product.id)
    } catch (error) {
      handleError(error) // Error Boundary로 전파
    }
  }

  return <button onClick={handleClick}>Add to Cart</button>
}
```

---

## 8. 핵심 정리

| 개념 | 설명 |
|------|------|
| `getDerivedStateFromError` | 에러 포착 & 상태 업데이트 |
| `componentDidCatch` | 에러 로깅 |
| **포착 범위** | 렌더링, 생명주기, 생성자 |
| **미포착** | 이벤트 핸들러, 비동기 코드 |
| **Fallback UI** | 사용자 친화적 에러 화면 |
| **try-catch** | 이벤트/비동기 에러 처리 |

---

## 9. 실습 과제

다음 컴포넌트에 Error Boundary를 적용하세요:

```tsx
function ProductDetailPage() {
  const { productId } = useParams()
  const [product, setProduct] = useState<Product | null>(null)

  useEffect(() => {
    // 에러 처리 없음
    fetch(`/api/products/${productId}`)
      .then(res => res.json())
      .then(data => setProduct(data))
  }, [productId])

  return <div>{product?.name}</div>
}

// 힌트:
// 1. try-catch로 fetch 에러 처리
// 2. useAsyncError로 Error Boundary에 전파
// 3. 컴포넌트를 ErrorBoundary로 감싸기
```

---

## 다음 학습

- [Suspense & Lazy Loading](./suspense-lazy.md)
- [Custom Hooks](./custom-hooks.md)
- [API Integration](./api-integration.md)
