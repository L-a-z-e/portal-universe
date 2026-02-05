# ⚛️ React 기본 문법

> JSX 문법과 컴포넌트의 기본 개념을 학습합니다.

**난이도**: ⭐⭐ (기초)
**학습 시간**: 40분

---

## 🎯 학습 목표

이 문서를 마치면 다음을 할 수 있습니다:
- [ ] JSX 문법 이해하기
- [ ] 함수형 컴포넌트 작성하기
- [ ] Props로 데이터 전달하기
- [ ] 조건부 렌더링 구현하기
- [ ] 리스트 렌더링하기

---

## 1️⃣ JSX 기본

### JSX란?

JSX는 JavaScript XML의 약자로, HTML과 유사한 문법으로 React 엘리먼트를 작성할 수 있습니다.

```tsx
// JSX
const element = <h1>Hello, World!</h1>;

// 컴파일 후 (Babel)
const element = React.createElement('h1', null, 'Hello, World!');
```

### JSX 규칙

**1. 단일 루트 엘리먼트**

```tsx
// ❌ 잘못된 예
function BadComponent() {
  return (
    <h1>Title</h1>
    <p>Content</p>
  );
}

// ✅ 올바른 예 - Fragment 사용
function GoodComponent() {
  return (
    <>
      <h1>Title</h1>
      <p>Content</p>
    </>
  );
}

// ✅ 또는 div로 감싸기
function GoodComponent2() {
  return (
    <div>
      <h1>Title</h1>
      <p>Content</p>
    </div>
  );
}
```

**2. 태그는 반드시 닫아야 함**

```tsx
// ❌ 잘못된 예
<img src="photo.jpg">
<br>

// ✅ 올바른 예
<img src="photo.jpg" />
<br />
```

**3. camelCase 속성명**

```tsx
// HTML
<div class="container" onclick="handleClick()">

// JSX
<div className="container" onClick={handleClick}>
```

**4. JavaScript 표현식 사용**

```tsx
function Greeting() {
  const name = "Alice";
  const age = 25;

  return (
    <div>
      <h1>Hello, {name}!</h1>
      <p>You are {age} years old</p>
      <p>Next year you will be {age + 1}</p>
    </div>
  );
}
```

### 인라인 스타일

```tsx
function StyledComponent() {
  const style = {
    color: 'blue',
    fontSize: '20px',
    backgroundColor: 'lightgray'  // CSS: background-color
  };

  return (
    <div style={style}>
      Styled Text
    </div>
  );
}

// 또는 직접 작성
<div style={{ color: 'red', padding: '10px' }}>
  Direct Style
</div>
```

---

## 2️⃣ 함수형 컴포넌트

### 기본 구조

```tsx
// 함수 선언문
function Welcome() {
  return <h1>Welcome!</h1>;
}

// 화살표 함수
const Welcome = () => {
  return <h1>Welcome!</h1>;
};

// 간단한 경우 return 생략 가능
const Welcome = () => <h1>Welcome!</h1>;
```

### 실제 예제

```tsx
// components/ProductCard.tsx
export function ProductCard() {
  return (
    <div className="product-card">
      <img
        src="/products/1.jpg"
        alt="Product"
        className="product-image"
      />
      <h3 className="product-name">MacBook Pro</h3>
      <p className="product-price">$2,399</p>
      <button className="btn-add-cart">
        Add to Cart
      </button>
    </div>
  );
}
```

---

## 3️⃣ Props (속성)

### Props 전달

```tsx
// 부모 컴포넌트
function ProductListPage() {
  return (
    <div>
      <ProductCard
        name="MacBook Pro"
        price={2399}
        inStock={true}
      />
      <ProductCard
        name="iPhone 15"
        price={999}
        inStock={false}
      />
    </div>
  );
}

// 자식 컴포넌트
interface ProductCardProps {
  name: string;
  price: number;
  inStock: boolean;
}

function ProductCard({ name, price, inStock }: ProductCardProps) {
  return (
    <div className="product-card">
      <h3>{name}</h3>
      <p>${price}</p>
      {inStock ? (
        <span className="badge-success">In Stock</span>
      ) : (
        <span className="badge-danger">Out of Stock</span>
      )}
    </div>
  );
}
```

### Props 기본값

```tsx
interface ButtonProps {
  text: string;
  variant?: 'primary' | 'secondary';  // ? = optional
  disabled?: boolean;
}

function Button({
  text,
  variant = 'primary',  // 기본값
  disabled = false
}: ButtonProps) {
  return (
    <button
      className={`btn btn-${variant}`}
      disabled={disabled}
    >
      {text}
    </button>
  );
}

// 사용
<Button text="Submit" />
<Button text="Cancel" variant="secondary" />
<Button text="Disabled" disabled={true} />
```

### Children Prop

