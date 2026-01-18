# Shopping Frontend 아키텍처

## Dual Mode 지원

```
┌────────────────────────────────────────────────────────┐
│                  Shopping Frontend                      │
│  ┌─────────────────────┐  ┌────────────────────────┐  │
│  │    main.tsx         │  │    bootstrap.tsx        │  │
│  │  (Standalone Mode)  │  │  (Embedded Mode)        │  │
│  │                     │  │                         │  │
│  │  - Browser Router   │  │  - Memory Router        │  │
│  │  - localhost:30002  │  │  - /shopping/* 경로     │  │
│  └─────────┬───────────┘  └────────────┬───────────┘  │
│            │                           │               │
│            └───────────┬───────────────┘               │
│                        ▼                               │
│  ┌─────────────────────────────────────────────────┐  │
│  │                    App.tsx                       │  │
│  │  ┌────────────────────────────────────────────┐ │  │
│  │  │                <Routes>                     │ │  │
│  │  │  ProductList | Cart | Orders | Admin       │ │  │
│  │  └────────────────────────────────────────────┘ │  │
│  └─────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

## mountShoppingApp API

```tsx
// bootstrap.tsx
export function mountShoppingApp(
  el: HTMLElement,
  options: MountOptions = {}
): ShoppingAppInstance {

  const { initialPath = '/', onNavigate } = options

  // 1. React Root 생성
  const root = ReactDOM.createRoot(el)

  // 2. data-service 속성 설정
  document.documentElement.setAttribute('data-service', 'shopping')

  // 3. 렌더링
  root.render(
    <React.StrictMode>
      <App
        initialPath={initialPath}
        onNavigate={onNavigate}
      />
    </React.StrictMode>
  )

  // 4. 인스턴스 반환
  return {
    onParentNavigate: (path) => navigateTo(path),
    unmount: () => {
      root.unmount()
      cleanupCSS()
    }
  }
}
```

## 라우터 설정

```tsx
// router/index.tsx
export const router = createMemoryRouter([
  {
    path: '/',
    element: <ProductListPage />
  },
  {
    path: '/products/:id',
    element: <ProductDetailPage />
  },
  {
    path: '/cart',
    element: <RequireAuth><CartPage /></RequireAuth>
  },
  {
    path: '/checkout',
    element: <RequireAuth><CheckoutPage /></RequireAuth>
  },
  {
    path: '/orders',
    element: <RequireAuth><OrderListPage /></RequireAuth>
  },
  {
    path: '/orders/:orderNumber',
    element: <RequireAuth><OrderDetailPage /></RequireAuth>
  },
  {
    path: '/admin/products',
    element: <RequireRole role="ROLE_ADMIN"><AdminProductListPage /></RequireRole>
  },
  {
    path: '/admin/products/new',
    element: <RequireRole role="ROLE_ADMIN"><AdminProductFormPage /></RequireRole>
  },
  {
    path: '/admin/products/:id/edit',
    element: <RequireRole role="ROLE_ADMIN"><AdminProductFormPage /></RequireRole>
  },
  {
    path: '/forbidden',
    element: <ForbiddenPage />
  }
])
```

## 인증 Guard

### RequireAuth

```tsx
// components/guards/RequireAuth.tsx
export const RequireAuth: React.FC<{ children: ReactNode }> = ({ children }) => {
  const { isAuthenticated } = useAuthStore()

  if (!isAuthenticated) {
    // Portal Shell의 로그인 트리거
    window.dispatchEvent(new CustomEvent('portal:login'))
    return null
  }

  return <>{children}</>
}
```

### RequireRole

```tsx
// components/guards/RequireRole.tsx
export const RequireRole: React.FC<{
  role: string
  children: ReactNode
}> = ({ role, children }) => {
  const { user } = useAuthStore()
  const navigate = useNavigate()

  if (!user?.roles?.includes(role)) {
    return <Navigate to="/forbidden" replace />
  }

  return <>{children}</>
}
```

## 상태 관리

### Cart Store (Zustand)

```typescript
// stores/cartStore.ts
interface CartState {
  items: CartItem[]
  isLoading: boolean
  error: string | null
}

