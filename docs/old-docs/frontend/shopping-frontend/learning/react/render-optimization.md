# Render 최적화

## 학습 목표
- React 렌더링 메커니즘 이해
- React.memo, useMemo, useCallback 활용법
- Shopping Frontend의 최적화 기법 분석

---

## 1. React 렌더링 기본

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        REACT RENDERING                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Trigger    ─────►  상태 변경, Props 변경                                   │
│   Render     ─────►  컴포넌트 함수 실행 (Virtual DOM 생성)                   │
│   Commit     ─────►  실제 DOM 업데이트                                        │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.1 렌더링이 발생하는 경우

```tsx
function Parent() {
  const [count, setCount] = useState(0)

  // count 변경 시 Parent + Child 모두 리렌더링
  return (
    <div>
      <button onClick={() => setCount(count + 1)}>
        Count: {count}
      </button>
      <Child /> {/* count를 사용하지 않아도 리렌더링! */}
    </div>
  )
}

function Child() {
  console.log('Child rendered')
  return <div>Child Component</div>
}
```

---

## 2. React.memo - 컴포넌트 메모이제이션

### 2.1 기본 사용법

```tsx
import { memo } from 'react'

// ❌ 부모가 리렌더링되면 항상 리렌더링
function Child({ name }: { name: string }) {
  console.log('Child rendered')
  return <div>{name}</div>
}

// ✅ props가 변경될 때만 리렌더링
const Child = memo(function Child({ name }: { name: string }) {
  console.log('Child rendered')
  return <div>{name}</div>
})
```

### 2.2 얕은 비교 (Shallow Comparison)

```tsx
const MemoizedChild = memo(Child)

function Parent() {
  const [count, setCount] = useState(0)

  // ✅ 원시 타입 props - 값이 같으면 리렌더링 안 됨
  return <MemoizedChild name="John" />

  // ❌ 객체 props - 매번 새 객체이므로 항상 리렌더링
  return <MemoizedChild user={{ name: 'John' }} />

  // ❌ 함수 props - 매번 새 함수이므로 항상 리렌더링
  return <MemoizedChild onClick={() => console.log('click')} />
}
```

### 2.3 해결: 객체/함수 메모이제이션

```tsx
function Parent() {
  const [count, setCount] = useState(0)

  // ✅ useMemo로 객체 메모이제이션
  const user = useMemo(() => ({ name: 'John' }), [])

  // ✅ useCallback으로 함수 메모이제이션
  const handleClick = useCallback(() => {
    console.log('click')
  }, [])

  return (
    <MemoizedChild
      user={user}
      onClick={handleClick}
    />
  )
}
```

### 2.4 Custom 비교 함수

```tsx
const ProductCard = memo(
  function ProductCard({ product }: Props) {
    return <div>{product.name}</div>
  },
  (prevProps, nextProps) => {
    // true를 반환하면 리렌더링 스킵
    return prevProps.product.id === nextProps.product.id &&
           prevProps.product.price === nextProps.product.price
  }
)
```

---

## 3. useMemo - 값 메모이제이션

### 3.1 비용이 큰 계산

```tsx
function ProductList({ products, filters }: Props) {
  // ❌ 매 렌더링마다 필터링/정렬 실행
  const filteredProducts = products
    .filter(p => p.category === filters.category)
    .filter(p => p.price >= filters.minPrice && p.price <= filters.maxPrice)
    .sort((a, b) => {
      if (filters.sortBy === 'price') return a.price - b.price
      return a.name.localeCompare(b.name)
    })

  // ✅ products나 filters가 변경될 때만 재계산
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

### 3.2 참조 동일성

```tsx
function Parent() {
  // ❌ 매 렌더링마다 새 배열 생성
  const emptyArray = []

  // ✅ 참조 유지
  const emptyArray = useMemo(() => [], [])

  return <Child items={emptyArray} />
}