```tsx
interface CardProps {
  title: string;
  children: React.ReactNode;
}

function Card({ title, children }: CardProps) {
  return (
    <div className="card">
      <h2 className="card-title">{title}</h2>
      <div className="card-body">
        {children}
      </div>
    </div>
  );
}

// 사용
<Card title="Product Details">
  <p>This is a great product!</p>
  <button>Buy Now</button>
</Card>
```

---

## 4️⃣ 조건부 렌더링

### 1. if 문 (컴포넌트 외부)

```tsx
function UserGreeting({ isLoggedIn }: { isLoggedIn: boolean }) {
  if (isLoggedIn) {
    return <h1>Welcome back!</h1>;
  }
  return <h1>Please sign in.</h1>;
}
```

### 2. 삼항 연산자

```tsx
function LoginButton({ isLoggedIn }: { isLoggedIn: boolean }) {
  return (
    <button>
      {isLoggedIn ? 'Logout' : 'Login'}
    </button>
  );
}
```

### 3. && 연산자

```tsx
function Notifications({ count }: { count: number }) {
  return (
    <div>
      <h1>Notifications</h1>
      {count > 0 && (
        <span className="badge">{count}</span>
      )}
    </div>
  );
}
```

### 4. 여러 조건

```tsx
function OrderStatus({ status }: { status: string }) {
  let badge;

  if (status === 'pending') {
    badge = <span className="badge-warning">Pending</span>;
  } else if (status === 'shipped') {
    badge = <span className="badge-info">Shipped</span>;
  } else if (status === 'delivered') {
    badge = <span className="badge-success">Delivered</span>;
  } else {
    badge = <span className="badge-secondary">Unknown</span>;
  }

  return <div>Status: {badge}</div>;
}
```

### 5. Null 반환

```tsx
function WarningBanner({ show }: { show: boolean }) {
  if (!show) {
    return null;  // 아무것도 렌더링하지 않음
  }

  return (
    <div className="alert alert-warning">
      Warning: Please verify your email!
    </div>
  );
}
```

---

## 5️⃣ 리스트 렌더링

### 기본 map 사용

```tsx
function ProductList() {
  const products = [
    { id: 1, name: 'MacBook', price: 2399 },
    { id: 2, name: 'iPhone', price: 999 },
    { id: 3, name: 'iPad', price: 599 }
  ];

  return (
    <ul>
      {products.map(product => (
        <li key={product.id}>
          {product.name} - ${product.price}
        </li>
      ))}
    </ul>
  );
}
```

### key의 중요성

```tsx
// ❌ key 없음 - 경고 발생
{products.map(product => (
  <ProductCard name={product.name} price={product.price} />
))}

// ❌ index를 key로 사용 - 비권장 (순서가 바뀌면 문제)
{products.map((product, index) => (
  <ProductCard key={index} name={product.name} price={product.price} />
))}

// ✅ 고유한 id를 key로 사용
{products.map(product => (
  <ProductCard
    key={product.id}
    name={product.name}
    price={product.price}
  />
))}
```

### 컴포넌트로 분리

```tsx
interface Product {
  id: number;
  name: string;
  price: number;
  image: string;
}

function ProductList() {
  const products: Product[] = [
    { id: 1, name: 'MacBook', price: 2399, image: '/1.jpg' },
    { id: 2, name: 'iPhone', price: 999, image: '/2.jpg' }
  ];

  return (
    <div className="product-grid">
      {products.map(product => (
        <ProductCard key={product.id} product={product} />
      ))}
    </div>
  );
}

function ProductCard({ product }: { product: Product }) {
  return (
    <div className="product-card">
      <img src={product.image} alt={product.name} />
      <h3>{product.name}</h3>
      <p>${product.price}</p>
    </div>
  );
}
```

### 빈 리스트 처리

```tsx
function ProductList({ products }: { products: Product[] }) {
  if (products.length === 0) {
    return (
      <div className="empty-state">
        <p>No products found.</p>
      </div>
    );
  }

  return (
    <div className="product-grid">
      {products.map(product => (
        <ProductCard key={product.id} product={product} />
      ))}
    </div>
  );
}
```

---

## 6️⃣ 이벤트 처리

### 기본 이벤트

```tsx
function ClickExample() {
  const handleClick = () => {
    alert('Button clicked!');
  };

  return (
    <button onClick={handleClick}>
      Click Me
    </button>
  );
}

// 인라인으로 작성
<button onClick={() => alert('Clicked!')}>
  Click Me
</button>
```

### 이벤트 객체

```tsx
function InputExample() {
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    console.log('Input value:', e.target.value);
  };

  return (
    <input
      type="text"
      onChange={handleChange}
      placeholder="Type something..."
    />
  );
}
```

### 이벤트 전파 방지

```tsx
function EventExample() {
  const handleDivClick = () => {
    console.log('Div clicked');
  };

  const handleButtonClick = (e: React.MouseEvent) => {
    e.stopPropagation();  // 부모로 전파 방지
    console.log('Button clicked');
  };

  return (
    <div onClick={handleDivClick}>
      <button onClick={handleButtonClick}>
        Click me
      </button>
    </div>
  );
}
```

