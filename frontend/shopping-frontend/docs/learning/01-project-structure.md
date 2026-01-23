# 📁 프로젝트 구조

> Shopping Frontend의 디렉토리 구조와 파일 역할을 이해합니다.

**난이도**: ⭐ (입문)
**학습 시간**: 20분

---

## 🎯 학습 목표

이 문서를 마치면 다음을 할 수 있습니다:
- [ ] 프로젝트 디렉토리 구조 이해하기
- [ ] 각 폴더의 역할 파악하기
- [ ] 진입점과 빌드 흐름 이해하기

---

## 1️⃣ 전체 구조

```
frontend/shopping-frontend/
├── src/                    # 소스 코드
│   ├── components/         # 재사용 가능한 컴포넌트
│   ├── pages/              # 페이지 컴포넌트
│   ├── stores/             # Zustand 상태 관리
│   ├── hooks/              # Custom Hooks
│   ├── api/                # API 클라이언트
│   ├── router/             # 라우팅 설정
│   ├── types/              # TypeScript 타입 정의
│   ├── App.tsx             # 루트 컴포넌트
│   ├── main.tsx            # 진입점 (Standalone)
│   ├── bootstrap.tsx       # 진입점 (Module Federation)
│   └── index.tsx           # 동적 import 처리
├── public/                 # 정적 파일
├── docs/                   # 문서
├── package.json            # 의존성 및 스크립트
├── tsconfig.json           # TypeScript 설정
├── vite.config.ts          # Vite 빌드 설정
└── tailwind.config.js      # Tailwind CSS 설정
```

---

## 2️⃣ src/ 디렉토리 상세

### components/ - 컴포넌트

```
components/
├── common/                 # 공통 컴포넌트
│   ├── Button.tsx          # 버튼
│   ├── ConfirmModal.tsx    # 확인 모달
│   └── Pagination.tsx      # 페이지네이션
├── form/                   # 폼 관련
│   ├── Input.tsx           # 입력 필드
│   └── TextArea.tsx        # 텍스트 영역
├── guards/                 # 라우트 가드
│   ├── RequireAuth.tsx     # 인증 필수
│   └── RequireRole.tsx     # 권한 필수
├── layout/                 # 레이아웃
│   └── AdminLayout.tsx     # 관리자 레이아웃
├── coupon/                 # 쿠폰 관련
│   ├── CouponCard.tsx
│   └── CouponSelector.tsx
├── timedeal/               # 타임딜 관련
│   ├── TimeDealCard.tsx
│   └── CountdownTimer.tsx
├── queue/                  # 대기열 관련
│   └── QueueStatus.tsx
├── ProductCard.tsx         # 상품 카드
└── CartItem.tsx            # 장바구니 아이템
```

**역할**:
- 재사용 가능한 UI 컴포넌트
- 비즈니스 로직 최소화
- Props로 동작 커스터마이징

### pages/ - 페이지

```
pages/
├── admin/                  # 관리자 페이지
│   ├── AdminProductListPage.tsx
│   ├── AdminProductFormPage.tsx
│   ├── AdminCouponListPage.tsx
│   ├── AdminCouponFormPage.tsx
│   ├── AdminTimeDealListPage.tsx
│   └── AdminTimeDealFormPage.tsx
├── coupon/                 # 쿠폰 페이지
│   └── CouponListPage.tsx
├── timedeal/               # 타임딜 페이지
│   ├── TimeDealListPage.tsx
│   └── TimeDealDetailPage.tsx
├── queue/                  # 대기열 페이지
│   └── QueueWaitingPage.tsx
├── error/                  # 에러 페이지
│   └── ForbiddenPage.tsx
├── ProductListPage.tsx     # 상품 목록
├── ProductDetailPage.tsx   # 상품 상세
├── CartPage.tsx            # 장바구니
├── CheckoutPage.tsx        # 결제
├── OrderListPage.tsx       # 주문 목록
└── OrderDetailPage.tsx     # 주문 상세
```

**역할**:
- Route와 1:1 매핑
- 페이지별 로직 포함
- 컴포넌트 조합

### stores/ - 상태 관리

```
stores/
├── cartStore.ts            # 장바구니 상태
└── authStore.ts            # 인증 상태 (Portal에서 주입)
```

**Zustand Store 예시**:
```typescript
// cartStore.ts
import { create } from 'zustand';

interface CartStore {
  items: CartItem[];
  addItem: (item: CartItem) => void;
  removeItem: (id: string) => void;
  clearCart: () => void;
}

export const useCartStore = create<CartStore>((set) => ({
  items: [],
  addItem: (item) => set((state) => ({
    items: [...state.items, item]
  })),
  removeItem: (id) => set((state) => ({
    items: state.items.filter(item => item.id !== id)
  })),
  clearCart: () => set({ items: [] })
}));
```

### hooks/ - Custom Hooks

```
hooks/
├── usePortalStore.ts       # Portal 상태 접근
├── useQueue.ts             # 대기열 Hook
├── useAdminProducts.ts     # 관리자 상품 Hook
├── useAdminCoupons.ts      # 관리자 쿠폰 Hook
├── useAdminTimeDeals.ts    # 관리자 타임딜 Hook
├── useCoupons.ts           # 쿠폰 Hook
└── useTimeDeals.ts         # 타임딜 Hook
```

