<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getPortalAuthState } from '@portal/vue-bridge';

const route = useRoute();
const router = useRouter();

const authState = computed(() => getPortalAuthState());
const userName = computed(() => authState.value.user?.nickname || authState.value.user?.email || 'Admin');

const mainNavItems = [
  { to: '/', label: 'Dashboard', icon: 'dashboard', exact: true },
  { to: '/users', label: 'Users', icon: 'group' },
  { to: '/roles', label: 'Roles', icon: 'shield' },
  { to: '/memberships', label: 'Memberships', icon: 'card_membership' },
  { to: '/seller-approvals', label: 'Approvals', icon: 'approval' },
];

const systemNavItems = [
  { to: '/audit-log', label: 'Audit Log', icon: 'history' },
];

const isActive = (path: string, exact = false) => {
  if (exact) return route.path === path;
  return route.path.startsWith(path);
};

const navigate = (path: string) => {
  router.push(path);
};

const isEmbedded = computed(() => {
  try {
    return (window as unknown as Record<string, unknown>).__POWERED_BY_PORTAL_SHELL__ === true;
  } catch {
    return false;
  }
});

function handleSignOut() {
  if (typeof (window as any).__PORTAL_LOGOUT__ === 'function') {
    (window as any).__PORTAL_LOGOUT__();
  }
}
</script>

<template>
  <div class="flex min-h-screen">
    <!-- Admin Sidebar (only in standalone mode) -->
    <aside
      v-if="!isEmbedded"
      class="w-60 bg-sidebar flex flex-col shrink-0 fixed inset-y-0 left-0 z-30"
    >
      <!-- Logo -->
      <div class="px-5 py-5">
        <span class="text-sidebar-textActive text-sm font-bold tracking-wide uppercase">Portal Admin</span>
      </div>

      <!-- Main Nav -->
      <nav class="flex-1 px-3 space-y-0.5 overflow-y-auto">
        <button
          v-for="item in mainNavItems"
          :key="item.to"
          @click="navigate(item.to)"
          :class="[
            'admin-nav-item w-full text-left',
            isActive(item.to, item.exact) ? 'active' : ''
          ]"
        >
          <span class="material-symbols-outlined nav-icon" style="font-size: 20px;">{{ item.icon }}</span>
          <span>{{ item.label }}</span>
        </button>

        <!-- System Section -->
        <div class="admin-section-label">System</div>

        <button
          v-for="item in systemNavItems"
          :key="item.to"
          @click="navigate(item.to)"
          :class="[
            'admin-nav-item w-full text-left',
            isActive(item.to) ? 'active' : ''
          ]"
        >
          <span class="material-symbols-outlined nav-icon" style="font-size: 20px;">{{ item.icon }}</span>
          <span>{{ item.label }}</span>
        </button>
      </nav>

      <!-- User Profile -->
      <div class="px-4 py-4 border-t border-white/10">
        <div class="flex items-center gap-3">
          <div class="w-8 h-8 rounded-full bg-sidebar-bgActive flex items-center justify-center">
            <span class="material-symbols-outlined text-sidebar-text" style="font-size: 18px;">person</span>
          </div>
          <div class="flex-1 min-w-0">
            <div class="text-sm text-sidebar-textActive truncate">{{ userName }}</div>
          </div>
        </div>
        <button
          @click="handleSignOut"
          class="mt-3 w-full flex items-center gap-2 px-2 py-1.5 rounded text-xs text-sidebar-text hover:text-sidebar-textActive hover:bg-sidebar-bgActive transition-colors"
        >
          <span class="material-symbols-outlined" style="font-size: 16px;">logout</span>
          Sign Out
        </button>
      </div>
    </aside>

    <!-- Main Content -->
    <main
      :class="[
        'flex-1 overflow-auto',
        !isEmbedded ? 'ml-60' : ''
      ]"
    >
      <div class="p-6 max-w-7xl mx-auto">
        <router-view />
      </div>
    </main>
  </div>
</template>