### 폼 제출

```tsx
function LoginForm() {
  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();  // 페이지 새로고침 방지

    const formData = new FormData(e.currentTarget);
    const email = formData.get('email');
    const password = formData.get('password');

    console.log('Login:', email, password);
  };

  return (
    <form onSubmit={handleSubmit}>
      <input name="email" type="email" placeholder="Email" />
      <input name="password" type="password" placeholder="Password" />
      <button type="submit">Login</button>
    </form>
  );
}
```

---

## ✍️ 실습 과제

### 과제 1: 사용자 카드 컴포넌트 (기초)

다음 요구사항을 만족하는 `UserCard` 컴포넌트를 작성하세요:

```tsx
interface User {
  name: string;
  email: string;
  age: number;
  isActive: boolean;
}

// 요구사항:
// 1. name, email, age를 표시
// 2. isActive가 true면 "Active" 배지, false면 "Inactive" 배지
// 3. age가 18 이상이면 "Adult", 미만이면 "Minor" 표시
```

<details>
<summary>정답 보기</summary>

```tsx
function UserCard({ name, email, age, isActive }: User) {
  return (
    <div className="user-card">
      <h3>{name}</h3>
      <p>{email}</p>
      <p>Age: {age} ({age >= 18 ? 'Adult' : 'Minor'})</p>
      {isActive ? (
        <span className="badge-success">Active</span>
      ) : (
        <span className="badge-secondary">Inactive</span>
      )}
    </div>
  );
}
```
</details>

### 과제 2: 쇼핑 카트 리스트 (중급)

장바구니 아이템을 렌더링하는 컴포넌트를 작성하세요:

```tsx
interface CartItem {
  id: number;
  productName: string;
  price: number;
  quantity: number;
}

const cartItems: CartItem[] = [
  { id: 1, productName: 'MacBook', price: 2399, quantity: 1 },
  { id: 2, productName: 'Mouse', price: 49, quantity: 2 }
];

// 요구사항:
// 1. 각 아이템의 이름, 가격, 수량 표시
// 2. 각 아이템의 소계 (price * quantity) 계산
// 3. 전체 합계 계산하여 하단에 표시
// 4. 장바구니가 비어있으면 "Your cart is empty" 메시지
```

<details>
<summary>정답 보기</summary>

```tsx
function ShoppingCart({ items }: { items: CartItem[] }) {
  if (items.length === 0) {
    return <p>Your cart is empty</p>;
  }

  const total = items.reduce((sum, item) =>
    sum + (item.price * item.quantity), 0
  );

  return (
    <div className="shopping-cart">
      <h2>Shopping Cart</h2>
      <ul>
        {items.map(item => (
          <li key={item.id}>
            <span>{item.productName}</span>
            <span>${item.price} x {item.quantity}</span>
            <span>= ${item.price * item.quantity}</span>
          </li>
        ))}
      </ul>
      <div className="cart-total">
        <strong>Total: ${total}</strong>
      </div>
    </div>
  );
}
```
</details>

### 과제 3: 검색 필터 (고급)

검색어를 입력하면 실시간으로 필터링되는 상품 리스트를 만드세요:

```tsx
const products = [
  { id: 1, name: 'MacBook Pro', category: 'Laptop' },
  { id: 2, name: 'iPhone 15', category: 'Phone' },
  { id: 3, name: 'iPad Air', category: 'Tablet' },
  { id: 4, name: 'MacBook Air', category: 'Laptop' }
];

// 요구사항:
// 1. 검색 input 추가
// 2. 검색어에 매칭되는 상품만 표시
// 3. 대소문자 구분 없이 검색
// 4. 검색 결과가 없으면 "No results found" 표시
```

<details>
<summary>힌트</summary>

- `useState` Hook 사용 (다음 챕터에서 자세히 배움)
- `filter()` 메서드로 배열 필터링
- `toLowerCase()`로 대소문자 변환
- `includes()`로 부분 문자열 검색

</details>

---

## 🎯 체크리스트

학습을 마쳤다면 체크해보세요:

- [ ] JSX 문법 규칙을 이해한다
- [ ] 함수형 컴포넌트를 작성할 수 있다
- [ ] Props를 정의하고 전달할 수 있다
- [ ] TypeScript 인터페이스로 Props 타입을 지정할 수 있다
- [ ] 조건부 렌더링을 다양한 방법으로 구현할 수 있다
- [ ] map으로 리스트를 렌더링하고 key를 올바르게 사용한다
- [ ] 이벤트 핸들러를 작성할 수 있다

---

**이전**: [← 프로젝트 구조](./01-project-structure.md)
**다음**: [Hooks 마스터하기 →](./03-hooks.md)
