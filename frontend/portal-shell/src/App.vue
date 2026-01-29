<script setup lang="ts">
import { useThemeStore } from "./store/theme.ts";
import { useSettingsStore } from "./store/settings.ts";
import { useAuthStore } from "./store/auth.ts";
import { onMounted, onBeforeUnmount, watch, ref, computed } from "vue";
import { useRoute } from "vue-router";
import Sidebar from "./components/Sidebar.vue";
import QuickActions from "./components/QuickActions.vue";
import LoginModal from "./components/LoginModal.vue";
import { ToastContainer } from "@portal/design-system-vue";

const themeStore = useThemeStore();
const settingsStore = useSettingsStore();
const authStore = useAuthStore();
const route = useRoute();

// Quick Actions modal state
const showQuickActions = ref(false);

// Sidebar collapsed state (synced with Sidebar component via localStorage)
const sidebarCollapsed = ref(localStorage.getItem('sidebar-collapsed') === 'true');

// Interval ID for cleanup
let sidebarCheckInterval: ReturnType<typeof setInterval> | null = null;

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
}

/**
 * data-service 속성 동기화
 * 현재 라우트에 맞는 data-service 속성을 설정하여 서비스별 CSS 변수 활성화
 * - remote 앱 라우트: remoteName 기반 (blog, shopping 등)
 * - portal 자체 라우트: "portal"
 */
function syncDataService() {
  const serviceName = route.meta.remoteName as string || 'portal';
  document.documentElement.setAttribute('data-service', serviceName);
  forceReflowToApplyCSSChanges();
}

/**
 * CSS 변수 강제 재계산
 * data-service 속성 변경 시 CSS 변수가 즉시 적용되도록 브라우저 리플로우 트리거
 */
function forceReflowToApplyCSSChanges() {
  const html = document.documentElement;
  void html.offsetHeight;
}

onMounted(() => {
  themeStore.initialize();
  settingsStore.initialize();
  syncDataService();
  updateDataTheme();

  // Expose login modal trigger for remote apps
  (window as any).__PORTAL_SHOW_LOGIN__ = () => authStore.requestLogin();
  (window as any).__PORTAL_ON_AUTH_ERROR__ = () => authStore.requestLogin();

  // Listen for localStorage changes (sidebar state)
  window.addEventListener('storage', updateSidebarState);

  // Periodic check for sidebar state (same-tab changes)
  sidebarCheckInterval = setInterval(updateSidebarState, 100);
});

onBeforeUnmount(() => {
  // Cleanup event listener
  window.removeEventListener('storage', updateSidebarState);

  // Cleanup interval
  if (sidebarCheckInterval) {
    clearInterval(sidebarCheckInterval);
    sidebarCheckInterval = null;
  }
});

watch(() => route.path, () => {
  syncDataService();
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

    <!-- Quick Actions Modal (Cmd+K) -->
    <QuickActions v-model="showQuickActions" />

    <!-- Global Login Modal (triggered by navigation guard) -->
    <LoginModal v-model="authStore.showLoginModal" />

    <!-- Global Toast -->
    <ToastContainer />

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
