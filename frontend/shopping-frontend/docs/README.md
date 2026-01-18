# Shopping Frontend

React 기반 쇼핑 마이크로 프론트엔드입니다.

## 개요

Module Federation Remote 모듈로, Portal Shell에 통합되거나 독립 실행됩니다.

## 포트

- 개발 서버: `30002`

## 주요 기능

| 기능 | 설명 |
|------|------|
| 상품 목록 | 카테고리, 검색, 필터링 |
| 상품 상세 | 이미지, 설명, 장바구니 |
| 장바구니 | 수량 조정, 삭제 |
| 주문 | 체크아웃, 주문 내역 |
| Admin | 상품 관리 (ROLE_ADMIN) |

## 기술 스택

- **Framework**: React 18
- **Build**: Vite + Module Federation
- **State**: Zustand
- **Router**: React Router (Memory Router for Embedded)
- **Styling**: SCSS + Tailwind

## 프로젝트 구조

```
shopping-frontend/
├── src/
│   ├── pages/
│   │   ├── ProductListPage.tsx
│   │   ├── ProductDetailPage.tsx
│   │   ├── CartPage.tsx
│   │   ├── CheckoutPage.tsx
│   │   ├── OrderListPage.tsx
│   │   ├── OrderDetailPage.tsx
│   │   ├── admin/
│   │   │   ├── AdminProductListPage.tsx
│   │   │   └── AdminProductFormPage.tsx
│   │   └── error/
│   │       └── ForbiddenPage.tsx
│   ├── components/
│   │   ├── ProductCard.tsx
│   │   ├── CartItem.tsx
│   │   ├── common/
│   │   │   ├── Button.tsx
│   │   │   ├── Pagination.tsx
│   │   │   └── ConfirmModal.tsx
│   │   ├── form/
│   │   │   ├── Input.tsx
│   │   │   └── TextArea.tsx
│   │   ├── guards/
│   │   │   ├── RequireAuth.tsx
│   │   │   └── RequireRole.tsx
│   │   └── layout/
│   │       └── AdminLayout.tsx
│   ├── api/
│   │   ├── client.ts
│   │   └── endpoints.ts
│   ├── stores/
│   │   ├── cartStore.ts
│   │   └── authStore.ts
│   ├── hooks/
│   │   └── useAdminProducts.ts
│   ├── router/
│   │   └── index.tsx
│   ├── bootstrap.tsx     # Embedded 모드 진입점
│   ├── main.tsx          # Standalone 모드 진입점
│   └── App.tsx
└── vite.config.ts
```

## 실행 모드

### Standalone (독립 실행)

```bash
npm run dev:shopping
```

`main.tsx`를 통해 실행, Browser History 사용

### Embedded (Portal 통합)

Portal Shell에서 `mountShoppingApp()` 호출

`bootstrap.tsx`를 통해 실행, Memory History 사용

## 라우팅

| 경로 | 컴포넌트 | 권한 |
|------|----------|------|
| `/` | ProductListPage | - |
| `/products/:id` | ProductDetailPage | - |
| `/cart` | CartPage | 인증 |
| `/checkout` | CheckoutPage | 인증 |
| `/orders` | OrderListPage | 인증 |
| `/orders/:orderNumber` | OrderDetailPage | 인증 |
| `/admin/products` | AdminProductListPage | ADMIN |
| `/admin/products/new` | AdminProductFormPage | ADMIN |
| `/admin/products/:id/edit` | AdminProductFormPage | ADMIN |

## Module Federation 설정

```typescript
// vite.config.ts
federation({
  name: 'shoppingFrontend',
  filename: 'remoteEntry.js',
  exposes: {
    './bootstrap': './src/bootstrap.tsx'
  },
  shared: ['react', 'react-dom', 'react-router-dom']
})
```

## 환경 변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `VITE_API_BASE_URL` | API Gateway URL | http://localhost:8080 |

## 관련 문서

- [ARCHITECTURE.md](./ARCHITECTURE.md) - 아키텍처 상세