interface CartActions {
  fetchCart: () => Promise<void>
  addItem: (productId: number, quantity: number) => Promise<void>
  updateQuantity: (itemId: number, quantity: number) => Promise<void>
  removeItem: (itemId: number) => Promise<void>
  checkout: () => Promise<void>
}

export const useCartStore = create<CartState & CartActions>((set, get) => ({
  items: [],
  isLoading: false,
  error: null,

  fetchCart: async () => {
    set({ isLoading: true })
    const response = await cartApi.get()
    set({ items: response.data.data.items, isLoading: false })
  },

  addItem: async (productId, quantity) => {
    await cartApi.addItem({ productId, quantity })
    get().fetchCart()
  },

  updateQuantity: async (itemId, quantity) => {
    await cartApi.updateItem(itemId, { quantity })
    get().fetchCart()
  },

  removeItem: async (itemId) => {
    await cartApi.removeItem(itemId)
    get().fetchCart()
  },

  checkout: async () => {
    await cartApi.checkout()
    set({ items: [] })
  }
}))
```

### Auth Store

```typescript
// stores/authStore.ts
interface AuthState {
  user: User | null
  isAuthenticated: boolean
  isAdmin: boolean
}

export const useAuthStore = create<AuthState>(() => ({
  user: null,
  isAuthenticated: false,
  isAdmin: false
}))

// Portal Shell에서 인증 상태 수신
window.addEventListener('portal:auth-changed', (e: CustomEvent) => {
  useAuthStore.setState({
    user: e.detail.user,
    isAuthenticated: !!e.detail.user,
    isAdmin: e.detail.user?.roles?.includes('ROLE_ADMIN')
  })
})
```

## Admin 기능

### AdminProductListPage

```tsx
const AdminProductListPage: React.FC = () => {
  const { products, isLoading, deleteProduct } = useAdminProducts()

  return (
    <AdminLayout>
      <div className="admin-products">
        <header>
          <h1>상품 관리</h1>
          <Link to="/admin/products/new">
            <Button>상품 추가</Button>
          </Link>
        </header>

        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>상품명</th>
              <th>가격</th>
              <th>재고</th>
              <th>작업</th>
            </tr>
          </thead>
          <tbody>
            {products.map((product) => (
              <tr key={product.id}>
                <td>{product.id}</td>
                <td>{product.name}</td>
                <td>{product.price.toLocaleString()}원</td>
                <td>{product.stockQuantity}</td>
                <td>
                  <Link to={`/admin/products/${product.id}/edit`}>수정</Link>
                  <Button onClick={() => deleteProduct(product.id)}>삭제</Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </AdminLayout>
  )
}
```

## CSS 정리 (Unmount)

```typescript
unmount: () => {
  root.unmount()
  el.innerHTML = ''

  // Shopping CSS 제거
  document.querySelectorAll('style').forEach((style) => {
    if (style.textContent?.includes('[data-service="shopping"]')) {
      style.remove()
    }
  })

  // data-service 초기화
  document.documentElement.removeAttribute('data-service')
}
```

## 테마 적용

```css
/* data-service="shopping" 일 때 적용 */
[data-service="shopping"] {
  --semantic-brand-primary: #FF922B;
  --semantic-brand-primaryHover: #FD7E14;

  /* 쇼핑 최적화 */
  --shadow-card: 0 4px 12px rgba(255, 146, 43, 0.15);
}
```

## Portal Shell 통신

```typescript
// Portal Shell에서 인증 상태 수신
window.addEventListener('portal:auth-changed', (e: CustomEvent) => {
  useAuthStore.setState({
    user: e.detail.user,
    isAuthenticated: !!e.detail.user
  })
})

// Portal Shell에 네비게이션 요청
export function requestPortalNavigation(path: string) {
  window.dispatchEvent(new CustomEvent('shopping:navigate', {
    detail: { path }
  }))
}
```
