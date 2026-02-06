# Context API

## 학습 목표
- Context API의 개념과 사용법 이해
- Provider, useContext 패턴 학습
- Zustand vs Context API 비교

---

## 1. Context API 개요

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           CONTEXT API                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Props Drilling 해결  ─────►  중간 컴포넌트를 거치지 않고 데이터 전달       │
│   전역 상태 관리      ─────►  앱 전체에서 공유되는 상태                       │
│   테마, 인증 등       ─────►  자주 변경되지 않는 데이터에 적합                │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.1 Props Drilling 문제

```tsx
// ❌ Props Drilling (5단계 전달)
function App() {
  const [user, setUser] = useState(null)
  return <Layout user={user} />
}

function Layout({ user }) {
  return <Sidebar user={user} />
}

function Sidebar({ user }) {
  return <UserMenu user={user} />
}

function UserMenu({ user }) {
  return <UserAvatar user={user} />
}

function UserAvatar({ user }) {
  return <img src={user.avatar} />
}
```

---

## 2. Context 생성 및 사용

### 2.1 기본 패턴

```tsx
import { createContext, useContext, useState, ReactNode } from 'react'

// 1. Context 생성
interface ThemeContextValue {
  theme: 'light' | 'dark'
  toggleTheme: () => void
}

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined)

// 2. Provider 컴포넌트
export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState<'light' | 'dark'>('light')

  const toggleTheme = () => {
    setTheme(prev => prev === 'light' ? 'dark' : 'light')
  }

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  )
}

// 3. Custom Hook (타입 안전성)
export function useTheme() {
  const context = useContext(ThemeContext)
  if (context === undefined) {
    throw new Error('useTheme must be used within ThemeProvider')
  }
  return context
}
```

### 2.2 사용 예시

```tsx
// App.tsx
function App() {
  return (
    <ThemeProvider>
      <Layout />
    </ThemeProvider>
  )
}

// 어느 컴포넌트에서든 사용 가능
function ThemeToggle() {
  const { theme, toggleTheme } = useTheme()

  return (
    <button onClick={toggleTheme}>
      Current: {theme}
    </button>
  )
}

function ThemedButton() {
  const { theme } = useTheme()

  return (
    <button className={theme === 'dark' ? 'dark-btn' : 'light-btn'}>
      Click me
    </button>
  )
}
```

---

## 3. 복잡한 Context 패턴

### 3.1 AuthContext 예시

```tsx
import { createContext, useContext, useState, useEffect, ReactNode } from 'react'

interface User {
  id: string
  username: string
  email: string
  role: string
}

interface AuthContextValue {
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  // 초기 인증 상태 확인
  useEffect(() => {
    checkAuth()
  }, [])

  const checkAuth = async () => {
    try {
      const response = await fetch('/api/auth/me')
      if (response.ok) {
        const userData = await response.json()
        setUser(userData)
      }
    } catch (error) {
      console.error('Auth check failed', error)
    } finally {
      setIsLoading(false)
    }
  }

  const login = async (username: string, password: string) => {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    })

    if (!response.ok) {
      throw new Error('Login failed')
    }

    const userData = await response.json()
    setUser(userData)
  }

  const logout = async () => {
    await fetch('/api/auth/logout', { method: 'POST' })
    setUser(null)
  }

  const value = {
    user,
    isAuthenticated: user !== null,
    isLoading,
    login,
    logout
  }

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}
```

### 3.2 AuthContext 사용

```tsx
function LoginPage() {
  const { login, isLoading } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      await login(username, password)
      // 로그인 성공
    } catch (error) {
      alert('Login failed')
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <input value={username} onChange={e => setUsername(e.target.value)} />
      <input type="password" value={password} onChange={e => setPassword(e.target.value)} />
      <button disabled={isLoading}>Login</button>
    </form>
  )
}

function UserProfile() {
  const { user, logout } = useAuth()

  if (!user) return null

  return (
    <div>
      <p>Welcome, {user.username}!</p>
      <button onClick={logout}>Logout</button>
    </div>
  )
}
```

---

## 4. 여러 Context 조합

### 4.1 Provider 합성

```tsx
function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <CartProvider>
          <Router />
        </CartProvider>
      </AuthProvider>
    </ThemeProvider>
  )
}
```

### 4.2 ComposeProviders 유틸리티

```tsx
interface ProviderProps {
  children: React.ReactNode
}

function ComposeProviders({
  providers,
  children
}: {
  providers: React.FC<ProviderProps>[]
  children: React.ReactNode
}) {
  return providers.reduceRight(
    (acc, Provider) => <Provider>{acc}</Provider>,
    children
  )
}

// 사용
function App() {
  return (
    <ComposeProviders providers={[ThemeProvider, AuthProvider, CartProvider]}>
      <Router />
    </ComposeProviders>
  )
}
```

---

## 5. Context 성능 최적화

### 5.1 문제: 불필요한 리렌더링

