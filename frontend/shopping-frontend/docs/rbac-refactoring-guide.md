# RBAC 리팩토링 구현 가이드 - Shopping Frontend (React)

## 관련 ADR
- [ADR-011: 계층적 RBAC + 멤버십 기반 인증/인가 시스템](../../../docs/adr/ADR-011-hierarchical-rbac-membership-system.md)

## 현재 상태
- Zustand authStore: `User.role` 단일 문자열 ('guest' | 'user' | 'admin')
- RequireAuth 컴포넌트: 인증 여부 확인, 미인증 시 리다이렉트
- RequireRole 컴포넌트: 역할 정규화(`normalizeRole`) 후 단일 역할 비교
- AdminWrapper: RequireAuth + RequireRole('admin')로 Admin 라우트 보호
- Portal Shell에서 `authAdapter`를 통해 인증 상태 동기화 (embedded mode)
- Standalone 모드 지원 (독립 실행 시 로컬 인증)

## 변경 목표
- User 타입: 단일 role → roles 배열 + memberships Map
- RequireRole 확장: 복수 역할 검증 (SELLER, SHOPPING_ADMIN 등)
- Seller 전용 라우트 및 UI 추가
- Membership 기반 UI 분기 (PREMIUM 전용 기능 등)
- Portal Shell authAdapter 변경 반영

---

## 구현 단계

### Phase 4: User 타입 확장

```typescript
// types/auth.ts 또는 stores/authStore.ts 수정

interface User {
  id: string
  email: string
  name: string
  roles: string[]                          // 변경: string → string[]
  memberships: Record<string, string>      // 신규: { "shopping": "PREMIUM" }
  avatar?: string
}

// 하위 호환: role getter
const getUserPrimaryRole = (user: User): string => {
  if (user.roles.includes('ROLE_SUPER_ADMIN')) return 'admin'
  if (user.roles.includes('ROLE_SHOPPING_ADMIN')) return 'admin'
  if (user.roles.includes('ROLE_SELLER')) return 'seller'
  if (user.roles.some(r => r.startsWith('ROLE_'))) return 'user'
  return 'guest'
}
```

### Phase 4: AuthStore 확장

```typescript
// stores/authStore.ts 수정

interface AuthState {
  user: User | null
  isAuthenticated: boolean
  accessToken: string | null
  loading: boolean
  error: string | null

  // 기존
  setUser: (user: User | null) => void
  setAccessToken: (token: string | null) => void
  logout: () => void
  syncFromPortal: () => Promise<void>

  // 신규
  hasRole: (role: string) => boolean
  hasAnyRole: (...roles: string[]) => boolean
  getMembershipTier: (service: string) => string
  isSeller: () => boolean
  isShoppingAdmin: () => boolean
}

export const useAuthStore = create<AuthState>((set, get) => ({
  // ... 기존 상태 ...

  hasRole: (role: string) => {
    const user = get().user
    if (!user) return false
    const normalized = role.toUpperCase().startsWith('ROLE_') ? role.toUpperCase() : `ROLE_${role.toUpperCase()}`
    return user.roles.includes(normalized)
  },

  hasAnyRole: (...roles: string[]) => {
    return roles.some(role => get().hasRole(role))
  },

  getMembershipTier: (service: string) => {
    return get().user?.memberships?.[service] || 'FREE'
  },

  isSeller: () => {
    return get().hasAnyRole('ROLE_SELLER', 'ROLE_SHOPPING_ADMIN', 'ROLE_SUPER_ADMIN')
  },

  isShoppingAdmin: () => {
    return get().hasAnyRole('ROLE_SHOPPING_ADMIN', 'ROLE_SUPER_ADMIN')
  },
}))
```

### Phase 4: Portal Sync 로직 변경

```typescript
// stores/authStore.ts - syncFromPortal 수정

syncFromPortal: async () => {
  // ... 기존 동기화 로직 ...

  if (authState.isAuthenticated && authState.user) {
    const roles: string[] = []
    // Portal authAdapter에서 역할 정보 가져오기
    if (authAdapter.hasAnyRole('ROLE_SUPER_ADMIN')) roles.push('ROLE_SUPER_ADMIN')
    if (authAdapter.hasAnyRole('ROLE_SHOPPING_ADMIN')) roles.push('ROLE_SHOPPING_ADMIN')
    if (authAdapter.hasAnyRole('ROLE_SELLER')) roles.push('ROLE_SELLER')
    if (authAdapter.hasAnyRole('ROLE_USER')) roles.push('ROLE_USER')
    // 또는 Portal에서 roles 배열을 직접 노출하는 경우 그대로 사용

    const memberships = authAdapter.getState().memberships || {}

    const mappedUser: User = {
      id: '',
      email: authState.user.email || '',
      name: authState.user.name || authState.user.nickname || authState.displayName,
      roles: roles.length > 0 ? roles : ['ROLE_USER'],
      memberships,
      avatar: authState.user.picture
    }
    set({ user: mappedUser, isAuthenticated: true })
  }
}
```

### Phase 4: RequireRole 컴포넌트 수정

```tsx
// components/guards/RequireRole.tsx 수정

interface RequireRoleProps {
  roles: string[]
  children: React.ReactNode
  redirectTo?: string
  mode?: 'any' | 'all'  // 신규: any = 하나만 매칭, all = 모두 매칭
}

const RequireRole: React.FC<RequireRoleProps> = ({
  roles,
  children,
  redirectTo = '/403',
  mode = 'any'
}) => {
  const { user, hasRole, hasAnyRole } = useAuthStore()

  const hasAccess = mode === 'any'
    ? hasAnyRole(...roles)
    : roles.every(role => hasRole(role))

  if (!hasAccess) {
    return <Navigate to={redirectTo} replace />
  }

  return <>{children}</>
}
```

