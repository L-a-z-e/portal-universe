# 🧭 라우팅 (React Router)

> React Router를 활용한 페이지 네비게이션을 학습합니다.

**난이도**: ⭐⭐ (기초)
**학습 시간**: 45분

---

## 🎯 학습 목표

이 문서를 마치면 다음을 할 수 있습니다:
- [ ] React Router 기본 개념 이해하기
- [ ] Route와 페이지 연결하기
- [ ] Link와 useNavigate로 네비게이션하기
- [ ] URL 파라미터와 쿼리 스트링 사용하기
- [ ] Protected Route 구현하기

---

## 1️⃣ React Router 기본

### 설치

```bash
pnpm add react-router-dom
```

### 기본 구조

```tsx
// main.tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import { RouterProvider } from 'react-router-dom';
import { router } from './router';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <RouterProvider router={router} />
  </React.StrictMode>
);

// router/index.tsx
import { createBrowserRouter } from 'react-router-dom';
import App from '../App';
import HomePage from '../pages/HomePage';
import AboutPage from '../pages/AboutPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      {
        index: true,
        element: <HomePage />
      },
      {
        path: 'about',
        element: <AboutPage />
      }
    ]
  }
]);

// App.tsx
import { Outlet, Link } from 'react-router-dom';

function App() {
  return (
    <div>
      <nav>
        <Link to="/">Home</Link>
        <Link to="/about">About</Link>
      </nav>
      <main>
        <Outlet />  {/* 자식 Route가 렌더링되는 위치 */}
      </main>
    </div>
  );
}

export default App;
```

---

## 2️⃣ Route 정의

### 중첩 라우트

```tsx
// router/index.tsx
export const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      {
        index: true,  // path: '' 와 동일
        element: <HomePage />
      },
      {
        path: 'products',
        element: <ProductLayout />,
        children: [
          {
            index: true,
            element: <ProductListPage />
          },
          {
            path: ':id',
            element: <ProductDetailPage />
          },
          {
            path: 'new',
            element: <ProductCreatePage />
          }
        ]
      },
      {
        path: 'cart',
        element: <CartPage />
      },
      {
        path: 'orders',
        element: <OrderListPage />
      },
      {
        path: 'orders/:id',
        element: <OrderDetailPage />
      }
    ]
  }
]);

// 결과:
// /                         → HomePage
// /products                 → ProductListPage
// /products/123             → ProductDetailPage (id=123)
// /products/new             → ProductCreatePage
// /cart                     → CartPage
// /orders                   → OrderListPage
// /orders/456               → OrderDetailPage (id=456)
```

### Layout 컴포넌트

```tsx
// components/layout/ProductLayout.tsx
import { Outlet } from 'react-router-dom';

function ProductLayout() {
  return (
    <div className="product-layout">
      <aside className="sidebar">
        <CategoryFilter />
        <PriceFilter />
      </aside>
      <main>
        <Outlet />  {/* ProductListPage 또는 ProductDetailPage */}
      </main>
    </div>
  );
}
```

---

## 3️⃣ 네비게이션

### Link 컴포넌트

```tsx
import { Link } from 'react-router-dom';

function Navigation() {
  return (
    <nav>
      {/* 기본 링크 */}
      <Link to="/">Home</Link>
      <Link to="/products">Products</Link>
      <Link to="/cart">Cart</Link>

      {/* 동적 링크 */}
      <Link to={`/products/${productId}`}>
        View Product
      </Link>

      {/* 쿼리 스트링 */}
      <Link to="/products?category=laptop&sort=price">
        Laptops
      </Link>

      {/* 스타일링 */}
      <Link
        to="/products"
        className="nav-link"
        style={{ color: 'blue' }}
      >
        Products
      </Link>
    </nav>
  );
}
```

### NavLink (활성 상태 표시)

```tsx
import { NavLink } from 'react-router-dom';

function Navigation() {
  return (
    <nav>
      <NavLink
        to="/"
        className={({ isActive }) =>
          isActive ? 'nav-link active' : 'nav-link'
        }
      >
        Home
      </NavLink>

      <NavLink
        to="/products"
        style={({ isActive }) => ({
          color: isActive ? 'blue' : 'black',
          fontWeight: isActive ? 'bold' : 'normal'
        })}
      >
        Products
      </NavLink>
    </nav>
  );
}
```

### useNavigate Hook

