<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../store/auth';
import { Button } from '@portal/design-system-vue';
import { NotificationBell } from './notification';

// Screen size detection for mobile header
const isMobile = ref(false);
const LG_BREAKPOINT = 1024;

const updateIsMobile = () => {
  isMobile.value = window.innerWidth < LG_BREAKPOINT;
};

onMounted(() => {
  updateIsMobile();
  window.addEventListener('resize', updateIsMobile);
});

onUnmounted(() => {
  window.removeEventListener('resize', updateIsMobile);
});

// Login/Logout 함수 정의
const handleLogin = () => {
  // App.vue의 글로벌 LoginModal을 사용 (authStore.requestLogin)
  authStore.requestLogin();
};

const handleLogout = async () => {
  await authStore.logout();
};

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

// Sidebar state
const isCollapsed = ref(false);
const isMobileOpen = ref(false);

// Navigation items
const baseNavItems = [
  {
    name: 'Home',
    path: '/',
    icon: '🏠',
    exact: true,
  },
  {
    name: 'Blog',
    path: '/blog',
    icon: '📝',
    children: [
      { name: 'Posts', path: '/blog' },
      { name: 'Series', path: '/blog/my?tab=series' },
      { name: 'Write', path: '/blog/write' },
    ],
  },
  {
    name: 'Shopping',
    path: '/shopping',
    icon: '🛒',
    children: [
      { name: 'Products', path: '/shopping' },
      { name: 'Cart', path: '/shopping/cart' },
      { name: 'Orders', path: '/shopping/orders' },
      { name: 'Coupons', path: '/shopping/coupons' },
      { name: 'Time Deals', path: '/shopping/time-deals' },
    ],
  },
  {
    name: 'Prism',
    path: '/prism',
    icon: '🤖',
    children: [
      { name: 'Boards', path: '/prism' },
      { name: 'Agents', path: '/prism/agents' },
      { name: 'Providers', path: '/prism/providers' },
    ],
  },
];

const sellerNavItem = {
  name: 'Seller',
  path: '/seller',
  icon: '🏪',
  children: [
    { name: 'Dashboard', path: '/seller' },
    { name: 'Products', path: '/seller/products' },
    { name: 'Coupons', path: '/seller/coupons' },
    { name: 'Time Deals', path: '/seller/time-deals' },
    { name: 'Orders', path: '/seller/orders' },
    { name: 'Stock', path: '/seller/stock-movements' },
    { name: 'Queue', path: '/seller/queue' },
  ],
};

const adminNavItem = {
  name: 'Admin',
  path: '/admin',
  icon: '⚙️',
  children: [
    { name: 'Dashboard', path: '/admin' },
    { name: 'Users', path: '/admin/users' },
    { name: 'Roles', path: '/admin/roles' },
    { name: 'Memberships', path: '/admin/memberships' },
  ],
};

const navItems = computed(() => {
  const items = [...baseNavItems];
  if (authStore.isSeller) {
    items.push(sellerNavItem);
  }
  if (authStore.isAdmin) {
    items.push(adminNavItem);
  }
  return items;
});

// Check if route is active
const isActive = (path: string, exact = false) => {
  if (exact) {
    return route.path === path;
  }
  return route.path.startsWith(path);
};

// Toggle sidebar collapse
const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value;
  localStorage.setItem('sidebar-collapsed', String(isCollapsed.value));
  window.dispatchEvent(new CustomEvent('sidebar-toggle', { detail: { collapsed: isCollapsed.value } }));
};

// Toggle mobile menu
const toggleMobile = () => {
  isMobileOpen.value = !isMobileOpen.value;
};

// Close mobile menu on navigation
watch(() => route.path, () => {
  isMobileOpen.value = false;
});

// Load saved state
const savedCollapsed = localStorage.getItem('sidebar-collapsed');
if (savedCollapsed === 'true') {
  isCollapsed.value = true;
}

// Navigate and close mobile menu
const navigate = (path: string) => {
  router.push(path);
  isMobileOpen.value = false;
};
</script>

