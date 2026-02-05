# Design: Sidebar Refactoring

> Feature: `sidebar-refactoring`
> Created: 2026-02-03
> Plan Reference: `docs/pdca/01-plan/features/sidebar-refactoring.plan.md`

## 1. 현재 상태 분석 (Playwright 검증)

### 1.1 스크린샷 기반 문제점

| 스크린샷 | 파일 |
|----------|------|
| 현재 상태 | `.playwright-mcp/sidebar-current-state.png` |
| 알림 드롭다운 열림 | `.playwright-mcp/sidebar-notification-dropdown.png` |

### 1.2 확인된 문제점

#### 문제 1: 알림 드롭다운 위치
```
현재:
┌────────────────────┐
│ Sidebar (w-64)     │←─── 드롭다운 대부분 잘림!
│                    │
│  🔔 알림           │ ┌──────────────────────┐
│                    │ │ NotificationDropdown │
│                    │ │ (w-80, right-0)      │
└────────────────────┘ └──────────────────────┘
                       ↑ 화면 밖으로 나감
```
- `absolute right-0`으로 인해 Bell 버튼 기준 오른쪽 확장
- Sidebar 너비(256px) < Dropdown 너비(320px)
- 결과: 드롭다운이 화면 왼쪽 밖으로 잘림

#### 문제 2: 알림 영역 구조
```html
<!-- 현재 (문제) -->
<div>
  <NotificationBell />        <!-- Bell 버튼만 -->
  <span>알림</span>           <!-- 별도 텍스트 (왜?) -->
</div>
<button>테스트유저</button>   <!-- 프로필 별도 행 -->
```
- "알림" 텍스트가 버튼 옆에 별도로 있음
- 프로필과 알림이 다른 행에 분리

#### 문제 3: 정렬 불일치
| 요소 | 현재 클래스 | 문제 |
|------|------------|------|
| Navigation | `justify-start gap-3` | ✅ 기준 |
| 알림 영역 | 조건부 `justify-center/start` | ❌ 불일치 |
| 프로필 | `justify-start gap-2` | ❌ gap 다름 |
| Status/Settings | `justify-start gap-3` | ✅ OK |

## 2. 디자인 방향

### 2.1 핵심 원칙

1. **일관성**: 모든 버튼 `gap-3` 통일
2. **통합**: 프로필 행에 알림 아이콘 배치
3. **가시성**: 알림 드롭다운이 화면 내에서 완전히 표시

### 2.2 목표 레이아웃

```
┌─────────────────────────────────────┐
│ ┌─┐ Portal Universe                 │
│ └─┘                                 │
├─────────────────────────────────────┤
│ 🏠 Home                             │
│ 📝 Blog                             │
│    └ Posts / Series / Write         │
│ 🛒 Shopping                         │
│ 🤖 Prism                            │
│                                     │
│         (spacer)                    │
│                                     │
├─────────────────────────────────────┤
│ ┌──┐ 테스트유저           🔔       │ ← 프로필 + Bell 통합
│ └──┘                                │
│ 📊 Status                           │
│ ⚙️ Settings                         │
│ 🚪 Logout                           │
│ ◀️ Collapse                         │
└─────────────────────────────────────┘
```

### 2.3 알림 드롭다운 위치 수정

```
수정 후:
┌────────────────────┐┌──────────────────────┐
│ Sidebar (w-64)     ││ NotificationDropdown │
│                    ││ (left-full)          │
│  🔔 테스트유저 🔔  ││ ┌─────────────────┐  │
│                    ││ │ 알림 목록       │  │
│                    ││ └─────────────────┘  │
└────────────────────┘└──────────────────────┘
                      ↑ Sidebar 오른쪽에 표시
```

**해결 방법**: `right-0` → `left-full` (Sidebar 바깥 오른쪽으로 확장)

## 3. 상세 설계

### 3.1 Sidebar.vue 수정

#### Before (현재)
```vue
<!-- Bottom Section -->
<div class="border-t border-border-default p-3 space-y-2 shrink-0">
  <!-- Notifications (로그인된 경우만) - 별도 행 -->
  <div v-if="authStore.isAuthenticated" class="flex items-center"
       :class="isCollapsed ? 'justify-center' : 'justify-start'">
    <NotificationBell />
    <span v-if="!isCollapsed" class="ml-2 text-sm text-text-body">알림</span>
  </div>

  <!-- User Section - 다른 행 -->
  <template v-if="authStore.isAuthenticated">
    <Button variant="ghost" @click="navigate('/profile')" ...>
      <div class="w-8 h-8 rounded-full ...">...</div>
      <div class="flex-1 min-w-0 text-left">
        <p>{{ authStore.displayName }}</p>
        <span v-if="authStore.isAdmin">ADMIN</span>
      </div>
    </Button>
  </template>
  ...
</div>
```

