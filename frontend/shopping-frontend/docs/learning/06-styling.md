# 🎨 스타일링 (Tailwind CSS)

> Tailwind CSS를 활용한 모던 스타일링을 학습합니다.

**난이도**: ⭐⭐ (기초)
**학습 시간**: 50분

---

## 🎯 학습 목표

이 문서를 마치면 다음을 할 수 있습니다:
- [ ] Tailwind CSS 기본 개념 이해하기
- [ ] Utility 클래스로 스타일링하기
- [ ] 반응형 디자인 구현하기
- [ ] 다크 모드 적용하기
- [ ] 커스텀 컴포넌트 스타일링하기

---

## 1️⃣ Tailwind CSS 소개

### Utility-First CSS

```html
<!-- 전통적인 CSS -->
<style>
  .button {
    background-color: blue;
    color: white;
    padding: 8px 16px;
    border-radius: 4px;
  }
</style>
<button class="button">Click me</button>

<!-- Tailwind CSS -->
<button class="bg-blue-600 text-white px-4 py-2 rounded">
  Click me
</button>
```

### 장점

- ✅ **빠른 개발**: HTML을 떠나지 않고 스타일링
- ✅ **일관성**: 정해진 디자인 시스템
- ✅ **번들 크기**: 사용하지 않는 클래스 자동 제거
- ✅ **반응형**: 쉬운 반응형 디자인

---

## 2️⃣ 기본 유틸리티

### 레이아웃

```tsx
// Flexbox
<div className="flex items-center justify-between">
  <span>Left</span>
  <span>Right</span>
</div>

// Grid
<div className="grid grid-cols-3 gap-4">
  <div>1</div>
  <div>2</div>
  <div>3</div>
</div>

// 중앙 정렬
<div className="flex items-center justify-center h-screen">
  <p>Centered</p>
</div>

// 간격
<div className="space-y-4">  {/* 수직 간격 */}
  <p>Item 1</p>
  <p>Item 2</p>
</div>

<div className="space-x-4">  {/* 수평 간격 */}
  <button>Button 1</button>
  <button>Button 2</button>
</div>
```

### 크기

```tsx
// Width & Height
<div className="w-64 h-32">Fixed size</div>
<div className="w-full h-screen">Full size</div>
<div className="w-1/2 h-1/4">Fractional</div>
<div className="min-w-0 max-w-lg">Min/Max</div>

// Padding & Margin
<div className="p-4">padding: 1rem</div>
<div className="px-4 py-2">padding x/y</div>
<div className="m-auto">margin: auto</div>
<div className="mt-4 mb-2">margin top/bottom</div>

// 크기 단위
// 4 = 1rem = 16px
// 8 = 2rem = 32px
// 12 = 3rem = 48px
```

### 색상

```tsx
// Background
<div className="bg-blue-600">Blue background</div>
<div className="bg-gray-100">Light gray</div>

// Text
<p className="text-red-600">Red text</p>
<p className="text-gray-900">Dark gray</p>

// Border
<div className="border border-gray-300">Border</div>
<div className="border-2 border-blue-500">Thick border</div>

// 색상 범위: 50, 100, 200, ..., 900
// 50: 가장 밝음
// 900: 가장 어두움
```

### 타이포그래피

```tsx
// Font Size
<p className="text-xs">Extra small</p>
<p className="text-sm">Small</p>
<p className="text-base">Base (16px)</p>
<p className="text-lg">Large</p>
<p className="text-xl">Extra large</p>
<p className="text-2xl">2XL</p>
<p className="text-3xl">3XL</p>

// Font Weight
<p className="font-light">Light</p>
<p className="font-normal">Normal</p>
<p className="font-medium">Medium</p>
<p className="font-semibold">Semibold</p>
<p className="font-bold">Bold</p>

// Text Align
<p className="text-left">Left</p>
<p className="text-center">Center</p>
<p className="text-right">Right</p>

// Text Style
<p className="italic">Italic</p>
<p className="underline">Underline</p>
<p className="line-through">Line through</p>
<p className="uppercase">UPPERCASE</p>
<p className="lowercase">lowercase</p>
<p className="capitalize">Capitalize</p>
```

---

## 3️⃣ 실전 컴포넌트

### 버튼

```tsx
// 기본 버튼
<button className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700">
  Primary
</button>

// 아웃라인 버튼
<button className="border border-blue-600 text-blue-600 px-4 py-2 rounded hover:bg-blue-50">
  Secondary
</button>

// 비활성화
<button
  disabled
  className="bg-gray-300 text-gray-500 px-4 py-2 rounded cursor-not-allowed"
>
  Disabled
</button>

// 로딩 버튼
<button className="bg-blue-600 text-white px-4 py-2 rounded flex items-center gap-2">
  <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24">
    {/* Spinner icon */}
  </svg>
  Loading...
</button>
```

### 카드