<template>
  <!-- Mobile Overlay -->
  <div
    v-if="isMobile && isMobileOpen"
    class="fixed inset-0 bg-black/50 z-40"
    @click="toggleMobile"
  />

  <!-- Mobile Header Bar (only shown on mobile) -->
  <div
    v-if="isMobile"
    class="fixed top-0 left-0 right-0 h-14 bg-bg-card/95 backdrop-blur-md border-b border-border-default z-50 flex items-center px-4"
  >
    <button
      @click="toggleMobile"
      class="p-2 rounded-lg hover:bg-bg-elevated transition-colors"
    >
      <svg class="w-6 h-6 text-text-body" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
      </svg>
    </button>
    <router-link to="/" class="ml-3 flex items-center gap-2">
      <div class="w-8 h-8 rounded-lg bg-brand-primary flex items-center justify-center">
        <span class="text-white font-bold text-sm">P</span>
      </div>
      <span class="font-semibold text-text-heading">Portal Universe</span>
    </router-link>
  </div>

  <!-- Sidebar -->
  <aside
    :class="[
      'fixed top-0 left-0 h-full bg-bg-card border-r border-border-default z-50 transition-all duration-300 flex flex-col',
      isCollapsed ? 'w-16' : 'w-64',
      isMobileOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
    ]"
  >
    <!-- Logo Section -->
    <div class="h-14 flex items-center px-4 border-b border-border-default shrink-0">
      <router-link to="/" class="flex items-center gap-3 overflow-hidden">
        <div class="w-8 h-8 rounded-lg bg-brand-primary flex items-center justify-center shrink-0">
          <span class="text-white font-bold text-sm">P</span>
        </div>
        <span
          v-if="!isCollapsed"
          class="font-semibold text-text-heading whitespace-nowrap"
        >
          Portal Universe
        </span>
      </router-link>
    </div>

    <!-- Navigation -->
    <nav class="py-4 px-2 overflow-y-auto">
      <div class="space-y-1">
        <template v-for="item in navItems" :key="item.path">
          <!-- Main nav item -->
          <Button
            variant="ghost"
            @click="navigate(item.path)"
            :class="[
              'w-full justify-start gap-3',
              isActive(item.path, item.exact)
                ? 'bg-brand-primary/10 text-brand-primary'
                : ''
            ]"
          >
            <span class="text-lg shrink-0">{{ item.icon }}</span>
            <span
              v-if="!isCollapsed"
              class="font-medium whitespace-nowrap"
            >
              {{ item.name }}
            </span>
          </Button>

          <!-- Sub items (only when expanded and parent is active) -->
          <div
            v-if="!isCollapsed && item.children && isActive(item.path)"
            class="ml-9 space-y-0.5 mt-1"
          >
            <Button
              v-for="child in item.children"
              :key="child.path"
              variant="ghost"
              size="sm"
              @click="navigate(child.path)"
              :class="[
                'w-full justify-start',
                route.path === child.path
                  ? 'text-brand-primary font-medium'
                  : 'text-text-meta'
              ]"
            >
              {{ child.name }}
            </Button>
          </div>
        </template>
      </div>
    </nav>

    <!-- Spacer to push bottom section down -->
    <div class="flex-1"></div>

    <!-- Bottom Section -->
    <div class="border-t border-border-default p-3 space-y-2 shrink-0">
      <!-- User Section with Notification Bell (통합) -->
      <template v-if="authStore.isAuthenticated">
        <!-- Expanded: Profile + Bell in same row -->
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
              <span v-if="authStore.isAdmin" class="text-xs px-2 py-0.5 bg-status-error text-white rounded-full">ADMIN</span>
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
        <Button
          variant="primary"
          @click="handleLogin"
          class="w-full justify-start gap-3"
        >
          <span class="text-lg shrink-0">🔐</span>
          <span v-if="!isCollapsed" class="font-medium">Login</span>
        </Button>
      </template>

      <!-- Service Status -->
      <Button
        variant="ghost"
        @click="navigate('/status')"
        :class="[
          'w-full justify-start gap-3',
          isActive('/status', true) ? 'bg-brand-primary/10 text-brand-primary' : ''
        ]"
      >
        <span class="text-lg shrink-0">📊</span>
        <span v-if="!isCollapsed" class="font-medium whitespace-nowrap">Status</span>
      </Button>

      <!-- Settings -->
      <Button
        variant="ghost"
        @click="navigate('/settings')"
        :class="[
          'w-full justify-start gap-3',
          isActive('/settings', true) ? 'bg-brand-primary/10 text-brand-primary' : ''
        ]"
      >
        <span class="text-lg shrink-0">⚙️</span>
        <span v-if="!isCollapsed" class="font-medium whitespace-nowrap">Settings</span>
      </Button>

      <!-- Logout (로그인된 경우) -->
      <Button
        v-if="authStore.isAuthenticated"
        variant="ghost"
        @click="handleLogout"
        class="w-full justify-start gap-3 text-status-error hover:bg-status-error/10"
      >
        <span class="text-lg shrink-0">🚪</span>
        <span v-if="!isCollapsed" class="font-medium">Logout</span>
      </Button>

      <!-- Collapse Toggle (Desktop only) -->
      <Button
        variant="ghost"
        @click="toggleSidebar"
        class="hidden lg:flex w-full justify-start gap-3"
      >
        <svg
          :class="['w-5 h-5 transition-transform', isCollapsed ? 'rotate-180' : '']"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 19l-7-7 7-7m8 14l-7-7 7-7" />
        </svg>
        <span v-if="!isCollapsed" class="text-sm">Collapse</span>
      </Button>
    </div>
  </aside>


</template>