#### After (수정)
```vue
<!-- Bottom Section -->
<div class="border-t border-border-default p-3 space-y-2 shrink-0">
  <!-- User Section with Notification Bell (통합) -->
  <template v-if="authStore.isAuthenticated">
    <!-- Expanded: Profile + Bell -->
    <div v-if="!isCollapsed" class="flex items-center gap-2">
      <Button
        variant="ghost"
        @click="navigate('/profile')"
        :class="[
          'flex-1 justify-start gap-3',
          isActive('/profile', true) ? 'bg-brand-primary/10' : 'bg-bg-elevated'
        ]"
      >
        <div class="w-8 h-8 rounded-full bg-brand-primary/20 flex items-center justify-center shrink-0">
          <span class="text-brand-primary font-medium text-sm">
            {{ authStore.displayName?.charAt(0)?.toUpperCase() || 'U' }}
          </span>
        </div>
        <div class="flex-1 min-w-0 text-left">
          <p class="text-sm font-medium text-text-heading truncate">
            {{ authStore.displayName }}
          </p>
          <span v-if="authStore.isAdmin" class="text-xs px-2 py-0.5 bg-status-error text-white rounded-full">
            ADMIN
          </span>
        </div>
      </Button>
      <!-- Notification Bell (우측) -->
      <NotificationBell dropdown-direction="right" />
    </div>

    <!-- Collapsed: Bell only (centered) -->
    <div v-else class="flex justify-center">
      <NotificationBell dropdown-direction="right" />
    </div>
  </template>

  <!-- Login Button (비로그인 시) -->
  <template v-else>
    <Button variant="primary" @click="handleLogin" class="w-full justify-start gap-3">
      <span class="text-lg shrink-0">🔐</span>
      <span v-if="!isCollapsed" class="font-medium">Login</span>
    </Button>
  </template>

  <!-- Status -->
  <Button variant="ghost" @click="navigate('/status')" class="w-full justify-start gap-3" ...>
    ...
  </Button>

  <!-- Settings -->
  ...
</div>
```

### 3.2 NotificationBell.vue 수정

#### Props 추가
```typescript
interface Props {
  dropdownDirection?: 'left' | 'right' | 'up'
}

const props = withDefaults(defineProps<Props>(), {
  dropdownDirection: 'right'
})
```

#### 드롭다운 위치 클래스
```vue
<template>
  <div class="relative notification-bell">
    <button @click.stop="store.toggleDropdown" ...>
      <!-- Bell icon -->
    </button>

    <!-- Dropdown - 방향에 따라 위치 변경 -->
    <NotificationDropdown
      v-if="store.isDropdownOpen"
      :direction="dropdownDirection"
    />
  </div>
</template>
```

### 3.3 NotificationDropdown.vue 수정

#### Props 추가
```typescript
interface Props {
  direction?: 'left' | 'right' | 'up'
}

const props = withDefaults(defineProps<Props>(), {
  direction: 'right'
})
```

#### 위치 클래스 계산
```typescript
const positionClass = computed(() => {
  switch (props.direction) {
    case 'right':
      // Sidebar 오른쪽으로 확장
      return 'left-full ml-2 top-0'
    case 'left':
      // 기존 방식 (오른쪽 정렬)
      return 'right-0 mt-2'
    case 'up':
      // 위쪽으로 확장
      return 'bottom-full mb-2 right-0'
    default:
      return 'left-full ml-2 top-0'
  }
})
```

#### Template 수정
```vue
<template>
  <div
    :class="[
      'notification-dropdown absolute w-80 max-h-[32rem] bg-bg-card rounded-lg shadow-lg border border-border-default z-[60] overflow-hidden',
      positionClass
    ]"
  >
    <!-- 내용 동일 -->
  </div>
</template>
```

## 4. 구현 순서

### Phase 1: NotificationDropdown 위치 수정
1. `NotificationDropdown.vue`에 `direction` prop 추가
2. `positionClass` computed 속성 구현
3. z-index 조정 (`z-50` → `z-[60]`)

### Phase 2: NotificationBell prop 전달
1. `NotificationBell.vue`에 `dropdownDirection` prop 추가
2. `NotificationDropdown`에 direction 전달

### Phase 3: Sidebar.vue 레이아웃 수정
1. 알림 영역 + 프로필 영역 통합
2. "알림" 텍스트 제거
3. Expanded/Collapsed 상태 처리
4. gap 통일 (`gap-3`)

### Phase 4: 검증
1. Playwright로 드롭다운 위치 확인
2. Collapsed 상태 확인
3. 모바일 반응형 확인

## 5. 파일 변경 목록

| 파일 | 변경 유형 | 변경 내용 |
|------|----------|----------|
| `NotificationDropdown.vue` | 수정 | direction prop, positionClass 추가 |
| `NotificationBell.vue` | 수정 | dropdownDirection prop 추가 |
| `Sidebar.vue` | 수정 | 프로필+알림 통합, 정렬 통일 |

## 6. 테스트 시나리오

| # | 시나리오 | 예상 결과 |
|---|----------|----------|
| 1 | 로그인 후 Sidebar 확인 | 프로필 우측에 🔔 아이콘 표시 |
| 2 | 🔔 클릭 | 드롭다운이 Sidebar 오른쪽에 완전히 표시 |
| 3 | Sidebar 접기 | 🔔 아이콘만 중앙에 표시 |
| 4 | 접힌 상태에서 🔔 클릭 | 드롭다운이 정상 표시 |
| 5 | 모바일 화면 | 드롭다운이 화면 내에서 표시 |

## 7. 다음 단계

```bash
/pdca do sidebar-refactoring
```
