# 🗄️ 상태 관리 (Zustand)

> Zustand를 활용한 전역 상태 관리를 학습합니다.

**난이도**: ⭐⭐⭐ (중급)
**학습 시간**: 50분

---

## 🎯 학습 목표

이 문서를 마치면 다음을 할 수 있습니다:
- [ ] Zustand의 기본 개념 이해하기
- [ ] Store 생성하고 사용하기
- [ ] 비동기 액션 처리하기
- [ ] Persist 미들웨어로 데이터 영속화하기
- [ ] 여러 Store 조합하기

---

## 1️⃣ Zustand란?

### 특징

- **간단함**: Redux보다 훨씬 적은 보일러플레이트
- **빠름**: 불필요한 리렌더링 최소화
- **타입 안전**: TypeScript 완벽 지원
- **React 외부에서도 사용 가능**: Vanilla JS 지원

### Redux vs Zustand

```tsx
// Redux - 많은 보일러플레이트
// actions, reducers, dispatch, connect...

// Zustand - 간결함
const useStore = create((set) => ({
  count: 0,
  increment: () => set((state) => ({ count: state.count + 1 }))
}));
```

---

## 2️⃣ 기본 Store 생성

### 간단한 카운터

```tsx
// stores/counterStore.ts
import { create } from 'zustand';

interface CounterStore {
  count: number;
  increment: () => void;
  decrement: () => void;
  reset: () => void;
}

export const useCounterStore = create<CounterStore>((set) => ({
  count: 0,
  increment: () => set((state) => ({ count: state.count + 1 })),
  decrement: () => set((state) => ({ count: state.count - 1 })),
  reset: () => set({ count: 0 })
}));

// 컴포넌트에서 사용
function Counter() {
  const { count, increment, decrement, reset } = useCounterStore();

  return (
    <div>
      <p>Count: {count}</p>
      <button onClick={increment}>+</button>
      <button onClick={decrement}>-</button>
      <button onClick={reset}>Reset</button>
    </div>
  );
}
```

### 선택적 구독

```tsx
// 전체 store를 구독 (비효율적)
const store = useCounterStore();

// 필요한 값만 구독 (권장)
const count = useCounterStore((state) => state.count);
const increment = useCounterStore((state) => state.increment);

// 여러 값 선택
const { count, increment } = useCounterStore((state) => ({
  count: state.count,
  increment: state.increment
}));
```

---

## 3️⃣ 장바구니 Store

### 기본 구조

```tsx
// stores/cartStore.ts
import { create } from 'zustand';

interface CartItem {
  id: number;
  name: string;
  price: number;
  quantity: number;
  image: string;
}

interface CartStore {
  items: CartItem[];
  addItem: (item: Omit<CartItem, 'quantity'>) => void;
  removeItem: (id: number) => void;
  updateQuantity: (id: number, quantity: number) => void;
  clearCart: () => void;
  getTotalPrice: () => number;
  getTotalItems: () => number;
}

export const useCartStore = create<CartStore>((set, get) => ({
  items: [],

  addItem: (item) => set((state) => {
    const existingItem = state.items.find(i => i.id === item.id);

    if (existingItem) {
      // 이미 있으면 수량 증가
      return {
        items: state.items.map(i =>
          i.id === item.id
            ? { ...i, quantity: i.quantity + 1 }
            : i
        )
      };
    }

    // 없으면 새로 추가
    return {
      items: [...state.items, { ...item, quantity: 1 }]
    };
  }),

  removeItem: (id) => set((state) => ({
    items: state.items.filter(item => item.id !== id)
  })),

  updateQuantity: (id, quantity) => set((state) => {
    if (quantity <= 0) {
      // 수량이 0 이하면 삭제
      return { items: state.items.filter(item => item.id !== id) };
    }

    return {
      items: state.items.map(item =>
        item.id === id ? { ...item, quantity } : item
      )
    };
  }),

  clearCart: () => set({ items: [] }),

  getTotalPrice: () => {
    const state = get();
    return state.items.reduce(
      (total, item) => total + (item.price * item.quantity),
      0
    );
  },

  getTotalItems: () => {
    const state = get();
    return state.items.reduce(
      (total, item) => total + item.quantity,
      0
    );
  }
}));
```

### 컴포넌트에서 사용

