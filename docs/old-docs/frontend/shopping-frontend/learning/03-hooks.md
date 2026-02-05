# 🪝 Hooks 마스터하기

> React Hooks를 활용한 상태 관리와 부수 효과 처리를 학습합니다.

**난이도**: ⭐⭐⭐ (중급)
**학습 시간**: 60분

---

## 🎯 학습 목표

이 문서를 마치면 다음을 할 수 있습니다:
- [ ] useState로 컴포넌트 상태 관리하기
- [ ] useEffect로 부수 효과 처리하기
- [ ] useCallback으로 함수 메모이제이션하기
- [ ] useMemo로 값 메모이제이션하기
- [ ] Custom Hook 작성하기

---

## 1️⃣ useState - 상태 관리

### 기본 사용법

```tsx
import { useState } from 'react';

function Counter() {
  // [현재값, 변경함수] = useState(초기값)
  const [count, setCount] = useState(0);

  return (
    <div>
      <p>Count: {count}</p>
      <button onClick={() => setCount(count + 1)}>
        Increment
      </button>
    </div>
  );
}
```

### 여러 상태 관리

```tsx
function LoginForm() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    console.log({ email, password, rememberMe });
  };

  return (
    <form onSubmit={handleSubmit}>
      <input
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        placeholder="Email"
      />
      <input
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        placeholder="Password"
      />
      <label>
        <input
          type="checkbox"
          checked={rememberMe}
          onChange={(e) => setRememberMe(e.target.checked)}
        />
        Remember me
      </label>
      <button type="submit">Login</button>
    </form>
  );
}
```

### 객체 상태 관리

```tsx
interface User {
  name: string;
  email: string;
  age: number;
}

function UserProfile() {
  const [user, setUser] = useState<User>({
    name: '',
    email: '',
    age: 0
  });

  const updateName = (name: string) => {
    setUser(prev => ({
      ...prev,  // 기존 값 유지
      name      // name만 업데이트
    }));
  };

  const updateEmail = (email: string) => {
    setUser(prev => ({ ...prev, email }));
  };

  return (
    <div>
      <input
        value={user.name}
        onChange={(e) => updateName(e.target.value)}
        placeholder="Name"
      />
      <input
        value={user.email}
        onChange={(e) => updateEmail(e.target.value)}
        placeholder="Email"
      />
      <p>Name: {user.name}</p>
      <p>Email: {user.email}</p>
    </div>
  );
}
```

### 배열 상태 관리

```tsx
function TodoList() {
  const [todos, setTodos] = useState<string[]>([]);
  const [input, setInput] = useState('');

  // 추가
  const addTodo = () => {
    if (input.trim()) {
      setTodos(prev => [...prev, input]);
      setInput('');
    }
  };

  // 삭제
  const removeTodo = (index: number) => {
    setTodos(prev => prev.filter((_, i) => i !== index));
  };

  return (
    <div>
      <input
        value={input}
        onChange={(e) => setInput(e.target.value)}
        onKeyPress={(e) => e.key === 'Enter' && addTodo()}
      />
      <button onClick={addTodo}>Add</button>
      <ul>
        {todos.map((todo, index) => (
          <li key={index}>
            {todo}
            <button onClick={() => removeTodo(index)}>
              Delete
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
```

### 함수형 업데이트

```tsx
function Counter() {
  const [count, setCount] = useState(0);

  // ❌ 잘못된 방법 - 이전 값 참조
  const increment = () => {
    setCount(count + 1);
    setCount(count + 1);  // 여전히 같은 count 참조
  };

  // ✅ 올바른 방법 - 함수형 업데이트
  const incrementCorrect = () => {
    setCount(prev => prev + 1);
    setCount(prev => prev + 1);  // 이전 업데이트 결과 참조
  };

  return (
    <div>
      <p>Count: {count}</p>
      <button onClick={incrementCorrect}>+2</button>
    </div>
  );
}
```

---

## 2️⃣ useEffect - 부수 효과

### 기본 사용법

```tsx
import { useState, useEffect } from 'react';

function DocumentTitle() {
  const [count, setCount] = useState(0);

  // 매 렌더링 후 실행
  useEffect(() => {
    document.title = `Count: ${count}`;
  });

  return (
    <button onClick={() => setCount(count + 1)}>
      Click {count} times
    </button>
  );
}
```

### 의존성 배열

