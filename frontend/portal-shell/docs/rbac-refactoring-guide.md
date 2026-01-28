# RBAC 리팩토링 구현 가이드 - Portal Shell (Host)

## 관련 ADR
- [ADR-011: 계층적 RBAC + 멤버십 기반 인증/인가 시스템](../../../docs/adr/ADR-011-hierarchical-rbac-membership-system.md)

## 현재 상태
- Pinia authStore: `user.authority.roles` 배열로 역할 저장
- `hasRole(role)`: 단일 역할 확인 메서드
- `isAdmin`: `hasRole('ROLE_ADMIN')` computed
- authService: JWT 파싱 시 `payload.roles` → String/Array 양쪽 지원
- storeAdapter: Module Federation으로 authStore를 Remote 앱에 노출
- router: `meta.requiresAuth` 정의되어 있으나 navigation guard 미구현
- Token: Access Token 메모리, Refresh Token localStorage

## 변경 목표
- 복수 Role 지원 강화 (SELLER, BLOG_ADMIN, SHOPPING_ADMIN 등)
- Membership 상태 저장 및 노출
- Navigation Guard 구현 (역할 기반 라우트 보호)
- storeAdapter 확장: memberships, hasAnyRole 노출
- 역할 기반 UI 분기 (메뉴, 네비게이션)

---

## 구현 단계

### Phase 4: Auth Store 확장

#### PortalUser 타입 확장
```typescript
// types/auth.ts 수정
interface UserAuthority {
  roles: string[]
  memberships: Record<string, string>  // NEW: { "shopping": "PREMIUM", "blog": "FREE" }
}

interface PortalUser {
  profile: UserProfile
  authority: UserAuthority
}
```

#### authStore 확장
```typescript
// store/auth.ts 수정

// 기존 hasRole 유지
const hasRole = (role: string): boolean => {
  return user.value?.authority.roles.includes(role) || false
}

// 신규: 복수 역할 중 하나라도 있는지 확인
const hasAnyRole = (...roles: string[]): boolean => {
  return roles.some(role => hasRole(role))
}

// 신규: 서비스별 멤버십 티어 확인
const getMembershipTier = (service: string): string => {
  return user.value?.authority.memberships?.[service] || 'FREE'
}

// 신규: 멤버십이 특정 티어 이상인지 확인
const hasMembershipTier = (service: string, minTier: string): boolean => {
  const tierOrder = ['FREE', 'BASIC', 'PREMIUM', 'VIP']
  const currentTier = getMembershipTier(service)
  return tierOrder.indexOf(currentTier) >= tierOrder.indexOf(minTier)
}

// 기존 isAdmin 변경
const isAdmin = computed(() => hasAnyRole('ROLE_SUPER_ADMIN', 'ROLE_ADMIN'))

// 신규: 서비스별 Admin 확인
const isBlogAdmin = computed(() => hasAnyRole('ROLE_BLOG_ADMIN', 'ROLE_SUPER_ADMIN'))
const isShoppingAdmin = computed(() => hasAnyRole('ROLE_SHOPPING_ADMIN', 'ROLE_SUPER_ADMIN'))
const isSeller = computed(() => hasAnyRole('ROLE_SELLER', 'ROLE_SHOPPING_ADMIN', 'ROLE_SUPER_ADMIN'))
```

### Phase 4: authService JWT 파싱 확장

```typescript
// services/authService.ts 수정

// JWT payload에서 memberships 추출 추가
const extractUserInfo = (payload: JwtPayload): UserInfo => {
  return {
    uuid: payload.sub || '',
    email: payload.sub || payload.email || '',
    username: payload.preferred_username || payload.username,
    name: payload.name,
    nickname: payload.nickname,
    picture: payload.picture,
    roles: Array.isArray(payload.roles) ? payload.roles :
           payload.roles ? [payload.roles] : [],
    scopes: Array.isArray(payload.scope) ? payload.scope :
            payload.scope ? payload.scope.split(' ') : [],
    memberships: payload.memberships || {}  // NEW
  }
}
```

### Phase 4: Navigation Guard 구현

```typescript
// router/index.ts 수정

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // 인증 필요한 라우트
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return next({ name: 'Login', query: { redirect: to.fullPath } })
  }

  // 역할 기반 접근 제어
  if (to.meta.requiredRoles) {
    const roles = to.meta.requiredRoles as string[]
    if (!authStore.hasAnyRole(...roles)) {
      return next({ name: 'Forbidden' })
    }
  }

  next()
})
```