```tsx
// pages/ProductListPage.tsx
function ProductListPage() {
  const addItem = useCartStore((state) => state.addItem);

  const products = [
    { id: 1, name: 'MacBook', price: 2399, image: '/1.jpg' },
    { id: 2, name: 'iPhone', price: 999, image: '/2.jpg' }
  ];

  return (
    <div className="product-grid">
      {products.map(product => (
        <div key={product.id} className="product-card">
          <img src={product.image} alt={product.name} />
          <h3>{product.name}</h3>
          <p>${product.price}</p>
          <button onClick={() => addItem(product)}>
            Add to Cart
          </button>
        </div>
      ))}
    </div>
  );
}

// components/CartIcon.tsx
function CartIcon() {
  const totalItems = useCartStore((state) => state.getTotalItems());

  return (
    <button className="cart-icon">
      🛒
      {totalItems > 0 && (
        <span className="badge">{totalItems}</span>
      )}
    </button>
  );
}

// pages/CartPage.tsx
function CartPage() {
  const { items, updateQuantity, removeItem, clearCart, getTotalPrice } =
    useCartStore();

  const total = getTotalPrice();

  if (items.length === 0) {
    return <div>Your cart is empty</div>;
  }

  return (
    <div>
      <h1>Shopping Cart</h1>
      {items.map(item => (
        <div key={item.id} className="cart-item">
          <img src={item.image} alt={item.name} />
          <div>
            <h3>{item.name}</h3>
            <p>${item.price}</p>
          </div>
          <div className="quantity-controls">
            <button onClick={() => updateQuantity(item.id, item.quantity - 1)}>
              -
            </button>
            <span>{item.quantity}</span>
            <button onClick={() => updateQuantity(item.id, item.quantity + 1)}>
              +
            </button>
          </div>
          <button onClick={() => removeItem(item.id)}>
            Remove
          </button>
        </div>
      ))}
      <div className="cart-summary">
        <h2>Total: ${total.toFixed(2)}</h2>
        <button onClick={clearCart}>Clear Cart</button>
        <button>Checkout</button>
      </div>
    </div>
  );
}
```

---

## 4️⃣ 비동기 액션

### API 호출 Store

```tsx
// stores/productStore.ts
import { create } from 'zustand';

interface Product {
  id: number;
  name: string;
  price: number;
  category: string;
}

interface ProductStore {
  products: Product[];
  loading: boolean;
  error: string | null;
  fetchProducts: () => Promise<void>;
  searchProducts: (query: string) => Promise<void>;
}

export const useProductStore = create<ProductStore>((set) => ({
  products: [],
  loading: false,
  error: null,

  fetchProducts: async () => {
    set({ loading: true, error: null });
    try {
      const response = await fetch('/api/products');
      if (!response.ok) {
        throw new Error('Failed to fetch products');
      }
      const data = await response.json();
      set({ products: data, loading: false });
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : 'Unknown error',
        loading: false
      });
    }
  },

  searchProducts: async (query) => {
    set({ loading: true, error: null });
    try {
      const response = await fetch(`/api/products/search?q=${query}`);
      const data = await response.json();
      set({ products: data, loading: false });
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : 'Unknown error',
        loading: false
      });
    }
  }
}));

// 사용
function ProductList() {
  const { products, loading, error, fetchProducts } = useProductStore();

  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div>
      {products.map(product => (
        <div key={product.id}>{product.name}</div>
      ))}
    </div>
  );
}
```

---

## 5️⃣ Persist 미들웨어

### Local Storage에 저장

```tsx
// stores/cartStore.ts
import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

interface CartStore {
  items: CartItem[];
  addItem: (item: CartItem) => void;
  removeItem: (id: number) => void;
  clearCart: () => void;
}

export const useCartStore = create<CartStore>()(
  persist(
    (set) => ({
      items: [],
      addItem: (item) => set((state) => ({
        items: [...state.items, item]
      })),
      removeItem: (id) => set((state) => ({
        items: state.items.filter(i => i.id !== id)
      })),
      clearCart: () => set({ items: [] })
    }),
    {
      name: 'cart-storage',  // localStorage key
      storage: createJSONStorage(() => localStorage)
    }
  )
);

// 이제 페이지 새로고침해도 장바구니 데이터 유지됨!
```

### Session Storage 사용

```tsx
export const useAuthStore = create<AuthStore>()(
  persist(
    (set) => ({
      user: null,
      token: null,
      login: (user, token) => set({ user, token }),
      logout: () => set({ user: null, token: null })
    }),
    {
      name: 'auth-storage',
      storage: createJSONStorage(() => sessionStorage)  // sessionStorage 사용
    }
  )
);
```

### 일부 필드만 저장

```tsx
export const usePreferencesStore = create<PreferencesStore>()(
  persist(
    (set) => ({
      theme: 'light',
      language: 'en',
      fontSize: 16,
      notifications: true,
      setTheme: (theme) => set({ theme }),
      setLanguage: (language) => set({ language }),
      setFontSize: (fontSize) => set({ fontSize }),
      toggleNotifications: () => set((state) => ({
        notifications: !state.notifications
      }))
    }),
    {
      name: 'preferences-storage',
      partialize: (state) => ({
        // notifications는 저장하지 않음
        theme: state.theme,
        language: state.language,
        fontSize: state.fontSize
      })
    }
  )
);
```

---

## 6️⃣ 여러 Store 조합

### Store 간 통신

