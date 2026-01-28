# RBAC 리팩토링 구현 가이드 - Blog Frontend (Vue)

## 관련 ADR
- [ADR-011: 계층적 RBAC + 멤버십 기반 인증/인가 시스템](../../../docs/adr/ADR-011-hierarchical-rbac-membership-system.md)

## 현재 상태
- Portal Shell의 authStore를 Module Federation으로 import (`portal/stores`)
- `useAuthStore()` → `isAuthenticated`, `user`, `displayName`, `isAdmin`
- 자체 auth store 없음 (Host에 의존)
- router: `meta.requiresAuth` 정의만 존재, navigation guard 미구현
- 두 가지 라우터 모드: embedded (Memory History) / standalone (Web History)
- 인증 체크: 컴포넌트 레벨에서 직접 수행

## 변경 목표
- Portal Shell의 확장된 authStore 활용 (hasAnyRole, getMembershipTier, memberships)
- Navigation Guard 구현 (역할 기반 라우트 보호)
- Blog Admin 라우트 추가 (게시물/댓글 관리)
- Membership 기반 UI 분기 (PREMIUM → 통계, 커스텀 테마)
- Federation 타입 정의 업데이트

---

## 구현 단계

### Phase 4: Federation 타입 정의 확장

```typescript
// types/federation.d.ts 수정

declare module 'portal/stores' {
  import type { ComputedRef } from 'vue'

  export const useAuthStore: () => {
    isAuthenticated: ComputedRef<boolean>
    user: ComputedRef<{
      profile?: {
        sub?: string
        email?: string
        name?: string
        nickname?: string
        picture?: string
      }
      authority?: {
        roles?: string[]
        memberships?: Record<string, string>  // NEW
      }
    } | null>
    displayName: ComputedRef<string>
    isAdmin: ComputedRef<boolean>
    isBlogAdmin: ComputedRef<boolean>             // NEW
    isShoppingAdmin: ComputedRef<boolean>          // NEW
    isSeller: ComputedRef<boolean>                 // NEW

    // Methods
    hasRole: (role: string) => boolean
    hasAnyRole: (...roles: string[]) => boolean    // NEW
    getMembershipTier: (service: string) => string // NEW
  }
}
```

### Phase 4: Navigation Guard 구현

```typescript
// router/index.ts 수정

// embedded mode
const createEmbeddedRouter = () => {
  const router = createRouter({
    history: createMemoryHistory(),
    routes
  })

  router.beforeEach((to, from, next) => {
    try {
      const authStore = useAuthStore()

      if (to.meta.requiresAuth && !authStore.isAuthenticated.value) {
        // embedded 모드에서는 Host의 로그인으로 유도
        window.parent?.postMessage({ type: 'REQUIRE_AUTH', path: to.fullPath }, '*')
        return next(false)
      }

      if (to.meta.requiredRoles) {
        const roles = to.meta.requiredRoles as string[]
        if (!authStore.hasAnyRole(...roles)) {
          return next({ name: 'Forbidden' })
        }
      }

      next()
    } catch {
      // authStore 사용 불가 시 (Module Federation 로드 실패) 통과
      next()
    }
  })

  return router
}

// standalone mode
const createStandaloneRouter = () => {
  const router = createRouter({
    history: createWebHistory('/blog'),
    routes
  })

  router.beforeEach((to, from, next) => {
    // standalone 모드에서는 인증 체크 스킵 (또는 로컬 인증 사용)
    next()
  })

  return router
}
```

### Phase 4: 라우트 확장

```typescript
// router/index.ts - routes 배열에 추가

// Admin 라우트 (신규)
{
  path: '/admin',
  name: 'BlogAdmin',
  component: () => import('../views/admin/AdminLayout.vue'),
  meta: {
    requiresAuth: true,
    requiredRoles: ['ROLE_BLOG_ADMIN', 'ROLE_SUPER_ADMIN']
  },
  children: [
    {
      path: '',
      name: 'AdminDashboard',
      component: () => import('../views/admin/AdminDashboard.vue')
    },
    {
      path: 'posts',
      name: 'AdminPosts',
      component: () => import('../views/admin/AdminPosts.vue')
    },
    {
      path: 'comments',
      name: 'AdminComments',
      component: () => import('../views/admin/AdminComments.vue')
    }
  ]
}

// 에러 페이지
{
  path: '/403',
  name: 'Forbidden',
  component: () => import('../views/ForbiddenPage.vue')
}
```

### Phase 4: 권한 체크 Composable

