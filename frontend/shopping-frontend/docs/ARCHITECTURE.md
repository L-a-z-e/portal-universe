# Shopping Frontend 아키텍처

Shopping Frontend의 설계 철학, 계층 구조, 데이터 흐름을 정의합니다.

## 아키텍처 개요

```
┌─────────────────────────────────────────────────────────────┐
│                     Portal Shell (Host)                      │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────────┐ │
│  │ themeStore   │  │ authStore    │  │ moduleLoader       │ │
│  └──────────────┘  └──────────────┘  └────────────────────┘ │
└──────────┬──────────────────────┬──────────────────┬─────────┘
           │ Props (theme, auth)  │ Shared Stores    │ Load MF
           │                      │                  │
┌──────────▼──────────────────────▼──────────────────▼─────────┐
│              Shopping Frontend (Remote)                       │
├───────────────────────────────────────────────────────────────┤
│ App Component                                                  │
│  ├─ Dual Mode (Embedded/Standalone)                          │
│  ├─ Props Sync (theme, locale, userRole)                     │
│  ├─ Store Sync (Portal Shell stores)                         │
│  └─ ShoppingRouter                                           │
├───────────────────────────────────────────────────────────────┤
│ Layout Layer                                                   │
│  ├─ AdminLayout                                              │
│  ├─ Guards (RequireAuth, RequireRole)                        │
│  └─ Navigation Sync                                          │
├───────────────────────────────────────────────────────────────┤
│ Page Layer                                                     │
│  ├─ ProductListPage, ProductDetailPage                       │
│  ├─ CartPage, CheckoutPage                                   │
│  ├─ OrderListPage, OrderDetailPage                           │
│  └─ Admin Pages                                              │
├───────────────────────────────────────────────────────────────┤
│ Service Layer                                                  │
│  ├─ Zustand Stores (auth, cart, product, order)             │
│  ├─ Custom Hooks (useAuth, useApi, useCart)                 │
│  └─ Type Definitions                                         │
├───────────────────────────────────────────────────────────────┤
│ API Layer                                                      │
│  ├─ axios Instance                                           │
│  ├─ Request/Response Interceptors                            │
│  └─ API Endpoints                                            │
└───────────────────────────────────────────────────────────────┘
```

## 계층 구조

### 1. Presentation Layer (UI)

**책임**: 사용자 인터페이스 렌더링

**구성요소**:
- **App.tsx**: Props 동기화, 테마 관리
- **Pages**: ProductListPage, CartPage, CheckoutPage 등
- **Components**: 
  - layout/ (AdminLayout, Header, Footer)
  - common/ (Button, Card, Modal)
  - form/ (Input, Select, FormGroup)
  - guards/ (RequireAuth, RequireRole)

### 2. Routing Layer

**책임**: 클라이언트 사이드 라우팅

**특징**:
- **Dual Mode**: MemoryRouter(Embedded) vs BrowserRouter(Standalone)
- **Code Splitting**: lazy() + Suspense
- **Parent 통신**: onNavigate 콜백

### 3. Service Layer

**Zustand Stores**:
- authStore: 인증 상태
- cartStore: 장바구니
- productStore: 상품 캐시
- orderStore: 주문 관리

**Custom Hooks**:
- useAuth(): 인증
- useApi(): API 호출
- useCart(): 장바구니
- useProduct(): 상품

### 4. API Layer

**구성**:
- apiClient: axios 인스턴스
- Interceptors: 토큰 주입, 에러 처리
- endpoints.ts: API 경로 상수

## 데이터 흐름

### Props 동기화
```
Portal Shell → App.tsx → document attributes → CSS 활성화
```

### Store 동기화
```
App.tsx useEffect → import('portal/authStore') → Shopping Store 업데이트
```

### API 호출
```
Component → Hook → axios Interceptor → Backend → Store 업데이트
```

## 상태 관리 전략

| 상황 | 기술 | 예시 |
|------|------|------|
| 전역 상태 | Zustand | 사용자, 장바구니 |
| 로컬 상태 | useState | 폼 입력 |
| URL 상태 | React Router | 현재 페이지 |

## 보안 고려사항

1. **XSS 방지**: dangerousInnerHTML 금지
2. **CSRF 보호**: CSRF 토큰 포함
3. **민감 정보**: 토큰만 localStorage 저장
4. **권한 검증**: RequireRole 가드

## 성능 최적화

1. **Code Splitting**: lazy() + Suspense
2. **이미지 최적화**: loading="lazy"
3. **메모이제이션**: memo(), useMemo()
4. **번들 분석**: vite build --analyze

## 다음 단계

자세한 로드맵은 [ROADMAP.md](./ROADMAP.md)를 참고하세요.