```tsx
function UserProfile({ userId }: { userId: string }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(false);

  // userId가 변경될 때만 실행
  useEffect(() => {
    const fetchUser = async () => {
      setLoading(true);
      try {
        const response = await fetch(`/api/users/${userId}`);
        const data = await response.json();
        setUser(data);
      } finally {
        setLoading(false);
      }
    };

    fetchUser();
  }, [userId]);  // 의존성 배열

  if (loading) return <div>Loading...</div>;
  return <div>{user?.name}</div>;
}
```

### 마운트 시에만 실행

```tsx
function Analytics() {
  useEffect(() => {
    // 컴포넌트 마운트 시 1회만 실행
    console.log('Component mounted');
    trackPageView();
  }, []);  // 빈 배열 = 마운트 시에만

  return <div>Analytics Tracker</div>;
}
```

### 클린업 함수

```tsx
function Timer() {
  const [seconds, setSeconds] = useState(0);

  useEffect(() => {
    // 타이머 시작
    const interval = setInterval(() => {
      setSeconds(prev => prev + 1);
    }, 1000);

    // 클린업: 컴포넌트 언마운트 시 실행
    return () => {
      clearInterval(interval);
      console.log('Timer cleaned up');
    };
  }, []);

  return <div>Seconds: {seconds}</div>;
}
```

### 이벤트 리스너

```tsx
function WindowSize() {
  const [width, setWidth] = useState(window.innerWidth);

  useEffect(() => {
    const handleResize = () => {
      setWidth(window.innerWidth);
    };

    window.addEventListener('resize', handleResize);

    // 클린업: 이벤트 리스너 제거
    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, []);

  return <div>Window width: {width}px</div>;
}
```

### 여러 useEffect 사용

```tsx
function UserDashboard({ userId }: { userId: string }) {
  const [user, setUser] = useState(null);
  const [posts, setPosts] = useState([]);

  // 사용자 정보 가져오기
  useEffect(() => {
    fetch(`/api/users/${userId}`)
      .then(res => res.json())
      .then(setUser);
  }, [userId]);

  // 사용자 게시글 가져오기
  useEffect(() => {
    fetch(`/api/users/${userId}/posts`)
      .then(res => res.json())
      .then(setPosts);
  }, [userId]);

  // 페이지 제목 업데이트
  useEffect(() => {
    if (user) {
      document.title = `${user.name}'s Dashboard`;
    }
  }, [user]);

  return <div>...</div>;
}
```

---

## 3️⃣ useCallback - 함수 메모이제이션

### 기본 사용법

```tsx
import { useState, useCallback } from 'react';

function SearchProducts() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);

  // query가 변경될 때만 함수 재생성
  const search = useCallback(async () => {
    const response = await fetch(`/api/search?q=${query}`);
    const data = await response.json();
    setResults(data);
  }, [query]);

  return (
    <div>
      <input
        value={query}
        onChange={(e) => setQuery(e.target.value)}
      />
      <button onClick={search}>Search</button>
      <ResultsList results={results} />
    </div>
  );
}
```

### 자식 컴포넌트 최적화

```tsx
import React, { useState, useCallback, memo } from 'react';

// memo로 감싸서 props가 변경될 때만 리렌더링
const ExpensiveChild = memo(({ onClick }: { onClick: () => void }) => {
  console.log('ExpensiveChild rendered');
  return <button onClick={onClick}>Click me</button>;
});

function Parent() {
  const [count, setCount] = useState(0);
  const [text, setText] = useState('');

  // useCallback 없으면 매 렌더링마다 새 함수 생성
  const handleClick = useCallback(() => {
    console.log('Button clicked');
  }, []);  // 의존성 없음 = 함수 재생성 안됨

  return (
    <div>
      <input
        value={text}
        onChange={(e) => setText(e.target.value)}
      />
      <p>Count: {count}</p>
      <button onClick={() => setCount(count + 1)}>
        Increment
      </button>
      {/* text가 변경되어도 ExpensiveChild는 리렌더링 안됨 */}
      <ExpensiveChild onClick={handleClick} />
    </div>
  );
}
```

### 의존성이 있는 경우

```tsx
function ProductList() {
  const [category, setCategory] = useState('all');
  const [sort, setSort] = useState('name');

  const fetchProducts = useCallback(async () => {
    const response = await fetch(
      `/api/products?category=${category}&sort=${sort}`
    );
    return response.json();
  }, [category, sort]);  // 의존성 변경 시 함수 재생성

  return (
    <div>
      <CategoryFilter onChange={setCategory} />
      <SortSelector onChange={setSort} />
      <ProductGrid fetchFn={fetchProducts} />
    </div>
  );
}
```

---

## 4️⃣ useMemo - 값 메모이제이션

### 기본 사용법

```tsx
import { useState, useMemo } from 'react';

