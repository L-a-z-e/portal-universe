<script setup lang="ts">
import { useThemeStore } from "./store/theme.ts";
import { onMounted, watch, ref, computed } from "vue";
import { useRoute } from "vue-router";
import Sidebar from "./components/Sidebar.vue";

const themeStore = useThemeStore();
const route = useRoute();

// Sidebar collapsed state (synced with Sidebar component via localStorage)
const sidebarCollapsed = ref(localStorage.getItem('sidebar-collapsed') === 'true');

// Watch localStorage changes
const updateSidebarState = () => {
  sidebarCollapsed.value = localStorage.getItem('sidebar-collapsed') === 'true';
};

// Computed class for main content margin
const mainClass = computed(() => ({
  'lg:ml-64': !sidebarCollapsed.value,
  'lg:ml-16': sidebarCollapsed.value,
  'pt-14 lg:pt-0': true, // Mobile header offset
}));

function updateDataTheme() {
  const isDark = document.documentElement.classList.contains('dark');
  document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light');
  console.log(`[Portal-Shell] Theme updated: ${isDark ? 'dark' : 'light'}`);
}

/**
 * data-service 초기화
 * 호스트 앱 경로로 이동할 때 data-service="portal"로 리셋
 */
function resetDataService() {
  if (!route.meta.remoteName) {
    document.documentElement.setAttribute('data-service', 'portal');
    console.log('[Portal-Shell] Route change: Reset data-service="portal"');
    forceReflowToApplyCSSChanges();
  }
}

/**
 * CSS 변수 강제 재계산
 */
function forceReflowToApplyCSSChanges() {
  const html = document.documentElement;
  const trigger = html.offsetHeight;
  void trigger;
  console.log('[Portal-Shell] Forced CSS recalculation');
}

onMounted(() => {
  themeStore.initialize();
  resetDataService();
  updateDataTheme();

  // Listen for localStorage changes (sidebar state)
  window.addEventListener('storage', updateSidebarState);

  // Periodic check for sidebar state (same-tab changes)
  setInterval(updateSidebarState, 100);
});

watch(() => route.path, () => {
  resetDataService();
});

watch(() => themeStore.isDark, (newVal) => {
  if (newVal) {
    document.documentElement.classList.add('dark');
  } else {
    document.documentElement.classList.remove('dark');
  }
  updateDataTheme();
  forceReflowToApplyCSSChanges();
});
</script>

<template>
  <div class="min-h-screen bg-bg-page text-text-body transition-colors duration-normal">
    <!-- Sidebar -->
    <Sidebar />

    <!-- Main Content Area -->
    <div
      :class="['min-h-screen flex flex-col transition-all duration-300', mainClass]"
    >
      <!-- Main Content -->
      <main class="flex-1">
        <Suspense>
          <template #default>
            <router-view v-slot="{ Component, route }">
              <KeepAlive v-if="route.meta.keepAlive" :max="3">
                <component
                  :is="Component"
                  :key="route.meta.remoteName || route.name"
                />
              </KeepAlive>
              <component
                v-else
                :is="Component"
                :key="route.name"
              />
            </router-view>
          </template>
          <template #fallback>
            <div class="flex items-center justify-center min-h-[400px]">
              <div class="text-center">
                <div class="w-10 h-10 border-2 border-brand-primary border-t-transparent rounded-full animate-spin mx-auto"></div>
                <p class="mt-4 text-text-meta text-sm font-medium">Loading...</p>
              </div>
            </div>
          </template>
        </Suspense>
      </main>

      <!-- Footer -->
      <footer class="bg-bg-card border-t border-border-default py-4 mt-auto">
        <div class="max-w-7xl mx-auto px-4 text-center">
          <p class="text-sm text-text-meta">© 2025 Portal Universe. All rights reserved.</p>
        </div>
      </footer>
    </div>
  </div>
</template>
