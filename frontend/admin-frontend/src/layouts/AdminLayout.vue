<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();

const navItems = [
  { to: '/', label: 'Dashboard', exact: true },
  { to: '/users', label: 'Users' },
  { to: '/roles', label: 'Roles' },
  { to: '/memberships', label: 'Memberships' },
  { to: '/seller-approvals', label: 'Seller Approvals' },
  { to: '/audit-log', label: 'Audit Log' },
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
</script>

<template>
  <div class="flex min-h-screen">
    <!-- Admin Sidebar (only in standalone mode; embedded uses portal-shell sidebar) -->
    <aside
      v-if="!isEmbedded"
      class="w-sidebar bg-bg-card border-r border-border-default flex flex-col shrink-0"
    >
      <div class="p-4 text-lg font-bold text-text-heading border-b border-border-default">
        Portal Admin
      </div>
      <nav class="flex-1 p-2 space-y-1">
        <button
          v-for="item in navItems"
          :key="item.to"
          @click="navigate(item.to)"
          :class="[
            'block w-full text-left px-3 py-2 rounded text-sm transition-colors',
            isActive(item.to, item.exact)
              ? 'bg-brand-primary/10 text-brand-primary font-medium'
              : 'text-text-body hover:bg-bg-elevated'
          ]"
        >
          {{ item.label }}
        </button>
      </nav>
    </aside>

    <!-- Main Content -->
    <main class="flex-1 p-6 overflow-auto">
      <router-view />
    </main>
  </div>
</template>