function ExpensiveCalculation({ items }: { items: number[] }) {
  const [multiplier, setMultiplier] = useState(1);

  // items가 변경될 때만 재계산
  const total = useMemo(() => {
    console.log('Calculating total...');
    return items.reduce((sum, item) => sum + item, 0);
  }, [items]);

  const result = total * multiplier;

  return (
    <div>
      <p>Total: {total}</p>
      <p>Result: {result}</p>
      <button onClick={() => setMultiplier(multiplier + 1)}>
        x{multiplier + 1}
      </button>
    </div>
  );
}
```

### 필터링/정렬 최적화

```tsx
function ProductTable({ products }: { products: Product[] }) {
  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState<'name' | 'price'>('name');

  // searchTerm이나 products가 변경될 때만 필터링
  const filteredProducts = useMemo(() => {
    return products.filter(p =>
      p.name.toLowerCase().includes(searchTerm.toLowerCase())
    );
  }, [products, searchTerm]);

  // filteredProducts나 sortBy가 변경될 때만 정렬
  const sortedProducts = useMemo(() => {
    return [...filteredProducts].sort((a, b) => {
      if (sortBy === 'name') {
        return a.name.localeCompare(b.name);
      }
      return a.price - b.price;
    });
  }, [filteredProducts, sortBy]);

  return (
    <div>
      <input
        value={searchTerm}
        onChange={(e) => setSearchTerm(e.target.value)}
        placeholder="Search..."
      />
      <select
        value={sortBy}
        onChange={(e) => setSortBy(e.target.value as any)}
      >
        <option value="name">Name</option>
        <option value="price">Price</option>
      </select>
      <table>
        {sortedProducts.map(product => (
          <tr key={product.id}>
            <td>{product.name}</td>
            <td>${product.price}</td>
          </tr>
        ))}
      </table>
    </div>
  );
}
```

### 객체/배열 참조 안정화

```tsx
function UserProfile({ userId }: { userId: string }) {
  const [user, setUser] = useState(null);

  // 매번 새 객체를 생성하지 않고 메모이제이션
  const config = useMemo(() => ({
    headers: { 'User-Id': userId },
    timeout: 5000
  }), [userId]);

  useEffect(() => {
    fetch(`/api/users/${userId}`, config)
      .then(res => res.json())
      .then(setUser);
  }, [userId, config]);  // config는 userId 변경 시에만 변경

  return <div>{user?.name}</div>;
}
```

---

## 5️⃣ Custom Hooks

### 기본 패턴

```tsx
// hooks/useToggle.ts
import { useState } from 'react';

export function useToggle(initialValue = false) {
  const [value, setValue] = useState(initialValue);

  const toggle = () => setValue(prev => !prev);
  const setTrue = () => setValue(true);
  const setFalse = () => setValue(false);

  return { value, toggle, setTrue, setFalse };
}

// 사용
function Modal() {
  const { value: isOpen, toggle, setFalse } = useToggle();

  return (
    <div>
      <button onClick={toggle}>Open Modal</button>
      {isOpen && (
        <div className="modal">
          <button onClick={setFalse}>Close</button>
        </div>
      )}
    </div>
  );
}
```

### API 호출 Hook

```tsx
// hooks/useFetch.ts
import { useState, useEffect } from 'react';

interface UseFetchResult<T> {
  data: T | null;
  loading: boolean;
  error: Error | null;
  refetch: () => void;
}

export function useFetch<T>(url: string): UseFetchResult<T> {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const json = await response.json();
      setData(json);
    } catch (e) {
      setError(e as Error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [url]);

  return { data, loading, error, refetch: fetchData };
}

// 사용
function UserProfile({ userId }: { userId: string }) {
  const { data: user, loading, error, refetch } = useFetch<User>(
    `/api/users/${userId}`
  );

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error.message}</div>;

  return (
    <div>
      <h1>{user?.name}</h1>
      <button onClick={refetch}>Refresh</button>
    </div>
  );
}
```

### Local Storage Hook

```tsx
// hooks/useLocalStorage.ts
import { useState, useEffect } from 'react';