const Child = memo(function Child({ items }: { items: any[] }) {
  useEffect(() => {
    console.log('items changed')
  }, [items]) // emptyArray가 매번 새 배열이면 계속 실행됨
  // ...
})
```

---

## 4. useCallback - 함수 메모이제이션

### 4.1 기본 사용법

```tsx
function Parent() {
  const [count, setCount] = useState(0)

  // ❌ 매 렌더링마다 새 함수 생성
  const handleClick = () => {
    console.log('clicked')
  }

  // ✅ 함수 참조 유지
  const handleClick = useCallback(() => {
    console.log('clicked')
  }, [])

  return <MemoizedChild onClick={handleClick} />
}
```

### 4.2 의존성 배열

```tsx
function SearchBar() {
  const [keyword, setKeyword] = useState('')

  // ✅ keyword가 변경될 때만 새 함수 생성
  const handleSearch = useCallback(async () => {
    if (!keyword) return
    const results = await searchProducts(keyword)
    console.log(results)
  }, [keyword])

  return (
    <div>
      <input value={keyword} onChange={e => setKeyword(e.target.value)} />
      <button onClick={handleSearch}>Search</button>
    </div>
  )
}
```

---

## 5. Portal Universe 최적화 사례

### 5.1 ProductCard 컴포넌트

```tsx
// components/ProductCard.tsx
import { memo } from 'react'

interface Props {
  product: Product
  inventory?: Inventory
}

// ✅ memo로 감싸서 product/inventory 변경 시에만 리렌더링
export const ProductCard = memo(function ProductCard({ product, inventory }: Props) {
  return (
    <Link to={`/products/${product.id}`}>
      <div className="card">
        <img src={product.imageUrl} alt={product.name} />
        <h3>{product.name}</h3>
        <p>{formatPrice(product.price)}</p>
        {inventory && (
          <span>Stock: {inventory.quantity}</span>
        )}
      </div>
    </Link>
  )
})
```

### 5.2 ProductListPage - fetchProducts 메모이제이션

```tsx
// pages/ProductListPage.tsx
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
}, [currentPage, searchKeyword, category]) // 이 값들이 변경될 때만 새 함수 생성

useEffect(() => {
  fetchProducts()
}, [fetchProducts])
```

### 5.3 CartItem 컴포넌트

```tsx
interface Props {
  item: CartItem
  onUpdateQuantity: (itemId: number, quantity: number) => void
  onRemove: (itemId: number) => void
}

// ✅ memo + useCallback 조합
export const CartItem = memo(function CartItem({ item, onUpdateQuantity, onRemove }: Props) {
  // 함수 메모이제이션으로 불필요한 리렌더링 방지
  const handleIncrease = useCallback(() => {
    onUpdateQuantity(item.id, item.quantity + 1)
  }, [item.id, item.quantity, onUpdateQuantity])

  const handleDecrease = useCallback(() => {
    if (item.quantity > 1) {
      onUpdateQuantity(item.id, item.quantity - 1)
    }
  }, [item.id, item.quantity, onUpdateQuantity])

  const handleRemove = useCallback(() => {
    onRemove(item.id)
  }, [item.id, onRemove])

  return (
    <div className="cart-item">
      <img src={item.product.imageUrl} alt={item.product.name} />
      <div>
        <h3>{item.product.name}</h3>
        <p>{formatPrice(item.price)}</p>
      </div>
      <div>
        <button onClick={handleDecrease}>-</button>
        <span>{item.quantity}</span>
        <button onClick={handleIncrease}>+</button>
      </div>
      <button onClick={handleRemove}>Remove</button>
    </div>
  )
})
```

---

## 6. 최적화 안티패턴

### 6.1 과도한 최적화

```tsx
// ❌ 간단한 컴포넌트에 memo 적용 (오히려 오버헤드)
const SimpleText = memo(function SimpleText({ text }: { text: string }) {
  return <span>{text}</span>
})

// ✅ 그냥 사용
function SimpleText({ text }: { text: string }) {
  return <span>{text}</span>
}
```

### 6.2 잘못된 의존성 배열

```tsx
// ❌ 의존성 누락 (린트 경고 무시)
const handleSubmit = useCallback(() => {
  submitForm(formData) // formData가 의존성에 없음
}, []) // eslint-disable-line

// ✅ 의존성 포함
const handleSubmit = useCallback(() => {
  submitForm(formData)
}, [formData])
```

### 6.3 매번 새 객체/배열 전달

```tsx
// ❌ memo 무용지물
const MemoizedChild = memo(Child)