#### 라우트 메타 확장
```typescript
// 라우트 정의 예시
{
  path: '/admin',
  name: 'Admin',
  component: AdminPage,
  meta: {
    title: '관리자',
    requiresAuth: true,
    requiredRoles: ['ROLE_SUPER_ADMIN']
  }
}
```

### Phase 4: Store Adapter 확장

```typescript
// store/storeAdapter.ts 수정

export const authAdapter = {
  getState: (): AuthState => {
    const store = useAuthStore()
    return {
      isAuthenticated: store.isAuthenticated,
      displayName: store.displayName,
      isAdmin: store.isAdmin,
      isBlogAdmin: store.isBlogAdmin,         // NEW
      isShoppingAdmin: store.isShoppingAdmin,  // NEW
      isSeller: store.isSeller,                // NEW
      user: store.user ? {
        email: store.user.profile.email,
        username: store.user.profile.username,
        name: store.user.profile.name,
        nickname: store.user.profile.nickname,
        picture: store.user.profile.picture
      } : null,
      memberships: store.user?.authority.memberships || {}  // NEW
    }
  },
  hasRole: (role: string): boolean => {
    const store = useAuthStore()
    return store.hasRole(role)
  },
  hasAnyRole: (...roles: string[]): boolean => {  // NEW
    const store = useAuthStore()
    return store.hasAnyRole(...roles)
  },
  getMembershipTier: (service: string): string => {  // NEW
    const store = useAuthStore()
    return store.getMembershipTier(service)
  }
}
```

### Phase 4: 역할 기반 UI 분기

#### 네비게이션 메뉴
```vue
<!-- components/AppNavigation.vue -->
<template>
  <nav>
    <!-- 공통 메뉴 -->
    <RouterLink to="/">홈</RouterLink>
    <RouterLink to="/blog">블로그</RouterLink>
    <RouterLink to="/shopping">쇼핑</RouterLink>

    <!-- 인증된 사용자 -->
    <template v-if="authStore.isAuthenticated">
      <RouterLink to="/dashboard">대시보드</RouterLink>
      <RouterLink to="/profile">프로필</RouterLink>
    </template>

    <!-- Seller 메뉴 -->
    <template v-if="authStore.isSeller">
      <RouterLink to="/shopping/seller">판매자 센터</RouterLink>
    </template>

    <!-- Admin 메뉴 -->
    <template v-if="authStore.isAdmin">
      <RouterLink to="/admin">관리자</RouterLink>
    </template>
  </nav>
</template>
```

---

## 영향받는 파일

### 수정 필요
```
src/types/auth.ts (또는 관련 타입 파일)
  - UserAuthority에 memberships 필드 추가
  - PortalUser 타입 확장

src/store/auth.ts
  - hasAnyRole, getMembershipTier, hasMembershipTier 추가
  - isAdmin 변경, isBlogAdmin, isShoppingAdmin, isSeller 추가

src/services/authService.ts
  - JWT payload에서 memberships 파싱 추가

src/store/storeAdapter.ts
  - authAdapter 확장: hasAnyRole, getMembershipTier, memberships 노출
  - AuthState 타입에 isBlogAdmin, isShoppingAdmin, isSeller, memberships 추가

src/router/index.ts
  - beforeEach navigation guard 구현
  - 라우트 meta에 requiredRoles 추가

src/components/AppNavigation.vue (또는 해당 네비게이션 컴포넌트)
  - 역할 기반 메뉴 분기
```

### 신규 생성
```
src/views/ForbiddenPage.vue      - 403 에러 페이지
src/composables/usePermission.ts - 권한 확인 composable (선택)
```

---

## 테스트 체크리스트

### 단위 테스트
- [ ] hasRole: 단일 역할 확인
- [ ] hasAnyRole: 복수 역할 중 하나라도 매칭
- [ ] getMembershipTier: 서비스별 티어 반환
- [ ] hasMembershipTier: 티어 순서 비교
- [ ] JWT v1 (roles: String) 파싱 → 배열 변환
- [ ] JWT v2 (roles: String[]) 파싱 → 배열 유지
- [ ] JWT v2 memberships 파싱

### 통합 테스트
- [ ] Navigation guard: 미인증 → 로그인 리다이렉트
- [ ] Navigation guard: 역할 부족 → 403 페이지
- [ ] storeAdapter: Remote 앱에서 hasAnyRole 호출 정상
- [ ] storeAdapter: Remote 앱에서 memberships 조회 정상

### 하위 호환 테스트
- [ ] JWT v1 토큰으로 기존 기능 정상 동작
- [ ] 기존 isAdmin 동작 유지 (ROLE_ADMIN → ROLE_SUPER_ADMIN 전환 후)
- [ ] Module Federation Remote 앱 (blog, shopping) 정상 동작