```tsx
function ProductCard({ product }: { product: Product }) {
  return (
    <div className="bg-white rounded-lg shadow-md overflow-hidden hover:shadow-lg transition-shadow">
      {/* 이미지 */}
      <img
        src={product.image}
        alt={product.name}
        className="w-full h-48 object-cover"
      />

      {/* 내용 */}
      <div className="p-4">
        <h3 className="text-lg font-semibold text-gray-900 mb-2">
          {product.name}
        </h3>
        <p className="text-gray-600 text-sm mb-4">
          {product.description}
        </p>

        {/* 가격 & 버튼 */}
        <div className="flex items-center justify-between">
          <span className="text-2xl font-bold text-blue-600">
            ${product.price}
          </span>
          <button className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700">
            Add to Cart
          </button>
        </div>
      </div>
    </div>
  );
}
```

### 입력 필드

```tsx
function InputExample() {
  return (
    <div className="space-y-4">
      {/* Text Input */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          Email
        </label>
        <input
          type="email"
          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          placeholder="you@example.com"
        />
      </div>

      {/* Textarea */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          Message
        </label>
        <textarea
          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          rows={4}
          placeholder="Enter your message..."
        />
      </div>

      {/* Select */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          Category
        </label>
        <select className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500">
          <option>Select category</option>
          <option>Electronics</option>
          <option>Clothing</option>
        </select>
      </div>

      {/* Checkbox */}
      <div className="flex items-center">
        <input
          type="checkbox"
          id="terms"
          className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
        />
        <label htmlFor="terms" className="ml-2 text-sm text-gray-700">
          I agree to the terms and conditions
        </label>
      </div>
    </div>
  );
}
```

### 네비게이션 바

```tsx
function Navbar() {
  return (
    <nav className="bg-white shadow-md">
      <div className="max-w-7xl mx-auto px-4">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <div className="flex items-center">
            <img src="/logo.svg" alt="Logo" className="h-8 w-8" />
            <span className="ml-2 text-xl font-bold text-gray-900">
              Shopping
            </span>
          </div>

          {/* Links */}
          <div className="hidden md:flex space-x-8">
            <a href="/" className="text-gray-700 hover:text-blue-600">
              Home
            </a>
            <a href="/products" className="text-gray-700 hover:text-blue-600">
              Products
            </a>
            <a href="/about" className="text-gray-700 hover:text-blue-600">
              About
            </a>
          </div>

          {/* Actions */}
          <div className="flex items-center space-x-4">
            <button className="text-gray-700 hover:text-blue-600">
              🔍
            </button>
            <button className="text-gray-700 hover:text-blue-600 relative">
              🛒
              <span className="absolute -top-2 -right-2 bg-red-500 text-white text-xs w-5 h-5 rounded-full flex items-center justify-center">
                3
              </span>
            </button>
          </div>
        </div>
      </div>
    </nav>
  );
}
```

---

## 4️⃣ 반응형 디자인

### Breakpoints

| 프리픽스 | 최소 너비 | CSS |
|----------|----------|-----|
| `sm:` | 640px | `@media (min-width: 640px)` |
| `md:` | 768px | `@media (min-width: 768px)` |
| `lg:` | 1024px | `@media (min-width: 1024px)` |
| `xl:` | 1280px | `@media (min-width: 1280px)` |
| `2xl:` | 1536px | `@media (min-width: 1536px)` |

### 반응형 예제

```tsx
// 모바일: 1열, 태블릿: 2열, 데스크탑: 3열
<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
  <ProductCard />
  <ProductCard />
  <ProductCard />
</div>

// 모바일에서 숨김, 데스크탑에서 표시
<div className="hidden lg:block">
  <Sidebar />
</div>

// 반응형 텍스트 크기
<h1 className="text-2xl md:text-3xl lg:text-4xl">
  Responsive Heading
</h1>

// 반응형 패딩
<div className="p-4 md:p-6 lg:p-8">
  Content
</div>

// 반응형 Flexbox 방향
<div className="flex flex-col md:flex-row">
  <aside className="w-full md:w-1/4">Sidebar</aside>
  <main className="w-full md:w-3/4">Content</main>
</div>
```

### 실전 반응형 레이아웃

```tsx
function ResponsiveLayout() {
  return (
    <div className="min-h-screen bg-gray-50">
      {/* Navbar */}
      <nav className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <span className="text-xl font-bold">Logo</span>

            {/* Mobile menu button */}
            <button className="md:hidden">☰</button>

            {/* Desktop menu */}
            <div className="hidden md:flex space-x-8">
              <a href="/">Home</a>
              <a href="/products">Products</a>
            </div>
          </div>
        </div>
      </nav>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Hero Section */}
        <section className="mb-12">
          <h1 className="text-3xl sm:text-4xl lg:text-5xl font-bold mb-4">
            Welcome to Our Store
          </h1>
          <p className="text-base sm:text-lg text-gray-600">
            Find the best products at great prices
          </p>
        </section>

        {/* Product Grid */}
        <section>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 sm:gap-6">
            {/* Products */}
          </div>
        </section>
      </main>
    </div>
  );
}
```

---

## 5️⃣ 상태와 인터랙션

### Hover, Focus, Active

