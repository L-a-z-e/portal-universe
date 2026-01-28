<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../store/auth';
import { authService } from '../services/authService';
import LoginModal from './LoginModal.vue';

// Login Modal state
const showLoginModal = ref(false);

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
  // Open Login Modal instead of redirecting to social login
  showLoginModal.value = true;
};

const handleLogout = async () => {
  try {
    await authService.logout();
    const authStore = useAuthStore();
    authStore.setAuthenticated(false);
    authStore.setUser(null);
  } catch (error) {
    console.error('Logout failed:', error);
  }
};

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

// Sidebar state
const isCollapsed = ref(false);
const isMobileOpen = ref(false);

// Navigation items
const navItems = [
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
    ],
  },
];

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
          <button
            @click="navigate(item.path)"
            :class="[
              'w-full flex items-center gap-3 px-3 py-2.5 rounded-lg transition-all text-left',
              isActive(item.path, item.exact)
                ? 'bg-brand-primary/10 text-brand-primary'
                : 'text-text-body hover:bg-bg-elevated hover:text-text-heading'
            ]"
          >
            <span class="text-lg shrink-0">{{ item.icon }}</span>
            <span
              v-if="!isCollapsed"
              class="font-medium whitespace-nowrap"
            >
              {{ item.name }}
            </span>
          </button>

          <!-- Sub items (only when expanded and parent is active) -->
          <div
            v-if="!isCollapsed && item.children && isActive(item.path)"
            class="ml-9 space-y-0.5 mt-1"
          >
            <button
              v-for="child in item.children"
              :key="child.path"
              @click="navigate(child.path)"
              :class="[
                'w-full text-left px-3 py-1.5 rounded-md text-sm transition-colors',
                route.path === child.path
                  ? 'text-brand-primary font-medium'
                  : 'text-text-meta hover:text-text-body hover:bg-bg-elevated'
              ]"
            >
              {{ child.name }}
            </button>
          </div>
        </template>
      </div>
    </nav>

    <!-- Spacer to push bottom section down -->
    <div class="flex-1"></div>

    <!-- Bottom Section -->
    <div class="border-t border-border-default p-3 space-y-2 shrink-0">
      <!-- User Section (최상단) -->
      <template v-if="authStore.isAuthenticated">
        <button
          v-if="!isCollapsed"
          @click="navigate('/profile')"
          :class="[
            'w-full flex items-center gap-2 px-3 py-2 rounded-lg transition-colors',
            isActive('/profile', true)
              ? 'bg-brand-primary/10'
              : 'bg-bg-elevated hover:bg-bg-elevated/80'
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
            <span v-if="authStore.isAdmin" class="text-xs px-2 py-0.5 bg-red-500 text-white rounded-full">ADMIN</span>
          </div>
        </button>
      </template>
      <template v-else>
        <button
          @click="handleLogin"
          :class="[
            'w-full flex items-center gap-3 px-3 py-2 rounded-lg transition-colors',
            'bg-blue-600 text-white hover:bg-blue-700'
          ]"
        >
          <span class="text-lg shrink-0">🔐</span>
          <span v-if="!isCollapsed" class="font-medium">Login</span>
        </button>
      </template>

      <!-- Service Status -->
      <button
        @click="navigate('/status')"
        :class="[
          'w-full flex items-center gap-3 px-3 py-2 rounded-lg transition-colors',
          isActive('/status', true)
            ? 'bg-brand-primary/10 text-brand-primary'
            : 'text-text-body hover:bg-bg-elevated hover:text-text-heading'
        ]"
      >
        <span class="text-lg shrink-0">📊</span>
        <span v-if="!isCollapsed" class="font-medium whitespace-nowrap">Status</span>
      </button>

      <!-- Settings -->
      <button
        @click="navigate('/settings')"
        :class="[
          'w-full flex items-center gap-3 px-3 py-2 rounded-lg transition-colors',
          isActive('/settings', true)
            ? 'bg-brand-primary/10 text-brand-primary'
            : 'text-text-body hover:bg-bg-elevated hover:text-text-heading'
        ]"
      >
        <span class="text-lg shrink-0">⚙️</span>
        <span v-if="!isCollapsed" class="font-medium whitespace-nowrap">Settings</span>
      </button>

      <!-- Logout (로그인된 경우) -->
      <button
        v-if="authStore.isAuthenticated"
        @click="handleLogout"
        :class="[
          'w-full flex items-center gap-3 px-3 py-2 rounded-lg transition-colors',
          'text-red-500 hover:bg-red-500/10'
        ]"
      >
        <span class="text-lg shrink-0">🚪</span>
        <span v-if="!isCollapsed" class="font-medium">Logout</span>
      </button>

      <!-- Collapse Toggle (Desktop only) -->
      <button
        @click="toggleSidebar"
        class="hidden lg:flex w-full items-center gap-3 px-3 py-2 rounded-lg text-text-meta hover:bg-bg-elevated hover:text-text-body transition-colors"
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
      </button>
    </div>
  </aside>

  <!-- Login Modal -->
  <LoginModal v-model="showLoginModal" />
</template>
