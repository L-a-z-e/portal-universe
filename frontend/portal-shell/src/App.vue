<script setup lang="ts">
import { useAuthStore } from "./store/auth.ts";
import { login, logout } from "./services/authService.ts";
import { Button, Badge } from '@portal/design-system';
import { useThemeStore } from "./store/theme.ts";
import { onMounted, watch} from "vue";
import ThemeToggle from "./components/ThemeToggle.vue";
import { useRoute } from "vue-router";

const authStore = useAuthStore();
const themeStore = useThemeStore();
const route = useRoute();

function updateDataTheme() {
  const isDark = document.documentElement.classList.contains('dark');
  document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light');
  console.log(`[Portal-Shell] Theme updated: ${isDark ? 'dark' : 'light'}`);
}

/**
 * 🟢 data-service 초기화
 * 호스트 앱 경로로 이동할 때 data-service="portal"로 리셋
 */
function resetDataService() {
  // 현재 라우트가 Remote가 아닐 때만 리셋
  if (!route.meta.remoteName) {
    document.documentElement.setAttribute('data-service', 'portal');
    console.log('[Portal-Shell] Route change: Reset data-service="portal"');
    forceReflowToApplyCSSChanges();
  }
}

/**
 * 🟢 CSS 변수 강제 재계산
 * KeepAlive로 인해 Blog CSS가 <head>에 남아있을 때,
 * data-service 변경 후 CSS 변수를 다시 계산하도록 강제함
 */
function forceReflowToApplyCSSChanges() {
  // 트릭: DOM 재배치 강제 (reflow trigger)
  // 이렇게 하면 브라우저가 CSS 변수 재계산 → Tailwind 클래스 다시 적용
  const html = document.documentElement;
  const trigger = html.offsetHeight;
  void trigger; // 변수 사용 (no-op)
  console.log('[Portal-Shell] Forced CSS recalculation');
}

// 페이지 로드 시 로컬 스토리지 값 반영
onMounted(() => {
  themeStore.initialize();
  
  // 🟢 초기 data-service 설정
  resetDataService();
  
  updateDataTheme();
});

// 라우트 변경 감지: data-service 리셋
watch(() => route.path, () => {
  resetDataService();
});

// <html> 태그에 dark 클래스 토글 반영
watch(() => themeStore.isDark, (newVal) => {
  if (newVal) {
    document.documentElement.classList.add('dark');
  } else {
    document.documentElement.classList.remove('dark');
  }
  updateDataTheme();
  
  // 🟢 강제 reflow: CSS 변수 재계산
  forceReflowToApplyCSSChanges();
});

</script>

<template>
  <div class="min-h-screen flex flex-col bg-bg-page text-text-body dark:bg-bg-page dark:text-text-body transition-colors duration-300">
    <!-- Header -->
    <header class="bg-bg-card dark:bg-bg-elevated backdrop-blur-md border-b border-border-default sticky top-0 z-50">
      <div class="max-w-7xl mx-auto px-4 py-4">
        <div class="flex items-center justify-between">
          <!-- Logo -->
          <router-link to="/" class="flex items-center gap-3 hover:opacity-80 transition-opacity">
            <div class="w-10 h-10 rounded-lg bg-gradient-to-br from-brand-primary to-brand-secondary flex items-center justify-center shadow-lg">
              <span class="text-text-inverse font-bold text-lg">P</span>
            </div>
            <span class="text-xl font-bold hidden sm:block text-text-heading">
              Portal Universe
            </span>
          </router-link>

          <!-- Navigation -->
          <nav class="flex items-center gap-8">
            <router-link
                to="/"
                class="text-text-meta hover:text-brand-primary font-medium transition-colors"
                active-class="text-brand-primary font-bold"
            >
              Home
            </router-link>
            <router-link
                to="/blog"
                class="text-text-meta hover:text-brand-primary font-medium transition-colors"
                active-class="text-brand-primary font-bold"
            >
              Blog
            </router-link>
          </nav>

          <!-- Auth Section -->
          <div class="flex items-center gap-3">
            <ThemeToggle />
            <template v-if="authStore.isAuthenticated">
              <div class="hidden md:flex items-center gap-2 px-4 py-2 rounded-lg bg-status-infoBg border border-border-default">
                <span class="text-sm font-semibold text-status-info">{{ authStore.displayName }}</span>
                <Badge v-if="authStore.isAdmin" variant="danger" size="sm">ADMIN</Badge>
              </div>
              <Button variant="secondary" size="sm" @click="logout">Logout</Button>
            </template>
            <template v-else>
              <div class="flex items-center gap-2">
                <router-link to="/signup">
                  <Button variant="secondary" size="sm">Sign Up</Button>
                </router-link>
                <Button variant="primary" size="sm" @click="login">Login</Button>
              </div>
            </template>
          </div>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="flex-1">
      <Suspense>
        <template #default>
          <router-view v-slot="{ Component, route }">
            <!-- 🔧 FIX: KeepAlive :max="1" → :max="3" (다중 페이지 캐싱으로 CSS 충돌 방지) -->
            <KeepAlive :max="3">
              <component
                  :is="Component"
                  :key="route.meta.remoteName || route.name"
              />
            </KeepAlive>
          </router-view>
        </template>
        <template #fallback>
          <div class="flex items-center justify-center min-h-[400px]">
            <div class="text-center">
              <div class="w-12 h-12 border-4 border-brand-primary border-t-transparent rounded-full animate-spin mx-auto"></div>
              <p class="mt-4 text-text-meta font-medium">Loading...</p>
            </div>
          </div>
        </template>
      </Suspense>
    </main>

    <!-- Footer -->
    <footer class="bg-bg-muted border-t border-border-default py-8 mt-auto">
      <div class="max-w-7xl mx-auto px-4 text-center">
        <p class="text-sm text-text-meta">© 2025 Portal Universe. All rights reserved.</p>
      </div>
    </footer>
  </div>
</template>