```tsx
import { useNavigate } from 'react-router-dom';

function LoginPage() {
  const navigate = useNavigate();

  const handleLogin = async (email: string, password: string) => {
    try {
      await api.login(email, password);
      // 로그인 성공 시 홈으로 이동
      navigate('/');
    } catch (error) {
      alert('Login failed');
    }
  };

  const handleCancel = () => {
    // 이전 페이지로
    navigate(-1);
  };

  return (
    <form onSubmit={(e) => {
      e.preventDefault();
      handleLogin(email, password);
    }}>
      <input type="email" />
      <input type="password" />
      <button type="submit">Login</button>
      <button type="button" onClick={handleCancel}>
        Cancel
      </button>
    </form>
  );
}
```

### Replace vs Push

```tsx
function ExampleNavigation() {
  const navigate = useNavigate();

  // 히스토리에 추가 (기본 동작)
  navigate('/products');

  // 현재 페이지를 교체 (뒤로가기 불가)
  navigate('/login', { replace: true });

  // 상태 전달
  navigate('/products/123', {
    state: { from: 'cart' }
  });
}
```

---

## 4️⃣ URL 파라미터

### Path Parameters

```tsx
// router/index.tsx
{
  path: 'products/:id',
  element: <ProductDetailPage />
}

// pages/ProductDetailPage.tsx
import { useParams } from 'react-router-dom';

function ProductDetailPage() {
  const { id } = useParams();  // URL의 :id 값

  const [product, setProduct] = useState(null);

  useEffect(() => {
    fetch(`/api/products/${id}`)
      .then(res => res.json())
      .then(setProduct);
  }, [id]);

  if (!product) return <div>Loading...</div>;

  return (
    <div>
      <h1>{product.name}</h1>
      <p>{product.description}</p>
      <p>${product.price}</p>
    </div>
  );
}
```

### 여러 파라미터

```tsx
// router/index.tsx
{
  path: 'categories/:categoryId/products/:productId',
  element: <ProductDetailPage />
}

// pages/ProductDetailPage.tsx
function ProductDetailPage() {
  const { categoryId, productId } = useParams();

  // /categories/electronics/products/123
  // categoryId: "electronics"
  // productId: "123"

  return <div>...</div>;
}
```

---

## 5️⃣ Query String

### useSearchParams Hook

```tsx
import { useSearchParams } from 'react-router-dom';

function ProductListPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  // URL에서 파라미터 읽기
  const category = searchParams.get('category') || 'all';
  const sort = searchParams.get('sort') || 'name';
  const page = parseInt(searchParams.get('page') || '1');

  const handleCategoryChange = (newCategory: string) => {
    // 쿼리 스트링 업데이트
    setSearchParams({
      category: newCategory,
      sort,
      page: '1'  // 카테고리 변경 시 1페이지로
    });
  };

  const handleSortChange = (newSort: string) => {
    setSearchParams({
      category,
      sort: newSort,
      page: page.toString()
    });
  };

  return (
    <div>
      {/* URL: /products?category=laptop&sort=price&page=1 */}
      <select value={category} onChange={(e) => handleCategoryChange(e.target.value)}>
        <option value="all">All</option>
        <option value="laptop">Laptops</option>
        <option value="phone">Phones</option>
      </select>

      <select value={sort} onChange={(e) => handleSortChange(e.target.value)}>
        <option value="name">Name</option>
        <option value="price">Price</option>
      </select>

      <ProductList category={category} sort={sort} page={page} />
    </div>
  );
}
```

---

## 6️⃣ Protected Routes

### RequireAuth 컴포넌트

```tsx
// components/guards/RequireAuth.tsx
import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';

interface RequireAuthProps {
  children: React.ReactNode;
}

export function RequireAuth({ children }: RequireAuthProps) {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const location = useLocation();

  if (!isAuthenticated) {
    // 로그인 페이지로 리다이렉트
    // 로그인 후 원래 페이지로 돌아오기 위해 state에 경로 저장
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <>{children}</>;
}

// router/index.tsx
{
  path: 'cart',
  element: (
    <RequireAuth>
      <CartPage />
    </RequireAuth>
  )
},
{
  path: 'orders',
  element: (
    <RequireAuth>
      <OrderListPage />
    </RequireAuth>
  )
}
```

### Role 기반 보호

