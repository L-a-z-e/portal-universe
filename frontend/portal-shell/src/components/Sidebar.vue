<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../store/auth';
import MaterialIcon from './MaterialIcon.vue';

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
    icon: 'home',
    exact: true,
  },
  {
    name: 'Blog',
    path: '/blog',
    icon: 'article',
    children: [
      { name: 'Posts', path: '/blog' },
      { name: 'Series', path: '/blog/my?tab=series' },
      { name: 'Write', path: '/blog/write' },
    ],
  },
  {
    name: 'Shopping',
    path: '/shopping',
    icon: 'shopping_cart',
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
    icon: 'smart_toy',
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
  icon: 'storefront',
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
  icon: 'admin_panel_settings',
  children: [
    { name: 'Dashboard', path: '/admin' },
    { name: 'Users', path: '/admin/users' },
    { name: 'Roles', path: '/admin/roles' },
    { name: 'Memberships', path: '/admin/memberships' },
  ],
};

const navItems = computed(() => {
  const items = [...baseNavItems];
  if (authStore.isAuthenticated) {
    // Insert Dashboard after Home
    items.splice(1, 0, {
      name: 'Dashboard',
      path: '/dashboard',
      icon: 'dashboard',
      exact: true,
    });
  }
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

  <!-- Mobile Header Bar -->
  <div
    v-if="isMobile"
    class="fixed top-0 left-0 right-0 h-14 bg-bg-card/95 backdrop-blur-md border-b border-border-default z-50 flex items-center px-4"
  >
    <button
      @click="toggleMobile"
      class="p-2 rounded-lg hover:bg-bg-elevated transition-colors"
    >
      <MaterialIcon name="menu" :size="24" class="text-text-body" />
    </button>
    <router-link to="/" class="ml-3 flex items-center gap-2">
      <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-nightfall-300 to-nightfall-500 flex items-center justify-center">
        <MaterialIcon name="grid_view" :size="18" class="text-white" :filled="true" />
      </div>
      <span class="font-semibold text-text-heading">Portal Universe</span>
    </router-link>
  </div>

  <!-- Sidebar -->
  <aside
    :class="[
      'fixed top-0 left-0 h-full bg-bg-sidebar border-r border-border-default z-50 transition-all duration-300 flex flex-col',
      isCollapsed ? 'w-16' : 'w-72',
      isMobileOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
    ]"
  >
    <!-- Logo Section -->
    <div class="h-14 flex items-center px-4 border-b border-border-default shrink-0">
      <router-link to="/" class="flex items-center gap-3 overflow-hidden">
        <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-nightfall-300 to-nightfall-500 flex items-center justify-center shrink-0">
          <MaterialIcon name="grid_view" :size="18" class="text-white" :filled="true" />
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
    <nav class="py-4 px-3 overflow-y-auto flex-1">
      <div class="space-y-1">
        <template v-for="item in navItems" :key="item.path">
          <!-- Main nav item -->
          <button
            @click="navigate(item.path)"
            :class="[
              'flex items-center gap-3 w-full rounded-lg text-sm font-medium transition-all duration-100 cursor-pointer select-none',
              isCollapsed ? 'px-0 py-2 justify-center' : 'px-3 py-2',
              isActive(item.path, item.exact)
                ? 'bg-brand-primary/10 text-brand-primary border border-brand-primary/20'
                : 'text-text-meta border border-transparent hover:text-text-heading hover:bg-white/5 light:hover:bg-black/5'
            ]"
            :title="isCollapsed ? item.name : undefined"
          >
            <MaterialIcon :name="item.icon" :size="20" :filled="isActive(item.path, item.exact)" />
            <span v-if="!isCollapsed" class="whitespace-nowrap">{{ item.name }}</span>
          </button>

          <!-- Sub items (only when expanded and parent is active) -->
          <div
            v-if="!isCollapsed && item.children && isActive(item.path)"
            class="ml-10 space-y-0.5 mt-1"
          >
            <button
              v-for="child in item.children"
              :key="child.path"
              @click="navigate(child.path)"
              :class="[
                'w-full text-left px-3 py-1.5 rounded-md text-sm transition-colors',
                route.path === child.path
                  ? 'text-brand-primary font-medium'
                  : 'text-text-meta hover:text-text-body'
              ]"
            >
              {{ child.name }}
            </button>
          </div>
        </template>
      </div>

      <!-- Divider -->
      <div class="border-t border-border-default mx-0 my-4" />

      <!-- Bottom nav items -->
      <div class="space-y-1">
        <!-- Status -->
        <button
          @click="navigate('/status')"
          :class="[
            'flex items-center gap-3 w-full rounded-lg text-sm font-medium transition-all duration-100 cursor-pointer',
            isCollapsed ? 'px-0 py-2 justify-center' : 'px-3 py-2',
            isActive('/status', true)
              ? 'bg-brand-primary/10 text-brand-primary border border-brand-primary/20'
              : 'text-text-meta border border-transparent hover:text-text-heading hover:bg-white/5 light:hover:bg-black/5'
          ]"
          :title="isCollapsed ? 'Status' : undefined"
        >
          <MaterialIcon name="bar_chart" :size="20" :filled="isActive('/status', true)" />
          <span v-if="!isCollapsed">Status</span>
        </button>

        <!-- Settings -->
        <button
          @click="navigate('/settings')"
          :class="[
            'flex items-center gap-3 w-full rounded-lg text-sm font-medium transition-all duration-100 cursor-pointer',
            isCollapsed ? 'px-0 py-2 justify-center' : 'px-3 py-2',
            isActive('/settings', true)
              ? 'bg-brand-primary/10 text-brand-primary border border-brand-primary/20'
              : 'text-text-meta border border-transparent hover:text-text-heading hover:bg-white/5 light:hover:bg-black/5'
          ]"
          :title="isCollapsed ? 'Settings' : undefined"
        >
          <MaterialIcon name="settings" :size="20" :filled="isActive('/settings', true)" />
          <span v-if="!isCollapsed">Settings</span>
        </button>
      </div>
    </nav>

    <!-- Bottom: Collapse Toggle -->
    <div class="border-t border-border-default p-3 shrink-0">
      <button
        @click="toggleSidebar"
        :class="[
          'hidden lg:flex items-center gap-3 w-full rounded-lg text-sm text-text-meta hover:text-text-heading hover:bg-white/5 light:hover:bg-black/5 transition-colors cursor-pointer',
          isCollapsed ? 'px-0 py-2 justify-center' : 'px-3 py-2',
        ]"
      >
        <MaterialIcon
          :name="isCollapsed ? 'chevron_right' : 'chevron_left'"
          :size="20"
        />
        <span v-if="!isCollapsed" class="text-sm">Collapse</span>
      </button>
    </div>
  </aside>
</template>