function Parent() {
  return (
    <MemoizedChild
      config={{ theme: 'dark', locale: 'ko' }} // 매번 새 객체
      items={[]} // 매번 새 배열
      onClick={() => console.log('click')} // 매번 새 함수
    />
  )
}

// ✅ 메모이제이션
function Parent() {
  const config = useMemo(() => ({ theme: 'dark', locale: 'ko' }), [])
  const items = useMemo(() => [], [])
  const handleClick = useCallback(() => console.log('click'), [])

  return (
    <MemoizedChild
      config={config}
      items={items}
      onClick={handleClick}
    />
  )
}
```

---

## 7. 측정 기반 최적화

### 7.1 React DevTools Profiler

```tsx
// 1. React DevTools 설치
// 2. Profiler 탭 열기
// 3. Record 시작
// 4. 상호작용 수행
// 5. Record 정지
// 6. Flamegraph 분석
```

### 7.2 console.log로 측정

```tsx
function ProductList({ products }: Props) {
  console.log('ProductList rendered')

  return (
    <div>
      {products.map(p => (
        <ProductCard key={p.id} product={p} />
      ))}
    </div>
  )
}

const ProductCard = memo(function ProductCard({ product }: Props) {
  console.log(`ProductCard ${product.id} rendered`)
  return <div>{product.name}</div>
})
```

### 7.3 성능 측정 유틸리티

```tsx
function useRenderCount(componentName: string) {
  const renderCount = useRef(0)

  useEffect(() => {
    renderCount.current += 1
    console.log(`${componentName} render count: ${renderCount.current}`)
  })
}

// 사용
function MyComponent() {
  useRenderCount('MyComponent')
  // ...
}
```

---

## 8. 최적화 체크리스트

### 8.1 컴포넌트 최적화

- [ ] **React.memo**: 자주 리렌더링되는 자식 컴포넌트에 적용
- [ ] **key prop**: 리스트 렌더링 시 안정적인 key 사용
- [ ] **컴포넌트 분리**: 변경되는 부분과 변경되지 않는 부분 분리

### 8.2 Hook 최적화

- [ ] **useCallback**: 자식 props로 전달되는 함수에 적용
- [ ] **useMemo**: 비용이 큰 계산에만 적용
- [ ] **의존성 배열**: 린트 경고 무시하지 않기

### 8.3 상태 관리 최적화

- [ ] **상태 위치**: 필요한 곳에 가장 가까이 배치
- [ ] **선택적 구독**: Zustand의 selector 활용
- [ ] **상태 분리**: 자주 변경되는 상태와 그렇지 않은 상태 분리

---

## 9. 핵심 정리

| 기법 | 용도 | 주의사항 |
|------|------|----------|
| `React.memo` | 컴포넌트 메모이제이션 | props가 객체/함수면 메모이제이션 필요 |
| `useMemo` | 값 메모이제이션 | 비용 큰 계산에만 사용 |
| `useCallback` | 함수 메모이제이션 | 자식 props, 의존성에만 사용 |
| **측정 우선** | 프로파일링 후 최적화 | 추측 금지 |
| **의존성 배열** | 린트 규칙 준수 | 경고 무시 금지 |

---

## 10. 실습 과제

```tsx
// 다음 컴포넌트를 최적화하세요
function ProductList({ products, onAddToCart }: Props) {
  const [filter, setFilter] = useState('')

  const filtered = products.filter(p =>
    p.name.toLowerCase().includes(filter.toLowerCase())
  )

  return (
    <div>
      <input value={filter} onChange={e => setFilter(e.target.value)} />
      {filtered.map(product => (
        <div key={product.id}>
          <h3>{product.name}</h3>
          <button onClick={() => onAddToCart(product.id)}>
            Add to Cart
          </button>
        </div>
      ))}
    </div>
  )
}

// 힌트:
// 1. filtered 계산을 useMemo로 감싸기
// 2. ProductCard 컴포넌트로 분리하고 memo 적용
// 3. onAddToCart 함수를 useCallback으로 감싸기
```

---

## 다음 학습

- [Suspense & Lazy Loading](./suspense-lazy.md)
- [Error Boundary](./error-boundary.md)
- [Custom Hooks](./custom-hooks.md)
