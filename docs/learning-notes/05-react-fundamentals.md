# React 기초부터 Shopping Frontend 구현까지

## 목차
1. [React 기본 개념](#1-react-기본-개념)
2. [React Hooks](#2-react-hooks)
3. [TypeScript와 React](#3-typescript와-react)
4. [React Router](#4-react-router)
5. [상태 관리 (Zustand)](#5-상태-관리-zustand)
6. [API 통신 (Axios)](#6-api-통신-axios)
7. [Module Federation (Micro Frontend)](#7-module-federation-micro-frontend)
8. [Shopping Frontend 구현 분석](#8-shopping-frontend-구현-분석)

---

## 1. React 기본 개념

### 1.1 React란?

React는 Facebook에서 개발한 **UI 라이브러리**입니다. 핵심 철학은:

- **선언적(Declarative)**: "어떻게" 보다 "무엇을" 렌더링할지 기술
- **컴포넌트 기반(Component-Based)**: UI를 독립적인 조각으로 분리
- **단방향 데이터 흐름**: 부모 → 자식으로 Props 전달

### 1.2 JSX (JavaScript XML)

JSX는 JavaScript 안에서 HTML과 유사한 문법을 사용할 수 있게 해줍니다.

```tsx
// JSX 예시
const element = <h1>Hello, {name}</h1>;

// 컴파일 결과 (React.createElement)
const element = React.createElement('h1', null, `Hello, ${name}`);
```

**JSX 규칙:**
```tsx
// 1. 반드시 하나의 루트 요소
return (
  <div>
    <h1>Title</h1>
    <p>Content</p>
  </div>
);

// 또는 Fragment 사용
return (
  <>
    <h1>Title</h1>
    <p>Content</p>
  </>
);

// 2. JavaScript 표현식은 {} 안에
const price = 10000;
return <p>Price: {price.toLocaleString()}원</p>;

// 3. class 대신 className
return <div className="container">...</div>;

// 4. style은 객체로
return <div style={{ color: 'red', fontSize: '16px' }}>...</div>;

// 5. 이벤트는 camelCase
return <button onClick={handleClick}>Click</button>;
```

### 1.3 컴포넌트 (Component)

컴포넌트는 **재사용 가능한 UI 조각**입니다.

```tsx
// 함수형 컴포넌트 (권장)
function Greeting({ name }: { name: string }) {
  return <h1>Hello, {name}!</h1>;
}

// 화살표 함수 스타일
const Greeting: React.FC<{ name: string }> = ({ name }) => {
  return <h1>Hello, {name}!</h1>;
};

// 사용
<Greeting name="World" />
```

### 1.4 Props (Properties)

Props는 **부모 → 자식**으로 데이터를 전달하는 방법입니다.

```tsx
// 부모 컴포넌트
function Parent() {
  return <Child message="Hello" count={42} />;
}

// 자식 컴포넌트
interface ChildProps {
  message: string;
  count: number;
  optional?: boolean;  // 선택적 prop
}

function Child({ message, count, optional = false }: ChildProps) {
  return (
    <div>
      <p>{message}</p>
      <p>Count: {count}</p>
    </div>
  );
}
```

**children Props:**
```tsx
interface CardProps {
  title: string;
  children: React.ReactNode;  // 자식 요소
}

function Card({ title, children }: CardProps) {
  return (
    <div className="card">
      <h2>{title}</h2>
      <div className="card-body">{children}</div>
    </div>
  );
}

// 사용
<Card title="My Card">
  <p>This is the card content</p>
  <button>Click me</button>
</Card>
```

### 1.5 조건부 렌더링

```tsx
function UserStatus({ isLoggedIn }: { isLoggedIn: boolean }) {
  // 방법 1: if-else
  if (isLoggedIn) {
    return <p>Welcome back!</p>;
  }
  return <p>Please log in</p>;
}

function UserStatus2({ isLoggedIn }: { isLoggedIn: boolean }) {
  return (
    <div>
      {/* 방법 2: 삼항 연산자 */}
      {isLoggedIn ? <p>Welcome back!</p> : <p>Please log in</p>}

      {/* 방법 3: && 연산자 (조건이 true일 때만 렌더링) */}
      {isLoggedIn && <button>Logout</button>}
    </div>
  );
}
```

### 1.6 리스트 렌더링

```tsx
interface Product {
  id: number;
  name: string;
  price: number;
}

function ProductList({ products }: { products: Product[] }) {
  return (
    <ul>
      {products.map((product) => (
        // key는 필수! React가 변경사항을 효율적으로 감지
        <li key={product.id}>
          {product.name} - {product.price}원
        </li>
      ))}
    </ul>
  );
}
```

**key 규칙:**
- 고유하고 안정적인 값 사용 (보통 id)
- 배열 index는 피하기 (순서 변경 시 문제 발생)
- 형제 요소 사이에서만 고유하면 됨

---

## 2. React Hooks

Hooks는 함수형 컴포넌트에서 **상태와 생명주기**를 다루는 방법입니다.

### 2.1 useState - 상태 관리

```tsx
import { useState } from 'react';

function Counter() {
  // [현재값, 설정함수] = useState(초기값)
  const [count, setCount] = useState(0);

  return (
    <div>
      <p>Count: {count}</p>
      <button onClick={() => setCount(count + 1)}>+1</button>
      <button onClick={() => setCount(prev => prev - 1)}>-1</button>
    </div>
  );
}
```

**복잡한 상태:**
```tsx
interface FormData {
  name: string;
  email: string;
}

function Form() {
  const [formData, setFormData] = useState<FormData>({
    name: '',
    email: ''
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    // 이전 상태를 유지하면서 특정 필드만 업데이트
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  return (
    <form>
      <input name="name" value={formData.name} onChange={handleChange} />
      <input name="email" value={formData.email} onChange={handleChange} />
    </form>
  );
}
```

### 2.2 useEffect - 사이드 이펙트 처리

```tsx
import { useState, useEffect } from 'react';

function UserProfile({ userId }: { userId: string }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // 이 함수는 컴포넌트 마운트 후 실행
    const fetchUser = async () => {
      setLoading(true);
      const response = await fetch(`/api/users/${userId}`);
      const data = await response.json();
      setUser(data);
      setLoading(false);
    };

    fetchUser();

    // Cleanup 함수 (컴포넌트 언마운트 또는 의존성 변경 전 실행)
    return () => {
      console.log('Cleanup: userId changed or component unmounted');
    };
  }, [userId]); // 의존성 배열: userId가 변경될 때마다 실행

  if (loading) return <p>Loading...</p>;
  if (!user) return <p>User not found</p>;

  return <div>{user.name}</div>;
}
```

**의존성 배열 패턴:**
```tsx
// 1. 빈 배열: 마운트 시 1번만 실행
useEffect(() => {
  console.log('Mounted');
  return () => console.log('Unmounted');
}, []);

// 2. 의존성 있음: 의존성 변경 시마다 실행
useEffect(() => {
  console.log('userId changed:', userId);
}, [userId]);

// 3. 의존성 배열 없음: 매 렌더링마다 실행 (주의!)
useEffect(() => {
  console.log('Every render');
});
```

### 2.3 useRef - DOM 참조 및 값 유지

```tsx
import { useRef, useEffect } from 'react';

function TextInput() {
  // DOM 요소 참조
  const inputRef = useRef<HTMLInputElement>(null);

  // 렌더링 사이에 값 유지 (변경해도 리렌더링 안 됨)
  const renderCount = useRef(0);

  useEffect(() => {
    // 마운트 시 input에 포커스
    inputRef.current?.focus();
    renderCount.current += 1;
  });

  return (
    <div>
      <input ref={inputRef} />
      <p>Render count: {renderCount.current}</p>
    </div>
  );
}
```

### 2.4 useMemo와 useCallback - 성능 최적화

```tsx
import { useMemo, useCallback, useState } from 'react';

function ExpensiveComponent({ items, filter }: Props) {
  // useMemo: 계산 결과를 캐싱
  const filteredItems = useMemo(() => {
    console.log('Filtering items...');
    return items.filter(item => item.includes(filter));
  }, [items, filter]); // items나 filter가 변경될 때만 재계산

  // useCallback: 함수를 캐싱
  const handleClick = useCallback((id: number) => {
    console.log('Clicked:', id);
  }, []); // 의존성이 없으므로 항상 같은 함수 참조

  return (
    <ul>
      {filteredItems.map(item => (
        <li key={item} onClick={() => handleClick(item.id)}>
          {item.name}
        </li>
      ))}
    </ul>
  );
}
```

### 2.5 커스텀 Hook

반복되는 로직을 재사용 가능한 Hook으로 추출합니다.

```tsx
// hooks/useLocalStorage.ts
function useLocalStorage<T>(key: string, initialValue: T) {
  const [storedValue, setStoredValue] = useState<T>(() => {
    try {
      const item = window.localStorage.getItem(key);
      return item ? JSON.parse(item) : initialValue;
    } catch {
      return initialValue;
    }
  });

  const setValue = (value: T | ((val: T) => T)) => {
    const valueToStore = value instanceof Function ? value(storedValue) : value;
    setStoredValue(valueToStore);
    window.localStorage.setItem(key, JSON.stringify(valueToStore));
  };

  return [storedValue, setValue] as const;
}

// 사용
function App() {
  const [theme, setTheme] = useLocalStorage('theme', 'light');

  return (
    <button onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')}>
      Current: {theme}
    </button>
  );
}
```

---

## 3. TypeScript와 React

### 3.1 Props 타입 정의

```tsx
// 기본 Props
interface ButtonProps {
  label: string;
  onClick: () => void;
  disabled?: boolean;
  variant?: 'primary' | 'secondary';  // Union 타입
}

function Button({
  label,
  onClick,
  disabled = false,  // 기본값
  variant = 'primary'
}: ButtonProps) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={`btn-${variant}`}
    >
      {label}
    </button>
  );
}
```

### 3.2 이벤트 타입

```tsx
function Form() {
  // 클릭 이벤트
  const handleClick = (e: React.MouseEvent<HTMLButtonElement>) => {
    console.log('Clicked');
  };

  // 폼 제출
  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    console.log('Submitted');
  };

  // Input 변경
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    console.log(e.target.value);
  };

  // 키보드 이벤트
  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      console.log('Enter pressed');
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <input
        onChange={handleChange}
        onKeyDown={handleKeyDown}
      />
      <button onClick={handleClick}>Submit</button>
    </form>
  );
}
```

### 3.3 제네릭 컴포넌트

```tsx
interface ListProps<T> {
  items: T[];
  renderItem: (item: T) => React.ReactNode;
  keyExtractor: (item: T) => string | number;
}

function List<T>({ items, renderItem, keyExtractor }: ListProps<T>) {
  return (
    <ul>
      {items.map(item => (
        <li key={keyExtractor(item)}>
          {renderItem(item)}
        </li>
      ))}
    </ul>
  );
}

// 사용
interface User {
  id: number;
  name: string;
}

<List<User>
  items={users}
  keyExtractor={user => user.id}
  renderItem={user => <span>{user.name}</span>}
/>
```

---

## 4. React Router

### 4.1 기본 설정

```tsx
// router/index.tsx
import { createBrowserRouter, RouterProvider, Outlet } from 'react-router-dom';

// 라우트 정의
const routes = [
  {
    path: '/',
    element: <Layout />,  // 공통 레이아웃
    children: [
      { index: true, element: <HomePage /> },         // /
      { path: 'products', element: <ProductList /> }, // /products
      { path: 'products/:id', element: <ProductDetail /> }, // /products/123
      { path: 'cart', element: <CartPage /> },
      { path: '*', element: <NotFound /> }  // 404
    ]
  }
];

// 라우터 생성
const router = createBrowserRouter(routes);

// App에서 사용
function App() {
  return <RouterProvider router={router} />;
}

// Layout 컴포넌트
function Layout() {
  return (
    <div>
      <Header />
      <main>
        <Outlet />  {/* 자식 라우트가 여기에 렌더링 */}
      </main>
      <Footer />
    </div>
  );
}
```

### 4.2 네비게이션

```tsx
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';

function Navigation() {
  const navigate = useNavigate();

  return (
    <nav>
      {/* 선언적 네비게이션 */}
      <Link to="/">Home</Link>
      <Link to="/products">Products</Link>
      <Link to="/products/123">Product 123</Link>

      {/* 프로그래매틱 네비게이션 */}
      <button onClick={() => navigate('/cart')}>Go to Cart</button>
      <button onClick={() => navigate(-1)}>Back</button>
    </nav>
  );
}

function ProductDetail() {
  // URL 파라미터: /products/:id
  const { id } = useParams<{ id: string }>();

  // Query String: /products?category=shoes&sort=price
  const [searchParams, setSearchParams] = useSearchParams();
  const category = searchParams.get('category');

  return (
    <div>
      <p>Product ID: {id}</p>
      <p>Category: {category}</p>
    </div>
  );
}
```

### 4.3 Memory Router (임베디드 환경)

```tsx
import { createMemoryRouter, createBrowserRouter } from 'react-router-dom';

// 환경에 따라 다른 라우터 사용
function createRouter(options: { isEmbedded: boolean; initialPath: string }) {
  const { isEmbedded, initialPath } = options;

  if (isEmbedded) {
    // Module Federation 환경: 메모리 라우터
    // URL을 직접 변경하지 않음 (Host가 관리)
    return createMemoryRouter(routes, {
      initialEntries: [initialPath],
      initialIndex: 0
    });
  }

  // 독립 실행: 브라우저 라우터
  return createBrowserRouter(routes);
}
```

---

## 5. 상태 관리 (Zustand)

### 5.1 Zustand 소개

Zustand는 **간단하고 가벼운** 상태 관리 라이브러리입니다.

**Redux vs Zustand:**
| 특징 | Redux | Zustand |
|------|-------|---------|
| 보일러플레이트 | 많음 (action, reducer) | 적음 |
| 러닝 커브 | 높음 | 낮음 |
| 번들 크기 | ~7KB | ~1KB |
| 미들웨어 | redux-thunk, saga | 내장 |

### 5.2 기본 사용법

```tsx
// stores/cartStore.ts
import { create } from 'zustand';

interface CartItem {
  productId: number;
  name: string;
  price: number;
  quantity: number;
}

interface CartState {
  // 상태
  items: CartItem[];

  // 액션
  addItem: (item: CartItem) => void;
  removeItem: (productId: number) => void;
  updateQuantity: (productId: number, quantity: number) => void;
  clearCart: () => void;

  // 계산된 값 (getter)
  getTotalPrice: () => number;
  getTotalItems: () => number;
}

export const useCartStore = create<CartState>((set, get) => ({
  items: [],

  addItem: (item) => set((state) => {
    const existing = state.items.find(i => i.productId === item.productId);
    if (existing) {
      return {
        items: state.items.map(i =>
          i.productId === item.productId
            ? { ...i, quantity: i.quantity + item.quantity }
            : i
        )
      };
    }
    return { items: [...state.items, item] };
  }),

  removeItem: (productId) => set((state) => ({
    items: state.items.filter(i => i.productId !== productId)
  })),

  updateQuantity: (productId, quantity) => set((state) => ({
    items: state.items.map(i =>
      i.productId === productId ? { ...i, quantity } : i
    )
  })),

  clearCart: () => set({ items: [] }),

  getTotalPrice: () => {
    return get().items.reduce((sum, item) => sum + item.price * item.quantity, 0);
  },

  getTotalItems: () => {
    return get().items.reduce((sum, item) => sum + item.quantity, 0);
  }
}));
```

### 5.3 컴포넌트에서 사용

```tsx
function CartPage() {
  // 필요한 상태만 선택 (성능 최적화)
  const items = useCartStore((state) => state.items);
  const removeItem = useCartStore((state) => state.removeItem);
  const getTotalPrice = useCartStore((state) => state.getTotalPrice);

  return (
    <div>
      {items.map(item => (
        <div key={item.productId}>
          <span>{item.name}</span>
          <span>{item.price} x {item.quantity}</span>
          <button onClick={() => removeItem(item.productId)}>Remove</button>
        </div>
      ))}
      <p>Total: {getTotalPrice().toLocaleString()}원</p>
    </div>
  );
}
```

### 5.4 미들웨어 (persist, devtools)

```tsx
import { create } from 'zustand';
import { persist, devtools } from 'zustand/middleware';

export const useCartStore = create<CartState>()(
  devtools(  // Redux DevTools 연동
    persist(  // LocalStorage 자동 저장
      (set, get) => ({
        items: [],
        // ... 액션들
      }),
      {
        name: 'cart-storage',  // localStorage key
        partialize: (state) => ({ items: state.items }),  // 저장할 필드 선택
      }
    ),
    { name: 'CartStore' }  // DevTools에서 보이는 이름
  )
);
```

---

## 6. API 통신 (Axios)

### 6.1 Axios 클라이언트 설정

```tsx
// api/client.ts
import axios, { AxiosInstance, AxiosError } from 'axios';

const createApiClient = (): AxiosInstance => {
  const client = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
    timeout: 30000,
    headers: {
      'Content-Type': 'application/json'
    },
    withCredentials: true  // 쿠키 포함
  });

  // Request Interceptor: 토큰 주입
  client.interceptors.request.use(
    (config) => {
      const token = localStorage.getItem('accessToken');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    },
    (error) => Promise.reject(error)
  );

  // Response Interceptor: 에러 처리
  client.interceptors.response.use(
    (response) => response,
    (error: AxiosError) => {
      if (error.response?.status === 401) {
        // 토큰 만료 처리
        localStorage.removeItem('accessToken');
        window.location.href = '/login';
      }
      return Promise.reject(error);
    }
  );

  return client;
};

export const apiClient = createApiClient();
```

### 6.2 API 엔드포인트 정의

```tsx
// api/endpoints.ts
import { apiClient } from './client';
import type { Product, ApiResponse, PaginatedResponse } from '@/types';

export const productApi = {
  // 상품 목록 조회
  getProducts: async (params?: { page?: number; size?: number; keyword?: string }) => {
    const response = await apiClient.get<ApiResponse<PaginatedResponse<Product>>>(
      '/api/v1/shopping/products',
      { params }
    );
    return response.data;
  },

  // 상품 상세 조회
  getProduct: async (id: number) => {
    const response = await apiClient.get<ApiResponse<Product>>(
      `/api/v1/shopping/products/${id}`
    );
    return response.data;
  }
};
```

### 6.3 컴포넌트에서 API 호출

```tsx
function ProductList() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchProducts = async () => {
      try {
        setLoading(true);
        setError(null);
        const response = await productApi.getProducts({ page: 0, size: 20 });
        if (response.success) {
          setProducts(response.data.content);
        } else {
          setError(response.message);
        }
      } catch (err: any) {
        setError(err.message || 'Failed to fetch products');
      } finally {
        setLoading(false);
      }
    };

    fetchProducts();
  }, []);

  if (loading) return <Spinner />;
  if (error) return <ErrorMessage message={error} />;

  return (
    <div className="grid grid-cols-4 gap-4">
      {products.map(product => (
        <ProductCard key={product.id} product={product} />
      ))}
    </div>
  );
}
```

---

## 7. Module Federation (Micro Frontend)

### 7.1 Module Federation이란?

Module Federation은 **런타임에 원격 모듈을 로드**하는 Webpack/Vite 기능입니다.

**핵심 개념:**
- **Host (Shell)**: 원격 모듈을 소비하는 애플리케이션
- **Remote**: 원격으로 노출되는 모듈
- **Shared**: 공유되는 라이브러리 (react, react-dom)

```
┌─────────────────────────────────────────────────────────┐
│                    Portal Shell (Host)                   │
│                     http://localhost:30000              │
│  ┌─────────────────────────────────────────────────┐   │
│  │  remotes: {                                      │   │
│  │    blog: 'http://localhost:30001/remoteEntry.js' │   │
│  │    shopping: 'http://localhost:30002/remoteEntry.js' │
│  │  }                                               │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
              │                    │
              ▼                    ▼
┌─────────────────────┐  ┌─────────────────────┐
│   Blog Frontend     │  │  Shopping Frontend  │
│   (Remote - Vue)    │  │   (Remote - React)  │
│  exposes:           │  │  exposes:           │
│   './bootstrap'     │  │   './bootstrap'     │
└─────────────────────┘  └─────────────────────┘
```

### 7.2 Remote 설정 (Shopping Frontend)

```ts
// vite.config.ts
import federation from '@originjs/vite-plugin-federation';

export default defineConfig({
  plugins: [
    react(),
    federation({
      name: 'shopping-frontend',
      filename: 'remoteEntry.js',  // 생성될 진입점 파일

      // 외부로 노출할 모듈
      exposes: {
        './bootstrap': './src/bootstrap.tsx'
      },

      // 호스트와 공유할 라이브러리
      shared: ['react', 'react-dom']
    })
  ]
});
```

### 7.3 Host 설정 (Portal Shell)

```ts
// vite.config.ts (Portal Shell)
federation({
  name: 'portal',

  // 원격 모듈 등록
  remotes: {
    blog: 'http://localhost:30001/assets/remoteEntry.js',
    shopping: 'http://localhost:30002/assets/remoteEntry.js'
  },

  // 호스트가 노출하는 모듈 (원격에서 사용 가능)
  exposes: {
    './themeStore': './src/store/theme.ts',
    './authStore': './src/store/auth.ts'
  },

  shared: ['vue', 'pinia', 'axios']
})
```

### 7.4 Bootstrap 함수 패턴

Remote 앱이 Host에 마운트되기 위한 표준 인터페이스:

```tsx
// bootstrap.tsx
export type MountOptions = {
  initialPath?: string;
  onNavigate?: (path: string) => void;
}

export type AppInstance = {
  onParentNavigate: (path: string) => void;
  unmount: () => void;
}

export function mountShoppingApp(
  el: HTMLElement,
  options: MountOptions = {}
): AppInstance {
  const { initialPath = '/', onNavigate } = options;

  // React 앱 마운트
  const root = ReactDOM.createRoot(el);
  root.render(<App initialPath={initialPath} onNavigate={onNavigate} />);

  return {
    // Host → Remote 네비게이션
    onParentNavigate: (path: string) => {
      navigateTo(path);  // 내부 라우터 네비게이션
    },

    // 정리 함수
    unmount: () => {
      root.unmount();
      el.innerHTML = '';
    }
  };
}
```

### 7.5 Host에서 Remote 로드

```ts
// portal-shell/src/services/remoteLoader.ts
export async function loadRemote(config: RemoteConfig) {
  // 1. remoteEntry.js 로드
  const remoteEntry = await import(config.url);

  // 2. 모듈 가져오기
  const moduleFactory = await remoteEntry.get('./bootstrap');
  const module = await moduleFactory();

  // 3. mount 함수 반환
  return module.mountShoppingApp;
}
```

```vue
<!-- RemoteWrapper.vue -->
<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { remoteLoader } from '@/services/remoteLoader';

const props = defineProps<{ config: RemoteConfig }>();
const container = ref<HTMLElement | null>(null);
let remoteApp = null;

onMounted(async () => {
  const mountFn = await remoteLoader.loadRemote(props.config);

  remoteApp = mountFn(container.value, {
    initialPath: '/',
    onNavigate: (path) => {
      // Remote의 네비게이션을 Host URL에 반영
      router.push(`${props.config.basePath}${path}`);
    }
  });
});

onUnmounted(() => {
  remoteApp?.unmount();
});
</script>

<template>
  <div ref="container"></div>
</template>
```

---

## 8. Shopping Frontend 구현 분석

### 8.1 프로젝트 구조

```
shopping-frontend/
├── src/
│   ├── api/
│   │   ├── client.ts          # Axios 인스턴스
│   │   └── endpoints.ts       # API 함수들
│   ├── components/
│   │   ├── ProductCard.tsx    # 상품 카드
│   │   └── CartItem.tsx       # 장바구니 아이템
│   ├── pages/
│   │   ├── ProductListPage.tsx
│   │   ├── ProductDetailPage.tsx
│   │   ├── CartPage.tsx
│   │   ├── CheckoutPage.tsx
│   │   ├── OrderListPage.tsx
│   │   └── OrderDetailPage.tsx
│   ├── router/
│   │   └── index.tsx          # React Router 설정
│   ├── stores/
│   │   ├── cartStore.ts       # Zustand 장바구니
│   │   └── authStore.ts       # 인증 상태 (Host 연동)
│   ├── styles/
│   │   └── index.scss         # Tailwind + Design System
│   ├── types/
│   │   └── index.ts           # TypeScript 타입
│   ├── App.tsx                # 루트 컴포넌트
│   ├── bootstrap.tsx          # Module Federation 진입점
│   └── main.tsx               # Standalone 진입점
├── vite.config.ts
├── tailwind.config.js
└── tsconfig.json
```

### 8.2 데이터 흐름

```
┌─────────────────────────────────────────────────────────┐
│                      App.tsx                             │
│  - Theme 동기화 (Portal Shell 연동)                      │
│  - data-service="shopping" 설정                          │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                   ShoppingRouter                         │
│  - MemoryRouter (Embedded) / BrowserRouter (Standalone) │
│  - onNavigate 콜백으로 Host에 경로 알림                   │
└─────────────────────────────────────────────────────────┘
                          │
          ┌───────────────┼───────────────┐
          ▼               ▼               ▼
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│ ProductList │  │   CartPage  │  │ OrderList   │
│    Page     │  │             │  │    Page     │
└─────────────┘  └─────────────┘  └─────────────┘
          │               │               │
          ▼               ▼               ▼
┌─────────────────────────────────────────────────────────┐
│                     API Layer                            │
│  productApi.getProducts()                                │
│  cartApi.getCart()                                       │
│  orderApi.getOrders()                                    │
└─────────────────────────────────────────────────────────┘
          │               │               │
          ▼               ▼               ▼
┌─────────────────────────────────────────────────────────┐
│                  Backend Services                        │
│  shopping-service (MySQL)                                │
└─────────────────────────────────────────────────────────┘
```

### 8.3 핵심 컴포넌트 패턴

#### ProductListPage - 목록 조회 패턴

```tsx
function ProductListPage() {
  // 상태
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [keyword, setKeyword] = useState('');

  // 데이터 로딩
  useEffect(() => {
    const fetchProducts = async () => {
      try {
        setLoading(true);
        const response = await productApi.getProducts({ page, size: 12, keyword });
        if (response.success) {
          setProducts(response.data.content);
          setTotalPages(response.data.totalPages);
        }
      } catch (err) {
        setError('Failed to load products');
      } finally {
        setLoading(false);
      }
    };
    fetchProducts();
  }, [page, keyword]);  // page나 keyword 변경 시 재조회

  // 검색 핸들러
  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);  // 검색 시 첫 페이지로
  };

  // 렌더링
  if (loading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message={error} />;

  return (
    <div>
      <SearchForm onSubmit={handleSearch} value={keyword} onChange={setKeyword} />
      <ProductGrid products={products} />
      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
    </div>
  );
}
```

#### CartPage - Zustand 연동 패턴

```tsx
function CartPage() {
  // Zustand store에서 상태와 액션 가져오기
  const items = useCartStore(state => state.items);
  const updateQuantity = useCartStore(state => state.updateQuantity);
  const removeItem = useCartStore(state => state.removeItem);
  const getTotalPrice = useCartStore(state => state.getTotalPrice);

  // 체크아웃으로 이동
  const navigate = useNavigate();
  const handleCheckout = () => {
    if (items.length === 0) {
      alert('Cart is empty');
      return;
    }
    navigate('/checkout');
  };

  return (
    <div className="grid grid-cols-3 gap-6">
      {/* 장바구니 아이템 목록 */}
      <div className="col-span-2">
        {items.map(item => (
          <CartItem
            key={item.productId}
            item={item}
            onQuantityChange={(qty) => updateQuantity(item.productId, qty)}
            onRemove={() => removeItem(item.productId)}
          />
        ))}
      </div>

      {/* 주문 요약 */}
      <div className="col-span-1">
        <OrderSummary
          totalPrice={getTotalPrice()}
          itemCount={items.length}
          onCheckout={handleCheckout}
        />
      </div>
    </div>
  );
}
```

#### CheckoutPage - 멀티 스텝 폼 패턴

```tsx
type CheckoutStep = 'address' | 'payment' | 'complete';

function CheckoutPage() {
  const [step, setStep] = useState<CheckoutStep>('address');
  const [order, setOrder] = useState<Order | null>(null);

  // 각 스텝의 데이터
  const [addressData, setAddressData] = useState<AddressForm>({...});
  const [paymentData, setPaymentData] = useState<PaymentForm>({...});

  // 주문 생성
  const handleCreateOrder = async () => {
    const response = await orderApi.createOrder({
      shippingAddress: addressData,
      items: cartItems
    });
    if (response.success) {
      setOrder(response.data);
      setStep('payment');
    }
  };

  // 결제 처리
  const handlePayment = async () => {
    const response = await paymentApi.processPayment({
      orderNumber: order!.orderNumber,
      ...paymentData
    });
    if (response.success) {
      clearCart();
      setStep('complete');
    }
  };

  // 스텝별 렌더링
  return (
    <div>
      <StepIndicator current={step} />

      {step === 'address' && (
        <AddressForm
          data={addressData}
          onChange={setAddressData}
          onNext={handleCreateOrder}
        />
      )}

      {step === 'payment' && order && (
        <PaymentForm
          order={order}
          data={paymentData}
          onChange={setPaymentData}
          onSubmit={handlePayment}
        />
      )}

      {step === 'complete' && order && (
        <OrderComplete order={order} />
      )}
    </div>
  );
}
```

### 8.4 스타일링 패턴

#### Design System + Tailwind 통합

```scss
// styles/index.scss

// 1. Design System CSS 변수 로드
@import '@portal/design-system/style.css';

// 2. Tailwind 레이어
@tailwind base;
@tailwind components;
@tailwind utilities;

// 3. Shopping 전용 스타일
[data-service="shopping"] {
  .product-card {
    @apply bg-bg-card border border-border-default rounded-lg;
    @apply hover:shadow-lg transition-all;
  }

  .btn-primary {
    @apply bg-brand-primary text-white px-4 py-2 rounded-lg;
    @apply transition-colors;
  }
}
```

#### Tailwind 설정

```js
// tailwind.config.js
export default {
  content: ['./src/**/*.{js,ts,jsx,tsx}'],

  // 다크모드: data-theme 속성으로 전환
  darkMode: ['selector', '[data-theme="dark"]'],

  theme: {
    extend: {
      colors: {
        // Design System CSS 변수 연결
        'brand-primary': 'var(--color-brand-primary)',
        'bg-page': 'var(--color-bg-page)',
        'text-heading': 'var(--color-text-heading)',
        'status-success': 'var(--color-status-success)',
        // ...
      }
    }
  }
}
```

---

## 요약

### React 핵심 개념
1. **컴포넌트**: 재사용 가능한 UI 조각
2. **Props**: 부모 → 자식 데이터 전달
3. **State (useState)**: 컴포넌트 내부 상태
4. **Effect (useEffect)**: 사이드 이펙트 처리
5. **Ref (useRef)**: DOM 참조 및 값 유지

### 상태 관리 (Zustand)
- `create()`: 스토어 생성
- `set()`: 상태 업데이트
- `get()`: 현재 상태 조회
- `persist`: LocalStorage 연동
- `devtools`: Redux DevTools 연동

### Module Federation
- **Host**: 원격 모듈 소비
- **Remote**: 모듈 노출 (`exposes`)
- **Shared**: 공유 라이브러리
- **mount/unmount**: 표준 마운트 인터페이스

### Shopping Frontend 아키텍처
```
bootstrap.tsx (MF 진입점)
    └── App.tsx (테마/서비스 설정)
        └── ShoppingRouter (라우팅)
            └── Pages (페이지 컴포넌트)
                ├── API Layer (axios)
                └── Zustand Store (상태)
```