**Custom Hook 예시**:
```typescript
// useCoupons.ts
export function useCoupons() {
  const [coupons, setCoupons] = useState<Coupon[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchCoupons = async () => {
    setLoading(true);
    try {
      const data = await api.getCoupons();
      setCoupons(data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCoupons();
  }, []);

  return { coupons, loading, refetch: fetchCoupons };
}
```

### api/ - API 클라이언트

```
api/
├── client.ts               # Axios 인스턴스
└── endpoints.ts            # API 엔드포인트 함수
```

**API 클라이언트**:
```typescript
// client.ts
import axios from 'axios';

export const apiClient = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000
});

// 요청 인터셉터
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

### router/ - 라우팅

```
router/
└── index.tsx               # React Router 설정
```

**라우터 구조**:
```typescript
// router/index.tsx
import { createBrowserRouter } from 'react-router-dom';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      {
        index: true,
        element: <ProductListPage />
      },
      {
        path: 'products/:id',
        element: <ProductDetailPage />
      },
      {
        path: 'cart',
        element: (
          <RequireAuth>
            <CartPage />
          </RequireAuth>
        )
      },
      // ...
    ]
  }
]);
```

---

## 3️⃣ 진입점

### Standalone 모드: main.tsx

```typescript
// main.tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import { RouterProvider } from 'react-router-dom';
import { router } from './router';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <RouterProvider router={router} />
  </React.StrictMode>
);
```

**용도**: `pnpm dev`로 독립 실행 시

### Module Federation: bootstrap.tsx

```typescript
// bootstrap.tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';

export function mount(el: HTMLElement, portalContext?: any) {
  // Portal에서 주입된 컨텍스트 사용
  const apiClient = portalContext?.apiClient;
  const authStore = portalContext?.authStore;

  const root = ReactDOM.createRoot(el);
  root.render(
    <React.StrictMode>
      <App apiClient={apiClient} authStore={authStore} />
    </React.StrictMode>
  );

  return () => root.unmount();
}

// Standalone 모드 지원
if (import.meta.env.DEV && document.getElementById('root')) {
  mount(document.getElementById('root')!);
}
```

**용도**: Portal Shell에서 Remote로 로드될 때

### 동적 Import: index.tsx

```typescript
// index.tsx
// Module Federation에서 비동기 로드 필요
import('./bootstrap');
```

---

## 4️⃣ 설정 파일

### package.json

```json
{
  "name": "shopping-frontend",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "react-router-dom": "^7.1.1",
    "zustand": "^5.0.2",
    "axios": "^1.7.9"
  },
  "devDependencies": {
    "@vitejs/plugin-react": "^4.3.4",
    "vite": "^6.0.11",
    "typescript": "^5.7.3",
    "tailwindcss": "^4.0.0"
  }
}
```

### tsconfig.json

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "ESNext",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "jsx": "react-jsx",
    "strict": true,
    "moduleResolution": "bundler",
    "resolveJsonModule": true,
    "esModuleInterop": true,
    "skipLibCheck": true
  }
}
```

### vite.config.ts

```typescript
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import federation from '@originjs/vite-plugin-federation';

export default defineConfig({
  plugins: [
    react(),
    federation({
      name: 'shopping',
      filename: 'remoteEntry.js',
      exposes: {
        './App': './src/bootstrap'
      },
      shared: ['react', 'react-dom', 'react-router-dom']
    })
  ]
});
```

---

## ✍️ 실습 과제

### 과제 1: 디렉토리 탐색 (기초)

프로젝트를 열고 각 폴더를 둘러보세요.

```bash
cd frontend/shopping-frontend

# 구조 확인
tree -L 3 src/

# 각 파일 열어보기
code src/App.tsx
code src/pages/ProductListPage.tsx
code src/components/ProductCard.tsx
```

**확인사항**:
- [ ] components와 pages의 차이를 이해했는가?
- [ ] Custom Hook 파일을 찾았는가?
- [ ] API 클라이언트 위치를 파악했는가?

### 과제 2: 파일 추적 (중급)

사용자가 상품을 클릭하면 어떤 파일들이 실행되는지 추적하세요.

```
1. pages/ProductListPage.tsx
   └─ <ProductCard /> 클릭
2. components/ProductCard.tsx
   └─ onClick → navigate(`/products/${id}`)
3. router/index.tsx
   └─ Route 매칭 → ProductDetailPage
4. pages/ProductDetailPage.tsx
   └─ useParams()로 id 추출
5. hooks/useProducts.ts (또는 API 호출)
   └─ api/endpoints.ts → getProductById(id)
6. 상품 데이터 렌더링
```

### 과제 3: 새 페이지 추가 (고급)

"위시리스트" 페이지를 추가해보세요.

```typescript
// 1. pages/WishlistPage.tsx 생성
export default function WishlistPage() {
  return (
    <div>
      <h1>My Wishlist</h1>
    </div>
  );
}

// 2. router/index.tsx에 라우트 추가
{
  path: 'wishlist',
  element: <WishlistPage />
}

// 3. 네비게이션 링크 추가
<Link to="/wishlist">Wishlist</Link>
```

---

## 🎯 체크리스트

학습을 마쳤다면 체크해보세요:

- [ ] src/ 디렉토리 구조를 설명할 수 있다
- [ ] components와 pages의 차이를 이해한다
- [ ] main.tsx vs bootstrap.tsx의 차이를 안다
- [ ] Custom Hook이 무엇인지 안다
- [ ] 라우터가 어디에 정의되는지 안다

---

**다음**: [React 기본 문법 학습하기](./02-react-basics.md) →