```tsx
// components/guards/RequireRole.tsx
import { Navigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';

interface RequireRoleProps {
  children: React.ReactNode;
  role: 'admin' | 'user';
}

export function RequireRole({ children, role }: RequireRoleProps) {
  const { isAuthenticated, user } = useAuthStore();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (user?.role !== role) {
    return <Navigate to="/forbidden" replace />;
  }

  return <>{children}</>;
}

// router/index.tsx
{
  path: 'admin',
  element: (
    <RequireRole role="admin">
      <AdminLayout />
    </RequireRole>
  ),
  children: [
    {
      path: 'products',
      element: <AdminProductListPage />
    },
    {
      path: 'orders',
      element: <AdminOrderListPage />
    }
  ]
}
```

### 로그인 후 리다이렉트

```tsx
// pages/LoginPage.tsx
import { useNavigate, useLocation } from 'react-router-dom';

function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogin = async (email: string, password: string) => {
    try {
      await api.login(email, password);

      // RequireAuth에서 저장한 경로로 이동
      const from = location.state?.from?.pathname || '/';
      navigate(from, { replace: true });
    } catch (error) {
      alert('Login failed');
    }
  };

  return <form>...</form>;
}
```

---

## 7️⃣ Error Handling

### 404 페이지

```tsx
// router/index.tsx
export const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      // ... 다른 라우트들 ...
      {
        path: '*',  // 모든 매칭되지 않는 경로
        element: <NotFoundPage />
      }
    ]
  }
]);

// pages/NotFoundPage.tsx
import { Link } from 'react-router-dom';

function NotFoundPage() {
  return (
    <div className="not-found">
      <h1>404 - Page Not Found</h1>
      <p>The page you're looking for doesn't exist.</p>
      <Link to="/">Go Home</Link>
    </div>
  );
}
```

### Error Boundary

```tsx
// router/index.tsx
export const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    errorElement: <ErrorPage />,
    children: [
      // ...
    ]
  }
]);

// pages/ErrorPage.tsx
import { useRouteError, Link } from 'react-router-dom';

function ErrorPage() {
  const error = useRouteError() as any;

  return (
    <div className="error-page">
      <h1>Oops!</h1>
      <p>Sorry, an unexpected error occurred.</p>
      <p>
        <i>{error.statusText || error.message}</i>
      </p>
      <Link to="/">Go Home</Link>
    </div>
  );
}
```

---

## ✍️ 실습 과제

### 과제 1: 블로그 라우팅 (기초)

다음 페이지 구조를 가진 블로그 앱의 라우팅을 구현하세요:

```
/                  → HomePage
/posts             → PostListPage
/posts/:id         → PostDetailPage
/posts/new         → PostCreatePage
/about             → AboutPage
```

### 과제 2: 검색 필터 (중급)

상품 목록 페이지에 검색 필터를 구현하세요:

```tsx
// 요구사항:
// 1. URL: /products?category=laptop&sort=price&page=2
// 2. 카테고리 선택 시 URL 업데이트
// 3. 정렬 옵션 변경 시 URL 업데이트
// 4. 페이지 변경 시 URL 업데이트
// 5. 브라우저 뒤로가기/앞으로가기 동작
```

### 과제 3: 관리자 페이지 (고급)

관리자 전용 페이지를 구현하세요:

```tsx
// 요구사항:
// 1. /admin 경로는 admin 권한 필요
// 2. 권한 없으면 /forbidden으로 리다이렉트
// 3. 로그인 안되어있으면 /login으로 리다이렉트
// 4. 로그인 후 원래 페이지로 복귀
// 5. AdminLayout으로 감싸진 중첩 라우트
```

<details>
<summary>힌트</summary>

```tsx
// 1. RequireRole 컴포넌트 만들기
// 2. router에 admin 라우트 추가
// 3. errorElement로 ForbiddenPage 추가
// 4. location.state로 이전 경로 저장
```
</details>

---

## 🎯 체크리스트

학습을 마쳤다면 체크해보세요:

- [ ] createBrowserRouter로 라우터를 생성할 수 있다
- [ ] 중첩 라우트와 Outlet을 이해한다
- [ ] Link와 NavLink의 차이를 안다
- [ ] useNavigate로 프로그래밍 방식 네비게이션을 할 수 있다
- [ ] useParams로 URL 파라미터를 읽을 수 있다
- [ ] useSearchParams로 쿼리 스트링을 다룰 수 있다
- [ ] Protected Route를 구현할 수 있다
- [ ] 404 페이지와 Error Boundary를 설정할 수 있다

---

**이전**: [← 상태 관리 (Zustand)](./04-state-management.md)
**다음**: [스타일링 (Tailwind CSS) →](./06-styling.md)
