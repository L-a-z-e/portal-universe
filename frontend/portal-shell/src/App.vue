<script setup lang="ts">
import { useThemeStore } from "./store/theme.ts";
import { useSettingsStore } from "./store/settings.ts";
import { useAuthStore } from "./store/auth.ts";
import { useNotificationStore } from "./store/notification.ts";
import { useWebSocket } from "./composables/useWebSocket.ts";
import { onMounted, onBeforeUnmount, watch, ref, computed } from "vue";
import { useRoute } from "vue-router";
import Sidebar from "./components/Sidebar.vue";
import TopBar from "./components/TopBar.vue";
import QuickActions from "./components/QuickActions.vue";
import LoginModal from "./components/LoginModal.vue";
import ChatWidget from "./components/chat/ChatWidget.vue";
import { ToastContainer } from "@portal/design-vue";

const themeStore = useThemeStore();
const settingsStore = useSettingsStore();
const authStore = useAuthStore();
const notificationStore = useNotificationStore();
const route = useRoute();

// Initialize WebSocket for real-time notifications
// Connection state is managed internally by the composable
useWebSocket();

// Quick Actions modal state
const showQuickActions = ref(false);

// Sidebar collapsed state (synced with Sidebar component via CustomEvent)
const sidebarCollapsed = ref(localStorage.getItem('sidebar-collapsed') === 'true');

// Handle sidebar toggle event from Sidebar component
const handleSidebarToggle = (e: Event) => {
  const detail = (e as CustomEvent<{ collapsed: boolean }>).detail;
  sidebarCollapsed.value = detail.collapsed;
};

// Handle cross-tab localStorage changes
const handleStorageChange = (e: StorageEvent) => {
  if (e.key === 'sidebar-collapsed') {
    sidebarCollapsed.value = e.newValue === 'true';
  }
};

// Computed class for main content margin
const mainClass = computed(() => ({
  'lg:ml-72': !sidebarCollapsed.value,
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

  // Listen for sidebar toggle events (same-tab)
  window.addEventListener('sidebar-toggle', handleSidebarToggle);
  // Listen for cross-tab localStorage changes
  window.addEventListener('storage', handleStorageChange);
});

onBeforeUnmount(() => {
  window.removeEventListener('sidebar-toggle', handleSidebarToggle);
  window.removeEventListener('storage', handleStorageChange);
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

// Notification management based on auth state (WebSocket handles real-time updates)
watch(() => authStore.isAuthenticated, (isAuth) => {
  if (isAuth) {
    // Fetch initial unread count on login
    // Real-time updates handled by WebSocket (useWebSocket composable)
    notificationStore.fetchUnreadCount();
  } else {
    // Clear notifications on logout
    notificationStore.reset();
  }
}, { immediate: true });
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

    <!-- Chat Widget -->
    <ChatWidget />

    <!-- Main Content Area -->
    <div
      :class="['min-h-screen flex flex-col transition-all duration-300', mainClass]"
    >
      <!-- TopBar -->
      <TopBar @open-search="showQuickActions = true" />

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
      <footer class="border-t border-border-default py-8 mt-auto bg-bg-card">
        <div class="max-w-7xl mx-auto px-6 flex flex-col md:flex-row items-center justify-between gap-4 text-sm text-text-meta">
          <div>Portal Universe &copy; {{ new Date().getFullYear() }}</div>
          <div class="flex gap-8">
            <a href="https://github.com" target="_blank" rel="noopener" class="hover:text-text-heading transition-colors">GitHub</a>
            <router-link to="/blog" class="hover:text-text-heading transition-colors">Blog</router-link>
            <router-link to="/status" class="hover:text-text-heading transition-colors">Status</router-link>
          </div>
        </div>
      </footer>
    </div>
  </div>
</template>