```tsx
// ❌ value 객체가 매 렌더링마다 재생성
function ThemeProvider({ children }) {
  const [theme, setTheme] = useState('light')

  return (
    <ThemeContext.Provider value={{ theme, setTheme }}>
      {children}
    </ThemeContext.Provider>
  )
}
```

### 5.2 해결: useMemo로 메모이제이션

```tsx
// ✅ value를 메모이제이션
function ThemeProvider({ children }) {
  const [theme, setTheme] = useState('light')

  const value = useMemo(() => ({
    theme,
    setTheme
  }), [theme])

  return (
    <ThemeContext.Provider value={value}>
      {children}
    </ThemeContext.Provider>
  )
}
```

### 5.3 Context 분리

```tsx
// ❌ 하나의 큰 Context
const AppContext = createContext({ user, cart, theme, ... })

// ✅ 여러 개의 작은 Context
const UserContext = createContext(user)
const CartContext = createContext(cart)
const ThemeContext = createContext(theme)

// 필요한 Context만 구독
function UserProfile() {
  const user = useContext(UserContext) // user 변경 시에만 리렌더링
  // ...
}
```

---

## 6. Portal Universe와 Context

### 6.1 Shopping Frontend는 Zustand 사용

Shopping Frontend는 Context API 대신 **Zustand**를 사용합니다:

```tsx
// stores/cartStore.ts
export const useCartStore = create<CartState>()(
  devtools(
    persist(
      (set) => ({
        cart: null,
        fetchCart: async () => { /* ... */ }
      }),
      { name: 'shopping-cart-storage' }
    ),
    { name: 'CartStore' }
  )
)

// 컴포넌트에서 사용
function CartPage() {
  const { cart, fetchCart } = useCartStore()
  // ...
}
```

### 6.2 왜 Zustand를 선택했을까?

| 특성 | Context API | Zustand |
|------|-------------|---------|
| **보일러플레이트** | Provider, Context 생성 필요 | 최소한의 코드 |
| **성능** | 수동 최적화 필요 | 자동 선택적 구독 |
| **DevTools** | 별도 설정 | 내장 미들웨어 |
| **Persistence** | 직접 구현 | persist 미들웨어 |
| **번들 사이즈** | 0 (내장) | ~1KB |

---

## 7. Context API vs Zustand 비교

### 7.1 동일한 기능 구현

**Context API**
```tsx
// 1. Context 생성
const CartContext = createContext<CartContextValue | undefined>(undefined)

// 2. Provider 구현
export function CartProvider({ children }) {
  const [cart, setCart] = useState(null)
  const [loading, setLoading] = useState(false)

  const fetchCart = useCallback(async () => {
    setLoading(true)
    try {
      const response = await cartApi.getCart()
      setCart(response.data)
    } finally {
      setLoading(false)
    }
  }, [])

  const value = useMemo(() => ({
    cart,
    loading,
    fetchCart
  }), [cart, loading, fetchCart])

  return (
    <CartContext.Provider value={value}>
      {children}
    </CartContext.Provider>
  )
}

// 3. Custom Hook
export function useCart() {
  const context = useContext(CartContext)
  if (!context) throw new Error('useCart must be used within CartProvider')
  return context
}

// 4. App에서 Provider 설정
function App() {
  return (
    <CartProvider>
      <Router />
    </CartProvider>
  )
}
```

**Zustand**
```tsx
// 1. Store 생성 (한 번에!)
export const useCartStore = create<CartState>((set) => ({
  cart: null,
  loading: false,
  fetchCart: async () => {
    set({ loading: true })
    try {
      const response = await cartApi.getCart()
      set({ cart: response.data })
    } finally {
      set({ loading: false })
    }
  }
}))

// 2. 어디서든 바로 사용 (Provider 불필요)
function CartPage() {
  const { cart, loading, fetchCart } = useCartStore()
  // ...
}
```

---

## 8. 언제 Context API를 사용할까?

### 8.1 적합한 경우

- ✅ 테마, 로케일 등 자주 변경되지 않는 데이터
- ✅ 특정 서브트리에서만 사용되는 데이터
- ✅ 외부 라이브러리 의존성을 피하고 싶을 때
- ✅ SSR 환경에서 인스턴스 격리가 필요할 때

### 8.2 부적합한 경우

- ❌ 자주 변경되는 상태 (성능 이슈)
- ❌ 복잡한 상태 로직 (보일러플레이트 증가)
- ❌ DevTools, Persistence 등 추가 기능 필요

---

## 9. 핵심 정리

| 개념 | 설명 |
|------|------|
| `createContext` | Context 생성 |
| `<Context.Provider>` | 값 제공 |
| `useContext` | 값 구독 |
| `useMemo` | Provider value 최적화 |
| **Context 분리** | 불필요한 리렌더링 방지 |
| **Custom Hook** | 타입 안전성 & 사용 편의성 |

---

## 다음 학습

- [Zustand 상태 관리](./zustand-state.md)
- [Render 최적화](./render-optimization.md)
- [Custom Hooks](./custom-hooks.md)