export function useLocalStorage<T>(
  key: string,
  initialValue: T
): [T, (value: T) => void] {
  // 초기값은 localStorage에서 가져오기
  const [storedValue, setStoredValue] = useState<T>(() => {
    try {
      const item = window.localStorage.getItem(key);
      return item ? JSON.parse(item) : initialValue;
    } catch (error) {
      console.error(error);
      return initialValue;
    }
  });

  // 값 변경 시 localStorage에 저장
  const setValue = (value: T) => {
    try {
      setStoredValue(value);
      window.localStorage.setItem(key, JSON.stringify(value));
    } catch (error) {
      console.error(error);
    }
  };

  return [storedValue, setValue];
}

// 사용
function Settings() {
  const [theme, setTheme] = useLocalStorage('theme', 'light');
  const [language, setLanguage] = useLocalStorage('language', 'en');

  return (
    <div>
      <select value={theme} onChange={(e) => setTheme(e.target.value)}>
        <option value="light">Light</option>
        <option value="dark">Dark</option>
      </select>
      <select value={language} onChange={(e) => setLanguage(e.target.value)}>
        <option value="en">English</option>
        <option value="ko">한국어</option>
      </select>
    </div>
  );
}
```

### 폼 관리 Hook

```tsx
// hooks/useForm.ts
import { useState, ChangeEvent } from 'react';

export function useForm<T extends Record<string, any>>(initialValues: T) {
  const [values, setValues] = useState<T>(initialValues);

  const handleChange = (
    e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
  ) => {
    const { name, value, type } = e.target;
    const checked = (e.target as HTMLInputElement).checked;

    setValues(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const reset = () => setValues(initialValues);

  return { values, handleChange, reset, setValues };
}

// 사용
function LoginForm() {
  const { values, handleChange, reset } = useForm({
    email: '',
    password: '',
    rememberMe: false
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    console.log(values);
  };

  return (
    <form onSubmit={handleSubmit}>
      <input
        name="email"
        value={values.email}
        onChange={handleChange}
        placeholder="Email"
      />
      <input
        name="password"
        type="password"
        value={values.password}
        onChange={handleChange}
        placeholder="Password"
      />
      <label>
        <input
          name="rememberMe"
          type="checkbox"
          checked={values.rememberMe}
          onChange={handleChange}
        />
        Remember me
      </label>
      <button type="submit">Login</button>
      <button type="button" onClick={reset}>Reset</button>
    </form>
  );
}
```

---

## ✍️ 실습 과제

### 과제 1: 카운트다운 타이머 (기초)

`useEffect`를 사용하여 카운트다운 타이머를 만드세요:

```tsx
// 요구사항:
// 1. 초기값 60초
// 2. 매초 1씩 감소
// 3. 0이 되면 "Time's up!" 메시지
// 4. Start/Pause 버튼
// 5. Reset 버튼
```

### 과제 2: 검색 디바운스 (중급)

입력 후 500ms 대기 후 검색하는 기능을 구현하세요:

```tsx
// 요구사항:
// 1. 검색어 입력 필드
// 2. 입력 중에는 검색 안함
// 3. 입력 멈춘 후 500ms 후 검색 실행
// 4. useEffect 클린업 활용
```

<details>
<summary>힌트</summary>

```tsx
useEffect(() => {
  const timer = setTimeout(() => {
    // 검색 로직
  }, 500);

  return () => clearTimeout(timer);  // 클린업
}, [searchTerm]);
```
</details>

### 과제 3: useArray Custom Hook (고급)

배열 조작을 쉽게 해주는 Custom Hook을 만드세요:

```tsx
// 요구사항:
// 1. push(item) - 아이템 추가
// 2. remove(index) - 아이템 삭제
// 3. update(index, item) - 아이템 수정
// 4. clear() - 전체 삭제
// 5. filter(predicate) - 필터링
```

---

## 🎯 체크리스트

학습을 마쳤다면 체크해보세요:

- [ ] useState로 상태를 관리할 수 있다
- [ ] 객체와 배열 상태를 올바르게 업데이트할 수 있다
- [ ] useEffect의 의존성 배열을 이해한다
- [ ] useEffect 클린업 함수를 작성할 수 있다
- [ ] useCallback과 useMemo의 차이를 이해한다
- [ ] 성능 최적화가 필요한 시점을 안다
- [ ] Custom Hook을 작성할 수 있다

---

**이전**: [← React 기본 문법](./02-react-basics.md)
**다음**: [상태 관리 (Zustand) →](./04-state-management.md)