### Phase 4: Seller 라우트 추가

```tsx
// router/index.tsx 수정

// Seller Wrapper
const SellerWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <Suspense fallback={<PageLoader />}>
    <RequireAuth>
      <RequireRole roles={['seller', 'shopping_admin', 'super_admin']}>
        {children}
      </RequireRole>
    </RequireAuth>
  </Suspense>
)

// 라우트 정의에 추가
{
  path: 'seller',
  element: (
    <SellerWrapper>
      <Suspense fallback={<PageLoader />}>
        <SellerLayout />
      </Suspense>
    </SellerWrapper>
  ),
  children: [
    { index: true, element: <SellerDashboard /> },
    { path: 'products', element: <SellerProducts /> },
    { path: 'products/new', element: <SellerProductCreate /> },
    { path: 'products/:id/edit', element: <SellerProductEdit /> },
    { path: 'orders', element: <SellerOrders /> },
    { path: 'analytics', element: <SellerAnalytics /> },
  ]
}
```

### Phase 4: AdminWrapper 수정

```tsx
// router/index.tsx 수정

const AdminWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <Suspense fallback={<PageLoader />}>
    <RequireAuth>
      <RequireRole roles={['shopping_admin', 'super_admin']}>  {/* 변경: admin → shopping_admin */}
        {children}
      </RequireRole>
    </RequireAuth>
  </Suspense>
)
```

### Phase 4: Membership 기반 UI 분기

```tsx
// components/guards/RequireMembership.tsx (신규)

interface RequireMembershipProps {
  service: string
  minTier: string
  children: React.ReactNode
  fallback?: React.ReactNode
}

const TIER_ORDER = ['FREE', 'BASIC', 'PREMIUM', 'VIP']

const RequireMembership: React.FC<RequireMembershipProps> = ({
  service,
  minTier,
  children,
  fallback
}) => {
  const { getMembershipTier } = useAuthStore()
  const currentTier = getMembershipTier(service)
  const hasAccess = TIER_ORDER.indexOf(currentTier) >= TIER_ORDER.indexOf(minTier)

  if (!hasAccess) {
    return fallback ? <>{fallback}</> : <MembershipUpgradePrompt service={service} requiredTier={minTier} />
  }

  return <>{children}</>
}
```

#### 사용 예시
```tsx
// 타임딜 조기 접근 (PREMIUM 이상)
<RequireMembership service="shopping" minTier="PREMIUM">
  <EarlyAccessTimeDealSection />
</RequireMembership>

// VIP 전용 할인
<RequireMembership
  service="shopping"
  minTier="VIP"
  fallback={<VIPUpgradePromotion />}
>
  <ExclusiveVIPDiscounts />
</RequireMembership>
```

---

## 영향받는 파일

### 수정 필요
```
src/stores/authStore.ts
  - User 타입: role → roles[], memberships 추가
  - hasRole, hasAnyRole, getMembershipTier 메서드 추가
  - syncFromPortal 로직 변경 (복수 역할 + 멤버십 동기화)
  - isSeller, isShoppingAdmin computed 추가

src/components/guards/RequireRole.tsx
  - 복수 역할 검증, mode(any/all) 옵션 추가
  - normalizeRole 확장

src/router/index.tsx
  - AdminWrapper: roles 변경 (admin → shopping_admin, super_admin)
  - SellerWrapper 추가
  - Seller 라우트 추가

src/types/federation.d.ts
  - Portal authAdapter 타입 확장 (hasAnyRole, getMembershipTier, memberships)
```

### 신규 생성
```
src/components/guards/RequireMembership.tsx
  - 멤버십 티어 기반 UI 분기

src/pages/seller/SellerLayout.tsx
src/pages/seller/SellerDashboard.tsx
src/pages/seller/SellerProducts.tsx
src/pages/seller/SellerProductCreate.tsx
src/pages/seller/SellerProductEdit.tsx
src/pages/seller/SellerOrders.tsx
src/pages/seller/SellerAnalytics.tsx

src/pages/error/ForbiddenPage.tsx - 403 에러 페이지

src/components/common/MembershipUpgradePrompt.tsx
  - 멤버십 업그레이드 유도 컴포넌트
```

---

## 테스트 체크리스트

### 단위 테스트
- [ ] hasRole: ROLE_ 접두사 정규화 정상 동작
- [ ] hasAnyRole: 복수 역할 중 하나 매칭
- [ ] getMembershipTier: 서비스별 티어 반환, 미등록 시 FREE
- [ ] RequireRole: roles=['seller'] → SELLER 유저 통과, USER 차단
- [ ] RequireRole: mode='all' → 모든 역할 보유 시만 통과
- [ ] RequireMembership: 티어 순서 비교 정상 동작

### 통합 테스트
- [ ] Portal Sync: embedded 모드에서 roles/memberships 정상 동기화
- [ ] AdminWrapper: SHOPPING_ADMIN 접근 성공, USER 차단
- [ ] SellerWrapper: SELLER 접근 성공, USER 차단
- [ ] Seller 페이지 전체 라우트 정상 렌더링

### 하위 호환 테스트
- [ ] 기존 Admin 페이지 정상 동작 (ROLE_ADMIN → ROLE_SUPER_ADMIN 전환 후)
- [ ] Standalone 모드 정상 동작
- [ ] Embedded 모드 Portal Shell 연동 정상 동작