```tsx
// Hover
<button className="bg-blue-600 hover:bg-blue-700">
  Hover me
</button>

// Focus (키보드 네비게이션)
<input className="border focus:ring-2 focus:ring-blue-500 focus:outline-none" />

// Active (클릭 중)
<button className="bg-blue-600 active:bg-blue-800">
  Press me
</button>

// 조합
<button className="bg-blue-600 hover:bg-blue-700 active:bg-blue-800 focus:ring-2 focus:ring-blue-300">
  Interactive
</button>

// Disabled
<button
  disabled
  className="bg-gray-300 disabled:opacity-50 disabled:cursor-not-allowed"
>
  Disabled
</button>
```

### 조건부 클래스

```tsx
import { useState } from 'react';

function ToggleButton() {
  const [isActive, setIsActive] = useState(false);

  return (
    <button
      onClick={() => setIsActive(!isActive)}
      className={`
        px-4 py-2 rounded
        ${isActive
          ? 'bg-blue-600 text-white'
          : 'bg-gray-200 text-gray-700'
        }
      `}
    >
      {isActive ? 'Active' : 'Inactive'}
    </button>
  );
}

// clsx 라이브러리 사용 (권장)
import clsx from 'clsx';

function Button({ primary, disabled }: { primary: boolean; disabled: boolean }) {
  return (
    <button
      className={clsx(
        'px-4 py-2 rounded',
        primary ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-700',
        disabled && 'opacity-50 cursor-not-allowed'
      )}
    >
      Click me
    </button>
  );
}
```

### 트랜지션과 애니메이션

```tsx
// Transition
<button className="bg-blue-600 hover:bg-blue-700 transition-colors duration-300">
  Smooth color change
</button>

<div className="transform hover:scale-110 transition-transform duration-200">
  Scale on hover
</div>

// 여러 속성 트랜지션
<div className="opacity-0 hover:opacity-100 translate-y-4 hover:translate-y-0 transition-all duration-500">
  Fade in and slide up
</div>

// 애니메이션
<div className="animate-spin">⚙️</div>
<div className="animate-pulse">💓</div>
<div className="animate-bounce">⬆️</div>
```

---

## 6️⃣ 다크 모드

### 설정

```tsx
// tailwind.config.js
module.exports = {
  darkMode: 'class',  // 또는 'media'
  // ...
}
```

### 사용

```tsx
// 라이트/다크 모드 색상
<div className="bg-white dark:bg-gray-900 text-gray-900 dark:text-white">
  Content
</div>

<button className="bg-blue-600 dark:bg-blue-500 text-white">
  Button
</button>

// 다크 모드 토글
import { useState, useEffect } from 'react';

function DarkModeToggle() {
  const [isDark, setIsDark] = useState(false);

  useEffect(() => {
    if (isDark) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [isDark]);

  return (
    <button
      onClick={() => setIsDark(!isDark)}
      className="p-2 rounded bg-gray-200 dark:bg-gray-700"
    >
      {isDark ? '🌙' : '☀️'}
    </button>
  );
}
```

---

## ✍️ 실습 과제

### 과제 1: 프로필 카드 (기초)

다음 디자인의 프로필 카드를 만드세요:

```
┌─────────────────────┐
│   [프로필 이미지]      │
│                     │
│   John Doe          │
│   Software Engineer │
│                     │
│   📧 Email  💼 LinkedIn │
└─────────────────────┘
```

요구사항:
- 카드: 흰색 배경, 그림자, 둥근 모서리
- 이미지: 중앙 정렬, 원형
- 텍스트: 이름은 크고 굵게, 직책은 회색
- 버튼: 호버 효과

### 과제 2: 반응형 헤더 (중급)

모바일과 데스크탑에서 다르게 보이는 헤더를 만드세요:

```
모바일 (<768px):
Logo                [☰]

데스크탑 (≥768px):
Logo    Home  Products  About    [🔍] [🛒]
```

요구사항:
- 모바일: 햄버거 메뉴
- 데스크탑: 전체 네비게이션
- 고정 헤더 (스크롤해도 상단 고정)

### 과제 3: 상품 필터 (고급)

필터가 있는 상품 목록을 만드세요:

```
[전체 ▼] [정렬 ▼]   [검색...]

┌────┐ ┌────┐ ┌────┐
│상품1│ │상품2│ │상품3│
└────┘ └────┘ └────┘
```

요구사항:
- 카테고리 드롭다운
- 정렬 옵션
- 검색 입력
- 반응형 그리드 (1/2/3/4열)
- 호버 시 카드 확대

---

## 🎯 체크리스트

학습을 마쳤다면 체크해보세요:

- [ ] Tailwind의 Utility 클래스를 이해한다
- [ ] Flexbox와 Grid 레이아웃을 만들 수 있다
- [ ] 반응형 디자인을 구현할 수 있다
- [ ] Hover, Focus 등 상태 스타일을 적용할 수 있다
- [ ] 조건부 클래스를 올바르게 사용한다
- [ ] 트랜지션과 애니메이션을 추가할 수 있다
- [ ] 다크 모드를 구현할 수 있다

---

**이전**: [← 라우팅 (React Router)](./05-routing.md)
**다음**: [Module Federation →](./07-module-federation.md)