```tsx
// stores/authStore.ts
export const useAuthStore = create<AuthStore>((set) => ({
  user: null,
  isAuthenticated: false,
  login: (user) => set({ user, isAuthenticated: true }),
  logout: () => set({ user: null, isAuthenticated: false })
}));

// stores/cartStore.ts
import { useAuthStore } from './authStore';

export const useCartStore = create<CartStore>((set, get) => ({
  items: [],

  checkout: async () => {
    const { isAuthenticated } = useAuthStore.getState();

    if (!isAuthenticated) {
      throw new Error('Please login first');
    }

    const items = get().items;

    const response = await fetch('/api/checkout', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ items })
    });

    if (response.ok) {
      set({ items: [] });  // 장바구니 비우기
    }
  }
}));
```

### Computed 값 (Selector)

```tsx
// stores/cartStore.ts
export const useCartStore = create<CartStore>((set, get) => ({
  items: [],

  // ... actions ...

  // Computed values
  getItemCount: () => get().items.length,
  getTotalPrice: () => get().items.reduce(
    (sum, item) => sum + item.price * item.quantity,
    0
  ),
  hasItems: () => get().items.length > 0,
  getItemById: (id: number) => get().items.find(item => item.id === id)
}));

// 사용
function CartSummary() {
  const totalPrice = useCartStore((state) => state.getTotalPrice());
  const itemCount = useCartStore((state) => state.getItemCount());
  const hasItems = useCartStore((state) => state.hasItems());

  if (!hasItems) {
    return <div>Cart is empty</div>;
  }

  return (
    <div>
      <p>Items: {itemCount}</p>
      <p>Total: ${totalPrice.toFixed(2)}</p>
    </div>
  );
}
```

---

## 7️⃣ DevTools 연동

### Redux DevTools 사용

```tsx
import { create } from 'zustand';
import { devtools } from 'zustand/middleware';

interface CounterStore {
  count: number;
  increment: () => void;
  decrement: () => void;
}

export const useCounterStore = create<CounterStore>()(
  devtools(
    (set) => ({
      count: 0,
      increment: () => set((state) => ({ count: state.count + 1 }), false, 'increment'),
      decrement: () => set((state) => ({ count: state.count - 1 }), false, 'decrement')
    }),
    {
      name: 'CounterStore'  // DevTools에 표시될 이름
    }
  )
);
```

---

## ✍️ 실습 과제

### 과제 1: Todo Store (기초)

Todo 앱의 Store를 만드세요:

```tsx
// 요구사항:
// 1. Todo 추가 (addTodo)
// 2. Todo 삭제 (removeTodo)
// 3. Todo 완료 토글 (toggleTodo)
// 4. 전체 삭제 (clearCompleted)
// 5. 완료된 Todo 개수 (getCompletedCount)
```

<details>
<summary>정답 보기</summary>

```tsx
interface Todo {
  id: number;
  text: string;
  completed: boolean;
}

interface TodoStore {
  todos: Todo[];
  addTodo: (text: string) => void;
  removeTodo: (id: number) => void;
  toggleTodo: (id: number) => void;
  clearCompleted: () => void;
  getCompletedCount: () => number;
}

export const useTodoStore = create<TodoStore>((set, get) => ({
  todos: [],

  addTodo: (text) => set((state) => ({
    todos: [...state.todos, {
      id: Date.now(),
      text,
      completed: false
    }]
  })),

  removeTodo: (id) => set((state) => ({
    todos: state.todos.filter(todo => todo.id !== id)
  })),

  toggleTodo: (id) => set((state) => ({
    todos: state.todos.map(todo =>
      todo.id === id
        ? { ...todo, completed: !todo.completed }
        : todo
    )
  })),

  clearCompleted: () => set((state) => ({
    todos: state.todos.filter(todo => !todo.completed)
  })),

  getCompletedCount: () => {
    return get().todos.filter(todo => todo.completed).length;
  }
}));
```
</details>

### 과제 2: User Store with Persist (중급)

사용자 정보를 localStorage에 저장하는 Store를 만드세요:

```tsx
// 요구사항:
// 1. 로그인 (login)
// 2. 로그아웃 (logout)
// 3. 프로필 업데이트 (updateProfile)
// 4. localStorage에 자동 저장
// 5. token은 저장하지 않음 (보안)
```

### 과제 3: Async Search Store (고급)

검색 기능이 있는 Store를 만드세요:

```tsx
// 요구사항:
// 1. 검색어 입력 시 API 호출
// 2. 로딩 상태 관리
// 3. 에러 처리
// 4. 검색 히스토리 저장 (최근 5개)
// 5. 히스토리 삭제 기능
```

---

## 🎯 체크리스트

학습을 마쳤다면 체크해보세요:

- [ ] Zustand Store를 생성할 수 있다
- [ ] set과 get 함수를 올바르게 사용한다
- [ ] 선택적 구독으로 성능 최적화를 할 수 있다
- [ ] 비동기 액션을 처리할 수 있다
- [ ] Persist 미들웨어를 사용할 수 있다
- [ ] 여러 Store를 조합할 수 있다
- [ ] Redux DevTools를 활용할 수 있다

---

**이전**: [← Hooks 마스터하기](./03-hooks.md)
**다음**: [라우팅 (React Router) →](./05-routing.md)