```typescript
// composables/usePermission.ts (신규)

import { computed } from 'vue'

export function usePermission() {
  let authStore: ReturnType<typeof useAuthStore> | null = null

  try {
    const { useAuthStore } = await import('portal/stores')
    authStore = useAuthStore()
  } catch {
    // Module Federation 로드 실패 시 기본값
  }

  const isAuthenticated = computed(() => authStore?.isAuthenticated.value ?? false)
  const isBlogAdmin = computed(() => authStore?.isBlogAdmin?.value ?? false)
  const isAdmin = computed(() => authStore?.isAdmin?.value ?? false)

  const hasRole = (role: string): boolean => {
    return authStore?.hasRole?.(role) ?? false
  }

  const hasAnyRole = (...roles: string[]): boolean => {
    return authStore?.hasAnyRole?.(...roles) ?? false
  }

  const getMembershipTier = (service: string): string => {
    return authStore?.getMembershipTier?.(service) ?? 'FREE'
  }

  const blogTier = computed(() => getMembershipTier('blog'))

  const isPremium = computed(() => {
    const tierOrder = ['FREE', 'BASIC', 'PREMIUM', 'VIP']
    return tierOrder.indexOf(blogTier.value) >= tierOrder.indexOf('PREMIUM')
  })

  return {
    isAuthenticated,
    isBlogAdmin,
    isAdmin,
    hasRole,
    hasAnyRole,
    getMembershipTier,
    blogTier,
    isPremium
  }
}
```

### Phase 4: Membership 기반 UI 분기

```vue
<!-- views/MyPage.vue 수정 예시 -->
<script setup lang="ts">
import { usePermission } from '@/composables/usePermission'

const { isPremium, blogTier, isBlogAdmin } = usePermission()
</script>

<template>
  <div class="my-page">
    <!-- 기본 기능: 모든 사용자 -->
    <MyPostsList />
    <MySeriesList />

    <!-- PREMIUM 이상: 통계 대시보드 -->
    <template v-if="isPremium">
      <MyAnalyticsDashboard />
    </template>
    <template v-else>
      <MembershipUpgradePrompt
        service="blog"
        required-tier="PREMIUM"
        feature="통계 대시보드"
      />
    </template>

    <!-- Blog Admin: 관리자 도구 -->
    <template v-if="isBlogAdmin">
      <RouterLink to="/admin" class="admin-link">
        관리자 도구 →
      </RouterLink>
    </template>
  </div>
</template>
```

### Phase 4: 소유권 표시 및 Admin 기능

```vue
<!-- views/PostDetailPage.vue 수정 예시 -->
<script setup lang="ts">
import { usePermission } from '@/composables/usePermission'
const { isAuthenticated, isBlogAdmin } = usePermission()

// 본인 게시물인지 또는 Admin인지 확인
const canEdit = computed(() => {
  if (!isAuthenticated.value) return false
  if (isBlogAdmin.value) return true
  return post.value?.authorId === currentUserId.value
})

const canDelete = computed(() => {
  if (!isAuthenticated.value) return false
  if (isBlogAdmin.value) return true
  return post.value?.authorId === currentUserId.value
})
</script>

<template>
  <!-- 수정/삭제 버튼 표시 조건 변경 -->
  <div v-if="canEdit" class="post-actions">
    <button @click="editPost">수정</button>
    <button v-if="canDelete" @click="deletePost">삭제</button>
    <!-- Admin 전용: 게시물 상태 변경 -->
    <button v-if="isBlogAdmin" @click="changePostStatus">상태 변경</button>
  </div>
</template>
```

---

## 영향받는 파일

### 수정 필요
```
src/types/federation.d.ts
  - useAuthStore 타입 확장
  - isBlogAdmin, hasAnyRole, getMembershipTier 추가
  - user.authority.memberships 타입 추가

src/router/index.ts
  - beforeEach navigation guard 구현 (embedded/standalone 각각)
  - Admin 라우트 추가
  - 403 에러 페이지 라우트 추가
  - meta.requiredRoles 지원

src/views/PostDetailPage.vue
  - canEdit/canDelete 로직에 BLOG_ADMIN 바이패스 추가

src/views/PostListPage.vue
  - Admin인 경우 관리자 도구 링크 표시

src/views/MyPage.vue (또는 해당 마이페이지)
  - Membership 기반 기능 분기 UI 추가
```

### 신규 생성
```
src/composables/usePermission.ts
  - 권한 확인 composable (Portal authStore 래핑)

src/views/admin/AdminLayout.vue
src/views/admin/AdminDashboard.vue
src/views/admin/AdminPosts.vue
src/views/admin/AdminComments.vue

src/views/ForbiddenPage.vue
  - 403 에러 페이지

src/components/common/MembershipUpgradePrompt.vue
  - 멤버십 업그레이드 유도 컴포넌트
```

---

## 테스트 체크리스트

### 단위 테스트
- [ ] usePermission: Portal authStore 정상 연동
- [ ] usePermission: Module Federation 실패 시 기본값 반환
- [ ] isPremium: 티어 순서 비교 정상
- [ ] canEdit/canDelete: 본인 게시물 + Admin 바이패스

### 통합 테스트
- [ ] Navigation guard: embedded 모드에서 미인증 시 Host에 메시지 전송
- [ ] Navigation guard: 역할 부족 시 403 페이지
- [ ] Admin 라우트: BLOG_ADMIN 접근 성공, USER 차단
- [ ] Membership UI: PREMIUM → 통계 대시보드 표시, FREE → 업그레이드 유도

### 하위 호환 테스트
- [ ] 기존 게시물 읽기/쓰기 정상 동작
- [ ] embedded 모드 Portal Shell 연동 정상
- [ ] standalone 모드 정상 동작
- [ ] isAdmin 기존 동작 유지
